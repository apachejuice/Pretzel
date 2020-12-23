package com.pretzel.core.ast

import com.pretzel.core.parser.Parser

class BinaryExpression(
    val left: Expression,
    val right: Expression,
    val operator: Parser.BinaryOperator,
    precedence: Precedence
) : Expression(left.start, right.end, precedence) {

    override fun <T> accept(visitor: NodeVisitor<T>): T {
        return visitor.visitBinaryExpression(this)
    }

    override fun toString(): String {
        return "left:$left op:${operator.operator} right:$right"
    }
}