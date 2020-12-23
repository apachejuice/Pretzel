package com.pretzel.core.ast

interface NodeVisitor<T> {
    fun visitRoot(node: Node): T
    fun visitUseStmt(useStmt: UseStmt): T
    fun visitExpression(expression: Expression): T
    fun visitBinaryExpression(binaryExpression: BinaryExpression): T
    fun visitUnaryExpression(unaryExpression: UnaryExpression): T
    fun visitLiteral(literal: Literal): T
    fun visitFunctionCall(functionCall: FunctionCall): T
}