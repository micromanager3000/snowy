#!/bin/sh
# Change Snowy's facial expression via the hardware bridge.
# Usage: show_face.sh <emotion>
# Emotions: happy, ecstatic, curious, playful, content, sleepy, lonely, confused, alert
STATE="${1:-happy}"
curl -s -X POST http://127.0.0.1:42618/face/show \
  -H "Content-Type: application/json" \
  -d "{\"state\":\"$STATE\"}"
