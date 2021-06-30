package com.pretzel.core.parser.ast.expr

import com.pretzel.core.lexer.Lexer

class LambdaExpression(val args: List<SymbolReference>, val body: Expression? /* Can be a BlockExpression */, sourceRange: Lexer.Span) :
    Expression(sourceRange) {
    override val isEvaluatableAtCompileTime: Boolean
        get() = false

    override fun toString(): String {
        return "LambdaExpression(args = ${args.size})"
    }

    override val nodeType: Type
        get() = Type.LAMBDA
}