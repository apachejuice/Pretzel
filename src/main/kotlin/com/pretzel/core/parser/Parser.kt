/*
 * Copyright 2021 apachejuice
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     <http://www.apache.org/licenses/LICENSE-2.0>
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pretzel.core.parser

import com.pretzel.core.ConsoleReporter
import com.pretzel.core.ErrorType
import com.pretzel.core.Reporter
import com.pretzel.core.ast.Argument
import com.pretzel.core.ast.Associativity
import com.pretzel.core.ast.BinaryExpression
import com.pretzel.core.ast.BooleanLiteral
import com.pretzel.core.ast.Expression
import com.pretzel.core.ast.FloatLiteral
import com.pretzel.core.ast.FunctionCall
import com.pretzel.core.ast.IntLiteral
import com.pretzel.core.ast.LongLiteral
import com.pretzel.core.ast.ModStmt
import com.pretzel.core.ast.Node
import com.pretzel.core.ast.ObjectCreation
import com.pretzel.core.ast.ParenthesizedExpression
import com.pretzel.core.ast.Precedence
import com.pretzel.core.ast.ShortLiteral
import com.pretzel.core.ast.StringLiteral
import com.pretzel.core.ast.TrinaryExpression
import com.pretzel.core.ast.UseStmt
import com.pretzel.core.ast.VariableReference
import com.pretzel.core.lexer.Lexer.Token
import com.pretzel.core.lexer.Lexer.Location
import com.pretzel.core.lexer.Lexer.TokenType
import com.pretzel.core.lexer.TokenStream

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Stack
import java.util.StringJoiner

/**
 * A parser for Pretzel source files.
 */
class Parser(val stream: TokenStream) {
    //region DFA
    data class StateTransition(val from: Int, val to: Int, val timestamp: Date = Date()) {
        companion object {
            fun statesFor(state: Int): MutableList<Int> {
                val result = mutableListOf<Int>()
                for (s in VALID_STATES) {
                    if ((state and s) != 0) {
                        result.add(s)
                    }
                }

                return if (result.isEmpty()) mutableListOf(0) else result
            }
        }

        override fun toString(): String = "$before -> $after"

        val diff: String
            get() {
                var result = ""

                if (from == DEFAULT) return "[0] + $added"
                if (from == -1) return "no state + $added"
                if (removed.isNotEmpty()) {
                    result += if (removed.size == 1) {
                        "$before - $removed"
                    } else {
                        "[0]"
                    }
                }

                if (added.isNotEmpty()) {
                    result += if (removed.isEmpty()) {
                        "$before + $added"
                    } else {
                        if (added[0] == DEFAULT) "" else " + $added"
                    }
                }

                return result
            }

        val before: List<Int> = statesFor(from)
        val after: List<Int> = statesFor(to)
        val added: List<Int> = after - before
        val removed: List<Int> = before - after
    }

    companion object {
        const val DEFAULT: Int = 0
        const val FUNCTION: Int = 256
        const val EXPRESSION: Int = 512
        const val BLOCK: Int = 1024
        const val ERROR: Int = 8192

        val VALID_STATES = listOf(
            DEFAULT,
            FUNCTION,
            EXPRESSION,
            BLOCK,
            ERROR,
        )

        val SYNC_POINTS = listOf(
            TokenType.FUNC,
            TokenType.USE,
            TokenType.MOD,
            TokenType.IF,
            TokenType.VAR,
            TokenType.ELSE,
            TokenType.ELIF,
            TokenType.EOF,
        )
    }

    /**
     * Merge the current states into a unique integer
     * representing the combination of the states.
     */
    private fun mergeStates(): Int {
        if (currentStates.isEmpty()) return DEFAULT
        var savedState = currentStates[0]
        if (currentStates.size < 2) return savedState
        for (i in 1 until currentStates.size) savedState = savedState or currentStates[i]
        return savedState
    }

    private fun stateName(state: Int): String {
        verifyState(state)
        return when (state) {
            DEFAULT -> "default state"
            FUNCTION -> "function"
            EXPRESSION -> "expression"
            BLOCK -> "block"
            ERROR -> "error"
            else -> null!!
        }
    }

