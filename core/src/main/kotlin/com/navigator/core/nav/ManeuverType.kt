package com.navigator.core.nav

/** Normalized maneuver kinds, decoupled from any specific navigation SDK's vocabulary. */
enum class ManeuverType {
    DEPART,
    CONTINUE_STRAIGHT,
    TURN_LEFT,
    TURN_RIGHT,
    SLIGHT_LEFT,
    SLIGHT_RIGHT,
    SHARP_LEFT,
    SHARP_RIGHT,
    KEEP_LEFT,
    KEEP_RIGHT,
    UTURN,
    MERGE,
    FORK_LEFT,
    FORK_RIGHT,
    ROUNDABOUT,
    RAMP,
    TAKE_FLYOVER,
    TAKE_UNDERPASS,
    ARRIVE,
    UNKNOWN,
}
