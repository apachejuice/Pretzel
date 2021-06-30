package com.pretzel.core.parser

import com.pretzel.core.errors.ErrorCode
import com.pretzel.core.lexer.Lexer
import com.pretzel.core.lexer.TokenStream
import com.pretzel.core.parser.ast.Block
import com.pretzel.core.parser.ast.CodeNode
import com.pretzel.core.parser.ast.LocalVariableDeclaration
import com.pretzel.core.parser.ast.ModuleDeclaration
import com.pretzel.core.parser.ast.Symbol
import com.pretzel.core.parser.ast.expr.SymbolReference
import com.pretzel.core.parser.ast.UseStatement
import com.pretzel.core.parser.ast.VariableAssignment
import com.pretzel.core.parser.ast.VariableDeclaration
import com.pretzel.core.parser.ast.WildcardSymbol
import com.pretzel.core.parser.ast.expr.Expression
import com.pretzel.core.parser.ast.expr.LambdaExpression

/* A common base class for the pretzel parsers. */
abstract class CommonParser(stream: TokenStream, options: ParserOptions) :
    AbstractParser(stream, options) {
    override fun nodeTypeLegalInThisContext(type: CodeNode.Type): Boolean {
        return true
    }

    protected val lookahead: ParserLookahead = ParserLookahead(options)

    override val synchronizationPoints: List<Lexer.TokenType>
        get() = listOf(
            Lexer.TokenType.FUNC,
            Lexer.TokenType.IF,
            Lexer.TokenType.ELSE,
            Lexer.TokenType.ELIF,
            Lexer.TokenType.FOR,
            Lexer.TokenType.FROM,
            Lexer.TokenType.MOD,
            Lexer.TokenType.LBRACE,
            Lexer.TokenType.USE,
            Lexer.TokenType.WHILE,
            Lexer.TokenType.VAR,
            Lexer.TokenType.WHEN,
        )

    protected fun parseModuleDeclaration(): ModuleDeclaration {
        val start = rangeBegin()
        accept(Lexer.TokenType.MOD, code = ErrorCode.ASSERTED)
        val nsname = parseSymbol()

        requireSemi()
        return ModuleDeclaration(nsname, makeSpan(start, currentPos))
    }

    protected fun requireSemi() {
        skipUntil(currentPos, Lexer.TokenType.SEMI)
        accept(Lexer.TokenType.SEMI, code = ErrorCode.EXPECTED_SEMI, overEdge = true)
    }

    protected fun parseUseStatement(): UseStatement {
        val start = rangeBegin()
        accept(Lexer.TokenType.USE, code = ErrorCode.ASSERTED)
        val nsname = parseSymbol()

        requireSemi()
        return UseStatement(nsname, makeSpan(start, currentPos))
    }

    protected fun parseBlock(): Block {
        val start = rangeBegin()
        accept(Lexer.TokenType.LBRACE, code = ErrorCode.ASSERTED)
        val result = mutableListOf<CodeNode?>()
        while (stream.hasNext() && stream.seek().type != Lexer.TokenType.RBRACE
            && stream.seek().type != Lexer.TokenType.EOF
        ) {
            val n = parseNextNode()
            if (n == null) {
                trySync()
            }

            result.add(n)
        }

        accept(Lexer.TokenType.RBRACE, code = ErrorCode.EXPECTED_BLOCK_END, overEdge = true)
        return Block(result, makeSpan(start, currentPos))
    }

    protected fun parseSymbol(canBeWildcard: Boolean = false): Symbol {
        val start = rangeBegin()
        val sym =
            mutableListOf(parseSymbolPart(ErrorCode.ASSERTED))
        if (stream.acceptIfNext(Lexer.TokenType.DOT)) {
            while (true) {
                sym.add(parseSymbolPart(ErrorCode.EXPECTED_NSNAME))
                if (!stream.acceptIfNext(Lexer.TokenType.DOT)) {
                    break
                } else if (canBeWildcard && stream.acceptIfNext(Lexer.TokenType.MUL)) {
                    return WildcardSymbol(sym, makeSpan(start, currentPos))
                }
            }
        }

        return Symbol(sym, makeSpan(start, currentPos))
    }

    protected fun parseExpression(uncontextual: Boolean = false): Expression? {
        return ExpressionParser(stream, options).parse().also {
            if (it == null) {
                trySync()
            } else if (uncontextual) {
                // Usually synchronization makes a mess here so we just shift by one if there is no semicolon.
                if (!stream.acceptIfNext(Lexer.TokenType.SEMI)) {
                    options.reporter.error(ErrorCode.EXPECTED_SEMI, makeSpan(it.sourceRange.end, it.sourceRange.end))
                    stream.advance()
                    println(stream.seek())
                }
            }
        }
    }

    protected fun parseSymbolPart(code: ErrorCode): SymbolReference {
        val start = rangeBegin()
        val name = accept(Lexer.TokenType.IDENTIFIER, code = code).lexeme
        return SymbolReference(name, makeSpan(start, currentPos))
    }

    protected fun parseVariableDeclaration(hasAssignment: Boolean): VariableDeclaration {
        val start = rangeBegin()
        accept(Lexer.TokenType.VAR, code = ErrorCode.ASSERTED)
        val id =
            accept(Lexer.TokenType.IDENTIFIER, code = ErrorCode.EXPECTED_ID)
        val sym = SymbolReference(id.lexeme, id.span)

        if (!hasAssignment) {
            requireSemi()
            // in context agnostic parsing all variables on top level are treated as local
            return LocalVariableDeclaration(sym, listOf(), makeSpan(start, currentPos), null)
        } else {
            val eqStart = accept(Lexer.TokenType.ASSIGN, code = ErrorCode.ASSERTED).span.start
            val value = parseExpression()
            return if (value == null) {
                trySync()
                LocalVariableDeclaration(sym, listOf(), makeSpan(start, currentPos), null)
            } else {
                requireSemi()
                LocalVariableDeclaration(
                    sym, listOf(), makeSpan(start, currentPos),
                    VariableAssignment(sym.toSymbol(), makeSpan(eqStart, currentPos), value)
                )
            }
        }
    }
}