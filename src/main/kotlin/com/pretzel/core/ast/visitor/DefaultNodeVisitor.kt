/*
 * Copyright 2021 apachejuice
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     <http://www.apache.org/licenses/LICENSE-2.0>
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pretzel.core.ast.visitor

import com.pretzel.core.ast.Argument
import com.pretzel.core.ast.BinaryExpression
import com.pretzel.core.ast.Block
import com.pretzel.core.ast.EmptyStatement
import com.pretzel.core.ast.Expression
import com.pretzel.core.ast.FunctionCall
import com.pretzel.core.ast.FunctionDeclaration
import com.pretzel.core.ast.IfStatement
import com.pretzel.core.ast.InputArgument
import com.pretzel.core.ast.Literal
import com.pretzel.core.ast.MemberAccess
import com.pretzel.core.ast.ModStmt
import com.pretzel.core.ast.Node
import com.pretzel.core.ast.ObjectCreation
import com.pretzel.core.ast.ParsenthesizedExpression
import com.pretzel.core.ast.ReturnStatement
import com.pretzel.core.ast.UnaryExpression
import com.pretzel.core.ast.UseStmt
import com.pretzel.core.ast.VariableAssignment
import com.pretzel.core.ast.VariableCreation
import com.pretzel.core.ast.VariableReference

class DefaultNodeVisitor : NodeVisitor<String> {
    override fun visitUseStmt(useStmt: UseStmt): String {
        val result = StringBuilder("use ")
        result.append(useStmt.target)
        if (useStmt.isWildcard) result.append(":*")
        return result.toString()
    }

    override fun visitExpression(expression: Expression): String {
        return expression.accept(this)
    }

    override fun visitBinaryExpression(binaryExpression: BinaryExpression): String {
        return ""//"${visitExpression(binaryExpression.left)} ${binaryExpression.operator.operator} ${visitExpression(binaryExpression.right)}"
    }

    override fun visitUnaryExpression(unaryExpression: UnaryExpression): String {
        return ""//"${unaryExpression.operator.operator}${visitExpression(unaryExpression.target)}"
    }

    override fun visitLiteral(literal: Literal): String {
        return literal.toString()
    }

    override fun visitFunctionCall(functionCall: FunctionCall): String {
        val args = if (functionCall.args.isEmpty()) ""
        else functionCall.args.joinToString(separator = ", ") { visitArgument(it) }
        return "${functionCall.name} ($args)"
    }

    override fun visitModStmt(modStmt: ModStmt): String {
        val result = StringBuilder("mod ")
        result.append(modStmt.target)
        return result.toString()
    }

    override fun visitObjectCreation(objectCreation: ObjectCreation): String {
        val result = StringBuilder("new ")
        val args = if (objectCreation.args.isEmpty()) ""
        else objectCreation.args.joinToString(separator = ", ") { visitArgument(it) }
        result.append("${objectCreation.name} ($args)")
        return result.toString()
    }

    override fun visitArgument(argument: Argument): String {
        return argument.toString()
    }

    override fun visiParenthesizedExpression(parsenthesizedExpression: ParsenthesizedExpression): String {
        return "(${visitExpression(parsenthesizedExpression.expression)})"
    }

    override fun visitVariableCreation(variableCreation: VariableCreation): String {
        return "$${variableCreation.name}" + if (variableCreation.initializer != null)
            " = ${visitExpression(variableCreation.initializer)}" else ""
    }

    override fun visitVariableAssignment(variableAssignment: VariableAssignment): String {
        return "$variableAssignment.name = ${visitExpression(variableAssignment.newValue)}"
    }

    override fun visitMemberAccess(memberAccess: MemberAccess): String {
        return "${visitExpression(memberAccess.from)}.${visitExpression(memberAccess.accessor)}"
    }

    override fun visitVariableReference(variableReference: VariableReference): String {
        return variableReference.toString()
    }

    override fun visitNode(node: Node): String {
        return node.accept(this)
    }

    override fun visitIfStatement(ifStatement: IfStatement): String {
        return ifStatement.toString()
    }

    override fun visitBlock(block: Block): String {
        return "{${block.nodes.joinToString(separator = "\n") { visitNode(it) }}}"
    }

    override fun visitEmptyStatement(emptyStatement: EmptyStatement): String {
        return "/* empty */"
    }

    override fun visitFunctionDeclaration(functionDeclaration: FunctionDeclaration): String {
        TODO("Not yet implemented")
    }

    override fun visitInputArgument(inputArgument: InputArgument): String {
        TODO("Not yet implemented")
    }

    override fun visitReturnStatement(returnStatement: ReturnStatement): String {
        TODO("Not yet implemented")
    }
}