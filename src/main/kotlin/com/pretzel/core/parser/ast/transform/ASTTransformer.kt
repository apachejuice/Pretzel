package com.pretzel.core.parser.ast.transform

import com.pretzel.core.parser.ast.CodeNode

interface ASTTransformer<T, Q> {
    fun transform(ast: CodeNode, vararg data: Q): T
}