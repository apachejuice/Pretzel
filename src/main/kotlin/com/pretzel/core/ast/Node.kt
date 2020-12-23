package com.pretzel.core.ast

import com.pretzel.core.lexer.Lexer

abstract class Node(val start: Lexer.Context, val end: Lexer.Context) : Iterable<Node> {
    private val children: ArrayList<Node> = ArrayList()
    private val trace: ArrayList<Node> = ArrayList()

    fun addChild(child: Node) = children.add(child)

    fun traceTo(node: Node) = trace.add(node)

    override fun iterator(): Iterator<Node> = children.iterator()

    abstract fun <T> accept(visitor: NodeVisitor<T>): T

    companion object {
        fun getRootInstance(children: List<Node>): Node {
            if (children.isEmpty()) throw RuntimeException("children.size == 0")
            val start = children[0].start
            val end = children[children.indices.last].end
            return object : Node(start, end) {
                override fun <T> accept(visitor: NodeVisitor<T>): T = visitor.visitRoot(this)
            }.also { node -> children.forEach { node.addChild(it) } }
        }
    }
}