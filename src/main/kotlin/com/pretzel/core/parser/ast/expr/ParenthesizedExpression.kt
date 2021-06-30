package com.pretzel.core.parser.ast.expr

import com.pretzel.core.lexer.Lexer
import com.pretzel.core.parser.ast.visit.ASTVisitor

class ParenthesizedExpression(val inner: Expression, sourceRange: Lexer.Span) :
    Expression(sourceRange) {
    override val isEvaluatableAtCompileTime: Boolean
        get() = inner.isEvaluatableAtCompileTime

    override fun toString(): String {
        return "ParenthesizedExpression($inner)"
    }

    override val nodeType: Type
        get() = Type.PARENTHESIZED_EXPRESSION

    override fun <T> accept(astVisitor: ASTVisitor<T>): T {
        return astVisitor.visitParenthesizedExpression(this)
    }
}