# 007: Android Pet Face and Senses

**Status:** In Progress
**Created:** 2026-02-22
**Related projects:** 006_zeroclaw-foreground-service (APK foundation), 004_pet-agent-personality (SOUL/skills design)

---

## Initial Prompt

> let's call this phase done and move to the next phase where we give snowy a face on the android device and a way to access camera/microphone/speakers/etc

## Background

Phase 2 (project 006) got Snowy running as an always-on foreground service with boot survival and a web dashboard. But the phone just sits there showing a notification — Snowy has a brain but no face, no eyes, no ears, and no voice. This phase gives Snowy a body on the phone.

ZeroClaw already defines skill interfaces for expression (`show_face.py --state <emotion>`) and vision (`look.sh` / `describe.sh`), but these are shell scripts designed for Termux. On Android, we need native implementations backed by Camera2, Compose Canvas, MediaRecorder, and TextToSpeech APIs.

## Context

A robot pet that can't see you, react to you, or make sounds isn't much of a pet. The Motorola has a screen, front camera, microphone, and speaker — all the hardware Snowy needs. This phase bridges ZeroClaw's agent brain to Android's hardware APIs.

### What ZeroClaw Already Provides
- **Multimodal vision**: Accepts images (up to 4 per request, 5MB each), sends to Claude for description
- **Transcription**: Audio → text via Groq Whisper API (needs GROQ_API_KEY)
- **Memory**: Remembers people, events, emotional context
- **Skills system**: Shell-command-based skill execution
- **WebSocket gateway**: `/ws/chat` for real-time agent communication
- **REST API**: `/api/*` for tools, memory, config, status

### What the Android App Must Provide
- **Face rendering** — Animated puppy face on screen (eyes, mouth, ears, tail)
- **Camera capture** — Front camera images for ZeroClaw's vision
- **Microphone** — Audio recording for transcription
- **Speaker/TTS** — Voice output for Snowy's responses
- **Hardware bridge** — Local HTTP server so ZeroClaw's skills can trigger hardware actions

## Approach

### Architecture: Hardware Bridge Pattern

The Android app runs a local HTTP server (port 42618) that ZeroClaw's skills call. This keeps ZeroClaw's skill system working as designed (shell commands that make HTTP calls) while the Android app handles actual hardware.

```
┌─────────────────────────────────────────────────┐
│                 ANDROID APP                      │
│                                                  │
│  ┌──────────────┐    ┌───────────────────────┐  │
│  │  Face UI      │    │  Hardware Bridge      │  │
│  │  (Compose)    │◄───│  HTTP :42618          │  │
│  │               │    │                       │  │
│  │  Eyes, mouth, │    │  POST /face/show      │  │
│  │  ears, tail   │    │  POST /camera/capture │  │
│  │  animations   │    │  POST /audio/record   │  │
│  └──────────────┘    │  POST /tts/speak      │  │
│                       │  GET  /status         │  │
│                       └───────────┬───────────┘  │
│                                   │              │
│  ┌────────────────────────────────┴──────────┐  │
│  │  Android Hardware APIs                     │  │
│  │  Camera2 · MediaRecorder · TextToSpeech   │  │
│  └────────────────────────────────────────────┘  │
└──────────────────────┬──────────────────────────┘
                       │ localhost
┌──────────────────────┴──────────────────────────┐
│  ZeroClaw Daemon (:42617)                        │
│                                                  │
│  Skills execute:                                 │
│    curl -X POST localhost:42618/face/show        │
│         -d '{"state":"happy"}'                   │
│    curl -X POST localhost:42618/camera/capture   │
│         → returns base64 image                   │
└──────────────────────────────────────────────────┘
```

### Phase 3a: Face Rendering (MVP)

**Full-screen Compose Canvas face** replacing the current text-only MainActivity.

**Emotion states** (from SOUL.md):
- happy, ecstatic, curious, playful, content, sleepy, lonely, confused, alert

