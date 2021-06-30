package com.pretzel.core.parser.ast.expr

import com.pretzel.core.lexer.Lexer

// We're using a List<Pair<...>> instead of a Map<...> because the expressions
// don't necessarily work like a map at compile time.
class DictLiteral(
    val values: List<DictPair?>,
    sourceRange: Lexer.Span
) : Expression(sourceRange) {
    companion object {
        fun empty(span: Lexer.Span): DictLiteral = DictLiteral(listOf(), span)
    }

    override val isEvaluatableAtCompileTime: Boolean
        get() = false

    override fun toString(): String {
        return "DictLiteral(${values.size})"
    }

    override val nodeType: Type
        get() = Type.DICT_LITERAL
}