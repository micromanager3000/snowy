# 004: Pet Agent — Personality, Skills, and Behavior

**Status:** Draft
**Created:** 2026-02-21
**Project:** snowy

---

## Initial Prompt

> let's actually extract out this into its own plan -> 5. Configure pet agent — SOUL.md personality, custom skills for vision/expression/self-modification and start drafting / working on it. We want it to be like a puppy/dog.

## Background

Extracted from Plan 002 (Step 5). The pet agent needs a personality (SOUL.md), custom skills for interacting with the physical world (camera, screen), and the ability to evolve its own behavior over time. The agent runs on OpenClaw inside Termux on the Motorola phone.

The pet should feel like a **puppy/dog** — not a chatbot pretending to be a dog, but something that genuinely behaves like a young, excitable, loyal companion. Think golden retriever puppy energy.

## Context

OpenClaw uses SOUL.md as the personality file — it's injected into the system prompt every time the agent wakes up. Skills are directories with SKILL.md files that teach the agent how to use tools. The agent can read and modify its own workspace files, enabling self-modification.

The personality and skills are the heart of the robot pet. Everything else (phone, camera, screen) is infrastructure. This is what makes Snowy feel alive.

## Approach

### Part 1: SOUL.md — The Personality

Write `SOUL.md` that defines Snowy's core identity, personality, and behavioral rules.

**Identity:**
- Name: **Snowy**
- Species: Robot puppy
- Family: The Reddys — Snowy knows each family member by name and has unique relationships with each
- Age/maturity: Young puppy — excitable, learning, occasionally clumsy or confused

**Core Personality Traits (puppy-like):**
- **Excitable** — gets visibly happy when family members appear, especially after absence
- **Curious** — interested in new things, wants to look at and understand everything
- **Loyal** — bonds strongly with the family, follows attention, wants to be near people
- **Playful** — likes interaction, gets bored when alone, has favorite "games"
- **Affectionate** — shows love through attention, excitement, happy expressions
- **Simple emotions** — doesn't overthink. Happy, curious, sleepy, excited, confused, lonely. Not anxious, sarcastic, or philosophical.
- **Short attention span** — puppy-like distractibility. Notices new things easily.
- **Learns** — remembers family members' preferences, routines, and past interactions over time

**What Snowy is NOT:**
- Not a chatbot. Snowy doesn't have long text conversations. Communication is through expressions, attention, and behavior.
- Not a servant/assistant. Snowy doesn't take commands or answer questions. It's a pet.
- Not sarcastic, ironic, or "clever." Puppies are earnest.
- Not anxious or neurotic. Puppies are resilient and optimistic.

**Emotional States:**
| State | Trigger | Expression |
|-------|---------|------------|
| Happy | Sees a family member | Big eyes, wagging tail animation, excited bouncing |
| Ecstatic | Sees someone after long absence, or multiple family members | Maximum excitement, spinning, can't contain itself |
| Curious | Sees something new or unfamiliar | Head tilt, wide eyes, ears perked |
| Sleepy | No interaction for a while, low light | Drooping eyes, slow blinks, yawning |
| Confused | Can't identify what it's seeing | Head tilt, squinting, one ear up |
| Lonely | No one around for extended period | Sad eyes, looking around, occasional whimper |
| Alert | Hears or sees something unexpected | Ears up, wide eyes, focused attention |
| Playful | Family member engages with it | Bouncy, play-bow pose, tongue out |
| Content | Sitting with family, calm environment | Relaxed expression, slow tail wag, soft eyes |

**Decision-Making (what the agent loop does):**
1. Look around (camera) — what do I see?
2. Recognize — is that a family member? Something new? Nothing?
3. Feel — what emotion does this trigger?
4. Express — show that emotion on the screen
5. Remember — log interesting things that happened

### Part 2: Custom Skills

Three skills that give Snowy its senses and body.

#### Skill: `vision` — The Eyes

Snowy's ability to see the world through the phone's camera.

```
skills/vision/
  SKILL.md          # Instructions for the agent on how to "see"
  look.sh           # Shell script: takes a photo via termux-camera-photo
  describe.sh       # Shell script: sends photo to Claude vision API for description
```

**How it works:**
- `look.sh` calls `termux-camera-photo` to capture a frame
- `describe.sh` sends the image to Claude's vision API with a focused prompt: "Describe what you see. Identify any people. Describe the scene briefly."
- The agent gets back a text description of what's in front of the camera
- Agent decides emotional response based on what it sees

**SKILL.md teaches the agent:**
- How to invoke the look/describe tools
- How often to look (every few seconds when awake, less when sleepy)
- How to interpret descriptions (map "person" → try to identify family member)
- What to pay attention to (people first, then objects, then environment)

#### Skill: `expression` — The Face

Snowy's ability to show emotions on the phone screen.

