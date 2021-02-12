package com.pretzel.core.ast

import com.pretzel.core.ast.visitor.NodeVisitor

class TrinaryExpression(
    val condition: Expression,
    val ifTrue: Expression,
    val ifFalse: Expression
) : Expression(condition.start, ifFalse.end, Precedence.EXTREMELY_LOW) {
    override fun toString(): String {
        return "$condition?$ifTrue:$ifFalse"
    }

    override fun <T> accept(visitor: NodeVisitor<T>): T {
        return visitor.visitTrinaryExpression(this)
    }
}