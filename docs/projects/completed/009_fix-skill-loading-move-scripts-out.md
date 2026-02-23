# 009: Fix Skill Loading — Remove Scripts, Use Direct Curl

**Status:** Completed
**Created:** 2026-02-23
**Related projects:** 008_version-control-config-and-deploy (skill deployment), 007_android-pet-face-and-senses (skills created)

---

## Initial Prompt

> let's see what the best way for these scripts to run is maybe? like maybe it would prefer python for zeroclaw instead or somehow mark them as safe?
> actually, why do we need .sh files at all? can't we just tell the agent to make calls directly?

## Background

ZeroClaw's skill security audit blocks any skill directory containing `.sh` files — no whitelist exists. This causes all skills to be skipped entirely, including their SKILL.md context.

The `.sh` scripts are simple wrappers around curl calls to the hardware bridge (port 42618). The LLM already has `curl`, `sh`, `base64`, and `sed` in its allowed_commands. The SKILL.md files already document the exact HTTP endpoints. There's no reason to keep the scripts — just tell the LLM to make the calls directly.

## Context

After deploying skills in project 008, ZeroClaw logs:
```
skipping insecure skill directory .../expression: show_face.sh: script-like files are blocked by skill security policy.
```

## Approach

### 1. Delete all `.sh` files from skill directories

Remove:
- `agent/skills/expression/show_face.sh`
- `agent/skills/vision/look.sh`
- `agent/skills/vision/describe.sh`
- `agent/skills/voice/speak.sh`
- `agent/skills/hearing/listen.sh`

### 2. Update SKILL.md files with direct commands

Replace script references (`{baseDir}/show_face.sh <emotion>`) with the actual curl commands the LLM should run. The SKILL.md already has "Hardware Bridge Command" sections with the endpoint docs — just update the "How to" sections to give the LLM copy-paste-ready curl commands instead of script invocations.

For multi-step operations (look.sh captures + decodes base64, listen.sh records + decodes), include the full pipeline as an example command in the SKILL.md.

For describe.sh (sends image to Anthropic vision API via curl) — this is redundant since ZeroClaw *is* an LLM. It can read images natively if they're provided. Remove this and just have the vision skill capture the photo and let ZeroClaw interpret it.

### 3. Simplify deploy script

Remove the bin/scripts deployment complexity from `deploy-snowy.sh`. Skills are now just `.md` files — the existing deployment loop handles them already. Remove the expression `faces/` subdir reference if it's empty.

## Files Changed

```
# Deleted:
agent/skills/expression/show_face.sh
agent/skills/vision/look.sh
agent/skills/vision/describe.sh
agent/skills/voice/speak.sh
agent/skills/hearing/listen.sh

# Modified:
agent/skills/expression/SKILL.md  — replace script ref with direct curl command
agent/skills/vision/SKILL.md      — replace script refs with direct curl commands
agent/skills/voice/SKILL.md       — replace script ref with direct curl command
agent/skills/hearing/SKILL.md     — replace script ref with direct curl command
scripts/deploy-snowy.sh           — simplify (skills are just .md files now)
```

## Discussion Log

- **User:** Asked about the best way for skill scripts to run — maybe Python or marking them as safe?
- **Claude:** Investigated ZeroClaw's audit system. Found `.sh` files hardcoded-blocked with no bypass. Proposed moving scripts to `agent/bin/`.
- **User:** Why do we need .sh files at all? Can't we just tell the agent to make calls directly?
- **Claude:** Agreed — the scripts are thin curl wrappers. The LLM has curl in allowed_commands and the SKILL.md already documents the endpoints. Simplified plan: delete scripts, update SKILL.md with direct curl commands.

## Verification

1. Run `./scripts/deploy-snowy.sh` — verify skills deploy (just .md files)
2. Check device: `adb shell ls workspace/skills/expression/` — only SKILL.md
3. Send Snowy a message — no "skipping insecure skill" warnings
4. Ask Snowy to show a face expression — verify she constructs and runs the curl command

## Progress

- 2026-02-23: Deleted all 5 .sh scripts from skill dirs. Updated SKILL.md files to include direct curl commands instead of script references. Simplified deploy script to only push .md files. Verified: no audit warnings, Snowy constructs curl commands from SKILL.md instructions.
