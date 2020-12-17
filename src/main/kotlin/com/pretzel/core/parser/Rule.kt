package com.pretzel.core.parser

import com.pretzel.core.lexer.Lexer
import com.pretzel.core.lexer.TokenStream
import java.util.Collections


class Rule(var queue: List<Match>) {
    val minLength: Int
        get() {
            var result = 0
            for (i in queue)
                if (i !is Substitute) result++

            return result
        }

    val maxLength: Int
        get() = queue.size

    val varies: Int
        get() = maxLength - minLength

    val length: Int
        get() = maxLength

    fun matches(tokens: List<Lexer.Token>): Boolean {
        for (i in 0 until length)
            if (!queue[i].match(tokens[i].type))
                if (queue[i].required) return false

        return true
    }

    fun matches(stream: TokenStream): Boolean {
        val oldPos = stream.position

        for (i in 0 until length) {
            if (stream.hasNext()) {
                if (!queue[i].match(stream.next().type)) {
                    if (queue[i].required) {
                        stream.position = oldPos
                        return false
                    }
                }
            }
        }

        return true
    }

    // Utility classes to help with token matching
    interface Match {
        fun match(ttype: Lexer.TokenType): Boolean
        val types: List<Lexer.TokenType>
        val required: Boolean

        val possibleMatchCount: Int
            get() = types.size
    }

    class Substitute(override val types: List<Lexer.TokenType>) : Match {
        init {
            if (types.isEmpty()) throw RuntimeException("types is empty")
        }

        override fun match(ttype: Lexer.TokenType): Boolean {
            return ttype in types
        }

        override val required: Boolean = true
    }

    class Direct(private val type: Lexer.TokenType, override var required: Boolean = true) : Match {
        init {
            if (types.size > 1) throw RuntimeException("types.size > 1")
        }

        override fun match(ttype: Lexer.TokenType): Boolean {
            return ttype in types
        }

        override val types: List<Lexer.TokenType>
            get() = Collections.singletonList(type)

    }
}