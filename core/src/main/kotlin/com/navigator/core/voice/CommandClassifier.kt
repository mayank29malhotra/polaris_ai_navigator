package com.navigator.core.voice

/**
 * Maps a free-form transcript to a known [VoiceCommand] using simple keyword rules.
 *
 * This handles the obvious commands locally (fast, no LLM); anything unrecognized returns
 * [VoiceCommand.UNKNOWN] and is later routed to the agent.
 */
class CommandClassifier {

    fun classify(transcript: String): VoiceCommand {
        val t = transcript.lowercase().trim()
        return when {
            t.isEmpty() -> VoiceCommand.UNKNOWN
            t.contains("repeat") || t.contains("say again") || t.contains("again") -> VoiceCommand.REPEAT
            t.contains("should i") -> VoiceCommand.SHOULD_I_TAKE
            t.contains("which way") -> VoiceCommand.WHICH_WAY
            (t.contains("is this") || t.contains("am i")) && (t.contains("correct") || t.contains("right")) -> VoiceCommand.IS_THIS_CORRECT
            t.contains("correct turn") || t.contains("right way") || t.contains("right route") -> VoiceCommand.IS_THIS_CORRECT
            t.contains("what") && t.contains("next") -> VoiceCommand.WHATS_NEXT
            t.contains("next") && (t.contains("turn") || t.contains("maneuver") || t.contains("step")) -> VoiceCommand.WHATS_NEXT
            t.contains("how far") || t.contains("distance") || (t.contains("how") && t.contains("far")) -> VoiceCommand.HOW_FAR
            t.contains("how long") || t.contains("eta") || (t.contains("how") && t.contains("long")) -> VoiceCommand.HOW_LONG
            t.contains("where") && (t.contains("going") || t.contains("destination") || t.contains(" to")) -> VoiceCommand.WHERE_TO
            t.contains("toll") -> VoiceCommand.AVOID_TOLLS
            t.contains("highway") || t.contains("motorway") || t.contains("freeway") -> VoiceCommand.AVOID_HIGHWAYS
            t.contains("recalculate") || t.contains("reroute") || t.contains("new route") -> VoiceCommand.RECALCULATE
            (t.contains("stop") || t.contains("cancel") || t.contains("end")) &&
                (t.contains("nav") || t.contains("route") || t.contains("trip") || t.contains("guidance")) -> VoiceCommand.STOP_NAVIGATION
            t == "stop" || t == "cancel" -> VoiceCommand.STOP_NAVIGATION
            else -> VoiceCommand.UNKNOWN
        }
    }
}
