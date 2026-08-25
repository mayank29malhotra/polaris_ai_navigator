# Navigator — Task Tracker

> Completed + pending tasks. Update whenever work starts, completes, or is re-scoped.
> Legend: [x] done · [~] in progress · [ ] pending · [!] blocked

**Last updated:** 2026-08-25 15:12

---

> **Sequencing note:** work is split into **[DO NOW]** (authoring on this machine, no
> Android Studio) and **[BUILD MACHINE]** (compile/run/on-device). See
> [Execution_Plan.md](../Execution_Plan.md). Phases 0–9 are mostly do-now; Phase 10 is the
> consolidated build + device bring-up.

> **Execution-Plan do-now progress:** Phases 0–9 ✅ (all do-now authoring complete) ·
> **Phase 10 (build & device bring-up) = build machine ⬅ next**.
> (Note: the "Pending — by phase" list below uses Plan_navigator.md's original numbering;
> the `:core` domain corresponds to its Phase 5 + Phase 7.)

## Completed

- [x] Author Business Requirements Document (`Navigator_brd.md`).
- [x] Author phased Implementation Plan (`Plan_navigator.md`).
- [x] Initialize persistent project memory (`/project_memory/`, six files).
- [x] Author reuse-first, build-later execution plan (`Execution_Plan.md`).
- [x] Phase 0 — project scaffolding: two-module Gradle build (`:app` + `:core`), version
  catalog, Gradle 8.9 wrapper, manifest, `MainActivity` + resources, `WatchOutput` seed +
  JVM unit test. (Compile/run deferred to the build machine.)
- [x] Phase 1 (Execution Plan) — `:core` domain model: `LatLng`, `Stop`, `RoutePreference`,
  `Trip`/`TripManager` (deterministic, clock-injected, 25-waypoint cap), and the
  `NavigationState` read-model, with JVM unit tests.
- [x] Phase 2 (Execution Plan) — notification layer: `:core` `NotificationFormatter`
  (+tests) and `:app` `NotificationWatchOutput` + TEST WATCH button (MVP-0 proof path;
  on-device verification deferred to the build machine).
- [x] Phase 3 (Execution Plan) — Google Maps parser bridge: `:core`
  `GoogleMapsNotificationParser` (+tests) and `:app` `GoogleMapsBridgeService`
  (NotificationListenerService) + ENABLE MAPS BRIDGE button. Completes passive Maps → watch
  (MVP-1 code; on-device tuning/binding on the build machine).
- [x] Phase 4 (Execution Plan) — voice layer: `:core` `CommandClassifier` +
  `SpokenInstructionFormatter` (+tests) and `:app` `TtsManager` / `SpeechInput` /
  `VoiceSession` + VOICE COMMAND button (on-device TTS/speech verification on the build machine).
- [x] Phase 5 (Execution Plan) — controlled navigation: `:core` `NavigationController` +
  `NavigationStateStore` (+test) and `:app` `GoogleNavigationManager` (Navigation SDK wrapper),
  Gradle SDK dep + `MAPS_API_KEY` placeholder, manifest perms/meta-data, START NAV button.
  (SDK symbols/version/key + turn-by-turn are build-machine work.)
- [x] Phase 6 (Execution Plan) — navigation tools: `:core` `ToolRegistry` + 10 tools over
  `TripManager` + `NavigationController` (+`NavigationToolsTest`), and `:app` SET DEST / ADD
  STOP / CLEAR STOPS buttons dispatching by name (MVP-3, `tool → controller/SDK`).
- [x] Phase 7 (Execution Plan) — agent: `:core` provider-agnostic `LlmClient` + `ToolSchemas`
  + `NavigatorAgent` (classifier fast-path, else LLM → tool call) + `stop_navigation` tool,
  mock-LLM tests; `:app` agent-driven `VoiceSession` + `PlaceholderLlmClient`. (Real key = build.)
- [x] Phase 8 (Execution Plan) — context + rider commands: `:core` `AgentContextBuilder` +
  Tier 3/4 commands (avoid tolls/highways → tool; should-I-take / which-way / is-this-correct
  answered from state), all unit-tested.
- [x] Phase 9 (Execution Plan) — safety/UX policies: `:core` `WatchUpdatePolicy` /
  `SpeechPolicy` / `ConfirmationPolicy` (+tests); agent confirms destructive tool calls,
  shortens speech, handles unknown tools gracefully; `:app` throttles watch posts.
  **Do-now authoring track (Phases 0–9) complete.**

## In progress

- (none)

## Pending — by phase (from Plan_navigator.md)

