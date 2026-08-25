# Navigator — Business Requirements Document (BRD)

## 1. Product summary

**Navigator** is a hands-free motorcycle riding assistant for Android phones. It is designed for riders whose bike has no navigation display and who do not want to continuously look at a phone.

The phone is the primary computing/GPS device. A connected Bluetooth headset provides concise spoken guidance. A compatible Noise smartwatch acts as a secondary glanceable/haptic output by receiving Android notifications through the watch's existing notification mirroring.

The long-term differentiator is an agent interface: the rider can ask navigation questions or modify the trip using short voice commands such as:

- “What’s next?”
- “Repeat.”
- “Should I take the flyover?”
- “Change destination to Manyata Tech Park.”
- “Add a stop at Orion Mall.”
- “Remove the next stop.”
- “Avoid toll roads.”
- “Take me home instead.”

The agent must never invent route facts. It should call deterministic navigation tools and use the navigation engine as the source of truth.

---

## 2. Problem

The rider has:

- no navigation display on the motorcycle;
- a Noise smartwatch that can show phone notifications but is not expected to provide GPS/navigation itself;
- Bluetooth earphones/headset;
- difficulty following long or continuous voice instructions;
- a need for short, context-aware navigation information without looking at a phone.

Existing navigation apps solve route calculation, but their interaction model is not optimized for this exact hardware setup or for natural voice modification of an active trip.

---

## 3. Goals

### Primary goals

1. Provide safe, concise turn-by-turn information without requiring the rider to look at the phone.
2. Show important navigation events on the Noise watch through Android notifications.
3. Provide short spoken guidance through Bluetooth earphones.
4. Allow the rider to ask contextual navigation questions by voice.
5. Allow the rider to modify an active route by voice.
6. Recalculate automatically after route changes or off-route events.
7. Keep the AI layer separate from the deterministic navigation/routing engine.
8. Build as much as possible on mature existing navigation infrastructure rather than implementing maps/routing from scratch.

### Secondary goals

- Maintain a trip state and ordered stop list.
- Support repeat/next/status commands.
- Provide a compact rider-focused UI.
- Support route simulation for testing.
- Make the project strong enough to demonstrate Android, APIs, agent/tool calling, navigation, event-driven architecture and wearable integration.

### Non-goals for v1

- Building a new maps database.
- Building a new routing engine.
- Continuous conversational AI while riding.
- Replacing a phone's full navigation UI.
- Direct BLE integration with the Noise watch unless notification mirroring proves insufficient.
- Exact arbitrary-road selection as a first-release feature.

---

## 4. Target architecture

```text
                   ┌─────────────────────────┐
                   │        RIDER            │
                   │ Voice / headset button  │
                   └────────────┬────────────┘
                                │
                                ▼
                   ┌─────────────────────────┐
                   │ Android Navigator App   │
                   │                         │
                   │ Voice Input             │
                   │ Agent / Intent Router    │
                   │ Trip State               │
                   │ Notification Formatter   │
                   │ TTS                     │
                   └───────┬─────────┬───────┘
                           │         │
                    tool calls       │
                           │         │
                           ▼         ▼
                ┌────────────────┐  ┌─────────────┐
                │ Navigation SDK │  │ Android TTS │
                │ / Navigator    │  │ + Bluetooth │
                └───────┬────────┘  └─────────────┘
                        │
                        │ navigation events,
                        │ route/maneuver state
                        ▼
                ┌────────────────────┐
                │ Instruction Engine │
                └─────────┬──────────┘
                          │
                          ▼
                 Android Notifications
                          │
                          ▼
                     Noise Watch
```

---

## 5. Core product components

### A. Android application

Own:

- permissions;
- foreground navigation service;
- location lifecycle;
- navigation session;
- trip state;
- voice interaction;
- agent/tool execution;
- notification generation;
- TTS;
- logging/debugging;
- settings;
- battery-management guidance.

### B. Navigation engine

Preferred production direction: **Google Maps Platform Navigation SDK for Android**, subject to current account, billing, licensing and motorcycle-routing requirements.

