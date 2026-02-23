# 003: Jailbreak Motorola — Clean Android Install

**Status:** Draft
**Created:** 2026-02-21
**Project:** snowy

---

## Initial Prompt

> can you make a draft plan of what we would do if we wanted to jailbreak the device and install a clean android?

## Background

The Moto G Play 2024 (XT2413-1) is a Cricket/AT&T variant. Motorola denied the bootloader unlock request ("device does not qualify"). The standard path is closed. This plan explores all known methods to bypass the locked bootloader and install a clean Android image. This is a research/reference plan — not urgent, since the ADB-debloated device works fine for prototyping.

## Context

The ADB debloat (Plan 001, Phase 0) removed ~195 packages, but the phone still has carrier firmware, Motorola services running in the background, and the Cricket boot splash. A truly clean Android (AOSP GSI or LineageOS) would give us full control, better performance, no background junk, and a blank canvas for the robot pet.

## Known Methods

### Method 1: EDL (Emergency Download Mode) — Most Promising

**What it is:** Qualcomm's lowest-level flashing interface. Bypasses the bootloader entirely. Works even on locked devices.

**How it works:**
1. Disassemble the phone to access the motherboard
2. Locate the EDL test points on the board (two small solder pads that need to be shorted)
3. Short the test points while connecting USB — phone enters EDL mode (shows as "Qualcomm HS-USB QDLoader 9008" on PC)
4. Use a Qualcomm flashing tool (QFIL, bkerler's edl tool, or MiFlash) to write directly to partitions
5. Flash a stock firmware with unlocked bootloader flag, or directly flash GSI partitions

**Requirements:**
- EDL test point locations for the XT2413 board (not yet publicly documented — need to find or map them ourselves)
- A Qualcomm firehose programmer file (`.mbn` or `.elf`) for this device's chipset (Snapdragon 680 / SM6225)
- [bkerler/edl](https://github.com/bkerler/edl) — open-source Python-based Qualcomm EDL tool
- Precision tools for disassembly + fine-tip tweezers or probe wires for shorting test points

**Risks:**
- Test points not publicly documented for this specific board — would need to identify them from service manuals or by probing
- Firehose programmer files are device-specific and often leaked rather than officially available
- Hardware risk if test points are shorted incorrectly
- Some XDA users have reported the XT2413 failing to enter EDL mode even with test point shorting

**Status:** Partially viable. The Snapdragon 680 supports EDL, but the device-specific pieces (test points, programmer file) are not yet confirmed available. A Scribd document claims to be a "Moto G Play XT2413 Level 3 Service Manual" which might contain test point info.

**Sources:**
- https://xdaforums.com/t/moto-g-play-2024-other-devices-frp-bl-unlock.4759719/
- https://www.scribd.com/document/825812236/Moto-G-Play-XT2413-Level-3-Service-Manual

### Method 2: avbroot — Custom OTA with Locked Bootloader

**What it is:** A tool that re-signs Android OTA updates with a custom key, allowing you to sideload modified firmware while keeping the bootloader locked.

**How it works:**
1. Obtain the stock OTA zip for the XT2413-1
2. Use [avbroot](https://github.com/chenxiaolong/avbroot) to patch the OTA — inject a Magisk-patched boot image and re-sign
3. Sideload the patched OTA via recovery mode
4. Device boots the modified image with root access

**Requirements:**
- Stock OTA zip file for this exact firmware version
- The device must support custom AVB keys via `fastboot flash avb_custom_key` — which likely requires an unlocked bootloader in the first place

**Risks:**
- Catch-22: avbroot's typical workflow assumes you can flash a custom AVB key, which requires an unlocked bootloader. For devices that are *already* locked, it's mainly useful for re-locking after rooting.
- May not be applicable to our situation at all.

**Status:** Likely a dead end for a carrier-locked device. avbroot is designed for a different use case (re-locking after unlock, not bypassing a lock).

**Source:** https://github.com/chenxiaolong/avbroot

### Method 3: Paid Unlock Services (Chimera / Octoplus / UnlockTool)

**What it is:** Commercial tools used by phone repair shops that have proprietary exploits or carrier agreements to unlock bootloaders.

**How it works:**
1. Purchase a license/credits for a tool like Chimera, Octoplus, or UnlockTool
2. Connect phone, run the tool
3. Tool uses proprietary methods (often EDL-based) to modify the bootloader lock flag

**Requirements:**
- Windows PC
- License purchase ($20-100+ depending on tool and credits)
- Tool must support the specific device model and firmware version

**Risks:**
- No guarantee of support for XT2413-1 specifically
- These tools come and go — exploits get patched
- Running proprietary closed-source tools from third parties on a device connected to your computer

**Status:** Possible but unverified. Would need to check if any of these tools currently list the XT2413/fogona as supported.

### Method 4: Buy an Unlockable Variant

**What it is:** Just buy a different phone that allows bootloader unlocking.

**Options:**
- **Motorola Moto G Play 2024 — unlocked/T-Mobile variant** (XT2413-2 or XT2413-3) — T-Mobile variants can unlock via Motorola's portal. Consumer Cellular variant is also reported as OEM-unlockable.
- **Any Pixel phone** — Google Pixels have the easiest bootloader unlock (one fastboot command, no carrier restrictions on most models). Even a used Pixel 6a or 7a would be cheap and very well supported by LineageOS/GrapheneOS/AOSP.
- **OnePlus** — generally bootloader-unlockable.

**Cost:** $50-150 for a used device.

**Status:** The most reliable path by far. A used Pixel would be the best option for a robot pet — excellent camera, excellent custom ROM support, well-documented.

## Recommendation

**If this becomes important, buy a Pixel.** The EDL path is interesting but requires hardware hacking with incomplete documentation. The commercial tools are a gamble. A used Pixel 6a/7a is $50-80 on eBay, has a trivially unlockable bootloader, first-class LineageOS/GrapheneOS support, and a great camera. It would save hours of fighting with Motorola's carrier lock.

For now, the debloated Cricket Moto G Play works fine for prototyping. Revisit this plan if/when the background Motorola services become a real problem.

## Open Questions

1. **Is the Level 3 service manual on Scribd legit?** Could contain EDL test point locations.
2. **Do any commercial unlock tools currently support XT2413-1?** Would need to check Chimera/Octoplus compatibility lists.
3. **Budget for a replacement device?** A used Pixel might be the pragmatic answer.

## Discussion Log

- **User:** Asked for a draft plan on jailbreaking the device and installing clean Android.
- **Claude:** Researched all known methods: EDL mode (most promising but hardware docs missing), avbroot (likely dead end for locked devices), commercial unlock tools (possible but unverified), buying an unlockable device (most reliable). Recommended buying a used Pixel if clean Android becomes important.
