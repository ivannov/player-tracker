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
- **`@JoinColumn` required for camelCase FK fields**: Hibernate does not convert camelCase to snake_case for FK column names. Any `@ManyToOne` field whose Java name contains multiple words (e.g. `homeTeam`, `awayTeam`, `teamFormation`) must have `@JoinColumn(name = "snake_case_col")` explicitly, otherwise Hibernate generates the wrong column name (e.g. `homeTeam_id` instead of `home_team_id`).
- **`@Column(length=...)` must match migration DDL**: Hibernate schema validation compares declared length to the DB column. Always set `length` on `@Column` to match the `VARCHAR(N)` in the SQL migration (e.g. `logo_url VARCHAR(512)` → `@Column(length = 512)`).

## Migration Conventions
- Single file `V1__create_teams.sql` holds the full initial schema (keep appending until first production deployment)
- Table order must respect foreign key dependencies
- All text columns are UTF-8 and support Bulgarian Cyrillic — DB must be created with `ENCODING 'UTF8'`
- Roles (`ADMIN`, `USER`) are seeded in the migration
- `%dev.quarkus.flyway.repair-at-start=true` is set so that local schema edits to V1 don't break startup during development

## Resource / URL Conventions
- Management resources live at top-level paths: `/teams`, `/competitions`, `/team-formations`, `/participations`
- Security is enforced via `@Authenticated` (GET) and `@RolesAllowed("ADMIN")` (POST/DELETE) on the resource class/methods — no URL-pattern config needed
- Form parameters use `@RestForm` (from `org.jboss.resteasy.reactive`), not `@FormParam` — this project uses `quarkus-rest` (reactive stack)
- List pages that traverse lazy associations use JOIN FETCH JPQL to avoid N+1

## Template Conventions
- Shared nav extracted to `templates/tags/appNav.html` — takes `{@String username}` parameter; invoke as `{#appNav username=username /}`
- **Avoid `{N}` quantifiers in Qute HTML attributes** — Qute interprets `{4}` as a template expression. Use explicit repetition instead: `\d\d\d\d` not `\d{4}`
- BFU image URLs require `referrerpolicy="no-referrer"` on `<img>` tags (hotlink protection)

## FormationType Labels
`FIRST=""`, `SECOND="II"`, `THIRD="III"` — first team has no suffix; second/third use Roman numerals

## Test Conventions
- `@QuarkusTest` + REST Assured + `@TestSecurity(user=..., roles={...})` for resource tests
- Always add `.redirects().follow(false)` on POST/DELETE calls when asserting 303/403/204 — REST Assured follows redirects by default
- `Response.seeOther(URI)` returns **303**, not 302 — assert `statusCode(303)`
- Test data setup/teardown uses `QuarkusTransaction.requiringNew().call(...)` / `.run(...)`
- Test profile uses Quarkus Dev Services (PostgreSQL container) — DB credentials are in `%dev` profile only, leaving test profile unconfigured so Dev Services activates
