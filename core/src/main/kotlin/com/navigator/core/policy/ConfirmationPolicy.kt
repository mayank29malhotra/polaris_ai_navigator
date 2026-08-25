package com.navigator.core.policy

/** Flags destructive or bulk tools that should be confirmed before executing (BRD Rule 4). */
object ConfirmationPolicy {

    private val RISKY = setOf("clear_stops")

    fun requiresConfirmation(toolName: String): Boolean = toolName in RISKY
}
