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

package com.pretzel

import com.pretzel.core.lexer.Lexer
import com.pretzel.core.lexer.TokenStream
import com.pretzel.core.parser.Parser

class Main {
    companion object {
        @JvmStatic
        fun main(vararg args: String) {
            runRepl(*args)
        }

        @JvmStatic
        fun runRepl(vararg args: String) {
            val l = Lexer("() + {]} 345 -2 5/2 ", Lexer.SourceMode.DIRECT)
            Parser(TokenStream.open(l))
        }
    }
}