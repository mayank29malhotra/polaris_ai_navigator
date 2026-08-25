package com.navigator.core.voice

import org.junit.Assert.assertEquals
import org.junit.Test

class CommandClassifierTest {

    private val classifier = CommandClassifier()

    @Test
    fun repeat() {
        assertEquals(VoiceCommand.REPEAT, classifier.classify("Repeat"))
        assertEquals(VoiceCommand.REPEAT, classifier.classify("say that again"))
    }

    @Test
    fun whats_next() {
        assertEquals(VoiceCommand.WHATS_NEXT, classifier.classify("What's next?"))
        assertEquals(VoiceCommand.WHATS_NEXT, classifier.classify("next turn"))
    }

    @Test
    fun how_far() {
        assertEquals(VoiceCommand.HOW_FAR, classifier.classify("How far?"))
    }

    @Test
    fun how_long() {
        assertEquals(VoiceCommand.HOW_LONG, classifier.classify("how long until we get there"))
    }

    @Test
    fun where_to() {
        assertEquals(VoiceCommand.WHERE_TO, classifier.classify("where are we going"))
    }

    @Test
    fun recalculate() {
        assertEquals(VoiceCommand.RECALCULATE, classifier.classify("recalculate"))
    }

    @Test
    fun stop_navigation() {
        assertEquals(VoiceCommand.STOP_NAVIGATION, classifier.classify("stop navigation"))
        assertEquals(VoiceCommand.STOP_NAVIGATION, classifier.classify("stop"))
    }

    @Test
    fun unknown() {
        assertEquals(VoiceCommand.UNKNOWN, classifier.classify("play some music"))
        assertEquals(VoiceCommand.UNKNOWN, classifier.classify(""))
    }

    @Test
    fun avoid_tolls() {
        assertEquals(VoiceCommand.AVOID_TOLLS, classifier.classify("avoid tolls"))
    }

    @Test
    fun avoid_highways() {
        assertEquals(VoiceCommand.AVOID_HIGHWAYS, classifier.classify("avoid the highway"))
    }

    @Test
    fun should_i_take() {
        assertEquals(VoiceCommand.SHOULD_I_TAKE, classifier.classify("should I take the flyover"))
    }

    @Test
    fun which_way() {
        assertEquals(VoiceCommand.WHICH_WAY, classifier.classify("which way now"))
    }

    @Test
    fun is_this_correct() {
        assertEquals(VoiceCommand.IS_THIS_CORRECT, classifier.classify("is this the correct turn"))
        assertEquals(VoiceCommand.IS_THIS_CORRECT, classifier.classify("am I going the right way"))
    }
}
