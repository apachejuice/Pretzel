package com.pretzel.core.parser.ast.visit

import com.pretzel.core.parser.ast.Block
import com.pretzel.core.parser.ast.CodeNode
import com.pretzel.core.parser.ast.ModuleDeclaration
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
import com.pretzel.core.parser.ast.expr.SymbolReference

/* This class is used to debug the expression parser (mostly precedence) */
class DebugASTVisitor : ASTVisitor<Double> {
    override fun visitCodeNode(codeNode: CodeNode): Double {
        return codeNode.accept(this)
    }

    override fun visitModuleDeclaration(moduleDeclaration: ModuleDeclaration): Double {
        TODO("Not yet implemented")
    }

    override fun visitSymbol(symbol: Symbol): Double {
        TODO("Not yet implemented")
    }

    override fun visitSymbolPart(symbolReference: SymbolReference): Double {
        TODO("Not yet implemented")
    }

    override fun visitUseStatement(useStatement: UseStatement): Double {
        TODO("Not yet implemented")
    }

    override fun visitVariableAssignment(variableAssignment: VariableAssignment): Double {
        TODO("Not yet implemented")
    }

    override fun visitVariableDeclaration(variableDeclaration: VariableDeclaration): Double {
        TODO("Not yet implemented")
    }

    override fun visitCallArgument(callArgument: CallArgument): Double {
        TODO("Not yet implemented")
    }

    override fun visitDecimalLiteral(decimalLiteral: DecimalLiteral): Double {
        return decimalLiteral.literal.toDouble()
    }

    override fun visitExpression(expression: Expression): Double {
        return expression.accept(this)
    }

    override fun visitFunctionCall(functionCall: FunctionCall): Double {
        TODO("Not yet implemented")
    }

    override fun visitInfixExpression(infixExpression: InfixExpression): Double {
        val left = visitExpression(infixExpression.left)
        val right = visitExpression(infixExpression.right)
        return when (infixExpression.operator) {
            InfixExpression.Operator.PLUS -> left + right
            InfixExpression.Operator.MINUS -> left - right
            InfixExpression.Operator.MUL -> left * right
            else -> TODO("Not yet implemented")
        }
    }

    override fun visitIntegralLiteral(integralLiteral: IntegralLiteral): Double {
        return integralLiteral.literal.toInt().toDouble()
    }

    override fun visitPostfixExpression(postfixExpression: PostfixExpression): Double {
        TODO("Not yet implemented")
    }

    override fun visitPrefixExpression(prefixExpression: PrefixExpression): Double {
        TODO("Not yet implemented")
    }

    override fun visitStringLiteral(stringLiteral: StringLiteral): Double {
        TODO("Not yet implemented")
    }

    override fun visitWildcardSymbol(wildcardSymbol: WildcardSymbol): Double {
        TODO("Not yet implemented")
    }

    override fun visitRootNode(rootNode: CodeNode.RootNode): Double {
        var result = 0.0
        rootNode.childNodes.forEach { result += it.accept(this) }
        return result
    }

    override fun visitSymbolReference(symbolReference: SymbolReference): Double {
        TODO("Not yet implemented")
    }

    override fun visitCastExpression(castExpression: CastExpression): Double {
        TODO("Not yet implemented")
    }

    override fun visitParenthesizedExpression(parenthesizedExpression: ParenthesizedExpression): Double {
        return visitExpression(parenthesizedExpression.inner)
    }

    override fun visitBlock(block: Block): Double {
        TODO("Not yet implemented")
    }
}