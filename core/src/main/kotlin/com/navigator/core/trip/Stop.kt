package com.navigator.core.trip

import com.navigator.core.geo.LatLng

/** Role of a stop within a trip. */
enum class StopType { WAYPOINT, DESTINATION }

/** A place in a trip: an intermediate waypoint or the final destination. */
data class Stop(
    val id: String,
    val name: String,
    val location: LatLng,
    val placeId: String? = null,
    val order: Int = 0,
    val type: StopType = StopType.WAYPOINT,
)
