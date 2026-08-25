package com.navigator.core.nav

import kotlin.jvm.Volatile

/**
 * Holds the latest [NavigationState] and notifies a single listener on change. This is the
 * shared hand-off point between state producers (Navigation SDK / Maps bridge) and consumers
 * (watch, TTS, agent) — the single source of truth from decision D-005.
 */
class NavigationStateStore {

    @Volatile
    private var state: NavigationState = NavigationState.EMPTY

    private var listener: ((NavigationState) -> Unit)? = null

    val current: NavigationState get() = state

    fun update(newState: NavigationState) {
        state = newState
        listener?.invoke(newState)
    }

    fun setListener(listener: ((NavigationState) -> Unit)?) {
        this.listener = listener
    }
}
