# Developer Setup

## Prerequisites

| Tool | Why |
|------|-----|
| JDK 21 (full JDK with `jshell`) | build/run; `jshell` is used to generate bcrypt password hashes |
| Docker + Docker Compose v2 | dev PostgreSQL (with pgvector) and Ollama; Testcontainers for tests |
| Git | — |

No local Maven needed — use the wrapper `./mvnw`. All commands below run from the `website/` directory.

## Stack at a glance

Quarkus 3 (Java 21) · Hibernate ORM Panache · Flyway · PostgreSQL 17 + `pg_trgm` + `pgvector` ·
Qute templates + HTMX + Pico CSS · Jsoup scrapers · Quarkus Scheduler · Ollama (`nomic-embed-text`)
for optional semantic name matching.

```
src/main/java/com/nosoftskills/lineup/
  model/         JPA entities (all extend TrackerEntity: id, version, created_at, last_updated)
  resource/      CRUD resources: /teams /competitions /participations /players /matches, login, landing
  participation/ Participation import wizard (/participations/import)
  extraction/    Match extraction wizard (/matches/extract) + ScheduledExtractionJob (23:00 daily)
  scraping/      Jsoup scrapers for bfu-tournaments.com and ebfu.net
  matching/      Team/player name resolution (aliases, trigram, Ollama embeddings) + embedding sync job
  inbox/         Ambiguity inbox (/inbox)
  security/      CurrentUser helper
src/main/resources/
  db/migration/V1__create_teams.sql   the whole schema + seeded roles and admin user
  templates/                          Qute templates, one folder per resource
```

## Run in dev mode

```bash
docker compose up -d db ollama                               # Postgres on :5432, Ollama on :11434
docker compose exec ollama ollama pull nomic-embed-text      # once; optional (see below)
./mvnw quarkus:dev
```

App: http://localhost:8080 · Dev UI: http://localhost:8080/q/dev/

Dev mode connects to the compose Postgres (`lineup`/`lineup` on `localhost:5432`, overridable via
`DB_URL`, `DB_USER`, `DB_PASSWORD`).

Ollama is optional. Without it (or without the model pulled), player matching falls back to trigram
similarity only and `PlayerEmbeddingSyncJob` logs a warning every minute. Override its address with
`OLLAMA_URL`.

### The dev database is disposable

`%dev.quarkus.flyway.clean-at-start=true` **drops and rebuilds the schema every time the app
starts**. That lets you edit `V1__create_teams.sql` freely during development. It also means
anything you import in dev disappears when the app restarts. Never keep data you care about in the
dev DB. For a persistent instance, use the [local production stack](install-and-update.md).

### Logging in as admin in dev

Migration V1 seeds an `admin` user (role ADMIN), but nobody knows the plaintext of its password
hash. After each startup (because the schema is rebuilt), set a password you know:

```bash
CP=$(./mvnw -q dependency:build-classpath -Dmdep.outputFile=/dev/stdout)
HASH=$(echo 'System.out.println(io.quarkus.elytron.security.common.BcryptUtil.bcryptHash("admin"));' \
  | jshell --class-path "$CP" -q -)
docker compose exec -T db psql -U lineup -d lineup \
  -c "UPDATE users SET password='$HASH' WHERE username='admin';"
```

Keep `HASH` (or the commands) in your shell history. Only the last command needs re-running after
a restart.

### Triggering scheduled jobs by hand

Dev UI → **Scheduler** lists both jobs with an **Execute** button:

- `ScheduledExtractionJob.extractToday`: normally runs at 23:00 daily.
- `PlayerEmbeddingSyncJob.syncMissingEmbeddings`: runs every minute.

## Tests

```bash
./mvnw test
```

Tests are `@QuarkusTest` + REST Assured. The test profile has no datasource configured, so Quarkus
Dev Services starts a throwaway `pgvector/pgvector:pg17` container, which means **Docker must be
running**. Scheduler jobs are disabled in tests (`%test.quarkus.scheduler.enabled=false`); tests
call the job methods directly. Scraper tests parse saved HTML fixtures and don't hit the network.

Conventions (from `CLAUDE.md`): use `@TestSecurity(user=..., roles={...})` for auth; add
`.redirects().follow(false)` when asserting 303/403/204; `Response.seeOther` is **303**; set up data
with `QuarkusTransaction.requiringNew()`.

## Build

```bash
./mvnw package                 # target/quarkus-app/quarkus-run.jar (+ lib/)
./mvnw package -DskipTests     # what the install/update scripts run
```

`src/main/docker/Dockerfile.jvm` packages that output into the image used by
`docker-compose.prod.yml`.

## Conventions worth knowing before you change things

The full list lives in [`CLAUDE.md`](../CLAUDE.md). The ones that bite most often:

- **Schema changes.** Until the first production deployment, V1 is edited in place. **Once a
  production instance exists, never edit V1 again.** Add `V2__...sql`, `V3__...sql` instead.
  Production runs Flyway `migrate-at-start` without `clean`/`repair`, so a changed V1 checksum makes
  the production app refuse to start.
- `@Column(length = N)` must match `VARCHAR(N)` in the migration (Hibernate schema validation).
- camelCase `@ManyToOne` fields need an explicit `@JoinColumn(name = "snake_case")`.
- Use `@RestForm`, not `@FormParam` (`quarkus-rest` reactive stack).
- Security is public-read / admin-write: list and detail `GET`s are open, and everything else is
  `@RolesAllowed("ADMIN")`. List templates hide admin controls behind `isAdmin`.
- In Qute HTML attributes, write `\d\d\d\d`, not `\d{4}`.
- BFU-hosted `<img>` tags need `referrerpolicy="no-referrer"`.

## Project tooling

- **Backlog**: tasks live in `.backlog/` and are managed through Backlog.md (MCP or `backlog` CLI).
- **Experience playbook**: domain traps and validated fixes live in `experiences/`, managed with
  `python3 manage-experience/scripts/experiences.py` (`list-categories`, `get-frontmatter`,
  `read-experience`, `create-experience`). Check it before non-trivial Hibernate, Qute, testing or
  scraping work.
- **Manual QA**: [`manual-test-plan.md`](manual-test-plan.md) holds the end-to-end test plan and
  the results of the last run.
