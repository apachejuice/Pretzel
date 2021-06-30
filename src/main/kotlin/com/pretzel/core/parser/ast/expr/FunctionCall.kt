package com.pretzel.core.parser.ast.expr

import com.pretzel.core.lexer.Lexer
import com.pretzel.core.parser.ast.visit.ASTVisitor

class FunctionCall(
    val callable: Expression,
    val arguments: List<Expression?>,
    sourceRange: Lexer.Span
) : Expression(sourceRange) {
    override fun toString(): String {
        return "FunctionCall(callable = $callable arguments = [${arguments.joinToString { "$it, " }}])"
    }

    override val isEvaluatableAtCompileTime: Boolean = false

    override val nodeType: Type
        get() = Type.FUNCTION_CALL

    override fun <T> accept(astVisitor: ASTVisitor<T>): T {
        return astVisitor.visitFunctionCall(this)
    }
}