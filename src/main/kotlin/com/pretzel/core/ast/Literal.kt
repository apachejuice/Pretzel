package com.pretzel.core.ast

import com.pretzel.core.lexer.Lexer

class Literal(private val token: Lexer.Token) : Expression(token.toContext(), token.toContext(), Precedence.LOWEST) {
    override fun <T> accept(visitor: NodeVisitor<T>): T {
        return visitor.visitLiteral(this)
    }

    override fun toString(): String = token.lexeme ?: "null"

    val type: Lexer.TokenType
        get() = token.type
}