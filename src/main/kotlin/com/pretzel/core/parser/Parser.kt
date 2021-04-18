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
import com.pretzel.core.lexer.Lexer.Token
import com.pretzel.core.lexer.Lexer.TokenType
import com.pretzel.core.lexer.TokenStream

abstract class Parser protected constructor(private val stream: TokenStream) {
    data class ParserError(val msg: String, val token: Token, val parser: Parser) : Exception(msg)

    abstract fun parse(stream: TokenStream, limit: Int)
    var synchronize: Boolean = true
    var errorCallback: (token: Token) -> Unit = { _: Token -> }
    var warningCallback: (token: Token) -> Unit = { _: Token -> }

    protected fun accept(vararg type: TokenType, whenError: ErrorCode): Token {
        return if (stream.isNext(*type)) {
            stream.next()
        } else {
            errorCallback(stream.seek())
            if (!synchronize) {
                throw ParserError(whenError.message, stream.seek(), this)
            }

            stream.seek()
        }
    }
}