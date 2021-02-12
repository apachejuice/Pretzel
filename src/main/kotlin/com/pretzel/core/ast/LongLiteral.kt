package com.pretzel.core.ast

import com.pretzel.core.ast.visitor.NodeVisitor
import com.pretzel.core.lexer.Lexer

class LongLiteral(token: Lexer.Token) : Literal<Long>(token) {
    init {
        if (token.type != Lexer.TokenType.LONG_LITERAL || token.lexeme.toLongOrNull() == null)
            throw RuntimeException("LongLiteral value must be a valid long")
    }

    override fun <T> accept(visitor: NodeVisitor<T>): T {
        return visitor.visitLongLiteral(this)
    }

    override val value: Long
        get() = literal.toLong()
}