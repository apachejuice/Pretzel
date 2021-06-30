package com.pretzel.core.parser

import com.pretzel.core.doIf
import com.pretzel.core.lexer.Lexer
import com.pretzel.core.lexer.TokenStream
import com.pretzel.core.parser.ast.CodeNode

class ContextAgnosticParser(
    stream: TokenStream,
    options: ParserOptions
) : CommonParser(stream, options), FullInputParser {
    override fun parseNextNode(): CodeNode? {
        val start = rangeBegin()

        return when (val tt = stream.seek().type) {
            Lexer.TokenType.USE -> parseUseStatement()
            Lexer.TokenType.MOD -> parseModuleDeclaration()
            Lexer.TokenType.VAR -> parseVariableDeclaration(lookahead.isDeclarationAssignment(stream))
            Lexer.TokenType.SEMI -> null
            Lexer.TokenType.EOF -> null
            else -> parseExpression(true)
        }
    }

    override fun parse(): CodeNode? {
        val nodes = mutableListOf<CodeNode>()
        val start = rangeBegin()

        while (stream.hasNext() && stream.seek().type != Lexer.TokenType.EOF) {
            val node = parseNextNode()
            if (node != null) {
                nodes.add(node)
            } else {
                break
            }
        }

        return nodes.isEmpty().doIf({ null }, {
            CodeNode.RootNode(nodes, makeSpan(start, currentPos))
        })
    }
}