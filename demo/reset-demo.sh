#!/usr/bin/env bash
# Rebuild data/sems.db from schema.sql and demo/demo-seed.sql.
# Run from the repository root: ./demo/reset-demo.sh

set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$root"

if ! command -v sqlite3 >/dev/null 2>&1; then
    echo "sqlite3 is required but was not found on PATH." >&2
    exit 1
fi

if ! command -v lsof >/dev/null 2>&1; then
    echo "lsof is required to check whether anything is listening on port 8080." >&2
    exit 1
fi

if lsof -nP -iTCP:8080 -sTCP:LISTEN >/dev/null 2>&1; then
    echo "Refusing to reset: a process is listening on port 8080." >&2
    echo "Stop the SEMS application before replacing data/sems.db. SQLite should not be replaced while the app is running." >&2
    exit 1
fi

mkdir -p data

if [[ -f data/sems.db ]]; then
    timestamp="$(date +%Y%m%d-%H%M%S)"
    backup="data/sems-before-demo-reset-${timestamp}.db"
    if [[ -e "$backup" ]]; then
        echo "Refusing to reset: backup already exists at ${backup}." >&2
        exit 1
    fi
    cp -p data/sems.db "$backup"
    if [[ -f data/sems.db-wal ]]; then
        cp -p data/sems.db-wal "${backup}-wal"
    fi
    if [[ -f data/sems.db-shm ]]; then
        cp -p data/sems.db-shm "${backup}-shm"
    fi
    echo "Backed up data/sems.db to ${backup}"
    rm -f data/sems.db data/sems.db-wal data/sems.db-shm
else
    echo "No existing data/sems.db. Creating a new database."
fi

sqlite3 data/sems.db <<'EOF'
.bail on
PRAGMA foreign_keys = ON;
.read src/main/resources/schema.sql
.read demo/demo-seed.sql
EOF

echo "Row counts:"
sqlite3 -header -column data/sems.db <<'EOF'
.bail on
SELECT 'employees' AS table_name, COUNT(*) AS rows FROM employees
UNION ALL
SELECT 'users', COUNT(*) FROM users
UNION ALL
SELECT 'leave_requests', COUNT(*) FROM leave_requests
UNION ALL
SELECT 'audit_events', COUNT(*) FROM audit_events;
EOF
