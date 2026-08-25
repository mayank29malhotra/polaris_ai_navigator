package com.navigator.core.geo

/** A WGS84 geographic coordinate. Validated at construction (a system boundary). */
data class LatLng(val latitude: Double, val longitude: Double) {
    init {
        require(latitude in -90.0..90.0) { "latitude out of range [-90, 90]: $latitude" }
        require(longitude in -180.0..180.0) { "longitude out of range [-180, 180]: $longitude" }
    }
}
