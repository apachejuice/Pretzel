package com.pretzel.core

import com.pretzel.core.lexer.Lexer
import com.pretzel.core.lexer.Lexer.SourceMode
import com.pretzel.core.lexer.TokenStream
import com.pretzel.core.parser.Parser

data class CodeContext(
    val keepGoing: Boolean,
    val debug: Boolean,
    val source: String,
    val sourceMode: SourceMode,
) {
    fun createParser(stream: TokenStream): Parser {
        val p = Parser(stream)
        p.keepGoing = keepGoing
        p.debug = debug
        return p
    }

    fun createLexer(): Lexer {
        val l = Lexer(source, sourceMode)
        l.keepGoing = keepGoing
        return l
    }
}