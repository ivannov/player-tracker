#!/usr/bin/env bash
# First-time install into ~/.player-tracker. Builds the app and starts the prod stack.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"
source ./lib.sh

if [ -f "$ENV_FILE" ]; then
  echo "Already installed ($ENV_FILE exists). Use ./update.sh instead." >&2
  exit 1
fi

read -r -s -p "DB password for new install: " DB_PASSWORD
echo
if [ -z "$DB_PASSWORD" ]; then
  echo "DB password must not be empty." >&2
  exit 1
fi

mkdir -p "$BACKUPS_DIR"
umask 077
{
  echo "DB_USER=lineup"
  echo "DB_PASSWORD=$DB_PASSWORD"
} > "$ENV_FILE"

build_and_deploy
set_random_admin_password

echo
echo "==> Installed. App running at http://127.0.0.1:8080"
