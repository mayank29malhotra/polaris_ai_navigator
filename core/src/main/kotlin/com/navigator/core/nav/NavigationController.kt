package com.navigator.core.nav

import com.navigator.core.trip.Stop

/**
 * SDK-agnostic control surface for an active navigation session. Implemented in `:app` by the
 * Google Navigation SDK wrapper and called by the UI, tools, and (later) the agent, so those
 * layers never touch the SDK directly.
 */
interface NavigationController {

    /** Set the ordered route (intermediate [stops] then final [destination]) and calculate it. */
    fun setDestinations(destination: Stop, stops: List<Stop> = emptyList())

    fun startGuidance()
    fun stopGuidance()
    fun clearNavigation()

    /** Simulate travel along the current route, for testing without riding. */
    fun startSimulation()
    fun stopSimulation()
}
