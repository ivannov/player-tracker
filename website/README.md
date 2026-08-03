# lineup-tracker

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: https://quarkus.io/ .

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```shell script
./mvnw compile quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8080/q/dev/.

## Packaging and running the application

The application can be packaged using:
```shell script
./mvnw package
```
It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:
```shell script
./mvnw package -Dquarkus.package.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using: 
```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using: 
```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/lineup-tracker-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult https://quarkus.io/guides/maven-tooling.

## Local AI embeddings (Ollama)

Player-name disambiguation (LT-005) layers semantic matching on top of trigram similarity, using a
local [Ollama](https://ollama.com/) instance to compute name embeddings with the `nomic-embed-text`
model (768 dimensions, matching the `players.name_embedding vector(768)` column).

Start Ollama alongside the database and pull the model once:
```shell script
docker compose up -d ollama
docker compose exec ollama ollama pull nomic-embed-text
```

The app talks to Ollama at `http://localhost:11434` by default (override with the `OLLAMA_URL` env
var). If Ollama isn't running or the model hasn't been pulled yet, matching falls back to
trigram-only similarity automatically -- it's an enhancement layer, never a hard dependency.

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

## Related Guides

- REST Qute ([guide](https://quarkus.io/guides/qute-reference#rest_integration)): Qute integration for Quarkus REST. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on it.
- Scheduler ([guide](https://quarkus.io/guides/scheduler)): Schedule jobs and tasks
- Flyway ([guide](https://quarkus.io/guides/flyway)): Handle your database schema migrations
- REST Client ([guide](https://quarkus.io/guides/rest-client)): Call REST services
- JDBC Driver - PostgreSQL ([guide](https://quarkus.io/guides/datasource)): Connect to the PostgreSQL database via JDBC
- Security JPA ([guide](https://quarkus.io/guides/security-getting-started)): Secure your applications with username/password stored in a database via Jakarta Persistence
- Hibernate ORM with Panache ([guide](https://quarkus.io/guides/hibernate-orm-panache)): Simplify your persistence code for Hibernate ORM via the active record or the repository pattern
- REST ([guide](https://quarkus.io/guides/rest)): A Jakarta REST implementation utilizing build time processing and Vert.x. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on it.
