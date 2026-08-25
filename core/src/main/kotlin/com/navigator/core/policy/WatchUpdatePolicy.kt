package com.navigator.core.policy

import com.navigator.core.util.Clock
import com.navigator.core.watch.WatchPriority
import com.navigator.core.watch.WatchUpdate

/**
 * Throttles watch updates to avoid spam: identical updates and rapid non-critical updates are
 * suppressed, while critical changes always pass. Stateful; call [shouldPost] once per update.
 */
class WatchUpdatePolicy(
    private val clock: Clock = Clock.SYSTEM,
    private val minIntervalMs: Long = 3_000,
) {
    private var last: WatchUpdate? = null
    private var lastAt: Long = 0

    fun shouldPost(update: WatchUpdate): Boolean {
        val now = clock.nowMillis()
        val previous = last
        val allow = when {
            previous == null -> true
            update == previous -> false
            update.priority == WatchPriority.CRITICAL -> true
            now - lastAt < minIntervalMs -> false
            else -> true
        }
        if (allow) {
            last = update
            lastAt = now
        }
        return allow
    }

    fun reset() {
        last = null
        lastAt = 0
    }
}
