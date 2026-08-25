package com.navigator.core.trip

import com.navigator.core.util.Clock

/**
 * Holds the single active [Trip] and applies edits, stamping [Trip.updatedAt] from [clock].
 *
 * This is the one place trip state changes; the agent and UI drive it through these methods
 * rather than mutating [Trip] values directly.
 */
class TripManager(private val clock: Clock = Clock.SYSTEM) {

    private var activeTrip: Trip? = null

    /** The current trip, or null if none is active. */
    val current: Trip? get() = activeTrip

    fun startTrip(
        id: String,
        destination: Stop,
        preference: RoutePreference = RoutePreference.DEFAULT,
    ): Trip {
        val now = clock.nowMillis()
        return Trip(
            id = id,
            destination = destination.copy(type = StopType.DESTINATION),
            routePreference = preference,
            status = TripStatus.ACTIVE,
            startedAt = now,
            updatedAt = now,
        ).also { activeTrip = it }
    }

    fun addStop(stop: Stop): Trip = edit { it.addStop(stop) }

    fun insertStop(index: Int, stop: Stop): Trip = edit { it.insertStop(index, stop) }

    fun removeStopAt(index: Int): Trip = edit { it.removeStopAt(index) }

    fun removeStop(stopId: String): Trip = edit { it.removeStopById(stopId) }

    fun reorderStop(fromIndex: Int, toIndex: Int): Trip = edit { it.reorderStop(fromIndex, toIndex) }

    fun clearStops(): Trip = edit { it.clearStops() }

    fun changeDestination(destination: Stop): Trip = edit { it.changeDestination(destination) }

    fun setRoutePreference(preference: RoutePreference): Trip = edit { it.withRoutePreference(preference) }

    fun markArrived(): Trip = edit { it.copy(status = TripStatus.ARRIVED) }

    fun cancel(): Trip = edit { it.copy(status = TripStatus.CANCELLED) }

    private fun edit(transform: (Trip) -> Trip): Trip {
        val trip = activeTrip ?: error("No active trip. Call startTrip() first.")
        return transform(trip).copy(updatedAt = clock.nowMillis()).also { activeTrip = it }
    }
}
