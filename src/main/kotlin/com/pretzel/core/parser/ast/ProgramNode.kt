package com.pretzel.core.parser.ast

import com.pretzel.core.lexer.Lexer

class ProgramNode(span: Lexer.Span) : Node(span) {
    override var canBeEvaluated: Boolean
        get() = TODO("Not yet implemented")
        set(value) {}

    override fun toString(): String {
        TODO("Not yet implemented")
    }
}