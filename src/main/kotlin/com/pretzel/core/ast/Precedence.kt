package com.pretzel.core.ast

enum class Precedence(val level: Int) {
    /**
     * Parentheses.
     */
    HIGHEST(15),

    /**
     * Function calls, array element access
     */
    EXTREMELY_HIGH(14),

    /**
     * Unary operations.
     */
    SUPER_HIGH(13),

    /**
     * Object creation.
     */
    VERY_HIGH(12),

    /**
     * Multiplicative operators: *, % and /
     */
    QUITE_HIGH(11),

    /**
     * Plus, minus and string concat.
     */
    PRETTY_HIGH(10),

    /**
     * Shift operators "<<", ">>" and ">>>"
     */
    LITTLE_HIGH(9),

    /**
     * Relativity
     */
    MEDIUM(8),

    /**
     * Equality
     */
    LOW(7),

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
     * The if expression/statement `<thing> if <condition> else <thing> `
     */
    EXTREMELY_LOW(1),

    /**
     * Assignment:
     *(  +=   -=
     * *=   /=   %=
     * &=   ^=   |=
     * <<=  >>= >>>=
     *
     * Literals.
     */
    LOWEST(0),
}