package com.pretzel.core.parser.ast.expr

import com.pretzel.core.lexer.Lexer

class MemberAccess(
    val source: Expression,
    val accessor: SymbolReference,
    val op: Operator,
    sourceRange: Lexer.Span
) : Expression(sourceRange) {
    enum class Operator(val op: String) {
        NULLSAFE("?."),
        CLASSIC("."),
        CHAINING(".."),
    }

    override val isEvaluatableAtCompileTime: Boolean
        get() = true

    override fun toString(): String {
        return "MemberAccess(source = $source op = ${op.op} accessor = $accessor)"
    }

    override val nodeType: Type
        get() = Type.MEMBER_ACCESS
}