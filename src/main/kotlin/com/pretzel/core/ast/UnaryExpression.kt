package com.pretzel.core.ast

import com.pretzel.core.parser.Parser

class UnaryExpression(val target: Expression, val operator: Parser.UnaryOperator?
    ) : Expression(target.start, target.end, Precedence.SUPER_HIGH) {
    override fun <T> accept(visitor: NodeVisitor<T>): T {
        return visitor.visitUnaryExpression(this)
    }

    override fun toString(): String {
        return "${operator?.operator ?: ""}$target"
    }
}