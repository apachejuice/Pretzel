package com.pretzel.core.ast

import com.pretzel.core.lexer.Lexer

class ModStmt(target: List<String>, start: Lexer.Context, end: Lexer.Context) : Node(start, end) {
    val target: String = target.joinToString(separator = ":") { it -> it }

    override fun <T> accept(visitor: NodeVisitor<T>): T {
        return visitor.visitModStmt(this)
    }

    override fun toString(): String {
        return "mod $target"
    }
}