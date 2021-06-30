package com.pretzel.core.parser

import com.pretzel.core.errors.ErrorCode
import com.pretzel.core.lexer.Lexer
import com.pretzel.core.lexer.TokenStream
import com.pretzel.core.parser.ast.Block
import com.pretzel.core.parser.ast.CodeNode
import com.pretzel.core.parser.ast.expr.BlockExpression
import com.pretzel.core.parser.ast.expr.BooleanLiteral
import com.pretzel.core.parser.ast.expr.CastExpression
import com.pretzel.core.parser.ast.expr.DecimalLiteral
import com.pretzel.core.parser.ast.expr.DictLiteral
import com.pretzel.core.parser.ast.expr.DictPair
import com.pretzel.core.parser.ast.expr.Expression
import com.pretzel.core.parser.ast.expr.FunctionCall
import com.pretzel.core.parser.ast.expr.InfixExpression
import com.pretzel.core.parser.ast.expr.IntegralLiteral
import com.pretzel.core.parser.ast.expr.LambdaExpression
import com.pretzel.core.parser.ast.expr.ListLiteral
import com.pretzel.core.parser.ast.expr.MemberAccess
import com.pretzel.core.parser.ast.expr.ParenthesizedExpression
import com.pretzel.core.parser.ast.expr.PostfixExpression
import com.pretzel.core.parser.ast.expr.PrefixExpression
import com.pretzel.core.parser.ast.expr.StringLiteral
import com.pretzel.core.parser.ast.expr.SymbolReference

