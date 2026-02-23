# 008: Version-Control Config and Deploy Skills

**Status:** Completed
**Created:** 2026-02-22
**Related projects:** 007_android-pet-face-and-senses (skills created), 006_zeroclaw-foreground-service (deploy script)

---

## Initial Prompt

> for all these config changes, can we somehow check these into our repo so the defaults if we were to start from scratch would be correct?

## Background

Over the course of projects 006 and 007, we've made several config changes directly on the device (adding `curl`/`sh`/`base64`/`sed` to allowed_commands, tuning security settings, etc.) and deployed skill scripts ad-hoc via manual `adb push` commands. None of these changes are captured in version control. If we factory-reset the phone or set up a second device, we'd have to redo everything by hand.

The current `android/config.toml.example` is a 4-line stub that doesn't reflect the 300+ line production config. Skill scripts live in `agent/skills/` but the deploy script doesn't know about them.

## Context

Two problems to solve:
1. **Config drift** — The real config on the device has diverged far from the checked-in example. Defaults like allowed_commands, autonomy settings, memory config, gateway settings, etc. should be version-controlled so a fresh deploy gets the right config.
2. **Skill deployment** — The deploy script (`scripts/deploy-snowy.sh`) deploys `.md` personality files from `android/` but doesn't deploy skill scripts from `agent/skills/`. Skills are currently pushed manually.

## Approach

### 1. Expand config.toml.example to full default config

Replace the 4-line stub with the complete production config, but with secrets placeholder'd:
- `api_key = "YOUR_ANTHROPIC_API_KEY_HERE"` (not the encrypted device key)
- `paired_tokens = []` (empty — the Android app injects its own token at runtime)
- Everything else: real production values (allowed_commands, security, memory, gateway, etc.)

The workflow stays the same: `cp config.toml.example config.toml`, fill in API key, deploy. But now you get all 300+ lines of tuned defaults for free.

### 2. Update deploy script to deploy skills

Add a loop to `deploy-snowy.sh` that copies all skill directories from `agent/skills/` to the device workspace `skills/` directory. Each skill subdir (expression, vision, voice, hearing) with its SKILL.md and .sh files gets deployed and made executable.

### 3. No structural changes

- `android/config.toml` stays gitignored (has real secrets)
- `android/config.toml.example` is the version-controlled template
- Deploy script workflow unchanged — just does more

## Files Changed

```
# Modified:
android/config.toml.example       # Expand from 4-line stub to full default config
scripts/deploy-snowy.sh            # Add skill deployment from agent/skills/
```

## Open Questions

None — straightforward.

## Discussion Log

- **User:** Asked if config changes could be checked into repo so starting from scratch would have correct defaults.
- **Claude:** Investigated current setup. Found config.toml.example is a 4-line stub, real config is 300+ lines on device only. Skills deployed manually. Proposed expanding the example config and adding skill deployment to the deploy script.

## Progress

- 2026-02-22: Pulled full config from device via `adb shell run-as`. Expanded `android/config.toml.example` from 7-line stub to full ~300-line config with `api_key` placeholder'd and `paired_tokens = []`. Updated `scripts/deploy-snowy.sh` to deploy all skill subdirs from `agent/skills/` in both APK mode (via run-as cp) and legacy ADB mode (via direct push). Skills get `.sh` files chmod'd executable.
