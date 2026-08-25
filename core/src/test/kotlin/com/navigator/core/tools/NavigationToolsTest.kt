package com.navigator.core.tools

import com.navigator.core.nav.NavigationController
import com.navigator.core.nav.NavigationState
import com.navigator.core.nav.NavigationStateStore
import com.navigator.core.nav.RouteStatus
import com.navigator.core.trip.Stop
import com.navigator.core.trip.TripManager
import com.navigator.core.util.Clock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationToolsTest {

    private class RecordingController : NavigationController {
        var lastDestination: Stop? = null
        var lastStops: List<Stop> = emptyList()
        override fun setDestinations(destination: Stop, stops: List<Stop>) {
            lastDestination = destination
            lastStops = stops
        }
        override fun startGuidance() {}
        override fun stopGuidance() {}
        override fun clearNavigation() {}
        override fun startSimulation() {}
        override fun stopSimulation() {}
    }

    private fun context(): Pair<ToolContext, RecordingController> {
        val controller = RecordingController()
        val ctx = ToolContext(
            tripManager = TripManager(Clock { 0L }),
            controller = controller,
            store = NavigationStateStore(),
        )
        return ctx to controller
    }

    private fun office() = ToolArgs.of("name" to "Office", "lat" to 12.97, "lng" to 77.59)
    private fun mall() = ToolArgs.of("name" to "Orion Mall", "lat" to 13.03, "lng" to 77.55)

    @Test
    fun set_destination_starts_trip_and_applies_to_controller() {
        val (ctx, controller) = context()
        val registry = NavigatorToolset.standard(ctx)

        val result = registry.execute("set_destination", office())

        assertTrue(result.success)
        assertEquals("Office", ctx.tripManager.current?.destination?.name)
        assertEquals("Office", controller.lastDestination?.name)
    }

    @Test
    fun add_stop_requires_active_trip() {
        val (ctx, _) = context()
        val registry = NavigatorToolset.standard(ctx)

        assertFalse(registry.execute("add_stop", mall()).success)
    }

    @Test
    fun add_stop_after_destination_updates_waypoints() {
        val (ctx, controller) = context()
        val registry = NavigatorToolset.standard(ctx)
        registry.execute("set_destination", office())

        val result = registry.execute("add_stop", mall())

        assertTrue(result.success)
        assertEquals(listOf("Orion Mall"), controller.lastStops.map { it.name })
        assertEquals("Office", controller.lastDestination?.name)
    }

    @Test
    fun remove_stop_without_name_removes_next() {
        val (ctx, _) = context()
        val registry = NavigatorToolset.standard(ctx)
        registry.execute("set_destination", office())
        registry.execute("add_stop", mall())

        val result = registry.execute("remove_stop", ToolArgs.EMPTY)

        assertTrue(result.success)
        assertTrue(ctx.tripManager.current!!.stops.isEmpty())
    }

    @Test
    fun clear_stops_keeps_destination() {
        val (ctx, _) = context()
        val registry = NavigatorToolset.standard(ctx)
        registry.execute("set_destination", office())
        registry.execute("add_stop", mall())

        registry.execute("clear_stops")

        assertTrue(ctx.tripManager.current!!.stops.isEmpty())
        assertEquals("Office", ctx.tripManager.current!!.destination.name)
    }

    @Test
    fun invalid_location_returns_error() {
        val (ctx, _) = context()
        val registry = NavigatorToolset.standard(ctx)

        val result = registry.execute("set_destination", ToolArgs.of("name" to "Bad", "lat" to 200.0, "lng" to 0.0))

        assertFalse(result.success)
    }

    @Test
    fun unknown_tool_returns_error() {
        val (ctx, _) = context()
        val registry = NavigatorToolset.standard(ctx)

        assertFalse(registry.execute("nope").success)
    }

    @Test
    fun get_navigation_state_reads_store() {
        val (ctx, _) = context()
        ctx.store.update(NavigationState(destinationName = "Office", routeStatus = RouteStatus.ON_ROUTE))
        val registry = NavigatorToolset.standard(ctx)

        val result = registry.execute("get_navigation_state")

        assertTrue(result.success)
        assertEquals("Office", result.data["destination"])
    }
}