**Face components:**
- **Eyes** — Most expressive. Wide/narrow/closed, pupil position, sparkle effect. Blink animation.
- **Mouth** — Simple curves: smile, neutral, frown, open (excited/surprised)
- **Ears** — Floppy. Perked (alert), drooped (sad/sleepy), one-up-one-down (curious)
- **Tail** — Wag speed reflects energy: fast (ecstatic), slow (content), tucked (confused/lonely)
- **Background color** — Subtle tint matching mood (warm for happy, cool for sleepy)

**Idle animations:**
- Blink every 3-8 seconds (random interval)
- Occasional glance left/right
- Breathing animation (subtle body scale pulse)
- Tail idle sway

**Bridge endpoint:**
```
POST /face/show
Body: {"state": "happy"}
Response: {"ok": true}
```

### Phase 3b: Camera/Vision

**Front camera capture** for Snowy to "look" at the family.

- Use Camera2 API or CameraX for image capture
- Capture to JPEG, return as base64 via bridge endpoint
- ZeroClaw's vision skill calls the bridge, receives image, sends to Claude vision API

**Bridge endpoint:**
```
POST /camera/capture
Body: {"camera": "front"}  // "front" or "rear"
Response: {"image": "<base64 jpeg>", "width": 1280, "height": 720}
```

**Periodic look mode:** Configurable timer (every 30-60s) auto-captures and feeds to ZeroClaw so Snowy can react to what it sees without explicit prompting.

### Phase 3c: Microphone/Audio

- MediaRecorder captures audio to OGG/WAV file
- Bridge endpoint returns audio as base64
- ZeroClaw sends to Groq Whisper for transcription
- Requires GROQ_API_KEY in config.toml

**Bridge endpoint:**
```
POST /audio/record
Body: {"duration_secs": 5}
Response: {"audio": "<base64 ogg>", "format": "ogg"}
```

**Ambient listen mode:** Periodically record short clips, transcribe, feed to ZeroClaw. Snowy hears the family talking.

### Phase 3d: Speaker/TTS

- Android's built-in TextToSpeech API for voice output
- Alternatively, play pre-recorded sound effects (bark, whimper, purr, yip)
- ZeroClaw response text → TTS → speaker

**Bridge endpoint:**
```
POST /tts/speak
Body: {"text": "Woof woof!", "pitch": 1.5, "speed": 1.0}
Response: {"ok": true, "duration_ms": 1200}
```

### Phase 3e: Skill Script Updates

Replace the Termux-based skill scripts with bridge-calling versions:

**look.sh** (new):
```bash
#!/bin/sh
curl -s -X POST http://127.0.0.1:42618/camera/capture \
  -H "Content-Type: application/json" \
  -d '{"camera":"front"}' | jq -r '.image' | base64 -d > /tmp/snowy-look.jpg
```

**show_face.py** → **show_face.sh** (new):
```bash
#!/bin/sh
curl -s -X POST http://127.0.0.1:42618/face/show \
  -H "Content-Type: application/json" \
  -d "{\"state\":\"$1\"}"
```

### Implementation Order

1. **Face UI** — Most visible impact. Replace text-only MainActivity with full-screen animated face.
2. **Hardware bridge server** — NanoHTTPD or Ktor embedded server in the Android app.
3. **Camera integration** — Connect front camera to bridge endpoint.
4. **Update ZeroClaw skills** — Point scripts at bridge instead of Termux.
5. **TTS** — Add voice output.
6. **Microphone** — Add ambient listening (requires GROQ_API_KEY).

### Tech Choices

| Component | Library | Why |
|-----------|---------|-----|
| Face rendering | Jetpack Compose Canvas | Modern, declarative, good animation support |
| HTTP bridge | NanoHTTPD | Tiny (1 file), zero dependencies, perfect for local-only server |
| Camera | CameraX | Simpler than Camera2, handles lifecycle well |
| TTS | android.speech.tts.TextToSpeech | Built-in, no external API needed |
| Audio capture | MediaRecorder | Built-in, outputs to OGG/AMR |
| Animations | Compose `Animatable` + `InfiniteTransition` | Smooth, composable, coroutine-based |

