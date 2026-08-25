package com.navigator.core.tools

/** A parameter a tool accepts; used to build a JSON schema for the LLM layer later. */
data class ToolParameter(
    val name: String,
    val type: ToolParamType,
    val required: Boolean,
    val description: String,
)

enum class ToolParamType { STRING, NUMBER, INTEGER, BOOLEAN }

/** Loosely-typed argument bag; the LLM layer fills this from parsed JSON. */
class ToolArgs(private val values: Map<String, Any?>) {

    fun string(key: String): String? = values[key]?.toString()

    fun int(key: String): Int? = when (val v = values[key]) {
        is Number -> v.toInt()
        is String -> v.toIntOrNull()
        else -> null
    }

    fun double(key: String): Double? = when (val v = values[key]) {
        is Number -> v.toDouble()
        is String -> v.toDoubleOrNull()
        else -> null
    }

    fun boolean(key: String): Boolean? = when (val v = values[key]) {
        is Boolean -> v
        is String -> v.toBooleanStrictOrNull()
        else -> null
    }

    companion object {
        val EMPTY = ToolArgs(emptyMap())
        fun of(vararg pairs: Pair<String, Any?>): ToolArgs = ToolArgs(pairs.toMap())
    }
}

/** Outcome of a tool invocation. [data] carries structured values for the agent/UI. */
data class ToolResult(
    val success: Boolean,
    val message: String,
    val data: Map<String, Any?> = emptyMap(),
) {
    companion object {
        fun ok(message: String, data: Map<String, Any?> = emptyMap()) = ToolResult(true, message, data)
        fun error(message: String) = ToolResult(false, message)
    }
}

/** A deterministic action the UI or agent can invoke by [name]. */
interface NavigatorTool {
    val name: String
    val description: String
    val parameters: List<ToolParameter>
    fun execute(args: ToolArgs): ToolResult
}

/** Registry that dispatches tool calls by name. */
class ToolRegistry(val tools: List<NavigatorTool>) {

    private val byName: Map<String, NavigatorTool> = tools.associateBy { it.name }

    fun get(name: String): NavigatorTool? = byName[name]

    fun names(): List<String> = tools.map { it.name }

    fun execute(name: String, args: ToolArgs = ToolArgs.EMPTY): ToolResult =
        (byName[name] ?: return ToolResult.error("Unknown tool: $name")).execute(args)
}
