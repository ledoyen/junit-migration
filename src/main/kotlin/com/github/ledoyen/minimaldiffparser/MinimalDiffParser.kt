package com.github.ledoyen.minimaldiffparser

import com.github.javaparser.Position
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.AnnotationExpr
import com.github.javaparser.ast.expr.MemberValuePair
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations
import com.github.javaparser.ast.nodeTypes.NodeWithModifiers
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.Statement
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.collections.ArrayList


object MinimalDiffParser {
    fun parse(code: String) = MinimalDiffCompilationUnit(code, StaticJavaParser.parse(code))

    fun parse(inputStream: InputStream) = parse(Scanner(inputStream, StandardCharsets.UTF_8.name()).use { it.useDelimiter("\\A").next() })
}

interface MinimalDiffNode<out T : Node> {

    fun cu(): MinimalDiffCompilationUnit
    fun n(): T

    fun delete() {
        val end = n().end.get()
        val endLine = cu().lineAt(end)
        val haveSpaceAfterToken = endLine.length > end.column + 1 && endLine[end.column] == ' '
        cu().changes.add(Deletion(n().begin.get(), end.plusCol(if (haveSpaceAfterToken) 2 else 1)))
        n().remove()
    }

    fun replace(replacementFunction: (String, String) -> String) {
        cu().changes.add(Replacement(n().begin.get(), n().end.get(), replacementFunction))
    }
}

class MinimalDiffCompilationUnit(private var code: String, val cu: CompilationUnit) {

    internal val changes = TreeSet<Change>()

    fun accept(visitor: MinimalDiffVisitor): MinimalDiffCompilationUnit {
        visitor.visit(this)
        return this
    }

    fun getChanges() = changes.toList()

    fun lineAt(pos: Position) = code.lines()[pos.line - 1]

    private fun ordinalPosition(pos: Position) = code.lines().subList(0, pos.line - 1).map { l -> l.length + 1 }.sum() + pos.column - 1

    fun applyChanges(): String {
        changes.forEach {

            when (it) {
                is Deletion -> {
                    // TODO code.replace ?
                    val lines = ArrayList(code.lines())
                    if (it.start.line == it.end.line) {
                        val lineIndex = it.start.line - 1
                        lines[lineIndex] = lines[lineIndex].removeRange(it.start.column - 1, it.end.column - 1)
                        if (lines[lineIndex].trim().isEmpty()) {
                            lines.removeAt(lineIndex)
                        }
                    } else {
                        TODO("multiline")
                    }
                    code = lines.joinToString("\n")
                }
                is Replacement -> {
                    val indent = lineAt(it.start).takeWhile { c -> c == ' ' }
                    val ordinalStart = ordinalPosition(it.start)
                    val ordinalEnd = ordinalPosition(it.end) + 1
                    val statement = code.substring(ordinalStart, ordinalEnd)
                    val replacement = it.replacementFunction(statement, indent)
                    code = code.replaceRange(ordinalStart, ordinalEnd, replacement)
                }
                is Insertion -> {
                    val parts = code.chunked(ordinalPosition(it.pos))
                    code = parts[0] + it.content + parts.drop(1).joinToString("")
                }
            }

        }
        return code
    }

    fun addStaticImport(fullyQualifiedMethod: String) {
        val staticImports = cu.imports.filter { i -> i.isStatic }
        if (staticImports
                .none { i -> i.nameAsString == fullyQualifiedMethod }
        ) {
            val (endOfLastImport, contentPrefix) = if (staticImports.isEmpty()) {
                Pair(cu.imports.last().end.get(), "\n")
            } else {
                Pair(staticImports.last().end.get(), "")
            }
            changes.add(Insertion(endOfLastImport.plusCol(1), "$contentPrefix\nimport static $fullyQualifiedMethod;"))
            cu.addImport(fullyQualifiedMethod, true, false)
        }
    }

    fun replaceTextByRegex(regex: String, replacement: String) = replaceTextByRegex(regex) { replacement }

    fun replaceTextByRegex(regex: String, replacementFunction: (Matcher) -> String) {
        val contentBuilder = StringBuffer()
        val matcher = Pattern.compile(regex).matcher(code)

        while (matcher.find()) {
            val replacement: String = replacementFunction(matcher)
            matcher.appendReplacement(contentBuilder, replacement)
        }
        matcher.appendTail(contentBuilder)

        this.code = contentBuilder.toString()
    }
}

interface MinimalDiffNodeWithModifiers<T> : MinimalDiffNode<T> where T : NodeWithModifiers<out Node>, T : Node {
    fun getModifiers() = n().modifiers.map { MinimalDiffModifier(cu(), it) }
}

