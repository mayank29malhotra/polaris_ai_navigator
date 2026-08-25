# Navigator — Execution Plan (Reuse-First, Build-Later)

> Companion to [Plan_navigator.md](Plan_navigator.md). Same product and phase intent, but
> re-sequenced around two real constraints:
> 1. **Reuse existing open-source code**, strip what we don't need, add Navigator logic.
> 2. **Android Studio may not be allowed on this machine** — author everything now, build
>    later on a machine that has Android Studio + the Android SDK + a phone.

---

## 0. The key insight that makes this work

An Android app is **just text files** — Kotlin (`.kt`), XML layouts/resources, and Gradle
build scripts. **Writing code is not the same as building it.**

| Activity | Needs Android Studio / SDK / phone? | When |
|---|---|---|
| Writing Kotlin, XML, Gradle, manifests | No — any text editor (VS Code) | **Now** |
| Designing architecture, modules, data models | No | **Now** |
| Pure-Kotlin logic + JVM unit tests (JDK only) | No Android Studio; only a JDK | **Now (optional)** |
| Adapting reference-repo source files | No | **Now** |
| Compiling the APK / Gradle sync of Android modules | Yes — Android SDK | Later |
| Running on device, notifications → watch, TTS, voice | Yes — phone | Later |
| Google Navigation SDK live routing + API key | Yes — SDK + billing | Later |

**Conclusion:** ~90% of the codebase (all source, resources, config, and the entire
deterministic logic layer) can be written and committed now. Only compiling and on-device
testing move to the build machine.

---

## 1. Architectural refinement to maximize "do-now" work

Split the code into two modules (see decision **D-010**):

```text
:core   Pure Kotlin / JVM library — NO Android dependencies
        TripState, Stop, NavigationState, ManeuverType,
        NotificationFormatter (string logic), WatchOutput interface,
        Agent tool contracts, tool router, command classifier, context builder.
        -> Compiles & unit-tests with only a JDK. Fully verifiable now.

:app    Android module — depends on :core
        Activities, foreground NavigationService, NotificationWatchOutput
        (posts standard Android notifications), TTS, SpeechRecognizer,
        Google Navigation SDK wiring, permissions.
        -> Needs the Android SDK to build. Deferred to build machine.
```

This puts the hardest, most valuable, most testable logic (the deterministic navigation
brain) into `:core`, which does **not** need Android Studio at all.

---

## 2. Two work tracks

### Track A — Do now (this machine, no Android Studio)
All file/code creation, the entire `:core` module, all `:app` source written against the
SDK APIs, resources, Gradle scripts, manifest, reference-code adaptation, and JVM unit
tests for `:core`.

### Track B — Do later (build machine with Android Studio + phone)
Open project → Gradle sync → install Android SDK packages → add API keys → build APK → run
on device → verify notifications on the Noise watch, TTS/voice, and live Navigation SDK.

---

## 3. Reuse map — take / modify / remove / add

| Source repo | Take (reuse) | Modify | Remove (unwanted) | Add (our stuff) |
|---|---|---|---|---|
| **GMapsParser** (`3v1n0/GMapsParser`) | `NotificationListenerService` pattern, Google Maps nav-notification parsing, distance/direction extraction | Map its output into our `NavigationState`/event model | WebSocket/event-server exposure — its way to push data to *external networked* devices; our consumers are in-process, so it is dead weight + attack surface | Navigator event model, our formatter hook |
| **MiBandNavigator** (`satvikpandurangi/MiBandNavigator`) | Background notification processing, compact turn formatting, battery/foreground patterns | Adapt compact-format logic behind our `WatchOutput` interface | Xiaomi/Zepp/Mi Band **BLE transport** as the *base* — hardware-specific and narrower than notification mirroring (kept as reference for an optional BLE adapter later) | `NotificationWatchOutput` (works for any watch that mirrors phone notifications) |
| **pebble-map-android** (`konsumer/pebble-map-android`) | Nav-data extraction ideas, compact directional output | Reference only — port ideas, not files | Pebble-specific transport | — |
| **Google Navigation SDK** (docs) | `Navigator`, `Waypoint`, `setDestination()`, `setDestinations()`, `startGuidance()`, listeners, simulation | Wrap behind our `NavigationManager` | — | Our tool → SDK adapter, TripState sync |

**Rule (from D-004):** reference repos are *source material*, not the product foundation.
Copy specific files/functions into our structure, keep our architecture.

### Multi-watch output — why we drop vendor transports (D-011)

We are **not** locking to one watch. All watch output goes through a `WatchOutput` interface
in `:core`:

- **`NotificationWatchOutput` (default):** posts a standard Android notification. This is the
  *most* compatible path — it works on Noise **and** virtually any watch that mirrors phone
  notifications (Wear OS, Amazfit/Zepp, Samsung, Fitbit, boAt, Fire-Boltt, etc.) with zero
  vendor-specific code.
- **Optional `BleWatchOutput` (later):** direct BLE for a specific band that needs richer
  output than a notification can carry (custom icons/haptics). MiBandNavigator is the
  reference for this adapter.

So the Xiaomi/Zepp BLE forwarding is removed **as the base**, not because multi-watch is
unwanted — notification mirroring already gives the widest coverage, and the interface lets
us add direct adapters without touching the rest of the app. Likewise GMapsParser's WebSocket
server is removed because it exposes data to *remote* consumers we don't have; our consumers
(TTS, formatter, watch) are all in-process.

---

## 4. Phase-wise plan

Each phase notes **[DO NOW]** vs **[BUILD MACHINE]**. Phases 0–9 are almost entirely
do-now (authoring). Phase 10 is the consolidated build/bring-up on the other machine.

