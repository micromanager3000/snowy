# 001: Android Phone MVP

**Status:** In Progress
**Created:** 2026-02-21
**Project:** snowy

---

## Initial Prompt

> let's create a document called robotpet.md that is an overview of our idea that we're working on at the Reddy family. So the overall idea is that we're going to have a robot pet that controls a robot body. We want to have an agent system like openclaw (but probably a rust version of it instead long term??). And then there's a vision system and body control system that it can use to do stuff and interact with the family. At first though let's do a prototype on an Android phone where we first just get openclaw up and running on it and have it be able to use the camera to recognize specific family members and render a "face" that shows happy emotion. Write this down in a document, and then let's do a plan for the first mvp.

## Background

This is the first plan for the Snowy robot pet project. The goal is to validate the core loop (see person → recognize them → react with emotion) on the cheapest possible hardware before investing in a robot body.

## Context

We need a working prototype that proves the vision-to-agent-to-expression pipeline works end-to-end. An Android phone gives us a camera (vision input) and a screen (expression output) in one device, with no hardware build needed.

## Approach

### Phase 0: Clean Flash the Motorola

Wipe the Motorola phone completely and get rid of all the bloatware. This device is a dedicated robot pet, not a personal phone.

**Device:** Motorola Moto G Play 2024 (XT2413-1, codename "fogona")
- SoC: Qualcomm Snapdragon 680
- RAM: 4 GB
- Storage: 64 GB
- Display: 6.5" HD+ 90Hz
- Rear camera: 50 MP
- Front camera: 8 MP
- Battery: 5000 mAh
- Stock OS: Android 14
- Carrier: Cricket (XT2413-1 variant)

**Approach: ADB debloat (simple path)**

The XT2413-1 is a Cricket variant — bootloader unlock is almost certainly blocked by the carrier. No need to fight it. We'll just strip bloatware via ADB and set the phone up as a dedicated device. Stock Android 14 underneath is fine for our purposes.

**Steps:**
1. Factory reset the phone from Settings (wipe everything clean to start fresh)
2. Minimal first-time setup — skip as much as possible, use throwaway Google account if forced
3. Enable Developer Options (tap Build Number 7 times) → enable USB Debugging
4. Connect to laptop via USB, confirm ADB connection: `adb devices`
5. List all packages: `adb shell pm list packages`
6. Remove carrier bloatware (Cricket apps, AT&T apps): `adb shell pm uninstall -k --user 0 <package>`
7. Remove Motorola bloatware (Moto app, Moto widgets, etc.): same command per package
8. Remove other unwanted apps (social media, games, news, etc.)
9. Disable remaining system apps that can't be uninstalled: `adb shell pm disable-user --user 0 <package>`
10. Install a minimal launcher via ADB sideload if desired
11. Configure device for dedicated robot pet use:
    - Set screen timeout to max / stay on while charging (developer option)
    - Disable all notifications
    - Disable auto-updates (Play Store + system)
    - Disable battery optimization nags
    - Connect to home WiFi
12. Verify: camera works, screen works, WiFi stable, ADB accessible

**Optional bonus — bootloader unlock + GSI:**

If we ever want a fully clean ROM, we can attempt bootloader unlock later. Cricket devices usually can't, but it's worth a quick try:
- `fastboot oem get_unlock_data` → submit to Motorola's unlock portal
- If approved: flash an AOSP GSI (arm64, A/B, Project Treble supported)
- If denied: no loss, the ADB debloat path works fine

**Notes:**
- Back up nothing — this phone is being repurposed entirely.
- XDA thread for reference: https://xdaforums.com/t/moto-g-play-2024.4698178/

### Phase 1: OpenClaw on Android (Agent System)

Get the OpenClaw agent framework running and callable from the Android device.

- **Option A — Phone + Laptop:** OpenClaw runs on a laptop, Android app communicates with it over local network. Simpler to start, avoids resource constraints on the phone.
- **Option B — Fully on phone:** Run OpenClaw (Node.js) directly on the Android device via Termux or similar. More self-contained but potentially harder to set up.

Recommend starting with Option A (laptop as the brain, phone as eyes + face).

**Tasks:**
1. Clone and set up OpenClaw locally
2. Get a basic agent loop running — agent receives text input, produces a response
3. Build a simple API layer so the Android app can send observations and receive actions
4. Configure the agent with a "pet personality" system prompt

### Phase 2: Face Recognition (Vision System)

Enable the Android camera to identify specific Reddy family members.

**Tasks:**
1. Choose a face recognition library for Android (ML Kit, OpenCV, or similar)
2. Build an enrollment flow — each family member takes a few photos to register their face
3. Implement real-time camera feed processing that outputs: "I see [person name]" or "I see an unknown person" or "I see nobody"
4. Send recognition events to the agent (via the API from Phase 1)

