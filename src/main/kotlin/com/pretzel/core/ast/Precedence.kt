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

enum class Precedence(val level: Int) {
    /**
     * Parentheses.
     */
    HIGHEST(14),

    /**
     * Function calls, array element access
     */
    EXTREMELY_HIGH(13),

    /**
     * Unary operations.
     */
    SUPER_HIGH(12),

    /**
     * Object creation, POW
     */
    VERY_HIGH(11),

    /**
     * Multiplicative operators: *, % and /
     */
    QUITE_HIGH(10),

    /**
     * Plus, minus and string concat.
     */
    PRETTY_HIGH(9),

    /**
     * Shift operators "<<", ">>" and ">>>"
     */
    LITTLE_HIGH(8),

    /**
     * Relativity
     */
    MEDIUM(7),

    /**
     * Bitwise AND
     */
    LITTLE_LOW(6),

    /**
     * Bitwise XOR
     */
    PRETTY_LOW(5),

    /**
     * Bitwise OR
     */
    QUITE_LOW(4),

    /**
     * Logical AND
     */
    VERY_LOW(3),

    /**
     * Logical OR
     */
    SUPER_LOW(2),

    /**
     * The trinary expression <condition> ? <expression-if-yes> : <expression-if-no>
     */
    EXTREMELY_LOW(1),

    /**
     * Assignment:
     * +=   -=  **=
     * *=   /=   %=
     * &=   ^=   |=
     * <<=  >>= >>>=
     *
     * Literals, variables, member access.
     */
    LOWEST(0),
}