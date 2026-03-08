# Lineup Tracker — Project Instructions

## Project Overview
Web app that scrapes starting lineups from the Bulgarian Football Union (BFU) site for elite youth leagues and the top 3 men's leagues. Exposes a web UI with HTMX.

## Stack
- **Backend**: Quarkus 3.x, Java 21, Hibernate ORM Panache
- **Frontend**: Qute templates + HTMX + Pico CSS
- **Database**: PostgreSQL via JDBC, migrations with Flyway
- **Scraping**: Jsoup
- **Auth**: JPA-based form login, roles: ADMIN and USER
- **Scheduler**: Quarkus Scheduler — runs lineup extraction daily at 23:00

## Features
1. On-demand lineup extraction for a given date (all competitions)
2. Scheduled daily extraction at 23:00
3. Player history search
4. Team statistics
5. Login with ADMIN / USER roles

## Key Conventions
- Package root: `com.nosoftskills.lineup`
- DB migrations in `src/main/resources/db/migration/`
- Qute templates in `src/main/resources/templates/`
- REST resources under `src/main/java/.../resource/`
- Services under `src/main/java/.../service/`
- JPA entities under `src/main/java/.../model/`

## Entity Conventions
- All entities extend `TrackerEntity` (not `PanacheEntity` directly)
- `TrackerEntity` provides: `id` (BIGSERIAL), `version` (@Version for optimistic locking), `createdAt`, `lastUpdated`
- All DB tables include the same four base columns: `id`, `version`, `created_at`, `last_updated`
- Enum columns stored as `VARCHAR` with `@Enumerated(EnumType.STRING)`
- Relationships use `FetchType.LAZY` by default; `EAGER` only where necessary (e.g. security roles)

## Migration Conventions
- Single file `V1__create_teams.sql` holds the full initial schema (keep appending until first production deployment)
- Table order must respect foreign key dependencies
- All text columns are UTF-8 and support Bulgarian Cyrillic — DB must be created with `ENCODING 'UTF8'`
- Roles (`ADMIN`, `USER`) are seeded in the migration
