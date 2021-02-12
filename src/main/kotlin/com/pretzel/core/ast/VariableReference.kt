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

package com.pretzel.core.ast

import com.pretzel.core.ast.visitor.NodeVisitor
import com.pretzel.core.lexer.Lexer.Location

class VariableReference(val fullPath: String, start: Location, end: Location) : Expression(start, end, Precedence.LOWEST) {
    override fun <T> accept(visitor: NodeVisitor<T>): T {
        return visitor.visitVariableReference(this)
    }

    override fun toString(): String {
        return fullPath
    }
}