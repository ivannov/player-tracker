#!/usr/bin/env bash
# Shared vars/functions for install.sh and update.sh. Not meant to be run directly.
set -euo pipefail

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
INSTALL_DIR="$HOME/.player-tracker"
ENV_FILE="$INSTALL_DIR/.env.prod"
BACKUPS_DIR="$INSTALL_DIR/backups"
COMPOSE_FILE="$REPO_DIR/docker-compose.prod.yml"

compose() {
  BACKUPS_DIR="$BACKUPS_DIR" docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" "$@"
}

build_and_deploy() {
  echo "==> Building jar ($REPO_DIR)"
  (cd "$REPO_DIR" && ./mvnw package -DskipTests)

  echo "==> Building and starting containers"
  compose up -d --build

  echo "==> Pulling ollama embedding model (nomic-embed-text)"
  # ponytail: fixed retry count for ollama container startup lag, bump if flaky on slower hardware
  for i in 1 2 3 4 5; do
    if compose exec -T ollama ollama pull nomic-embed-text; then
      break
    fi
    sleep 2
  done
}

# Sets the seeded admin account's password to a random value and prints it once.
set_random_admin_password() {
  local password hash
  password="$(openssl rand -hex 12)"

  echo "==> Hashing admin password"
  local cp
  cp="$(cd "$REPO_DIR" && ./mvnw -q dependency:build-classpath -Dmdep.outputFile=/dev/stdout)"
  hash="$(echo "System.out.println(io.quarkus.elytron.security.common.BcryptUtil.bcryptHash(\"$password\"));" \
    | jshell --class-path "$cp" -q -)"

  echo "==> Setting admin password"
  # ponytail: fixed retry count for db startup lag, same shape as the ollama wait above
  local i ok=false
  for i in 1 2 3 4 5; do
    if printf 'UPDATE users SET password=%s WHERE username=%s;\n' "'$hash'" "'admin'" \
      | compose exec -T db sh -c 'psql -U "$POSTGRES_USER" -d lineup'; then
      ok=true
      break
    fi
    sleep 2
  done
  [ "$ok" = true ] || { echo "Failed to set admin password." >&2; return 1; }

  cat <<EOF

==> admin password: $password
    (shown once -- save it now)
EOF
}
