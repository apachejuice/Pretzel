package com.pretzel.core.parser

import com.pretzel.core.parser.ast.CodeNode

interface FullInputParser {
    fun parse(): CodeNode?
}