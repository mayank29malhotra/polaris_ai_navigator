# Navigator — Current State

> Full snapshot of the system as it exists **right now**. Regenerate this file fully at the
> end of major features so the whole system is understandable without reading the log.

**Snapshot date:** 2026-08-25 15:12
**Phase:** Phase 9 complete (optimization + safety/UX). Do-now authoring (Phases 0–9) DONE;
Phase 10 = build machine.
**Overall status:** All do-now `:core` logic + `:app` code is authored and unit-tested (JVM).
Nothing is compiled/built here (no toolchain/SDK). Remaining work is entirely on the
Android-Studio machine: Gradle build, API keys, SDK validation, on-device MVP-0…MVP-5.

---

## Strategy in effect

- **Reuse-first:** take proven code from reference repos (GMapsParser, MiBandNavigator,
  pebble-map-android) + Google Navigation SDK, strip what we don't need, add Navigator
  logic. See the reuse map in [Execution_Plan.md](../Execution_Plan.md).
- **Defer Android Studio (D-009):** author all files/code now in VS Code; build the APK and
  run on-device tests later on a machine with Android Studio + Android SDK + phone.
- **Module split (D-010):** `:core` = pure Kotlin/JVM logic (unit-testable with only a
  JDK); `:app` = Android/SDK code that needs the build machine.
- **Multi-watch output (D-011):** `WatchOutput` interface with a `NotificationWatchOutput`
  default (widest compatibility) and an optional direct-BLE adapter later; not Noise-locked.

## What exists now

- `Navigator_brd.md` — complete Business Requirements Document (product, goals, functional
  requirements FR-01…FR-15, agent tools/rules, safety, data model, MVP-0…MVP-5, acceptance
  criteria, risks).
- `Plan_navigator.md` — complete phased Implementation Plan (Phase 0–14, build order, repo
  structure, GitHub references, definition of done, roadmap).
- `Execution_Plan.md` — reuse-first, build-later plan: do-now vs build-machine tracks,
  reuse map, `:core`/`:app` split, per-phase Android-Studio-dependency flags.
- `project_memory/` — persistent memory system (this snapshot + log, architecture,
  decisions, task tracker, file index).
- **Android project scaffold (Phase 0):** two-module Gradle build (`:app` Android + `:core`
  pure Kotlin/JVM) using Kotlin DSL + a version catalog (`gradle/libs.versions.toml`), the
  Gradle 8.9 wrapper (jar + scripts), `.gitignore`, `AndroidManifest.xml`, a minimal
  `MainActivity` + resources (adaptive launcher icon, Material3 theme, layout), and the seed
  `WatchOutput`/`WatchUpdate` API in `:core` with a JVM unit test.
- **`:core` domain model (Phase 1):** pure-Kotlin trip/navigation engine — `LatLng`, `Stop`
  (+`StopType`), `RoutePreference`, `Trip` (+`TripStatus`), `TripManager` (add/insert/remove/
  reorder/clear stops, change destination, preferences, status; enforces the 25-waypoint
  limit; deterministic via an injected `Clock`), and the navigation read-model
  `NavigationState`/`Maneuver`/`ManeuverType`/`RouteStatus`. Covered by JVM unit tests
  (`LatLngTest`, `TripTest`, `TripManagerTest`, `NavigationStateTest`).
- **Notification layer (Phase 2):** pure-Kotlin `NotificationFormatter` in `:core`
  (`NavigationState` → compact `WatchUpdate` with glyph + label + rounded distance, tiered
  priority, arrival/reroute/route-change cases) with `NotificationFormatterTest`; plus the
  `:app` `NotificationWatchOutput` (implements `WatchOutput`, posts standard Android
  notifications on a nav channel) and a working **TEST WATCH** button in `MainActivity`
  (requests `POST_NOTIFICATIONS` on API 33+) — the MVP-0 proof path.