    private fun buildStateName(state: Int): String {
        val result = StringBuilder()
        val stateList = StateTransition.statesFor(state)
        if (stateList.isEmpty()) return ""
        if (stateList.size == 1) return stateName(stateList[0])

        var i = 0
        for (s in 0 until stateList.size - 2) {
            result.append("${stateName(stateList[s])}, ")
            i++
        }

        result.append(stateName(stateList[i]))
        i++
        result.append(" and ${stateName(stateList[i])}")
        return result.toString()
    }

    /**
     * Verify that [state] is a valid state.
     */
    private fun verifyState(state: Int) {
        if (state !in VALID_STATES) throw IllegalArgumentException("invalid state number $state")
    }

    private fun hasState(state: Int): Boolean = (state and mergeStates()) != 0

    /**
     * Push a new state onto the state stack, merging it
     * with the current ones.
     */
    private fun pushState(state: Int) {
        verifyState(state)
        // make sure we're not in that state already
        if ((mergeStates() and state) != 0) return
        val oldState = mergeStates()
        // create the new state and push the result
        val newState = mergeStates() or state
        stateStack.push(newState)
        transitions.push(StateTransition(oldState, newState))
        currentStates.addAll(StateTransition.statesFor(newState))
    }

    /**
     * Pop a state from the state stack and
     * merge the old ones into a new state.
     */
    private fun popState(state: Int) {
        if (state == ERROR) throw RuntimeException("cannot pop error state")
        verifyState(state)
        if ((mergeStates() and state) == 0) return // we aren't in that state
        val oldState = mergeStates()
        var newState = DEFAULT
        for (s in StateTransition.statesFor(oldState)) {
            if (s != state) {
                newState = newState or s
            }
        }

        stateStack.push(newState)
        transitions.push(StateTransition(oldState, newState))
        currentStates = StateTransition.statesFor(newState)
    }

    fun dumpStateTransitions(filename: String = "parser.states") {
        val stateData = StringBuilder()
        val sdf = SimpleDateFormat("dd-MM-yyy HH-mm-ss:SS")
        transitions.forEach {
            val label = "${buildStateName(it.from)} to ${buildStateName(it.to)}"
            stateData.append("transition from $label: \n${sdf.format(it.timestamp)}: $it (${it.diff} = ${it.after} = ${it.to})\n\n")
        }

        val f = File(filename)
        if (f.exists()) f.delete()
        val buf = BufferedOutputStream(FileOutputStream(f))
        for (c in stateData) buf.write(c.toInt())
        buf.close()
    }

    private val realLoc: Location
        get() = stream.seek().toLocation()

    private val lastTransition: StateTransition?
        get() {
            return if (transitions.isEmpty()) null
            else transitions.lastElement()
        }

    private var currentStates: MutableList<Int> = mutableListOf()
    private val stateStack: Stack<Int> = Stack()
    val transitions: Stack<StateTransition> = Stack()
    //endregion

    private var beginLoc: Location = stream.seek().toLocation()

    private val reporter: Reporter = ConsoleReporter()
    var keepGoing: Boolean = true
    var debug: Boolean = false
        set(value) {
            field = value
            reporter.debug = field
        }

    val hadError: Boolean
        get() = hasState(ERROR)

    var cancellationHook: (l: Location, state: Int, lastTransition: StateTransition?) -> Unit =
        { _: Location, _: Int, _: StateTransition? -> }

    private fun synchronize() {
        while (stream.seek().type !in SYNC_POINTS) stream.advance()
    }

    private fun warning(message: String) {
        reporter.warning(ErrorType.PARSER, message, realLoc)
    }

    private fun cancel(message: String, loc: Location = realLoc, sync: Boolean = true) {
        println("cancel: $realLoc")
        pushState(ERROR)
        cancellationHook.invoke(loc, mergeStates(), lastTransition)
        reporter.error(ErrorType.PARSER, message, loc)
        if (sync)
            if (keepGoing)
                synchronize()
            else
                throw ParsingError(this, message, mergeStates(), loc)
    }

    init {
        reporter.columnOffset = 1
    }

    class ParsingError(
        parser: Parser,
        message: String,
        state: Int,
        location: Location
    ) : RuntimeException("parsing error at $location: state $state: $message. DFA dump in file error-states.txt") {
        init {
            if (parser.debug) parser.dumpStateTransitions("error-states.txt")
        }
    }

