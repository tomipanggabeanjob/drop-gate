#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -ne 1 ]; then
  echo "Usage: $0 <postgres-container-name-or-id>"
  exit 1
fi

CONTAINER_ID="$1"

if docker exec -i "$CONTAINER_ID" psql -U postgres -tc "SELECT 1 FROM pg_database WHERE datname = 'dropgate';" | grep -q 1; then
  echo "Database 'dropgate' already exists."
else
  docker exec -i "$CONTAINER_ID" psql -U postgres -c "CREATE DATABASE dropgate;"
  echo "Created database 'dropgate'."
fi
