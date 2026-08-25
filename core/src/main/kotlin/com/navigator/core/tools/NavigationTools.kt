package com.navigator.core.tools

import com.navigator.core.geo.LatLng
import com.navigator.core.nav.NavigationController
import com.navigator.core.nav.NavigationStateStore
import com.navigator.core.trip.RoutePreference
import com.navigator.core.trip.Stop
import com.navigator.core.trip.TripManager
import com.navigator.core.voice.SpokenInstructionFormatter

/** Shared dependencies for the navigation tools. */
class ToolContext(
    val tripManager: TripManager,
    val controller: NavigationController,
    val store: NavigationStateStore,
    val formatter: SpokenInstructionFormatter = SpokenInstructionFormatter(),
) {
    /** Push the current trip's ordered waypoints to the navigation controller. */
    fun applyTrip(): Boolean {
        val trip = tripManager.current ?: return false
        controller.setDestinations(trip.destination, trip.stops)
        return true
    }
}

private fun stopFromArgs(args: ToolArgs): Stop {
    val name = args.string("name") ?: throw IllegalArgumentException("Missing 'name'.")
    val lat = args.double("lat") ?: throw IllegalArgumentException("Missing 'lat'.")
    val lng = args.double("lng") ?: throw IllegalArgumentException("Missing 'lng'.")
    val id = args.string("id") ?: name.lowercase().replace(Regex("\\s+"), "-")
    return Stop(id = id, name = name, location = LatLng(lat, lng))
}

private inline fun guarded(block: () -> ToolResult): ToolResult =
    try {
        block()
    } catch (e: IllegalArgumentException) {
        ToolResult.error(e.message ?: "Invalid arguments.")
    }

class SetDestinationTool(private val ctx: ToolContext) : NavigatorTool {
    override val name = "set_destination"
    override val description = "Set or replace the final destination and calculate a route."
    override val parameters = listOf(
        ToolParameter("name", ToolParamType.STRING, true, "Destination name."),
        ToolParameter("lat", ToolParamType.NUMBER, true, "Latitude."),
        ToolParameter("lng", ToolParamType.NUMBER, true, "Longitude."),
    )

    override fun execute(args: ToolArgs): ToolResult = guarded {
        val stop = stopFromArgs(args)
        if (ctx.tripManager.current == null) {
            ctx.tripManager.startTrip(id = stop.id, destination = stop)
        } else {
            ctx.tripManager.changeDestination(stop)
        }
        ctx.applyTrip()
        ToolResult.ok("Destination set to ${stop.name}.")
    }
}

class AddStopTool(private val ctx: ToolContext) : NavigatorTool {
    override val name = "add_stop"
    override val description = "Add an intermediate stop to the active trip."
    override val parameters = listOf(
        ToolParameter("name", ToolParamType.STRING, true, "Stop name."),
        ToolParameter("lat", ToolParamType.NUMBER, true, "Latitude."),
        ToolParameter("lng", ToolParamType.NUMBER, true, "Longitude."),
    )

    override fun execute(args: ToolArgs): ToolResult = guarded {
        if (ctx.tripManager.current == null) return@guarded ToolResult.error("No active trip. Set a destination first.")
        val stop = stopFromArgs(args)
        ctx.tripManager.addStop(stop)
        ctx.applyTrip()
        ToolResult.ok("Added stop ${stop.name}.")
    }
}

class RemoveStopTool(private val ctx: ToolContext) : NavigatorTool {
    override val name = "remove_stop"
    override val description = "Remove a stop by name, or the next stop if no name is given."
    override val parameters = listOf(
        ToolParameter("name", ToolParamType.STRING, false, "Stop name to remove (optional)."),
    )

    override fun execute(args: ToolArgs): ToolResult = guarded {
        val trip = ctx.tripManager.current ?: return@guarded ToolResult.error("No active trip.")
        if (trip.stops.isEmpty()) return@guarded ToolResult.error("There are no stops to remove.")
        val name = args.string("name")
        if (name == null) {
            val removed = trip.stops.first().name
            ctx.tripManager.removeStopAt(0)
            ctx.applyTrip()
            ToolResult.ok("Removed the next stop, $removed.")
        } else {
            val match = trip.stops.firstOrNull { it.name.equals(name, ignoreCase = true) }
                ?: return@guarded ToolResult.error("No stop named $name.")
            ctx.tripManager.removeStop(match.id)
            ctx.applyTrip()
            ToolResult.ok("Removed stop ${match.name}.")
        }
    }
}

class ReorderStopTool(private val ctx: ToolContext) : NavigatorTool {
    override val name = "reorder_stop"
    override val description = "Move a stop from one position to another (0-based)."
    override val parameters = listOf(
        ToolParameter("from", ToolParamType.INTEGER, true, "Current index."),
        ToolParameter("to", ToolParamType.INTEGER, true, "Target index."),
    )

    override fun execute(args: ToolArgs): ToolResult = guarded {
        if (ctx.tripManager.current == null) return@guarded ToolResult.error("No active trip.")
        val from = args.int("from") ?: return@guarded ToolResult.error("Missing 'from'.")
        val to = args.int("to") ?: return@guarded ToolResult.error("Missing 'to'.")
        ctx.tripManager.reorderStop(from, to)
        ctx.applyTrip()
        ToolResult.ok("Moved stop $from to $to.")
    }
}

