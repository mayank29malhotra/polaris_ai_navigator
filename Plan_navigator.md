# Navigator — Implementation Plan

## 1. End-state

Build an Android motorcycle navigation assistant that:

- uses the phone's GPS/navigation stack;
- sends concise navigation events to a Noise smartwatch as notifications;
- speaks instructions through Bluetooth earphones;
- accepts short voice commands;
- uses an agent to translate those commands into deterministic navigation tools;
- supports destination and waypoint editing during an active trip.

Target interaction:

```text
Rider:
"Navigator, take me home."

Navigator:
"Starting navigation home."

Rider:
"Add a stop at Orion Mall."

Navigator:
"Added Orion Mall."

Rider:
"What's next?"

Navigator:
"Take the flyover in 300 metres."

Rider:
"Should I take it?"

Navigator:
"Yes, take the flyover."

Rider:
"Actually, change destination to office."

Navigator:
"Destination changed."
```

---

# 2. Development strategy

Do not attempt the entire product at once.

Build in this order:

```text
Noise notification
      ↓
Navigation data
      ↓
Voice output
      ↓
Controlled navigation
      ↓
Trip editing
      ↓
Voice commands
      ↓
Agent
      ↓
Context-aware navigation assistant
```

This order minimizes risk.

---

# 3. Phase 0 — Environment setup

## Tasks

- Install Android Studio.
- Create Kotlin Android project.
- Set minimum Android version based on current SDK requirements.
- Set up Git repository.
- Create development and release build variants if useful.
- Configure logging.
- Establish package/module structure.

Suggested modules:

```text
app/
navigation/
agent/
voice/
notifications/
data/
```

For the first prototype, these can remain packages inside one module to avoid premature complexity.

## Deliverable

Blank Navigator app that installs and runs on the phone.

---

# 4. Phase 1 — Prove Noise notification delivery

## Goal

Do not touch navigation yet.

Create a single button:

```text
TEST WATCH
```

When pressed:

```text
NAVIGATOR
TURN LEFT
200 m
```

is posted as an Android notification.

## Test

- Install on phone.
- Enable Navigator notifications.
- Enable Navigator notifications inside the Noise companion app.
- Confirm notification reaches watch.
- Check truncation.
- Check vibration.
- Check whether title/body formatting survives.

## Important

Test the actual Noise model you own.

Do not assume every Noise watch exposes the same notification behavior.

## Deliverable

Reliable:

```text
Android notification → Noise watch
```

---

# 5. Phase 2 — Passive Google Maps bridge

## Goal

Get useful navigation information quickly without implementing navigation.

Study and potentially reuse:

### GMapsParser

https://github.com/3v1n0/GMapsParser

Use it to understand:

- NotificationListenerService;
- Google Maps navigation notification parsing;
- structured navigation events;
- distance/direction extraction.

### MiBandNavigator

https://github.com/satvikpandurangi/MiBandNavigator

Use it to understand:

- background notification processing;
- compact wearable instructions;
- notification forwarding;
- battery considerations.

### Pebble Maps Nav

https://github.com/konsumer/pebble-map-android

Use as an additional reference for:

- converting navigation information to wearable output.

## Implementation

```text
Google Maps
     ↓
NotificationListenerService
     ↓
GMapsParser/reference logic
     ↓
Navigator event model
     ↓
NotificationFormatter
     ↓
Noise watch
```

## Deliverable

Start a route in Google Maps and see:

```text
↰ LEFT
200 m
```

on the Noise watch.

---

# 6. Phase 3 — Add TTS

Once navigation events are available:

```text
NavigationEvent
      ↓
InstructionFormatter
      ↓
"Turn left in 200 metres."
      ↓
Android TTS
      ↓
Bluetooth headset
```

## Commands/features

At this stage, no AI.

Implement:

- current instruction;
- repeat;
- concise voice messages.

## Deliverable

A complete passive navigation experience:

```text
Google Maps
    ↓
Navigator
   ↙ ↘
watch headset
```

---

# 7. Phase 4 — Build controlled navigation

This is the architectural transition.

Move away from depending on another app's navigation notifications.

Use:

**Google Maps Platform Navigation SDK for Android**

Official documentation:

https://developers.google.com/maps/documentation/navigation/android-sdk

The Navigation SDK provides a `Navigator` that controls navigation sessions, including destination and multi-destination routes.

## Learn

Focus only on:

1. SDK setup.
2. `NavigationApi`.
3. `Navigator`.
4. `Waypoint`.
5. `setDestination()`.
6. `setDestinations()`.
7. `startGuidance()`.
8. navigation listeners.
9. arrival events.
10. route status.
11. route simulation.
12. routing options.

## Deliverable

Navigator can:

```text
Set destination
      ↓
Calculate route
      ↓
Start guidance
      ↓
Receive maneuver events
```

---

# 8. Phase 5 — Create your internal navigation state

Do not let the agent directly manipulate the SDK everywhere.

Create one central state object.

Example:

```text
TripState

destination
stops[]
currentLocation
currentRoad
nextManeuver
nextManeuverDistance
upcomingManeuvers[]
eta
remainingDistance
routePreference
navigationStatus
```

