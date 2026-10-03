#!/usr/bin/env bash
set -euo pipefail
FILE="${1:?Usage: ./scripts/generate-presentation.sh path/to/file.pdf [slides]}"
SLIDES="${2:-8}"
curl -f -X POST "http://localhost:8080/api/presentations/generate?slides=${SLIDES}" \
  -F "file=@${FILE}" \
  -o generated-presentation.pptx

echo "Created generated-presentation.pptx"
