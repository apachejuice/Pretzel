package com.pretzel.core.ast

import com.pretzel.core.ast.visitor.NodeVisitor
import com.pretzel.core.lexer.Lexer

class TypeReference(
    val target: String,
    val arrayDepth: Int,
    start: Lexer.Location,
    end: Lexer.Location
) : Node(start, end) {
    override fun <T> accept(visitor: NodeVisitor<T>): T {
        return visitor.visitTypeReference(this)
    }
}