class ExpressionParser(
    stream: TokenStream,
    options: ParserOptions,
) : CommonParser(stream, options), FullInputParser {
    override fun nodeTypeLegalInThisContext(type: CodeNode.Type): Boolean {
        return true
    }

    override fun parseNextNode(): Expression? {
        return parseRootExpression()
    }

    override fun parse(): Expression? {
        return parseNextNode()
    }

    private fun parseRootExpression(): Expression? {
        return if (stream.isNext(Lexer.TokenType.COLON)) {
            parseLambdaExpression()
        } else {
            parseAssignmentExpression()
        }
    }

    private val assignmentOps: Map<Lexer.TokenType, InfixExpression.Operator> =
        mapOf(
            Lexer.TokenType.ASSIGN to InfixExpression.Operator.ASSIGN,
            Lexer.TokenType.PLUSSET to InfixExpression.Operator.PLUS_ASSIGN,
            Lexer.TokenType.MINUSSET to InfixExpression.Operator.MINUS_ASSIGN,
            Lexer.TokenType.MODSET to InfixExpression.Operator.MOD_ASSIGN,
            Lexer.TokenType.MULSET to InfixExpression.Operator.MUL_ASSIGN,
            Lexer.TokenType.DIVSET to InfixExpression.Operator.DIV_ASSIGN,
            Lexer.TokenType.BANDSET to InfixExpression.Operator.BAND_ASSIGN,
            Lexer.TokenType.BORSET to InfixExpression.Operator.BOR_ASSIGN,
            Lexer.TokenType.XORSET to InfixExpression.Operator.XOR_ASSIGN,
            Lexer.TokenType.POWSET to InfixExpression.Operator.POW_ASSIGN,
            Lexer.TokenType.COALESCESET to InfixExpression.Operator.COALESCE_ASSIGN,
            Lexer.TokenType.CONCAT_COMMA_SET to InfixExpression.Operator.VARARG_ASSIGN,
            Lexer.TokenType.LSHIFTSET to InfixExpression.Operator.LSHIFT_ASSIGN,
            Lexer.TokenType.RSHIFTSET to InfixExpression.Operator.RSHIFT_ASSIGN,
            Lexer.TokenType.URSHIFTSET to InfixExpression.Operator.URSHIFT_ASSIGN,
        )

    @Suppress("RemoveExplicitTypeArguments") // false positive
    private fun parseLambdaExpression(): LambdaExpression {
        val start = rangeBegin()
        accept(Lexer.TokenType.COLON, code = ErrorCode.ASSERTED)
        val args = if (stream.isNext(Lexer.TokenType.ARROW)) {
            listOf<SymbolReference>()
        } else {
            val list = mutableListOf<SymbolReference>()
            do {
                list.add(parseSymbolPart(ErrorCode.EXPECTED_LAMBDA_PARAMETER))
            } while (stream.acceptIfNext(Lexer.TokenType.COMMA))

            list
        }

        accept(Lexer.TokenType.ARROW, code = ErrorCode.EXPECTED_LAMBDA_ARROW)
        val body = if (stream.acceptIfNext(Lexer.TokenType.LBRACE)) {
            parseBlockExpression(currentPos)
        } else {
            parseRootExpression()
        }

        return LambdaExpression(args, body, makeSpan(start, currentPos))
    }

    private fun parseAssignmentExpression(): Expression? {
        val root = parseLogicalOrExpression()
        root ?: return null

        if (stream.isNext(*assignmentOps.keys.toTypedArray())) {
            val start = rangeBegin()
            val tt = stream.next().type
            val rhs = parseAssignmentExpression()
            rhs ?: return null

            return InfixExpression(
                root,
                rhs,
                assignmentOps[tt]!!,
                makeSpan(start, currentPos)
            )
        } else {
            return root
        }
    }

    private fun parseLogicalOrExpression(): Expression? {
        var root = parseLogicalAndExpression()
        root ?: return null

        while (stream.isNext(Lexer.TokenType.OR)) {
            val start = rangeBegin()
            stream.advance()
            val rhs = parseLogicalAndExpression()
            rhs ?: return null

            root = InfixExpression(
                root!!, rhs,
                InfixExpression.Operator.OR,
                makeSpan(start, currentPos)
            )
        }

        return root
    }

    private fun parseLogicalAndExpression(): Expression? {
        var root = parseBitwiseOrExpression()
        root ?: return null

        while (stream.isNext(Lexer.TokenType.AND)) {
            val start = rangeBegin()
            stream.advance()
            val rhs = parseBitwiseOrExpression()
            rhs ?: return null

            root = InfixExpression(
                root!!, rhs,
                InfixExpression.Operator.AND,
                makeSpan(start, currentPos)
            )
        }

        return root
    }

    private fun parseBitwiseOrExpression(): Expression? {
        var root = parseBitwiseXorExpression()
        root ?: return null

        while (stream.isNext(Lexer.TokenType.BOR)) {
            val start = rangeBegin()
            stream.advance()
            val rhs = parseBitwiseXorExpression()
            rhs ?: return null

            root = InfixExpression(
                root!!, rhs,
                InfixExpression.Operator.BOR,
                makeSpan(start, currentPos)
            )
        }

        return root
    }

    private fun parseBitwiseXorExpression(): Expression? {
        var root = parseBitwiseAndExpression()
        root ?: return null

        while (stream.isNext(Lexer.TokenType.XOR)) {
            val start = rangeBegin()
            stream.advance()
            val rhs = parseBitwiseAndExpression()
            rhs ?: return null

            root = InfixExpression(
                root!!, rhs,
                InfixExpression.Operator.XOR,
                makeSpan(start, currentPos)
            )
        }

        return root
    }

    private fun parseBitwiseAndExpression(): Expression? {
        var root = parseEqualityExpression()
        root ?: return null

        while (stream.isNext(Lexer.TokenType.BAND)) {
            val start = rangeBegin()
            stream.advance()
            val rhs = parseEqualityExpression()
            rhs ?: return null

            root = InfixExpression(
                root!!, rhs,
                InfixExpression.Operator.BAND,
                makeSpan(start, currentPos)
            )
        }

        return root
    }

    private fun parseEqualityExpression(): Expression? {
        var root = parseComparisonExpression()
        root ?: return null

        while (stream.isNext(Lexer.TokenType.EQ, Lexer.TokenType.NOTEQ)) {
            val start = rangeBegin()
            val tt = stream.next().type
            val rhs = parseComparisonExpression()
            rhs ?: return null

            root = InfixExpression(
                root!!, rhs, if (tt == Lexer.TokenType.EQ) {
                    InfixExpression.Operator.EQ
                } else {
                    InfixExpression.Operator.NOTEQ
                }, makeSpan(start, currentPos)
            )
        }

        return root
    }

    private fun parseComparisonExpression(): Expression? {
        var root = parseShiftExpression()
        root ?: return null

        while (stream.isNext(
                Lexer.TokenType.GT,
                Lexer.TokenType.LT,
                Lexer.TokenType.LTEQ,
                Lexer.TokenType.GTEQ
            )
        ) {
            val start = rangeBegin()
            val tt = stream.next().type
            val rhs = parseShiftExpression()
            rhs ?: return null

            root = InfixExpression(
                root!!, rhs, when (tt) {
                    Lexer.TokenType.LT -> InfixExpression.Operator.LT
                    Lexer.TokenType.GT -> InfixExpression.Operator.GT
                    Lexer.TokenType.LTEQ -> InfixExpression.Operator.LTEQ
                    else -> InfixExpression.Operator.GTEQ
                },
                makeSpan(start, currentPos)
            )
        }

        return root
    }

    private fun parseShiftExpression(): Expression? {
        var root = parseAdditiveExpression()
        root ?: return null

        while (stream.isNext(
                Lexer.TokenType.LSHIFT,
                Lexer.TokenType.URSHIFT,
                Lexer.TokenType.RSHIFT
            )
        ) {
            val start = rangeBegin()
            val tt = stream.next().type
            val rhs = parseAdditiveExpression()
            rhs ?: return null

            root = InfixExpression(
                root!!, rhs, when (tt) {
                    Lexer.TokenType.LSHIFT -> InfixExpression.Operator.LSHIFT
                    Lexer.TokenType.RSHIFT -> InfixExpression.Operator.RSHIFT
                    else -> InfixExpression.Operator.URSHIFT
                }, makeSpan(start, currentPos)
            )
        }

        return root
    }

    private fun parseAdditiveExpression(): Expression? {
        var root = parseMultiplicativeExpression()
        root ?: return null

        while (stream.isNext(
                Lexer.TokenType.PLUS,
                Lexer.TokenType.MINUS,
                Lexer.TokenType.CONCAT_COMMA
            )
        ) {
            val start = rangeBegin()
            val tt = stream.next().type
            val rhs = parseMultiplicativeExpression()
            rhs ?: return null

            root = InfixExpression(
                root!!, rhs, when (tt) {
                    Lexer.TokenType.PLUS -> InfixExpression.Operator.PLUS
                    Lexer.TokenType.MINUS -> InfixExpression.Operator.MINUS
                    else -> InfixExpression.Operator.VARARG
                }, makeSpan(start, currentPos)
            )
        }

        return root
    }

    private fun parseMultiplicativeExpression(): Expression? {
        var root = parseCastExpression()
        root ?: return null

        while (stream.isNext(
                Lexer.TokenType.MUL,
                Lexer.TokenType.DIV,
                Lexer.TokenType.MOD
            )
        ) {
            val start = rangeBegin()
            val tt = stream.next().type
            val rhs = parseCastExpression()
            rhs ?: return null

            root = InfixExpression(
                root!!, rhs, when (tt) {
                    Lexer.TokenType.MUL -> InfixExpression.Operator.MUL
                    Lexer.TokenType.DIV -> InfixExpression.Operator.DIV
                    else -> InfixExpression.Operator.MOD
                }, makeSpan(start, currentPos)
            )
        }

        return root
    }

    private fun parseCastExpression(): Expression? {
        var root = parsePrefix()
        root ?: return null

        while (stream.isNext(Lexer.TokenType.AS, Lexer.TokenType.TO)) {
            val start = rangeBegin()
            val tt = stream.next().type
            val target = parseCastExpressionName()
            root = CastExpression(
                root!!, target,
                if (tt == Lexer.TokenType.AS) {
                    CastExpression.Operator.AS
                } else {
                    CastExpression.Operator.TO
                },
                makeSpan(start, currentPos),
            )
        }

        return root
    }

    private fun parseCastExpressionName(): Expression {

        val id =
            accept(Lexer.TokenType.IDENTIFIER, code = ErrorCode.EXPECTED_ID)
        var access: Expression? = null
        while (stream.isNext(Lexer.TokenType.DOT)) {
            val start = rangeBegin()
            stream.advance()
            val span = access?.sourceRange ?: id.span
            access = MemberAccess(
                access ?: SymbolReference(id.lexeme, id.span),
                SymbolReference(
                    accept(
                        Lexer.TokenType.IDENTIFIER,
                        code = ErrorCode.EXPECTED_ID
                    ).lexeme,
                    makeSpan(start, currentPos)
                ),
                MemberAccess.Operator.CLASSIC,
                span,
            )
        }

        return access ?: SymbolReference(id.lexeme, id.span)
    }

    private fun parsePrefix(): Expression? {
        if (stream.isNext(
                Lexer.TokenType.PLUSPLUS,
                Lexer.TokenType.MINUSMINUS,
                Lexer.TokenType.PLUS,
                Lexer.TokenType.MINUS,
                Lexer.TokenType.NOT,
                Lexer.TokenType.TILDE
            )
        ) {
            val start = rangeBegin()
            val tt = stream.next().type
            val dest = parsePrefix()
            dest ?: return null

            return PrefixExpression(
                dest, when (tt) {
                    Lexer.TokenType.PLUSPLUS -> PrefixExpression.Operator.INC
                    Lexer.TokenType.MINUSMINUS -> PrefixExpression.Operator.DEC
                    Lexer.TokenType.PLUS -> PrefixExpression.Operator.PLUS
                    Lexer.TokenType.MINUS -> PrefixExpression.Operator.MINUS
                    Lexer.TokenType.NOT -> PrefixExpression.Operator.NOT
                    else -> PrefixExpression.Operator.NEG
                }, makeSpan(start, currentPos)
            )
        } else {
            return parsePostfix()
        }
    }

    private fun parsePostfix(): Expression? {
        var root = parseMemberAccess()
        root ?: return null

        if (stream.isNext(
                Lexer.TokenType.PLUSPLUS,
                Lexer.TokenType.MINUSMINUS,
                Lexer.TokenType.NONNULL
            )
        ) {
            val start = rangeBegin()
            val tt = stream.next().type
            root = PostfixExpression(
                makeSpan(start, currentPos), root,
                when (tt) {
                    Lexer.TokenType.PLUSPLUS -> PostfixExpression.Operator.INC
                    Lexer.TokenType.MINUSMINUS -> PostfixExpression.Operator.DEC
                    else -> PostfixExpression.Operator.NOTNULL
                }
            )
        }

        return root
    }

    private fun parsePowers(): Expression? {
        var root = parseValue()
        root ?: return null

        while (stream.isNext(Lexer.TokenType.POW)) {
            val start = rangeBegin()
            val tt = stream.next().type
            val rhs = parseMemberAccess()
            rhs ?: return null
            root = InfixExpression(
                root!!,
                rhs,
                InfixExpression.Operator.POW,
                makeSpan(start, currentPos)
            )
        }

        return root
    }

    private fun parseMemberAccess(): Expression? {
        var root = parseValue()
        root ?: return null

        while (stream.isNext(
                Lexer.TokenType.DOT,
                Lexer.TokenType.CONCAT_SPACE
            )
        ) {
            val start = rangeBegin()
            val tt = stream.next().type
            root = MemberAccess(
                root!!,
                SymbolReference(
                    accept(
                        Lexer.TokenType.IDENTIFIER,
                        code = ErrorCode.EXPECTED_ID
                    ).lexeme,
                    makeSpan(start, currentPos)
                ),
                if (tt == Lexer.TokenType.DOT) {
                    MemberAccess.Operator.CLASSIC
                } else {
                    MemberAccess.Operator.CHAINING
                },
                makeSpan(root.sourceRange.start, currentPos)
            )
        }

        while (stream.isNext(Lexer.TokenType.LPAREN)) {
            val start = rangeBegin()
            stream.advance()
            root = FunctionCall(
                root!!, if (!stream.isNext(Lexer.TokenType.RPAREN)) {
                    parseArgumentList()
                } else {
                    listOf()
                }, makeSpan(start, currentPos)
            )
            accept(
                Lexer.TokenType.RPAREN,
                code = ErrorCode.EXPECTED_CLOSING_PAREN
            )
        }

        return root
    }

    private fun parseValue(): Expression? {
        val start = rangeBegin()
        val t = stream.next()

        return when (t.type) {
            Lexer.TokenType.SHORT_LITERAL,
            Lexer.TokenType.LONG_LITERAL,
            Lexer.TokenType.INTEGER_LITERAL -> {
                IntegralLiteral(t.lexeme, makeSpan(start, currentPos))
            }

            Lexer.TokenType.FLOAT_LITERAL -> {
                DecimalLiteral(t.lexeme, makeSpan(start, currentPos))
            }

            Lexer.TokenType.STRING_LITERAL, Lexer.TokenType.TEMPLATE_STRING -> {
                StringLiteral(
                    t.lexeme.toByteArray(charset = Charsets.UTF_8),
                    makeSpan(start, currentPos)
                )
            }

            Lexer.TokenType.IDENTIFIER -> {
                when (t.lexeme) {
                    "dict" -> {
                        accept(
                            Lexer.TokenType.LBRACE,
                            code = ErrorCode.EXPECTED_DICT_BRACE
                        )
                        if (stream.acceptIfNext(Lexer.TokenType.RBRACE)) {
                            DictLiteral(listOf(), makeSpan(start, currentPos))
                        } else {
                            val pairs = mutableListOf<DictPair>()
                            do {
                                val elementStart = rangeBegin()
                                val key = parseRootExpression()
                                accept(
                                    Lexer.TokenType.COLON,
                                    code = ErrorCode.EXPECTED_DICT_COLON
                                )
                                val value = parseRootExpression()
                                key ?: return null
                                value ?: return null
                                pairs.add(
                                    DictPair(
                                        key,
                                        value,
                                        makeSpan(elementStart, currentPos)
                                    )
                                )
                            } while (stream.acceptIfNext(Lexer.TokenType.COMMA))

                            stream.acceptIfNext(Lexer.TokenType.COMMA)
                            accept(Lexer.TokenType.RBRACE, code = ErrorCode.EXPECTED_BLOCK_END)
                            DictLiteral(pairs, makeSpan(start, currentPos))
                        }
                    }

                    "list" -> {
                        if (stream.acceptIfNext(Lexer.TokenType.RSQB)) {
                            ListLiteral(listOf(), makeSpan(start, currentPos))
                        } else {
                            ListLiteral(
                                parseArgumentList(),
                                makeSpan(start, currentPos)
                            )
                        }
                    }

                    else -> SymbolReference(t.lexeme, makeSpan(start, currentPos))
                }
            }

            Lexer.TokenType.LPAREN -> {
                val value = ParenthesizedExpression(
                    parseRootExpression() ?: return null,
                    makeSpan(start, currentPos)
                )

                accept(
                    Lexer.TokenType.RPAREN,
                    code = ErrorCode.EXPECTED_CLOSING_PAREN,
                    overEdge = true
                )
                value
            }

            Lexer.TokenType.YES, Lexer.TokenType.NO -> {
                BooleanLiteral(
                    t.type == Lexer.TokenType.YES,
                    makeSpan(start, currentPos)
                )
            }

            Lexer.TokenType.LSQB -> {
                if (stream.acceptIfNext(Lexer.TokenType.RSQB)) {
                    ListLiteral(listOf(), makeSpan(start, currentPos))
                } else {
                    ListLiteral(
                        parseArgumentList(),
                        makeSpan(start, currentPos)
                    ).also { accept(Lexer.TokenType.RSQB, code = ErrorCode.EXPECTED_LIST_END) }
                }
            }

            Lexer.TokenType.LBRACE -> {
                stream.position-- // get the correct range
                val lStart = rangeBegin()
                accept(Lexer.TokenType.LBRACE, code = ErrorCode.ASSERTED)
                // an empty pair of braces is a dict literal
                if (stream.acceptIfNext(Lexer.TokenType.RBRACE)) {
                    DictLiteral.empty(makeSpan(lStart, currentPos))
                } else {
                    parseBlockExpression(lStart)
                }
            }

            else -> {
                accept(Lexer.TokenType.INVALID, code = ErrorCode.EXPECTED_VALUE, explanation = "Expected a value or an expression here")
                null
            }
        }
    }

    private fun parseBlockExpression(start: Lexer.Location): BlockExpression {
        val result = mutableListOf<CodeNode?>()
        while (stream.hasNext() && stream.seek().type != Lexer.TokenType.RBRACE && stream.seek().type != Lexer.TokenType.EOF) {
            val n = lookahead.parseNextNode(stream)
            if (n == null) {
                trySync()
            }

            result.add(n)
        }

        accept(Lexer.TokenType.RBRACE, code = ErrorCode.EXPECTED_BLOCK_END, overEdge = true)
        return BlockExpression(Block(result, makeSpan(start, currentPos)))
    }

    private fun parseArgumentList(): List<Expression?> {
        val result = mutableListOf<Expression?>()
        while (true) {
            result.add(parseRootExpression())
            if (!stream.acceptIfNext(Lexer.TokenType.COMMA)) {
                break
            }
        }

        return result
    }
}