### Future: Video Streaming

The bridge architecture supports future video streaming over localhost (zero network cost):

- `GET /camera/stream` → MJPEG stream (HTTP multipart/x-mixed-replace)
- Android app streams at full framerate internally
- **Smart frame selection** on the app side avoids flooding the LLM:
  - ML Kit face detection runs locally (~15ms/frame on Snapdragon 680)
  - Motion detection triggers: "someone walked in", "face appeared"
  - Only "interesting" frames get sent to ZeroClaw → Claude
  - Idle: snapshot every 30-60s. Active (person detected): every 5-10s
- This is strictly better than dumb polling — cheap local processing gates expensive LLM calls

### Permissions Needed (new)

- `CAMERA` — front camera access
- `RECORD_AUDIO` — microphone access

## Files Changed

```
# Modified:
snowy-android/app/build.gradle.kts              # Add Compose, CameraX, NanoHTTPD deps
snowy-android/app/src/main/AndroidManifest.xml   # Add CAMERA, RECORD_AUDIO permissions

# New:
snowy-android/app/src/main/java/com/snowy/pet/
  ui/
    PetFaceScreen.kt          # Full-screen Compose face
    FaceState.kt              # Emotion → face parameters mapping
    FaceAnimations.kt         # Idle animations, transitions
    FaceComponents.kt         # Eye, mouth, ear, tail composables
  bridge/
    HardwareBridge.kt         # NanoHTTPD server on :42618
    CameraManager.kt          # CameraX capture logic
    AudioManager.kt           # MediaRecorder wrapper
    TtsManager.kt             # TextToSpeech wrapper

# Updated skill scripts:
android/skills/vision/look.sh       # Bridge-based camera capture
android/skills/vision/describe.sh   # Unchanged (already calls API)
android/skills/expression/show_face.sh  # Bridge-based face control
```

## Resolved Questions

1. **Compose vs View Canvas** — Switch to Compose entirely. The current app is trivial (one TextView), no cost to migrate. ComponentActivity replaces AppCompatActivity.
2. **Face design** — Cartoon/emoji-style. Bold, expressive, reads well on the 720p Motorola screen. Think Tamagotchi-meets-Pixar.
3. **NanoHTTPD vs Ktor** — NanoHTTPD. It's a single Java file, zero deps, perfect for a local-only bridge. No need for Ktor's weight.
4. **Ambient sensing frequency** — Configurable, default 30s for camera when idle, 5s when person detected. Mic: skip for now (Phase 3c deferred until GROQ_API_KEY available).
5. **GROQ_API_KEY** — Skip mic/audio for this implementation. Focus on face + camera + TTS. Audio/mic becomes Phase 3c later.
6. **Wake word** — Deferred with mic. When we add it, always-listen with short clips is simpler than wake word detection.

## Discussion Log

- **User:** Requested Phase 3 — face, camera, microphone, speakers for Snowy on Android.
- **Claude:** Explored ZeroClaw codebase for existing capabilities. Key findings: ZeroClaw has multimodal vision support (accepts images, sends to Claude), transcription via Groq Whisper, skill system with shell commands. Missing: actual camera/mic/screen access — that's the Android app's job. Proposed Hardware Bridge architecture: Android app runs local HTTP server (:42618) that ZeroClaw skills call for hardware access. Phased approach: face first (most visible), then camera, then TTS, then mic.
- **User:** Asked about future video streaming compatibility. Confirmed localhost makes this trivial.
- **Claude:** Added video streaming section — smart frame selection with local ML Kit face detection gating expensive LLM calls.
- **User:** Approved plan, resolved open questions. Moving to implementation.
- **Decision:** Compose for UI, cartoon face style, NanoHTTPD for bridge, skip mic/audio for now. Implementation scope: face + camera + TTS + bridge + skill updates.
