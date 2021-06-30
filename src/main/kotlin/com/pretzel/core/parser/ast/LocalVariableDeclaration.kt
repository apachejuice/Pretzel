package com.pretzel.core.parser.ast

import com.pretzel.core.lexer.Lexer
import com.pretzel.core.parser.ast.annot.Modifier
import com.pretzel.core.parser.ast.expr.SymbolReference

class LocalVariableDeclaration(
    target: SymbolReference,
    modifiers: List<Modifier>,
    sourceRange: Lexer.Span,
    assignment: VariableAssignment?
) : VariableDeclaration(
    target, modifiers, sourceRange, assignment
) {
    override val nodeType: Type
        get() = Type.LOCAL_VARIABLE_DECLARATION
    override val permittedModifiers: List<Modifier>
        get() = listOf(Modifier.FINAL, Modifier.SHARED)

    override fun toString(): String {
        return "LocalVariableDeclaration(modifiers = $modifiers target = $target assignment = $assignment)"
    }
}