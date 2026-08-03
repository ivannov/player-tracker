# Local production stack — design

## Goal
Run the lineup-tracker app in Quarkus `%prod` mode on the developer's local machine,
backed by a persistent (non-dev-services) Postgres, with automated DB backups —
without introducing infrastructure the single-machine setup doesn't need (no TLS,
no reverse proxy, no off-machine backup copy, no orchestrator).

## Architecture

Four containers on one docker-compose stack, all on an internal bridge network.
Only the app port is published to the host, and only on `127.0.0.1`.

```
[host:127.0.0.1:8080] -> app (Dockerfile.jvm, %prod profile)
                            +- db (pgvector/pgvector:pg17, volume db-data-prod)
                            +- ollama (embeddings)
db <- backup sidecar (prodrigestivill/postgres-backup-local) -> ./backups/ (bind mount, gitignored)
```

`db` and `ollama` publish no host port — the `app` container reaches them by
service-name DNS on the compose network, same pattern the existing dev
`docker-compose.yml` already uses for host access.

## Components / files

- **`docker-compose.prod.yml`** (new, separate from dev `docker-compose.yml`):
  - `db`: `pgvector/pgvector:pg17`, named volume `db-data-prod`, `restart: unless-stopped`, no host port.
  - `ollama`: `ollama/ollama:latest`, named volume `ollama-data-prod`, `restart: unless-stopped`, no host port.
  - `app`: built from existing `src/main/docker/Dockerfile.jvm`, `restart: unless-stopped`,
    port mapped `127.0.0.1:8080:8080`, `env_file: .env.prod`, `depends_on: [db, ollama]`.
  - `backup`: `prodrigestivill/postgres-backup-local`, `depends_on: [db]`, bind-mounts
    `./backups:/backups`, schedule + retention (7 daily / 4 weekly) from env vars.

- **`.env.prod.example`** — committed template (`DB_USER=`, `DB_PASSWORD=`, `DB_URL=`,
  `OLLAMA_URL=`, `BACKUP_SCHEDULE=`, retention counts). **`.env.prod`** — real secrets,
  gitignored, used as the compose `env_file` for `app` and `backup`/`db`.

- **`application.properties`** — add a `%prod` block:
  `%prod.quarkus.datasource.username/password/jdbc.url` from
  `${DB_USER}/${DB_PASSWORD}/${DB_URL}`, **no defaults** (fail fast instead of silently
  misconnecting if `.env.prod` isn't wired up). Same pattern for the prod Ollama URL,
  since it must resolve to the compose service name `ollama`, not `localhost`.

- **`.gitignore`** — add `.env.prod`, `backups/`.

- **`README.md`** — "Run in production locally" section: build the jar, bring the
  stack up, restore-from-backup command, reminder to change the seeded `admin`
  password after first login (V1 migration already seeds one and already flags
  this in a comment).

## Data flow / operational notes

- **Build**: `./mvnw package` (fast-jar, already the project default) produces
  `target/quarkus-app/`; `docker compose -f docker-compose.prod.yml build app`
  picks that up per the existing `Dockerfile.jvm`.
- **Backups**: sidecar runs `pg_dump` on its own cron (env-configured schedule),
  writes to `./backups/`, rotation (7 daily + 4 weekly) handled by the image itself —
  no host cron, no custom script to maintain.
- **Migrations**: `quarkus.flyway.migrate-at-start=true` is unprefixed, so it already
  applies under `%prod`. The `%dev`-only `clean-at-start`/`repair-at-start` correctly
  stay dev-only, so prod data is never wiped on container restart.
- **Restore path**: documented in README — `docker exec -i <db-container> psql ... <
  backups/<file>.sql` (or the backup image's bundled restore script).

## Testing / verification

- Bring the stack up (`docker compose -f docker-compose.prod.yml up -d --build`),
  hit `http://127.0.0.1:8080`, confirm login works with the seeded `admin` account.
- Trigger a manual backup run, confirm a dump file appears under `./backups/`.
- `docker compose restart db` — confirm data survives (schema + seeded rows still
  present via the UI).

## Explicitly out of scope (skipped, add if needed later)

- TLS / reverse proxy — not needed for localhost-only access.
- Off-machine backup copy (S3, rsync to another host) — single local dump is enough
  for now.
- systemd unit for the app — compose already gives `restart: unless-stopped`.
