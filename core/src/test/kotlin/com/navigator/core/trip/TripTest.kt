package com.navigator.core.trip

import com.navigator.core.geo.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class TripTest {

    private fun stop(id: String, name: String = id) =
        Stop(id = id, name = name, location = LatLng(12.9716, 77.5946))

    private fun newTrip() = Trip(id = "trip-1", destination = stop("office", "Office"))

    @Test
    fun destination_is_marked_and_ordered_last() {
        val trip = newTrip().addStop(stop("gym")).addStop(stop("mall"))
        val ordered = trip.orderedWaypoints()

        assertEquals(listOf("gym", "mall", "office"), ordered.map { it.id })
        assertEquals(StopType.DESTINATION, ordered.last().type)
        assertEquals(2, ordered.last().order)
    }

    @Test
    fun addStop_appends_and_reindexes() {
        val trip = newTrip().addStop(stop("a")).addStop(stop("b"))
        assertEquals(listOf("a", "b"), trip.stops.map { it.id })
        assertEquals(listOf(0, 1), trip.stops.map { it.order })
    }

    @Test
    fun insertStop_places_at_index() {
        val trip = newTrip().addStop(stop("a")).addStop(stop("c")).insertStop(1, stop("b"))
        assertEquals(listOf("a", "b", "c"), trip.stops.map { it.id })
        assertEquals(listOf(0, 1, 2), trip.stops.map { it.order })
    }

    @Test
    fun removeStopAt_removes_and_reindexes() {
        val trip = newTrip().addStop(stop("a")).addStop(stop("b")).removeStopAt(0)
        assertEquals(listOf("b"), trip.stops.map { it.id })
        assertEquals(listOf(0), trip.stops.map { it.order })
    }

    @Test
    fun removeStopById_removes_matching() {
        val trip = newTrip().addStop(stop("a")).addStop(stop("b")).removeStopById("a")
        assertEquals(listOf("b"), trip.stops.map { it.id })
    }

    @Test
    fun removeStopById_unknown_throws() {
        val trip = newTrip().addStop(stop("a"))
        assertThrows(IllegalArgumentException::class.java) { trip.removeStopById("zzz") }
    }

    @Test
    fun reorderStop_moves_element() {
        val trip = newTrip().addStop(stop("a")).addStop(stop("b")).addStop(stop("c"))
            .reorderStop(2, 0)
        assertEquals(listOf("c", "a", "b"), trip.stops.map { it.id })
        assertEquals(listOf(0, 1, 2), trip.stops.map { it.order })
    }

    @Test
    fun reorderStop_out_of_range_throws() {
        val trip = newTrip().addStop(stop("a"))
        assertThrows(IllegalArgumentException::class.java) { trip.reorderStop(0, 5) }
    }

    @Test
    fun clearStops_keeps_destination() {
        val trip = newTrip().addStop(stop("a")).addStop(stop("b")).clearStops()
        assertTrue(trip.stops.isEmpty())
        assertEquals("office", trip.destination.id)
        assertEquals(listOf("office"), trip.orderedWaypoints().map { it.id })
    }

    @Test
    fun changeDestination_replaces_final() {
        val trip = newTrip().addStop(stop("a")).changeDestination(stop("home", "Home"))
        assertEquals("home", trip.destination.id)
        assertEquals(StopType.DESTINATION, trip.destination.type)
        assertEquals(listOf("a", "home"), trip.orderedWaypoints().map { it.id })
    }

    @Test
    fun addStop_enforces_waypoint_limit() {
        var trip = newTrip()
        repeat(Trip.MAX_WAYPOINTS - 1) { i -> trip = trip.addStop(stop("s$i")) }

        assertEquals(Trip.MAX_WAYPOINTS, trip.orderedWaypoints().size)
        assertThrows(IllegalArgumentException::class.java) { trip.addStop(stop("overflow")) }
    }

    @Test
    fun editing_returns_new_instance_and_leaves_original_unchanged() {
        val original = newTrip().addStop(stop("a"))
        original.addStop(stop("b"))
        assertEquals(listOf("a"), original.stops.map { it.id })
    }
}
