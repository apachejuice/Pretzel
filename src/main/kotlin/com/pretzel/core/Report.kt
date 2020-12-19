package com.pretzel.core


import com.pretzel.core.lexer.Lexer
import org.jetbrains.annotations.Contract
import kotlin.system.exitProcess

class Report {
    companion object {
        var debug: Boolean = false

        @Contract("_, _, _, false -> halt")
        fun error(errorType: ErrorType, message: String, context: Lexer.Context, repl: Boolean) {
            val msg = """
                |A ${errorType.format} occurred at $context
                |Message: $message
                |line ${context.line} ${if (debug) "in class " + Throwable().stackTrace[3] else ""}:
                | | ${context.lineContent}
                |   ${" ".repeat(context.column) + "^"}
            """.trimMargin()
            println(msg)

            if (!repl)
                exitProcess(errorType.exitCode)
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