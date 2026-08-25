package com.navigator.core.policy

/** Keeps spoken output to one short sentence so guidance stays glanceable while riding. */
object SpeechPolicy {

    const val MAX_CHARS = 140

    fun shorten(text: String): String {
        val trimmed = text.trim()
        val match = SENTENCE_END.find(trimmed)
        val firstSentence = if (match != null) trimmed.substring(0, match.range.first + 1).trim() else trimmed
        return if (firstSentence.length <= MAX_CHARS) {
            firstSentence
        } else {
            firstSentence.take(MAX_CHARS).trimEnd() + "\u2026"
        }
    }

    // A sentence ends at . ! or ? that is followed by whitespace or end of string.
    private val SENTENCE_END = Regex("[.!?](\\s|$)")
}