- **Google Maps bridge (Phase 3):** pure-Kotlin `GoogleMapsNotificationParser` in `:core`
  (`RawNavNotification` text → `NavigationState`: distance m/km/mi/ft, maneuver keywords,
  road, arrival) with `GoogleMapsNotificationParserTest`; plus the `:app`
  `GoogleMapsBridgeService` (`NotificationListenerService` for Google Maps) and an ENABLE
  MAPS BRIDGE button. Completes the passive Maps → watch pipeline (MVP-1).
- **Voice layer (Phase 4):** pure-Kotlin `CommandClassifier` (transcript → `VoiceCommand`)
  and `SpokenInstructionFormatter` (state/command → concise spoken line) in `:core`, both
  unit-tested; plus `:app` `TtsManager`, `SpeechInput`, and `VoiceSession` (speech → classify
  → speak) with a VOICE COMMAND button (requests `RECORD_AUDIO`).
- **Controlled navigation (Phase 5):** `:core` `NavigationController` (SDK-agnostic control
  surface) + `NavigationStateStore` (shared producer→consumer state, `NavigationStateStoreTest`);
  `:app` `GoogleNavigationManager` wrapping the Navigation SDK (session, destinations,
  guidance, simulator, ETA/arrival/route-status → store). Gradle pulls the Navigation SDK with
  a `MAPS_API_KEY` placeholder; manifest has location perms + Maps key. A START NAV (SDK)
  button drives it. SDK symbols/version are unverified here (build-machine validation).
- **Navigation tools (Phase 6):** pure-Kotlin tool framework (`NavigatorTool`, `ToolArgs`,
  `ToolResult`, `ToolRegistry`) + 10 tools (set_destination, add_stop, remove_stop,
  reorder_stop, clear_stops, set_route_preference, recalculate_route, repeat_instruction,
  get_next_maneuver, get_navigation_state) over `TripManager` + `NavigationController`, with
  `NavigationToolsTest`; `:app` SET DESTINATION / ADD STOP / CLEAR STOPS buttons dispatch
  through the registry (MVP-3, proving `tool → controller/SDK`).
- **Agent (Phase 7):** provider-agnostic `LlmClient` seam + `ToolSchemas` (strict JSON from
  tool params) + `NavigatorAgent` in `:core` (classifier fast-path for obvious commands, else
  LLM → tool call via the registry), with `NavigatorAgentTest`/`ToolSchemasTest` (mock LLM).
  Added the `stop_navigation` tool. `:app` `VoiceSession` is now agent-driven with a
  `PlaceholderLlmClient` (real provider + key = build machine).
- **Context + rider commands (Phase 8):** `:core` `AgentContextBuilder` (compact JSON incl.
  upcoming maneuvers) + Tier 3/4 commands — AVOID_TOLLS/AVOID_HIGHWAYS route to
  `set_route_preference`, and SHOULD_I_TAKE/WHICH_WAY/IS_THIS_CORRECT are answered purely from
  `NavigationState` (BRD FR-07). All unit-tested; `:app` unchanged (already agent-driven).
- **Safety/UX policies (Phase 9):** `:core` `WatchUpdatePolicy` (watch anti-spam),
  `SpeechPolicy` (one-sentence/short TTS), `ConfirmationPolicy` (risky tools), with tests. The
  agent now confirms destructive LLM tool calls, shortens spoken output, and answers
  unsupported requests gracefully (D-008); `:app` throttles watch posts via the policy.

Not yet compiled/built — that happens on the build machine (Track B).

## How modules connect

`:app` depends on `:core` (one-way). `:core` holds the trip engine, the `NavigationState`
store (D-005), the formatter, the Maps parser, the voice logic, the `NavigationController`
interface, the tool layer (`ToolRegistry`), and the **agent** (`NavigatorAgent` over an
`LlmClient` seam + `ToolSchemas` + `AgentContextBuilder`, with deterministic Tier 1/3/4
answers). In `:app`, `GoogleNavigationManager` produces state into the
store (→ watch + voice); `VoiceSession` sends transcripts to `NavigatorAgent`, which answers
obvious commands locally or asks the LLM for a tool call executed through the registry. The
concrete LLM provider (real key) is the only remaining `:app` piece.

