#!/bin/sh
# Make Snowy speak aloud via the hardware bridge.
# Usage: speak.sh "Hello!"
# Optional env: PITCH (default 1.5), SPEED (default 1.0)
TEXT="${1:-Woof!}"
PITCH="${PITCH:-1.5}"
SPEED="${SPEED:-1.0}"
curl -s -X POST http://127.0.0.1:42618/tts/speak \
  -H "Content-Type: application/json" \
  -d "{\"text\":\"$TEXT\",\"pitch\":$PITCH,\"speed\":$SPEED}"
