package com.pretzel.core.parser

import com.pretzel.core.ErrorType.PARSER
import com.pretzel.core.Report
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
    private val contextStack: Stack<Context> = Stack()
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

    private fun finishContexts(): Stack<Context> {
        val result = contextStack
        contextStack.clear()
        return result
    }

    init {
        this.lexer = stream.lexer
    }

    private fun reportError(message: String) {
        Report.error(PARSER, message, context, repl)
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
        if (!stream.hasNext()) error("unexpected EOF")
        else return stream.accept(tt)
        return stream.seek() // will never be called
    }

    private fun error(message: String) =
        Report.error(PARSER, message, stream.seek().toContext(), repl)

    // PARSING METHODS
    // The base parsing method; the root of the tree.
    fun parse() {
        when (stream.seek().type) {
            TokenType.USE -> parseUseStmt()
            else -> error("not implemented yet")
        }
    }
    
    private fun parseUseStmt() {
        val result = StringJoiner(":")
        acceptToken(TokenType.USE)
        result.add(acceptToken(TokenType.IDENTIFIER).lexeme)

        if (stream.isNext(TokenType.COLON)) {
            stream.advance()
            while (true) {
                result.add(acceptToken(TokenType.IDENTIFIER).lexeme)
                if (!stream.isNext(TokenType.COLON)) break
            }
        }

        println(result.toString())
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
