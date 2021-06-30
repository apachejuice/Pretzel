package com.pretzel.core.parser.ast.expr

import com.pretzel.core.lexer.Lexer

class BooleanLiteral(val value: Boolean, sourceRange: Lexer.Span) : Expression(sourceRange) {
    override val isEvaluatableAtCompileTime: Boolean
        get() = true

    override fun toString(): String {
        return "BooleanLiteral($value)"
    }

    override val nodeType: Type
        get() = Type.BOOLEAN_LITERAL
}