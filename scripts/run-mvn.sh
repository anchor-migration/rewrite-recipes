#!/usr/bin/env bash
# Run Maven inside Docker — no host JDK/Maven required.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

if ! docker info >/dev/null 2>&1; then
  echo "Docker is not available. Install Docker and ensure the daemon is running." >&2
  exit 1
fi

if [[ $# -eq 0 ]]; then
  set -- -B test
fi

echo "==> rewrite-recipes (Docker Maven)"
echo "    mvn $*"
docker compose run --rm mvn mvn "$@"
