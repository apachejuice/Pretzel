/*
 * Copyright 2020 Valio Valtokari
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     <http://www.apache.org/licenses/LICENSE-2.0>
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF Unit KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pretzel.core.ast.interpreter

import com.pretzel.core.ErrorType
import com.pretzel.core.Report
import com.pretzel.core.ast.Argument
import com.pretzel.core.ast.BinaryExpression
import com.pretzel.core.ast.Block
import com.pretzel.core.ast.EmptyStatement
import com.pretzel.core.ast.Expression
import com.pretzel.core.ast.FunctionCall
import com.pretzel.core.ast.IfStatement
import com.pretzel.core.ast.Literal
import com.pretzel.core.ast.MemberAccess
import com.pretzel.core.ast.ModStmt
import com.pretzel.core.ast.Node
import com.pretzel.core.ast.ObjectCreation
import com.pretzel.core.ast.ParsenthesizedExpression
import com.pretzel.core.ast.UnaryExpression
import com.pretzel.core.ast.UseStmt
import com.pretzel.core.ast.VariableAssignment
import com.pretzel.core.ast.VariableCreation
import com.pretzel.core.ast.VariableReference
import com.pretzel.core.ast.visitor.NodeVisitor
import com.pretzel.core.lexer.Lexer

class Interpreter : NodeVisitor<Unit> {
    private var mod: ModStmt? = null
    private val imports: MutableList<UseStmt> = mutableListOf()
    
    override fun visitUseStmt(useStmt: UseStmt) {
        for (i in imports) {
            if (useStmt.target == i.target) {
                warning("double import for '${i.target}'", useStmt.end)
                return
            }
        }

        imports.add(useStmt)
    }

    override fun visitExpression(expression: Expression) {
        expression.accept(this)
    }

    override fun visitBinaryExpression(binaryExpression: BinaryExpression) {
        println("binary: $binaryExpression")
    }

    override fun visitUnaryExpression(unaryExpression: UnaryExpression) {
        println("unary: $unaryExpression")
    }

    override fun visitLiteral(literal: Literal) {
        println("Literal: $literal")
    }

    override fun visitFunctionCall(functionCall: FunctionCall) {
        println("function call $functionCall")
    }

    override fun visitModStmt(modStmt: ModStmt) {
        if (mod != null)
            Report.warning(ErrorType.AST, "only one module statement allowed per file", modStmt.end)
        else mod = modStmt
    }

    override fun visitObjectCreation(objectCreation: ObjectCreation) {
        println("instantiation: $objectCreation")
    }

    override fun visitArgument(argument: Argument) {
        println("argument: $argument")
    }

    override fun visiParenthesizedExpression(parsenthesizedExpression: ParsenthesizedExpression) {
        println("parens: $parsenthesizedExpression")
    }

    override fun visitVariableCreation(variableCreation: VariableCreation) {
        println("variable: $variableCreation")
    }

    override fun visitVariableAssignment(variableAssignment: VariableAssignment) {
        println("assignment: $variableAssignment")
    }

    override fun visitMemberAccess(memberAccess: MemberAccess) {
        println("member access: $memberAccess")
    }

    override fun visitVariableReference(variableReference: VariableReference) {
        println("variable reference: $variableReference")
    }

    override fun visitNode(node: Node) {
        node.accept(this)
    }

    override fun visitIfStatement(ifStatement: IfStatement) {
        println("if: $ifStatement")
    }

    override fun visitBlock(block: Block) {
        println("block:")
        for (i in block) i.accept(this)
    }

    override fun visitEmptyStatement(emptyStatement: EmptyStatement) {
        println("empty: {}")
    }

    private fun warning(message: String, context: Lexer.Context) {
        Report.warning(ErrorType.AST, message, context)
    }
}