package com.pretzel.core.parser

import com.pretzel.core.ErrorType.PARSER
import com.pretzel.core.Report
import com.pretzel.core.ast.BinaryExpression
import com.pretzel.core.ast.Expression
import com.pretzel.core.ast.FunctionCall
import com.pretzel.core.ast.Literal
import com.pretzel.core.ast.Node
import com.pretzel.core.ast.Precedence
import com.pretzel.core.ast.UnaryExpression
import com.pretzel.core.ast.UseStmt
import com.pretzel.core.lexer.Lexer
import com.pretzel.core.lexer.Lexer.Context
import com.pretzel.core.lexer.Lexer.SourceMode
import com.pretzel.core.lexer.Lexer.Token
import com.pretzel.core.lexer.Lexer.TokenType
import com.pretzel.core.lexer.TokenStream

import java.util.Stack
import java.util.StringJoiner
import kotlin.system.exitProcess


class Parser private constructor(val stream: TokenStream) {
    private var hadError: Boolean = false
    private val contextStack: Stack<Context> = Stack()
    private val nodeCache: Stack<Node> = Stack()
    private val nodes = ArrayList<Node>()

    var cancellationHook: (token: Token) -> Unit = { exitProcess(1) }
    val lexer: Lexer
    val sourceName: String = stream.lexer.filename

