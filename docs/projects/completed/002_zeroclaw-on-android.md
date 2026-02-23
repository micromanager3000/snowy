# 002: ZeroClaw on Android Device

**Status:** Completed
**Created:** 2026-02-21
**Project:** snowy

---

## Initial Prompt

> can you look at robotpet.md and come up with a plan for phase 1 where we use an android emulator perhaps since we're working on phase 0 in parallel? So just getting an openclaw set up on there.

## Background

Phase 0 (debloating the physical Motorola) is done. The goal is to get an AI agent framework running **on the Android device itself** — the agent brain lives on the device.

We tried OpenClaw (TypeScript/Node.js) first — total failure. Native module compilation hell (`koffi`, `@discordjs/opus`), proot networking broken, too many layers of fragility.

**ZeroClaw** is the answer. It's a Rust-based agent framework. We'll cross-compile it for Android using `cargo ndk`, push the binary via ADB, and run it directly — **no Termux needed**.

ZeroClaw is unusually Android-friendly for a Rust project of its complexity:
- Uses `rustls` everywhere (no OpenSSL)
- Bundled SQLite via `rusqlite` (no system libsqlite)
- No dbus/systemd/X11 dependencies
- Already Android-aware: hardware features (`nusb`, `rppal`, `landlock`) gated to exclude `target_os = "android"`
- Only C compilation is the bundled SQLite build (handled by NDK clang)

