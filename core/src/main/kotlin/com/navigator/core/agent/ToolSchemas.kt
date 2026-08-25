package com.navigator.core.agent

import com.navigator.core.tools.NavigatorTool
import com.navigator.core.tools.ToolParamType
import com.navigator.core.tools.ToolParameter
import com.navigator.core.tools.ToolRegistry

/** LLM-facing description of a tool. */
data class ToolSchema(
    val name: String,
    val description: String,
    val parameters: List<ToolParameter>,
)

/** Builds tool schemas and their strict JSON (OpenAI function-calling format). */
object ToolSchemas {

    fun from(tool: NavigatorTool): ToolSchema = ToolSchema(tool.name, tool.description, tool.parameters)

    fun forRegistry(registry: ToolRegistry): List<ToolSchema> = registry.tools.map(::from)

    fun toJson(schemas: List<ToolSchema>): String =
        schemas.joinToString(prefix = "[", postfix = "]", separator = ",", transform = ::functionJson)

    private fun functionJson(schema: ToolSchema): String {
        val properties = schema.parameters.joinToString(",") { p ->
            "${quote(p.name)}:{\"type\":${quote(jsonType(p.type))},\"description\":${quote(p.description)}}"
        }
        val required = schema.parameters.filter { it.required }.joinToString(",") { quote(it.name) }
        return "{\"type\":\"function\",\"function\":{" +
            "\"name\":${quote(schema.name)}," +
            "\"description\":${quote(schema.description)}," +
            "\"parameters\":{\"type\":\"object\",\"properties\":{$properties},\"required\":[$required]}}}"
    }

    private fun jsonType(type: ToolParamType): String = when (type) {
        ToolParamType.STRING -> "string"
        ToolParamType.NUMBER -> "number"
        ToolParamType.INTEGER -> "integer"
        ToolParamType.BOOLEAN -> "boolean"
    }

    private fun quote(value: String): String {
        val escaped = value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
        return "\"$escaped\""
    }
}
