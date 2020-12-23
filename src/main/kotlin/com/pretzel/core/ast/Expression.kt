package com.pretzel.core.ast

import com.pretzel.core.lexer.Lexer

open class Expression(start: Lexer.Context, end: Lexer.Context, precedence: Precedence) : Node(start, end) {
    override fun <T> accept(visitor: NodeVisitor<T>): T {
        return visitor.visitExpression(this)
    }
}