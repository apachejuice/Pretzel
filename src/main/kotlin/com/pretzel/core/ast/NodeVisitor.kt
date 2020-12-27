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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pretzel.core.ast

interface NodeVisitor<T> {
    fun visitRoot(node: Node): T
    fun visitUseStmt(useStmt: UseStmt): T
    fun visitExpression(expression: Expression): T
    fun visitBinaryExpression(binaryExpression: BinaryExpression): T
    fun visitUnaryExpression(unaryExpression: UnaryExpression): T
    fun visitLiteral(literal: Literal): T
    fun visitFunctionCall(functionCall: FunctionCall): T
    fun visitModStmt(modStmt: ModStmt): T
    fun visitObjectCreation(objectCreation: ObjectCreation): T
    fun visitArgument(argument: Argument): T
    fun visiParenthesizedExpression(parsenthesizedExpression: ParsenthesizedExpression): T
    fun visitVariableCreation(variableCreation: VariableCreation): T
    fun visitVariableAssignment(variableAssignment: VariableAssignment): T
    fun visitMemberAccess(memberAccess: MemberAccess): T
    fun visitVariableReference(variableReference: VariableReference): T
}