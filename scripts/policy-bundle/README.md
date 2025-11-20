# Policy Bundle Scripts

- `package-policy-bundle.sh [output-dir]`: gathers `docs/permission-bundle-digests.json` and referenced policy sources, computes a SHA-256 digest, and emits `policy-bundle-<digest>.tar.gz` plus metadata under the given directory (defaults to `build/policy-bundles`). Requires `jq` for manifest parsing.
- `publish-policy-bundle.sh <bundle-path>`: POSTs the bundle to the GitOps webhook specified by `POLICY_GATEWAY_URL` (with optional `POLICY_GATEWAY_TOKEN`). Used by CI/CD and Jenkins rollback jobs.
