package com.pretzel.core.errors

import com.pretzel.core.lexer.Lexer

interface Reporter {
    fun error(code: ErrorCode, span: Lexer.Span, additionalInfo: String = "")
    fun warning(code: ErrorCode, span: Lexer.Span, additionalInfo: String = "")
    fun note(span: Lexer.Span, additionalInfo: String)
    fun fatal(code: ErrorCode, span: Lexer.Span, additionalInfo: String = "")
    val errorCount: Int
    val warningCount: Int
    val noteCount: Int
}