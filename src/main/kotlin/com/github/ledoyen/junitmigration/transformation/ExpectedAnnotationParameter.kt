package com.github.ledoyen.junitmigration.transformation

import com.github.ledoyen.minimaldiffparser.MinimalDiffCompilationUnit
import com.github.ledoyen.minimaldiffparser.MinimalDiffMethodDeclaration
import com.github.ledoyen.minimaldiffparser.MinimalDiffVisitorAdapter

class ExpectedAnnotationParameter : MinimalDiffVisitorAdapter() {

    override fun visit(cu: MinimalDiffCompilationUnit, n: MinimalDiffMethodDeclaration) {
        val annotationExpr = n.getAnnotationByName("Test")
        if (annotationExpr.isPresent) {
            val expectedMemberValuePair = annotationExpr.get()
                .getMemberValuePairByName("expected")

            if (expectedMemberValuePair.isPresent) {
                val exceptionClass = expectedMemberValuePair.get().valueAsString()
                expectedMemberValuePair.get().delete()

                val lastStatement = n.getStatements().last()

                lastStatement.replace { stmt, indent ->
                    """
                    assertThatExceptionOfType($exceptionClass)
                    $indent    .isThrownBy(() -> %s);
                    """.trimIndent()
                        // Little trick here, we do not use string interpolation
                        // to avoid trimIndent to remove intended indent from the original statement.
                        //
                        // Then we add on all lines (after the first which is inlined) the 4 spaces
                        // indent from the isThrownBy line.
                        .format(addIndentStartingAtSecondLine(stmt.substringBeforeLast(';'), "    "))
                }

                cu.addStaticImport("org.assertj.core.api.Assertions.assertThatExceptionOfType")
            }
        }
    }

    private fun addIndentStartingAtSecondLine(bloc: String, indent: String): String {
        val lines = bloc.lines().toMutableList()
        lines.subList(1, lines.size).replaceAll { l -> indent + l }
        return lines.joinToString("\n")
    }
}
