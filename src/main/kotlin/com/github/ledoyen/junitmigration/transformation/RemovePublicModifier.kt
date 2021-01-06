package com.github.ledoyen.junitmigration.transformation

import com.github.javaparser.ast.Modifier
import com.github.ledoyen.minimaldiffparser.*

class RemovePublicModifier : MinimalDiffVisitorAdapter() {

    override fun visit(cu: MinimalDiffCompilationUnit, n: MinimalDiffClassOrInterfaceDeclaration) {
        n.getModifiers().forEach { deleteModifierIfPublic(it) }
        super.visit(cu, n)
    }

    override fun visit(cu: MinimalDiffCompilationUnit, n: MinimalDiffMethodDeclaration) {
        n.getModifiers().forEach { deleteModifierIfPublic(it) }
    }

    private fun deleteModifierIfPublic(m: MinimalDiffModifier) {
        if (m.getKeyword() == Modifier.Keyword.PUBLIC) m.delete()
    }
}
