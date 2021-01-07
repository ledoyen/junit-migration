package com.github.ledoyen.junitmigration

import com.github.ledoyen.junitmigration.transformation.ExpectedAnnotationParameter
import com.github.ledoyen.junitmigration.transformation.RemovePublicModifier
import com.github.ledoyen.junitmigration.transformation.ReplaceBasicAnnotations
import com.github.ledoyen.minimaldiffparser.MinimalDiffParser
import com.github.ledoyen.minimaldiffparser.MinimalDiffVisitor
import picocli.CommandLine
import java.nio.file.Files
import java.nio.file.Path
import java.util.regex.Pattern
import kotlin.system.exitProcess
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.toDuration

fun main(args: Array<String>) {
    val arguments = Arguments()
    val exitCode = CommandLine(arguments).execute(*args)
    exitProcess(exitCode)
}

class Arguments : Runnable {
    @CommandLine.Parameters
    lateinit var rootPath: Path

    @CommandLine.Option(names = ["-v"], description = ["verbose, display the modified files"])
    var verbose: Boolean = false

    private val visitors = listOf<MinimalDiffVisitor>(
        RemovePublicModifier(),
        ExpectedAnnotationParameter(),
        ReplaceBasicAnnotations()
    )

    @ExperimentalTime
    override fun run() {
        val startTime = System.currentTimeMillis()
        val onlyJUnit4Pattern = Pattern.compile("org\\.junit\\.[^j]")
        val modifiedFiles = rootPath.toFile().walk()
            .map { it.toPath() }
            .filter { p -> Files.isRegularFile(p) }
            .filter { p -> p.fileName.toString().endsWith(".java") }
            .map { PathAndContent(it) }
            .filter { pac -> onlyJUnit4Pattern.matcher(pac.content).find() }
            .map { transform(it) }
            .filter { it }
            .count()

        val hrDuration = (System.currentTimeMillis() - startTime)
            .toDuration(DurationUnit.MILLISECONDS)
            .toComponents { minutes, seconds, nanoseconds ->
                (if (minutes > 0) "$minutes min " else "") + "$seconds sec ${nanoseconds / 1_000_000} ms" }
        if(verbose) println()
        println("Modified $modifiedFiles files in $hrDuration.")
    }

    private fun transform(it: PathAndContent): Boolean {
        val compilationUnit = MinimalDiffParser.parse(it.content)
        visitors.forEach { compilationUnit.accept(it) }

        val modifiedCode = compilationUnit.applyChanges()

        Files.write(it.path, modifiedCode.toByteArray())

        if(verbose) println(it.path)
        return it.content != modifiedCode
    }
}

data class PathAndContent(val path: Path) {
    val content = String(Files.readAllBytes(path))
}
