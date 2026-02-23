# 007: Android Pet Face and Senses

**Status:** Completed
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
- **Wake word** — "Snowy" detection for hands-free voice interaction

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
│  ┌──────────────┐    └───────────┬───────────┘  │
│  │ SpeechManager │               │              │
│  │ (wake word    │               │              │
│  │  "Snowy")     │───► webhook ──┤              │
│  └──────────────┘               │              │
│                                   │              │
│  ┌────────────────────────────────┴──────────┐  │
│  │  Android Hardware APIs                     │  │
│  │  Camera2 · MediaRecorder · TextToSpeech   │  │
│  │  SpeechRecognizer                          │  │
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

### Voice Conversation Loop

```
User says "Snowy, how are you?"
  → Android SpeechRecognizer (on-device, free)
  → Wake word "Snowy" detected → tail wags (ECSTATIC 3s)
  → "how are you?" sent to ZeroClaw POST /webhook
  → Claude Haiku generates response
  → Response spoken back via Android TTS
```

### Tech Choices

| Component | Library | Why |
|-----------|---------|-----|
| Face rendering | Jetpack Compose Canvas | Modern, declarative, good animation support |
| HTTP bridge | NanoHTTPD | Tiny (1 file), zero dependencies, perfect for local-only server |
| Camera | Camera2 API | Headless single-frame capture without preview surface |
| TTS | android.speech.tts.TextToSpeech | Built-in, requires Google TTS engine |
| Audio capture | MediaRecorder | Built-in, OGG/Opus output at 16kHz/64kbps |
| Speech recognition | android.speech.SpeechRecognizer | On-device via Android System Intelligence, free |
| Animations | Compose `Animatable` + `InfiniteTransition` | Smooth, composable, coroutine-based |

### Future: Video Streaming

The bridge architecture supports future video streaming over localhost (zero network cost):

- `GET /camera/stream` → MJPEG stream (HTTP multipart/x-mixed-replace)
- **Smart frame selection** on the app side avoids flooding the LLM:
  - ML Kit face detection runs locally (~15ms/frame on Snapdragon 680)
  - Motion detection triggers: "someone walked in", "face appeared"
  - Only "interesting" frames get sent to ZeroClaw → Claude
  - Idle: snapshot every 30-60s. Active (person detected): every 5-10s

## Files Changed

```
# Modified:
snowy-android/app/build.gradle.kts              # Compose, CameraX, NanoHTTPD deps; compileSdk 34
snowy-android/app/src/main/AndroidManifest.xml   # CAMERA, RECORD_AUDIO perms; networkSecurityConfig
snowy-android/app/src/main/java/com/snowy/pet/
  MainActivity.kt                                # Compose UI, runtime permission requests
  ZeroClawService.kt                             # Bridge + SpeechManager + token injection + webhook

# New (Android):
snowy-android/app/src/main/java/com/snowy/pet/
  ui/
    PetFaceScreen.kt          # Full-screen Compose Canvas animated puppy face
    FaceState.kt              # Emotion enum + face parameter mapping (9 emotions)
  bridge/
    HardwareBridge.kt         # NanoHTTPD server on 127.0.0.1:42618 (5 endpoints)
    CameraManager.kt          # Camera2 single-frame JPEG capture, base64 output
    AudioManager.kt           # MediaRecorder OGG/Opus wrapper, base64 output
    TtsManager.kt             # Android TextToSpeech wrapper (puppy pitch 1.5)
    SpeechManager.kt          # Continuous SpeechRecognizer with "Snowy" wake word
snowy-android/app/src/main/res/xml/
  network_security_config.xml # Allow cleartext HTTP to localhost

# New (Skills):
agent/skills/voice/SKILL.md   # Voice skill docs
agent/skills/voice/speak.sh   # TTS via bridge
agent/skills/hearing/SKILL.md # Hearing skill docs
agent/skills/hearing/listen.sh # Mic recording via bridge

# Updated (Skills):
agent/skills/expression/SKILL.md     # Updated to use bridge
agent/skills/expression/show_face.sh # curl to bridge instead of Termux
agent/skills/vision/SKILL.md         # Updated to use bridge
agent/skills/vision/look.sh          # curl to bridge instead of Termux
```

