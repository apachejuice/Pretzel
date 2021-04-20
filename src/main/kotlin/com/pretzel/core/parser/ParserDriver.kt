package com.pretzel.core.parser

import com.pretzel.core.lexer.TokenStream
import com.pretzel.core.parser.ast.Node

interface ParserDriver {
    fun execute(stream: TokenStream): Node
}