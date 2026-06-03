#!/usr/bin/env bash
#
# Run the full test suite in Docker — backend (Quarkus + Postgres) and web (Vitest).
# Requires only Docker; no local JDK or Node needed.
#
# Usage:
#   ./scripts/run-tests.sh            # run backend + web tests
#   ./scripts/run-tests.sh backend    # backend only
#   ./scripts/run-tests.sh web        # web only
#
# Exits non-zero if any selected suite fails.
set -euo pipefail

cd "$(dirname "$0")/.."
compose=(docker compose -f docker-compose.test.yml)

target="${1:-all}"
case "$target" in
  all|backend|web) ;;
  *) echo "usage: $0 [all|backend|web]" >&2; exit 2 ;;
esac

# Always tear down the stack (and its volumes) on exit so reruns start clean.
cleanup() { "${compose[@]}" down -v --remove-orphans >/dev/null 2>&1 || true; }
trap cleanup EXIT

backend_rc=0
web_rc=0

if [[ "$target" == "all" || "$target" == "backend" ]]; then
  echo "▶ Backend tests (Quarkus + PostgreSQL)…"
  "${compose[@]}" run --rm backend-tests || backend_rc=$?
fi

if [[ "$target" == "all" || "$target" == "web" ]]; then
  echo "▶ Web tests (Vitest)…"
  "${compose[@]}" run --rm web-tests || web_rc=$?
fi

echo
echo "──────── summary ────────"
[[ "$target" == "all" || "$target" == "backend" ]] && \
  echo "backend: $([[ $backend_rc -eq 0 ]] && echo PASS || echo "FAIL ($backend_rc)")"
[[ "$target" == "all" || "$target" == "web" ]] && \
  echo "web:     $([[ $web_rc -eq 0 ]] && echo PASS || echo "FAIL ($web_rc)")"

[[ $backend_rc -eq 0 && $web_rc -eq 0 ]]