## What is stable

- The requirements and the dependency-ordered build plan are stable and agreed.
- Key architectural decisions are recorded in [DECISIONS.md](DECISIONS.md) (D-001…D-011).
- The Phase 0 scaffold (Gradle config, module split, wrapper) is in place and intended to
  Gradle-sync unchanged on the build machine.
- The `:core` domain model (Phase 1) is implemented with unit tests; its API is the stable
  base the tool, agent, TTS, and watch layers build on.
- The notification formatting + watch-output layer (Phase 2) is implemented; the
  `NavigationState → WatchUpdate → notification` path is complete end to end in code.
- The Google Maps parser (Phase 3) is implemented with unit tests; the passive Maps → watch
  pipeline is complete in code.
- The voice layer (Phase 4) is implemented; `CommandClassifier` + `SpokenInstructionFormatter`
  are unit-tested, and the listen → classify → speak loop is wired in `:app`.
- The `:core` control layer (Phase 5) is implemented and tested (`NavigationController` +
  `NavigationStateStore`); the SDK wrapper (`GoogleNavigationManager`) is authored but
  unverified until the build machine.
- The tool layer (Phase 6) is implemented and unit-tested; the `ToolRegistry` is the stable
  call surface for both the UI and the upcoming agent.
- The agent (Phase 7) is implemented and unit-tested with a mock LLM; the `LlmClient` seam
  keeps the provider swappable and the tool-call path deterministic.
- The rider-command coverage (Phase 8) is implemented and unit-tested; Tiers 1/3/4 are
  answered deterministically from state, and the LLM receives a compact structured context.
- The safety/UX policies (Phase 9) are implemented and unit-tested (watch anti-spam, short TTS,
  confirm-before-destructive); the do-now authoring track (Phases 0–9) is complete.

## What is experimental / unproven

- Noise smartwatch notification mirroring behavior (model-specific — must be tested first).
- Google Navigation SDK setup, pricing/licensing, and motorcycle-routing suitability.
- Voice recognition accuracy in helmet/wind/traffic noise.
- Whether the notification-parsing bridge (GMapsParser approach) is reliable enough for the
  MVP-1 prototype.

## Known limitations / open questions

- Exact Noise watch model and companion-app capabilities not yet confirmed.
- Google Maps Platform billing/account not yet verified.
- Agent/LLM provider not yet chosen (Orion concepts vs LangGraph vs direct LLM API).
- Build is unverified here (no Android SDK on this machine); the first real Gradle
  sync/build happens on the build machine. Chosen versions: AGP 8.5.2, Gradle 8.9,
  Kotlin 2.0.20, compile/target SDK 34, minSdk 26.
- Navigation SDK (Phase 5) is authored but unverified: confirm the artifact version
  (`navigation = 6.0.0`), validate SDK class/method names, add `MAPS_API_KEY` + billing,
  accept the SDK Terms, and wire turn-by-turn maneuvers on the build machine.
- Foreground-service lifecycle, command timeout, battery, and real-world tuning are deferred
  to the build machine (Phase 10).
- Exact arbitrary-road enforcement is explicitly **not** a v1 promise (see D-008).

## Immediate next step

**Phase 10 — Build & device bring-up (build machine only).** Copy the repo to an Android-Studio
machine, then: Gradle-sync, run `:core` unit tests (`gradlew :core:test`), add `MAPS_API_KEY` +
an LLM key, validate/​bump the Navigation SDK, build the APK, and run the deferred on-device
tests MVP-0…MVP-5 (watch, Maps bridge, Navigation SDK, tools, agent). See
[Execution_Plan.md](../Execution_Plan.md) §6 (handoff checklist) and [TASK_TRACKER.md](TASK_TRACKER.md).

Optional do-now polish before handoff: `git init` + first commit; a real `LlmClient` provider
implementation (still needs a key to run).
