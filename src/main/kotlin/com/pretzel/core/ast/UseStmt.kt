package com.pretzel.core.ast

import com.pretzel.core.lexer.Lexer

class UseStmt(target: List<String>, start: Lexer.Context, end: Lexer.Context, isWildcard: Boolean = false) : Node(start, end) {
    val isWildcard: Boolean = isWildcard
    val target: String = target.joinToString { it -> "/$it" }

    override fun toString(): String {
        return "use $target" + if (isWildcard) ":*" else ""
    }
}