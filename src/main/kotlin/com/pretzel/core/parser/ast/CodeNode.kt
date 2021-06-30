package com.pretzel.core.parser.ast

import com.pretzel.core.lexer.Lexer
import com.pretzel.core.parser.ast.visit.ASTVisitor

abstract class CodeNode {
    enum class Type {
        GENERATED,
        MODULE_DECLARATION,
        SYMBOL_PART,
        SYMBOL,
        ROOT,
        USE_STATEMENT,
        WILDCARD_SYMBOL,
        VARIABLE_ASSIGNMENT,
        INFIX_EXPRESSION,
        PREFIX_EXPRESSION,
        POSTFIX_EXPRESSION,
        FUNCTION_CALL,
        CALL_ARGUMENT,
        STRING_LITERAL,
        INTEGRAL_LITERAL,
        DECIMAL_LITERAL,
        PARENTHESIZED_EXPRESSION,
        MEMBER_ACCESS,
        CAST,
        BOOLEAN_LITERAL,
        LOCAL_VARIABLE_DECLARATION,
        BLOCK,
        BLOCK_EXPR,
        LIST_LITERAL,
        DICT_LITERAL,
        DICT_PAIR,
        LAMBDA,
        BASE,
    }

    open fun <T> accept(astVisitor: ASTVisitor<T>): T {
        return astVisitor.visitCodeNode(this)
    }

    abstract override fun toString(): String

    open val nodeType: Type
        get() = Type.BASE

    val type: Type
        get() = if (isVisibleInSource) nodeType else Type.GENERATED

    abstract val isVisibleInSource: Boolean
    abstract val sourceRange: Lexer.Span?

    class RootNode(val childNodes: List<CodeNode>, override val sourceRange: Lexer.Span) :
        CodeNode() {
        override fun toString(): String {
            return childNodes.joinToString { "$it\n" }
        }

        override val nodeType: Type
            get() = Type.ROOT

        override val isVisibleInSource: Boolean
            get() = true

        override fun <T> accept(astVisitor: ASTVisitor<T>): T {
            return astVisitor.visitRootNode(this)
        }
    }
}