Google's current Navigation SDK exposes a `Navigator` for controlling navigation, including `setDestination()` and `setDestinations()`. It supports multiple destinations/waypoints and navigation events. Google's documentation currently states a maximum of 25 waypoints including the final destination.

The SDK is therefore a strong fit for controlled navigation inside Navigator rather than merely parsing another app's notifications.

### C. Agent layer

The agent translates natural language into a small set of deterministic navigation tools.

Example:

User:
> “Add a stop at Orion Mall.”

Agent:
```text
resolve_location("Orion Mall")
add_stop(place)
```

The navigation layer performs the actual route change.

### D. Noise watch output

Do not assume direct watch GPS is necessary.

First implementation:

```text
Navigator app
    ↓
Android notification
    ↓
Noise companion app / notification mirroring
    ↓
Noise watch
```

The exact Noise model and companion-app behavior must be tested because notification capabilities vary by model.

### E. Bluetooth audio

Use Android's normal audio routing and TTS.

The application should generate short messages rather than long spoken explanations.

---

## 6. Functional requirements

### FR-01 — Start navigation

The rider can select or speak a destination.

Examples:

- “Navigate to office.”
- “Take me to Manyata Tech Park.”
- “Start navigation to home.”

Expected behavior:

1. Resolve destination.
2. Confirm if ambiguous.
3. Create route.
4. Start guidance.
5. Announce concise first instruction.
6. Push relevant instruction to watch.

---

### FR-02 — Turn-by-turn guidance

The app must provide:

- next maneuver;
- approximate distance;
- road/street where useful;
- maneuver direction;
- arrival/stop state;
- rerouting state.

Examples:

> “Left in 300 metres.”

> “Take the flyover in 100 metres.”

> “Turn right now.”

---

### FR-03 — Noise watch notification

The app should generate compact notifications such as:

```text
↰ LEFT
200 m
```

```text
↑ STRAIGHT
800 m
```

```text
↱ RIGHT NOW
```

```text
🛣 TAKE FLYOVER
```

The notification design must be tested against the actual Noise watch screen size and notification truncation.

---

### FR-04 — Voice guidance

TTS should provide the same essential information through the headset.

Examples:

> “Turn left in 200 metres.”

> “Take the flyover.”

> “Continue straight for one kilometre.”

Avoid verbose language.

---

### FR-05 — Repeat

Voice command:

> “Repeat.”

Expected:

> “Turn left in 200 metres.”

---

### FR-06 — What's next?

Voice command:

> “What’s next?”

Expected:

> “Take the flyover in about 400 metres.”

---

### FR-07 — Route question

Voice command:

> “Should I take the flyover?”

The agent must inspect current navigation state and answer using structured route/maneuver data.

It must not infer road choices from an LLM's general knowledge.

---

### FR-08 — Change destination

Example:

> “Change destination to home.”

Tool:

```text
change_destination(destination)
```

The previous final destination is replaced and navigation is recalculated.

Google's Navigation SDK supports replacing the current destination through `setDestination()`.

---

### FR-09 — Add stop

Example:

> “Add a stop at Orion Mall.”

Tool:

```text
add_stop(location)
```

The stop becomes a waypoint in the active trip.

Google's Navigation SDK supports multiple destinations through `setDestinations()`.

---

### FR-10 — Remove stop

Examples:

> “Remove the next stop.”

> “Remove Orion Mall.”

The application updates the trip's waypoint list and recalculates.

---

### FR-11 — Edit stop

Examples:

> “Change my second stop to Phoenix Mall.”

> “Move the gym stop before the office.”

This should operate on Navigator's own trip-state list and then call the navigation SDK with the updated ordered destinations.

---

### FR-12 — Avoid route preference

Examples:

> “Avoid tolls.”

> “Avoid highways.”

The agent maps the command to supported routing options.

If the requested preference is not supported exactly, the app must say so rather than pretending it has complied.

---

### FR-13 — Preferred path / particular road

Example:

> “Take the Hebbal flyover.”

Possible implementation order:

1. Resolve the named road/landmark.
2. Create a waypoint if appropriate.
3. Recalculate.
4. Verify that the resulting route actually uses the intended segment if verification is available.
5. Otherwise tell the rider that the request was approximated.

