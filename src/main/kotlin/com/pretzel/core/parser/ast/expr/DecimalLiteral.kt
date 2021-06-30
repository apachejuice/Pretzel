package com.pretzel.core.parser.ast.expr

import com.pretzel.core.lexer.Lexer
import com.pretzel.core.parser.ast.visit.ASTVisitor

class DecimalLiteral(val literal: String, sourceRange: Lexer.Span) :
    Expression(sourceRange) {
    override val isEvaluatableAtCompileTime: Boolean
        get() = true

    override fun toString(): String {
        return "DecimalLiteral($literal)"
    }

    override val nodeType: Type
        get() = Type.DECIMAL_LITERAL

    override fun <T> accept(astVisitor: ASTVisitor<T>): T {
        return astVisitor.visitDecimalLiteral(this)
    }
}