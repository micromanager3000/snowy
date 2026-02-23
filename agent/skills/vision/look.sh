#!/bin/sh
# Capture a photo from the front camera via the hardware bridge.
# Saves to /tmp/snowy-look.jpg
curl -s -X POST http://127.0.0.1:42618/camera/capture \
  -H "Content-Type: application/json" \
  -d '{"camera":"front"}' | \
  sed 's/.*"image":"//;s/".*//' | \
  base64 -d > /tmp/snowy-look.jpg 2>/dev/null

if [ -s /tmp/snowy-look.jpg ]; then
    echo "Photo captured: /tmp/snowy-look.jpg"
else
    echo "Error: Camera capture failed"
    exit 1
fi
