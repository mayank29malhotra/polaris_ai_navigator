# Navigator — File Index

> List of created/modified files with purpose. Update whenever a file is created, renamed,
> deleted, or structurally changed.

**Last updated:** 2026-08-25 13:44

---

## Documentation (root)

| File | Status | Purpose |
|---|---|---|
| [Navigator_brd.md](../Navigator_brd.md) | existing | Business Requirements Document: product summary, goals, functional requirements (FR-01…FR-15), agent tools & behavior rules, safety, data model, notification/voice UX, MVP-0…MVP-5, acceptance criteria, risks. |
| [Plan_navigator.md](../Plan_navigator.md) | existing | Phased Implementation Plan: development strategy, Phase 0–14, recommended dependency order, suggested repo structure, GitHub references, definition of done, long-term roadmap. |
| [Execution_Plan.md](../Execution_Plan.md) | created | Reuse-first, build-later execution plan: do-now vs build-machine tracks, reuse map (take/modify/remove/add per repo), `:core`/`:app` module split, per-phase Android-Studio-dependency flags, handoff checklist. |

## Project memory (`/project_memory/`)

| File | Status | Purpose |
|---|---|---|
| [SESSION_LOG.md](SESSION_LOG.md) | created | Chronological, append-only log of all meaningful actions. |
| [ARCHITECTURE.md](ARCHITECTURE.md) | created | System design and its evolution (target architecture, modules, data flow, engine strategy, tool contract). |
| [DECISIONS.md](DECISIONS.md) | created | Design decisions with reasoning, alternatives, and tradeoffs (D-001…D-008). |
| [CURRENT_STATE.md](CURRENT_STATE.md) | created | Full current-state snapshot: what exists, what's stable/experimental, limitations, next step. |
| [TASK_TRACKER.md](TASK_TRACKER.md) | created | Completed and pending tasks, organized by phase/MVP. |
| [FILE_INDEX.md](FILE_INDEX.md) | created | This file — index of all files and their purpose. |

## Build & config (root)

| File | Status | Purpose |
|---|---|---|
| [settings.gradle.kts](../settings.gradle.kts) | created | Root settings: plugin/dependency repositories; includes `:app` and `:core`. |
| [build.gradle.kts](../build.gradle.kts) | created | Root build: declares AGP + Kotlin plugins (applied per module). |
| [gradle.properties](../gradle.properties) | created | JVM args, AndroidX flags, Kotlin code style. |
| [gradle/libs.versions.toml](../gradle/libs.versions.toml) | created | Version catalog (AGP 8.5.2, Kotlin 2.0.20, AndroidX libraries + Google Navigation SDK). |
| gradle/wrapper/gradle-wrapper.properties | created | Wrapper config → Gradle 8.9. |
| gradle/wrapper/gradle-wrapper.jar | created | Official Gradle 8.9 wrapper bootstrap jar. |
| gradlew / gradlew.bat | created | Gradle wrapper launch scripts (Unix / Windows). |
| [.gitignore](../.gitignore) | created | Ignore build outputs, IDE files, local secrets; keep wrapper jar. |

## `:core` module (pure Kotlin / JVM)

