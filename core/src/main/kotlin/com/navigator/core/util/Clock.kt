package com.navigator.core.util

/** Abstraction over the wall clock so time-dependent logic stays deterministic in tests. */
fun interface Clock {
    fun nowMillis(): Long

    companion object {
        val SYSTEM = Clock { System.currentTimeMillis() }
    }
}
