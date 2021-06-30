package com.pretzel.core.parser

import com.pretzel.core.Util
import com.pretzel.core.errors.ErrorCode
import com.pretzel.core.lexer.Lexer
import com.pretzel.core.lexer.TokenStream
import com.pretzel.core.parser.ast.CodeNode
import kotlin.math.sign

/* An abstract parser for any language that uses the pretzel lexer. */
abstract class AbstractParser(
    val stream: TokenStream,
    val options: ParserOptions
) {
    class ParserCannotSyncException(
        val endPos: Int,
        val syncRange: List<Lexer.Token>
    ) :
        RuntimeException(Util.serializeObjectDict(syncRange))

    class IllegalParserInputException(message: String) : IllegalArgumentException(message)

    init {
        if (stream.length == 0) {
            throw IllegalParserInputException("Empty stream")
        }

        if (stream.errors > 0) {
            throw IllegalParserInputException("Erroneus input; lexer input must be correct")
        }
    }

    var state: Int = ParserState.Flag.INITIAL.value
    val flags: MutableList<ParserState.Flag> = mutableListOf()
    abstract fun nodeTypeLegalInThisContext(type: CodeNode.Type): Boolean
    abstract fun parseNextNode(): CodeNode?
    abstract val synchronizationPoints: List<Lexer.TokenType>
    var startPos: Lexer.Location = stream.seek().span.start
    val currentPos: Lexer.Location
        get() = stream.seek().span.end

    fun nodeTypeLegalForLanguageLevel(type: CodeNode.Type): Boolean {
        // for the future
        return true
    }

    fun featureLegalForLanguageLevel(feature: ParserOptions.Feature): Boolean {
        // for the future
        return true
    }

    protected fun rangeBegin(): Lexer.Location {
        return currentPos.also { startPos = currentPos }
    }

    protected fun makeSpan(
        start: Lexer.Location,
        end: Lexer.Location
    ): Lexer.Span {
        return Lexer.Span(
            start,
            end,
            stream.lexer.source.substring(start.pos, end.pos)
        )
    }

    protected fun makeSpanWithContent(
        start: Lexer.Location,
        end: Lexer.Location,
        content: String,
    ): Lexer.Span {
        return Lexer.Span(start, end, content)
    }

    protected fun skipUntil(
        start: Lexer.Location,
        tt: Lexer.TokenType
    ): Pair<Lexer.Span, Int> {
        var tokens = 0
        if (stream.seek().type == tt) {
            return makeSpan(start, start) to 0
        }

        while (stream.hasNext() && stream.seek().type != tt) {
            stream.next()
            tokens++
        }

        return makeSpanWithContent(
            start,
            currentPos,
            stream.lexer.source.lines()[start.line - 1]
        ) to tokens
    }

    protected fun skipAndErrorUntil(
        start: Lexer.Location,
        tt: Lexer.TokenType,
    ): Lexer.Span {
        val skipped = skipUntil(start, tt)
        options.reporter.error(
            if (skipped.second > 1) {
                ErrorCode.P_UNEXPECTED_TOKENS
            } else {
                ErrorCode.P_UNEXPECTED_TOKEN
            }, skipped.first
        )

        return skipped.first
    }

    fun pushStateFlag(flag: ParserState.Flag) {
        if ((flag.value and state) == 0) {
            return
        }

        flags.add(flag)
        state = state or flag.value
    }

    fun redactStateFlag(flag: ParserState.Flag) {
        if ((flag.value and state) != 0) {
            return
        }

        flags.remove(flag)
        state = state and flag.value.inv()
    }

    fun trySync(vararg moreSyncPoints: Lexer.TokenType): Pair<Boolean, List<Lexer.Token>> {
        val allSyncPoints = synchronizationPoints + moreSyncPoints
        val startIndex = stream.position
        var howMany = 0

        while (stream.seek().type !in allSyncPoints) {
            if (!stream.hasNext()) {
                rollback(howMany)
                return false to stream.tokens.subList(
                    startIndex,
                    stream.position
                )
            }

            howMany++
            stream.next()
        }

        return true to stream.tokens.subList(startIndex, stream.position)
    }

    fun rollback(nTokens: Int): Int {
        val target = stream.position - nTokens
        if (target.sign == -1) {
            throw IllegalArgumentException("rollback(): nTokens rolls back before 0")
        }

        while (target != stream.position) {
            stream.position--
        }

        return stream.position
    }

    fun accept(
        vararg type: Lexer.TokenType,
        code: ErrorCode,
        explanation: String? = null,
        offset: Int = 0,
        overEdge: Boolean = false,
    ): Lexer.Token {
        if (stream.seek().type !in type) {
            var lStartPos = startPos
            var lCurrentPos = currentPos
            // Report the error
            if (overEdge) {
                val lOffset = if (offset != 0) offset else 1
                lCurrentPos = Lexer.Location(
                    lCurrentPos.pos + lOffset,
                    lCurrentPos.line,
                    lCurrentPos.column + lOffset,
                    lCurrentPos.file,
                )

                lStartPos = lCurrentPos
            } else if (offset != 0) {
                lCurrentPos = Lexer.Location(
                    lCurrentPos.pos + offset,
                    lCurrentPos.line,
                    lCurrentPos.column + offset,
                    lCurrentPos.file,
                )

                lStartPos = Lexer.Location(
                    lStartPos.pos + offset,
                    lStartPos.line,
                    lStartPos.column + offset,
                    lStartPos.file,
                )
            }

            options.reporter.error(
                code,
                Lexer.Span(
                    lStartPos,
                    lCurrentPos,
                    stream.lexer.source,
                ),
                additionalInfo = explanation ?: "",
            )

            // try to synchronize
            var canSync = false
            var syncTokens: List<Lexer.Token>? = null
            if (options.keepGoing) {
                val result = trySync()
                canSync = result.first
                syncTokens = result.second
            }

            // If options.keepGoing = false, this will be executed
            // and not the if block above. If synchronization fails,
            // we will also end up here.
            if (!canSync && !options.keepGoing) {
                throw ParserCannotSyncException(
                    stream.position,
                    /* cannot be null */ syncTokens!!
                )
            }

            // Synchronization completed successfully and we can return from the point
            // after synchronization.
            return if (stream.hasNext()) stream.next() else stream.seek()
        }

        return stream.next()
    }

    init {
        pushStateFlag(ParserState.Flag.INITIAL)
    }
}