#!/bin/sh
# Record audio from the microphone via the hardware bridge.
# Usage: listen.sh [duration_secs]
# Saves OGG/Opus audio to /tmp/snowy-listen.ogg
DURATION="${1:-5}"
RESPONSE=$(curl -s -X POST http://127.0.0.1:42618/audio/record \
  -H "Content-Type: application/json" \
  -d "{\"duration\":$DURATION}")

# Extract base64 audio and decode to file
echo "$RESPONSE" | sed 's/.*"audio":"\([^"]*\)".*/\1/' | base64 -d > /tmp/snowy-listen.ogg

if [ -s /tmp/snowy-listen.ogg ]; then
  echo "Recorded ${DURATION}s of audio to /tmp/snowy-listen.ogg"
else
  echo "Recording failed"
  exit 1
fi
