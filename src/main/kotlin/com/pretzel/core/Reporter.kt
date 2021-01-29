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

package com.pretzel.core

import com.pretzel.core.lexer.Lexer
import org.fusesource.jansi.Ansi


interface Reporter {
    var debug: Boolean
    val errorCount: Int
    val warningCount: Int
    fun error(errorType: ErrorType, message: String, fault: Lexer.Location, overEOF: Boolean = false)
    fun warning(errorType: ErrorType, message: String, fault: Lexer.Location)
}

enum class ErrorType {
    LEXER,
    PARSER,
    AST;

    val format: String
        get() {
            return when (this) {
                LEXER -> "lexical error"
                PARSER, AST -> "syntax error"
            }
        }
}