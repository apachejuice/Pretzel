package com.pretzel.core.parser

import com.pretzel.core.errors.ErrorCode.*
import com.pretzel.core.lexer.Lexer.TokenType
import com.pretzel.core.lexer.TokenStream

import java.util.StringJoiner

class UseStmtParser(stream: TokenStream) : Parser(stream) {
    override fun parse(stream: TokenStream, limit: Int) {
        accept(TokenType.USE, whenError = ASSERTED)
        var target = parseNamespace(stream)
    }

    private fun parseNamespace(stream: TokenStream): String {
        val result = StringJoiner(":")
        result.add(accept(TokenType.IDENTIFIER, whenError = EXPECTED_NSNAME).lexeme)
        if (stream.acceptIfNext(TokenType.COLON)) {
            while (true) {
                result.add(accept(TokenType.IDENTIFIER, whenError = EXPECTED_ID).lexeme)
                if (!stream.isNext(TokenType.COLON)) break
                else accept(TokenType.COLON, whenError = EXPECTED_MEMBER_ACCESSOR)
            }
        }

        return result.toString()
    }
}