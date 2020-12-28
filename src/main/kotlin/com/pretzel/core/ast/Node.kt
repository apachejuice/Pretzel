/*
 * Copyright 2020 Valio Valtokari
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     <http://www.apache.org/licenses/LICENSE-2.0>
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pretzel.core.ast

import com.pretzel.core.ast.visitor.NodeVisitor
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
                override fun <T> accept(visitor: NodeVisitor<T>): T = visitor.visitNode(this)
            }.also { node -> children.forEach { node.addChild(it) } }
        }
    }
}