| File | Status | Purpose |
|---|---|---|
| [core/build.gradle.kts](../core/build.gradle.kts) | created | Kotlin JVM module (JDK 17 toolchain, JUnit). |
| [geo/LatLng.kt](../core/src/main/kotlin/com/navigator/core/geo/LatLng.kt) | created | Validated WGS84 coordinate. |
| [util/Clock.kt](../core/src/main/kotlin/com/navigator/core/util/Clock.kt) | created | Testable wall-clock abstraction (`Clock.SYSTEM`). |
| [trip/Stop.kt](../core/src/main/kotlin/com/navigator/core/trip/Stop.kt) | created | `Stop` + `StopType` (waypoint/destination). |
| [trip/RoutePreference.kt](../core/src/main/kotlin/com/navigator/core/trip/RoutePreference.kt) | created | Avoid tolls/highways/ferries preferences. |
| [trip/Trip.kt](../core/src/main/kotlin/com/navigator/core/trip/Trip.kt) | created | Immutable trip + `TripStatus`; pure stop/destination edits; 25-waypoint limit. |
| [trip/TripManager.kt](../core/src/main/kotlin/com/navigator/core/trip/TripManager.kt) | created | Holds the active trip; applies edits; stamps `updatedAt` via `Clock`. |
| [nav/ManeuverType.kt](../core/src/main/kotlin/com/navigator/core/nav/ManeuverType.kt) | created | Normalized maneuver kinds. |
| [nav/Maneuver.kt](../core/src/main/kotlin/com/navigator/core/nav/Maneuver.kt) | created | Upcoming maneuver (type, distance, road). |
| [nav/RouteStatus.kt](../core/src/main/kotlin/com/navigator/core/nav/RouteStatus.kt) | created | Route status enum. |
| [nav/NavigationState.kt](../core/src/main/kotlin/com/navigator/core/nav/NavigationState.kt) | created | Immutable live-navigation read-model (D-005). |
| [nav/NavigationStateStore.kt](../core/src/main/kotlin/com/navigator/core/nav/NavigationStateStore.kt) | created | Holds latest `NavigationState` + notifies a listener (shared producer→consumer). |
| [nav/NavigationController.kt](../core/src/main/kotlin/com/navigator/core/nav/NavigationController.kt) | created | SDK-agnostic navigation control interface (impl in `:app`). |
| [watch/WatchOutput.kt](../core/src/main/kotlin/com/navigator/core/watch/WatchOutput.kt) | created | Transport-agnostic watch-output interface (D-011). |
| [watch/WatchUpdate.kt](../core/src/main/kotlin/com/navigator/core/watch/WatchUpdate.kt) | created | `WatchUpdate` data class + `WatchPriority` enum. |
| [format/NotificationFormatter.kt](../core/src/main/kotlin/com/navigator/core/format/NotificationFormatter.kt) | created | `NavigationState` → compact `WatchUpdate` (glyph + label + distance, tiered priority). |
| [parse/RawNavNotification.kt](../core/src/main/kotlin/com/navigator/core/parse/RawNavNotification.kt) | created | Android-free holder of notification text fields. |
| [parse/GoogleMapsNotificationParser.kt](../core/src/main/kotlin/com/navigator/core/parse/GoogleMapsNotificationParser.kt) | created | Google Maps notification text → `NavigationState` (distance/maneuver/road/arrival). |
| [voice/VoiceCommand.kt](../core/src/main/kotlin/com/navigator/core/voice/VoiceCommand.kt) | created | Recognized Tier-1 voice commands. |
| [voice/CommandClassifier.kt](../core/src/main/kotlin/com/navigator/core/voice/CommandClassifier.kt) | created | Transcript → `VoiceCommand` (keyword rules). |
| [voice/SpokenInstructionFormatter.kt](../core/src/main/kotlin/com/navigator/core/voice/SpokenInstructionFormatter.kt) | created | State/command → concise spoken sentence. |
| [tools/Tooling.kt](../core/src/main/kotlin/com/navigator/core/tools/Tooling.kt) | created | Tool framework: `NavigatorTool`, `ToolArgs`, `ToolResult`, `ToolRegistry`. |
| [tools/NavigationTools.kt](../core/src/main/kotlin/com/navigator/core/tools/NavigationTools.kt) | created | `ToolContext` + 11 navigation tools + `NavigatorToolset.standard`. |
| [agent/Llm.kt](../core/src/main/kotlin/com/navigator/core/agent/Llm.kt) | created | Provider-agnostic `LlmClient` + `LlmRequest`/`LlmResponse`. |
| [agent/ToolSchemas.kt](../core/src/main/kotlin/com/navigator/core/agent/ToolSchemas.kt) | created | `ToolSchema` + strict OpenAI-style function JSON from tools. |
| [agent/NavigatorAgent.kt](../core/src/main/kotlin/com/navigator/core/agent/NavigatorAgent.kt) | created | Transcript → local answer or LLM tool call via the registry. |
| [agent/AgentContextBuilder.kt](../core/src/main/kotlin/com/navigator/core/agent/AgentContextBuilder.kt) | created | Compact JSON navigation context for the LLM (incl. upcoming). |
| [policy/WatchUpdatePolicy.kt](../core/src/main/kotlin/com/navigator/core/policy/WatchUpdatePolicy.kt) | created | Watch anti-spam (suppress identical/rapid non-critical updates). |
| [policy/SpeechPolicy.kt](../core/src/main/kotlin/com/navigator/core/policy/SpeechPolicy.kt) | created | Short-TTS: first sentence / max length (decimal-safe). |
| [policy/ConfirmationPolicy.kt](../core/src/main/kotlin/com/navigator/core/policy/ConfirmationPolicy.kt) | created | Flags destructive/bulk tools needing confirmation. |
| _tests_ | | |
| [geo/LatLngTest.kt](../core/src/test/kotlin/com/navigator/core/geo/LatLngTest.kt) | created | LatLng validation. |
| [trip/TripTest.kt](../core/src/test/kotlin/com/navigator/core/trip/TripTest.kt) | created | Trip edit / reindex / waypoint-limit behavior. |
| [trip/TripManagerTest.kt](../core/src/test/kotlin/com/navigator/core/trip/TripManagerTest.kt) | created | Manager state + timestamps (fake clock). |
| [nav/NavigationStateTest.kt](../core/src/test/kotlin/com/navigator/core/nav/NavigationStateTest.kt) | created | Read-model defaults / values. |
| [nav/NavigationStateStoreTest.kt](../core/src/test/kotlin/com/navigator/core/nav/NavigationStateStoreTest.kt) | created | Store update + listener notification. |
| [watch/WatchUpdateTest.kt](../core/src/test/kotlin/com/navigator/core/watch/WatchUpdateTest.kt) | created | `WatchUpdate` defaults. |
| [format/NotificationFormatterTest.kt](../core/src/test/kotlin/com/navigator/core/format/NotificationFormatterTest.kt) | created | Formatter tiers, glyphs, distance formatting. |
| [parse/GoogleMapsNotificationParserTest.kt](../core/src/test/kotlin/com/navigator/core/parse/GoogleMapsNotificationParserTest.kt) | created | Parser cases (units, road, arrival, ETA rejection). |
| [voice/CommandClassifierTest.kt](../core/src/test/kotlin/com/navigator/core/voice/CommandClassifierTest.kt) | created | Command classification cases. |
| [voice/SpokenInstructionFormatterTest.kt](../core/src/test/kotlin/com/navigator/core/voice/SpokenInstructionFormatterTest.kt) | created | Spoken responses (turns, distance, ETA). |
| [tools/NavigationToolsTest.kt](../core/src/test/kotlin/com/navigator/core/tools/NavigationToolsTest.kt) | created | Tool dispatch: set/add/remove/clear + errors. |
| [agent/ToolSchemasTest.kt](../core/src/test/kotlin/com/navigator/core/agent/ToolSchemasTest.kt) | created | Schema coverage + JSON shape. |
| [agent/NavigatorAgentTest.kt](../core/src/test/kotlin/com/navigator/core/agent/NavigatorAgentTest.kt) | created | Fast-path vs LLM tool-call routing (mock LLM). |
| [agent/AgentContextBuilderTest.kt](../core/src/test/kotlin/com/navigator/core/agent/AgentContextBuilderTest.kt) | created | Context JSON fields + upcoming array. |
| [policy/WatchUpdatePolicyTest.kt](../core/src/test/kotlin/com/navigator/core/policy/WatchUpdatePolicyTest.kt) | created | Throttle / suppression / critical-passthrough. |
| [policy/SpeechPolicyTest.kt](../core/src/test/kotlin/com/navigator/core/policy/SpeechPolicyTest.kt) | created | First-sentence / truncation / decimal-safe. |

