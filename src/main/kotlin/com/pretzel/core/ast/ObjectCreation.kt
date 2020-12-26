/*
 * Copyright 2020 Valio Valtokari
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

package com.pretzel.core.ast

import com.pretzel.core.lexer.Lexer

class ObjectCreation(val name: String, val args: List<Argument>? = null, start: Lexer.Context, end: Lexer.Context) : Expression(start, end, Precedence.VERY_HIGH) {
    override fun <T> accept(visitor: NodeVisitor<T>): T {
        return visitor.visitObjectCreation(this)
    }

    override fun toString(): String {
        return "new $name(${args?.joinToString(separator = ",") { it -> "$it" } ?: ""})"
    }
}
