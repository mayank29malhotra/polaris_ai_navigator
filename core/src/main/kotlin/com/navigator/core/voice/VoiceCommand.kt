package com.navigator.core.voice

/** The small set of deterministic voice commands handled without the LLM (Tiers 1, 3, 4). */
enum class VoiceCommand {
    REPEAT,
    WHATS_NEXT,
    HOW_FAR,
    HOW_LONG,
    WHERE_TO,
    WHICH_WAY,
    IS_THIS_CORRECT,
    SHOULD_I_TAKE,
    AVOID_TOLLS,
    AVOID_HIGHWAYS,
    RECALCULATE,
    STOP_NAVIGATION,
    UNKNOWN,
}
