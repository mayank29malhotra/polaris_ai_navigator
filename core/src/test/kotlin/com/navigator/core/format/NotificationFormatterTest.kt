package com.navigator.core.format

import com.navigator.core.nav.ManeuverType
import com.navigator.core.nav.NavigationState
import com.navigator.core.nav.RouteStatus
import com.navigator.core.watch.WatchPriority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationFormatterTest {

    private val formatter = NotificationFormatter()

    private fun state(
        maneuver: ManeuverType? = ManeuverType.TURN_LEFT,
        distance: Int? = 200,
        status: RouteStatus = RouteStatus.ON_ROUTE,
    ) = NavigationState(
        nextManeuver = maneuver,
        nextManeuverDistanceMeters = distance,
        routeStatus = status,
    )

    @Test
    fun formats_turn_with_distance_and_medium_priority() {
        val update = formatter.format(state(ManeuverType.TURN_LEFT, 200))!!
        assertEquals(formatter.glyphFor(ManeuverType.TURN_LEFT) + " LEFT", update.primary)
        assertEquals("200 m", update.secondary)
        assertEquals(WatchPriority.MEDIUM, update.priority)
    }

    @Test
    fun imminent_maneuver_shows_now_and_is_critical() {
        val update = formatter.format(state(ManeuverType.TURN_RIGHT, 20))!!
        assertTrue(update.primary.endsWith("RIGHT NOW"))
        assertNull(update.secondary)
        assertEquals(WatchPriority.CRITICAL, update.priority)
    }

    @Test
    fun near_maneuver_is_critical_with_distance() {
        val update = formatter.format(state(ManeuverType.TURN_LEFT, 80))!!
        assertEquals("80 m", update.secondary)
        assertEquals(WatchPriority.CRITICAL, update.priority)
    }

    @Test
    fun far_maneuver_is_not_shown() {
        assertNull(formatter.format(state(ManeuverType.TURN_LEFT, 800)))
    }

    @Test
    fun arrival_takes_precedence() {
        val update = formatter.format(state(status = RouteStatus.ARRIVED))!!
        assertEquals("ARRIVED", update.primary)
        assertEquals(WatchPriority.CRITICAL, update.priority)
    }

    @Test
    fun off_route_shows_rerouting() {
        val update = formatter.format(state(status = RouteStatus.OFF_ROUTE))!!
        assertEquals("REROUTING", update.primary)
    }

    @Test
    fun no_maneuver_returns_null() {
        assertNull(formatter.format(state(maneuver = null, distance = null)))
    }

    @Test
    fun unknown_distance_is_medium_without_secondary() {
        val update = formatter.format(state(ManeuverType.CONTINUE_STRAIGHT, null))!!
        assertNull(update.secondary)
        assertEquals(WatchPriority.MEDIUM, update.priority)
    }

    @Test
    fun formats_meters_rounded_to_ten() {
        assertEquals("200 m", formatter.formatDistance(200))
        assertEquals("50 m", formatter.formatDistance(48))
        assertEquals("950 m", formatter.formatDistance(950))
    }

    @Test
    fun formats_kilometres() {
        assertEquals("1.2 km", formatter.formatDistance(1200))
        assertEquals("5 km", formatter.formatDistance(5000))
        assertEquals("1 km", formatter.formatDistance(1000))
    }

    @Test
    fun route_and_destination_change_helpers() {
        assertEquals("ROUTE UPDATED", formatter.routeUpdated().primary)
        assertEquals("DESTINATION", formatter.destinationChanged().primary)
        assertEquals("CHANGED", formatter.destinationChanged().secondary)
    }
}
