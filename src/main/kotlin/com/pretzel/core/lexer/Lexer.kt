/*
 * Copyright 2021 apachejuice
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

package com.pretzel.core.lexer

import com.pretzel.core.errors.ErrorCode
import com.pretzel.core.errors.Reporter
import com.pretzel.core.errors.StreamReporter

import java.io.File
import java.util.Stack

class Lexer(_source: String, mode: SourceMode) {
    private val reporter: Reporter = StreamReporter(System.err)

    enum class SourceMode {
        FILE,
        DIRECT,
    }

    enum class TokenType {
        PLUSSET,
        MINUSSET,
        MULSET,
        DIVSET,
        MODSET,
        BANDSET,
        XORSET,
        POWSET,
        BORSET,
        RSHIFTSET,
        URSHIFTSET,
        LSHIFTSET,
        CONCAT_COMMA_SET,
        CONCAT_SPACE_SET,
        BAND,
        ASSIGN,
        ARROW,
        AT,
        BOOL,
        BREAK,
        HEX_LITERAL,
        CLASS,
        COLON,
        COMMA,
        CONCAT_COMMA,
        CONCAT_SPACE,
        DIV,
        DOT,
        ELIF,
        ELSE,
        EQ,
        ERROR,
        FLOAT_LITERAL,
        FUNC,
        GT,
        GTEQ,
        HASHTAG,
        IDENTIFIER,
        IF,
        IN,
        INTEGER_LITERAL,
        IS,
        LBRACE,
        LONG_LITERAL,
        LPAREN,
        LT,
        LTEQ,
        MINUS,
        NEW,
        MOD,
        MUL,
        NO,
        BOR,
        AND,
        OR,
        NOT,
        NOTEQ,
        NOTHING,
        PLUS,
        FOREACH,
        POW,
        RBRACE,
        RPAREN,
        SHORT_LITERAL,
        LSQB,
        FROM,
        RSQB,
        STRING_LITERAL,
        TEMPLATE_STRING,
        TILDE,
        FOR,
        WHILE,
        USE,
        VAR,
        WHEN,
        YES,
        XOR,
        QUERY,
        RSHIFT,
        URSHIFT,
        COALESCE,
        LSHIFT,
        QUOTE,
        SEMI,
        EOF,

        // no token
        INVALID,
    }

    data class LexingException(val last: Token, val source: String) : RuntimeException()

    data class Location(
        val pos: Int, val line: Int, val column: Int, val file: String
    ) {
        override operator fun equals(other: Any?): Boolean {
            if (other == null || other !is Location)
                return false

            if (other.hashCode() == hashCode())
                return true

            return line == other.line && column == other.column && file == other.file
        }

        override fun hashCode(): Int {
            var result = line
            result = 31 * result + column
            result = 31 * result + file.hashCode()
            return result
        }

        fun toString(columnOffset: Int = 0): String {
            return "file '$file' $line:${column + columnOffset}"
        }
    }

    data class Span(
        val start: Location,
        val end: Location,
        val content: String,
        val lineContent: String
    ) {
        override operator fun equals(other: Any?): Boolean {
            if (other == null || other !is Span)
                return false

            if (other.hashCode() == hashCode())
                return true

            return other.start == start && other.end == end && other.content == content
        }

        override fun hashCode(): Int {
            var result = start.hashCode()
            result = 31 * result + end.hashCode()
            result = 31 * result + content.hashCode()
            return result
        }

        override fun toString(): String {
            return "($start - $end)[$content]"
        }
    }

    open class Token(
        val lexeme: String, val type: TokenType, val span: Span
    ) {
        override operator fun equals(other: Any?): Boolean {
            if (other == null || other !is Token)
                return false

            if (other.hashCode() == hashCode())
                return true

            return lexeme == other.lexeme && span == other.span && type == other.type
        }

        override fun toString(): String {
            return "${type}('${lexeme}' $span"
        }

        override fun hashCode(): Int {
            var result = lexeme.hashCode()
            result = 31 * result + type.hashCode()
            result = 31 * result + span.hashCode()
            return result
        }

        fun isType(t: TokenType): Boolean {
            return type == t
        }

        fun isType(s: String): Boolean {
            return lexeme == s
        }
    }

    private fun cancel(code: ErrorCode, t: Token) {
        reporter.error(code, t.span, "'${t.lexeme}'")
        cancellationHook.invoke(code, t)
        if (!keepGoing)
            throw LexingException(t, source)
    }

    var keepGoing: Boolean = true
    var hadError: Boolean = false
    var cancellationHook: (code: ErrorCode, t: Token) -> Unit =
        { _: ErrorCode, _: Token -> }

    private val kwds: Map<String, TokenType> = mapOf(
        "bool" to TokenType.BOOL,
        "break" to TokenType.BREAK,
        "elif" to TokenType.ELIF,
        "else" to TokenType.ELSE,
        "error" to TokenType.ERROR,
        "func" to TokenType.FUNC,
        "if" to TokenType.IF,
        "in" to TokenType.IN,
        "is" to TokenType.IS,
        "no" to TokenType.NO,
        "new" to TokenType.NEW,
        "nothing" to TokenType.NOTHING,
        "use" to TokenType.USE,
        "when" to TokenType.WHEN,
        "yes" to TokenType.YES,
        "class" to TokenType.CLASS,
        "mod" to TokenType.MOD,
        "let" to TokenType.VAR,
        "for" to TokenType.FOR,
        "foreach" to TokenType.FOREACH,
        "while" to TokenType.WHILE,
        "from" to TokenType.FROM,
    )


    private var line: Int = 1
    private var column: Int = 1
    private var pos: Int = 0
    private val mode: SourceMode
    private val sourceLength: Int
        get() = source.length
    private val currentLoc: Location
        get() = Location(pos, line, column, sourceName)
    private var startLocation: Location

    val source: String

    val file: File?
        get() {
            return when (mode) {
                SourceMode.FILE -> File(source)
                SourceMode.DIRECT -> null
            }
        }

    val sourceName: String
        get() {
            return if (mode == SourceMode.DIRECT) "<input>" else file!!.name
        }

    val tokens: Stack<Token> = Stack()

    init {
        source = when (mode) {
            SourceMode.FILE -> File(_source).readText()
            SourceMode.DIRECT -> _source
        }

        this.mode = mode
        this.startLocation = currentLoc
    }

    fun clean() {
        line = 1
        column = 1
        pos = 0
        tokens.clear()
    }

    private fun shiftInLine() {
        pos++
        column++
    }

    private fun isAtEnd(n: Int = 0): Boolean {
        return pos + n >= sourceLength
    }

    private fun peek(n: Int = 0): Char {
        if (isAtEnd(n)) return '\u0000'
        return source[pos + n]
    }

    private fun next(): Char {
        val c = peek()
        if (c == '\n') {
            line++
            column = 1
        } else column++

        pos++
        return c
    }

    private fun isDigit(c: Char) = c in '0'..'9'

    private fun isAlpha(c: Char) = c in 'a'..'z' || c in 'A'..'Z' || c == '_'

    private fun isAlphaNumeric(c: Char) = isAlpha(c) || isDigit(c)

    private fun match(expected: Char): Boolean {
        if (isAtEnd()) return false
        if (source[pos] != expected) return false
        pos++
        column++
        return true
    }

    private fun match(expected: Int): Boolean = match(expected.toChar())

    private fun divOrComment() {
        when {
            match('/') -> while (peek() != '\n' && !isAtEnd()) next()
            match('*') -> {
                while (true) {
                    if (next() == '*')
                        if (peek() == '/') break
                    if (isAtEnd()) {
                        pushToken(TokenType.MUL, "*")
                        cancel(ErrorCode.UNEXPECTED_EOF, tokens.lastElement())
                        hadError = true
                        // make sure to not fall in an endless loop
                        next()
                        return
                    }
                }

                // The remaining /
                next()
            }
            else -> pushToken(TokenType.DIV, '/')
        }
    }

    private fun string() {
        val result = StringBuilder()

        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') line++
            result.append(peek())
            shiftInLine()
        }

        if (isAtEnd()) {
            pushToken(TokenType.QUOTE, '"')
            cancel(ErrorCode.UNEXPECTED_EOF, tokens.lastElement())
            hadError = true
            return
        }

        next()
        pushToken(TokenType.STRING_LITERAL, result.toString())
    }

    private fun templateString() {
        next()
        string()

        val template = tokens.pop()
        pushToken(TokenType.TEMPLATE_STRING, template.lexeme)
    }

    private fun number(
        startDigit: Char,
        possibleHex: Boolean,
        returnString: Boolean = false
    ): String? {
        val result = StringBuilder()
        val ishex = possibleHex && peek().toLowerCase() == 'x'
        if (ishex) {
            result.append("x")
            next()
            while (peek().isDigit() || peek().toLowerCase() in "abcdef") result.append(
                next().toUpperCase()
            )
            if (result.length != 1) {
                pushToken(TokenType.INVALID, peek())
                cancel(ErrorCode.UNEXPECTED_EOF, tokens.pop())
            }

            if (!returnString) {
                pushToken(TokenType.HEX_LITERAL, "0$result")
                return null
            }

            return "0$result"
        }

        while (peek().isDigit()) result.append(next())
        var frac = false
        var long = false
        var short = false

        if (peek() == '.' && peek(1).isDigit() && !ishex) {
            frac = true
            result.append(next())
            while (peek().isDigit()) result.append(next())
        } else if (peek().toLowerCase() == 'l') {
            long = true
            next()
        } else if (peek().toLowerCase() == 's') {
            short = true
            next()
        }

        if (!returnString) {
            pushToken(
                when {
                    frac -> TokenType.FLOAT_LITERAL
                    long -> TokenType.LONG_LITERAL
                    short -> TokenType.SHORT_LITERAL
                    else -> TokenType.INTEGER_LITERAL
                },
                startDigit + result.toString()
            )
            return null
        }

        return startDigit + result.toString()
    }

    private fun identifier(startDigit: Char) {
        val result = StringBuilder(startDigit.toString())
        while (isAlphaNumeric(peek())) result.append(next())
        // See if the identifier is a reserved word.
        val text = result.toString()

        val type = if (text !in kwds.keys) TokenType.IDENTIFIER
        else kwds[text]
        pushToken(type!!, result.toString())
    }

    private fun pushToken(type: TokenType, literal: String) {
        tokens.push(
            Token(
                literal,
                type,
                Span(
                    startLocation,
                    currentLoc,
                    source.substring(startLocation.pos, currentLoc.pos),
                    source.lines()[startLocation.line - 1]
                )
            )
        )

        this.startLocation = currentLoc
    }

    private fun pushToken(type: TokenType, literal: Char) =
        pushToken(type, "%c".format(literal))

    private fun getNextToken() {
        when (val c = next()) {
            '(' -> pushToken(TokenType.LPAREN, c)
            ')' -> pushToken(TokenType.RPAREN, c)
            '{' -> pushToken(TokenType.LBRACE, c)
            '}' -> pushToken(TokenType.RBRACE, c)
            '[' -> pushToken(TokenType.LSQB, c)
            ']' -> pushToken(TokenType.RSQB, c)
            ',' -> pushToken(TokenType.COMMA, c)
            ':' -> pushToken(TokenType.COLON, c)
            '#' -> pushToken(TokenType.HASHTAG, c)
            '$' -> pushToken(TokenType.VAR, c)
            '~' -> pushToken(TokenType.TILDE, c)
            ';' -> pushToken(TokenType.SEMI, c)

            '?' -> {
                val tok: String
                val tt = when {
                    match('?') -> {
                        tok = "??"
                        TokenType.COALESCE
                    }

                    else -> {
                        tok = "?"
                        TokenType.QUERY
                    }
                }

                pushToken(tt, tok)
            }

            '@' -> {
                if (match('"'))
                    templateString()
                else pushToken(TokenType.AT, c)
            }

            '^' -> {
                val tok: String
                val tt = when {
                    match('=') -> {
                        tok = "^="
                        TokenType.XORSET
                    }

                    else -> {
                        tok = "^"
                        TokenType.XOR
                    }
                }

                pushToken(tt, tok)
            }

            '-' -> {
                val tok: String
                val tt = when {
                    match('>') -> {
                        tok = "->"
                        TokenType.ARROW
                    }

                    match('=') -> {
                        tok = "-="
                        TokenType.MINUSSET
                    }

                    else -> {
                        tok = "-"
                        TokenType.MINUS
                    }
                }

                pushToken(tt, tok)
            }

            '*' -> {
                val tok: String
                val tt = when {
                    match('*') -> {
                        when {
                            match('=') -> {
                                tok = "**="
                                TokenType.POWSET
                            }

                            else -> {
                                tok = "**"
                                TokenType.POW
                            }
                        }
                    }

                    match('=') -> {
                        tok = "*="
                        TokenType.MULSET
                    }

                    else -> {
                        tok = "*"
                        TokenType.MUL
                    }
                }

                pushToken(tt, tok)
            }

            '%' -> {
                val tok: String
                val tt = when {
                    match('=') -> {
                        tok = "%="
                        TokenType.MODSET
                    }

                    else -> {
                        tok = "%"
                        TokenType.MOD
                    }
                }

                pushToken(tt, tok)
            }

            '+' -> {
                val tok: String
                val tt = when {
                    match('=') -> {
                        tok = "+="
                        TokenType.PLUSSET
                    }

                    else -> {
                        tok = "+"
                        TokenType.PLUS
                    }
                }

                pushToken(tt, tok)
            }

            '.' -> {
                val tok: String
                val tt = when {
                    match('.') -> {
                        when {
                            match('.') -> {
                                when {
                                    match('=') -> {
                                        tok = "...="
                                        TokenType.CONCAT_COMMA_SET
                                    }

                                    else -> {
                                        tok = "..."
                                        TokenType.CONCAT_COMMA
                                    }
                                }
                            }

                            else -> when {
                                match('=') -> {
                                    tok = "..="
                                    TokenType.CONCAT_SPACE_SET
                                }

                                else -> {
                                    tok = ".."
                                    TokenType.CONCAT_SPACE
                                }
                            }
                        }
                    }

                    else -> {
                        tok = "."
                        TokenType.DOT
                    }
                }

                pushToken(tt, tok)
            }

            '!' -> {
                val tok: String
                val tt = when {
                    match('=') -> {
                        tok = "!="
                        TokenType.NOTEQ
                    }

                    else -> {
                        tok = "!"
                        TokenType.NOT
                    }
                }

                pushToken(tt, tok)
            }

            '=' -> {
                val tok: String
                val tt = when {
                    match('=') -> {
                        tok = "=="
                        TokenType.EQ
                    }

                    else -> {
                        tok = "="
                        TokenType.ASSIGN
                    }
                }

                pushToken(tt, tok)
            }

            '<' -> {
                val tok: String
                val tt = when {
                    match('=') -> {
                        tok = "<="
                        TokenType.LTEQ
                    }

                    match('<') -> {
                        when {
                            match('=') -> {
                                tok = "<<="
                                TokenType.LSHIFTSET
                            }

                            else -> {
                                tok = "<<"
                                TokenType.LSHIFT
                            }
                        }
                    }

                    else -> {
                        tok = "<"
                        TokenType.LT
                    }
                }

                pushToken(tt, tok)
            }

            '>' -> {
                val tok: String
                val tt = when {
                    match('=') -> {
                        tok = ">="
                        TokenType.GTEQ
                    }

                    match('>') -> {
                        when {
                            match('=') -> {
                                tok = ">>="
                                TokenType.RSHIFTSET
                            }

                            match('>') -> {
                                when {
                                    match('=') -> {
                                        tok = ">>>="
                                        TokenType.URSHIFTSET
                                    }

                                    else -> {
                                        tok = ">>>"
                                        TokenType.URSHIFT
                                    }
                                }
                            }

                            else -> {
                                tok = ">>"
                                TokenType.RSHIFT
                            }
                        }
                    }

                    else -> {
                        tok = ">"
                        TokenType.GT
                    }
                }

                pushToken(tt, tok)
            }

            '&' -> {
                val tok: String
                val tt = when {
                    match('=') -> {
                        tok = "&="
                        TokenType.BANDSET
                    }

                    match('&') -> {
                        tok = "&&"
                        TokenType.AND
                    }

                    else -> {
                        tok = "&"
                        TokenType.BAND
                    }
                }

                pushToken(tt, tok)
            }

            '|' -> {
                val tok: String
                val tt = when {
                    match('=') -> {
                        tok = "|="
                        TokenType.BORSET
                    }

                    match('|') -> {
                        tok = "||"
                        TokenType.OR
                    }

                    else -> {
                        tok = "|"
                        TokenType.BOR
                    }
                }

                pushToken(tt, tok)
            }

            '/' -> {
                if (match('=')) pushToken(TokenType.DIVSET, "/=")
                else divOrComment()
            }

            '\t', ' ', '\r' -> { /* ignore */
            }
            '\n' -> line++
            '"' -> string()
            '\'' -> string()
            in '0'..'9' -> number(c, c == '0')
            else -> {
                if (isAlpha(c)) identifier(c)
                else {
                    pushToken(TokenType.INVALID, c.toString())
                    cancel(ErrorCode.UNEXPECTED_TOKEN, tokens.lastElement())
                    hadError = true
                }
            }
        }
    }

    fun getAllTokens() {
        while (!isAtEnd()) getNextToken()
    }
}