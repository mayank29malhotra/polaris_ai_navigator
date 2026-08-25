package com.navigator.core.voice

import com.navigator.core.nav.ManeuverType
import com.navigator.core.nav.NavigationState
import com.navigator.core.nav.RouteStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class SpokenInstructionFormatterTest {

    private val formatter = SpokenInstructionFormatter()

    @Test
    fun next_instruction_with_distance() {
        val s = NavigationState(nextManeuver = ManeuverType.TURN_LEFT, nextManeuverDistanceMeters = 200)
        assertEquals("Turn left in 200 metres.", formatter.nextInstruction(s))
    }

    @Test
    fun next_instruction_flyover_kilometres() {
        val s = NavigationState(nextManeuver = ManeuverType.TAKE_FLYOVER, nextManeuverDistanceMeters = 1200)
        assertEquals("Take the flyover in 1.2 kilometres.", formatter.nextInstruction(s))
    }

    @Test
    fun arrival() {
        val s = NavigationState(routeStatus = RouteStatus.ARRIVED)
        assertEquals("You have arrived.", formatter.nextInstruction(s))
    }

    @Test
    fun no_maneuver() {
        assertEquals("Continue on your route.", formatter.nextInstruction(NavigationState.EMPTY))
    }

    @Test
    fun how_far_uses_remaining_distance() {
        val s = NavigationState(remainingDistanceMeters = 5400)
        assertEquals("About 5.4 kilometres to go.", formatter.howFar(s))
    }

    @Test
    fun how_long_minutes() {
        val s = NavigationState(etaSeconds = 900)
        assertEquals("About 15 minutes.", formatter.howLong(s))
    }

    @Test
    fun destination_name() {
        val s = NavigationState(destinationName = "Office")
        assertEquals("Heading to Office.", formatter.destination(s))
    }

    @Test
    fun respond_to_stop() {
        assertEquals("Navigation stopped.", formatter.respondTo(VoiceCommand.STOP_NAVIGATION, NavigationState.EMPTY))
    }

    @Test
    fun respond_to_whats_next_matches_next_instruction() {
        val s = NavigationState(nextManeuver = ManeuverType.TURN_RIGHT, nextManeuverDistanceMeters = 300)
        assertEquals(formatter.nextInstruction(s), formatter.respondTo(VoiceCommand.WHATS_NEXT, s))
    }

    @Test
    fun should_i_take_flyover_yes() {
        val s = NavigationState(nextManeuver = ManeuverType.TAKE_FLYOVER, nextManeuverDistanceMeters = 280)
        assertEquals("Yes, take the flyover in 280 metres.", formatter.shouldITake("should I take the flyover", s))
    }

    @Test
    fun should_i_take_flyover_no() {
        val s = NavigationState(nextManeuver = ManeuverType.TURN_LEFT, nextManeuverDistanceMeters = 200)
        assertEquals("No, stay on your current road for now.", formatter.shouldITake("should I take the flyover", s))
    }

    @Test
    fun is_this_correct_on_route() {
        assertEquals("Yes, you're on the right route.", formatter.isThisCorrect(NavigationState(routeStatus = RouteStatus.ON_ROUTE)))
    }

    @Test
    fun is_this_correct_off_route() {
        assertEquals("No, you're off route. Recalculating.", formatter.isThisCorrect(NavigationState(routeStatus = RouteStatus.OFF_ROUTE)))
    }
}
