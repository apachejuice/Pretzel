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

package com.pretzel.core.parser


import com.pretzel.core.ast.Argument
import com.pretzel.core.ast.BinaryExpression
import com.pretzel.core.ast.Block
import com.pretzel.core.ast.Expression
import com.pretzel.core.ast.FunctionCall
import com.pretzel.core.ast.IfStatement
import com.pretzel.core.ast.Literal
import com.pretzel.core.ast.MemberAccess
import com.pretzel.core.ast.ModStmt
import com.pretzel.core.ast.Node
import com.pretzel.core.ast.ObjectCreation
import com.pretzel.core.ast.ParsenthesizedExpression
import com.pretzel.core.ast.Precedence
import com.pretzel.core.ast.UnaryExpression
import com.pretzel.core.ast.UseStmt
import com.pretzel.core.ast.VariableAssignment
import com.pretzel.core.ast.VariableCreation
import com.pretzel.core.ast.VariableReference
import com.pretzel.core.lexer.Lexer
import com.pretzel.core.lexer.Lexer.Context
import com.pretzel.core.lexer.Lexer.SourceMode
import com.pretzel.core.lexer.Lexer.Token
import com.pretzel.core.lexer.Lexer.TokenType
import com.pretzel.core.lexer.TokenStream

import java.util.Stack
import java.util.StringJoiner


class Parser private constructor(val stream: TokenStream) {
    open class ParsingException(val last: Token, val nodes: MutableList<Node>, val overEOF: Boolean = false) :
        RuntimeException()

    private var hadError: Boolean = false
    private val contextStack: Stack<Context> = Stack()
    private val nodeCache: Stack<Node> = Stack()
    private val nodes = mutableListOf<Node>()

    var cancellationHook: (message: String, t: Token, nodes: List<Node>) -> Unit =
        { _: String, _: Token, _: List<Node> -> }
    val lexer: Lexer = stream.lexer
    val sourceName: String = stream.lexer.filename

    private fun cancel(message: String = "") {
        cancellationHook.invoke(message, stream.seek(), nodes)
        throw ParsingException(stream.seek(), nodes)
    }

    private val context: Context
        get() = stream.seek().toContext()

    private fun initContexts(pushFirst: Boolean = false) {
        // Even if finishContexts() hasn't been called, clean up
        contextStack.clear()
        if (pushFirst) pushContext()
    }

    private fun pushContext() {
        contextStack.push(context)
    }

    private fun getStart(): Context = contextStack[contextStack.indices.first]
    private fun getEnd(): Context = contextStack[contextStack.indices.last]

    private fun finishContexts(): Stack<Context> {
        val result = contextStack
        contextStack.clear()
        return result
    }

    private fun acceptToken(vararg tt: TokenType, missing: Boolean = false): Token {
        return if (stream.seek().type in tt) {
            stream.seek().also { stream.advance() }
        } else {
            cancel(
                "expected one of '${tt.joinToString(separator = " ,") { "$it" }}', got '${stream.seek().type}'",
            )
            if (stream.hasNext()) stream.next() else stream.seek()
        }
    }

    private fun checkAndPushNode() {
        if (nodeCache.isEmpty())
            return

        nodes.addAll(nodeCache)
        nodeCache.clear()
    }

    private fun pushNodeUnlessError(node: Node) {
        // Make sure we dont operate on a broken stack
        if (!hadError)
            nodeCache.push(node).also {
                finishContexts()
                hadError = false
            }
    }

    // region PARSING METHODS
    // The base parsing method; the root of the tree.
    fun parse(): Node? {
        while (stream.hasNext() && !hadError) {
            val node = when (stream.seek().type) {
                TokenType.USE -> parseUseStmt()
                TokenType.MOD -> parseModuleStmt()
                TokenType.VAR -> parseVariable()
                TokenType.IDENTIFIER -> parseAssignment()
                TokenType.IF -> parseIfStatement()
                else -> parseExpression(stream)
            }

            pushNodeUnlessError(node)
            stream.acceptIfNext(TokenType.EOF)
            checkAndPushNode()
        }

        nodes.forEach { println("[$it]      (${it.javaClass.name})") }
        return if (nodes.isEmpty())
            null
        else
            Node.getRootInstance(nodes)
    }

