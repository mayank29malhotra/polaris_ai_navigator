package com.navigator.core.trip

/** Lifecycle state of a trip. */
enum class TripStatus { PLANNING, ACTIVE, ARRIVED, CANCELLED }

/**
 * Immutable trip: an ordered list of intermediate [stops] plus a final [destination].
 *
 * Every editing method returns a new [Trip] and never reads the clock, so the logic is
 * deterministic and unit-testable. Timestamping is handled by [TripManager].
 */
data class Trip(
    val id: String,
    val destination: Stop,
    val stops: List<Stop> = emptyList(),
    val routePreference: RoutePreference = RoutePreference.DEFAULT,
    val status: TripStatus = TripStatus.ACTIVE,
    val startedAt: Long = 0L,
    val updatedAt: Long = 0L,
) {
    /** Ordered SDK waypoint list: intermediate stops followed by the final destination. */
    fun orderedWaypoints(): List<Stop> =
        reindex(stops) + destination.copy(order = stops.size, type = StopType.DESTINATION)

    fun addStop(stop: Stop): Trip {
        requireWithinLimit()
        return copy(stops = reindex(stops + stop.copy(type = StopType.WAYPOINT)))
    }

    fun insertStop(index: Int, stop: Stop): Trip {
        require(index in 0..stops.size) { "insert index $index out of range 0..${stops.size}" }
        requireWithinLimit()
        val updated = stops.toMutableList().apply { add(index, stop.copy(type = StopType.WAYPOINT)) }
        return copy(stops = reindex(updated))
    }

    fun removeStopAt(index: Int): Trip {
        require(index in stops.indices) { "remove index $index out of range ${stops.indices}" }
        return copy(stops = reindex(stops.toMutableList().apply { removeAt(index) }))
    }

    fun removeStopById(stopId: String): Trip {
        val filtered = stops.filterNot { it.id == stopId }
        require(filtered.size != stops.size) { "No stop with id '$stopId'." }
        return copy(stops = reindex(filtered))
    }

    fun reorderStop(fromIndex: Int, toIndex: Int): Trip {
        require(fromIndex in stops.indices) { "fromIndex $fromIndex out of range ${stops.indices}" }
        require(toIndex in stops.indices) { "toIndex $toIndex out of range ${stops.indices}" }
        if (fromIndex == toIndex) return this
        val updated = stops.toMutableList()
        updated.add(toIndex, updated.removeAt(fromIndex))
        return copy(stops = reindex(updated))
    }

    fun clearStops(): Trip = copy(stops = emptyList())

    fun changeDestination(newDestination: Stop): Trip =
        copy(destination = newDestination.copy(order = stops.size, type = StopType.DESTINATION))

    fun withRoutePreference(preference: RoutePreference): Trip =
        copy(routePreference = preference)

    private fun requireWithinLimit() =
        require(stops.size + 2 <= MAX_WAYPOINTS) {
            "Cannot add stop: at most $MAX_WAYPOINTS waypoints including the destination."
        }

    private fun reindex(list: List<Stop>): List<Stop> =
        list.mapIndexed { i, stop -> stop.copy(order = i, type = StopType.WAYPOINT) }

    companion object {
        /** Google Navigation SDK limit: 25 waypoints including the final destination. */
        const val MAX_WAYPOINTS = 25
    }
}