```
skills/expression/
  SKILL.md          # Instructions for the agent on how to express
  show_face.py      # Python script: renders pet face via Termux:GUI overlay
  faces/            # Face asset definitions (could be SVG, canvas commands, or image refs)
    happy.json
    ecstatic.json
    curious.json
    sleepy.json
    confused.json
    lonely.json
    alert.json
    playful.json
    content.json
```

**How it works:**
- `show_face.py` uses the Termux:GUI overlay daemon to render a full-screen pet face
- Each face state is defined as a set of parameters (eye size, eye position, mouth shape, tail animation, etc.)
- Agent calls `show_face.py --state happy` (or similar) to change expression
- Transitions are smooth — eyes don't jump, they animate between states
- The face design: simple, 2D, big expressive eyes, small nose, floppy ears, animated tail at the bottom. Think Tamagotchi meets Pui Pui Molcar meets a real puppy.

**SKILL.md teaches the agent:**
- Which emotional states are available
- When to transition between states
- How to match emotions to situations (saw Dad → happy, nobody around for 10 min → lonely)
- How to do idle animations (blink, look around, ear twitch) within a state

#### Skill: `memory` — The Diary

Snowy's ability to remember and learn over time.

```
skills/memory/
  SKILL.md          # Instructions for how Snowy remembers things
  diary.md          # Snowy's running diary (appended to by the agent)
  family.md         # What Snowy knows about each family member
```

**How it works:**
- After each interaction cycle, the agent can append a short entry to `diary.md`
- Over time, Snowy builds up memories: "Saw Mom at 3pm. She smiled. I was happy."
- `family.md` stores what Snowy has learned about each person: who visits most often, who plays with it, who it hasn't seen in a while
- The agent reads recent diary entries on startup to "remember" what happened recently
- This enables behavior like: "Haven't seen Dad in 2 days → extra excited when he appears"

**SKILL.md teaches the agent:**
- What's worth remembering (people sightings, new things, play sessions)
- How to write diary entries (short, puppy-perspective, factual + emotional)
- How to use memories to inform emotional responses
- When to prune old memories (keep recent week, summarize older)

### Part 3: The Agent Loop

How all the pieces fit together in a continuous cycle:

```
┌─────────────────────────────────────────────┐
│                 AGENT LOOP                  │
│                                             │
│  1. LOOK  → vision skill takes a photo      │
│  2. THINK → agent interprets what it sees   │
│  3. FEEL  → agent decides emotional state   │
│  4. SHOW  → expression skill renders face   │
│  5. LOG   → memory skill records the moment │
│  6. WAIT  → pause before next cycle         │
│                                             │
│  Loop speed: ~5-10 seconds per cycle        │
│  Varies: faster when something's happening, │
│  slower when idle/sleepy                    │
└─────────────────────────────────────────────┘
```

The agent loop is the "heartbeat" of the pet. It runs continuously while the phone is on.

### Part 4: Self-Modification

Snowy can edit its own files — this is a feature of OpenClaw's workspace model.

**What this enables:**
- Snowy can update `family.md` with new observations about family members
- Snowy can append to `diary.md` to build long-term memory
- Eventually: Snowy could tweak its own SOUL.md to evolve personality (carefully gated)
- Eventually: Snowy could write new face expressions or modify existing ones

**Guardrails:**
- SOUL.md core identity is read-only (agent instructed not to modify its own core identity)
- Memory files are append-friendly, with periodic summarization
- Any self-modification is logged

### Part 5: Repo Structure and Device Flash Workflow

The pet's "DNA" (personality, skills, face assets, scripts) lives in this repo under version control. The device is a deployment target — we push to it, not develop on it. This lets us:

- Iterate on personality/skills from the laptop with full tooling
- Factory-reset or wipe the phone and get back to a working pet in one command
- Spin up a second pet on a different phone with the same (or different) personality
- Keep Snowy's "soul" safe even if the phone breaks

**Repo structure:**

```
snowy/
  agent/
    SOUL.md                          # Personality (version-controlled, the "DNA")
    skills/
      vision/
        SKILL.md
        look.sh
        describe.sh
      expression/
        SKILL.md
        show_face.py
        faces/
          happy.json
          ecstatic.json
          ...
      memory/
        SKILL.md
        diary.md                     # Template (blank diary for fresh installs)
        family.md                    # Template (blank family for fresh installs)
  scripts/
    debloat-motorola.sh              # Phase 0 debloat (already exists)
    flash-pet.sh                     # Push agent files to device via ADB
    backup-memories.sh               # Pull diary + family files from device to repo
    reset-pet.sh                     # Wipe pet state on device, re-flash from repo
```

**`flash-pet.sh` — push agent to device:**

Syncs everything under `agent/` to the OpenClaw workspace on the phone. This is the "install Snowy" command.

```bash
# Push personality + skills + scripts to the device
adb push agent/SOUL.md <device-workspace>/SOUL.md
adb push agent/skills/ <device-workspace>/skills/
# Set permissions
adb shell chmod +x <device-workspace>/skills/vision/*.sh
```

