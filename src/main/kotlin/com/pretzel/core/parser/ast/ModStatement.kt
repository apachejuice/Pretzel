package com.pretzel.core.parser.ast

import com.pretzel.core.lexer.Lexer
import com.pretzel.core.parser.ast.visitor.Checker
import com.pretzel.core.parser.ast.visitor.Visitor

class ModStatement(val target: String, span: Lexer.Span) : Node(span) {
    override var canBeEvaluated: Boolean = false

    override fun <T, Q> accept(visitor: Visitor<T, Q>, data: Q?): T =
        visitor.visitModStatement(this, data)

    override fun <T, Q> check(checker: Checker<T, Q>, data: Q?): T =
        checker.visitModStatement(this, data)

    override fun toString(): String {
        return "use $target;"
    }
}