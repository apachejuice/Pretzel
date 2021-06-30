package com.pretzel.core.parser.ast.expr

import com.pretzel.core.lexer.Lexer
import com.pretzel.core.parser.ast.visit.ASTVisitor

class InfixExpression(
    val left: Expression,
    val right: Expression,
    val operator: Operator,
    sourceRange: Lexer.Span
) : Expression(sourceRange) {
    enum class Operator(val op: String) {
        PLUS("+"),
        MINUS("-"),
        MOD("%"),
        MUL("*"),
        DIV("/"),
        AND("&&"),
        BAND("&"),
        OR("||"),
        BOR("|"),
        XOR("^"),
        POW("**"),
        COALESCE("??"),
        VARARG("..."),
        LSHIFT("<<"),
        RSHIFT(">>"),
        URSHIFT(">>>"),
        LT(">"),
        GT("<"),
        LTEQ(">="),
        GTEQ("<="),
        EQ("=="),
        NOTEQ("!="),

        // Assignment
        ASSIGN("="),
        PLUS_ASSIGN("+="),
        MINUS_ASSIGN("-="),
        MOD_ASSIGN("%="),
        MUL_ASSIGN("*="),
        DIV_ASSIGN("/="),
        BAND_ASSIGN("&="),
        BOR_ASSIGN("|="),
        XOR_ASSIGN("^="),
        POW_ASSIGN("**="),
        COALESCE_ASSIGN("??="),
        VARARG_ASSIGN("...="),
        LSHIFT_ASSIGN("<<="),
        RSHIFT_ASSIGN(">>="),
        URSHIFT_ASSIGN(">>>=");

        fun isAssignmentOperator(): Boolean = this.name.endsWith("_ASSIGN")
    }

    override fun toString(): String {
        return "InfixExpression(left = $left op = ${operator.op} right = $right)"
    }

    override val nodeType: Type
        get() = Type.INFIX_EXPRESSION

    override val isEvaluatableAtCompileTime: Boolean =
        left.isEvaluatableAtCompileTime && right.isEvaluatableAtCompileTime

    override fun <T> accept(astVisitor: ASTVisitor<T>): T {
        return astVisitor.visitInfixExpression(this)
    }
}