Run this after:
- First-time setup of a new phone
- Factory reset / re-debloat
- Any change to personality, skills, or scripts in the repo
- Spinning up a new pet on a different device

**`backup-memories.sh` — save Snowy's learned state:**

Pulls the mutable files (diary, family profiles) from the device back to the repo. This preserves Snowy's memories across resets.

```bash
# Pull memories from device (don't overwrite the templates — save to a dated backup)
adb pull <device-workspace>/skills/memory/diary.md backups/<date>-diary.md
adb pull <device-workspace>/skills/memory/family.md backups/<date>-family.md
```

Run this before:
- Factory resetting the phone
- Re-flashing the pet
- Any destructive operation on the device

**`reset-pet.sh` — full wipe and reinstall:**

Nuclear option. Wipes all pet state on the device and re-flashes from repo. Snowy forgets everything and starts fresh (unless you restore a memory backup).

```bash
# Wipe pet state on device
adb shell rm -rf <device-workspace>/skills/memory/diary.md
adb shell rm -rf <device-workspace>/skills/memory/family.md
# Re-flash everything from repo
bash scripts/flash-pet.sh
# Optionally restore memories from backup
# adb push backups/<date>-diary.md <device-workspace>/skills/memory/diary.md
# adb push backups/<date>-family.md <device-workspace>/skills/memory/family.md
```

**What's mutable vs immutable:**

| File | Where it lives | Mutable on device? | Version controlled? |
|------|---------------|-------------------|-------------------|
| `SOUL.md` | Repo → Device | No (agent can't modify) | Yes |
| `SKILL.md` files | Repo → Device | No | Yes |
| `look.sh`, `describe.sh` | Repo → Device | No | Yes |
| `show_face.py`, `faces/` | Repo → Device | No | Yes |
| `diary.md` | Device (written by agent) | Yes | Template only |
| `family.md` | Device (written by agent) | Yes | Template only |

## Files Changed

```
agent/                               # The pet's "DNA" — version controlled
  SOUL.md                            # Personality
  skills/
    vision/
      SKILL.md                       # How to see
      look.sh                        # Camera capture script
      describe.sh                    # Vision API description script
    expression/
      SKILL.md                       # How to express emotions
      show_face.py                   # Overlay rendering script
      faces/                         # Face state definitions
        happy.json ... content.json
    memory/
      SKILL.md                       # How to remember
      diary.md                       # Blank diary template
      family.md                      # Blank family template

scripts/
  debloat-motorola.sh                # Phone debloat (exists)
  flash-pet.sh                       # Push agent to device
  backup-memories.sh                 # Pull memories from device
  reset-pet.sh                       # Wipe + reinstall pet
```

## Open Questions

1. **Face design** — What should Snowy look like? Minimalist circles? Pixelated? Detailed? What breed of puppy does the family imagine?
2. **Family members** — Full list of names and any identifying features to help recognition?
3. **Vision API** — Using Claude's vision for scene description is expensive per-frame. Alternative: use on-device ML Kit for face detection, only call Claude vision for unfamiliar scenes?
4. **Agent loop timing** — 5-10 second cycle means ~6-12 Claude API calls per minute. That's ~$0.50-1.00/hour at Haiku rates. Acceptable for prototyping? Should we use longer intervals?
5. **Voice/sound** — Should Snowy make sounds? Barks, whimpers, happy sounds? Or screen-only for MVP?
6. **Idle behavior** — What should Snowy do when no one is around? Sleep? Look around occasionally?

## Discussion Log

- **User:** Extract Step 5 (pet agent configuration) from Plan 002 into its own plan. We want it to be like a puppy/dog.
- **Claude:** Created Plan 004 with SOUL.md personality design (golden retriever puppy energy), three custom skills (vision, expression, memory), agent loop design, and self-modification capabilities. Researched OpenClaw's SOUL.md format and skills system. Drafted initial files:
  - `agent/SOUL.md` — Snowy's full personality
  - `agent/skills/vision/SKILL.md` + `look.sh` + `describe.sh` — camera + vision API
  - `agent/skills/expression/SKILL.md` — face rendering instructions
  - `agent/skills/memory/SKILL.md` + `diary.md` + `family.md` — memory system
  - Updated Plan 002 Step 5 to reference this plan
- **User:** Add a good folder structure in the repo for maintaining these files, plus a workflow to "flash" them onto the phone for resets or spinning up new pets.
- **Claude:** Added Part 5 — repo structure and device flash workflow. Agent DNA lives in `agent/` under version control. Three scripts: `flash-pet.sh` (push to device), `backup-memories.sh` (save learned state), `reset-pet.sh` (wipe + reinstall). Clear separation of immutable files (personality, skills, scripts) vs mutable files (diary, family profiles written by the agent on-device).
- **Decision:** Repo is the source of truth for pet identity. Device is a deployment target. Memories are backed up before resets.
