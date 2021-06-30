package com.pretzel.core.parser.ast

import com.pretzel.core.lexer.Lexer
import com.pretzel.core.parser.ast.expr.SymbolReference
import com.pretzel.core.parser.ast.visit.ASTVisitor

open class Symbol(val parts: List<SymbolReference>, override val sourceRange: Lexer.Span) :
    CodeNode() {
    override val nodeType: Type
        get() = Type.SYMBOL

    override val isVisibleInSource: Boolean
        get() = true

    override fun <T> accept(astVisitor: ASTVisitor<T>): T {
        return astVisitor.visitSymbol(this)
    }

    override fun toString(): String {
        return "Symbol(${parts.joinToString { "$it" }})"
    }
}