### Phase 0 — Project scaffolding  ✅ do-now parts done
- [x] Create Kotlin Android project structure (Gradle, `:app` + `:core` modules). [DO NOW]
- [x] Set minimum Android version (minSdk 26; compile/target SDK 34). [DO NOW]
- [x] Establish package/module structure + Gradle wrapper + `.gitignore`. [DO NOW]
- [ ] Set up Git repository (`git init` + first commit). [DO NOW — optional]
- [ ] Deliverable: blank app installs and runs on the phone. [BUILD MACHINE]

### MVP-0 / Phase 1 — Prove Noise notification delivery
- [x] Single "TEST WATCH" button posts a navigation-style Android notification. [DO NOW]
- [ ] Enable Navigator notifications on phone + in the Noise companion app. [BUILD MACHINE]
- [ ] Verify delivery, truncation, vibration, and title/body formatting on the Noise watch. [BUILD MACHINE]

### MVP-1 / Phase 2–3 — Passive Google Maps bridge + TTS
- [x] `NotificationListenerService` to parse Google Maps nav notifications (ref: GMapsParser). [DO NOW]
- [x] Navigator event model + `NotificationFormatter` for compact watch output. [DO NOW]
- [x] Android TTS through Bluetooth headset; implement current-instruction + repeat. [DO NOW code; device test later]

### MVP-2 / Phase 4 — Controlled navigation (Google Navigation SDK)
- [x] SDK setup, `NavigationApi`, `Navigator`, `Waypoint` (authored in `GoogleNavigationManager`). [DO NOW code]
- [x] `setDestinations()`, `startGuidance()`, arrival + time/distance listeners, route status → store. [DO NOW code]
- [~] Audio guidance + route simulation wired; rerouting / turn-by-turn / basic nav UI pending. [BUILD MACHINE]

### Phase 5 — Central navigation state
- [x] Implement `NavigationState` + `NavigationStateStore` as the single source of truth (D-005). [DO NOW]

### MVP-3 / Phase 6–7 — Navigation tools + trip/waypoint management (no AI)
- [x] Tools: set_destination, add_stop, remove_stop, reorder_stop, clear_stops,
      repeat_instruction, get_navigation_state, get_next_maneuver, recalculate_route,
      set_route_preference (`:core` `ToolRegistry`, unit-tested). [DO NOW]
- [x] Expose tools as UI buttons (SET DEST / ADD STOP / CLEAR STOPS); prove `tool → SDK`. [DO NOW code]

### MVP-4 / Phase 8–10 — Voice input + agent/tool calling + context
- [x] Speech recognition → transcript; basic command classifier (repeat / what's next / stop). [DO NOW code]
- [x] LLM tool-calling with strict JSON schemas (`NavigatorAgent` + `ToolSchemas`, mock-tested; real provider on build machine). [DO NOW code]
- [x] Compact structured navigation context builder for the agent (`AgentContextBuilder`). [DO NOW]

### MVP-5 / Phase 11 — Rider intelligence commands
- [x] Tier 1: what's next / repeat / how far / how long. [DO NOW]
- [~] Tier 2: change destination / add stop / remove stop / skip stop (tools + agent done; LLM + geocoding on build machine). [DO NOW code]
- [x] Tier 3: avoid tolls / avoid highways / recalculate. [DO NOW]
- [x] Tier 4: should I take the flyover / which way now / is this the correct turn. [DO NOW]
- [ ] Tier 5: specific-road preference (only when engine can enforce/verify — see D-008).

### Phase 12–14 — Optimization, safety/UX, testing
- [~] Noise notification tiering / anti-spam via `WatchUpdatePolicy` (code); device tuning pending. [DO NOW code]
- [ ] Battery optimization (foreground service only during nav; event-driven updates). [BUILD MACHINE]
- [~] Safety/UX: short TTS + confirmation for bulk/risky + graceful failures / explicit
      uncertainty (code); command timeout pending on device. [DO NOW code]
- [ ] Simulation-based testing (destination, stops, navigation, voice, agent cases). [BUILD MACHINE]

## Blocked / needs input

- [!] Confirm exact Noise watch model + companion-app notification behavior.
- [!] Confirm Google Maps Platform account/billing/licensing for the Navigation SDK.
- [!] Choose agent/LLM provider (Orion concepts vs LangGraph vs direct LLM API).

## Acceptance criteria for v1 (from BRD §19)

Destination without typing · nav continues without viewing phone · watch gets important
notifications · headset gives concise turns · "what's next" correct · "repeat" works ·
change destination by voice · add/remove stops by voice · recalculation after changes ·
off-route handling · no dependence on watch GPS · agent cannot fabricate routes ·
simulate/test without riding.
