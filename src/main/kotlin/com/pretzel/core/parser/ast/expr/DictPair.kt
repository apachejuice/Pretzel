package com.pretzel.core.parser.ast.expr

import com.pretzel.core.lexer.Lexer

class DictPair(
    val left: Expression,
    val right: Expression,
    sourceRange: Lexer.Span
) : Expression(sourceRange) {
    override val isEvaluatableAtCompileTime: Boolean
        get() = false

    override fun toString(): String {
        return "DictPair($left : $right)"
    }

    override val nodeType: Type
        get() = Type.DICT_PAIR
}