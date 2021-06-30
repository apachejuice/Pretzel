package com.pretzel.core.parser.ast.expr

import com.pretzel.core.lexer.Lexer
import com.pretzel.core.parser.ast.CodeNode
import com.pretzel.core.parser.ast.Symbol
import com.pretzel.core.parser.ast.visit.ASTVisitor
import com.pretzel.core.toList

class SymbolReference(val name: String, sourceRange: Lexer.Span) :
    Expression(sourceRange) {
    override val nodeType: Type
        get() = Type.SYMBOL_PART

    override val isVisibleInSource: Boolean
        get() = true

    override val isEvaluatableAtCompileTime: Boolean
        get() = true

    override fun <T> accept(astVisitor: ASTVisitor<T>): T {
        return astVisitor.visitSymbolReference(this)
    }

    override fun toString(): String {
        return "SymbolReference($name)"
    }

    fun toSymbol(): Symbol {
        return Symbol(SymbolReference(name, sourceRange).toList(), sourceRange)
    }
}