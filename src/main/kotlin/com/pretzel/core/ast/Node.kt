package com.pretzel.core.ast

import com.pretzel.core.lexer.Lexer

open class Node(val start: Lexer.Context, val end: Lexer.Context) : Iterable<Node> {
    private val children: ArrayList<Node> = ArrayList()
    private val trace: ArrayList<Node> = ArrayList()

    fun addChild(child: Node) = children.add(child)

    fun traceTo(node: Node) = trace.add(node)

    override fun iterator(): Iterator<Node> = children.iterator()

    companion object {
        fun getRootInstance(children: List<Node>): Node {
            val start = children[0].start
            val end = children[children.indices.last].end
            return Node(start, end).also { node -> children.forEach { node.addChild(it) } }
        }
    }
}