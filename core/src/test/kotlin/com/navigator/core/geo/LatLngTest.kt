package com.navigator.core.geo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class LatLngTest {

    @Test
    fun accepts_valid_coordinates() {
        val p = LatLng(12.9716, 77.5946)
        assertEquals(12.9716, p.latitude, 0.0)
        assertEquals(77.5946, p.longitude, 0.0)
    }

    @Test
    fun rejects_latitude_out_of_range() {
        assertThrows(IllegalArgumentException::class.java) { LatLng(91.0, 0.0) }
    }

    @Test
    fun rejects_longitude_out_of_range() {
        assertThrows(IllegalArgumentException::class.java) { LatLng(0.0, 181.0) }
    }
}
