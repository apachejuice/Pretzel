/*
 * Copyright 2020 Valio Valtokari
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

import com.pretzel.core.ErrorType.PARSER
import com.pretzel.core.Report

import java.lang.IndexOutOfBoundsException
import java.util.Objects
import java.util.Spliterator
import java.util.function.Consumer

class TokenStream private constructor(tokens: MutableList<Lexer.Token>) : Iterable<Lexer.Token?> {
    val tokens: MutableList<Lexer.Token>
        get() = _tokens

    /**
     * Returns the lexer used by this instance of Lexer.TokenStream.
     *
     * @return The lexer used by this Lexer.TokenStream
     */
    lateinit var lexer: Lexer
    private lateinit var _tokens: MutableList<Lexer.Token>
    private var idx: Int
    val length: Int
        get() = tokens.size

    var position: Int
        get() = idx
        set(value) { idx = value }

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

        Report.error(PARSER, applicant.joinToString(separator = ", ") { it }, token.toContext(), false)
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
                    PARSER, "expected symbol of type '$s', got '$tt'", token.toContext(), false
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
        if (!hasNext()) return false
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
            if (tt == s) {
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
    fun next(): Lexer.Token {
        if (idx >= tokens.size) throw IndexOutOfBoundsException("idx >= tokens.size")
        return tokens[idx++]
    }

    /**
     * Returns the current token in the stream.
     *
     * @return The current token in the stream.
     */
    fun seek(): Lexer.Token {
        if (idx > tokens.size) throw IndexOutOfBoundsException("idx > tokens.size")
        if (idx >= tokens.size) return tokens[tokens.indices.last]
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
                ), token.toContext(), false
            )
        }
        return token
    }

    override fun toString(): String {
        return "$tokens:$position"
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
            return open(lx)
        }

        fun fromTokens(tokens: MutableList<Lexer.Token>): TokenStream {
            return TokenStream(tokens)
        }
    }

    init {
        this._tokens = tokens
        if (tokens.isNotEmpty()) {
            val last = tokens.last()
            _tokens.add(
                Lexer.Token(
                    "",
                    Lexer.TokenType.EOF,
                    last.line,
                    last.column + 1,
                    last.file,
                    last.lineContent
                )
            )
        }
        idx = 0
    }
}