package com.pretzel.core.parser.ast

import com.pretzel.core.lexer.Lexer
import com.pretzel.core.parser.ast.visit.ASTVisitor

class UseStatement(
    override val target: Symbol,
    override val sourceRange: Lexer.Span,
) : CodeNode(), SymbolTargetNode {
    override fun toString(): String {
        return "UseStatement($target)"
    }

    override val nodeType: Type
        get() = Type.USE_STATEMENT
    override val isVisibleInSource: Boolean
        get() = true

    override fun <T> accept(astVisitor: ASTVisitor<T>): T {
        return astVisitor.visitUseStatement(this)
    }

    init {
        if (target is WildcardSymbol) {
            throw IllegalArgumentException(target.javaClass.canonicalName)
        }
    }
}