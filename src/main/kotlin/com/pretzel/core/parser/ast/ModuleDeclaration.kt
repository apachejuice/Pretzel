package com.pretzel.core.parser.ast

import com.pretzel.core.lexer.Lexer
import com.pretzel.core.parser.ast.visit.ASTVisitor

class ModuleDeclaration(
    override val target: Symbol,
    override val sourceRange: Lexer.Span
) : CodeNode(), SymbolTargetNode {
    override val nodeType: Type
        get() = Type.MODULE_DECLARATION

    override val isVisibleInSource: Boolean
        get() = true

    override fun <T> accept(astVisitor: ASTVisitor<T>): T {
        return astVisitor.visitModuleDeclaration(this)
    }

    override fun toString(): String {
        return "ModuleDeclaration($target)"
    }

    init {
        if (target is WildcardSymbol) {
            throw IllegalArgumentException(target.javaClass.canonicalName)
        }
    }
}