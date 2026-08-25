package com.navigator.core.nav

import com.navigator.core.geo.LatLng

/**
 * Immutable read-model of the live navigation state, produced by the navigation engine and
 * consumed by the agent, TTS, and watch layers. This is the single source of truth for
 * "where am I / what's next" (see decision D-005).
 */
data class NavigationState(
    val currentLocation: LatLng? = null,
    val currentRoad: String? = null,
    val nextManeuver: ManeuverType? = null,
    val nextManeuverDistanceMeters: Int? = null,
    val nextRoad: String? = null,
    val upcomingManeuvers: List<Maneuver> = emptyList(),
    val destinationName: String? = null,
    val etaSeconds: Int? = null,
    val remainingDistanceMeters: Int? = null,
    val routeStatus: RouteStatus = RouteStatus.UNKNOWN,
) {
    companion object {
        val EMPTY = NavigationState()
    }
}