    @Synchronized
    private fun cancel(token: Token) {
        Thread.currentThread().interrupt()
        cancellationHook.invoke(token)
        throw IllegalStateException("parse shoud've been cancelled")
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

    private fun getContext(i: Int): Context {
        return contextStack[i]
    }

    private fun finishContexts(): Stack<Context> {
        val result = contextStack
        contextStack.clear()
        return result
    }

    init {
        this.lexer = stream.lexer
    }

    private fun consumeToken(action: (t: Token, c: Context) -> Unit) {
        if (!stream.hasNext()) {
            error("expected token, got 'EOF'")
        } else action.invoke(stream.next(), context)
    }

    private fun acceptToken(
        tt: TokenType,
        strict: Boolean = false,
        action: (strict: Boolean, t: Token, c: Context) -> Unit
    ) {
        val tok = if (strict) stream.accept(tt) else {
            stream.acceptIfNext(tt)
            stream.seek()
        }

        return action.invoke(strict, tok, context)
    }

    private fun acceptToken(tt: TokenType): Token {
        return if (stream.isNext(tt)) {
            stream.seek().also { stream.advance() }
        } else {
            error(
                "expected symbol of type '$tt', got '${if (stream.hasNext()) stream.seek().type else "EOF"}'",
                !stream.hasNext()
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

    private fun error(message: String, missing: Boolean = false) =
        Report.error(PARSER, message, stream.seek(), missing).also {
            hadError = true
            cancel(stream.seek())
        }

    // region PARSING METHODS
    // The base parsing method; the root of the tree.
    fun parse(): Node? {
        while (stream.hasNext()) {
            when (stream.seek().type) {
                TokenType.USE -> {
                    parseUseStmt()
                    checkAndPushNode()
                }
                else -> parseExpression(stream)//error("not implemented yet")
            }
        }

        nodes.forEach { println(it) }

        return if (nodes.isEmpty())
            null
        else
            Node.getRootInstance(nodes)
    }

    //region STATEMENT PARSING

    private fun parseUseStmt() {
        val result = StringJoiner(":")
        var wildcard = false
        acceptToken(TokenType.USE)
        initContexts(pushFirst = true)
        result.add(acceptToken(TokenType.IDENTIFIER).lexeme)
        if (hadError) return

        if (stream.acceptIfNext(TokenType.COLON)) {
            while (true) {
                if (stream.isNext(TokenType.MUL)) {
                    wildcard = true
                    break
                }

                result.add(acceptToken(TokenType.IDENTIFIER).lexeme)
                if (!stream.isNext(TokenType.COLON)) break
                else stream.accept(TokenType.COLON)
            }
        } else if (stream.isNext(TokenType.MUL)) wildcard = true

        pushContext()
        pushNodeUnlessError(
            UseStmt(
                result.toString().split(":"),
                getContext(0),
                getContext(1),
                wildcard
            )
        )

        stream.advance()
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
        STRSPACE("..");
    }

    enum class UnaryOperator(val operator: String) {
        NEG("~"),
        NOT("!"),
        PLUS("+"),
        MINUS("-");
    }

    private fun parseExpression(stream: TokenStream) {
        val node = object : Any() {
            fun parse(): Expression {
                return parseExpression()
            }

            // Grammar:
            // expression = term | expression `+` term | expression `-` term
            // term = factor | term `*` factor | term `/` factor
            // factor = `+` factor | `-` factor | `(` expression `)`
            //        | number | functionName factor | factor `^` factor
            private fun parseExpression(): Expression {
                var e = parseTerm()
                var op: BinaryOperator

                while (true) {
                    if (stream.acceptIfNext(TokenType.PLUS, TokenType.MINUS)) {
                        op = if (stream.seek().type == TokenType.PLUS) BinaryOperator.PLUS else BinaryOperator.MINUS
                        e = BinaryExpression(
                            e,
                            parseTerm(),
                            op,
                            Precedence.PRETTY_HIGH
                        )
                    }

                    else return e
                }
            }

            private fun parseTerm(): Expression {
                var e = parseFactor()
                var op: BinaryOperator

                while (true) {
                    if (stream.isNext(TokenType.MUL, TokenType.DIV)) {
                        op = if (stream.seek().type == TokenType.MUL) BinaryOperator.MUL else BinaryOperator.DIV
                        stream.advance()
                        e = BinaryExpression(
                            e,
                            parseFactor(),
                            op,
                            Precedence.PRETTY_HIGH
                        )
                    }

                    else return e
                }
            }

            private fun parseFactor(): Expression {
                if (stream.acceptIfNext(TokenType.PLUS))  return UnaryExpression(parseFactor(), UnaryOperator.PLUS) // unary plus
                if (stream.acceptIfNext(TokenType.MINUS)) return UnaryExpression(parseFactor(), UnaryOperator.MINUS) // unary minus
                if (stream.acceptIfNext(TokenType.TILDE)) return UnaryExpression(parseFactor(), UnaryOperator.NEG) // negate
                if (stream.acceptIfNext(TokenType.NOT))   return UnaryExpression(parseFactor(), UnaryOperator.NOT) // not
                var e: Expression

                if (stream.acceptIfNext(TokenType.LPAREN)) { // parentheses
                    e = parseExpression()
                    stream.accept(TokenType.RPAREN)
                } else e = parseValue()

                if (stream.acceptIfNext(TokenType.POW)) e = BinaryExpression(e, parseFactor(), BinaryOperator.POW, Precedence.VERY_HIGH)
                return e
            }

            private fun parseValue(): Expression {
                val e: Expression

                if (stream.isNext(
                        TokenType.INTEGER_LITERAL,
                        TokenType.FLOAT_LITERAL,
                        TokenType.SHORT_LITERAL,
                        TokenType.HEX_LITERAL,
                        TokenType.LONG_LITERAL,
                        TokenType.YES,
                        TokenType.NO,
                    )
                ) {
                    e = Literal(stream.seek())
                    stream.advance()
                } else if (stream.isNext(TokenType.IDENTIFIER)) {
                    val t = stream.seek()
                    stream.advance()
                    e = if (stream.isNext(TokenType.LPAREN)) {
                        stream.advance()
                        stream.accept(TokenType.RPAREN)
                        FunctionCall(t.lexeme.toString(), t.toContext(), context)
                    } else Literal(t)
                    stream.advance()
                } else {
                    error("unexpected token '${stream.seek().type}'")
                    return null!! // the thread has been killed
                }

                return e
            }

        }.parse()

        pushNodeUnlessError(node)
        checkAndPushNode()
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
