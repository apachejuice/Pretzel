package com.pretzel.core.ast

import com.pretzel.core.ast.visitor.NodeVisitor
import com.pretzel.core.lexer.Lexer

class ShortLiteral(token: Lexer.Token) : Literal<Short>(token) {
    init {
        if (token.type != Lexer.TokenType.SHORT_LITERAL || token.lexeme.toShortOrNull() == null)
            throw RuntimeException("ShortLiteral value must be a valid short")
    }

    override fun <T> accept(visitor: NodeVisitor<T>): T {
        return visitor.visitShortLiteral(this)
    }

    override val value: Short
        get() = literal.toShort()
}