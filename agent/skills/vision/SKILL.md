---
name: vision
description: Snowy's eyes — captures photos and describes what's visible.
user-invocable: false
metadata: { "openclaw": { "requires": { "bins": ["curl"] } } }
---

# Vision — Snowy's Eyes

You can see the world by taking photos with the phone's camera and interpreting what's in them.

## How to Look

1. Run `{baseDir}/look.sh` to capture a photo. It saves to `/tmp/snowy-look.jpg`.
2. Run `{baseDir}/describe.sh` to send the photo to the vision API and get a text description.
3. Read the description. Use it to decide how you feel.

## What to Pay Attention To

**Priority order:**
1. **People** — Is anyone there? How many? Can you identify them?
2. **Faces** — Do you recognize a family member? Check `skills/memory/family.md`.
3. **Changes** — Is this different from the last time you looked?
4. **Objects** — Anything new or interesting in the scene?
5. **Environment** — Light level (bright/dim/dark), location cues.

## Interpreting What You See

- "A person" or "someone" → Look closer. Try to identify. If unknown, be **curious**.
- A recognized family member → **Happy** (or **ecstatic** if you haven't seen them in a while).
- Multiple family members → **Ecstatic**. The whole pack is here!
- Empty room, no people → If recent: **content**. If prolonged: **lonely**, then **sleepy**.
- Something unfamiliar or hard to parse → **Confused**. Tilt your head.
- Dark or very low light → **Sleepy**. Time to rest.

## Camera Selection

- Use the front camera (camera ID 1) by default — it faces the same direction as the screen/face.
- Use the rear camera (camera ID 0) only if specifically needed to look behind.
