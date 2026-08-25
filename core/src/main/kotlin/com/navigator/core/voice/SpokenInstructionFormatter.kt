package com.navigator.core.voice

import com.navigator.core.nav.ManeuverType
import com.navigator.core.nav.NavigationState
import com.navigator.core.nav.RouteStatus

/**
 * Turns navigation state and voice commands into short spoken sentences for TTS.
 *
 * Pure logic (no Android), so it is unit-testable. Responses are intentionally one line to
 * keep spoken guidance concise while riding.
 */
class SpokenInstructionFormatter {

    fun respondTo(command: VoiceCommand, state: NavigationState): String = when (command) {
        VoiceCommand.REPEAT, VoiceCommand.WHATS_NEXT -> nextInstruction(state)
        VoiceCommand.HOW_FAR -> howFar(state)
        VoiceCommand.HOW_LONG -> howLong(state)
        VoiceCommand.WHERE_TO -> destination(state)
        VoiceCommand.WHICH_WAY -> whichWay(state)
        VoiceCommand.IS_THIS_CORRECT -> isThisCorrect(state)
        VoiceCommand.SHOULD_I_TAKE -> shouldITake("", state)
        VoiceCommand.AVOID_TOLLS -> "Avoiding tolls where possible."
        VoiceCommand.AVOID_HIGHWAYS -> "Avoiding highways where possible."
        VoiceCommand.RECALCULATE -> "Recalculating the route."
        VoiceCommand.STOP_NAVIGATION -> "Navigation stopped."
        VoiceCommand.UNKNOWN -> "Sorry, I didn't catch that."
    }

    fun nextInstruction(state: NavigationState): String {
        if (state.routeStatus == RouteStatus.ARRIVED) return "You have arrived."
        val maneuver = state.nextManeuver ?: return "Continue on your route."
        val phrase = spokenManeuver(maneuver)
        val distance = state.nextManeuverDistanceMeters
        return if (distance != null) "$phrase in ${spokenDistance(distance)}." else "$phrase."
    }

    fun howFar(state: NavigationState): String {
        val meters = state.remainingDistanceMeters ?: state.nextManeuverDistanceMeters
        return if (meters != null) "About ${spokenDistance(meters)} to go." else "I don't have the distance yet."
    }

    fun howLong(state: NavigationState): String {
        val seconds = state.etaSeconds ?: return "I don't have the time yet."
        val minutes = (seconds + 30) / 60
        return when {
            minutes <= 0 -> "Less than a minute."
            minutes == 1 -> "About 1 minute."
            else -> "About $minutes minutes."
        }
    }

    fun destination(state: NavigationState): String {
        val name = state.destinationName ?: return "No destination set."
        return "Heading to $name."
    }

    fun spokenManeuver(maneuver: ManeuverType): String = when (maneuver) {
        ManeuverType.TURN_LEFT -> "Turn left"
        ManeuverType.TURN_RIGHT -> "Turn right"
        ManeuverType.SLIGHT_LEFT -> "Slight left"
        ManeuverType.SLIGHT_RIGHT -> "Slight right"
        ManeuverType.SHARP_LEFT -> "Sharp left"
        ManeuverType.SHARP_RIGHT -> "Sharp right"
        ManeuverType.KEEP_LEFT -> "Keep left"
        ManeuverType.KEEP_RIGHT -> "Keep right"
        ManeuverType.FORK_LEFT -> "Take the left fork"
        ManeuverType.FORK_RIGHT -> "Take the right fork"
        ManeuverType.CONTINUE_STRAIGHT -> "Continue straight"
        ManeuverType.DEPART -> "Start"
        ManeuverType.UTURN -> "Make a U-turn"
        ManeuverType.MERGE -> "Merge"
        ManeuverType.ROUNDABOUT -> "Enter the roundabout"
        ManeuverType.RAMP -> "Take the ramp"
        ManeuverType.TAKE_FLYOVER -> "Take the flyover"
        ManeuverType.TAKE_UNDERPASS -> "Take the underpass"
        ManeuverType.ARRIVE -> "You have arrived"
        ManeuverType.UNKNOWN -> "Continue"
    }

    fun spokenDistance(meters: Int): String {
        require(meters >= 0) { "distance must be >= 0: $meters" }
        if (meters < 1000) {
            val rounded = ((meters + 5) / 10) * 10
            return "$rounded metres"
        }
        val tenthsKm = (meters + 50) / 100
        val whole = tenthsKm / 10
        val frac = tenthsKm % 10
        val number = if (frac == 0) "$whole" else "$whole.$frac"
        return if (number == "1") "1 kilometre" else "$number kilometres"
    }

    fun whichWay(state: NavigationState): String = nextInstruction(state)

    fun isThisCorrect(state: NavigationState): String = when (state.routeStatus) {
        RouteStatus.ON_ROUTE, RouteStatus.ROUTING -> "Yes, you're on the right route."
        RouteStatus.OFF_ROUTE -> "No, you're off route. Recalculating."
        RouteStatus.ARRIVED -> "You have arrived."
        else -> "I'm not certain right now."
    }

    fun shouldITake(question: String, state: NavigationState): String {
        val q = question.lowercase()
        val target = when {
            q.contains("flyover") || q.contains("overpass") -> ManeuverType.TAKE_FLYOVER
            q.contains("underpass") -> ManeuverType.TAKE_UNDERPASS
            q.contains("ramp") || q.contains("exit") -> ManeuverType.RAMP
            q.contains("left") -> ManeuverType.TURN_LEFT
            q.contains("right") -> ManeuverType.TURN_RIGHT
            else -> null
        }
        val next = state.nextManeuver ?: return "I don't have the next turn yet."
        return when {
            target == null -> nextInstruction(state)
            maneuverMatches(target, next) ->
                "Yes, " + spokenManeuver(next).replaceFirstChar { it.lowercase() } + distanceSuffix(state) + "."
            state.upcomingManeuvers.any { maneuverMatches(target, it.type) } ->
                "Yes, it's coming up on your route."
            else -> "No, stay on your current road for now."
        }
    }

    private fun distanceSuffix(state: NavigationState): String =
        state.nextManeuverDistanceMeters?.let { " in ${spokenDistance(it)}" } ?: ""

    private fun maneuverMatches(target: ManeuverType, actual: ManeuverType): Boolean = when (target) {
        ManeuverType.TURN_LEFT -> actual in LEFT_TURNS
        ManeuverType.TURN_RIGHT -> actual in RIGHT_TURNS
        else -> actual == target
    }

    companion object {
        private val LEFT_TURNS = setOf(
            ManeuverType.TURN_LEFT, ManeuverType.SLIGHT_LEFT, ManeuverType.SHARP_LEFT,
            ManeuverType.KEEP_LEFT, ManeuverType.FORK_LEFT,
        )
        private val RIGHT_TURNS = setOf(
            ManeuverType.TURN_RIGHT, ManeuverType.SLIGHT_RIGHT, ManeuverType.SHARP_RIGHT,
            ManeuverType.KEEP_RIGHT, ManeuverType.FORK_RIGHT,
        )
    }
}
