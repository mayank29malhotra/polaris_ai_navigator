package com.navigator.core.trip

import com.navigator.core.geo.LatLng
import com.navigator.core.util.Clock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class TripManagerTest {

    private class FakeClock(var value: Long = 0L) : Clock {
        override fun nowMillis(): Long = value
    }

    private fun stop(id: String) = Stop(id = id, name = id, location = LatLng(12.9716, 77.5946))

    @Test
    fun startTrip_sets_active_trip_with_timestamps() {
        val clock = FakeClock(1_000L)
        val manager = TripManager(clock)

        val trip = manager.startTrip(id = "t1", destination = stop("office"))

        assertEquals("t1", manager.current?.id)
        assertEquals(TripStatus.ACTIVE, trip.status)
        assertEquals(1_000L, trip.startedAt)
        assertEquals(1_000L, trip.updatedAt)
        assertEquals(StopType.DESTINATION, trip.destination.type)
    }

    @Test
    fun edit_updates_updatedAt_but_not_startedAt() {
        val clock = FakeClock(1_000L)
        val manager = TripManager(clock)
        manager.startTrip(id = "t1", destination = stop("office"))

        clock.value = 5_000L
        val trip = manager.addStop(stop("gym"))

        assertEquals(1_000L, trip.startedAt)
        assertEquals(5_000L, trip.updatedAt)
        assertEquals(listOf("gym"), trip.stops.map { it.id })
    }

    @Test
    fun editing_without_active_trip_throws() {
        val manager = TripManager(FakeClock())
        assertThrows(IllegalStateException::class.java) { manager.addStop(stop("gym")) }
    }

    @Test
    fun current_is_null_before_start() {
        assertNull(TripManager(FakeClock()).current)
    }

    @Test
    fun markArrived_and_cancel_update_status() {
        val manager = TripManager(FakeClock())
        manager.startTrip(id = "t1", destination = stop("office"))

        assertEquals(TripStatus.ARRIVED, manager.markArrived().status)
        assertEquals(TripStatus.CANCELLED, manager.cancel().status)
    }
}
