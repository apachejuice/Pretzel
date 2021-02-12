package com.pretzel.core.ast

import com.pretzel.core.ast.visitor.NodeVisitor
import com.pretzel.core.lexer.Lexer

class IntLiteral(token: Lexer.Token) : Literal<Int>(token) {
    init {
        if (token.type != Lexer.TokenType.INTEGER_LITERAL || token.lexeme.toIntOrNull() == null)
            throw RuntimeException("IntegerLiteral value must be a valid integer")
    }

    override fun <T> accept(visitor: NodeVisitor<T>): T {
        return visitor.visitIntLiteral(this)
    }

    override val value: Int
        get() = literal.toInt()
}