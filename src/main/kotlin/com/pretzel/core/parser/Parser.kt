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
import com.pretzel.core.ast.Expression
import com.pretzel.core.ast.FunctionCall
import com.pretzel.core.ast.Literal
import com.pretzel.core.ast.ModStmt
import com.pretzel.core.ast.Node
import com.pretzel.core.ast.ObjectCreation
import com.pretzel.core.ast.ParsenthesizedExpression
import com.pretzel.core.ast.Precedence
import com.pretzel.core.ast.UnaryExpression
import com.pretzel.core.ast.UseStmt
import com.pretzel.core.ast.VariableAssignment
import com.pretzel.core.ast.VariableCreation
import com.pretzel.core.lexer.Lexer
import com.pretzel.core.lexer.Lexer.Context
import com.pretzel.core.lexer.Lexer.SourceMode
import com.pretzel.core.lexer.Lexer.Token
import com.pretzel.core.lexer.Lexer.TokenType
import com.pretzel.core.lexer.TokenStream

import java.util.Stack
import java.util.StringJoiner


class Parser private constructor(val stream: TokenStream) {
    open class ParsingException(val last: Token, val nodes: MutableList<Node>, val overEOF: Boolean = false) : RuntimeException()

    private var hadError: Boolean = false
    var overEOF: Boolean = false
    private val contextStack: Stack<Context> = Stack()
    private val nodeCache: Stack<Node> = Stack()
    private val nodes = mutableListOf<Node>()

    var cancellationHook: (message: String, t: Token, nodes: List<Node>) -> Unit =
        { _: String, _: Token, _: List<Node> -> }
    val lexer: Lexer = stream.lexer
    val sourceName: String = stream.lexer.filename

    private fun cancel(message: String = "") {
        cancellationHook.invoke(message, stream.seek(), nodes)
        throw ParsingException(stream.seek(), nodes, overEOF)
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
    
    private fun acceptToken(vararg tt: TokenType): Token {
        return if (stream.seek().type in tt) {
            stream.seek().also { stream.advance() }
        } else {
            cancel(
                "expected one of '${tt.joinToString(separator = " ,") { "$it" }}', got '${stream.seek()}'",
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
            when (stream.seek().type) {
                TokenType.USE -> parseUseStmt()
                TokenType.MOD -> parseModuleStmt()
                TokenType.VAR -> parseVariable()
                else -> pushNodeUnlessError(parseExpression(stream))
            }

            checkAndPushNode()
        }

        nodes.forEach { println("[$it] (${it.javaClass.name})") }
        return if (nodes.isEmpty())
            null
        else
            Node.getRootInstance(nodes)
    }

    private fun parseVariable() {
        initContexts(pushFirst = true)
        acceptToken(TokenType.VAR)
        val ntok = acceptToken(TokenType.IDENTIFIER)
        val name = ntok.lexeme!!

        // including other assignment operators
        // for more specific error messages
        val assign = stream.isNext(
            TokenType.ASSIGN,
            TokenType.PLUSSET,
            TokenType.MINUSSET,
            TokenType.MULSET,
            TokenType.DIVSET,
            TokenType.MODSET,
            TokenType.BANDSET,
            TokenType.XORSET,
            TokenType.POWSET,
            TokenType.BORSET
        )

        if (!assign) {
            pushContext()
            pushNodeUnlessError(VariableCreation(name, null, getStart(), getEnd()))
            finishContexts()
            return
        }

        val tok = stream.seek()
        val op = tok.type
        val lit = tok.lexeme!!

        if (op != TokenType.ASSIGN)
            cancel("cannot use expression-assignment operator '$lit' when creating new variables")

        if (op == TokenType.ASSIGN) {
            pushContext()
            stream.advance()
            pushNodeUnlessError(VariableCreation(name, parseExpression(stream), getStart(), getEnd()))
            finishContexts()
            return
        } else if (op == TokenType.ASSIGN) {
            pushContext()
            stream.advance()
            pushNodeUnlessError(VariableAssignment(name, parseExpression(stream), getStart(), getEnd()))
            finishContexts()
            return
        }
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

    private fun parseUseStmt() {
        acceptToken(TokenType.USE)
        val result = parseModulePath()

        pushContext()
        pushNodeUnlessError(
            UseStmt(
                result.first.split(":"),
                getStart(),
                getEnd(),
                result.second,
            )
        )
    }

    private fun parseModuleStmt() {
        acceptToken(TokenType.MOD)
        val result = parseModulePath()
        if (result.second)
            cancel("mod statement cannot have wildcards")

        pushContext()
        pushNodeUnlessError(
            ModStmt(
                result.first.split(":"),
                getStart(),
                getEnd(),
            )
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
                    } else return e
                }
            }

            private fun parseTerm(stream: TokenStream): Expression {
                var e = parseFactor(stream)
                var op: BinaryOperator
                var p: Precedence

                while (true) {
                    when (val t = stream.seek().type) {
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
                        return Literal(stream.seek())
                    }

                    e = ParsenthesizedExpression(this.parseExpression(stream))
                    acceptToken(TokenType.RPAREN)
                } else e = parseValue(stream)

                if (stream.acceptIfNext(TokenType.POW)) e =
                    BinaryExpression(e, parseFactor(stream), BinaryOperator.POW, Precedence.VERY_HIGH)
                return e
            }

            private fun parseValue(stream: TokenStream): Expression {
                val e: Expression
                initContexts(pushFirst = true)

                when {
                    stream.isNext(
                        TokenType.INTEGER_LITERAL,
                        TokenType.FLOAT_LITERAL,
                        TokenType.SHORT_LITERAL,
                        TokenType.HEX_LITERAL,
                        TokenType.LONG_LITERAL,
                        TokenType.YES,
                        TokenType.NO,
                        TokenType.NOTHING
                    ) -> {
                        e = Literal(stream.seek())
                        stream.advance()
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
                        } else Literal(t)
                    }

                    else -> {
                        cancel("unexpected token '${if (stream.hasNext()) stream.seek().type else "EOF".also { overEOF = true }}', expected expression or value")
                        e = Literal(stream.seek())
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
                }

                val expr = parseExpression()
                return Argument(expr, name)
            }


        }.parseExpression()
    }

    //endregion
    //endregion

    companion object {
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
