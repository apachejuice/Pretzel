package com.pretzel.core.parser.ast

import com.pretzel.core.lexer.Lexer
import com.pretzel.core.parser.ast.expr.Expression
import com.pretzel.core.parser.ast.visit.ASTVisitor

class VariableAssignment(
    override val target: Symbol,
    override val sourceRange: Lexer.Span,
    val value: Expression,
) : CodeNode(), SymbolTargetNode {
    override fun toString(): String {
        return "VariableAssignment(target = $target source = $value)"
    }

    override fun <T> accept(astVisitor: ASTVisitor<T>): T {
        return astVisitor.visitVariableAssignment(this)
    }

    override val nodeType: Type
        get() = Type.VARIABLE_ASSIGNMENT

    override val isVisibleInSource: Boolean = true
}