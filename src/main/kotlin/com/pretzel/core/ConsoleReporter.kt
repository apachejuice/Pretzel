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

class ConsoleReporter : Reporter {
    private var warningCalls: Int = 0
    private var errorCalls: Int = 0

    override var debug: Boolean = false
    override val errorCount: Int
        get() = errorCalls
    override val warningCount: Int
        get() = warningCalls
    override var columnOffset: Int = 0

    override fun error(errorType: ErrorType, message: String, fault: Lexer.Location, overEOF: Boolean) {
        errorCalls++
        val msg = """
                |Message: $message
                |line ${fault.line}:
                | | ${fault.lineContent}
                |  ${" ".repeat(columnOffset + fault.column + (if (overEOF) 1 else 0)) + "^"}
                |${if (debug) "Stack trace:\n" else ""}
            """.trimMargin()
        print(
            Ansi.ansi().fg(Ansi.Color.RED).bold().a("A ${errorType.format} occurred at ${fault.toString(columnOffset)}").newline()
                .reset().a(
                msg
            )
        )
        if (debug) prettyPrintStackTrace(Throwable().stackTrace)
    }

    private fun prettyPrintStackTrace(trace: Array<StackTraceElement>, prefix: String = "    ") {
        for (i in trace)
            println("$prefix$i")
    }

    override fun warning(errorType: ErrorType, message: String, fault: Lexer.Location) {
        warningCalls++
        val msg = """
                | | ${fault.lineContent}
                |${if (debug) "Stack trace:\n" else ""}
            """.trimMargin()
        print(
            Ansi.ansi().bold().fg(Ansi.Color.MAGENTA)
                .a("Warning near ${fault.line}:${fault.column} at ${fault.file}: $message").newline().reset().a(msg)
        )
        if (debug) prettyPrintStackTrace(Throwable().stackTrace)
    }
}
