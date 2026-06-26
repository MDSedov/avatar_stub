#!/usr/bin/env bash
set -euo pipefail

DIR="${1:-./data/avatars}"

java -jar target/avatar-stub-1.0.0.jar \
  --avatar.mode=server \
  --avatar.dir="$DIR"
