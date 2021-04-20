package com.pretzel.core.errors

import com.pretzel.core.lexer.Lexer
import org.fusesource.jansi.Ansi

import java.io.OutputStream
import java.io.PrintStream
import java.lang.IllegalArgumentException
import kotlin.math.max
import kotlin.streams.toList

class StreamReporter(val outputStream: PrintStream) : Reporter {
    private var _errorCount: Int = 0
    private var _warningCount: Int = 0
    private var _noteCount: Int = 0
    private var ansi: Ansi = Ansi.ansi()

    private val errorCodes: List<ErrorCode>
    private val warningCodes: List<ErrorCode>
    private val noteCodes: List<ErrorCode>
    private val fatalCodes: List<ErrorCode>

    init {
        var stream = ErrorCode.values().toList().stream()
        errorCodes = stream.filter { it.severity == ErrorSeverity.ERROR }.toList()
        stream = ErrorCode.values().toList().stream()
        warningCodes = stream.filter { it.severity == ErrorSeverity.WARNING }.toList()
        stream = ErrorCode.values().toList().stream()
        noteCodes = stream.filter { it.severity == ErrorSeverity.NOTE }.toList()
        stream = ErrorCode.values().toList().stream()
        fatalCodes = stream.filter { it.severity == ErrorSeverity.FATAL }.toList()
    }

    private fun writeStringColored(s: String, color: Ansi.Color, bold: Boolean = false) {
        if (bold)
            ansi.bold()
        writeString(ansi.fg(color).a(s).reset().toString())
        ansi = Ansi.ansi()
    }

    @Suppress("NON_EXHAUSTIVE_WHEN")
    private fun redirectCode(code: ErrorCode, span: Lexer.Span, additionalInfo: String) {
        when (code) {
            in errorCodes -> {
                _error(code, span, additionalInfo)
            }

            in warningCodes -> {
                _warning(code, span, additionalInfo)
            }

            in fatalCodes -> {
                _fatal(code, span, additionalInfo)
            }

            else -> throw IllegalArgumentException(code.toString())
        }
    }

    private fun writeString(s: String) {
        outputStream.println(s)
    }

    private fun formatSpan(span: Lexer.Span): String {
        return "${span.start.line}:${span.start.column} - ${span.end.line}:${span.end.column}"
    }

    private fun formatErrorTypeReport(code: ErrorCode): String {
        val suppressionSuggestion =
            if (code.category.suppressable) " (Suppress with -Wno-${code.category.errorName})" else ""
        return "\n${getCodeFlag(code)}$suppressionSuggestion"
    }

    private fun getCodeFlag(code: ErrorCode): String {
        return when (code.severity) {
            ErrorSeverity.FATAL, ErrorSeverity.ERROR -> ""
            ErrorSeverity.WARNING -> {
                "-W${code.category.errorName}"
            }

            ErrorSeverity.NOTE -> {
                "-N"
            }
        }
    }

    private fun getNumericCode(code: ErrorCode): String {
        return String.format("%04d", code.ordinal)
    }

    private fun writeThoseSquigglyThingsUnderTheError(span: Lexer.Span) {
        val diff = span.end.column - span.start.column

        val squiggles = when {
            diff < 1 -> ""
            diff == 2 -> "^"
            else -> "^" + "~".repeat(diff - 2)
        }

        writeString("  ${" ".repeat(span.start.column - 1)}$squiggles^")
    }

    private fun formatAdditionalInfo(additionalInfo: String): String =
        if (additionalInfo.isBlank()) "" else " $additionalInfo"

    private fun presentErroneusLine(span: Lexer.Span) {
        writeString("line ${max(span.start.line, span.end.line)}: ")
        writeString("> ${span.lineContent}")
        writeThoseSquigglyThingsUnderTheError(span)
    }

    private fun _error(code: ErrorCode, span: Lexer.Span, additionalInfo: String) {
        writeStringColored(
            "An error occured at file ${span.end.file} ${formatSpan(span)}",
            Ansi.Color.RED, true
        )

        writeString(
            "PZE${getNumericCode(code)}: ${code.message}${
                formatAdditionalInfo(
                    additionalInfo
                )
            } ${
                formatErrorTypeReport(
                    code
                )
            }"
        )
        presentErroneusLine(span)
    }

    private fun _warning(code: ErrorCode, span: Lexer.Span, additionalInfo: String) {
        writeStringColored(
            "Warning in file ${span.start.file}${formatAdditionalInfo(additionalInfo)} ${
                formatSpan(
                    span
                )
            }",
            Ansi.Color.YELLOW
        )

        writeString(
            "PZW${getNumericCode(code)}: ${code.message} ${
                formatErrorTypeReport(
                    code
                )
            }"
        )

        presentErroneusLine(span)
    }

    private fun _note(span: Lexer.Span, additionalInfo: String) {
        writeStringColored("Note: $additionalInfo", Ansi.Color.BLUE)
        presentErroneusLine(span)
    }

    private fun _fatal(code: ErrorCode, span: Lexer.Span, additionalInfo: String) {
        writeStringColored(
            "A fatal error occured at file ${span.end.file}${
                formatAdditionalInfo(
                    additionalInfo
                )
            } ${formatSpan(span)}",
            Ansi.Color.WHITE, true
        )

        writeString(
            "PZE${getNumericCode(code)}: ${code.message} $additionalInfo ${
                formatErrorTypeReport(
                    code
                )
            }"
        )
        presentErroneusLine(span)
    }

    override fun error(code: ErrorCode, span: Lexer.Span, additionalInfo: String) {
        redirectCode(code, span, additionalInfo)
    }

    override fun warning(code: ErrorCode, span: Lexer.Span, additionalInfo: String) {
        redirectCode(code, span, additionalInfo)
    }

    override fun note(span: Lexer.Span, additionalInfo: String) {
        _note(span, additionalInfo)
    }

    override fun fatal(code: ErrorCode, span: Lexer.Span, additionalInfo: String) {
        redirectCode(code, span, additionalInfo)
    }

    override val errorCount: Int
        get() = _errorCount

    override val warningCount: Int
        get() = _warningCount

    override val noteCount: Int
        get() = _noteCount
}