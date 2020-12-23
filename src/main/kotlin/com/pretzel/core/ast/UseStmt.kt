package com.pretzel.core.ast

import com.pretzel.core.lexer.Lexer

class UseStmt(target: List<String>, start: Lexer.Context, end: Lexer.Context, val isWildcard: Boolean = false) : Node(start, end) {
    val target: String = target.joinToString(separator = ":") { it -> it }

    override fun <T> accept(visitor: NodeVisitor<T>): T {
        return visitor.visitUseStmt(this)
    }

    override fun toString(): String {
        return "use $target" + if (isWildcard) ":*" else ""
    }
}