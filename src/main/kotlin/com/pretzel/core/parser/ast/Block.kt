package com.pretzel.core.parser.ast

import com.pretzel.core.lexer.Lexer
import com.pretzel.core.parser.ast.visit.ASTVisitor

class Block(val nodes: List<CodeNode?>, override val sourceRange: Lexer.Span) : CodeNode() {
    override fun toString(): String {
        return "Block(nodes = ${nodes.size})"
    }

    override val nodeType: Type
        get() = Type.BLOCK

    override val isVisibleInSource: Boolean
        get() = true

    override fun <T> accept(astVisitor: ASTVisitor<T>): T {
        return astVisitor.visitBlock(this)
    }
}