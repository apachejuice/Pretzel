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

package com.pretzel.core

import com.pretzel.core.lexer.Lexer
import org.jetbrains.annotations.Contract


class Report {
    companion object {
        var debug: Boolean = false
        @Contract("_, _, _, false -> halt")
        fun error(errorType: ErrorType, message: String, faultyToken: Lexer.Token, overEOF: Boolean = false) {
            val msg = """
                |A ${errorType.format} occurred at ${faultyToken.toContext()}
                |Message: $message
                |line ${faultyToken.line}:
                | | ${faultyToken.lineContent}
                |  ${" ".repeat(faultyToken.column + (if (overEOF) 1 else 0)) + "^"}
                |${if (debug) "Stack trace:\n" else ""}
            """.trimMargin()
            print(msg)
            if (debug) prettyPrintStackTrace(Throwable().stackTrace)
        }

        private fun prettyPrintStackTrace(trace: Array<StackTraceElement>, prefix: String = "    ") {
            for (i in trace)
                println("$prefix$i")
        }
    }
}

enum class ErrorType {
    LEXER,
    PARSER,
    AST;

    val format: String
        get () {
            return when (this) {
                LEXER -> "lexical error"
                PARSER, AST -> "syntax error"
            }
        }
}