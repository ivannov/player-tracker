# Local Production Stack Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Run the lineup-tracker app in Quarkus `%prod` mode on the developer's local
machine, backed by a persistent (non-dev-services) Postgres, with automated DB backups.

**Architecture:** A new `docker-compose.prod.yml` (separate from the existing dev
`docker-compose.yml`) runs four containers on one internal network — `db`
(pgvector/pgvector:pg17, persistent volume), `ollama`, `app` (built from the existing
`src/main/docker/Dockerfile.jvm`), and a `backup` sidecar (`prodrigestivill/postgres-backup-local`)
that dumps `db` on a schedule to a local bind-mounted folder. Only `app` publishes a
host port, bound to `127.0.0.1`.

**Tech Stack:** Quarkus 3.32.2 `%prod` profile, Docker Compose, existing
`pgvector/pgvector:pg17` and `ollama/ollama` images, `prodrigestivill/postgres-backup-local`.

## Global Constraints

- App port bound to `127.0.0.1` only — never `0.0.0.0` on the host side (container-internal `0.0.0.0` from `Dockerfile.jvm` is fine, only the host publish mapping matters).
- Real secrets (`DB_PASSWORD`) live only in `.env.prod`, which must be gitignored and never committed.
- `%prod` datasource and Ollama config must have **no defaults** — missing env vars must fail startup, not silently misconnect.
- Backup retention: 7 daily + 4 weekly, no monthly.
- No TLS, no reverse proxy, no off-machine backup copy, no systemd unit — explicitly out of scope per the spec.

---

### Task 1: Secrets scaffolding

**Files:**
- Create: `.env.prod.example`
- Modify: `.gitignore`

**Interfaces:**
- Produces: `.env.prod` (git-ignored, created by the developer by copying `.env.prod.example`) supplying `DB_USER` and `DB_PASSWORD` — consumed by `docker-compose.prod.yml` in Task 3 and by the `%prod` datasource config in Task 2 (via the `app` container's environment).

- [ ] **Step 1: Create the env template**

Create `.env.prod.example`:

```
DB_USER=lineup
DB_PASSWORD=change-me
```

- [ ] **Step 2: Add prod secrets and backups to .gitignore**

Add to `.gitignore` (append at end):

```

# Local production stack
.env.prod
backups/
```

- [ ] **Step 3: Verify gitignore works**

Run:
```bash
cp .env.prod.example .env.prod
git check-ignore -v .env.prod
mkdir -p backups && touch backups/test.sql
git check-ignore -v backups/test.sql
```
Expected: both commands print a match against the new `.gitignore` lines (confirms git will not track either path). Then:
```bash
rm -rf backups
```
(leave `.env.prod` in place — Task 2 will use it)

- [ ] **Step 4: Commit**

```bash
git add .env.prod.example .gitignore
git commit -m "Add prod secrets template and gitignore entries"
```

---

### Task 2: `%prod` datasource and Ollama config

**Files:**
- Modify: `src/main/resources/application.properties`

**Interfaces:**
- Consumes: env vars `DB_USER`, `DB_PASSWORD`, `DB_URL`, `OLLAMA_URL` (no code default — must be present in the process environment under the `prod` profile).
- Produces: a working `%prod` datasource + Ollama REST client config, consumed by the containerized `app` service in Task 3.

- [ ] **Step 1: Add the %prod block**

In `src/main/resources/application.properties`, immediately below the existing `%dev` datasource lines (after line 5, before the `quarkus.flyway.migrate-at-start=true` line), add:

```properties
%prod.quarkus.datasource.username=${DB_USER}
%prod.quarkus.datasource.password=${DB_PASSWORD}
%prod.quarkus.datasource.jdbc.url=${DB_URL}
```

And immediately below the existing `quarkus.rest-client.ollama-api.url=${OLLAMA_URL:http://localhost:11434}` line, add:

```properties
%prod.quarkus.rest-client.ollama-api.url=${OLLAMA_URL}
```

(The `%prod`-prefixed line overrides the unprefixed default in prod; there is no
`:default` fallback inside `${OLLAMA_URL}`/`${DB_URL}`/etc., so Quarkus throws a
config error at startup if the env var is unset — this is the fail-fast behavior
required by the global constraints.)

- [ ] **Step 2: Build the jar**

```bash
./mvnw package -DskipTests
```
Expected: `BUILD SUCCESS`, produces `target/quarkus-app/quarkus-run.jar`.

- [ ] **Step 3: Verify fail-fast with no env vars set**

```bash
env -u DB_USER -u DB_PASSWORD -u DB_URL -u OLLAMA_URL \
  java -Dquarkus.profile=prod -jar target/quarkus-app/quarkus-run.jar
```
Expected: process fails to start with a config error mentioning the missing
property (e.g. `SRCFG00011` / "could not expand value" for one of `DB_USER`,
`DB_PASSWORD`, `DB_URL`, or `OLLAMA_URL`) — confirms no silent default.

- [ ] **Step 4: Verify successful connection using the existing dev containers**

Bring up the existing dev DB/Ollama containers (already defined in the repo's
`docker-compose.yml`) so there's something real to connect to on `localhost`:

```bash
docker compose up -d db ollama
```

Then run the prod-profile jar against them:

```bash
DB_USER=lineup DB_PASSWORD=lineup DB_URL=jdbc:postgresql://localhost:5432/lineup \
  OLLAMA_URL=http://localhost:11434 \
  java -Dquarkus.profile=prod -jar target/quarkus-app/quarkus-run.jar
```
Expected: log line `Listening on: http://0.0.0.0:8080` with no errors. In another
terminal:
```bash
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/login
```
Expected: `200`. Stop the jar with Ctrl-C when confirmed.

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/application.properties
git commit -m "Add %prod datasource and Ollama config"
```

---

### Task 3: `docker-compose.prod.yml` — db, ollama, app

**Files:**
- Create: `docker-compose.prod.yml`
- Modify: `README.md`

**Interfaces:**
- Consumes: `.env.prod` (Task 1) for `DB_USER`/`DB_PASSWORD` compose-level substitution; the `%prod` config from Task 2; `src/main/docker/Dockerfile.jvm` (pre-existing, unmodified) as the app build target.
- Produces: a running stack reachable at `http://127.0.0.1:8080`, consumed by the `backup` service added in Task 4 (which depends on the same `db` service).

- [ ] **Step 1: Write the compose file**

Create `docker-compose.prod.yml`:

```yaml
services:
  db:
    image: pgvector/pgvector:pg17
    restart: unless-stopped
    environment:
      POSTGRES_USER: ${DB_USER}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
      POSTGRES_DB: lineup
    volumes:
      - db-data-prod:/var/lib/postgresql/data

  ollama:
    image: ollama/ollama:latest
    restart: unless-stopped
    volumes:
      - ollama-data-prod:/root/.ollama

  app:
    build:
      context: .
      dockerfile: src/main/docker/Dockerfile.jvm
    restart: unless-stopped
    depends_on:
      - db
      - ollama
    env_file:
      - .env.prod
    environment:
      DB_URL: jdbc:postgresql://db:5432/lineup
      OLLAMA_URL: http://ollama:11434
    ports:
      - "127.0.0.1:8080:8080"

volumes:
  db-data-prod:
  ollama-data-prod:
```

- [ ] **Step 2: Stop the ad-hoc dev containers from Task 2 to avoid a port clash**

```bash
docker compose down
```

- [ ] **Step 3: Build and start the prod stack**

```bash
./mvnw package -DskipTests
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --build
```
Expected: three containers (`db`, `ollama`, `app`) start and stay running (`docker compose --env-file .env.prod -f docker-compose.prod.yml ps` shows all `Up`).

- [ ] **Step 4: Verify the app is reachable and functional**

```bash
curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1:8080/login
```
Expected: `200`. Then confirm it's NOT reachable from outside localhost binding by checking the port is bound to `127.0.0.1` only:
```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml port app 8080
```
Expected output starts with `127.0.0.1:`.

- [ ] **Step 5: Verify data survives a container restart**

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml restart db
sleep 5
curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1:8080/login
```
Expected: `200` again (app reconnects, seeded schema/data still present — confirms the named volume persisted and no `clean-at-start` ran, since that stays `%dev`-only).

- [ ] **Step 6: Document the run steps in README**

Add a new section to `README.md` (after any existing "Running locally"/dev setup
section, or at the end if none exists):

```markdown
## Run in production locally

This runs the app in Quarkus `%prod` mode against a persistent (non-dev-services)
Postgres, all in Docker, bound to `127.0.0.1` only.

1. Copy `.env.prod.example` to `.env.prod` and set a real `DB_PASSWORD`.
2. Build the jar: `./mvnw package -DskipTests`
3. Bring up the stack: `docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --build`
4. Open http://127.0.0.1:8080 and log in as `admin` (see the seeded password hash
   in `V1__create_teams.sql`) — **change the password immediately after first login**.
