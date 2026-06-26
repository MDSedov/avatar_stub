#!/usr/bin/env bash
set -euo pipefail

COUNT="${1:-230000}"
DIR="${2:-./data/avatars}"

java -jar target/avatar-stub-1.0.0.jar \
  --spring.main.web-application-type=none \
  --avatar.mode=generate \
  --avatar.generate-count="$COUNT" \
  --avatar.dir="$DIR"
