package com.navigator.core.nav

/** A single upcoming maneuver with the distance to it and the road it leads onto. */
data class Maneuver(
    val type: ManeuverType,
    val distanceMeters: Int,
    val road: String? = null,
)
