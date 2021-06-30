package com.pretzel.core.parser.ast

import com.pretzel.core.lexer.Lexer
import com.pretzel.core.parser.ast.annot.Modifier
import com.pretzel.core.parser.ast.expr.SymbolReference
import com.pretzel.core.parser.ast.visit.ASTVisitor

abstract class VariableDeclaration(
    override val target: SymbolReference,
    override val modifiers: List<Modifier>,
    override val sourceRange: Lexer.Span,
    val assignment: VariableAssignment?,
) : CodeNode(), ModifierTargetNode, SymbolPartTargetNode {
    override val isVisibleInSource: Boolean = true

    override fun <T> accept(astVisitor: ASTVisitor<T>): T {
        return astVisitor.visitVariableDeclaration(this)
    }

    override val modifierInteger: Int
        get() {
            var result = 0
            modifiers.forEach { result = result or it.value }
            return result
        }

    abstract val permittedModifiers: List<Modifier>
}