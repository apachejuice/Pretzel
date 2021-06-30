package com.pretzel.core.parser.ast.expr

import com.pretzel.core.lexer.Lexer

class CastExpression(
    val source: Expression,
    val targetType: Expression,
    val op: Operator,
    sourceRange: Lexer.Span
) : Expression(sourceRange) {
    enum class Operator(val op: String) {
        TO("to"),
        AS("as"),
    }

    override val isEvaluatableAtCompileTime: Boolean
        get() = false // it can be discarded though

    override fun toString(): String {
        return "CastExpression(source = $source target = $targetType op = ${op.op})"
    }

    override val nodeType: Type
        get() = Type.CAST
}