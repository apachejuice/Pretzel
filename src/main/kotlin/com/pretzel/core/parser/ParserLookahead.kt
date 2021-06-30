package com.pretzel.core.parser

import com.pretzel.core.errors.ErrorCode
import com.pretzel.core.lexer.Lexer
import com.pretzel.core.lexer.TokenStream
import com.pretzel.core.parser.ast.CodeNode
import com.pretzel.core.parser.ast.expr.Expression
import kotlin.system.exitProcess

class ParserLookahead(private val options: ParserOptions) {
    lateinit var lookaheadParser: ContextAgnosticParser

    // Declaration ::= "let" <identifier> ';'
    // DeclarationAssignment ::= "let" <indentifier> "=" <expression> ';'
    fun isDeclarationAssignment(stream: TokenStream): Boolean {
        lookaheadParser = ContextAgnosticParser(stream, options)
        val start = stream.position

        // We expect to start at `let`
        lookaheadParser.accept(Lexer.TokenType.VAR, code = ErrorCode.ASSERTED)

        if (stream.seek().type == Lexer.TokenType.IDENTIFIER) {
            stream.advance()
            return stream.isNext(Lexer.TokenType.ASSIGN)
                .also { stream.position = start }
        }

        stream.position = start
        return false
    }

    // Parse the next node from the stream.
    fun parseNextNode(stream: TokenStream): CodeNode? =
        ContextAgnosticParser(stream, options).parseNextNode()
}