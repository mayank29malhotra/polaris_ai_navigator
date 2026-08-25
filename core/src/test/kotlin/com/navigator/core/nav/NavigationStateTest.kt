package com.navigator.core.nav

import com.navigator.core.geo.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationStateTest {

    @Test
    fun empty_has_unknown_status_and_no_maneuvers() {
        val s = NavigationState.EMPTY
        assertEquals(RouteStatus.UNKNOWN, s.routeStatus)
        assertTrue(s.upcomingManeuvers.isEmpty())
    }

    @Test
    fun holds_provided_values() {
        val s = NavigationState(
            currentLocation = LatLng(12.9716, 77.5946),
            currentRoad = "Outer Ring Road",
            nextManeuver = ManeuverType.TAKE_FLYOVER,
            nextManeuverDistanceMeters = 280,
            nextRoad = "Hebbal Flyover",
            upcomingManeuvers = listOf(Maneuver(ManeuverType.KEEP_RIGHT, 1200, "Ballari Road")),
            destinationName = "Office",
            etaSeconds = 900,
            remainingDistanceMeters = 5400,
            routeStatus = RouteStatus.ON_ROUTE,
        )

        assertEquals(ManeuverType.TAKE_FLYOVER, s.nextManeuver)
        assertEquals(1, s.upcomingManeuvers.size)
        assertEquals("Hebbal Flyover", s.nextRoad)
    }
}
