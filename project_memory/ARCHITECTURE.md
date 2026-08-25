# Navigator — Architecture

> System design and its evolution. Update whenever a structural decision is made or a
> module is added/changed. Keep the "Evolution log" at the bottom append-only.

## 1. Product in one line

Android-first, hands-free motorcycle navigation assistant. The phone is the GPS/compute
device; a Bluetooth headset gives concise spoken guidance; a Noise smartwatch shows
glanceable/haptic reinforcement via Android notification mirroring; a voice agent
translates natural language into deterministic navigation tools.

## 2. Guiding principle

**Do not build an AI that navigates. Build a navigation system that an AI can control.**

- The routing/navigation engine is authoritative for location, current road, next
  maneuver, and available routes.
- The agent only interprets the rider's request, selects a deterministic tool, and phrases
  the result. It must never fabricate route facts.

## 3. High-level component diagram

```text
        RIDER (voice / headset button)
                    │
                    ▼
        ┌───────────────────────────┐
        │   Android Navigator App   │
        │  Voice Input              │
        │  Agent / Intent Router    │
        │  Trip State               │
        │  Notification Formatter   │
        │  TTS                      │
        └──────┬──────────────┬─────┘
          tool calls          │
               │              │
               ▼              ▼
     ┌──────────────────┐  ┌──────────────┐
     │ Navigation SDK / │  │ Android TTS  │
     │ Navigator        │  │ + Bluetooth  │
     └────────┬─────────┘  └──────────────┘
              │ navigation/maneuver events
              ▼
     ┌──────────────────┐
     │ Instruction/     │
     │ Maneuver Engine  │
     └────────┬─────────┘
              ▼
     Android Notifications ──► Noise Watch
```

## 4. Central data flow

```text
Navigation SDK  ──►  NavigationState (single source of truth)
                          │
              ┌───────────┼───────────┐
              ▼           ▼           ▼
            Agent        TTS        Watch
```

Every subsystem reads from one central `NavigationState`/`TripState`. The agent does not
poke the SDK directly everywhere — it goes through deterministic tools that update state.

## 5. Planned modules / packages

From the Implementation Plan (initially packages inside one module to avoid premature
complexity):

```text
app/            MainActivity, NavigationService, permissions
navigation/     NavigationManager, NavigationState, RouteManager,
                ManeuverProcessor, NavigationSimulator
trip/           Trip, Stop, TripManager
agent/          NavigatorAgent, ToolRegistry, ToolSchemas, AgentContextBuilder
tools/          SetDestination, AddStop, RemoveStop, ReorderStop,
                GetNextManeuver, RepeatInstruction, RoutePreference
voice/          SpeechRecognizer, TtsManager, VoiceSession
notifications/  NavigationNotification, NotificationFormatter, WatchNotificationManager
data/           RoomDatabase, SavedPlaces
util/
```

## 6. Navigation engine strategy (two-stage)

1. **Prototype/bridge (MVP-1):** `NotificationListenerService` parses Google Maps
   turn-by-turn notifications (reference: GMapsParser) into a Navigator event model, then
   formats compact notifications for the watch + TTS. Quick path to useful data.
2. **Production (MVP-2+):** Google Maps Platform **Navigation SDK for Android** owns the
   navigation session (`Navigator`, `setDestination()`, `setDestinations()`,
   `startGuidance()`, listeners, route simulation). Navigator controls the trip rather than
   scraping another app's notifications.

## 7. Agent/tool contract (initial, deliberately small)

```text
resolve_location(query)          start_navigation(destination)
change_destination(destination)  add_stop(location)
remove_stop(identifier)          reorder_stop(from_index, to_index)
clear_stops()                    get_current_navigation_state()
get_next_maneuver()              get_upcoming_maneuvers()
repeat_instruction()             recalculate_route()
set_route_preference(preference) get_trip_state()
stop_navigation()
```

Later: `avoid_road`, `prefer_road`, `choose_route`, `save_place`.

## 8. Core data model (planned)

- `Trip { id, status, currentDestination, stops[], routePreference, startedAt, updatedAt }`
- `Stop { id, name, latitude, longitude, placeId, order, type }`
- `NavigationState { currentLocation, currentRoad, nextManeuver, nextManeuverDistance,
  nextRoad, upcomingManeuvers[], destination, eta, remainingDistance, routeStatus }`

## 9. Output channels

- **Bluetooth headset (primary):** short TTS messages only, never verbose.
- **Watch (secondary), via `WatchOutput` interface (D-011):** compact output (e.g.
  `↰ LEFT / 200 m`), tiered by importance (critical / medium / low). Default
  `NotificationWatchOutput` posts a standard Android notification — works on Noise and most
  watches that mirror phone notifications. Optional `BleWatchOutput` later for bands needing
  richer output. Exact behavior must be tested against the specific Noise model + companion
  app. Avoid notification spam.

