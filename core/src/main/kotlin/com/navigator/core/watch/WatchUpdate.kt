package com.navigator.core.watch

/** A single compact instruction to render on the watch. */
data class WatchUpdate(
    val primary: String,
    val secondary: String? = null,
    val priority: WatchPriority = WatchPriority.MEDIUM,
)

/** Relative importance used to decide whether and how to surface an update. */
enum class WatchPriority { CRITICAL, MEDIUM, LOW }
