package com.navigator.core.agent

import com.navigator.core.nav.Maneuver
import com.navigator.core.nav.ManeuverType
import com.navigator.core.nav.NavigationState
import com.navigator.core.nav.RouteStatus
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentContextBuilderTest {

    private val builder = AgentContextBuilder()

    @Test
    fun includes_key_fields_and_upcoming() {
        val json = builder.build(
            NavigationState(
                currentRoad = "Outer Ring Road",
                nextManeuver = ManeuverType.TAKE_FLYOVER,
                nextManeuverDistanceMeters = 280,
                destinationName = "Office",
                routeStatus = RouteStatus.ON_ROUTE,
                upcomingManeuvers = listOf(Maneuver(ManeuverType.KEEP_RIGHT, 1200, "Ballari Road")),
            ),
        )

        assertTrue(json.contains("\"current_road\":\"Outer Ring Road\""))
        assertTrue(json.contains("\"next_maneuver\":\"TAKE_FLYOVER\""))
        assertTrue(json.contains("\"route_status\":\"ON_ROUTE\""))
        assertTrue(json.contains("\"upcoming\":[{\"maneuver\":\"KEEP_RIGHT\",\"distance_m\":1200}]"))
    }

    @Test
    fun handles_empty_state() {
        val json = builder.build(NavigationState.EMPTY)
        assertTrue(json.contains("\"current_road\":null"))
        assertTrue(json.contains("\"upcoming\":[]"))
    }
}
