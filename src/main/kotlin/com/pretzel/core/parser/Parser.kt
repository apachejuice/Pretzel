package com.pretzel.core.parser

import com.pretzel.core.ErrorType.PARSER
import com.pretzel.core.Report
import com.pretzel.core.ast.Node
import com.pretzel.core.ast.UseStmt
import com.pretzel.core.lexer.Lexer
import com.pretzel.core.lexer.Lexer.Token
import com.pretzel.core.lexer.Lexer.SourceMode
import com.pretzel.core.lexer.Lexer.TokenType
import com.pretzel.core.lexer.Lexer.Context
import com.pretzel.core.lexer.TokenStream

import java.util.Stack
import java.util.StringJoiner


class Parser private constructor(val stream: TokenStream, private val repl: Boolean = false) {
    private var line: Int = 1
    private var column: Int = 1
    private var pos: Int = 0
    private var hadError: Boolean = false
    private val contextStack: Stack<Context> = Stack()
    private val nodeCache: Stack<Node> = Stack()
    private val nodes = ArrayList<Node>()
    val lexer: Lexer
    val sourceName: String = stream.lexer.filename

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
        return contextStack[i];
    }

    private fun finishContexts(): Stack<Context> {
        val result = contextStack
        contextStack.clear()
        return result
    }

    init {
        this.lexer = stream.lexer
    }

    private fun <T> consumeToken(action: (t: Token, c: Context) -> T): T {
        return action.invoke(stream.next(), context)
    }

    private fun <T> acceptToken(tt: TokenType, strict: Boolean = false, action: (strict: Boolean, t: Token, c: Context) -> T): T {
        val tok = if (strict) stream.accept(tt) else {
            stream.acceptIfNext(tt)
            stream.seek()
        }

        return action.invoke(strict, tok, context)
    }
    
    private fun acceptToken(tt: TokenType): Token {
        return if (stream.acceptIfNext(tt)) {
            stream.seek()
        } else {
            error("expected symbol of type '$tt', got '${ if (stream.hasNext()) stream.seek().type else "EOF"}'",
                if (stream.hasNext()) -1 else 0)
            if (stream.hasNext()) stream.next() else stream.seek()
        }
    }

    private fun error(message: String, pointerOffset: Int = 0) =
        Report.error(PARSER, message, stream.seek(), repl, pointerOffset = pointerOffset).also { hadError = true }

    // PARSING METHODS
    // The base parsing method; the root of the tree.
    fun parse() {
        when (stream.seek().type) {
            TokenType.USE -> {
                parseUseStmt()
                checkAndPushNode()
            }
            else -> error("not implemented yet")
        }
    }

    private fun checkAndPushNode() {
        if (nodeCache.isEmpty())
            return

        nodes.addAll(nodeCache)
        nodeCache.clear()
    }
    
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

        // Make sure we dont operate on a broken stack
        if (!hadError)
            nodeCache.push(UseStmt(
                result.toString().split(":"),
                getContext(0),
                getContext(1),
                wildcard
            ).also {
                finishContexts()
                hadError = false
            })
    }
    
    companion object {
        // Utility methods to get an instance
        fun fromFile(filename: String, repl: Boolean = false): Parser {
            return Parser(TokenStream.open(filename), repl)
        }

        fun fromString(source: String, repl: Boolean = false): Parser {
            return Parser(TokenStream.open(Lexer(source, if (repl) SourceMode.DIRECT else SourceMode.FILE, repl)), repl)
        }

        fun fromLexer(lexer: Lexer, repl: Boolean = false): Parser {
            return Parser(TokenStream.open(lexer), repl)
        }

        fun fromStream(stream: TokenStream, repl: Boolean = false): Parser {
            return Parser(stream, repl)
        }
    }
}