## 10. Technical stack (target)

Kotlin, Android Studio, Android SDK, Google Navigation SDK for Android, Google Maps
Platform credentials/billing, Android Location APIs, foreground service, notification APIs,
Android TTS, speech recognition, Bluetooth audio routing, Room/SQLite, Git/GitHub. Agent
layer: reuse existing Orion agent/tool concepts, or LangGraph / an LLM provider with strict
JSON tool schemas.

## 11. Non-goals for v1

New maps database; new routing engine; continuous conversational AI while riding; full
phone navigation UI replacement; direct BLE watch integration (unless notification
mirroring proves insufficient); exact arbitrary-road enforcement.

---

## Evolution log (append-only)

- **2026-08-25** — Architecture captured from `Navigator_brd.md` + `Plan_navigator.md`. No
  code exists yet; this reflects the intended/target design, not an implemented system.
- **2026-08-25** — Added `WatchOutput` abstraction (D-011): notification mirroring is the
  multi-watch default; optional direct-BLE adapter later. Reinforces the reuse-map removals.
- **2026-08-25** — Phase 0 scaffolding created: two-module Gradle project (`:app` + `:core`)
  with a version catalog + Gradle 8.9 wrapper; `:core` seeded with the `WatchOutput` API.
  Realizes D-010/D-011. Not yet built (deferred to the build machine per D-009).
- **2026-08-25** — Phase 1 implemented the `:core` domain: `LatLng`, `Stop`,
  `RoutePreference`, `Trip`/`TripManager` (deterministic, clock-injected, 25-waypoint cap),
  and the `NavigationState` read-model (`Maneuver`/`ManeuverType`/`RouteStatus`), with JVM
  unit tests. Realizes the central-state principle (D-005).
- **2026-08-25** — Phase 2 added the notification layer: `:core` `NotificationFormatter`
  (`NavigationState` → compact `WatchUpdate`, tiered) + tests, and `:app`
  `NotificationWatchOutput` (default `WatchOutput` via Android notifications) with a TEST
  WATCH button proving the state→formatter→watch path (MVP-0).
- **2026-08-25** — Phase 3 added the passive Google Maps bridge: `:core`
  `GoogleMapsNotificationParser` (notification text → `NavigationState`) + tests, and `:app`
  `GoogleMapsBridgeService` (`NotificationListenerService`) mirroring parsed guidance to the
  watch via the Phase 2 formatter/output (MVP-1).
- **2026-08-25** — Phase 4 added the voice layer: `:core` `CommandClassifier` +
  `SpokenInstructionFormatter` (+tests) and `:app` `TtsManager`/`SpeechInput`/`VoiceSession`
  (speech → classify → spoken response), demoed via a VOICE COMMAND button.
- **2026-08-25** — Phase 5 added controlled navigation: `:core` `NavigationController` +
  `NavigationStateStore` (tested) and `:app` `GoogleNavigationManager` wrapping the Google
  Navigation SDK (session, destinations, guidance, simulator, ETA/arrival/route-status →
  store). The store now links producers to the watch/voice consumers. SDK symbols unverified
  until the build machine; turn-by-turn maneuver extraction is a documented follow-up.
- **2026-08-25** — Phase 6 added the deterministic tool layer: `:core` `ToolRegistry` + 10
  tools over `TripManager` + `NavigationController` (+tests), and `:app` buttons dispatching
  tools by name (MVP-3). The registry is the shared call surface for the UI and the agent.
- **2026-08-25** — Phase 7 added the agent: `:core` provider-agnostic `LlmClient` +
  `ToolSchemas` (strict JSON) + `NavigatorAgent` (classifier fast-path, else LLM → tool call
  via the registry) with mock-LLM tests; added the `stop_navigation` tool; `:app` voice is now
  agent-driven with a `PlaceholderLlmClient`. Real provider/key = build machine (MVP-4).
- **2026-08-25** — Phase 8 added navigation context + rider commands: `:core`
  `AgentContextBuilder` (compact JSON + upcoming) and Tier 3/4 commands
  (avoid-tolls/highways → tool; should-I-take / which-way / is-this-correct answered from
  state, engine-authoritative per BRD FR-07), all unit-tested.
- **2026-08-25** — Phase 9 added safety/UX policies: `:core` `WatchUpdatePolicy` (anti-spam),
  `SpeechPolicy` (short TTS), `ConfirmationPolicy`; the agent now confirms destructive tool
  calls, shortens speech, and handles unknown tools gracefully. Completes the do-now authoring
  track (Phases 0–9); Phase 10 is build + device bring-up.
