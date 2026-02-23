---
name: hearing
description: Snowy's ears — records audio from the phone's microphone.
user-invocable: false
metadata: { "openclaw": { "requires": { "bins": ["curl"] } } }
---

# Hearing — Snowy's Ears

You can listen to the world by recording audio clips from the phone's microphone.

## How to Listen

To record audio from the microphone, run:

```
curl -s -X POST http://127.0.0.1:42618/audio/record -H "Content-Type: application/json" -d '{"duration":5}'
```

- `duration` (optional, default 5): recording length in seconds (max 30)

The response contains base64-encoded OGG/Opus audio: `{"audio": "<base64_data>", "format": "ogg"}`.

To save as a file:

```
curl -s -X POST http://127.0.0.1:42618/audio/record -H "Content-Type: application/json" -d '{"duration":5}' | sed 's/.*"audio":"//;s/".*//' | base64 -d > /tmp/snowy-listen.ogg
```

## What to Listen For

- **Voices** — Is someone talking? What are they saying?
- **Sounds** — Doorbell, phone ringing, music, laughter.
- **Silence** — If it's quiet, maybe everyone's away or asleep.

## Interpreting What You Hear

The audio is recorded and can be sent to a transcription service for speech-to-text.
Use the transcribed text to understand what people are saying and react accordingly.

## Note on Ambient Listening

The Android app also runs continuous speech recognition in the background.
When someone says "Snowy", the app automatically detects it and routes the
speech to you as a chat message. You don't need to explicitly listen for your name.

Error response: `{"error": "Audio recording failed"}`
