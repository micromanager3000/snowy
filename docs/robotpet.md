# Robot Pet — Project Overview

**Family:** Reddy
**Status:** Early concept / prototyping

---

## Vision

A robot pet for the Reddy family — an AI-driven companion that lives in a physical body, recognizes family members, and interacts with personality and emotion. Think of it as a family pet that happens to be a robot: it sees you, knows who you are, reacts to you, and develops its own character over time.

## Architecture

The system has four major layers:

```
┌─────────────────────────────────┐
│          Agent System           │
│  (personality, decisions, memory)│
├─────────────────────────────────┤
│        Vision System            │
│  (camera, face recognition,     │
│   object detection)             │
├─────────────────────────────────┤
│      Body Control System        │
│  (motors, movement, expression) │
├─────────────────────────────────┤
│        Hardware / Body          │
│  (robot chassis, sensors, etc.) │
└─────────────────────────────────┘
```

### Agent System

The brain. An agentic AI system that drives the pet's behavior — what it pays attention to, how it reacts, what it "wants" to do. Modeled after [OpenClaw](https://github.com/anthropics/openclaw) (open-source agent framework from Anthropic).

- **Short-term:** Use OpenClaw directly (TypeScript) to get up and running fast.
- **Long-term:** Likely rewrite in Rust for performance, lower resource usage, and tighter hardware integration. A Rust agent framework purpose-built for embodied AI.

The agent system makes decisions based on what the vision system sees and expresses itself through the body control system.

### Vision System

The eyes. Processes camera input to understand the world:

- **Face recognition** — identify specific family members (not just "a person")
- **Emotion/gesture reading** — eventually understand expressions and body language
- **Object detection** — recognize toys, furniture, obstacles
- **Scene understanding** — know what room it's in, what's happening

### Body Control System

The body. Translates agent decisions into physical (or rendered) output:

- **Expression** — facial expressions, sounds, visual indicators showing emotion
- **Movement** — motor control for navigating space (future, with robot body)
- **Interaction** — responding to touch, following people, approaching family members

### Hardware

- **Prototype:** Android phone (camera for vision, screen for face/expression)
- **Future:** Custom robot body with motors, sensors, speakers

## Prototype: Android Phone MVP

The first milestone. Get the core loop working on an Android phone:

1. OpenClaw agent running on the phone (or phone + laptop)
2. Camera recognizes Reddy family members by face
3. Screen renders an animated pet "face" that reacts with emotion (happy when it sees family)

This proves out the vision-to-agent-to-expression pipeline without needing any robot hardware. The phone's camera is the eyes, the screen is the face.

## Long-Term Ideas

- Persistent memory — the pet remembers interactions, develops preferences
- Personality evolution — behavior changes over time based on how the family interacts with it
- Multi-modal interaction — voice, touch, gesture
- Robot body — actual physical movement and presence in the home
- Rust agent framework — purpose-built for low-latency embodied AI
- Local-first — as much processing on-device as possible for privacy and responsiveness