    private fun parseIfStatement(): IfStatement {
        initContexts(pushFirst = true)
        acceptToken(TokenType.IF)
        pushContext()
        val condition = parseExpression(stream)
        acceptToken(TokenType.LBRACE)
        val body = ifBlock()
        val elsebody: Block = if (stream.acceptIfNext(TokenType.ELSE)) { acceptToken(TokenType.LBRACE); ifBlock() } else Block.empty(getEnd())

        return IfStatement(
            condition,
            body,
            elsebody,
        )
    }

    private fun ifBlock(): Block {
        var count = 0
        val nodes = mutableListOf<Node>()

        while (true) {
            when (stream.seek().type) {
                TokenType.RBRACE -> {
                    if (count == 0) return Block.empty(getStart())
                    else break
                }

                TokenType.VAR -> nodes.add(parseVariable())
                TokenType.IDENTIFIER -> nodes.add(parseAssignment())
                TokenType.IF -> nodes.add(parseIfStatement())
                else -> nodes.add(parseExpression(stream))
            }

            count++
        }

        stream.advance()
        return Block(nodes)
    }

    private fun parseAssignment(): Node {
        initContexts(pushFirst = true)
        val name = acceptToken(TokenType.IDENTIFIER)
        val isOp = stream.isNext(*ASSIGNMENT_OP)

        if (!isOp) {
            stream.position--
            return parseExpression(stream)
        } else {
            val op = stream.next().type
            if (op == TokenType.ASSIGN) {
                pushContext()
                val result = VariableAssignment(name.lexeme!!, parseExpression(stream), getStart(), getEnd())
                finishContexts()
                return result
            } else {
                val precedence: Precedence
                val biop = when (op) {
                    TokenType.PLUSSET -> {
                        precedence = Precedence.PRETTY_LOW
                        BinaryOperator.PLUS
                    }

                    TokenType.MINUSSET -> {
                        precedence = Precedence.PRETTY_LOW
                        BinaryOperator.MINUS
                    }

                    TokenType.MULSET -> {
                        precedence = Precedence.QUITE_HIGH
                        BinaryOperator.MUL
                    }

                    TokenType.DIVSET -> {
                        precedence = Precedence.QUITE_HIGH
                        BinaryOperator.DIV
                    }

                    TokenType.MODSET -> {
                        precedence = Precedence.QUITE_HIGH
                        BinaryOperator.MOD
                    }

                    TokenType.BANDSET -> {
                        precedence = Precedence.LITTLE_LOW
                        BinaryOperator.BAND
                    }

                    TokenType.XORSET -> {
                        precedence = Precedence.PRETTY_LOW
                        BinaryOperator.XOR
                    }

                    TokenType.POWSET -> {
                        precedence = Precedence.VERY_HIGH
                        BinaryOperator.POW
                    }

                    TokenType.BORSET -> {
                        precedence = Precedence.QUITE_LOW
                        BinaryOperator.BOR
                    }

                    else -> {
                        cancel("not reached")
                        precedence = null!!
                        BinaryOperator.UNKNOWN
                    }
                }

                pushContext()
                return VariableAssignment(
                        name.lexeme!!,
                        BinaryExpression(
                            VariableReference(name),
                            parseExpression(stream),
                            biop,
                            precedence
                        ),
                        getStart(),
                        getEnd(),
                    ).also { finishContexts() }
            }
        }
    }

    private fun parseVariable(): Node {
        initContexts(pushFirst = true)
        acceptToken(TokenType.VAR)
        val ntok = acceptToken(TokenType.IDENTIFIER)
        val name = ntok.lexeme!!

        // including other assignment operators
        // for more specific error messages
        val assign = stream.isNext(*ASSIGNMENT_OP)
        val result: Node

        if (!assign) {
            pushContext()
            result = VariableCreation(name, null, getStart(), getEnd())
            finishContexts()
            return result
        }

        val tok = stream.seek()
        val op = tok.type
        val lit = tok.lexeme!!

        if (op != TokenType.ASSIGN)
            cancel("cannot use expression-assignment operator '$lit' when creating new variables")

        pushContext()
        stream.advance()
        result = VariableCreation(name, parseExpression(stream), getStart(), getEnd())
        finishContexts()
        return result
    }

