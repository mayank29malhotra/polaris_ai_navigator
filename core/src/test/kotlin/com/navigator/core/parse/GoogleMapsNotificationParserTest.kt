package com.navigator.core.parse

import com.navigator.core.nav.ManeuverType
import com.navigator.core.nav.RouteStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GoogleMapsNotificationParserTest {

    private val parser = GoogleMapsNotificationParser()

    @Test
    fun parses_turn_left_with_distance_and_road() {
        val s = parser.parse(RawNavNotification(title = "Turn left", text = "200 m \u00B7 MG Road"))!!
        assertEquals(ManeuverType.TURN_LEFT, s.nextManeuver)
        assertEquals(200, s.nextManeuverDistanceMeters)
        assertEquals("MG Road", s.nextRoad)
        assertEquals(RouteStatus.ON_ROUTE, s.routeStatus)
    }

    @Test
    fun parses_onto_road_and_kilometres() {
        val s = parser.parse(RawNavNotification(title = "Slight right onto Ballari Road", text = "1.2 km"))!!
        assertEquals(ManeuverType.SLIGHT_RIGHT, s.nextManeuver)
        assertEquals(1200, s.nextManeuverDistanceMeters)
        assertEquals("Ballari Road", s.nextRoad)
    }

    @Test
    fun parses_imperial_feet() {
        val s = parser.parse(RawNavNotification(title = "Keep right", text = "500 ft \u00B7 Hebbal"))!!
        assertEquals(ManeuverType.KEEP_RIGHT, s.nextManeuver)
        assertEquals(152, s.nextManeuverDistanceMeters)
        assertEquals("Hebbal", s.nextRoad)
    }

    @Test
    fun detects_arrival() {
        val s = parser.parse(RawNavNotification(title = "Arriving at Office", text = "Office"))!!
        assertEquals(ManeuverType.ARRIVE, s.nextManeuver)
        assertEquals(RouteStatus.ARRIVED, s.routeStatus)
        assertNull(s.nextManeuverDistanceMeters)
    }

    @Test
    fun ignores_eta_minutes_and_uses_maneuver_distance() {
        val s = parser.parse(RawNavNotification(title = "Take the flyover", text = "12 min \u00B7 3.4 km"))!!
        assertEquals(ManeuverType.TAKE_FLYOVER, s.nextManeuver)
        assertEquals(3400, s.nextManeuverDistanceMeters)
    }

    @Test
    fun returns_null_when_unrecognized() {
        assertNull(parser.parse(RawNavNotification(title = "Google Maps", text = "Navigation")))
    }

    @Test
    fun returns_null_when_empty() {
        assertNull(parser.parse(RawNavNotification(title = null, text = null)))
    }

    @Test
    fun distance_only_still_produces_state() {
        val s = parser.parse(RawNavNotification(title = "300 m", text = null))!!
        assertEquals(300, s.nextManeuverDistanceMeters)
    }
}
