package com.pretzel.core.parser.ast.expr

import com.pretzel.core.lexer.Lexer
import com.pretzel.core.parser.ast.CodeNode
import com.pretzel.core.parser.ast.visit.ASTVisitor

abstract class Expression(override val sourceRange: Lexer.Span) : CodeNode() {
    override val isVisibleInSource: Boolean = true
    abstract val isEvaluatableAtCompileTime: Boolean

    override fun <T> accept(astVisitor: ASTVisitor<T>): T {
        return astVisitor.visitExpression(this)
    }
}