Exact arbitrary-road enforcement is **not** a v1 promise.

---

### FR-14 — Off-route detection

If the rider deviates:

1. Detect route deviation using navigation SDK state.
2. Allow navigation engine to recalculate.
3. Update trip state.
4. Announce concise rerouting message.
5. Update watch instruction.

---

### FR-15 — Arrival

When reaching a waypoint:

> “Stop 2 reached. Continue to office.”

At final destination:

> “You’ve arrived.”

---

## 7. Agent tools

The initial tool contract should be deliberately small.

```text
resolve_location(query)

start_navigation(destination)

change_destination(destination)

add_stop(location)

remove_stop(identifier)

reorder_stop(from_index, to_index)

clear_stops()

get_current_navigation_state()

get_next_maneuver()

get_upcoming_maneuvers()

repeat_instruction()

recalculate_route()

set_route_preference(preference)

get_trip_state()

stop_navigation()
```

Later:

```text
avoid_road(road)
prefer_road(road)
choose_route(route_id)
save_place(name, location)
```

The agent should never directly mutate navigation internals.

---

## 8. Agent behavior rules

### Rule 1 — Navigation engine is authoritative

The LLM cannot decide:

> “The flyover is probably correct.”

It must inspect navigation data.

### Rule 2 — Tool first, answer second

For navigation questions:

```text
User question
    ↓
Agent
    ↓
Navigation tool
    ↓
Structured state
    ↓
Short answer
```

### Rule 3 — Ask for clarification when ambiguous

Example:

> “Take me to the mall.”

If multiple likely destinations exist:

> “Which mall?”

### Rule 4 — Confirm destructive changes when useful

For:

> “Change destination.”

If destination is unambiguous, execute directly.

For a potentially accidental command:

> “Remove all stops.”

Prefer confirmation.

### Rule 5 — Keep responses short

Maximum normal spoken response target: one sentence.

---

## 9. Safety requirements

This is a riding-assistance application, not a substitute for rider attention.

Requirements:

- no long conversational responses while riding;
- no distracting visual UI requirement;
- no continuous unnecessary notifications;
- important instructions prioritized;
- voice output short;
- commands should be tolerant of noisy environments;
- avoid requiring the rider to type;
- route state must be deterministic;
- AI uncertainty must be surfaced;
- no claim of exact road compliance unless verified.

A physical headset button, steering control or other safe trigger can be considered later. Do not design around handling the phone while moving.

---

## 10. GitHub projects to study/reuse

### 1. GMapsParser — highest-value reference for MVP 1–3

**Repository:** `3v1n0/GMapsParser`

GitHub:
https://github.com/3v1n0/GMapsParser

What it provides:

- Kotlin Android implementation;
- Google Maps turn-by-turn notification parsing;
- structured navigation events;
- navigation notification listener;
- optional WebSocket/event exposure.

Use it for:

- understanding Google Maps notification parsing;
- learning how navigation events can be transformed into structured data;
- building the initial notification-based prototype.

Do **not** make it the foundation of the final controlled-navigation architecture if the Google Navigation SDK route works for the project.

---

### 2. MiBandNavigator — highest-value wearable reference

**Repository:** `satvikpandurangi/MiBandNavigator`

GitHub:
https://github.com/satvikpandurangi/MiBandNavigator

What it provides:

- Android `NotificationListenerService`;
- Google Maps navigation notification interception;
- compact turn-by-turn formatting;
- wearable notification forwarding;
- background-service patterns;
- battery-optimization considerations.

Use it for:

- Noise notification architecture inspiration;
- compact navigation notification formatting;
- wearable limitations;
- background execution patterns.

Do not copy its Xiaomi/Zepp-specific forwarding layer for Noise unless the Noise ecosystem requires an equivalent integration.

---

### 3. Pebble Maps Nav

**Repository:** `konsumer/pebble-map-android`

GitHub:
https://github.com/konsumer/pebble-map-android

Use it as an additional reference for:

- extracting navigation data;
- converting navigation events into wearable-friendly information;
- forwarding arrows/distance/street information.

---

### 4. Google Navigation SDK documentation — production foundation

Official documentation:

