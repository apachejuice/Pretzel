package com.pretzel.core.tools.serializer

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.pretzel.core.parser.ast.CodeNode
import com.pretzel.core.parser.ast.transform.ASTTransformer
import java.lang.reflect.Type


class JSONSerializer : ASTTransformer<String, Unit?> {
    override fun transform(ast: CodeNode, vararg data: Unit?): String {
        val create = GsonBuilder().disableHtmlEscaping().create()
        return create.toJson(ast)
    }
}