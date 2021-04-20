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

package com.pretzel.core.parser

import com.pretzel.core.errors.ErrorCode
import com.pretzel.core.errors.Reporter
import com.pretzel.core.errors.StreamReporter
import com.pretzel.core.lexer.Lexer
import com.pretzel.core.lexer.Lexer.Token
import com.pretzel.core.lexer.Lexer.TokenType
import com.pretzel.core.lexer.TokenStream
import com.pretzel.core.parser.ast.Node

abstract class Parser<T : Node>(protected val stream: TokenStream) {
    data class ParserError(val msg: String, val token: Token, val parser: Parser<*>) :
        Exception(msg)

    protected val reporter: Reporter = StreamReporter(System.err)

    abstract fun parse(): T
    var synchronize: Boolean = true
    var errorCallback: (token: Token) -> Unit = { _: Token -> }
    var warningCallback: (token: Token) -> Unit = { _: Token -> }

    protected var begin: Lexer.Span = stream.seek().span
    protected val currentSpan: Lexer.Span
        get() = stream.seek().span

    protected fun internalInitParse(synchronize: Boolean = true) {
        begin = currentSpan
        this.synchronize = synchronize
    }

    protected fun joinSpan(a: Lexer.Span, b: Lexer.Span): Lexer.Span = Lexer.Span(
        a.start,
        b.end,
        // We don't care about the content here anymore
        "",
        ""
    )


    protected fun followingSpan(
        baseSpan: Lexer.Span,
        startColumn: Int,
        endColumn: Int
    ): Lexer.Span = Lexer.Span(
        Lexer.Location(
            baseSpan.start.pos,
            baseSpan.start.line,
            startColumn,
            baseSpan.start.file,
        ),

        Lexer.Location(
            baseSpan.end.pos,
            baseSpan.end.line,
            endColumn,
            baseSpan.end.file,
        ),

        baseSpan.content,
        baseSpan.lineContent,
    )

    protected fun accept(
        vararg type: TokenType,
        whenError: ErrorCode,
        startColumn: Int = 0,
        endColumn: Int = 0
    ): Token {
        return if (stream.isNext(*type)) {
            stream.next()
        } else {
            reporter.error(
                whenError,
                followingSpan(
                    stream.seek().span,
                    if (startColumn != 0) startColumn else currentSpan.start.column,
                    if (endColumn != 0) endColumn else currentSpan.end.column
                )
            )
            errorCallback(stream.seek())
            if (!synchronize) {
                throw ParserError(whenError.message, stream.seek(), this)
            }

            stream.seek()
        }
    }

    protected fun acceptShift(
        vararg type: TokenType,
        whenError: ErrorCode,
        startShift: Int = 1,
        endShift: Int = 1,
    ): Token = accept(
        *type,
        whenError = whenError,
        startColumn = currentSpan.lineContent.length + startShift,
        endColumn = currentSpan.lineContent.length + endShift
    )
}