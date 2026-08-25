package com.navigator.core.watch

/**
 * Transport-agnostic sink for compact navigation output shown on a wearable.
 *
 * The default implementation posts a standard Android notification (widest watch
 * compatibility); an optional direct-BLE implementation can be added later without
 * changing callers. See decision D-011.
 */
interface WatchOutput {
    /** Show or update the current wearable instruction. */
    fun show(update: WatchUpdate)

    /** Clear any active wearable instruction (e.g. on arrival or stop). */
    fun clear()
}
