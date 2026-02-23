#!/bin/bash
# Print the next available project number
# Usage: ./next-number.sh

DIR="$(cd "$(dirname "$0")" && pwd)"
MAX=$(find "$DIR" -name '*.md' | grep -oE '/[0-9]{3}_' | grep -oE '[0-9]{3}' | sort -rn | head -1)
echo $(printf "%03d" $(( ${MAX:-0} + 1 )))
