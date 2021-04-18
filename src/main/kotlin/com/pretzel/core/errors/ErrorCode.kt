package com.pretzel.core.errors

enum class ErrorSeverity {
    NOTE,
    WARNING,
    ERROR,
    FATAL,
}

enum class ErrorCode(val message: String, val category: String) {
    EXPECTED_NSNAME("Expected namespace name", "UnexpectedToken"),
    EXPECTED_ID("Identifier expected", "UnexpectedToken"),
    EXPECTED_MEMBER_ACCESSOR("Member accessor token ':' expected", "UnexpectedToken"),

    // Keep this as the last code
    ASSERTED("Internal error: this token is conflicting with its context", "Internal"),
}