    private var startLocation: Location = beginLoc
    private val span: Pair<Location, Location>
        get() = startLocation to beginLoc

    private fun begin() {
        startLocation = beginLoc
    }

    private fun rollback(loc: Location) {
        beginLoc = loc
        while (stream.seek().toLocation() != loc) stream.position--
    }

    private fun acceptToken(vararg types: TokenType, message: String = ""): Token {
        val result = stream.seek()
        println(result.toLocation())
        if (result.type !in types) {
            var alts = ""
            if (types.size == 1) alts = types[0].name
            else {
                for (i in types.indices - 2) alts += "'${types[i]}', "
                alts += types[types.indices.last - 1].name
                alts += " or '${types.last()}'"
            }

            cancel(
                if (message.isEmpty())
                    "expected one of $alts, got '${result.type}'"
                else message
            )
        } else stream.advance()

        return result
    }

    private fun requireStatementDelimiter() =
        acceptToken(TokenType.SEMI, message = "statement delimiter ';' missing")

    fun parse(): Node? {
        val nodes = Stack<Node>()

        while (stream.hasNext()) {
            when (val tt = stream.seek().type) {
                TokenType.USE -> nodes.push(useStmt())
                TokenType.MOD -> nodes.push(modStmt())
                TokenType.EOF -> break
                else -> nodes.push(expression())
            }

            stream.advance()
            println("${nodes.peek()}        ${nodes.peek().javaClass.name}")
        }

        return if (nodes.isEmpty() || hadError) null else Node.getRootInstance(nodes)
    }

    enum class BinaryOperator(
        val literal: String,
        val precedence: Precedence,
        val tt: TokenType
    ) {
        // assignment
        SET("=", Precedence.LOWEST, TokenType.ASSIGN),
        PLUSSET("+=", Precedence.LOWEST, TokenType.PLUSSET),
        MINUSSET("-=", Precedence.LOWEST, TokenType.MINUSSET),
        STRCOMMASET("...=", Precedence.LOWEST, TokenType.CONCAT_COMMA_SET),
        BANDSET("&=", Precedence.LOWEST, TokenType.BANDSET),
        BORSET("|=", Precedence.LOWEST, TokenType.BORSET),
        SIGNEDLSET("<<=", Precedence.LOWEST, TokenType.LSHIFTSET),
        SIGNEDRSET(">>=", Precedence.LOWEST, TokenType.RSHIFTSET),
        UNSIGNEDRSET(">>>=", Precedence.LOWEST, TokenType.URSHIFTSET),
        XORSET("^=", Precedence.LOWEST, TokenType.XORSET),
        DIVSET("/=", Precedence.LOWEST, TokenType.DIVSET),
        MODSET("%=", Precedence.LOWEST, TokenType.MODSET),
        MULSET("*=", Precedence.LOWEST, TokenType.MODSET),
        POWSET("**=", Precedence.LOWEST, TokenType.POWSET),
        STRSPACESET("..=", Precedence.LOWEST, TokenType.CONCAT_SPACE_SET),

        // operations
        PLUS("+", Precedence.PRETTY_HIGH, TokenType.PLUS),
        MINUS("-", Precedence.PRETTY_HIGH, TokenType.MINUS),
        STRCOMMA("...", Precedence.PRETTY_HIGH, TokenType.CONCAT_COMMA),
        AND("&&", Precedence.VERY_LOW, TokenType.AND),
        BAND("&", Precedence.LITTLE_LOW, TokenType.BAND),
        BOR("|", Precedence.QUITE_LOW, TokenType.BOR),
        SIGNEDL("<<", Precedence.LITTLE_HIGH, TokenType.LSHIFT),
        SIGNEDR(">>", Precedence.LITTLE_HIGH, TokenType.RSHIFT),
        UNSIGNEDR(">>>", Precedence.LITTLE_HIGH, TokenType.URSHIFT),
        XOR("^", Precedence.PRETTY_LOW, TokenType.XOR),
        DIV("/", Precedence.QUITE_HIGH, TokenType.DIV),
        EQ("==", Precedence.MEDIUM, TokenType.EQ),
        GT(">", Precedence.MEDIUM, TokenType.GT),
        GTEQ(">=", Precedence.MEDIUM, TokenType.GTEQ),
        MOD("%", Precedence.QUITE_HIGH, TokenType.MOD),
        MUL("*", Precedence.QUITE_HIGH, TokenType.MUL),
        NOTEQ("!=", Precedence.MEDIUM, TokenType.NOTEQ),
        OR("||", Precedence.SUPER_LOW, TokenType.OR),
        POW("**", Precedence.VERY_HIGH, TokenType.POW),
        LT("<", Precedence.MEDIUM, TokenType.LT),
        LTEQ("<=", Precedence.MEDIUM, TokenType.LTEQ),
        STRSPACE("..", Precedence.PRETTY_HIGH, TokenType.CONCAT_SPACE),
        NONE("", Precedence.LOWEST, TokenType.EOF);

        companion object {
            fun forTT(tt: TokenType): BinaryOperator? {
                for (v in values())
                    if (v.tt == tt) return v

                return null
            }

            val tokenTypes: List<TokenType>
                get() {
                    val result = mutableListOf<TokenType>()
                    values().forEach { result.add(it.tt) }
                    return result
                }

            val assignmentOps: List<TokenType>
                get() {
                    val result = mutableListOf<TokenType>()
                    values().forEach { if (it.name.endsWith("SET")) result.add(it.tt) }
                    return result
                }
        }

        val assoc: Associativity
            get() {
                return when (this) {
                    POW -> Associativity.RIGHT
                    else -> Associativity.LEFT
                }
            }
    }

