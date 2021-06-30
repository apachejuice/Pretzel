package com.pretzel.core.parser.ast.visit

import com.pretzel.core.parser.ast.Block
import com.pretzel.core.parser.ast.CodeNode
import com.pretzel.core.parser.ast.ModuleDeclaration
import com.pretzel.core.parser.ast.expr.SymbolReference
import com.pretzel.core.parser.ast.Symbol
import com.pretzel.core.parser.ast.UseStatement
import com.pretzel.core.parser.ast.VariableAssignment
import com.pretzel.core.parser.ast.VariableDeclaration
import com.pretzel.core.parser.ast.WildcardSymbol
import com.pretzel.core.parser.ast.expr.CallArgument
import com.pretzel.core.parser.ast.expr.CastExpression
import com.pretzel.core.parser.ast.expr.DecimalLiteral
import com.pretzel.core.parser.ast.expr.Expression
import com.pretzel.core.parser.ast.expr.FunctionCall
import com.pretzel.core.parser.ast.expr.InfixExpression
import com.pretzel.core.parser.ast.expr.IntegralLiteral
import com.pretzel.core.parser.ast.expr.ParenthesizedExpression
import com.pretzel.core.parser.ast.expr.PostfixExpression
import com.pretzel.core.parser.ast.expr.PrefixExpression
import com.pretzel.core.parser.ast.expr.StringLiteral

interface ASTVisitor<T> {
    fun visitCodeNode(codeNode: CodeNode): T
    fun visitModuleDeclaration(moduleDeclaration: ModuleDeclaration): T
    fun visitSymbol(symbol: Symbol): T
    fun visitSymbolPart(symbolReference: SymbolReference): T
    fun visitUseStatement(useStatement: UseStatement): T
    fun visitVariableAssignment(variableAssignment: VariableAssignment): T
    fun visitVariableDeclaration(variableDeclaration: VariableDeclaration): T
    fun visitCallArgument(callArgument: CallArgument): T
    fun visitDecimalLiteral(decimalLiteral: DecimalLiteral): T
    fun visitExpression(expression: Expression): T
    fun visitFunctionCall(functionCall: FunctionCall): T
    fun visitInfixExpression(infixExpression: InfixExpression): T
    fun visitIntegralLiteral(integralLiteral: IntegralLiteral): T
    fun visitPostfixExpression(postfixExpression: PostfixExpression): T
    fun visitPrefixExpression(prefixExpression: PrefixExpression): T
    fun visitStringLiteral(stringLiteral: StringLiteral): T
    fun visitWildcardSymbol(wildcardSymbol: WildcardSymbol): T
    fun visitRootNode(rootNode: CodeNode.RootNode): T
    fun visitSymbolReference(symbolReference: SymbolReference): T
    fun visitCastExpression(castExpression: CastExpression): T
    fun visitParenthesizedExpression(parenthesizedExpression: ParenthesizedExpression): T
    fun visitBlock(block: Block): T
}