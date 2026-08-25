package com.navigator.core.policy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechPolicyTest {

    @Test
    fun keeps_a_short_single_sentence() {
        assertEquals("Turn left in 200 metres.", SpeechPolicy.shorten("Turn left in 200 metres."))
    }

    @Test
    fun keeps_only_the_first_sentence() {
        assertEquals("Turn left.", SpeechPolicy.shorten("Turn left. Then continue straight for a while."))
    }

    @Test
    fun does_not_split_on_a_decimal_point() {
        assertEquals("About 1.2 kilometres to go.", SpeechPolicy.shorten("About 1.2 kilometres to go."))
    }

    @Test
    fun truncates_very_long_text() {
        val long = "word ".repeat(60)
        assertTrue(SpeechPolicy.shorten(long).length <= SpeechPolicy.MAX_CHARS + 1)
    }
}
