#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
SRC_DIR="$ROOT_DIR/androidApp/src/main/res/drawable-nodpi"
DEST_DIR="$ROOT_DIR/iosApp/Resources/StepImages"

mkdir -p "$DEST_DIR"

count=0
for f in "$SRC_DIR"/step_*.png; do
  if [[ -f "$f" ]]; then
    cp "$f" "$DEST_DIR/"
    count=$((count + 1))
  fi
done

echo "Synced $count step assets to $DEST_DIR"

