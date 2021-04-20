package com.pretzel.core.parser.ast

import com.pretzel.core.lexer.Lexer
import com.pretzel.core.parser.ast.visitor.Checker
import com.pretzel.core.parser.ast.visitor.Visitor

abstract class Node(span: Lexer.Span) : Iterable<Node> {
    protected val children: MutableList<Node> = mutableListOf()

    fun addChild(child: Node) = children.add(child)

    abstract var canBeEvaluated: Boolean
        protected set

    var erroneous: Boolean = false
    var checked: Boolean = false

    open fun <T, Q> accept(visitor: Visitor<T, Q>, data: Q? = null): T =
        visitor.visitNode(this, data)

    open fun <T, Q> check(checker: Checker<T, Q>, data: Q? = null): T =
        checker.visitNode(this, data)

    abstract override fun toString(): String

    override fun iterator(): Iterator<Node> {
        return children.iterator()
    }
}