package com.navigator.core.agent

/** What the LLM returns: either a tool call or a direct text reply. */
sealed interface LlmResponse {
    data class ToolCall(val toolName: String, val arguments: Map<String, Any?>) : LlmResponse
    data class Text(val content: String) : LlmResponse
}

/** A single request to the LLM. */
data class LlmRequest(
    val systemPrompt: String,
    val userMessage: String,
    val context: String,
    val tools: List<ToolSchema>,
)

/**
 * Provider-agnostic LLM entry point. Implemented in `:app` by a concrete provider; the
 * implementation handles networking/threading and returns synchronously.
 */
interface LlmClient {
    fun complete(request: LlmRequest): LlmResponse
}
