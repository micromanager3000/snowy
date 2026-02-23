#!/bin/bash
# Send the most recent photo to Claude vision API for description.
# Requires ANTHROPIC_API_KEY in environment.
# Outputs a text description of what's in the image.

IMAGE_PATH="${1:-/tmp/snowy-look.jpg}"

if [ ! -f "$IMAGE_PATH" ]; then
    echo "Error: No image at $IMAGE_PATH. Run look.sh first."
    exit 1
fi

BASE64_IMAGE=$(base64 -w 0 "$IMAGE_PATH" 2>/dev/null || base64 "$IMAGE_PATH")

curl -s https://api.anthropic.com/v1/messages \
    -H "content-type: application/json" \
    -H "x-api-key: $ANTHROPIC_API_KEY" \
    -H "anthropic-version: 2023-06-01" \
    -d "{
        \"model\": \"claude-haiku-4-5-20251001\",
        \"max_tokens\": 256,
        \"messages\": [{
            \"role\": \"user\",
            \"content\": [
                {
                    \"type\": \"image\",
                    \"source\": {
                        \"type\": \"base64\",
                        \"media_type\": \"image/jpeg\",
                        \"data\": \"$BASE64_IMAGE\"
                    }
                },
                {
                    \"type\": \"text\",
                    \"text\": \"You are the eyes of a robot puppy. Describe what you see in 2-3 short sentences. Focus on: Are there any people? How many? Can you describe them (age, appearance)? What is the setting? Is it bright or dark? Is anything moving or different? Be factual and brief.\"
                }
            ]
        }]
    }" | python3 -c "import sys,json; print(json.load(sys.stdin)['content'][0]['text'])" 2>/dev/null || echo "Vision API error"