5. Stop the stack: `docker compose --env-file .env.prod -f docker-compose.prod.yml down`
   (data persists in the `db-data-prod` volume; use `down -v` only if you want to wipe it).
```

- [ ] **Step 7: Commit**

```bash
git add docker-compose.prod.yml README.md
git commit -m "Add docker-compose.prod.yml for local production stack"
```

---

### Task 4: Backup sidecar

**Files:**
- Modify: `docker-compose.prod.yml`
- Modify: `README.md`

**Interfaces:**
- Consumes: `db` service from Task 3 (same compose network, same `DB_USER`/`DB_PASSWORD`).
- Produces: dump files under `./backups/` on the host, per the retention in Global Constraints.

- [ ] **Step 1: Add the backup service**

In `docker-compose.prod.yml`, add a `backup` service (alongside `db`/`ollama`/`app`):

```yaml
  backup:
    image: prodrigestivill/postgres-backup-local:17
    restart: unless-stopped
    depends_on:
      - db
    environment:
      POSTGRES_HOST: db
      POSTGRES_DB: lineup
      POSTGRES_USER: ${DB_USER}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
      SCHEDULE: "@daily"
      BACKUP_KEEP_DAYS: 7
      BACKUP_KEEP_WEEKS: 4
      BACKUP_KEEP_MONTHS: 0
    volumes:
      - ./backups:/backups
```

- [ ] **Step 2: Start the backup service**

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d backup
```
Expected: `backup` container starts and stays running.

- [ ] **Step 3: Trigger a manual backup and verify output**

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml exec backup /backup.sh
ls -la backups/daily/
```
Expected: a `.sql.gz` dump file under `backups/daily/` with a recent timestamp.

- [ ] **Step 4: Document backup/restore in README**

Append to the "Run in production locally" section added in Task 3:

```markdown

### Backups

- Automatic: the `backup` service dumps the `db` container daily, keeping 7 daily
  and 4 weekly copies under `./backups/` (gitignored).
- Manual trigger: `docker compose --env-file .env.prod -f docker-compose.prod.yml exec backup /backup.sh`
- Restore from a dump:
  ```bash
  gunzip -c backups/daily/<dump-file>.sql.gz | \
    docker compose --env-file .env.prod -f docker-compose.prod.yml exec -T db \
    psql -U "$DB_USER" -d lineup
  ```
```

- [ ] **Step 5: Commit**

```bash
git add docker-compose.prod.yml README.md
git commit -m "Add automated DB backup sidecar to local production stack"
```
