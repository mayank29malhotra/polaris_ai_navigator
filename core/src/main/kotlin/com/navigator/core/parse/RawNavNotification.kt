package com.navigator.core.parse

/** Plain-text fields lifted from an Android notification, with no Android dependency. */
data class RawNavNotification(
    val title: String? = null,
    val text: String? = null,
    val subText: String? = null,
    val bigText: String? = null,
    val ticker: String? = null,
)
