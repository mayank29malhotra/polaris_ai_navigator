package com.navigator.core.nav

import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationStateStoreTest {

    @Test
    fun starts_empty() {
        assertEquals(NavigationState.EMPTY, NavigationStateStore().current)
    }

    @Test
    fun update_sets_current_and_notifies_listener() {
        val store = NavigationStateStore()
        var notified: NavigationState? = null
        store.setListener { notified = it }

        val next = NavigationState(destinationName = "Office", routeStatus = RouteStatus.ON_ROUTE)
        store.update(next)

        assertEquals(next, store.current)
        assertEquals(next, notified)
    }
}
