package com.navigator.core.trip

/** Routing preferences the agent can toggle; mapped to SDK routing options later. */
data class RoutePreference(
    val avoidTolls: Boolean = false,
    val avoidHighways: Boolean = false,
    val avoidFerries: Boolean = false,
) {
    companion object {
        val DEFAULT = RoutePreference()
    }
}
