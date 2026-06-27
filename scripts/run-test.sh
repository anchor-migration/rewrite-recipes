#!/usr/bin/env bash
# Run rewrite-recipes unit tests in Docker.
exec "$(dirname "$0")/run-mvn.sh" -B test
