package com.pretzel.core

import com.pretzel.core.lexer.Lexer
import org.jetbrains.annotations.Contract


class Report {
    companion object {
        var debug: Boolean = false
        @Contract("_, _, _, false -> halt")
        fun error(errorType: ErrorType, message: String, faultyToken: Lexer.Token) {
            val msg = """
                |A ${errorType.format} occurred at ${faultyToken.toContext()}
                |Message: $message
                |line ${faultyToken.line}:
                | | ${faultyToken.lineContent}
                |  ${" ".repeat(faultyToken.column) + "^"}
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

    val exitCode: Int
        get () {
            return when (this) {
                LEXER -> 20
                PARSER -> 21
                AST -> 22
            }
        }
}