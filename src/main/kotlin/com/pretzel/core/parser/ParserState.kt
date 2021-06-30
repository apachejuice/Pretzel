package com.pretzel.core.parser

class ParserState {
    enum class Flag(val value: Int) {
        INITIAL(1 shl 0),
        STRICT(1 shl 1),
        FUNCTION(1 shl 2),
    }
}
