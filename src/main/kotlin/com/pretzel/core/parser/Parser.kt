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

import com.pretzel.core.lexer.TokenStream

import java.util.Stack

/**
 * A parser for Pretzel source files.
 */
class Parser(val stream: TokenStream) {
    data class StateTransition(val from: Int, val to: Int) {
        companion object {
            fun statesFor(state: Int): List<Int> {
                val result = mutableListOf<Int>()
                for (s in VALID_STATES) {
                    if ((state and s) != 0) {
                        result.add(s)
                    }
                }

                return if (result.isEmpty()) listOf(0) else result
            }
        }

        override fun toString(): String = "$before -> $after"

        val diff: String
            get() {
                var result = ""

                if (from == DEFAULT) return "[0] + $added"
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

        val VALID_STATES = listOf(
            DEFAULT,
            FUNCTION,
            EXPRESSION,
            BLOCK
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

    private fun verifyState(state: Int) {
        if (state !in VALID_STATES) throw IllegalArgumentException("invalid state number $state")
    }

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

    private fun popState(state: Int) {
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
        currentStates.clear()
        currentStates.addAll(StateTransition.statesFor(newState))
    }

    val currentStates: MutableList<Int> = mutableListOf()
    val stateStack: Stack<Int> = Stack()
    val transitions: Stack<StateTransition> = Stack()

    init {
        pushState(EXPRESSION)
        pushState(FUNCTION)
        popState(EXPRESSION)
        pushState(BLOCK)
        popState(FUNCTION)
        popState(BLOCK)
        transitions.forEach { println("$it (${it.diff} = ${it.after} = ${it.to})") }
    }
}