interface MinimalDiffNodeWithAnnotations<T> : MinimalDiffNode<T> where T : NodeWithAnnotations<out Node>, T : Node {
    fun getAnnotationByName(name: String): Optional<MinimalDiffAnnotationExpr> = n().getAnnotationByName(name).map { MinimalDiffAnnotationExpr(cu(), it) }
}

class MinimalDiffClassOrInterfaceDeclaration(private val cu: MinimalDiffCompilationUnit, private val n: ClassOrInterfaceDeclaration) : MinimalDiffNodeWithModifiers<ClassOrInterfaceDeclaration> {
    override fun cu() = cu
    override fun n() = n

    fun accept(visitor: MinimalDiffVisitor) {
        visitor.visit(cu, this)
    }
}

class MinimalDiffMethodDeclaration(private val cu: MinimalDiffCompilationUnit, private val n: MethodDeclaration) : MinimalDiffNodeWithModifiers<MethodDeclaration>, MinimalDiffNodeWithAnnotations<MethodDeclaration> {
    override fun cu() = cu
    override fun n() = n

    fun accept(visitor: MinimalDiffVisitor) {
        visitor.visit(cu, this)
    }

    fun getStatements() = n().findFirst(BlockStmt::class.java).get().statements.map { MinimalDiffStatement(cu(), it) }.toList()
}

class MinimalDiffModifier(private val cu: MinimalDiffCompilationUnit, private val n: Modifier) : MinimalDiffNode<Modifier> {
    override fun cu() = cu
    override fun n() = n

    fun getKeyword(): Modifier.Keyword = n.keyword
}

class MinimalDiffAnnotationExpr(private val cu: MinimalDiffCompilationUnit, private val n: AnnotationExpr) : MinimalDiffNode<AnnotationExpr> {
    override fun cu() = cu
    override fun n() = n

    fun getMemberValuePairByName(name: String): Optional<MinimalDiffMemberValuePair> = Optional.ofNullable(n().findAll(MemberValuePair::class.java).find { mvp -> mvp.name.identifier == name }).map { MinimalDiffMemberValuePair(cu(), it, this) }
}

class MinimalDiffMemberValuePair(private val cu: MinimalDiffCompilationUnit, private val n: MemberValuePair, private val parent: MinimalDiffAnnotationExpr) : MinimalDiffNode<MemberValuePair> {
    override fun cu() = cu
    override fun n() = n

    fun valueAsString() = n.value.toString()

    override fun delete() {
        if (parent.n().findAll(MemberValuePair::class.java).size == 1) {
            n.removeForced()
            parent.replace { _, _ -> "@${parent.n().name.asString()}" }
        } else {
            super.delete()
            // TODO implement a value member removal among others
        }
    }
}

class MinimalDiffStatement(private val cu: MinimalDiffCompilationUnit, private val n: Statement) : MinimalDiffNode<Statement> {
    override fun cu() = cu
    override fun n() = n
}

interface MinimalDiffVisitor {
    fun visit(cu: MinimalDiffCompilationUnit)
    fun visit(cu: MinimalDiffCompilationUnit, n: MinimalDiffClassOrInterfaceDeclaration)
    fun visit(cu: MinimalDiffCompilationUnit, n: MinimalDiffMethodDeclaration)
}

open class MinimalDiffVisitorAdapter : MinimalDiffVisitor {
    override fun visit(cu: MinimalDiffCompilationUnit) {
        cu.cu.types
            .filterIsInstance<ClassOrInterfaceDeclaration>()
            .map { MinimalDiffClassOrInterfaceDeclaration(cu, it) }
            .forEach { it.accept(this) }
    }

    override fun visit(cu: MinimalDiffCompilationUnit, n: MinimalDiffClassOrInterfaceDeclaration) {
        n.n().members
            .filterIsInstance<MethodDeclaration>()
            .map { MinimalDiffMethodDeclaration(cu, it) }
            .forEach { it.accept(this) }
    }

    override fun visit(cu: MinimalDiffCompilationUnit, n: MinimalDiffMethodDeclaration) {
        // to override
    }
}


sealed class Change(private val anchor: Position) : Comparable<Change> {
    override fun compareTo(other: Change) = -anchor.compareTo(other.anchor)
}

data class Deletion(val start: Position, val end: Position) : Change(start)
data class Replacement(val start: Position, val end: Position, val replacementFunction: (String, String) -> String) : Change(start)
data class Insertion(val pos: Position, val content: String) : Change(pos)

fun Position.plusCol(colOffset: Int): Position {
    return Position(this.line, this.column + colOffset)
}
