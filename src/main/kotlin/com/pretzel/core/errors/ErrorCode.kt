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
    MISSING_TOKEN(false, "parser.missing"),

    INTERNAL(false, "pretzel.internal")
}

enum class ErrorCode(
    val message: String,
    val category: ErrorCategory,
    val severity: ErrorSeverity
) {
    // Keep this as the first code
    ASSERTED(
        "Internal error (please report to the maintainers of pretzel): This token was asserted to be here",
        ErrorCategory.INTERNAL,
        ErrorSeverity.FATAL
    ),

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
        "Member accessor token '.' or '..' expected",
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

    P_UNEXPECTED_TOKEN(
        "Unexpected token",
        ErrorCategory.P_UNEXPECTED_TOKEN,
        ErrorSeverity.ERROR,
    ),

    P_UNEXPECTED_TOKENS(
        "Unexpected tokens",
        ErrorCategory.P_UNEXPECTED_TOKEN,
        ErrorSeverity.ERROR,
    ),

    UNNECESSARY_COLON_IN_FUNCTION_PARAMETER(
        "Unnecessary colon between function parameter name and type",
        ErrorCategory.UNNECESSARY_TOKEN,
        ErrorSeverity.WARNING,
    ),

    EXPECTED_TOPLEVEL_DECL(
        "Expected top level declaration",
        ErrorCategory.P_UNEXPECTED_TOKEN,
        ErrorSeverity.ERROR,
    ),

    EXPECTED_VALUE(
        "Expected a value",
        ErrorCategory.P_UNEXPECTED_TOKEN,
        ErrorSeverity.ERROR,
    ),

    EXPECTED_CLOSING_PAREN(
        "Expected closing ')'",
        ErrorCategory.MISSING_SEPARATOR,
        ErrorSeverity.ERROR,
    ),

    EXPECTED_BLOCK_END(
        "Expected block end '}'",
        ErrorCategory.MISSING_SEPARATOR,
        ErrorSeverity.ERROR,
    ),

    EXPECTED_DICT_COLON(
        "Expected colon for dictionary value",
        ErrorCategory.MISSING_SEPARATOR,
        ErrorSeverity.ERROR,
    ),

    EXPECTED_DICT_BRACE(
        "Expected opening brace for dictionary literal",
        ErrorCategory.MISSING_TOKEN,
        ErrorSeverity.ERROR
    ),

    EXPECTED_LAMBDA_PARAMETER(
        "Expected lambda parameter name",
        ErrorCategory.P_UNEXPECTED_TOKEN,
        ErrorSeverity.ERROR,
    ),

    EXPECTED_LAMBDA_ARROW(
        "Expected '->' for lambda body",
        ErrorCategory.P_UNEXPECTED_TOKEN,
        ErrorSeverity.ERROR,
    ),

    EXPECTED_LIST_END(
        "Expected ']' to end a list",
        ErrorCategory.MISSING_SEPARATOR,
        ErrorSeverity.ERROR,
    ),
}

