package com.pretzel.core.parser.ast

import com.pretzel.core.lexer.Lexer
import com.pretzel.core.parser.ast.expr.SymbolReference
import com.pretzel.core.parser.ast.visit.ASTVisitor

class WildcardSymbol(parts: List<SymbolReference>,
                     sourceRange: Lexer.Span
) : Symbol(parts, sourceRange) {
    override val nodeType: Type
        get() = Type.WILDCARD_SYMBOL

    override fun toString(): String {
        return "WildcardSymbol(${parts.joinToString { "$it." }}.*)"
    }

    override fun <T> accept(astVisitor: ASTVisitor<T>): T {
        return astVisitor.visitWildcardSymbol(this)
    }
}