    enum class UnaryOperator(
        val literal: String,
        val tt: TokenType
    ) {
        MINUS("-", TokenType.MINUS),
        PLUS("+", TokenType.PLUS),
        NEG("~", TokenType.TILDE),
        NOT("!", TokenType.NOT);

        val precedence: Precedence
            get() = Precedence.SUPER_HIGH
    }

    private val literalTypes = listOf(
        TokenType.INTEGER_LITERAL,
        TokenType.FLOAT_LITERAL,
        TokenType.YES,
        TokenType.NO,
        TokenType.STRING_LITERAL,
        TokenType.TEMPLATE_STRING,
        TokenType.SHORT_LITERAL,
        TokenType.LONG_LITERAL,
        TokenType.HEX_LITERAL,
    )

    private val invalidExpr
        get() = VariableReference("", beginLoc, beginLoc)

    fun expression(): Expression {
        pushState(EXPRESSION)
        return expression(0).also { popState(EXPRESSION) }
    }

    private fun expression(minPrec: Int): Expression {
        var result = literalOrValue()

        while (true) {
            val current = stream.seek()
            if (current.type == TokenType.EOF
                    || current.type !in BinaryOperator.tokenTypes
                    || BinaryOperator.forTT(current.type)!!.precedence.level < minPrec) {
                break
            }

            val bop = BinaryOperator.forTT(current.type)!!
            val prec = bop.precedence.level
            val assoc = bop.assoc
            var nextMinPrec = prec
            if (assoc == Associativity.LEFT) nextMinPrec++
            stream.advance()
            val rhs = expression(nextMinPrec)
            result = BinaryExpression(result, rhs, bop)
        }

        return result
    }

