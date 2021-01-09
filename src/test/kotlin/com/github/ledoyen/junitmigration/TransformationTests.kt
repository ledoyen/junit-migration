package com.github.ledoyen.junitmigration

import com.github.difflib.DiffUtils
import com.github.ledoyen.junitmigration.transformation.ExpectedAnnotationParameter
import com.github.ledoyen.junitmigration.transformation.RemovePublicModifier
import com.github.ledoyen.junitmigration.transformation.ReplaceBasicAnnotations
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
    fun transformation_cases(transformationName: String, expectedPatchSize: Int, visitors: List<MinimalDiffVisitor>) {
        val compilationUnit = MinimalDiffParser.parse(streamOf("transformation/$transformationName/SampleTest.before.java"))

        visitors.forEach { compilationUnit.accept(it) }

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
            arguments("public_modifier", 3, listOf(RemovePublicModifier())),
            arguments("expected_annotation_parameter", 3, listOf(ExpectedAnnotationParameter())),
            arguments("replace_basic_annotations", 0, listOf(ReplaceBasicAnnotations())),

            arguments("all_in_one", 8, transformations),
        )
    }

    private fun toString(inputStream: InputStream) = Scanner(inputStream, StandardCharsets.UTF_8.name()).use { it.useDelimiter("\\A").next() }

    private fun streamOf(endPath: String) = TransformationTests::class.java.classLoader.getResourceAsStream(endPath)
}