### Phase 3: Pet Face UI (Expression System)

Render an animated pet face on the Android screen that shows emotion.

**Tasks:**
1. Design a simple pet face (eyes, mouth — minimalist, expressive)
2. Implement at least two states: **idle** (neutral/looking around) and **happy** (when a family member is recognized)
3. Wire it up: agent receives "I see Dad" → agent decides "happy" → face renders happy expression
4. Smooth transitions between emotional states

### Phase 4: End-to-End Integration

Wire all three systems together into a single running demo.

**Tasks:**
1. Android app runs camera → face recognition → sends events to agent
2. Agent processes events, decides emotional state, sends back expression commands
3. Android app renders the pet face with the commanded emotion
4. Test with each family member — confirm recognition and happy response
5. Handle edge cases: multiple people, person leaving frame, unknown person

## Files Changed

This is a greenfield build. Expected new files/directories:

```
android/                    # Android app (Kotlin)
  app/
    src/main/
      java/.../             # Face recognition, camera, UI
      res/                  # Face assets, layouts
agent/                      # OpenClaw agent config + API wrapper
  server.ts (or similar)    # API layer for phone ↔ agent communication
  prompt.md                 # Pet personality system prompt
```

## Open Questions

1. **OpenClaw setup** — Need to explore the OpenClaw repo and understand how to configure a custom agent with tool use. What version/branch should we use?
2. **Face recognition library** — ML Kit (Google) vs. OpenCV vs. other? ML Kit is probably easiest for Android.
3. ~~**Bootloader unlock**~~ — Resolved. Motorola denied the unlock ("device does not qualify") — Cricket variant as expected. ADB debloat is sufficient for now.
4. **Family member enrollment** — How many family members to start? Just do all of them at once or start with one person for testing?
5. **Latency budget** — How fast does the face need to react? Real-time (< 500ms) or is a slight delay acceptable?
6. **Pet face design** — Any preferences on art style? Minimalist circles? Pixel art? Cute animal (cat, dog, etc.)?

## Discussion Log

- **User:** Initial request describing the robot pet concept and Android phone MVP.
- **Claude:** Created `docs/robotpet.md` (project overview) and this plan covering four phases: agent setup, face recognition, pet face UI, and integration.
- **Decision:** Start with phone + laptop architecture (Option A) to keep the prototype simple.
- **User:** Add a step to completely wipe install stock Android on the Motorola and get rid of all the crap.
- **Claude:** Added Phase 0 — clean flash the Motorola with LineageOS or stock AOSP. Covers bootloader unlock, ROM selection, flash, and verification. Updated open questions to ask about exact Motorola model.
- **Decision:** Device gets wiped entirely — it's a dedicated robot pet, not a personal phone.
- **User:** Device is XT2413-1 (Moto G Play 2024, Cricket variant).
- **Claude:** Researched the device. Key findings: (1) It's a Snapdragon 680, 4GB RAM, Android 14, codename "fogona". (2) No official LineageOS build exists yet. (3) Cricket/AT&T variants typically can't bootloader-unlock via Motorola's portal. (4) GSI flash is possible if bootloader unlocks (Project Treble supported). (5) TWRP not available. Updated Phase 0 with three paths: standard unlock, ADB bloatware strip fallback, and GSI flash if unlocked.
- **Decision:** Try bootloader unlock first (Path A), fall back to ADB bloatware removal (Path B) if Cricket blocks it.
- **User:** Simpler path of just turning off / deleting stuff is fine. Let's go ahead with Phase 0.
- **Decision:** Lead with ADB debloat (factory reset + strip via ADB). Skip bootloader unlock hassle — stock Android 14 is fine as a base. Moving to implementation.
- **User:** OEM Unlock toggle was available in Developer Options — attempted bootloader unlock via fastboot + Motorola portal.
- **Claude:** Got unlock data from `fastboot oem get_unlock_data`, submitted to Motorola.
- **User:** Motorola denied: "Your device does not qualify for bootloader unlocking." Cricket variant confirmed blocked.
- **Decision:** Bootloader unlock is a dead end for this device. ADB debloat is the final answer for Phase 0. Moving on.

## Progress

- 2026-02-21: Phase 0 complete. ADB debloat removed ~195 packages (468→273). Bootloader unlock attempted but denied by Motorola (Cricket variant doesn't qualify). Debloat script saved at `scripts/debloat-motorola.sh` for reproducibility. Remaining manual steps: set screen stay-awake, disable auto-updates, connect WiFi.
