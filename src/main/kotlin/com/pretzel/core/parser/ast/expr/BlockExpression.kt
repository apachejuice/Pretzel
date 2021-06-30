package com.pretzel.core.parser.ast.expr

import com.pretzel.core.parser.ast.Block

class BlockExpression(val block: Block) : Expression(block.sourceRange) {
    override val isEvaluatableAtCompileTime: Boolean
        get() = false

    override fun toString(): String {
        return "BlockExpression($block)"
    }

    override val nodeType: Type
        get() = Type.BLOCK_EXPR
}