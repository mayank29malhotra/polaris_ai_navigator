package com.navigator.core.watch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WatchUpdateTest {

    @Test
    fun defaults_are_medium_priority_and_no_secondary_line() {
        val update = WatchUpdate(primary = "LEFT")

        assertEquals(WatchPriority.MEDIUM, update.priority)
        assertNull(update.secondary)
    }
}
