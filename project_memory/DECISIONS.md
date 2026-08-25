# Navigator — Decisions

> Reasoning + tradeoffs. Record every design choice, alternative considered, and why.
> Append-only. Do not delete superseded decisions — mark them as superseded instead.

Entry format:

```
### D-NNN — <title>  (YYYY-MM-DD)
- Status: accepted | superseded | proposed
- Context:
- Decision:
- Alternatives considered:
- Tradeoffs / consequences:
```

---

### D-001 — Android-first, not watch-first  (2026-08-25)
- Status: accepted
- Context: Rider has a phone (GPS/compute), a Noise smartwatch (notifications only), and a
  Bluetooth headset. The watch is not expected to provide GPS/navigation.
- Decision: Build Navigator as an Android application. The watch is a secondary glanceable
  output surface fed by Android notifications.
- Alternatives considered: Building a watch app / direct wearable integration.
- Tradeoffs: Simpler, uses mature Android infrastructure; depends on the Noise companion
  app's notification mirroring behavior (varies by model, must be tested).

### D-002 — Navigation engine is authoritative; AI only orchestrates  (2026-08-25)
- Status: accepted
- Context: LLMs hallucinate route facts. Safety-critical riding context.
- Decision: The routing/navigation engine is the single source of truth. The agent may only
  call deterministic tools and phrase structured results. It must never infer road choices
  from general LLM knowledge.
- Alternatives considered: Letting the LLM reason directly about routes/maps.
- Tradeoffs: More engineering (tool contract, state) but reliable and defensible; prevents
  fabricated navigation answers.

### D-003 — Two-stage navigation engine (parser bridge → Navigation SDK)  (2026-08-25)
- Status: accepted
- Context: Need useful data fast, but also want long-term control of the trip.
- Decision: Stage 1 uses a `NotificationListenerService` to parse Google Maps navigation
  notifications (GMapsParser as reference) for a quick prototype. Stage 2 migrates to the
  Google Maps Platform Navigation SDK for Android, which owns the navigation session.
- Alternatives considered: (a) Only parse Google Maps notifications forever; (b) jump
  straight to the Navigation SDK.
- Tradeoffs: Parser bridge is fast but fragile and read-only (cannot change destination).
  Navigation SDK enables trip editing but needs setup, billing/licensing review.

### D-004 — Do not fork a wearable-nav repo as the product base  (2026-08-25)
- Status: accepted
- Context: GMapsParser, MiBandNavigator, and pebble-map-android solve parts of the problem.
- Decision: Use them strictly as reference code, not as the foundation of the final
  architecture. Build TripState, tools, agent, formatter, and UX ourselves.
- Alternatives considered: Forking MiBandNavigator and extending it.
- Tradeoffs: More initial work, but avoids inheriting Xiaomi/Zepp-specific or
  parser-locked architecture that cannot support agent-driven trip editing.

### D-005 — Central TripState / NavigationState as single source of truth  (2026-08-25)
- Status: accepted
- Context: Agent, TTS, and watch all need consistent navigation data.
- Decision: One central state object; the SDK updates it, and all subsystems read from it.
  The agent mutates state only through deterministic tools, not by poking the SDK directly.
- Alternatives considered: Each subsystem querying the SDK independently.
- Tradeoffs: Slight indirection; gains consistency, testability, and clean agent boundary.

### D-006 — Build tools + manual buttons before adding the LLM  (2026-08-25)
- Status: accepted
- Context: Want to prove `tool → navigation SDK` before introducing AI variability.
- Decision: Implement navigation tools and expose them as UI buttons first (MVP-3), then
  add speech + LLM tool-calling (MVP-4).
- Alternatives considered: Wiring the LLM in from the start.
- Tradeoffs: Slower to reach the "voice" demo, but de-risks and isolates failures.

### D-007 — Persistent project memory lives in `/project_memory/`  (2026-08-25)
- Status: accepted
- Context: Project must survive lost chat context / model changes.
- Decision: Maintain six append-only memory files under `/project_memory/`. Update after
  every meaningful action; never overwrite history.
- Alternatives considered: Relying on chat memory only.
- Tradeoffs: Small maintenance overhead; guarantees reconstructibility.

### D-008 — No promise of exact arbitrary-road enforcement in v1  (2026-08-25)
- Status: accepted
- Context: "Take the service road" cannot always be guaranteed by the routing engine.
- Decision: Support avoid/prefer preferences that the SDK supports; when a specific road
  cannot be guaranteed, the app must say so rather than claim success.
- Alternatives considered: Faking compliance.
- Tradeoffs: Less "magical", but honest and safe; surfaces AI/routing uncertainty.

### D-009 — Defer Android Studio; author now, build later  (2026-08-25)
- Status: accepted
- Context: Android Studio may not be permitted on the current machine. Android code is just
  text (Kotlin/XML/Gradle); only compiling/running/on-device testing needs the SDK + IDE.
- Decision: Author 100% of files/code (source, resources, Gradle, manifest, unit tests) now
  in VS Code. Copy the repo to a machine with Android Studio + Android SDK + phone to build
  the APK and run on-device tests. See `Execution_Plan.md` Track A vs Track B.
- Alternatives considered: Waiting until an Android-Studio machine is available before
  writing anything; installing command-line Android SDK tools here (may be equally blocked).
- Tradeoffs: Cannot compile the Android `:app` here, so on-device behavior is unverified
  until the build machine; gains full progress on all code + logic now with a clean handoff.

### D-010 — Split `:core` (pure Kotlin/JVM) from `:app` (Android)  (2026-08-25)
- Status: accepted
- Context: Want to maximize what can be built and tested without the Android SDK, and to
  keep the deterministic navigation logic isolated from the platform (reinforces D-002).
- Decision: `:core` holds all Android-independent logic (TripState, models, formatter,
  command classifier, tool contracts/router, agent context builder) and is unit-testable
  with only a JDK. `:app` holds Android/SDK code and depends on `:core`.
- Alternatives considered: Single Android module (simpler tree, but nothing is testable
  without the Android SDK).
- Tradeoffs: Slightly more Gradle setup; gains off-device unit testing and a clean
  logic/platform boundary.

### D-011 — Abstract `WatchOutput`; notification mirroring as the multi-watch default  (2026-08-25)
- Status: accepted
- Context: MiBandNavigator forwards to Mi Band/Amazfit via a vendor-specific BLE/Zepp
  transport. Our target is a Noise watch, and we want to support other watches too.
- Decision: Define a `WatchOutput` interface in `:core`. Default implementation
  `NotificationWatchOutput` (`:app`) posts standard Android notifications — the widest-
  compatibility path (Noise, Wear OS, Amazfit, Samsung, Fitbit, boAt, Fire-Boltt, etc.).
  Keep an optional `BleWatchOutput` for later, using MiBandNavigator as reference. Do not
  adopt the Xiaomi/Zepp BLE forwarding as the base.
- Alternatives considered: (a) Keep MiBandNavigator's BLE forwarding for multi-watch — but
  it is narrower than notification mirroring and vendor-locked; (b) hardcode Noise-only.
- Tradeoffs: One extra interface; gains broad watch coverage now and pluggable direct
  adapters later without touching the rest of the app.
