package com.pretzel.core.parser.ast

import com.pretzel.core.parser.ast.annot.Modifier

interface ModifierTargetNode {
    val modifiers: List<Modifier>
    val modifierInteger: Int
}