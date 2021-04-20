package com.pretzel.core.parser.ast

import com.pretzel.core.lexer.Lexer
import com.pretzel.core.parser.ast.visitor.Checker
import com.pretzel.core.parser.ast.visitor.Visitor

class UseStatement(val target: String, span: Lexer.Span) : Node(span) {
    val isWildcard: Boolean = target.endsWith("*")
    override var canBeEvaluated: Boolean = false

    override fun <T, Q> accept(visitor: Visitor<T, Q>, data: Q?): T =
        visitor.visitUseStatement(this, data)

    override fun <T, Q> check(checker: Checker<T, Q>, data: Q?): T =
        checker.visitUseStatement(this, data)

    override fun toString(): String {
        return "use $target;"
    }
}