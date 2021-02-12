package com.pretzel.core.ast

import com.pretzel.core.ast.visitor.NodeVisitor
import com.pretzel.core.lexer.Lexer

class BooleanLiteral(token: Lexer.Token) : Literal<Boolean>(token) {
    init {
        if (token.type != Lexer.TokenType.YES || token.type != Lexer.TokenType.NO)
            throw RuntimeException("BooleanLiteral value must be 'yes' or 'no'")
    }

    override fun <T> accept(visitor: NodeVisitor<T>): T {
        return visitor.visitBooleanLiteral(this)
    }

    override val value: Boolean
        get() = this.literal == "yes" // yes, i know it's genius.
}