package com.navigator.core.agent

import com.navigator.core.nav.NavigationState
import com.navigator.core.policy.ConfirmationPolicy
import com.navigator.core.policy.SpeechPolicy
import com.navigator.core.tools.ToolArgs
import com.navigator.core.tools.ToolRegistry
import com.navigator.core.tools.ToolResult
import com.navigator.core.voice.CommandClassifier
import com.navigator.core.voice.SpokenInstructionFormatter
import com.navigator.core.voice.VoiceCommand

/** Result of handling one rider utterance. */
data class AgentResponse(
    val spoken: String,
    val command: VoiceCommand? = null,
    val toolName: String? = null,
    val toolResult: ToolResult? = null,
    val usedLlm: Boolean = false,
    val awaitingConfirmation: Boolean = false,
)

/**
 * Turns a rider transcript into an action + spoken reply. Obvious Tier-1 commands are handled
 * locally (no LLM); everything else goes to [llm], which may return a tool call executed
 * through [registry]. Risky tools require a spoken confirmation first (BRD Rule 4), and the LLM
 * never mutates navigation directly — it only selects a deterministic tool.
 */
class NavigatorAgent(
    private val llm: LlmClient,
    private val registry: ToolRegistry,
    private val classifier: CommandClassifier = CommandClassifier(),
    private val spokenFormatter: SpokenInstructionFormatter = SpokenInstructionFormatter(),
    private val contextBuilder: AgentContextBuilder = AgentContextBuilder(),
    private val systemPrompt: String = DEFAULT_SYSTEM_PROMPT,
) {
    private var pending: PendingAction? = null

    fun handle(transcript: String, state: NavigationState): AgentResponse {
        resolvePending(transcript)?.let { return it }

        when (val command = classifier.classify(transcript)) {
            VoiceCommand.UNKNOWN -> Unit // fall through to the LLM
            VoiceCommand.RECALCULATE -> return executeTool("recalculate_route", ToolArgs.EMPTY, command)
            VoiceCommand.STOP_NAVIGATION -> return executeTool("stop_navigation", ToolArgs.EMPTY, command)
            VoiceCommand.AVOID_TOLLS ->
                return executeTool("set_route_preference", ToolArgs.of("avoid_tolls" to true), command)
            VoiceCommand.AVOID_HIGHWAYS ->
                return executeTool("set_route_preference", ToolArgs.of("avoid_highways" to true), command)
            VoiceCommand.SHOULD_I_TAKE -> return AgentResponse(
                spoken = spokenFormatter.shouldITake(transcript, state),
                command = command,
                usedLlm = false,
            )
            else -> return AgentResponse(
                spoken = spokenFormatter.respondTo(command, state),
                command = command,
                usedLlm = false,
            )
        }

        val request = LlmRequest(
            systemPrompt = systemPrompt,
            userMessage = transcript,
            context = contextBuilder.build(state),
            tools = ToolSchemas.forRegistry(registry),
        )
        return when (val response = llm.complete(request)) {
            is LlmResponse.ToolCall -> handleToolCall(response)
            is LlmResponse.Text -> AgentResponse(spoken = SpeechPolicy.shorten(response.content), usedLlm = true)
        }
    }

    private fun handleToolCall(call: LlmResponse.ToolCall): AgentResponse {
        if (registry.get(call.toolName) == null) {
            return AgentResponse(spoken = UNSUPPORTED_MESSAGE, usedLlm = true)
        }
        val args = ToolArgs(call.arguments)
        if (ConfirmationPolicy.requiresConfirmation(call.toolName)) {
            pending = PendingAction(call.toolName, args)
            return AgentResponse(
                spoken = confirmPrompt(call.toolName),
                toolName = call.toolName,
                usedLlm = true,
                awaitingConfirmation = true,
            )
        }
        val result = registry.execute(call.toolName, args)
        return AgentResponse(
            spoken = SpeechPolicy.shorten(result.message),
            toolName = call.toolName,
            toolResult = result,
            usedLlm = true,
        )
    }

    /** Returns a response if a pending confirmation consumed this transcript, else null. */
    private fun resolvePending(transcript: String): AgentResponse? {
        val action = pending ?: return null
        return when (affirmationOf(transcript)) {
            Affirmation.YES -> {
                pending = null
                executeTool(action.toolName, action.args, null)
            }
            Affirmation.NO -> {
                pending = null
                AgentResponse(spoken = "Okay, cancelled.", usedLlm = false)
            }
            Affirmation.UNCLEAR -> {
                pending = null // drop it and process the new request normally
                null
            }
        }
    }

    private fun executeTool(name: String, args: ToolArgs, command: VoiceCommand?): AgentResponse {
        val result = registry.execute(name, args)
        return AgentResponse(
            spoken = SpeechPolicy.shorten(result.message),
            command = command,
            toolName = name,
            toolResult = result,
            usedLlm = false,
        )
    }

    private fun confirmPrompt(toolName: String): String = when (toolName) {
        "clear_stops" -> "Are you sure you want to clear all stops? Say yes to confirm."
        else -> "Are you sure? Say yes to confirm."
    }

    private fun affirmationOf(transcript: String): Affirmation {
        val t = transcript.lowercase()
        return when {
            t.contains("yes") || t.contains("confirm") || t.contains("go ahead") ||
                t.contains("do it") || t == "yeah" || t == "yep" -> Affirmation.YES
            t.contains("no") || t.contains("cancel") || t.contains("don't") -> Affirmation.NO
            else -> Affirmation.UNCLEAR
        }
    }

    private data class PendingAction(val toolName: String, val args: ToolArgs)

    private enum class Affirmation { YES, NO, UNCLEAR }

    companion object {
        const val UNSUPPORTED_MESSAGE =
            "Sorry, I can't do that one. I can change your destination, add or remove stops, " +
                "or avoid tolls and highways."

        const val DEFAULT_SYSTEM_PROMPT =
            "You are Navigator, a hands-free motorcycle riding assistant. The navigation engine " +
                "is the source of truth; never invent roads, distances, or directions. For any " +
                "navigation change, call exactly one tool. Keep spoken replies to one short " +
                "sentence. If a request is ambiguous, ask a brief clarifying question."
    }
}