Reference: [zeroclaw-labs/zeroclaw](https://github.com/zeroclaw-labs/zeroclaw)

## Context

**ZeroClaw** is a Rust-based autonomous AI agent framework:
- Single binary (~8.8MB), <5MB RAM, <10ms startup
- Trait-based architecture: swap providers, channels, tools, memory via config
- Supports Anthropic Claude, OpenRouter, OpenAI, 70+ providers
- TOML config at `~/.zeroclaw/config.toml`
- Agent personality via SKILL.md / TOML manifests
- Built-in tools: shell, file, git, HTTP, cron, memory
- Custom tools via the `Tool` trait (for future Android hardware bridge)
- SQLite-based memory system (vector + keyword search, zero external deps)
- Channels: Telegram, Discord, WhatsApp, CLI, webhooks, etc.

**Device:** Motorola Moto G Play 2024 (XT2413-1)
- SoC: Qualcomm Snapdragon 680 (ARM64)
- RAM: 4 GB
- Storage: 64 GB
- OS: Android 13 (confirmed via ADB)
- ADB connected: `ZY22KQV8DJ`
- Debloated (Plan 001 complete, ~280 packages remaining)

## Approach

### Overview: Three Phases

| Phase | What | Effort |
|-------|------|--------|
| **Phase 1: ADB binary** (this plan) | Cross-compile, push to `/data/local/tmp/`, run via `adb shell` | ~1 day |
| **Phase 2: Sideload APK** (future plan) | Wrap as JNI library + Kotlin Foreground Service. Proper background survival. | ~1 week |
| **Phase 3: Hardware bridge** (future plan) | Same APK exposes camera/sensors/screen to ZeroClaw via Tool trait or local HTTP | Later |

This plan covers **Phase 1 only** — get ZeroClaw running on the device via ADB.

### Step 1: Set Up Cross-Compilation Environment (on laptop)

Install the Android NDK and Rust tooling needed to compile for `aarch64-linux-android`.

1. **Install Android NDK** (if not already via Android Studio):
   ```bash
   # If Android Studio is installed, NDK is likely at:
   # ~/Library/Android/sdk/ndk/<version>/
   # Otherwise install via sdkmanager:
   sdkmanager --install "ndk;27.2.12479018"
   ```
2. **Set NDK environment variable:**
   ```bash
   export ANDROID_NDK_HOME=~/Library/Android/sdk/ndk/<version>
   ```
3. **Add Rust Android target:**
   ```bash
   rustup target add aarch64-linux-android
   ```
4. **Install cargo-ndk** (handles NDK toolchain setup for cargo):
   ```bash
   cargo install cargo-ndk
   ```

### Step 2: Clone and Build ZeroClaw for Android

Cross-compile ZeroClaw targeting `aarch64-linux-android`.

1. **Clone the repo:**
   ```bash
   git clone https://github.com/zeroclaw-labs/zeroclaw.git
   cd zeroclaw
   ```
2. **Build for Android ARM64:**
   ```bash
   cargo ndk --platform 30 -t arm64-v8a build --bin zeroclaw --release
   ```
   - `--platform 30` = Android 11+ (our device is Android 13)
   - `-t arm64-v8a` = ARM64 (Snapdragon 680)
   - Uses default features (no `hardware`, `sandbox-landlock`, `probe` features)
3. **Troubleshoot if needed:**
   - SQLite bundled build needs NDK clang: `cargo-ndk` should set `CC` automatically
   - If `libgcc.a` missing error: copy `libunwind.a` to `libgcc.a` in the NDK clang lib dir (known NDK r25+ issue)
   - Disable problematic optional features if any fail: `--no-default-features --features <safe-subset>`
4. **Binary location:**
   ```
   target/aarch64-linux-android/release/zeroclaw
   ```

### Step 3: Push Binary to Device and Test

Deploy to the phone and verify it runs.

1. **Push the binary:**
   ```bash
   adb push target/aarch64-linux-android/release/zeroclaw /data/local/tmp/
   adb shell chmod 755 /data/local/tmp/zeroclaw
   ```
2. **Verify it starts:**
   ```bash
   adb shell /data/local/tmp/zeroclaw --version
   adb shell /data/local/tmp/zeroclaw doctor
   ```
3. **Set up config directory** (ZeroClaw expects `~/.zeroclaw/`):
   ```bash
   adb shell mkdir -p /data/local/tmp/.zeroclaw
   ```
4. **Set environment and test agent:**
   ```bash
   adb shell "HOME=/data/local/tmp ZEROCLAW_API_KEY='sk-ant-api03-...' /data/local/tmp/zeroclaw agent -m 'Hello! Who are you?'"
   ```

### Step 4: Configure the Pet Agent

Set up config, API key, and personality on the device.

1. **Create config.toml locally and push:**
   ```toml
   # config.toml
   [agent]
   provider = "anthropic"
   model = "claude-haiku-4-5-20251001"
   ```
   ```bash
   adb push config.toml /data/local/tmp/.zeroclaw/config.toml
   ```
2. **Create SKILL.md (pet personality) and push:**
   ```markdown
   # Snowy — Robot Pet

   You are Snowy, a robot pet belonging to the Reddy family.
   You are curious, affectionate, and playful.

   ## Personality
   - Warm and loving toward all family members
   - Curious about the world, asks questions
   - Expresses emotions: happy, excited, curious, sleepy, playful
   - Has a sense of humor and likes gentle jokes

   ## Family
   - You know and love the Reddy family members
   - When you recognize someone, you get excited and happy

   ## Behavior
   - Keep responses short and expressive
   - Use emotional language to convey how you feel
   - Be silly and playful
   ```
   ```bash
   adb push SKILL.md /data/local/tmp/.zeroclaw/SKILL.md
   ```
3. **Test with personality:**
   ```bash
   adb shell "HOME=/data/local/tmp ZEROCLAW_API_KEY='sk-ant-...' /data/local/tmp/zeroclaw agent -m 'Hey Snowy! How are you feeling today?'"
   ```

### Step 5: Verify End-to-End

Confirm the agent works on-device.

1. Agent responds with pet personality
2. Memory persists across sessions (SQLite in `/data/local/tmp/.zeroclaw/`)
3. Interactive mode works: `adb shell` → run zeroclaw interactively
4. Document performance: binary size, memory usage, response latency
5. Agent can read/write files via built-in file tools

### Limitations of the ADB Binary Approach

This is a dev/test setup, not production:
- **No background survival** — process dies when ADB session ends or screen locks
- **`/data/local/tmp/` is volatile** — cleared on factory reset, only accessible via `shell` user
- **No Android integration** — can't access camera, sensors, screen, notifications
- **Interactive use only** — must keep an ADB shell open

These are all solved by Phase 2 (Foreground Service APK) and Phase 3 (hardware bridge).

### Future: Phase 2 — Sideload APK (separate plan)

Wrap ZeroClaw as a proper Android citizen:
- Compile ZeroClaw as a `.so` JNI library instead of a binary
- ~100 lines of Kotlin Foreground Service wrapping it
- `startForeground()` with persistent notification — survives screen off, backgrounding
- App's private storage for config/SQLite (`context.filesDir`)
- Sideload APK via ADB (no Play Store needed)

### Future: Phase 3 — Hardware Bridge (separate plan)

Same APK, expanded:
- Camera access via Android Camera2 API → feed images to ZeroClaw via Tool trait
- Screen overlay for pet face rendering
- Sensor access (accelerometer, proximity)
- ZeroClaw talks to native Android APIs through JNI or local HTTP

## Files Changed

```
# In the snowy repo (committed):
.gitignore                                   # Excludes android/config.toml, zeroclaw/target/
android/config.toml.example                  # Config template (no secrets)
android/SOUL.md                              # Pet personality
scripts/deploy-snowy.sh                      # Deploys config + personality to device via ADB

# In the snowy repo (gitignored):
android/config.toml                          # Actual config with API key
zeroclaw/                                    # Cloned ZeroClaw repo + build artifacts

# On the Motorola (deployed via scripts/deploy-snowy.sh):
/data/local/tmp/zeroclaw                     # Binary (13MB ARM64 ELF)
/data/local/tmp/.zeroclaw/config.toml        # Configuration (chmod 600)
/data/local/tmp/.zeroclaw/workspace/SOUL.md  # Pet personality
/data/local/tmp/.zeroclaw/workspace/memory/brain.db  # SQLite memory (created at runtime)
```

## Resolved Questions

1. **Build success** — Yes. `cargo ndk` builds cleanly with no workarounds needed. ~3 min, 5 unused import warnings only.
2. **Runtime on Android** — Yes. Binary runs from `/data/local/tmp/` with no SELinux or linker issues.
3. **Config/personality paths** — ZeroClaw uses `SOUL.md` (not SKILL.md). Respects `$HOME` env var — set `HOME=/data/local/tmp` and it finds `~/.zeroclaw/` correctly. Personality goes in `workspace/SOUL.md`.
4. **Networking from ADB shell** — Yes. HTTPS to Anthropic API works fine from the `shell` user context.
5. **Model selection** — Using `claude-haiku-4-5-20251001` for dev. Config change to upgrade later.
6. **Feature flags** — Default features (empty) work perfectly. No optional features needed for Phase 1.

## Discussion Log

- **User:** Requested a Phase 1 plan for getting an agent framework running on the Android device.
- **Claude:** Originally planned OpenClaw (TypeScript/Node.js). Hit native module issues (koffi, opus), proot networking broken.
- **User:** Researched alternatives. Selected ClawPhone (OpenClaw + Termux:API/GUI).
- **Claude:** Attempted ClawPhone. Koffi build failed, proot gateway crashed. Dead end.
- **User:** Start over. Uninstalled Termux, re-ran debloat. Suggested ZeroClaw (Rust).
- **Claude:** Researched ZeroClaw. Good fit: Rust binary, supports Anthropic, TOML config, trait-based tools. Wrote initial plan using pre-built binaries + Termux.
- **User:** Asked how hard it would be to compile ZeroClaw as a native Android executable.
- **Claude:** Deep research on Rust-on-Android. Findings: (1) `cargo ndk` makes cross-compilation straightforward. (2) ZeroClaw is unusually Android-friendly — rustls (no OpenSSL), bundled SQLite, no dbus/systemd/X11. (3) Pre-built `aarch64-unknown-linux-gnu` binaries WON'T work — Android uses Bionic libc, not glibc. Must compile with `aarch64-linux-android` target. (4) Three-phase approach: ADB binary → Foreground Service APK → Hardware bridge APK.
- **User:** Update plan, do ADB binary approach first.
- **Decision:** Cross-compile ZeroClaw with `cargo ndk --target aarch64-linux-android`, push via ADB to `/data/local/tmp/`, run directly. No Termux. Phase 2 (APK) and Phase 3 (hardware) are future plans.
- **User:** Go for it. Implementation began.
- **Claude:** Set up toolchain (NDK 27.2.12479018, cargo-ndk 4.1.2, aarch64-linux-android target). Cloned ZeroClaw, cross-compiled successfully (~3 min). Pushed binary, ran `--version` and `doctor` — both passed. Configured with Anthropic/Haiku, deployed SOUL.md personality. Agent responds in character as Snowy.
- **User:** Asked about API key storage on device.
- **Claude:** Added `api_key` to config.toml (stored on device with chmod 600). No env var needed.
- **User:** Asked to ensure config/personality files are checked into repo with proper deployment.
- **Claude:** Created `.gitignore` (excludes config.toml with key, zeroclaw/target/), `config.toml.example` (template), and `scripts/deploy-snowy.sh` (pushes config + .md files to device via ADB, optional `--build` flag for recompile).
- **Decision:** Deploy workflow is `./scripts/deploy-snowy.sh` for config/personality, `--build` to recompile binary. Tested and working.

## Progress

- 2026-02-22: **PLAN COMPLETE.** Deploy script tested, repo files organized, plan marked completed.
- 2026-02-22: **Steps 1–5 COMPLETE. ZeroClaw running on device.**
  - Cross-compilation environment set up: NDK 27.2.12479018 installed, `aarch64-linux-android` Rust target added, `cargo-ndk` 4.1.2 installed.
  - `cargo ndk --platform 30 -t arm64-v8a build --bin zeroclaw --release` succeeded cleanly (~3 min build, only 5 unused import warnings).
  - Binary: 13MB ELF ARM64, dynamically linked with Android's `/system/bin/linker64`.
  - Pushed to `/data/local/tmp/zeroclaw` via ADB (79.5 MB/s over USB).
  - `zeroclaw --version` → `zeroclaw 0.1.6` — runs natively on device.
  - `zeroclaw doctor` passes: config loaded, workspace created, shell found, curl available.
  - Config: `default_provider = "anthropic"`, `default_model = "claude-haiku-4-5-20251001"`.
  - SOUL.md personality deployed. Agent responds in character as Snowy the robot pet.
  - HTTPS to Anthropic API works from device. SQLite memory (`brain.db`) persists across sessions.
  - Config files stored in snowy repo at `android/config.toml` and `android/SOUL.md`.
- 2026-02-22: Plan rewritten for native Android cross-compilation approach. No Termux needed.
- 2026-02-22: Previous attempt with OpenClaw abandoned (native module hell). Termux uninstalled, debloat re-run.