    //region STATEMENT PARSING

    private fun parseModulePath(): Pair<String, Boolean> {
        val result = StringJoiner(":")
        var wildcard = false
        initContexts(pushFirst = true)
        result.add(acceptToken(TokenType.IDENTIFIER).lexeme)

        if (stream.acceptIfNext(TokenType.COLON)) {
            while (true) {
                if (stream.isNext(TokenType.MUL)) {
                    wildcard = true
                    break
                }

                result.add(acceptToken(TokenType.IDENTIFIER).lexeme)
                if (!stream.isNext(TokenType.COLON)) break
                else acceptToken(TokenType.COLON)
            }
        } else if (stream.isNext(TokenType.MUL)) wildcard = true

        return result.toString() to wildcard
    }

    private fun parseUseStmt(): UseStmt {
        acceptToken(TokenType.USE)
        val result = parseModulePath()

        pushContext()
        return UseStmt(
                result.first.split(":"),
                getStart(),
                getEnd(),
                result.second,
            )
    }

    private fun parseModuleStmt(): ModStmt {
        acceptToken(TokenType.MOD)
        val result = parseModulePath()
        if (result.second)
            cancel("mod statement cannot have wildcards")

        pushContext()
        return ModStmt(
                result.first.split(":"),
                getStart(),
                getEnd(),
        )
    }

    enum class BinaryOperator(val operator: String) {
        AND("&&"),
        BAND("&"),
        BOR("|"),
        SIGNEDL("<<"),
        SIGNEDR(">>"),
        UNSIGNEDR(">>>"),
        XOR("^"),
        DIV("/"),
        EQ("=="),
        GT(">"),
        GTEQ(">="),
        MINUS("-"),
        MOD("%"),
        MUL("*"),
        NOTEQ("!="),
        OR("||"),
        PLUS("+"),
        POW("^^"),
        LT("<"),
        LTEQ("<="),
        STRCOMMA("..."),
        STRSPACE(".."),
        UNKNOWN(""),
    }

    enum class UnaryOperator(val operator: String) {
        NEG("~"),
        NOT("!"),
        PLUS("+"),
        MINUS("-");
    }

