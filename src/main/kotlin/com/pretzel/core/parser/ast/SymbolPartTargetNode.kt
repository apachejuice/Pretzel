package com.pretzel.core.parser.ast

import com.pretzel.core.parser.ast.expr.SymbolReference

interface SymbolPartTargetNode {
    val target: SymbolReference
}