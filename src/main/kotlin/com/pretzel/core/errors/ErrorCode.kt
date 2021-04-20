package com.pretzel.core.errors

enum class ErrorSeverity {
    NOTE,
    WARNING,
    ERROR,
    FATAL,
}

enum class ErrorCategory(
    val suppressable: Boolean,
    val errorName: String
) {
    P_UNEXPECTED_TOKEN(false, "parser.unexpected"),
    L_UNEXPECTED_TOKEN(false, "lexer.unexpected"),
    MISSING_SEPARATOR(false, "parser.missingSeparator"),
    UNNECESSARY_TOKEN(true, "parser.unnecessary"),

    INTERNAL(false, "pretzel.internal"),
}

enum class ErrorCode(
    val message: String,
    val category: ErrorCategory,
    val severity: ErrorSeverity
) {
    EXPECTED_NSNAME(
        "Expected namespace name",
        ErrorCategory.P_UNEXPECTED_TOKEN,
        ErrorSeverity.ERROR
    ),
    EXPECTED_ID(
        "Identifier expected",
        ErrorCategory.P_UNEXPECTED_TOKEN,
        ErrorSeverity.ERROR
    ),
    EXPECTED_MEMBER_ACCESSOR(
        "Member accessor token ':' expected",
        ErrorCategory.P_UNEXPECTED_TOKEN,
        ErrorSeverity.ERROR
    ),

    EXPECTED_SEMI(
        "Expression/statement delimiter ';' missing",
        ErrorCategory.MISSING_SEPARATOR,
        ErrorSeverity.ERROR,
    ),

    UNEXPECTED_EOF(
        "Unexpected EOF while parsing",
        ErrorCategory.P_UNEXPECTED_TOKEN,
        ErrorSeverity.ERROR
    ),

    UNEXPECTED_TOKEN(
        "Unexpected token",
        ErrorCategory.L_UNEXPECTED_TOKEN,
        ErrorSeverity.ERROR
    ),

    UNNECESSARY_COLON_IN_FUNCTION_PARAMETER(
        "Unnecessary colon between function parameter name and type",
        ErrorCategory.UNNECESSARY_TOKEN,
        ErrorSeverity.WARNING,
    ),

    // Keep this as the last code
    ASSERTED(
        "Internal error (please report to the maintainers of pretzel):",
        ErrorCategory.INTERNAL,
        ErrorSeverity.FATAL
    ),
}

