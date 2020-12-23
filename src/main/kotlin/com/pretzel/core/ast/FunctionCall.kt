package com.pretzel.core.ast

import com.pretzel.core.lexer.Lexer

class FunctionCall(val name: String, start: Lexer.Context, end: Lexer.Context) : Expression(start, end, Precedence.EXTREMELY_HIGH) {
    override fun <T> accept(visitor: NodeVisitor<T>): T {
        return visitor.visitFunctionCall(this)
    }
}