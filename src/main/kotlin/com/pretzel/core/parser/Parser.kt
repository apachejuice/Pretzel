package com.pretzel.core.parser

import com.pretzel.core.lexer.Lexer
import com.pretzel.core.lexer.TokenStream


class Parser private constructor(val stream: TokenStream) {
    val sourceName: String
    val lexer: Lexer

    init {
        this.sourceName = stream.lexer.filename
        this.lexer = stream.lexer
    }

    companion object {
        // Utility methods to get an instance
        fun fromFile(filename: String): Parser {
            return Parser(TokenStream.open(filename))
        }

        fun fromString(source: String, repl: Boolean): Parser {
            return Parser(TokenStream.open(Lexer(source, if (repl) Lexer.SourceMode.DIRECT else Lexer.SourceMode.FILE, repl)))
        }

        fun fromLexer(lexer: Lexer): Parser {
            return Parser(TokenStream.open(lexer))
        }

        fun fromStream(stream: TokenStream): Parser {
            return Parser(stream)
        }
    }
}
