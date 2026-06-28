#!/usr/bin/env bash
# Run Maven inside Docker — no host JDK/Maven required.
# Optional: -Preset com.anchor.migration.presets.DukesBankStackMigration (see docs/rewrite-presets.md)
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

if ! docker info >/dev/null 2>&1; then
  echo "Docker is not available. Install Docker and ensure the daemon is running." >&2
  exit 1
fi

PRESET=""
MAVEN_ARGS=()

while [[ $# -gt 0 ]]; do
  case "$1" in
    -Preset)
      PRESET="$2"
      shift 2
      ;;
    *)
      MAVEN_ARGS+=("$1")
      shift
      ;;
  esac
done

if [[ ${#MAVEN_ARGS[@]} -eq 0 ]]; then
  MAVEN_ARGS=(-B test)
fi

if [[ -n "$PRESET" ]]; then
  MAVEN_ARGS+=("-Danchor.rewrite.preset=$PRESET")
fi

echo "==> rewrite-recipes (Docker Maven)"
echo "    mvn ${MAVEN_ARGS[*]}"
docker compose run --rm mvn mvn "${MAVEN_ARGS[@]}"
