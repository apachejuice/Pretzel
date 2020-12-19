package com.pretzel.core.parser

import com.pretzel.core.ErrorType.PARSER
import com.pretzel.core.Report
import com.pretzel.core.lexer.Lexer
import com.pretzel.core.lexer.Lexer.SourceMode
import com.pretzel.core.lexer.Lexer.Context
import com.pretzel.core.lexer.TokenStream

import java.util.Stack


class Parser private constructor(val stream: TokenStream, private val repl: Boolean = false) {
    private var line: Int = 1
    private var column: Int = 1
    private var pos: Int = 0
    private val contextStack: Stack<Context> = Stack()
    val lexer: Lexer
    val sourceName: String = stream.lexer.filename

    private val context: Context
        get() = Context(line, column, lexer.source.lines()[line - 1], sourceName)

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

    companion object {
        // Utility methods to get an instance
        fun fromFile(filename: String): Parser {
            return Parser(TokenStream.open(filename))
        }

        fun fromString(source: String, repl: Boolean): Parser {
            return Parser(TokenStream.open(Lexer(source, if (repl) SourceMode.DIRECT else SourceMode.FILE, repl)))
        }

        fun fromLexer(lexer: Lexer): Parser {
            return Parser(TokenStream.open(lexer))
        }

        fun fromStream(stream: TokenStream): Parser {
            return Parser(stream)
        }
    }
}
