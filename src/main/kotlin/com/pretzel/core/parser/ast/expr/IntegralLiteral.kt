package com.pretzel.core.parser.ast.expr

import com.pretzel.core.lexer.Lexer
import com.pretzel.core.parser.ast.visit.ASTVisitor

class IntegralLiteral(val literal: String, sourceRange: Lexer.Span) :
    Expression(sourceRange) {
    override val isEvaluatableAtCompileTime: Boolean
        get() = true

    override fun toString(): String {
        return "IntegralLiteral($literal)"
    }

    override val nodeType: Type
        get() = Type.INTEGRAL_LITERAL

    override fun <T> accept(astVisitor: ASTVisitor<T>): T {
        return astVisitor.visitIntegralLiteral(this)
    }
}
