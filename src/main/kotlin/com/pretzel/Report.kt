package com.pretzel


import org.jetbrains.annotations.Contract
import kotlin.system.exitProcess

class Report {
    companion object {
        @Contract("-> halt")
        fun error(errorType: ErrorType, message: String, context: Lexer.Context) {
            val msg = """
                |A ${errorType.format} occurred at $context
                |Message: $message
                |line ${context.line}:
                |${context.lineContent}
                |${" ".repeat(context.column - 1) + "^"}
            """.trimMargin()
            println(msg)
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