package com.navigator.app.nav

import android.app.Application
import com.google.android.libraries.navigation.ListenableResultFuture
import com.google.android.libraries.navigation.NavigationApi
import com.google.android.libraries.navigation.Navigator
import com.google.android.libraries.navigation.SimulationOptions
import com.google.android.libraries.navigation.TimeAndDistance
import com.google.android.libraries.navigation.Waypoint
import com.navigator.core.nav.NavigationController
import com.navigator.core.nav.NavigationState
import com.navigator.core.nav.NavigationStateStore
import com.navigator.core.nav.RouteStatus
import com.navigator.core.trip.Stop

/**
 * Google Navigation SDK implementation of [NavigationController]. Owns the navigation session
 * and pushes updates into [store] as the authoritative [NavigationState] producer (D-003),
 * superseding the Maps notification bridge once active.
 *
 * NOTE: the SDK symbols below follow the documented Navigation SDK API but are unverified on
 * this machine (no SDK present). Validate the class/method names against the pinned SDK
 * version on first build. Turn-by-turn maneuver extraction (nextManeuver / nextRoad) is a
 * documented follow-up — it needs the SDK's NavInfo/StepInfo stream wired here.
 */
class GoogleNavigationManager(
    private val application: Application,
    private val store: NavigationStateStore,
) : NavigationController {

    private var navigator: Navigator? = null
    private var pending: Pair<Stop, List<Stop>>? = null

    /** Acquire the SDK Navigator (async, includes terms acceptance on first use). */
    fun initialize(onReady: (Boolean) -> Unit = {}) {
        NavigationApi.getNavigator(application, object : NavigationApi.NavigatorListener {
            override fun onNavigatorReady(nav: Navigator) {
                navigator = nav
                nav.setAudioGuidance(Navigator.AudioGuidance.VOICE_ALERTS_AND_GUIDANCE)
                registerListeners(nav)
                pending?.let { (destination, stops) ->
                    pending = null
                    setDestinations(destination, stops)
                }
                onReady(true)
            }

            override fun onError(errorCode: Int) {
                store.update(store.current.copy(routeStatus = RouteStatus.NO_ROUTE))
                onReady(false)
            }
        })
    }

    override fun setDestinations(destination: Stop, stops: List<Stop>) {
        val nav = navigator ?: run {
            pending = destination to stops
            return
        }
        store.update(store.current.copy(routeStatus = RouteStatus.ROUTING, destinationName = destination.name))
        val waypoints = (stops + destination).map { it.toWaypoint() }
        val route: ListenableResultFuture<Navigator.RouteStatus> = nav.setDestinations(waypoints)
        route.setOnResultListener { code -> onRouteResult(code) }
    }

    override fun startGuidance() {
        navigator?.startGuidance()
    }

    override fun stopGuidance() {
        navigator?.stopGuidance()
    }

    override fun clearNavigation() {
        navigator?.clearDestinations()
        store.update(NavigationState.EMPTY)
    }

    override fun startSimulation() {
        navigator?.simulator?.simulateLocationsAlongExistingRoute(
            SimulationOptions().speedMultiplier(SIMULATION_SPEED),
        )
    }

    override fun stopSimulation() {
        navigator?.simulator?.unsetUserLocation()
    }

    private fun registerListeners(nav: Navigator) {
        nav.addArrivalListener { _ ->
            store.update(store.current.copy(routeStatus = RouteStatus.ARRIVED))
        }
        nav.addRemainingTimeOrDistanceChangedListener(
            REMAINING_TIME_THRESHOLD_SECONDS,
            REMAINING_DISTANCE_THRESHOLD_METERS,
        ) {
            val timeAndDistance: TimeAndDistance = nav.currentTimeAndDistance
            store.update(
                store.current.copy(
                    etaSeconds = timeAndDistance.seconds,
                    remainingDistanceMeters = timeAndDistance.meters,
                    routeStatus = RouteStatus.ON_ROUTE,
                ),
            )
        }
    }

    private fun onRouteResult(code: Navigator.RouteStatus) {
        val status = when (code) {
            Navigator.RouteStatus.OK -> RouteStatus.ON_ROUTE
            Navigator.RouteStatus.NO_ROUTE_FOUND -> RouteStatus.NO_ROUTE
            else -> RouteStatus.NO_ROUTE
        }
        store.update(store.current.copy(routeStatus = status))
    }

    private fun Stop.toWaypoint(): Waypoint =
        Waypoint.builder()
            .setLatLng(location.latitude, location.longitude)
            .setTitle(name)
            .build()

    companion object {
        private const val REMAINING_TIME_THRESHOLD_SECONDS = 60
        private const val REMAINING_DISTANCE_THRESHOLD_METERS = 100
        private const val SIMULATION_SPEED = 5f
    }
}
