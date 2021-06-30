package com.pretzel.core.parser.ast.expr

import com.pretzel.core.lexer.Lexer
import com.pretzel.core.parser.ast.visit.ASTVisitor

class StringLiteral(val data: ByteArray, sourceRange: Lexer.Span) :
    Expression(sourceRange) {
    override val isEvaluatableAtCompileTime: Boolean
        get() = true

    override fun toString(): String {
        return "StringLiteral(\"${String(data, charset = Charsets.UTF_8)}\")"
    }

    override val nodeType: Type
        get() = Type.STRING_LITERAL

    override fun <T> accept(astVisitor: ASTVisitor<T>): T {
        return astVisitor.visitStringLiteral(this)
    }
}