class ClearStopsTool(private val ctx: ToolContext) : NavigatorTool {
    override val name = "clear_stops"
    override val description = "Remove all intermediate stops, keeping the destination."
    override val parameters = emptyList<ToolParameter>()

    override fun execute(args: ToolArgs): ToolResult = guarded {
        if (ctx.tripManager.current == null) return@guarded ToolResult.error("No active trip.")
        ctx.tripManager.clearStops()
        ctx.applyTrip()
        ToolResult.ok("Cleared all stops.")
    }
}

class SetRoutePreferenceTool(private val ctx: ToolContext) : NavigatorTool {
    override val name = "set_route_preference"
    override val description = "Set routing preferences such as avoiding tolls or highways."
    override val parameters = listOf(
        ToolParameter("avoid_tolls", ToolParamType.BOOLEAN, false, "Avoid toll roads."),
        ToolParameter("avoid_highways", ToolParamType.BOOLEAN, false, "Avoid highways."),
    )

    override fun execute(args: ToolArgs): ToolResult = guarded {
        val trip = ctx.tripManager.current ?: return@guarded ToolResult.error("No active trip.")
        val preference = RoutePreference(
            avoidTolls = args.boolean("avoid_tolls") ?: trip.routePreference.avoidTolls,
            avoidHighways = args.boolean("avoid_highways") ?: trip.routePreference.avoidHighways,
            avoidFerries = trip.routePreference.avoidFerries,
        )
        ctx.tripManager.setRoutePreference(preference)
        ctx.applyTrip()
        ToolResult.ok("Updated route preferences.")
    }
}

class RecalculateRouteTool(private val ctx: ToolContext) : NavigatorTool {
    override val name = "recalculate_route"
    override val description = "Recalculate the route to the current destination."
    override val parameters = emptyList<ToolParameter>()

    override fun execute(args: ToolArgs): ToolResult = guarded {
        if (!ctx.applyTrip()) return@guarded ToolResult.error("No active trip.")
        ToolResult.ok("Recalculating the route.")
    }
}

class StopNavigationTool(private val ctx: ToolContext) : NavigatorTool {
    override val name = "stop_navigation"
    override val description = "Stop the current navigation session."
    override val parameters = emptyList<ToolParameter>()

    override fun execute(args: ToolArgs): ToolResult {
        if (ctx.tripManager.current != null) ctx.tripManager.cancel()
        ctx.controller.clearNavigation()
        return ToolResult.ok("Navigation stopped.")
    }
}

class RepeatInstructionTool(private val ctx: ToolContext) : NavigatorTool {
    override val name = "repeat_instruction"
    override val description = "Repeat the current navigation instruction."
    override val parameters = emptyList<ToolParameter>()

    override fun execute(args: ToolArgs): ToolResult =
        ToolResult.ok(ctx.formatter.nextInstruction(ctx.store.current))
}

class GetNextManeuverTool(private val ctx: ToolContext) : NavigatorTool {
    override val name = "get_next_maneuver"
    override val description = "Describe the next maneuver."
    override val parameters = emptyList<ToolParameter>()

    override fun execute(args: ToolArgs): ToolResult {
        val state = ctx.store.current
        return ToolResult.ok(
            ctx.formatter.nextInstruction(state),
            data = mapOf(
                "maneuver" to state.nextManeuver?.name,
                "distance_m" to state.nextManeuverDistanceMeters,
                "road" to state.nextRoad,
            ),
        )
    }
}

class GetNavigationStateTool(private val ctx: ToolContext) : NavigatorTool {
    override val name = "get_navigation_state"
    override val description = "Return the current navigation state."
    override val parameters = emptyList<ToolParameter>()

    override fun execute(args: ToolArgs): ToolResult {
        val s = ctx.store.current
        return ToolResult.ok(
            "On ${s.currentRoad ?: "route"} heading to ${s.destinationName ?: "destination"}.",
            data = mapOf(
                "current_road" to s.currentRoad,
                "destination" to s.destinationName,
                "next_maneuver" to s.nextManeuver?.name,
                "next_maneuver_distance_m" to s.nextManeuverDistanceMeters,
                "eta_seconds" to s.etaSeconds,
                "remaining_distance_m" to s.remainingDistanceMeters,
                "route_status" to s.routeStatus.name,
            ),
        )
    }
}

/** Builds the standard Navigator tool registry. */
object NavigatorToolset {
    fun standard(ctx: ToolContext): ToolRegistry = ToolRegistry(
        listOf(
            SetDestinationTool(ctx),
            AddStopTool(ctx),
            RemoveStopTool(ctx),
            ReorderStopTool(ctx),
            ClearStopsTool(ctx),
            SetRoutePreferenceTool(ctx),
            RecalculateRouteTool(ctx),
            StopNavigationTool(ctx),
            RepeatInstructionTool(ctx),
            GetNextManeuverTool(ctx),
            GetNavigationStateTool(ctx),
        ),
    )
}
