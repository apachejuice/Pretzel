package com.pretzel.core.parser.ast.expr

import com.pretzel.core.lexer.Lexer

class ListLiteral(val content: List<Expression?>, sourceRange: Lexer.Span) : Expression(sourceRange) {
    override val isEvaluatableAtCompileTime: Boolean
        get() = false

    override fun toString(): String {
        return "ListLiteral(${content.size})"
    }

    override val nodeType: Type
        get() = Type.LIST_LITERAL
}