Every part of the application reads from this state.

```text
Navigation SDK
      ↓
NavigationState
 ↙      ↓       ↘
Agent   TTS     Watch
```

This is critical.

---

# 9. Phase 6 — Build navigation tools WITHOUT AI

Before adding an LLM, create buttons in the app.

Example:

```text
[ Set Destination ]

[ Add Stop ]

[ Remove Next Stop ]

[ Repeat ]

[ Recalculate ]

[ Avoid Tolls ]
```

Implement the actual tool functions:

```text
set_destination()
add_stop()
remove_stop()
reorder_stop()
clear_stops()
repeat_instruction()
get_navigation_state()
get_next_maneuver()
recalculate_route()
set_route_preference()
```

## Why?

You want to prove:

```text
tool → navigation SDK
```

works before introducing AI.

## Deliverable

Everything works manually.

---

# 10. Phase 7 — Trip/waypoint management

Create a local trip model.

Example:

```text
HOME
  ↓
GYM
  ↓
ORION MALL
  ↓
OFFICE
```

The agent can later modify this list.

Implement:

### Add

```text
add_stop(ORION_MALL)
```

### Remove

```text
remove_stop(ORION_MALL)
```

### Replace

```text
change_destination(OFFICE)
```

### Reorder

```text
move_stop(ORION_MALL, before=GYM)
```

Then convert the list into the navigation SDK's waypoint list.

Google currently supports multiple destinations/waypoints in the Navigation SDK, with a documented maximum of 25 including the final destination.

---

# 11. Phase 8 — Voice input

Start without AI.

Use speech recognition to turn:

> “Add a stop at Orion Mall.”

into text.

Log the result.

Do not immediately send every utterance to an LLM.

First create a basic command classifier for obvious commands:

```text
"repeat"
"what's next"
"stop navigation"
```

This gives faster responses and reduces AI calls.

---

# 12. Phase 9 — Add agent/tool calling

Now introduce the LLM.

Architecture:

```text
Speech
  ↓
Transcript
  ↓
Agent
  ↓
Tool selection
  ↓
Tool execution
  ↓
Navigation SDK
  ↓
Updated TripState
  ↓
Response
  ↓
TTS + Watch
```

Example:

```text
User:
"Add a stop at Orion Mall."
```

Agent:

```text
resolve_location("Orion Mall")
```

Then:

```text
add_stop(location)
```

Then:

```text
Navigator.setDestinations(...)
```

Then:

> “Added Orion Mall.”

---

# 13. Phase 10 — Navigation context for the agent

The agent needs a compact structured context.

Example:

```json
{
  "current_road": "Outer Ring Road",
  "destination": "Office",
  "next_maneuver": "TAKE_FLYOVER",
  "next_maneuver_distance_m": 280,
  "next_road": "Hebbal Flyover",
  "upcoming": [
    {
      "maneuver": "TAKE_FLYOVER",
      "distance_m": 280
    },
    {
      "maneuver": "KEEP_RIGHT",
      "distance_m": 1200
    }
  ]
}
```

Then:

> “Should I take the flyover?”

becomes a deterministic contextual question.

The model doesn't need a map image.

---

# 14. Phase 11 — Riding commands

Implement in this order.

## Tier 1

```text
What's next?
Repeat.
How far?
How long?
```

## Tier 2

```text
Change destination to X.
Add a stop at X.
Remove the next stop.
Remove X.
Skip this stop.
```

## Tier 3

```text
Avoid tolls.
Avoid highways.
Recalculate.
```

## Tier 4

```text
Should I take the flyover?
Which way now?
Is this the correct turn?
```

## Tier 5

```text
Take this particular road.
Prefer this route.
Avoid this road.
```

Tier 5 should only be considered complete when the navigation engine can actually enforce or verify the requested road preference.

---

# 15. Phase 12 — Noise watch optimization

Once functionality works, optimize notification design.

Possible notification states:

### 500 m

No watch notification unless useful.

### 200 m

```text
↰ LEFT
200 m
```

### 50 m

```text
↰ LEFT
50 m
```

### Immediate

```text
↰ LEFT NOW
```

### Route change

```text
ROUTE UPDATED
```

### Destination change

```text
DESTINATION
CHANGED
```

Avoid constantly updating the notification every few seconds.

---

# 16. Phase 13 — Safety/UX

Implement:

- short TTS responses;
- no long AI explanations;
- no phone interaction required while riding;
- voice command timeout;
- command confirmation for risky/bulk changes;
- graceful failure when speech recognition fails;
- graceful failure when network is unavailable;
- explicit uncertainty for unsupported road-selection requests.

Example:

User:

> “Take the service road.”

If exact enforcement isn't possible:

> “I can try routing through the service road, but I can't guarantee it.”

Never:

> “Done.”

unless it is actually verified.

---

# 17. Phase 14 — Testing without riding

Do as much testing as possible before using it on the road.

Use navigation simulation/testing where supported by the Navigation SDK.

Test:

### Destination

- valid destination;
- invalid destination;
- ambiguous destination.

