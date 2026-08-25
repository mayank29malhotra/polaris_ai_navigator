package com.navigator.core.policy

import com.navigator.core.util.Clock
import com.navigator.core.watch.WatchPriority
import com.navigator.core.watch.WatchUpdate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchUpdatePolicyTest {

    private class FakeClock(var t: Long = 0) : Clock {
        override fun nowMillis(): Long = t
    }

    @Test
    fun first_update_posts() {
        val policy = WatchUpdatePolicy(FakeClock(), minIntervalMs = 3_000)
        assertTrue(policy.shouldPost(WatchUpdate("LEFT", "200 m")))
    }

    @Test
    fun identical_update_is_suppressed() {
        val clock = FakeClock()
        val policy = WatchUpdatePolicy(clock, minIntervalMs = 3_000)
        val update = WatchUpdate("LEFT", "200 m")

        assertTrue(policy.shouldPost(update))
        clock.t = 5_000
        assertFalse(policy.shouldPost(update))
    }

    @Test
    fun non_critical_update_too_soon_is_suppressed() {
        val clock = FakeClock()
        val policy = WatchUpdatePolicy(clock, minIntervalMs = 3_000)

        assertTrue(policy.shouldPost(WatchUpdate("LEFT", "200 m")))
        clock.t = 1_000
        assertFalse(policy.shouldPost(WatchUpdate("LEFT", "150 m")))
    }

    @Test
    fun critical_update_posts_even_when_soon() {
        val clock = FakeClock()
        val policy = WatchUpdatePolicy(clock, minIntervalMs = 3_000)

        assertTrue(policy.shouldPost(WatchUpdate("LEFT", "200 m")))
        clock.t = 500
        assertTrue(policy.shouldPost(WatchUpdate("LEFT NOW", priority = WatchPriority.CRITICAL)))
    }

    @Test
    fun different_update_after_interval_posts() {
        val clock = FakeClock()
        val policy = WatchUpdatePolicy(clock, minIntervalMs = 3_000)

        assertTrue(policy.shouldPost(WatchUpdate("LEFT", "200 m")))
        clock.t = 4_000
        assertTrue(policy.shouldPost(WatchUpdate("RIGHT", "100 m")))
    }
}
