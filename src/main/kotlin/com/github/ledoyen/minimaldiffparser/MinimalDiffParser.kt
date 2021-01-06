package com.github.ledoyen.minimaldiffparser

import com.github.javaparser.Position
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.nodeTypes.NodeWithModifiers
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.*
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
}

class MinimalDiffCompilationUnit(private var code: String, val cu: CompilationUnit) {

    internal val changes = ArrayList<Change>()

    fun accept(visitor: MinimalDiffVisitor): MinimalDiffCompilationUnit {
        visitor.visit(this)
        return this
    }

    fun getChanges() = changes.toList()

    fun lineAt(pos: Position) = code.lines()[pos.line - 1]

    fun applyChanges(): String {
        var lineOffset = 0
        changes.forEach {

            when (it) {
                is Deletion -> {
                    // TODO code.replace ?
                    val lines = ArrayList(code.lines())
                    if (it.start.line == it.end.line) {
                        val lineIndex = it.start.line - 1 + lineOffset
                        lines[lineIndex] = lines[lineIndex].removeRange(it.start.column - 1, it.end.column - 1)
                        if (lines[lineIndex].trim().isEmpty()) {
                            lines.removeAt(lineIndex)
                            lineOffset--
                        }
                    }
                    // TODO multi line
                    lineOffset -= (it.end.line - it.start.line)
                    code = lines.joinToString("\n")
                }
            }

        }
        return code
    }
}

interface MinimalDiffNodeWithModifiers<T> : MinimalDiffNode<T> where T : NodeWithModifiers<out Node>, T : Node {
    fun getModifiers() = n().modifiers.map { MinimalDiffModifier(cu(), it) }
}

class MinimalDiffClassOrInterfaceDeclaration(private val cu: MinimalDiffCompilationUnit, private val n: ClassOrInterfaceDeclaration) : MinimalDiffNodeWithModifiers<ClassOrInterfaceDeclaration> {
    override fun cu() = cu
    override fun n() = n

    fun accept(visitor: MinimalDiffVisitor) {
        visitor.visit(cu, this)
    }
}

class MinimalDiffMethodDeclaration(private val cu: MinimalDiffCompilationUnit, private val n: MethodDeclaration) : MinimalDiffNodeWithModifiers<MethodDeclaration> {
    override fun cu() = cu
    override fun n() = n

    fun accept(visitor: MinimalDiffVisitor) {
        visitor.visit(cu, this)
    }
}

class MinimalDiffModifier(private val cu: MinimalDiffCompilationUnit, private val n: Modifier) : MinimalDiffNode<Modifier> {
    override fun cu() = cu
    override fun n() = n

    fun getKeyword(): Modifier.Keyword = n.keyword
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


sealed class Change
data class Deletion(val start: Position, val end: Position) : Change()

fun Position.plusCol(colOffset: Int): Position {
    return Position(this.line, this.column + colOffset)
}