## `:app` module (Android)

| File | Status | Purpose |
|---|---|---|
| [app/build.gradle.kts](../app/build.gradle.kts) | updated | Android application module (SDK levels, viewBinding, depends on `:core` + Navigation SDK; `MAPS_API_KEY` from local.properties). |
| [app/proguard-rules.pro](../app/proguard-rules.pro) | created | Placeholder R8/ProGuard keep-rules. |
| [AndroidManifest.xml](../app/src/main/AndroidManifest.xml) | updated | `MainActivity` + `GoogleMapsBridgeService`; notification/mic/location perms; recognition `<queries>`; Maps API key + play-services meta-data. |
| [MainActivity.kt](../app/src/main/java/com/navigator/app/MainActivity.kt) | updated | Demo buttons (watch/bridge/voice/nav/tools); shared store → watch; `ToolRegistry` + `NavigatorAgent` (placeholder LLM) driving voice. |
| [watch/NotificationWatchOutput.kt](../app/src/main/java/com/navigator/app/watch/NotificationWatchOutput.kt) | created | `WatchOutput` impl posting standard Android notifications (channel + priority mapping). |
| [bridge/GoogleMapsBridgeService.kt](../app/src/main/java/com/navigator/app/bridge/GoogleMapsBridgeService.kt) | created | `NotificationListenerService` → parses Google Maps notifications → formats → watch. |
| [voice/TtsManager.kt](../app/src/main/java/com/navigator/app/voice/TtsManager.kt) | created | TextToSpeech wrapper for concise spoken guidance. |
| [voice/SpeechInput.kt](../app/src/main/java/com/navigator/app/voice/SpeechInput.kt) | created | One-shot `SpeechRecognizer` wrapper. |
| [voice/VoiceSession.kt](../app/src/main/java/com/navigator/app/voice/VoiceSession.kt) | updated | speech → `respond` (agent) → speak; provider-agnostic. |
| [nav/GoogleNavigationManager.kt](../app/src/main/java/com/navigator/app/nav/GoogleNavigationManager.kt) | created | Google Navigation SDK wrapper implementing `NavigationController`; updates the store. |
| [agent/PlaceholderLlmClient.kt](../app/src/main/java/com/navigator/app/agent/PlaceholderLlmClient.kt) | created | Stub `LlmClient` until a real provider + key is configured. |
| [activity_main.xml](../app/src/main/res/layout/activity_main.xml) | updated | Scrollable: status + all demo buttons (watch / bridge / voice / nav / tools). |
| [strings.xml](../app/src/main/res/values/strings.xml) | updated | App name + Phase 2–6 UI strings + listener label. |
| [colors.xml](../app/src/main/res/values/colors.xml) | created | Brand + launcher colors. |
| [themes.xml](../app/src/main/res/values/themes.xml) | created | `Theme.Navigator` (Material3). |
| [ic_launcher.xml](../app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml) | created | Adaptive launcher icon. |
| [ic_launcher_round.xml](../app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml) | created | Adaptive round launcher icon. |
| [ic_launcher_foreground.xml](../app/src/main/res/drawable/ic_launcher_foreground.xml) | created | Vector launcher foreground (nav arrow). |
