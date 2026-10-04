#!/usr/bin/env bash
#
# Run the full test suite in Docker — backend tests, web tests/lint/build, and admin lint/build.
# Requires only Docker; no local JDK or Node needed.
#
# Usage:
#   ./scripts/run-tests.sh            # run all checks
#   ./scripts/run-tests.sh backend    # backend only
#   ./scripts/run-tests.sh web        # web only
#   ./scripts/run-tests.sh admin      # admin only
#
# Exits non-zero if any selected suite fails.
set -euo pipefail

cd "$(dirname "$0")/.."
compose=(docker compose -f docker-compose.test.yml)

target="${1:-all}"
case "$target" in
  all|backend|web|admin) ;;
  *) echo "usage: $0 [all|backend|web|admin]" >&2; exit 2 ;;
esac

# Tear down the ephemeral DB and network; retain dependency/build caches.
cleanup() { "${compose[@]}" down --remove-orphans >/dev/null 2>&1 || true; }
trap cleanup EXIT

backend_rc=0
web_rc=0
admin_rc=0

if [[ "$target" == "all" || "$target" == "backend" ]]; then
  echo "▶ Backend tests (Quarkus + PostgreSQL)…"
  "${compose[@]}" run --rm backend-tests || backend_rc=$?
fi

if [[ "$target" == "all" || "$target" == "web" ]]; then
  echo "▶ Web tests (Vitest)…"
  "${compose[@]}" run --rm web-tests || web_rc=$?
fi

if [[ "$target" == "all" || "$target" == "admin" ]]; then
  echo "▶ Admin lint and build…"
  "${compose[@]}" run --rm admin-checks || admin_rc=$?
fi

echo
echo "──────── summary ────────"
[[ "$target" == "all" || "$target" == "backend" ]] && \
  echo "backend: $([[ $backend_rc -eq 0 ]] && echo PASS || echo "FAIL ($backend_rc)")"
[[ "$target" == "all" || "$target" == "web" ]] && \
  echo "web:     $([[ $web_rc -eq 0 ]] && echo PASS || echo "FAIL ($web_rc)")"

[[ "$target" == "all" || "$target" == "admin" ]] && \
  echo "admin:   $([[ $admin_rc -eq 0 ]] && echo PASS || echo "FAIL ($admin_rc)")"

[[ $backend_rc -eq 0 && $web_rc -eq 0 && $admin_rc -eq 0 ]]
