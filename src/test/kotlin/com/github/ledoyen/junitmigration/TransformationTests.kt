package com.github.ledoyen.junitmigration

import com.github.difflib.DiffUtils
import com.github.ledoyen.junitmigration.transformation.RemovePublicModifier
import com.github.ledoyen.minimaldiffparser.MinimalDiffParser
import com.github.ledoyen.minimaldiffparser.MinimalDiffVisitor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.stream.Stream

class TransformationTests {

    @ParameterizedTest
    @MethodSource
    fun transformation_cases(transformationName: String, expectedPatchSize: Int, visitor: MinimalDiffVisitor) {
        val compilationUnit = MinimalDiffParser.parse(streamOf("transformation/$transformationName/SampleTest.before.java"))

        compilationUnit.accept(visitor)

        val changes = compilationUnit.getChanges()
        val modifiedCode = compilationUnit.applyChanges()
        val expected = toString(streamOf("transformation/$transformationName/SampleTest.after.java"))
        val patch = DiffUtils.diff(expected, modifiedCode, null)
        assertThat(patch.deltas).`as`(patch.deltas.map { d -> "Expecting\n\t${d.source}\nbut was\n\t${d.target}  (${d.type})" }.joinToString("\n", "\n")).isEmpty()
        assertThat(changes).hasSize(expectedPatchSize)
    }

    companion object {
        @JvmStatic
        fun transformation_cases(): Stream<Arguments> = Stream.of(
            arguments("public_modifier", 3, RemovePublicModifier())
        )
    }

    private fun toString(inputStream: InputStream) = Scanner(inputStream, StandardCharsets.UTF_8.name()).use { it.useDelimiter("\\A").next() }

    private fun streamOf(endPath: String) = TransformationTests::class.java.classLoader.getResourceAsStream(endPath)
}