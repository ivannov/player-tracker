# Install & Update (local production stack)

The "production" deployment is a Docker Compose stack on your own machine
(`docker-compose.prod.yml`). It has four services:

| Service | Image | Purpose |
|---------|-------|---------|
| `db` | `pgvector/pgvector:pg17` | PostgreSQL; data in the named volume `db-data-prod` |
| `ollama` | `ollama/ollama` | embedding model for player-name matching; volume `ollama-data-prod` |
| `app` | built from `src/main/docker/Dockerfile.jvm` | the Quarkus app in `%prod` mode, bound to **127.0.0.1:8080 only** |
| `backup` | `prodrigestivill/postgres-backup-local:17` | daily `pg_dump` (7 daily + 4 weekly copies) |

Unlike dev mode, `%prod` never cleans the schema. It only runs pending Flyway migrations at startup.

## What lives where

| Path | Content |
|------|---------|
| your git checkout (`website/`) | source code; the stack is built from here |
| `~/.player-tracker/.env.prod` | `DB_USER` / `DB_PASSWORD` (mode 600, created by `install.sh`) |
| `~/.player-tracker/backups/` | `daily/`, `weekly/`, `monthly/`, `last/` SQL dumps |
| Docker volume `db-data-prod` | the live database |

Everything below uses this shell helper. Put it in your `~/.bashrc` and adjust the checkout path:

```bash
pt() {
  BACKUPS_DIR=~/.player-tracker/backups docker compose \
    --env-file ~/.player-tracker/.env.prod \
    -f ~/projects/ivannov/player-tracker/website/docker-compose.prod.yml "$@"
}
```

## Prerequisites

JDK 21 (with `jshell`), Docker + Compose v2, `openssl`, and a git checkout of the repository.

## First install

```bash
cd website
scripts/install.sh
```

The script does the following:

1. Refuses to run if `~/.player-tracker/.env.prod` already exists. In that case, use `update.sh`.
2. Asks for a database password and writes `~/.player-tracker/.env.prod`.
3. Builds the jar (`./mvnw package -DskipTests`) and runs `docker compose up -d --build`.
4. Pulls `nomic-embed-text` into the Ollama container, retrying while the container starts up.
5. Generates a random `admin` password, stores its bcrypt hash, and **prints the plaintext once**.
   Save it now.

Open http://127.0.0.1:8080 and log in as `admin`. Next, follow
[First season: getting data in](first-season.md).

The `DB_PASSWORD` only takes effect when Postgres initializes an empty volume. Editing
`.env.prod` later doesn't change the database password. You'd have to `ALTER ROLE` inside Postgres,
or wipe the volume.

## Update to a new version

```bash
cd website
git pull
pt exec backup /backup.sh     # take a fresh backup first (recommended)
scripts/update.sh
```

`update.sh` rebuilds the jar, rebuilds and restarts the containers, and re-pulls the embedding
model. Data in `db-data-prod` is kept. Any new Flyway migrations (`V2__…`, `V3__…`) run when the app
starts. If `~/.player-tracker/.env.prod` is missing, it runs `install.sh` instead.

After updating:

```bash
pt ps                         # all four services "running"
pt logs --tail=100 app        # look for Flyway "Successfully applied N migrations" / no errors
```

**If the app won't start after an update** and the log mentions a Flyway *checksum mismatch* or
*validate failed*, an already-applied migration file was edited. See the schema-change rule in
[Developer setup](developer-setup.md#conventions-worth-knowing-before-you-change-things). Restore that
file from the previous commit and move the change into a new `V<n>__` migration.

## Day-to-day operations

| Task | Command |
|------|---------|
| Status | `pt ps` |
| Logs | `pt logs -f app` |
| Stop (data kept) | `pt down` |
| Start again | `pt up -d` |
| psql shell | `pt exec db sh -c 'psql -U "$POSTGRES_USER" -d lineup'` |

**Never run `pt down -v`** unless you mean to delete the database. The `-v` flag removes the
`db-data-prod` volume.

The containers use `restart: unless-stopped`, so they come back when Docker starts. The nightly
extraction only runs while the stack is up. Days when the machine was off have to be backfilled by
hand (see [First season → Backfilling](first-season.md#backfilling-past-rounds)).

## Backups and restore

- Automatic: daily dumps go to `~/.player-tracker/backups/`, keeping 7 daily and 4 weekly copies,
  plus a rolling monthly copy.
- Manual: `pt exec backup /backup.sh`
- Restore into a fresh, empty database (overwrites current data):

  ```bash
  pt down
  docker volume rm website_db-data-prod     # check the exact name with: docker volume ls
  pt up -d db && sleep 5
  gunzip -c ~/.player-tracker/backups/daily/<dump>.sql.gz \
    | pt exec -T db sh -c 'psql -U "$POSTGRES_USER" -d lineup'
  pt up -d
  ```

  Restoring into an empty database matters. The app must not start first, because Flyway would
  create the schema and the dump would then collide with it. That's why only `db` is started
  before the restore.

## Managing accounts

There's no user-management UI. Every account change is SQL run through
`pt exec -T db sh -c 'psql -U "$POSTGRES_USER" -d lineup'`.

**Generate a bcrypt hash** (from the checkout):

```bash
CP=$(./mvnw -q dependency:build-classpath -Dmdep.outputFile=/dev/stdout)
echo 'System.out.println(io.quarkus.elytron.security.common.BcryptUtil.bcryptHash("new-password"));' \
  | jshell --class-path "$CP" -q -
```

**Reset the admin password.** Use a *quoted* heredoc, so the shell doesn't expand the `$` signs in
the hash:

```bash
pt exec -T db sh -c 'psql -U "$POSTGRES_USER" -d lineup' <<'SQL'
UPDATE users SET password='<hash>' WHERE username='admin';
SQL
```

**Add a user** (`USER` for read-only, `ADMIN` for full access):

```bash
pt exec -T db sh -c 'psql -U "$POSTGRES_USER" -d lineup' <<'SQL'
INSERT INTO users (version, created_at, last_updated, username, password)
VALUES (0, NOW(), NOW(), 'maria', '<hash>');
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r WHERE u.username = 'maria' AND r.name = 'ADMIN';
SQL
```

Every page is publicly readable anyway, so a `USER` account adds little beyond the search home page.

## Uninstall

```bash
pt down -v                     # removes containers AND data volumes
rm -rf ~/.player-tracker       # removes env file and backups. Copy the backups elsewhere first!
```
