package com.pretzel.core.parser

import com.pretzel.core.lexer.Lexer
import com.pretzel.core.lexer.TokenStream

import java.util.Collections


/**
 * A Rule is a sequence of constant (or regex-based)
 * elements in the source code. However a Rule can contain
 * other Rules making in very flexible and capable of
 * representing complex structures in the source code.
 */
class Rule(val queue: ArrayList<Match>) {
    fun addRule(rule: Rule) {
        queue.addAll(rule.queue)
    }

    fun addMatch(match: Match) {
        queue.add(match)
    }

    fun addToken(tt: Lexer.TokenType) {
        queue.add(Direct(tt))
    }

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

    fun match(tokens: List<Lexer.Token>): Boolean {
        for (i in 0 until length)
            if (!queue[i].match(tokens[i].type))
                if (queue[i].required) return false

        return true
    }

    fun match(stream: TokenStream): Boolean {
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


    interface Match {
        fun match(vararg ttype: Lexer.TokenType): Boolean
        val types: List<Lexer.TokenType>
        val required: Boolean

        val possibleMatchCount: Int
            get() = types.size
    }

    class Substitute(override val types: List<Lexer.TokenType>) : Match {
        init {
            if (types.isEmpty()) throw RuntimeException("types is empty")
        }

        override fun match(vararg ttype: Lexer.TokenType): Boolean {
            return ttype[0] in types
        }

        override val required: Boolean = true
    }

    class Direct(private val type: Lexer.TokenType, override var required: Boolean = true) : Match {
        init {
            if (types.size > 1) throw RuntimeException("types.size > 1")
        }

        override fun match(vararg ttype: Lexer.TokenType): Boolean {
            return ttype[0] in types
        }

        override val types: List<Lexer.TokenType>
            get() = Collections.singletonList(type)

    }
}