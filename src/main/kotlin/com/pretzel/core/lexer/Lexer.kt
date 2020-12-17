package com.pretzel.core.lexer


import com.pretzel.core.ErrorType.LEXER
import com.pretzel.core.Report
import java.io.File
import java.util.Stack

class Lexer(_source: String, mode: SourceMode, repl: Boolean = false) {
    enum class SourceMode {
        FILE,
        DIRECT,
    }

    enum class TokenType {
        ASSIGN,
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
        MOD,
        MUL,
        NO,
        NOT,
        SEMI,
        NOTEQ,
        NOTHING,
        PLUS,
        POW,
        RBRACE,
        RPAREN,
        SHORT_LITERAL,
        LSQB,
        RSQB,
        STRING_LITERAL,
        TEMPLATE_STRING,
        USE,
        VAR,
        WHEN,
        YES,
        XOR,
        // no token
        INVALID,
    }

    data class Context(val line: Int, private val _column: Int,
                       val lineContent: String, val file: String) {
        override operator fun equals(other: Any?): Boolean {
            if (other == null || other !is Context)
                return false

            if (other.hashCode() == hashCode())
                return true

            return line == other.line && _column == other._column
                    && other.lineContent == lineContent && file == other.file
        }

        val column: Int
            get() = _column - 1

        override fun hashCode(): Int {
            var result = line
            result = 31 * result + column
            result = 31 * result + lineContent.hashCode()
            result = 31 * result + file.hashCode()
            return result
        }

        override fun toString(): String {
            return "file '$file' $line:$column"
        }
    }

    open class Token(
        val lexeme: String?, val type: TokenType, val line: Int,
        val column: Int, val file: String, val lineContent: String) {
        companion object {
            class NullToken(file: String) : Token("", TokenType.INVALID, -1, -1, file, "")
        }

        override operator fun equals(other: Any?): Boolean {
            if (other == null || other !is Token)
                return false

            if (other.hashCode() == hashCode())
                return true

            return line == other.line && column == other.column && file == other.file && type == other.type
        }

        override fun toString(): String {
            return "${type}('${lexeme}' ${line}:${column})@${file}"
        }

        // autogenerated
        override fun hashCode(): Int {
            var result = lexeme.hashCode()
            result = 31 * result + type.hashCode()
            result = 31 * result + line.hashCode()
            result = 31 * result + column
            result = 31 * result + file.hashCode()
            return result
        }

        fun `is`(t: TokenType): Boolean {
            return type == t
        }

        fun `is`(s: String): Boolean {
            return lexeme == s
        }

        fun toContext(): Context {
            return Context(line, column, lineContent, file)
        }
    }

    val repl: Boolean
    private val context: Context
        get() = Context(line, column, source.lines()[line - 1], filename)

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
        "nothing" to TokenType.NOTHING,
        "use" to TokenType.USE,
        "when" to TokenType.WHEN,
        "yes" to TokenType.YES,
        "class" to TokenType.CLASS,
        "mod" to TokenType.MOD,
    )


    private var line: Int = 1
    private var column: Int = 1
    private var pos: Int = 0
    private var source: String
    private val mode: SourceMode
    private val sourceLength: Int
        get() = source.length

    val file: File?
        get() {
            return when (mode) {
                SourceMode.FILE -> File(source)
                SourceMode.DIRECT -> null
            }
        }

    val filename: String
        get() = if (file == null) "<input>" else file!!.name

    val tokens: Stack<Token> = Stack()

    init {
        source = when (mode) {
            SourceMode.FILE -> File(_source).readText()
            SourceMode.DIRECT -> _source
        }

        this.repl = repl
        this.mode = mode
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
                    if (isAtEnd()) Report.error(LEXER, "unterminated block comment", context, true)
                }

                next()
                if (peek() != '/' || isAtEnd())
                    Report.error(LEXER, "unterminated block comment", context, true)

                next(); next() // remaining * and /
            }
            else -> pushToken(TokenType.DIV)
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
            // We want an offset in string errors to point to the "missing" character
            val c = Context(context.line, context.column + 1, context.lineContent, context.file)
            Report.error(LEXER, "unterminated string", c, repl)
            return
        }

        next()
        pushToken(TokenType.STRING_LITERAL, result.toString())
    }

    private fun number(startDigit: Char, possibleHex: Boolean, returnString: Boolean = false): String? {
        val result = StringBuilder()
        val ishex = possibleHex && peek().toLowerCase() == 'x'
        if (ishex) {
            result.append("x")
            next()
            while (peek().isDigit() || peek().toLowerCase() in "abcdef") result.append(next())
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

        val type: TokenType?
        type = if (text !in kwds.keys) TokenType.IDENTIFIER
        else kwds[text]
        pushToken(type!!, result.toString())
    }

    private fun pushToken(type: TokenType, literal: String? = null) =
                    tokens.push(Token(literal, type, line, column - 1, filename, context.lineContent))

    private fun pushToken(type: TokenType, literal: Char) = pushToken(type, "%c".format(literal))

    private fun getNextToken() {
        when (val c = next()) {
            '(' -> pushToken(TokenType.LPAREN, c)
            ')' -> pushToken(TokenType.RPAREN, c)
            '{' -> pushToken(TokenType.LBRACE, c)
            '}' -> pushToken(TokenType.RBRACE, c)
            '[' -> pushToken(TokenType.LSQB, c)
            ']' -> pushToken(TokenType.RSQB, c)
            ',' -> pushToken(TokenType.COMMA, c)
            '-' -> pushToken(TokenType.MINUS, c)
            '+' -> pushToken(TokenType.PLUS, c)
            ';' -> pushToken(TokenType.SEMI, c)
            '*' -> pushToken(TokenType.MUL, c)
            ':' -> pushToken(TokenType.COLON, c)
            '@' -> pushToken(TokenType.AT, c)
            '#' -> pushToken(TokenType.HASHTAG, c)
            '$' -> pushToken(TokenType.VAR, c)
            '^' -> {
                if (match('^')) pushToken(TokenType.POW, "^^")
                else pushToken(TokenType.XOR, '^')
            }
            '.' -> {
                if (match('.')) {
                    if (match('.')) {
                        pushToken(TokenType.CONCAT_COMMA, "...")
                    } else pushToken(TokenType.CONCAT_SPACE, "..")
                } else pushToken(TokenType.DOT, '.')
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
                        TokenType.ASSIGN
                    }
                    else -> {
                        tok = "="
                        TokenType.EQ
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
                    else -> {
                        tok = ">"
                        TokenType.GT
                    }
                }

                pushToken(tt, tok)
            }
            '/' -> divOrComment()
            ' ', '\r' -> { /* ignore */ }
            '\t' -> { shiftInLine() }
            '\n' -> line++
            '"', '\'' -> string()
            in '0'..'9' -> number(c, c == '0')
            else -> {
                if (isAlpha(c)) identifier(c)
                else Report.error(LEXER, "unexpected character '$c'.", context, true)
            }
        }
    }

    fun getAllTokens() {
        while (!isAtEnd()) getNextToken()
        tokens.push(Token.Companion.NullToken(filename))
    }
}