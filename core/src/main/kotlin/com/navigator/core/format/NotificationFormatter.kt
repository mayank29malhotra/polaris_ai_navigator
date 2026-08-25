package com.navigator.core.format

import com.navigator.core.nav.ManeuverType
import com.navigator.core.nav.NavigationState
import com.navigator.core.nav.RouteStatus
import com.navigator.core.watch.WatchPriority
import com.navigator.core.watch.WatchUpdate

/**
 * Turns a [NavigationState] into a compact [WatchUpdate] for a glanceable wearable.
 *
 * Pure string logic with no Android types, so it is fully unit-testable off-device. The
 * distance tiers are intentionally simple and tunable (see the companion constants).
 */
class NotificationFormatter {

    /**
     * Build a watch update, or null when nothing should be shown (no active maneuver, or the
     * next maneuver is farther than [MAX_DISPLAY_METERS]).
     */
    fun format(state: NavigationState): WatchUpdate? {
        when (state.routeStatus) {
            RouteStatus.ARRIVED -> return WatchUpdate("ARRIVED", priority = WatchPriority.CRITICAL)
            RouteStatus.OFF_ROUTE -> return WatchUpdate("REROUTING", priority = WatchPriority.MEDIUM)
            else -> Unit
        }

        val maneuver = state.nextManeuver ?: return null
        val distance = state.nextManeuverDistanceMeters
        val glyph = glyphFor(maneuver)
        val label = labelFor(maneuver)

        if (distance != null && distance <= IMMINENT_METERS) {
            return WatchUpdate("$glyph $label NOW".trim(), priority = WatchPriority.CRITICAL)
        }
        if (distance != null && distance > MAX_DISPLAY_METERS) {
            return null
        }

        val priority = when {
            distance == null -> WatchPriority.MEDIUM
            distance <= NEAR_METERS -> WatchPriority.CRITICAL
            else -> WatchPriority.MEDIUM
        }
        return WatchUpdate(
            primary = "$glyph $label".trim(),
            secondary = distance?.let { formatDistance(it) },
            priority = priority,
        )
    }

    /** Compact watch update for a recalculated route. */
    fun routeUpdated(): WatchUpdate = WatchUpdate("ROUTE UPDATED", priority = WatchPriority.MEDIUM)

    /** Compact watch update for a changed destination. */
    fun destinationChanged(): WatchUpdate =
        WatchUpdate("DESTINATION", "CHANGED", priority = WatchPriority.MEDIUM)

    /** Rider-facing distance string, e.g. "200 m" or "1.2 km". */
    fun formatDistance(meters: Int): String {
        require(meters >= 0) { "distance must be >= 0: $meters" }
        if (meters < 1000) {
            val rounded = ((meters + 5) / 10) * 10
            return "$rounded m"
        }
        val tenthsKm = (meters + 50) / 100
        val whole = tenthsKm / 10
        val frac = tenthsKm % 10
        return if (frac == 0) "$whole km" else "$whole.$frac km"
    }

    /** Directional glyph for a maneuver. */
    fun glyphFor(maneuver: ManeuverType): String = when (maneuver) {
        ManeuverType.TURN_LEFT,
        ManeuverType.SLIGHT_LEFT,
        ManeuverType.SHARP_LEFT,
        ManeuverType.KEEP_LEFT,
        ManeuverType.FORK_LEFT -> "\u21B0"

        ManeuverType.TURN_RIGHT,
        ManeuverType.SLIGHT_RIGHT,
        ManeuverType.SHARP_RIGHT,
        ManeuverType.KEEP_RIGHT,
        ManeuverType.FORK_RIGHT -> "\u21B1"

        ManeuverType.CONTINUE_STRAIGHT,
        ManeuverType.DEPART -> "\u2191"

        ManeuverType.UTURN -> "\u21A9"
        ManeuverType.MERGE -> "\u2933"
        ManeuverType.ROUNDABOUT -> "\u21BB"
        ManeuverType.RAMP -> "\u2197"
        ManeuverType.TAKE_FLYOVER, ManeuverType.TAKE_UNDERPASS -> "\uD83D\uDEE3"
        ManeuverType.ARRIVE -> "\u2691"
        ManeuverType.UNKNOWN -> "\u2022"
    }

    /** Short uppercase label for a maneuver. */
    fun labelFor(maneuver: ManeuverType): String = when (maneuver) {
        ManeuverType.TURN_LEFT -> "LEFT"
        ManeuverType.TURN_RIGHT -> "RIGHT"
        ManeuverType.SLIGHT_LEFT -> "SLIGHT LEFT"
        ManeuverType.SLIGHT_RIGHT -> "SLIGHT RIGHT"
        ManeuverType.SHARP_LEFT -> "SHARP LEFT"
        ManeuverType.SHARP_RIGHT -> "SHARP RIGHT"
        ManeuverType.KEEP_LEFT -> "KEEP LEFT"
        ManeuverType.KEEP_RIGHT -> "KEEP RIGHT"
        ManeuverType.FORK_LEFT -> "FORK LEFT"
        ManeuverType.FORK_RIGHT -> "FORK RIGHT"
        ManeuverType.CONTINUE_STRAIGHT -> "STRAIGHT"
        ManeuverType.DEPART -> "START"
        ManeuverType.UTURN -> "U-TURN"
        ManeuverType.MERGE -> "MERGE"
        ManeuverType.ROUNDABOUT -> "ROUNDABOUT"
        ManeuverType.RAMP -> "RAMP"
        ManeuverType.TAKE_FLYOVER -> "TAKE FLYOVER"
        ManeuverType.TAKE_UNDERPASS -> "TAKE UNDERPASS"
        ManeuverType.ARRIVE -> "ARRIVE"
        ManeuverType.UNKNOWN -> "CONTINUE"
    }

    companion object {
        /** At or below this distance, show "<turn> NOW" with no distance line. */
        const val IMMINENT_METERS = 30

        /** At or below this distance, the update is critical (approaching the turn). */
        const val NEAR_METERS = 100

        /** Above this distance nothing is shown, to avoid noise far from the maneuver. */
        const val MAX_DISPLAY_METERS = 500
    }
}