https://developers.google.com/maps/documentation/navigation/android-sdk

Use this for the final navigation engine.

Important capabilities:

- `Navigator`;
- destination management;
- multiple destinations;
- route/navigation events;
- audio guidance;
- route preferences;
- navigation simulation/testing.

Google currently documents `setDestination()` for replacing a destination and `setDestinations()` for multiple destinations/waypoints.

---

## 11. What to reuse vs build yourself

| Component | Reuse | Build yourself |
|---|---|---|
| Routing | Google Navigation SDK | — |
| GPS/location | Navigation SDK/Android | — |
| Turn-by-turn engine | Google Navigation SDK | — |
| Map UI | SDK initially | Optional custom minimal UI |
| Google Maps notification parser | GMapsParser for prototype/reference | Final integration if required |
| Wearable notification idea | MiBandNavigator | Noise-specific testing/configuration |
| Voice/TTS | Android APIs | Message-selection logic |
| Speech recognition | Android/Google speech APIs initially | Command handling |
| LLM/agent | Existing Orion concepts | Rider-specific agent |
| Tool calling | — | **Build yourself** |
| Trip state | — | **Build yourself** |
| Destination editing | SDK + your tools | **Build yourself** |
| Stop management | SDK + your tools | **Build yourself** |
| “What’s next?” | SDK events | **Build yourself** |
| “Repeat” | SDK state | **Build yourself** |
| Route-question answering | SDK state | **Build yourself** |
| Notification formatter | Inspiration from repos | **Build yourself** |
| Noise integration | Existing notification mechanism | **Build/test yourself** |
| Riding UX | — | **Build yourself** |
| Safety/confirmation rules | — | **Build yourself** |
| Testing/simulation | SDK simulator | Test suite yourself |

---

## 12. Key product decision

Do **not** fork one of the wearable-navigation repositories and try to turn it into the entire product.

Use them as reference code.

The strongest architecture is:

```text
Google Navigation SDK
        +
Your Android application
        +
Your deterministic navigation tools
        +
Your agent
        +
Android notifications
        +
Noise watch
        +
Android TTS / Bluetooth headset
```

This gives you control over the trip and allows the agent to change destinations and stops.

---

## 13. Technical stack

### Required

- Kotlin
- Android Studio
- Android SDK
- Google Navigation SDK for Android
- Google Maps Platform credentials/billing as required
- Android Location APIs
- Android foreground service
- Android notification APIs
- Android Text-to-Speech
- Speech recognition
- Bluetooth audio routing
- Noise watch + companion application
- Git/GitHub

### Agent layer

Preferred options:

- reuse Orion's existing agent/tool architecture;
- LangGraph if already comfortable;
- OpenAI API or another LLM provider;
- structured tool/function calling;
- strict JSON schemas for tool arguments.

### Persistence

Initial:

- Room/SQLite for:
  - saved places;
  - trip state;
  - preferences;
  - logs.

Cloud backend is not required for v1.

---

## 14. Data model

### Trip

```text
Trip
- id
- status
- currentDestination
- stops[]
- routePreference
- startedAt
- updatedAt
```

### Stop

```text
Stop
- id
- name
- latitude
- longitude
- placeId
- order
- type
```

### NavigationState

```text
NavigationState
- currentLocation
- currentRoad
- nextManeuver
- nextManeuverDistance
- nextRoad
- upcomingManeuvers[]
- destination
- eta
- remainingDistance
- routeStatus
```

---

## 15. Notification strategy

### Critical

Always notify:

- immediate turn;
- major fork;
- flyover/underpass decision;
- arrival;
- rerouting;
- route failure.

### Medium

Notify:

- upcoming turn;
- waypoint arrival.

### Low

Avoid watch notifications for:

- minor route updates;
- continuous distance changes;
- every small GPS update.

The watch should be useful, not noisy.

---

## 16. Voice UX

Preferred command vocabulary:

```text
"What's next?"
"Repeat."
"Where am I going?"
"How far?"
"How long?"
"Should I take the flyover?"
"Change destination to <place>."
"Add a stop at <place>."
"Remove <place>."
"Remove the next stop."
"Skip this stop."
"Avoid tolls."
"Avoid highways."
"Recalculate."
"Stop navigation."
```

