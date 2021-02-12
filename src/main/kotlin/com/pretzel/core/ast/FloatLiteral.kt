package com.pretzel.core.ast

import com.pretzel.core.ast.visitor.NodeVisitor
import com.pretzel.core.lexer.Lexer

class FloatLiteral(token: Lexer.Token) : Literal<Float>(token) {
    init {
        if (token.type != Lexer.TokenType.FLOAT_LITERAL || token.lexeme.toFloatOrNull() == null)
            throw RuntimeException("FloatLiteral value must be a valid float")
    }

    override fun <T> accept(visitor: NodeVisitor<T>): T {
        return visitor.visitFloatLiteral(this)
    }

    override val value: Float
        get() = literal.toFloat()
}