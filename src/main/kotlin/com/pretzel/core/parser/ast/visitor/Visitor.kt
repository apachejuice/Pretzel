package com.pretzel.core.parser.ast.visitor

import com.pretzel.core.parser.ast.ModStatement
import com.pretzel.core.parser.ast.Node
import com.pretzel.core.parser.ast.UseStatement

interface Visitor<T, Q> {
    fun visitNode(node: Node, data: Q? = null): T
    fun visitUseStatement(useStatement: UseStatement, data: Q? = null): T
    fun visitModStatement(modStatement: ModStatement, data: Q? = null): T
}