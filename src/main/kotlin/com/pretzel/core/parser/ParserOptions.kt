package com.pretzel.core.parser

import com.pretzel.core.errors.Reporter

data class ParserOptions(
    val keepGoing: Boolean,
    val languageLevel: LanguageLevel,
    val reporter: Reporter
) {
    enum class LanguageLevel(val level: Float) {
        PRETZEL_0_1(0.1f),
    }

    enum class Feature(val availableInLevel: LanguageLevel)
}
