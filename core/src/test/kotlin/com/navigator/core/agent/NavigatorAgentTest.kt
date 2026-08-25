package com.navigator.core.agent

import com.navigator.core.geo.LatLng
import com.navigator.core.nav.ManeuverType
import com.navigator.core.nav.NavigationController
import com.navigator.core.nav.NavigationState
import com.navigator.core.nav.NavigationStateStore
import com.navigator.core.trip.Stop
import com.navigator.core.trip.TripManager
import com.navigator.core.tools.NavigatorToolset
import com.navigator.core.tools.ToolContext
import com.navigator.core.tools.ToolRegistry
import com.navigator.core.util.Clock
import com.navigator.core.voice.VoiceCommand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigatorAgentTest {

    private class NoOpController : NavigationController {
        override fun setDestinations(destination: Stop, stops: List<Stop>) {}
        override fun startGuidance() {}
        override fun stopGuidance() {}
        override fun clearNavigation() {}
        override fun startSimulation() {}
        override fun stopSimulation() {}
    }

    private class FakeLlm(private val response: LlmResponse) : LlmClient {
        var called = false
        override fun complete(request: LlmRequest): LlmResponse {
            called = true
            return response
        }
    }

    private val office = Stop(id = "office", name = "Office", location = LatLng(12.97, 77.59))

    private fun registry(tripManager: TripManager): ToolRegistry =
        NavigatorToolset.standard(ToolContext(tripManager, NoOpController(), NavigationStateStore()))

    @Test
    fun obvious_command_is_handled_without_the_llm() {
        val llm = FakeLlm(LlmResponse.Text("should not be used"))
        val agent = NavigatorAgent(llm, registry(TripManager(Clock { 0L })))
        val state = NavigationState(nextManeuver = ManeuverType.TURN_LEFT, nextManeuverDistanceMeters = 200)

        val response = agent.handle("what's next", state)

        assertFalse(llm.called)
        assertFalse(response.usedLlm)
        assertEquals(VoiceCommand.WHATS_NEXT, response.command)
        assertTrue(response.spoken.contains("Turn left"))
    }

    @Test
    fun unknown_command_calls_llm_and_executes_the_tool() {
        val tripManager = TripManager(Clock { 0L })
        tripManager.startTrip(id = "t", destination = office)
        val llm = FakeLlm(
            LlmResponse.ToolCall("add_stop", mapOf("name" to "Orion Mall", "lat" to 13.03, "lng" to 77.55)),
        )
        val agent = NavigatorAgent(llm, registry(tripManager))

        val response = agent.handle("swing by Orion Mall on the way", NavigationState.EMPTY)

        assertTrue(llm.called)
        assertTrue(response.usedLlm)
        assertEquals("add_stop", response.toolName)
        assertEquals(listOf("Orion Mall"), tripManager.current!!.stops.map { it.name })
    }

    @Test
    fun llm_text_response_is_spoken() {
        val llm = FakeLlm(LlmResponse.Text("Which mall?"))
        val agent = NavigatorAgent(llm, registry(TripManager(Clock { 0L })))

        val response = agent.handle("take me to the mall", NavigationState.EMPTY)

        assertTrue(response.usedLlm)
        assertEquals("Which mall?", response.spoken)
    }

    @Test
    fun stop_command_executes_stop_tool_without_the_llm() {
        val tripManager = TripManager(Clock { 0L })
        tripManager.startTrip(id = "t", destination = office)
        val llm = FakeLlm(LlmResponse.Text("unused"))
        val agent = NavigatorAgent(llm, registry(tripManager))

        val response = agent.handle("stop navigation", NavigationState.EMPTY)

        assertFalse(llm.called)
        assertEquals("stop_navigation", response.toolName)
    }

    @Test
    fun avoid_tolls_sets_route_preference_without_the_llm() {
        val tripManager = TripManager(Clock { 0L })
        tripManager.startTrip(id = "t", destination = office)
        val llm = FakeLlm(LlmResponse.Text("unused"))
        val agent = NavigatorAgent(llm, registry(tripManager))

        val response = agent.handle("avoid tolls", NavigationState.EMPTY)

        assertFalse(llm.called)
        assertEquals("set_route_preference", response.toolName)
        assertTrue(tripManager.current!!.routePreference.avoidTolls)
    }

    @Test
    fun should_i_take_is_answered_without_the_llm() {
        val llm = FakeLlm(LlmResponse.Text("unused"))
        val agent = NavigatorAgent(llm, registry(TripManager(Clock { 0L })))
        val state = NavigationState(nextManeuver = ManeuverType.TAKE_FLYOVER, nextManeuverDistanceMeters = 300)

        val response = agent.handle("should I take the flyover", state)

        assertFalse(llm.called)
        assertTrue(response.spoken.startsWith("Yes"))
    }

    @Test
    fun risky_tool_asks_for_confirmation_then_executes_on_yes() {
        val tripManager = TripManager(Clock { 0L })
        tripManager.startTrip(id = "t", destination = office)
        tripManager.addStop(Stop(id = "mall", name = "Mall", location = LatLng(13.0, 77.5)))
        val agent = NavigatorAgent(FakeLlm(LlmResponse.ToolCall("clear_stops", emptyMap())), registry(tripManager))

        val first = agent.handle("remove everything", NavigationState.EMPTY)
        assertTrue(first.awaitingConfirmation)
        assertEquals(1, tripManager.current!!.stops.size)

        val second = agent.handle("yes", NavigationState.EMPTY)
        assertEquals("clear_stops", second.toolName)
        assertTrue(tripManager.current!!.stops.isEmpty())
    }

    @Test
    fun risky_tool_is_cancelled_on_no() {
        val tripManager = TripManager(Clock { 0L })
        tripManager.startTrip(id = "t", destination = office)
        tripManager.addStop(Stop(id = "mall", name = "Mall", location = LatLng(13.0, 77.5)))
        val agent = NavigatorAgent(FakeLlm(LlmResponse.ToolCall("clear_stops", emptyMap())), registry(tripManager))

        agent.handle("remove everything", NavigationState.EMPTY)
        val response = agent.handle("no", NavigationState.EMPTY)

        assertEquals(1, tripManager.current!!.stops.size)
        assertTrue(response.spoken.contains("cancel", ignoreCase = true))
    }

    @Test
    fun unsupported_tool_returns_a_graceful_message() {
        val agent = NavigatorAgent(FakeLlm(LlmResponse.ToolCall("prefer_road", emptyMap())), registry(TripManager(Clock { 0L })))

        val response = agent.handle("take the service road", NavigationState.EMPTY)

        assertEquals(NavigatorAgent.UNSUPPORTED_MESSAGE, response.spoken)
    }
}
