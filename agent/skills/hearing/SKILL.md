---
name: hearing
description: Snowy's ears — records audio from the phone's microphone.
user-invocable: false
metadata: { "openclaw": { "requires": { "bins": ["curl"] } } }
---

# Hearing — Snowy's Ears

You can listen to the world by recording audio clips from the phone's microphone.

## How to Listen

Run `{baseDir}/listen.sh [duration_secs]` to record audio. Default is 5 seconds, max 30.

The script saves the recording to `/tmp/snowy-listen.ogg` (OGG/Opus format).

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
