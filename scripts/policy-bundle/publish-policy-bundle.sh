#!/usr/bin/env bash
set -euo pipefail

BUNDLE_PATH="${1:?bundle path is required}"
API_URL="${POLICY_GATEWAY_URL:-}"
API_TOKEN="${POLICY_GATEWAY_TOKEN:-}"

if [[ -z "$API_URL" ]]; then
  echo "POLICY_GATEWAY_URL env var is required" >&2
  exit 1
fi
if [[ ! -f "$BUNDLE_PATH" ]]; then
  echo "Bundle not found at $BUNDLE_PATH" >&2
  exit 1
fi

AUTH_HEADER=()
if [[ -n "$API_TOKEN" ]]; then
  AUTH_HEADER=(-H "Authorization: Bearer $API_TOKEN")
fi

curl -fsSL -X POST "$API_URL" \
  -H "Content-Type: application/gzip" \
  "${AUTH_HEADER[@]}" \
  --data-binary "@$BUNDLE_PATH"
