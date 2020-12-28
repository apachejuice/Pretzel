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

import com.pretzel.core.ast.visitor.NodeVisitor
import com.pretzel.core.parser.Parser

class UnaryExpression(val target: Expression, val operator: Parser.UnaryOperator?
    ) : Expression(target.start, target.end, Precedence.SUPER_HIGH) {
    override fun <T> accept(visitor: NodeVisitor<T>): T {
        return visitor.visitUnaryExpression(this)
    }

    override fun toString(): String {
        return "${operator?.operator ?: ""}$target"
    }
}