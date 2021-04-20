package com.pretzel.core.parser

import com.pretzel.core.errors.ErrorCode.*
import com.pretzel.core.lexer.Lexer.TokenType
import com.pretzel.core.lexer.TokenStream
import com.pretzel.core.parser.ast.UseStatement

import java.util.StringJoiner

class UseStatementParser(stream: TokenStream) : Parser<UseStatement>(stream) {
    override fun parse(): UseStatement {
        internalInitParse()
        accept(TokenType.USE, whenError = ASSERTED)
        val target = parseNamespace()
        acceptShift(TokenType.SEMI, whenError = EXPECTED_SEMI)
        return UseStatement(target, joinSpan(begin, currentSpan))
    }

    private fun parseNamespace(): String {
        val result = StringJoiner(":")
        result.add(acceptShift(TokenType.IDENTIFIER, whenError = EXPECTED_NSNAME).lexeme)
        if (stream.acceptIfNext(TokenType.COLON)) {
            while (true) {
                result.add(
                    acceptShift(
                        TokenType.IDENTIFIER,
                        whenError = EXPECTED_ID
                    ).lexeme
                )
                if (!stream.isNext(TokenType.COLON)) break
                else acceptShift(TokenType.COLON, whenError = EXPECTED_MEMBER_ACCESSOR)
            }
        }

        return result.toString()
    }
}