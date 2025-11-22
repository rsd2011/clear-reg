#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(git rev-parse --show-toplevel)"
MANIFEST_REL="docs/permission-bundle-digests.json"
MANIFEST_PATH="$ROOT_DIR/$MANIFEST_REL"
if [[ ! -f "$MANIFEST_PATH" ]]; then
  echo "Manifest $MANIFEST_REL not found" >&2
  exit 1
fi
if ! command -v jq >/dev/null 2>&1; then
  echo "jq is required" >&2
  exit 1
fi
OUTPUT_DIR="${1:-$ROOT_DIR/build/policy-bundles}"
mkdir -p "$OUTPUT_DIR"
mapfile -t RELATIVE_PATHS < <(jq -r '.[].path' "$MANIFEST_PATH")
REL_FILES=("$MANIFEST_REL")
ABS_FILES=("$MANIFEST_PATH")
for rel in "${RELATIVE_PATHS[@]}"; do
  rel_trimmed="${rel##./}"
  abs="$ROOT_DIR/$rel_trimmed"
  if [[ -f "$abs" ]]; then
    REL_FILES+=("$rel_trimmed")
    ABS_FILES+=("$abs")
  else
    echo "[WARN] Skipping missing policy file: $rel_trimmed" >&2
  fi
done
DIGEST_INPUT=""
for file in "${ABS_FILES[@]}"; do
  DIGEST_INPUT+="$file"
done
DIGEST=$(cat "${ABS_FILES[@]}" | sha256sum | awk '{print $1}')
BUNDLE_NAME="policy-bundle-$DIGEST.tar.gz"
 tar -czf "$OUTPUT_DIR/$BUNDLE_NAME" -C "$ROOT_DIR" "${REL_FILES[@]}"
cat <<INFO > "$OUTPUT_DIR/policy-bundle-$DIGEST.info"
bundle=$BUNDLE_NAME
digest=$DIGEST
generatedAt=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
files=${REL_FILES[*]}
INFO
echo "$OUTPUT_DIR/$BUNDLE_NAME"