The system should support natural variations rather than requiring exact phrases.

---

## 17. Error handling

### Destination not found

> “I couldn't find that place. Say the name again.”

### Multiple matches

> “I found two places with that name. Which one?”

### No route

> “I couldn't find a route.”

### Voice not understood

> “I didn't catch that.”

### Unsupported request

> “I can avoid highways, but I can't guarantee a specific road.”

### Navigation disconnected

> “Navigation has stopped. Please check the connection.”

---

## 18. MVP definition

### MVP-0 — Notification proof

Goal:

**Can the Noise watch reliably display Navigator notifications?**

Build:

- Android app;
- one button;
- custom notification;
- test on Noise watch.

Success:

> `TURN LEFT — 200 m`

appears correctly.

---

### MVP-1 — Passive navigation bridge

Build:

- GMapsParser;
- NotificationListenerService;
- navigation-event parsing;
- compact notification;
- Noise watch output;
- TTS.

Success:

Start Google Maps navigation and receive useful turn instructions on the watch and headset.

---

### MVP-2 — Controlled navigation

Replace passive Google Maps parsing with Google Navigation SDK.

Build:

- destination;
- route;
- turn-by-turn;
- navigation event listener;
- rerouting;
- audio guidance;
- basic navigation UI.

Success:

Navigator itself owns the navigation session.

---

### MVP-3 — Trip manipulation

Build tools:

- change destination;
- add stop;
- remove stop;
- reorder stop;
- clear stops;
- route preferences.

Success:

All operations can be performed from the app UI before adding voice.

---

### MVP-4 — Agent

Add:

- speech input;
- LLM;
- structured tool calling;
- navigation context;
- concise response generation.

Success:

Rider can say:

> “Add a stop at Orion Mall.”

and the actual route changes.

---

### MVP-5 — Rider intelligence

Add:

- “What's next?”
- “Repeat.”
- “Should I take the flyover?”
- contextual route explanations;
- smarter ambiguity handling;
- route preference commands;
- safe confirmation policy.

---

## 19. Acceptance criteria for v1

Navigator v1 is successful if:

1. A destination can be started without typing.
2. Navigation continues when the phone is not being actively viewed.
3. The Noise watch receives important navigation notifications.
4. Earphones provide concise turn instructions.
5. “What's next?” gives the correct next maneuver.
6. “Repeat” repeats the current instruction.
7. Destination can be changed by voice.
8. Stops can be added/removed by voice.
9. Route recalculates after trip changes.
10. Off-route behavior works.
11. The app does not depend on the Noise watch having GPS.
12. The agent cannot directly fabricate route decisions.
13. Navigation can be simulated/tested without physically riding.

---

## 20. Success metrics

For personal use:

- >95% correct interpretation of common navigation commands in testing.
- >99% correct deterministic navigation instruction delivery in stable GPS/network conditions.
- Important maneuvers visible on watch with minimal delay.
- Spoken instructions short enough to understand while riding.
- No unnecessary notification spam.
- Route modifications execute correctly.
- No need to touch the phone while moving.

---

## 21. Risks

### Google SDK cost/licensing

Navigation SDK usage requires checking current Google Maps Platform pricing, billing and terms before public or extended use.

### Noise compatibility

Notification mirroring varies by model and companion app.

### Voice recognition

Wind/helmet/traffic noise can reduce recognition accuracy.

### AI hallucination

Mitigation: LLM only calls structured tools; navigation SDK remains authoritative.

### Battery

Mitigation:

- foreground service only during active navigation;
- efficient event-driven updates;
- avoid unnecessary polling;
- battery-optimization testing.

### Network dependency

The exact routing/navigation behavior depends on the selected SDK and available connectivity.

---

## 22. Final recommendation

Build **Navigator as an Android-first application**, not as a watch application.

Use existing projects to accelerate the first prototype, but do not make their notification-parsing architecture the final product.

The most valuable part to build yourself is:

**voice → agent → deterministic navigation tools → route change → concise watch/audio output.**

That is the part that turns this from “Google Maps notifications on a watch” into a genuinely useful AI riding assistant.
