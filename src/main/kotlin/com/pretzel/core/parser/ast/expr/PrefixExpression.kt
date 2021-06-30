package com.pretzel.core.parser.ast.expr

import com.pretzel.core.lexer.Lexer
import com.pretzel.core.parser.ast.visit.ASTVisitor

class PrefixExpression(
    val target: Expression,
    val op: Operator,
    sourceRange: Lexer.Span
) : Expression(sourceRange) {
    enum class Operator(val op: String) {
        PLUS("+"),
        MINUS("-"),
        NEG("~"),
        NOT("!"),
        INC("++"),
        DEC("--"),
    }

    override fun toString(): String {
        return "PrefixExpression(target = $target op = ${op.op})"
    }

    override val nodeType: Type
        get() = Type.PREFIX_EXPRESSION

    override val isEvaluatableAtCompileTime: Boolean =
        target.isEvaluatableAtCompileTime

    override fun <T> accept(astVisitor: ASTVisitor<T>): T {
        return astVisitor.visitPrefixExpression(this)
    }
}