    private fun literalOrValue(): Expression {
        begin()
        val token = stream.seek()

        when (val type = token.type) {
            TokenType.LPAREN -> {
                stream.advance()
                val expr = expression()
                acceptToken(TokenType.RPAREN, message = "expected ')'")
                return ParenthesizedExpression(expr)
            }

            in literalTypes -> {
                stream.advance()
                return when (type) {
                    TokenType.INTEGER_LITERAL -> IntLiteral(token)
                    TokenType.FLOAT_LITERAL -> FloatLiteral(token)
                    TokenType.YES,
                    TokenType.NO -> BooleanLiteral(token)
                    TokenType.STRING_LITERAL -> StringLiteral(token, false)
                    TokenType.TEMPLATE_STRING -> StringLiteral(token, true)
                    TokenType.SHORT_LITERAL -> ShortLiteral(token)
                    TokenType.LONG_LITERAL -> LongLiteral(token)
                    TokenType.HEX_LITERAL -> null!! // not yet supported
                    else -> null!!
                }
            }

            TokenType.NEW -> {
                stream.advance()
                val objectName = name()
                var args = listOf<Argument>()
                acceptToken(TokenType.LPAREN)
                if (!stream.acceptIfNext(TokenType.RPAREN)) {
                    args = parseArgumenList()
                    acceptToken(TokenType.RPAREN)
                }

                return ObjectCreation(objectName.fullPath, args, startLocation, beginLoc)
            }

            TokenType.IDENTIFIER -> {
                val id = name()
                return if (stream.acceptIfNext(TokenType.LPAREN)) {
                    // Function call
                    var args = listOf<Argument>()
                    acceptToken(TokenType.LPAREN)
                    if (!stream.acceptIfNext(TokenType.RPAREN)) {
                        args = parseArgumenList()
                        acceptToken(TokenType.RPAREN)
                    }
                    acceptToken(TokenType.RPAREN)
                    FunctionCall(id.fullPath, startLocation, beginLoc, args)
                } else id
            }

            else -> {
                cancel("unexpected token '$type', expected expression or value")
                return invalidExpr
            }
        }
    }

    private fun parseArgumenList(): MutableList<Argument> {
        begin()
        val list = mutableListOf<Argument>()
        do {
            if (stream.isNext(TokenType.COMMA)) {
                warning("unnecessary trailing comma")
                break
            }

            list.add(parseArgument())
        } while (stream.acceptIfNext(TokenType.COMMA))

        return list;
    }

    private fun parseArgument(): Argument {
        begin()
        val start = realLoc
        val expr: Expression

        val name = stream.seek()
        if (stream.acceptIfNext(TokenType.IDENTIFIER)) {
            if (stream.acceptIfNext(TokenType.COLON)) {
                expr = expression()
                return Argument(expr, name.lexeme)
            } else rollback(start)
        }

        expr = expression()
        return Argument(expr)
    }


    private fun name(): VariableReference {
        begin()
        val begin = acceptToken(TokenType.IDENTIFIER)
        val result = StringBuilder(begin.lexeme)

        if (stream.acceptIfNext(TokenType.COLON)) {
            while (true) {
                if (stream.isNext(TokenType.MUL)) {
                    cancel("variable references cannot have wildcards")
                    stream.advance()
                }

                result.append(acceptToken(TokenType.IDENTIFIER).lexeme)
                if (!stream.isNext(TokenType.COLON)) break
                else result.append(acceptToken(TokenType.COLON).lexeme)
            }
        }

        if (stream.acceptIfNext(TokenType.DOT)) {
            while (true) {
                result.append(acceptToken(TokenType.IDENTIFIER).lexeme)
                if (!stream.isNext(TokenType.DOT)) break
                else result.append(acceptToken(TokenType.DOT).lexeme)
            }
        }

        return VariableReference(result.toString(), startLocation, beginLoc)
    }

    private fun useStmt(): UseStmt {
        begin()
        acceptToken(TokenType.USE)
        val target = modulePath()
        return UseStmt(
            target.first.split(":"),
            span.first,
            span.second,
            target.second,
        )
    }

    private fun modStmt(): ModStmt {
        begin()
        acceptToken(TokenType.MOD)
        val target = modulePath(true)
        return ModStmt(
            target.first.split(":"),
            span.first,
            span.second,
        )
    }

    private fun modulePath(module: Boolean = false): Pair<String, Boolean> {
        val result = StringJoiner(":")
        var wildcard = false
        result.add(acceptToken(TokenType.IDENTIFIER).lexeme)

        if (stream.acceptIfNext(TokenType.COLON)) {
            while (true) {
                if (stream.isNext(TokenType.MUL)) {
                    if (module) cancel("mod statements cannot have wildcards", sync = false)
                    wildcard = true
                    stream.advance()
                    break
                }

                result.add(acceptToken(TokenType.IDENTIFIER).lexeme)
                if (!stream.isNext(TokenType.COLON)) break
                else acceptToken(TokenType.COLON)
            }
        }

        requireStatementDelimiter()
        return result.toString() to wildcard
    }
}