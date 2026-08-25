package com.navigator.core.agent

import com.navigator.core.nav.NavigationState

/** Builds a compact JSON context string describing the current navigation state for the LLM. */
class AgentContextBuilder {

    fun build(state: NavigationState): String = buildString {
        append("{")
        append("\"current_road\":").append(jsonOrNull(state.currentRoad)).append(",")
        append("\"destination\":").append(jsonOrNull(state.destinationName)).append(",")
        append("\"next_maneuver\":").append(jsonOrNull(state.nextManeuver?.name)).append(",")
        append("\"next_maneuver_distance_m\":").append(state.nextManeuverDistanceMeters ?: "null").append(",")
        append("\"next_road\":").append(jsonOrNull(state.nextRoad)).append(",")
        append("\"eta_seconds\":").append(state.etaSeconds ?: "null").append(",")
        append("\"remaining_distance_m\":").append(state.remainingDistanceMeters ?: "null").append(",")
        append("\"route_status\":").append(jsonOrNull(state.routeStatus.name)).append(",")
        append("\"upcoming\":").append(upcomingJson(state))
        append("}")
    }

    private fun upcomingJson(state: NavigationState): String =
        state.upcomingManeuvers.joinToString(prefix = "[", postfix = "]", separator = ",") { m ->
            "{\"maneuver\":${jsonOrNull(m.type.name)},\"distance_m\":${m.distanceMeters}}"
        }

    private fun jsonOrNull(value: String?): String =
        if (value == null) "null" else "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}
