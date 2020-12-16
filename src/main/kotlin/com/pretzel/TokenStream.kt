package com.pretzel

import com.pretzel.ErrorType.PARSER
import java.util.Objects
import java.util.Spliterator
import java.util.function.Consumer

class TokenStream(tokens: MutableList<Lexer.Token>) : Iterable<Lexer.Token?> {
    /**
     * Returns the lexer used by this instance of Lexer.TokenStream.
     *
     * @return The lexer used by this Lexer.TokenStream
     */
    var lexer: Lexer? = null
    private val tokens: MutableList<Lexer.Token>
    private var idx: Int

    /**
     * Accepts a token at the current index.
     *
     * @param applicant Acceptable tokens
     * @return The accepted token
     */
    fun accept(vararg applicant: String): Lexer.Token {
        val token = tokens[idx]
        for (v in applicant) {
            if (v == token.lexeme) {
                idx++
                return token
            }
        }

        Report.error(PARSER, applicant.joinToString(separator = ", ") { it }, token.toContext())
        return Lexer.Token.Companion.NullToken("")
    }

    /**
     * Accepts a symbol type at the current index.
     *
     * @param applicant Acceptable symbol type
     * @return The accepted token
     */
    fun accept(vararg applicant: Lexer.TokenType): Lexer.Token {
        val token = tokens[idx++]
        val tt = token.type
        for (s in applicant) {
            if (tt !== s) {
                Report.error(
                    PARSER, "ttbol of type '$s', got '$tt'", token.toContext()
                )
            }
        }
        return token
    }

    /**
     * Accepts a token at a current index IF it exists.
     *
     * @param applicant Acceptable tokens
     * @return If the token was the next one
     */
    fun acceptIfNext(vararg applicant: String): Boolean {
        val token = tokens[idx]
        for (s in applicant) {
            if (token.`is`(s)) {
                idx++
                return true
            }
        }
        return false
    }

    /**
     * Increments the index counter by one.
     */
    fun advance() {
        idx++
    }

    /**
     * Accepts a symbol type at a current index IF it exists.
     *
     * @param applicant Acceptable symbol types
     * @return If the Lexer.TokenType type was the next one
     */
    fun acceptIfNext(vararg applicant: Lexer.TokenType): Boolean {
        val ttbol = tokens[idx].type
        for (tt in applicant) {
            if (ttbol === tt) {
                idx++
                return true
            }
        }
        return false
    }

    /**
     * Does the stream have more tokens?
     *
     * @return Whether the stream has tokens left
     */
    operator fun hasNext(): Boolean {
        return idx < tokens.size
    }

    /**
     * Return whether the next token equals to
     * one of the applicants' given.
     *
     * @return `true` or `false`
     */
    fun isNext(vararg applicant: String): Boolean {
        if (!hasNext()) return false
        val token: Lexer.Token = tokens[idx]
        for (s in applicant) {
            if (token.`is`(s)) {
                return true
            }
        }
        return false
    }

    /**
     * Return whether the next token's symbol type
     * equals to one of the applicant symbol types given.
     *
     * @return `true` or `false`
     */
    fun isNext(vararg applicant: Lexer.TokenType): Boolean {
        if (!hasNext()) return false
        val tt: Lexer.TokenType = tokens[idx].type
        for (s in applicant) {
            if (tt === s) {
                return true
            }
        }
        return false
    }

    /**
     * Returns the next token in the stream.
     *
     * @return The next token in the stream.
     */
    operator fun next(): Lexer.Token {
        return tokens[++idx]
    }

    /**
     * Returns the current token in the stream.
     *
     * @return The current token in the stream.
     */
    fun seek(): Lexer.Token {
        return tokens[idx]
    }

    /**
     * Returns the current token in the stream as a string.
     *
     * @return The current token in the stream as a string.
     */
    fun seekString(): String? {
        return tokens[idx].lexeme
    }

    /**
     * Accepts the next token if it is a valid identifier.
     *
     * @return The accepted identifier;
     */
    fun acceptIdentifier(): Lexer.Token {
        val token = tokens[idx++]
        if (!token.lexeme!!.matches(Regex("[_a-zA-Z][_a-zA-Z0-9]+ | _+"))) {
            Report.error(
                PARSER,
                java.lang.String.format(
                    "expected identifier, got '%s'",
                    token.lexeme
                ), token.toContext()
            )
        }
        return token
    }

    override fun toString(): String {
        return tokens.toString()
    }

    override fun iterator(): MutableIterator<Lexer.Token> {
        return tokens.iterator()
    }

    override fun forEach(action: Consumer<in Lexer.Token?>) {
        Objects.requireNonNull(action)
        for (t in this)
            action.accept(t)
    }

    override fun spliterator(): Spliterator<Lexer.Token?> {
        return tokens.spliterator()
    }

    companion object {
        /**
         * Opens a Lexer.TokenStream with the specified Lexer instance.
         *
         * @return A new Lexer.TokenStream instance
         */
        fun open(lexer: Lexer): TokenStream {
            // ensure that lexer is in start state
            lexer.clean()
            lexer.getAllTokens()
            val ts = TokenStream(lexer.tokens)
            ts.lexer = lexer
            return ts
        }

        /**
         * Opens a Lexer.TokenStream with the specified filename.
         *
         * @return A new Lexer.TokenStream instance
         */
        fun open(file: String): TokenStream {
            val lx = Lexer(file, Lexer.SourceMode.FILE)
            lx.getAllTokens()
            return TokenStream(lx.tokens)
        }
    }

    init {
        tokens.removeAt(tokens.size - 1)
        this.tokens = tokens
        idx = 0
    }
}