### Stops

- add;
- remove;
- reorder;
- multiple stops;
- final destination replacement.

### Navigation

- correct turn;
- missed turn;
- rerouting;
- arrival;
- route failure.

### Voice

- normal speech;
- noisy environment;
- different phrasing;
- incomplete commands.

### Agent

- correct tool;
- wrong tool;
- missing argument;
- ambiguous location;
- unsupported request.

---

# 18. Suggested repository structure

```text
Navigator/
│
├── app/
│   ├── MainActivity
│   ├── NavigationService
│   └── permissions/
│
├── navigation/
│   ├── NavigationManager
│   ├── NavigationState
│   ├── RouteManager
│   ├── ManeuverProcessor
│   └── NavigationSimulator
│
├── trip/
│   ├── Trip
│   ├── Stop
│   └── TripManager
│
├── agent/
│   ├── NavigatorAgent
│   ├── ToolRegistry
│   ├── ToolSchemas
│   └── AgentContextBuilder
│
├── tools/
│   ├── SetDestinationTool
│   ├── AddStopTool
│   ├── RemoveStopTool
│   ├── ReorderStopTool
│   ├── GetNextManeuverTool
│   ├── RepeatInstructionTool
│   └── RoutePreferenceTool
│
├── voice/
│   ├── SpeechRecognizer
│   ├── TtsManager
│   └── VoiceSession
│
├── notifications/
│   ├── NavigationNotification
│   ├── NotificationFormatter
│   └── WatchNotificationManager
│
├── data/
│   ├── RoomDatabase
│   └── SavedPlaces
│
└── util/
```

---

# 19. GitHub usage plan

## GMapsParser

Use as:

**Prototype/reference**

Study:

- notification listener;
- parsing;
- navigation event model.

Do not blindly copy it into the final architecture.

---

## MiBandNavigator

Use as:

**Wearable/notification reference**

Study:

- compact navigation alerts;
- background processing;
- wearable limitations.

Adapt the generic notification concept to Noise.

---

## Pebble Maps Nav

Use as:

**Additional wearable navigation reference**

Study:

- navigation event extraction;
- compact directional output.

---

# 20. What you should write yourself

These are the parts that make Navigator your project.

### Must build yourself

- Navigator Android app;
- Noise notification formatter;
- TripState;
- TripManager;
- navigation tool registry;
- agent integration;
- navigation context builder;
- voice command handling;
- destination editing;
- stop editing;
- route preference commands;
- “Should I take the flyover?” logic;
- response formatter;
- rider-specific UX;
- testing;
- safety rules.

### Reuse

- Google routing/navigation engine;
- Android speech/TTS infrastructure;
- existing open-source notification parsing ideas;
- wearable notification architecture ideas;
- standard Android components.

---

# 21. Recommended development order

```text
DAY 1
Android project
       ↓
Custom notification
       ↓
Noise watch test

DAY 2–3
Google Maps notification parsing
       ↓
Turn extraction
       ↓
Noise watch

DAY 3–4
TTS
       ↓
Bluetooth headset

DAY 5+
Google Navigation SDK
       ↓
Own navigation session

NEXT
TripState
       ↓
Destination
       ↓
Waypoints
       ↓
Route preferences

NEXT
Voice input
       ↓
Basic commands

NEXT
LLM
       ↓
Tool calling

NEXT
Navigation context
       ↓
"What's next?"
       ↓
"Should I take the flyover?"

FINAL
Noise optimization
       ↓
Battery optimization
       ↓
Simulation/testing
       ↓
Real-world testing
```

The exact time will depend heavily on your Android experience and Google SDK setup, so treat this as dependency order rather than a guaranteed calendar.

---

# 22. Definition of done

Navigator v1 is complete when you can mount the phone, connect your headset, wear the Noise watch and start a route without needing to look at the phone for normal navigation.

The following should work by voice:

```text
"Navigate to office."

"What's next?"

"Repeat."

"Should I take the flyover?"

"Add a stop at Orion Mall."

"Remove the next stop."

"Change destination to home."

"Avoid tolls."

"Stop navigation."
```

The watch should provide concise visual/haptic reinforcement while the headset provides the primary spoken interaction.

---

# 23. Long-term roadmap

After v1:

### v1.1
- saved places;
- favorite destinations;
- better voice recognition;
- route preferences.

### v1.2
- commute presets;
- “take my usual route”;
- automatic home/work detection if explicitly configured.

### v2
- road preference reasoning;
- route comparison;
- smarter rerouting explanations;
- traffic-aware conversational answers.

### v3
- optional direct wearable integrations if the Noise platform/model supports them;
- richer haptic patterns;
- optional handlebar controls;
- offline/fallback navigation research.

---

# 24. Most important architectural principle

**Do not build an AI that navigates. Build a navigation system that an AI can control.**

The routing engine determines:

- where the rider is;
- what road they are on;
- what the next maneuver is;
- what route is available.

The agent determines:

- what the rider's spoken request means;
- which deterministic tool to call;
- how to phrase the result.

This separation is what makes Navigator reliable and technically defensible.
