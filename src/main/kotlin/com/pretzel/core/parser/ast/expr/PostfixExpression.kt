package com.pretzel.core.parser.ast.expr

import com.pretzel.core.lexer.Lexer
import com.pretzel.core.parser.ast.visit.ASTVisitor

class PostfixExpression(
    sourceRange: Lexer.Span,
    val target: Expression,
    val op: Operator
) : Expression(sourceRange) {
    enum class Operator(val op: String) {
        INC("++"),
        DEC("--"),
        NOTNULL("!!"),
    }

    override fun toString(): String {
        return "PostfixExpression(target = $target op = ${op.op})"
    }

    override val nodeType: Type
        get() = Type.POSTFIX_EXPRESSION

    override val isEvaluatableAtCompileTime: Boolean =
        target.isEvaluatableAtCompileTime

    override fun <T> accept(astVisitor: ASTVisitor<T>): T {
        return astVisitor.visitPostfixExpression(this)
    }
}