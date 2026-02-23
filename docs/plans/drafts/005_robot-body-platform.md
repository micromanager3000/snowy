# 005: Robot Body Platform — Petoi Bittle X V2

**Status:** Draft
**Created:** 2026-02-21
**Project:** snowy

---

## Initial Prompt

> can you create a plan for the body of the robot using this chat for reference is there something like a wheeled or ideally legged platform I could mount my android phone to and connect to it via usb and have the android control the platform?

## Background

Surveyed robotic platforms (wheeled and legged) that support Android phone control via USB OTG. Evaluated options including DFRobot Romeo, Sabertooth motor controllers, Freenove hexapod kits, LewanSoul/Hiwonder hexapods, and Petoi Bittle. Settled on **Petoi Bittle X V2** (alloy servos, pre-assembled) as the best fit due to:

- ESP32-based BiBoard with built-in Bluetooth and USB-C
- Fully open-source firmware (OpenCat) with documented serial command protocol
- Clean Android integration path via USB-CDC or Bluetooth SPP
- Active community and existing Python/serial tooling
- Support for custom gaits and walking algorithms at multiple levels (joint commands, custom skills, or firmware fork)
- Small form factor suitable for phone mounting

Key decisions from discussion:
- **USB over Bluetooth** for the phone-to-robot connection — zero latency, no pairing, phone is mounted on the robot anyway
- **Alloy servos** over lite — more durable, supports optional arm gripper
- **Pre-assembled** — skip the build, get to software faster
- Both USB and Bluetooth expose the identical OpenCat serial protocol (115200 baud, ASCII commands like `ksit`, `kwkF`, `m <joint> <angle>`)
- Custom walking algorithms are possible at three levels: streaming joint commands from Android, defining custom skills in OpenCat format, or forking the OpenCat firmware to run IK/CPG on the ESP32

## Context

The project needs a physical robot body that an Android phone can control. The phone acts as the "brain" (running the agent), uses its camera for vision, and sends movement commands to the body via USB. This is the embodiment layer for the agent — the phone provides perception and decision-making, the robot provides locomotion.

## Approach

### Phase 1: Hardware Acquisition
- Purchase Petoi Bittle X V2, alloy servos, pre-assembled from https://www.petoi.com/products/petoi-robot-dog-bittle-x-voice-controlled
- Acquire USB-C to USB-C cable (or USB-C OTG adapter depending on phone port)
- Design/source a phone mount for the Bittle chassis (3D print using Petoi's open STL files as a starting point)

### Phase 2: Serial Communication Layer
- Implement Android USB-CDC serial connection using `usb-serial-for-android` library
- Build a serial command abstraction layer that maps high-level actions to OpenCat protocol commands:
  - `walk_forward()` → `kwkF`
  - `turn_left()` / `turn_right()` → `ktrL` / `ktrR`
  - `sit()` → `ksit`
  - `stand()` → `kup`
  - `move_joint(index, angle)` → `m <index> <angle>`
- Handle connection lifecycle (connect, reconnect, error handling)
- Test round-trip latency and command throughput

### Phase 3: Phone Mount & Integration
- Mount phone on Bittle chassis (3D printed bracket or off-the-shelf phone clamp)
- Calibrate Bittle servos (one-time setup, joint zeroing)
- Verify camera FOV when phone is mounted — ensure useful field of view for vision
- Test weight distribution — phone adds ~150-200g to the 290g robot, may affect gait stability

### Phase 4: Agent Integration
- Expose the movement command layer as tools/skills available to the agent
- Wire phone camera feed into the agent's perception pipeline
- Build a basic teleoperation mode for testing (manual control from a second device)
- Implement agent-driven locomotion: agent decides where to go based on vision, sends movement commands

## Files Changed

- New: Android USB serial communication module (within existing Android app structure)
- New: OpenCat command protocol wrapper / abstraction layer
- New: Robot body control skill/tool definitions for the agent
- Modified: Agent tool registry to include locomotion tools
- New: 3D print files for phone mount (STL, derived from Petoi open-source chassis files)

## Open Questions

1. **Phone model weight** — Which Android phone will be mounted? Weight affects gait stability and may require servo torque tuning.
2. **Power management** — Phone battery drain while tethered via USB. Separate power bank mount, or rely on phone battery?
3. **Camera orientation** — Phone mounted landscape or portrait? Front or rear camera facing forward?
4. **Custom gait priority** — Start with built-in OpenCat gaits and move to custom walking algorithms later, or invest in custom IK from the start?
5. **Arm gripper** — The Bittle X V2 supports an optional robotic arm gripper add-on. Worth getting now for future manipulation capabilities?

## Discussion Log

- **User:** Asked about wheeled or legged platforms for Android phone control via USB.
- **Claude:** Surveyed options across wheeled (DFRobot Romeo, Sabertooth, ROS bases) and legged (Freenove hexapod, LewanSoul, Petoi Bittle). Recommended Petoi Bittle as most turnkey legged option due to open-source firmware, serial command API, and active community.
- **User:** Asked to dig into Petoi Bittle specifically.
- **Claude:** Detailed breakdown — two models (original NyBoard vs Bittle X/BiBoard ESP32), pricing ($270-340), physical specs (9 DOF, 290g, ~1hr battery), Android connection paths (USB-CDC and Bluetooth SPP both using same OpenCat serial protocol), serial command reference, and gotchas (calibration, battery, shared serial port on old model). Recommended Bittle X V2 with alloy servos, assembled.
- **User:** Asked if Bluetooth and USB both offer full control.
- **Claude:** Confirmed both are identical — same serial protocol, same command set. Differences are only transport-level: USB has zero latency vs BT's ~20-50ms, USB is more reliable, USB tethers phone to robot (fine since phone is mounted). Recommended USB for this use case.
- **User:** Asked about custom walking algorithms.
- **Claude:** Three levels of customization: (1) stream joint-angle commands from Android app, (2) define custom skill/gait matrices in OpenCat format and flash to board, (3) fork OpenCat firmware and implement IK/CPG on the ESP32. Noted that standard firmware doesn't expose IK — you command joint angles directly, but 2-DOF leg IK is straightforward closed-form. Recommended Level 1 for fastest demo, Level 3 for robust terrain-adaptive locomotion.
- **User:** Asked for purchase link.
- **Claude:** Provided direct link to Petoi site — Bittle X V2, select alloy servos + pre-assembled, ~$300-340.
