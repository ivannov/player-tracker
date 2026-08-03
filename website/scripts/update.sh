#!/usr/bin/env bash
# Rebuilds and redeploys the prod stack. Installs first if ~/.player-tracker isn't set up yet.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"
source ./lib.sh

if [ ! -f "$ENV_FILE" ]; then
  echo "Not installed yet ($ENV_FILE missing) -- running install.sh instead."
  exec ./install.sh
fi

build_and_deploy

echo
echo "==> Updated. App running at http://127.0.0.1:8080"
