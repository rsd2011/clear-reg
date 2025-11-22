#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
DIGEST_FILE="$ROOT_DIR/docs/permission-bundle-digests.json"

if [[ ! -f "$DIGEST_FILE" ]]; then
  echo "[ERROR] permission bundle digest file not found: $DIGEST_FILE" >&2
  exit 1
fi

python3 - "$ROOT_DIR" "$DIGEST_FILE" <<'PY'
import hashlib
import json
import pathlib
import sys

root = pathlib.Path(sys.argv[1])
dice_file = pathlib.Path(sys.argv[2])
data = json.loads(dice_file.read_text(encoding='utf-8'))
failures = []
for entry in data:
    path = root / entry['path']
    expected = entry['sha256']
    description = entry.get('description', entry['path'])
    if not path.exists():
        failures.append(f"missing file: {path}")
        continue
    digest = hashlib.sha256(path.read_bytes()).hexdigest()
    if digest != expected:
        failures.append(
            f"digest mismatch for {description}\n  expected: {expected}\n    actual: {digest}"
        )
    else:
        print(f"[OK] {description} -> {digest}")

if failures:
    print("\n[FAILED] Permission bundle digest check failed:")
    for failure in failures:
        print(f" - {failure}")
    sys.exit(1)

print("\n[PASS] All permission bundle digests match recorded values.")
PY