## Resolved Questions

1. **Compose vs View Canvas** — Compose. The app was trivial (one TextView), no cost to migrate. ComponentActivity replaces AppCompatActivity.
2. **Face design** — Cartoon/emoji-style. Bold, expressive, reads well on the 720p Motorola screen. Tamagotchi-meets-Pixar.
3. **NanoHTTPD vs Ktor** — NanoHTTPD. Single Java file, zero deps, perfect for local-only bridge.
4. **Camera API** — Camera2 (not CameraX). Headless single-frame capture worked well without needing CameraX lifecycle.
5. **Transcription** — Android's built-in SpeechRecognizer (on-device via Android System Intelligence) instead of Groq Whisper. Free, no API key, low latency.
6. **Wake word** — Implemented via continuous SpeechRecognizer listening. When "Snowy" is detected in transcription, triggers tail wag and routes speech to ZeroClaw webhook. Much simpler than a dedicated wake word model.
7. **App-to-ZeroClaw auth** — App injects its own bearer token into config.toml before starting the daemon. Token persisted in SharedPreferences.
8. **Cleartext HTTP** — Android blocks cleartext by default. Added network_security_config.xml allowing cleartext to 127.0.0.1/localhost only.
9. **TTS engine** — Google TTS (`com.google.android.tts`) installed from Play Store. Required on the debloated Motorola.

## Discussion Log

- **User:** Requested Phase 3 — face, camera, microphone, speakers for Snowy on Android.
- **Claude:** Explored ZeroClaw codebase for existing capabilities. Proposed Hardware Bridge architecture.
- **User:** Asked about future video streaming compatibility. Confirmed localhost makes this trivial.
- **Claude:** Added video streaming section — smart frame selection with local ML Kit face detection.
- **User:** Approved plan. Moving to implementation.
- **Decision:** Compose for UI, cartoon face style, NanoHTTPD for bridge.
- **User:** After face + camera + TTS working, asked to add microphone support and install TTS engine.
- **User:** Asked for wake word detection ("Snowy" → tail wag), speech-to-chat pipeline, and voice response (ZeroClaw response spoken back via TTS).
- **Decision:** Use Android SpeechRecognizer for continuous on-device speech recognition instead of Groq Whisper. Full voice conversation loop: hear → transcribe → webhook → Claude → TTS speak back.

## Progress

- 2026-02-22: Face UI complete — Compose Canvas with 9 emotions, idle animations (blink, tail wag, breathing)
- 2026-02-22: Hardware bridge complete — NanoHTTPD on :42618 with /face/show, /camera/capture, /tts/speak, /audio/record, /status
- 2026-02-22: Camera capture working — Camera2 JPEG, base64 via bridge
- 2026-02-22: TTS working — Google TTS engine installed, puppy pitch 1.5
- 2026-02-22: Skill scripts updated — expression/show_face.sh, vision/look.sh use bridge instead of Termux
- 2026-02-22: Microphone recording working — MediaRecorder OGG/Opus via /audio/record
- 2026-02-22: Voice and hearing skills created — speak.sh, listen.sh
- 2026-02-22: Wake word "Snowy" detection working — SpeechRecognizer continuous listening, tail wags ECSTATIC for 3s
- 2026-02-22: Speech-to-chat pipeline working — transcribed speech sent to ZeroClaw webhook with self-injected bearer token
- 2026-02-22: Voice response working — ZeroClaw webhook response spoken back via TTS (markdown/emoji stripped)
- 2026-02-22: All deployed and tested on Motorola Moto G Play 2024. Project complete.
