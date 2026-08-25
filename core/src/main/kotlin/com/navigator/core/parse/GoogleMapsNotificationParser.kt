package com.navigator.core.parse

import com.navigator.core.nav.ManeuverType
import com.navigator.core.nav.NavigationState
import com.navigator.core.nav.RouteStatus
import kotlin.math.roundToInt

/**
 * Best-effort parser that turns a Google Maps turn-by-turn notification's text fields into a
 * [NavigationState]. Pure logic (no Android), so it is unit-testable with sample payloads.
 *
 * Live notification wording varies by app version and locale; the regexes/keywords here are
 * tuned against representative English samples and are expected to be adjusted on the build
 * machine against real notifications.
 */
class GoogleMapsNotificationParser {

    fun parse(raw: RawNavNotification): NavigationState? {
        val fields = listOfNotNull(raw.title, raw.text, raw.bigText, raw.subText, raw.ticker)
        if (fields.isEmpty()) return null

        val haystack = fields.joinToString("  ").lowercase()
        val arrival = haystack.contains("arriv")

        val maneuver = if (arrival) ManeuverType.ARRIVE else parseManeuver(haystack)
        val distance = if (arrival) null else fields.firstNotNullOfOrNull { parseDistanceToMeters(it) }
        val road = if (arrival) null else parseRoad(fields)

        if (!arrival && maneuver == ManeuverType.UNKNOWN && distance == null) return null

        return NavigationState(
            nextManeuver = maneuver,
            nextManeuverDistanceMeters = distance,
            nextRoad = road,
            routeStatus = if (arrival) RouteStatus.ARRIVED else RouteStatus.ON_ROUTE,
        )
    }

    // Multi-word variants are checked before the bare "left"/"right" fallbacks.
    private fun parseManeuver(text: String): ManeuverType = when {
        text.contains("u-turn") || text.contains("u turn") || text.contains("make a u") -> ManeuverType.UTURN
        text.contains("roundabout") || text.contains("rotary") -> ManeuverType.ROUNDABOUT
        text.contains("flyover") || text.contains("overpass") -> ManeuverType.TAKE_FLYOVER
        text.contains("underpass") -> ManeuverType.TAKE_UNDERPASS
        text.contains("merge") -> ManeuverType.MERGE
        text.contains("slight left") -> ManeuverType.SLIGHT_LEFT
        text.contains("slight right") -> ManeuverType.SLIGHT_RIGHT
        text.contains("sharp left") -> ManeuverType.SHARP_LEFT
        text.contains("sharp right") -> ManeuverType.SHARP_RIGHT
        text.contains("keep left") -> ManeuverType.KEEP_LEFT
        text.contains("keep right") -> ManeuverType.KEEP_RIGHT
        text.contains("fork") && text.contains("left") -> ManeuverType.FORK_LEFT
        text.contains("fork") && text.contains("right") -> ManeuverType.FORK_RIGHT
        text.contains("ramp") -> ManeuverType.RAMP
        text.contains("turn left") -> ManeuverType.TURN_LEFT
        text.contains("turn right") -> ManeuverType.TURN_RIGHT
        text.contains("left") -> ManeuverType.TURN_LEFT
        text.contains("right") -> ManeuverType.TURN_RIGHT
        text.contains("head") || text.contains("continue") || text.contains("straight") -> ManeuverType.CONTINUE_STRAIGHT
        text.contains("depart") -> ManeuverType.DEPART
        else -> ManeuverType.UNKNOWN
    }

    private fun parseDistanceToMeters(field: String): Int? {
        val match = DISTANCE_REGEX.find(field) ?: return null
        val value = match.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
        val meters = when (match.groupValues[2].lowercase()) {
            "km" -> value * 1000.0
            "m" -> value
            "mi" -> value * 1609.344
            "ft" -> value * 0.3048
            else -> return null
        }
        return meters.roundToInt()
    }

    private fun parseRoad(fields: List<String>): String? {
        for (field in fields) {
            val idx = field.indexOf("onto ", ignoreCase = true)
            if (idx >= 0) {
                val road = field.substring(idx + 5).substringBefore(" in ").trim().trim('\u00B7', '\u2013', '-', ' ')
                if (road.isNotEmpty()) return road
            }
        }
        for (field in fields) {
            for (segment in field.split('\u00B7', '\u2013', '|')) {
                val candidate = segment.trim()
                if (candidate.isNotEmpty() &&
                    DISTANCE_REGEX.find(candidate) == null &&
                    !TIME_REGEX.containsMatchIn(candidate) &&
                    parseManeuver(candidate.lowercase()) == ManeuverType.UNKNOWN
                ) {
                    return candidate
                }
            }
        }
        return null
    }

    companion object {
        private val DISTANCE_REGEX =
            Regex("""(\d+(?:[.,]\d+)?)\s*(km|m|mi|ft)\b""", RegexOption.IGNORE_CASE)
        private val TIME_REGEX =
            Regex("""\d{1,2}:\d{2}|\b\d+\s*(?:min|mins|hr|hrs|hour|hours|h)\b""", RegexOption.IGNORE_CASE)
    }
}
