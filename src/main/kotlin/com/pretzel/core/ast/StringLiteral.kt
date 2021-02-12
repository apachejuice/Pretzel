package com.pretzel.core.ast

import com.pretzel.core.ast.visitor.NodeVisitor
import com.pretzel.core.lexer.Lexer

class StringLiteral(token: Lexer.Token, val template: Boolean = false) : Literal<String>(token) {
    override val value: String = literal

    override fun <T> accept(visitor: NodeVisitor<T>): T {
        return visitor.visitStringLiteral(this)
    }
}