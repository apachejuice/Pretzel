package com.pretzel.core.parser.ast.expr

import com.pretzel.core.lexer.Lexer
import com.pretzel.core.parser.ast.visit.ASTVisitor

class CallArgument(
    val value: Expression,
    sourceRange: Lexer.Span,
    val name: SymbolReference? = null,
) : Expression(sourceRange) {
    override fun toString(): String {
        return "CallArgument(name = \"$name\" value = \"$value\")"
    }

    override val isEvaluatableAtCompileTime: Boolean =
        value.isEvaluatableAtCompileTime

    override val nodeType: Type
        get() = Type.CALL_ARGUMENT

    override fun <T> accept(astVisitor: ASTVisitor<T>): T {
        return astVisitor.visitCallArgument(this)
    }
}