### Phase 0 — Project scaffolding · [DO NOW]
- Create Gradle project: root `build.gradle`, `settings.gradle`, `gradle.properties`,
  Gradle **wrapper** (`gradlew`, `gradle-wrapper.jar/properties`), `.gitignore`.
- Create `:core` and `:app` modules; `:app/AndroidManifest.xml`; base package layout.
- Configure min/target SDK, Kotlin, versions in the build files (values only — no build yet).
- **Deliverable:** complete project tree that will Gradle-sync on the build machine.

### Phase 1 — Core domain (pure Kotlin) · [DO NOW]
- `:core`: `Trip`, `Stop`, `NavigationState`, `ManeuverType`, `RoutePreference`,
  `TripManager` (add/remove/reorder/clear/change-destination logic).
- JVM unit tests for all of the above (runnable with just a JDK, if available).
- **Deliverable:** the deterministic trip/state engine, fully unit-tested off-device.

### Phase 2 — Notification formatting + layer · [DO NOW write] · [BUILD MACHINE test]
- `:core`: `NotificationFormatter` (state → compact strings like `↰ LEFT / 200 m`, tiered
  by importance). Unit-test the string logic.
- `:app`: `WatchNotificationManager`, notification channel, posting code (adapted from
  MiBandNavigator patterns).
- **Later:** verify delivery/truncation/vibration on the actual Noise watch (MVP-0).

### Phase 3 — Google Maps parser bridge · [DO NOW adapt] · [BUILD MACHINE test]
- `:app`: `NotificationListenerService` + parser adapted from GMapsParser → emits our
  `NavigationState`.
- `:core`: parser→state mapping is pure logic → unit-testable with sample payloads.
- **Later:** run against live Google Maps navigation (MVP-1).

### Phase 4 — Voice output (TTS) + input (speech) · [DO NOW write] · [BUILD MACHINE test]
- `:app`: `TtsManager`, `SpeechRecognizer` wrappers, `VoiceSession`.
- `:core`: `CommandClassifier` for obvious commands (repeat / what's next / stop) — pure
  logic, unit-tested.
- **Later:** verify TTS over Bluetooth + recognition on device.

### Phase 5 — Navigation SDK integration · [DO NOW write] · [BUILD MACHINE wire+test]
- `:app`: `NavigationManager` wrapping `NavigationApi`/`Navigator`, `Waypoint`,
  `setDestination(s)`, `startGuidance`, listeners → updates `:core` `NavigationState`.
- Write all wiring against the documented API now; leave API key in a placeholder resource.
- **Later:** add Maps Platform key/billing, build, and test live + simulation (MVP-2).

### Phase 6 — Navigation tools + manual UI buttons · [DO NOW]
- `:core`: tool contracts + `ToolRegistry` + `ToolRouter` (set_destination, add_stop,
  remove_stop, reorder_stop, clear_stops, repeat_instruction, get_navigation_state,
  get_next_maneuver, recalculate_route, set_route_preference).
- `:app`: buttons calling those tools → SDK. Prove `tool → SDK` design before AI (MVP-3).

### Phase 7 — Agent / tool-calling · [DO NOW provider-agnostic] · [BUILD MACHINE keys]
- `:core`: `NavigatorAgent`, `ToolSchemas` (strict JSON), provider-agnostic interface.
- Unit-test tool selection with mocked LLM responses.
- **Later:** plug in the chosen LLM provider + API key on device (MVP-4).

### Phase 8 — Navigation context + rider commands · [DO NOW]
- `:core`: `AgentContextBuilder` (compact JSON context), rider command tiers 1–4,
  "should I take the flyover?" logic driven purely by structured state.
- Unit-test each command against sample states (MVP-5).

### Phase 9 — Optimization + safety/UX · [DO NOW code] · [BUILD MACHINE tune]
- Notification tiering/anti-spam, foreground-service lifecycle, short-TTS policy,
  confirmation for bulk/risky changes, graceful failure + explicit uncertainty (D-008).
- **Later:** battery + real-world tuning on device.

### Phase 10 — Build & device bring-up · [BUILD MACHINE only]
- Copy repo to Android-Studio machine → open → Gradle sync → install SDK packages.
- Add API keys/signing → build APK → run on phone.
- Execute deferred tests in order: MVP-0 (watch) → MVP-1 (parser) → MVP-2 (SDK) →
  MVP-3 (tools) → MVP-4 (agent) → MVP-5 (rider intelligence).

---

## 5. Deferred checklist — everything that waits for the build machine

- Gradle sync of Android modules + Android SDK package downloads.
- Building/installing the APK; running on a physical phone.
- Noise watch notification delivery/truncation/vibration.
- TTS over Bluetooth headset; on-device speech recognition.
- Google Navigation SDK: API key, billing/licensing, live routing + simulation.
- LLM provider API key + live tool-calling.
- Battery and real-world road testing.

## 6. Handoff checklist for the build machine

- [ ] Whole repo copied (including Gradle wrapper).
- [ ] Android Studio + matching Android SDK / build-tools installed.
- [ ] `local.properties` / secrets: Maps Platform API key, LLM key (never committed).
- [ ] Gradle sync succeeds; `:core` unit tests green.
- [ ] Notification-listener permission + notification access granted on phone.
- [ ] Noise companion app: Navigator notifications enabled.

## 7. What we can prove without Android Studio

- All of `:core` compiles and passes unit tests with only a JDK (trip logic, formatter,
  command classifier, tool router, context builder, flyover logic).
- Parser mapping and formatting verified against saved sample payloads.
- Everything else is written, reviewed, and ready — pending only a build.

---

_See [project_memory/](project_memory/) for the living log, decisions (D-009, D-010),
current state, task tracker, and file index._