    private fun parseExpression(stream: TokenStream): Expression {
        return object : Any() {
            // Grammar:
            // expression = term | expression `+` term | expression `-` term
            // term = factor | term `*` factor | term `/` factor
            // factor = `+` factor | `-` factor | `(` expression `)`
            //        | number | functionName factor | factor `^` factor
            fun parseExpression(stream: TokenStream = this@Parser.stream): Expression {
                var e = parseTerm(stream)
                var op: BinaryOperator

                while (true) {
                    if (stream.acceptIfNext(TokenType.PLUS, TokenType.MINUS)) {
                        op = if (stream.seek().type == TokenType.PLUS) BinaryOperator.PLUS else BinaryOperator.MINUS
                        e = BinaryExpression(
                            e,
                            parseTerm(stream),
                            op,
                            Precedence.PRETTY_HIGH
                        )
                    } else if (stream.isNext(TokenType.DOT)) {
                        return parseMemberAccess(e, stream)
                    } else return e
                }
            }

            private fun parseTerm(stream: TokenStream): Expression {
                var e = parseFactor(stream)
                var op: BinaryOperator
                var p: Precedence

                while (true) {
                    when (val t = stream.seek().type) {
                        TokenType.DOT -> {
                            stream.advance()
                            return parseMemberAccess(e, stream)
                        }

                        TokenType.MINUS, TokenType.PLUS -> {
                            op = BinaryOperator.valueOf(t.name)
                            p = Precedence.PRETTY_HIGH
                        }

                        TokenType.DIV, TokenType.MUL, TokenType.MOD -> {
                            op = BinaryOperator.valueOf(t.name)
                            p = Precedence.QUITE_HIGH
                        }

                        TokenType.LSHIFT, TokenType.RSHIFT, TokenType.URSHIFT -> {
                            op = when (t) {
                                TokenType.LSHIFT -> BinaryOperator.SIGNEDL
                                TokenType.RSHIFT -> BinaryOperator.SIGNEDR
                                TokenType.URSHIFT -> BinaryOperator.UNSIGNEDR

                                // will be never reached but when expressions
                                // must be exhaustive
                                else -> {
                                    cancel("unexpected token ${stream.seek().type}")
                                    BinaryOperator.UNKNOWN
                                }
                            }

                            p = Precedence.LITTLE_HIGH
                        }

                        TokenType.OR -> {
                            op = BinaryOperator.OR
                            p = Precedence.SUPER_LOW
                        }

                        TokenType.AND -> {
                            op = BinaryOperator.AND
                            p = Precedence.VERY_LOW
                        }

                        TokenType.BOR -> {
                            op = BinaryOperator.BOR
                            p = Precedence.QUITE_LOW
                        }

                        TokenType.XOR -> {
                            op = BinaryOperator.XOR
                            p = Precedence.PRETTY_LOW
                        }

                        TokenType.BAND -> {
                            op = BinaryOperator.BAND
                            p = Precedence.LITTLE_LOW
                        }

                        TokenType.EQ,
                        TokenType.NOTEQ,
                        TokenType.GT,
                        TokenType.GTEQ,
                        TokenType.LT,
                        TokenType.LTEQ -> {
                            op = BinaryOperator.valueOf(t.name)
                            p = Precedence.MEDIUM
                        }

                        else -> return e
                    }

                    stream.advance()
                    e = BinaryExpression(
                        e,
                        parseFactor(stream),
                        op,
                        p,
                    )
                }
            }

            private fun parseMemberAccess(base: Expression, stream: TokenStream): MemberAccess {
                var access: MemberAccess? = null
                var firstRound = true

                do {
                    val e: Expression = if (stream.isNext(TokenType.IDENTIFIER)) {
                        val t = acceptToken(TokenType.IDENTIFIER)
                        if (stream.isNext(TokenType.LPAREN)) {
                            val args = mutableListOf<Argument>()
                            acceptToken(TokenType.LPAREN)
                            args.addAll(parseArgumentList())
                            acceptToken(TokenType.RPAREN)
                            FunctionCall(t.lexeme!!, t.toContext(), context, args)
                        } else {
                            VariableReference(t)
                        }
                    } else {
                        cancel("unexpected token '${stream.seek().type}', expected function call or member variable")
                        null!!
                    }

                    if (firstRound) {
                        access = MemberAccess(base, e)
                    } else access!!.accessor = MemberAccess(access.accessor, e)

                    firstRound = false

                } while (stream.acceptIfNext(TokenType.DOT))

                return access!!
            }

            private fun parseFactor(stream: TokenStream): Expression {
                if (stream.acceptIfNext(TokenType.PLUS)) return UnaryExpression(
                    parseFactor(stream),
                    UnaryOperator.PLUS
                ) // unary plus
                if (stream.acceptIfNext(TokenType.MINUS)) return UnaryExpression(
                    parseFactor(stream),
                    UnaryOperator.MINUS
                ) // unary minus
                if (stream.acceptIfNext(TokenType.TILDE)) return UnaryExpression(
                    parseFactor(stream),
                    UnaryOperator.NEG
                ) // negate
                if (stream.acceptIfNext(TokenType.NOT)) return UnaryExpression(
                    parseFactor(stream),
                    UnaryOperator.NOT
                ) // not
                var e: Expression

                if (stream.acceptIfNext(TokenType.LPAREN)) { // parentheses
                    if (stream.isNext(TokenType.RPAREN)) {
                        cancel("expected expression, got '${stream.seek().type}'")
                        return null!!
                    }

                    e = ParsenthesizedExpression(this.parseExpression(stream))
                    acceptToken(TokenType.RPAREN)
                } else e = parseValue(stream)

                if (stream.acceptIfNext(TokenType.POW)) e =
                    BinaryExpression(e, parseFactor(stream), BinaryOperator.POW, Precedence.VERY_HIGH)
                return e
            }

            private fun parseValue(stream: TokenStream): Expression {
                var e: Expression
                initContexts(pushFirst = true)

                when {
                    stream.isNext(
                        TokenType.INTEGER_LITERAL,
                        TokenType.FLOAT_LITERAL,
                        TokenType.SHORT_LITERAL,
                        TokenType.HEX_LITERAL,
                        TokenType.LONG_LITERAL,
                        TokenType.STRING_LITERAL,
                        TokenType.TEMPLATE_STRING,
                        TokenType.YES,
                        TokenType.NO,
                        TokenType.NOTHING
                    ) -> {
                        e = Literal(stream.next())
                        if (stream.acceptIfNext(TokenType.DOT)) {
                            e = parseMemberAccess(e, stream)
                        }
                    }

                    stream.acceptIfNext(TokenType.NEW) -> {
                        val name = acceptToken(TokenType.IDENTIFIER).lexeme!!
                        val args = mutableListOf<Argument>()
                        acceptToken(TokenType.LPAREN)
                        args.addAll(parseArgumentList())
                        acceptToken(TokenType.RPAREN)
                        pushContext()
                        e = ObjectCreation(name, args, getStart(), getEnd())

                    }

                    stream.isNext(TokenType.IDENTIFIER) -> {
                        val t = acceptToken(TokenType.IDENTIFIER)
                        e = if (stream.isNext(TokenType.LPAREN)) {
                            val args = mutableListOf<Argument>()
                            acceptToken(TokenType.LPAREN)
                            args.addAll(parseArgumentList())
                            acceptToken(TokenType.RPAREN)
                            FunctionCall(t.lexeme!!, t.toContext(), context, args)
                        } else VariableReference(t)
                    }

                    else -> {
                        cancel("unexpected token '${if (stream.hasNext()) stream.seek().type else "EOF"}', expected expression or value")
                        e = null!!
                        stream.advance()
                    }
                }

                return e
            }

            // stolen from
            // https://gitlab.gnome.org/GNOME/vala/-/blob/master/vala/valaparser.vala#L609
            private fun parseArgumentList(): List<Argument> {
                val list = mutableListOf<Argument>()

                if (!stream.isNext(TokenType.RPAREN)) {
                    do {
                        // possible trailing comma
                        if (stream.isNext(TokenType.RPAREN)) break

                        list.add(parseArgument())
                    } while (stream.acceptIfNext(TokenType.COMMA))
                }

                return list
            }

            private fun parseArgument(): Argument {
                var name: String? = null

                if (stream.isNext(TokenType.IDENTIFIER)) {
                    name = acceptToken(TokenType.IDENTIFIER).lexeme
                    if (stream.isNext(TokenType.COLON)) stream.advance()
                    else {
                        stream.position--; name = null
                    }
                }
                //if (stream.isNext(TokenType.DOT)) stream.position--

                val expr = parseExpression()
                return Argument(expr, name)
            }


        }.parseExpression()
    }

    //endregion
    //endregion

    companion object {
        val ASSIGNMENT_OP = arrayOf(
            TokenType.ASSIGN,
            TokenType.PLUSSET,
            TokenType.MINUSSET,
            TokenType.MULSET,
            TokenType.DIVSET,
            TokenType.MODSET,
            TokenType.BANDSET,
            TokenType.XORSET,
            TokenType.POWSET,
            TokenType.BORSET,
        )

        // Utility methods to get an instance
        fun fromFile(filename: String): Parser {
            return Parser(TokenStream.open(filename))
        }

        fun fromString(source: String, mode: SourceMode): Parser {
            return Parser(TokenStream.open(Lexer(source, mode)))
        }

        fun fromLexer(lexer: Lexer): Parser {
            return Parser(TokenStream.open(lexer))
        }

        fun fromStream(stream: TokenStream): Parser {
            return Parser(stream)
        }
    }
}