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

## AI Unified Process (mandatory for every change)
This project follows the [AI Unified Process](https://unifiedprocess.ai) (plugin `aiup-core`). The specification in `docs/` is the source of truth for behavior; code follows the spec, never the other way round. It applies to **every** change: new features, enhancements, and bug fixes.

### Artifacts
- `docs/use_cases.puml`: actors and use cases (`UC-001`…`UC-013`)
- `docs/use_cases/UC-XXX-<kebab-name>.md`: one spec per use case (steps, alternative flows `A<n>`, business rules `BR-YYY`)
- `docs/entity_model.md`: Mermaid ER diagram plus attribute tables for every table
- Not created yet: `docs/vision.md`, `docs/requirements.md`, `docs/glossary.md`, `docs/test_cases/`, `docs/processes/`. Create them with `/requirements`, `/test-case`, and `/business-process` when a change needs them. Never invent `FR-*`/`NFR-*` ids for a `**Requirements:**` line until the catalog exists.
- The baseline was produced by `/reverse-engineer`, so all specs have `**Status:** Implemented`. A `> Note:` in a spec records a known gap or suspected bug in the current code.

### Before touching code
1. Find the affected use case(s): read `docs/use_cases.puml` and grep `docs/use_cases/` for the behavior. Read the whole spec plus `docs/entity_model.md`.
2. Read the business rules a spec cites from other use cases (`UC-009 BR-004`).

### New feature or behavior change
1. **Spec first.** Run `/use-case-spec` to update an existing `UC-XXX` or add a new one. A new use case also needs `/use-case-diagram`. A data change also needs `/entity-model`. A new use case starts as `Draft`.
2. Run the spec checks (below), show the user the spec diff, and **stop for review**. Implement only once the user approves. Never change a `**Status:**` line yourself; propose the new value instead.
3. Implement against the spec. Every schema change needs a new Flyway migration and the matching `docs/entity_model.md` update in the same change.
4. Add a Backlog task (see the Backlog section) whose description names the use case(s), and whose acceptance criteria map to spec steps, flows, and rules.

### Bug fix
Decide first whether the code or the spec is wrong:
- **Code deviates from the spec:** fix the code and add a test named after the flow or rule it violated. The spec stays unchanged.
- **The spec is wrong, silent, or ambiguous** (the behavior has no alternative flow or rule, or a `> Note:` describes it): update the spec via `/use-case-spec UC-XXX` first, get the user's confirmation, then fix the code. Remove the `> Note:` once it is resolved.
- **Purely technical** (build, infrastructure, performance with no visible behavior change): no spec change. Say so explicitly in the summary.

Never close a gap in the spec with a silent assumption. Ask the user, or list it under **Open questions** (`UC-XXX step N / A<n> / BR-YYY`: question, possible readings).

### Traceability markers
- **Code:** put `// UC-XXX BR-YYY: <rule restated in one line>` directly above the method, query condition, or check that enforces a rule. Add the marker whenever you implement or touch a rule, and keep it in step when a rule changes or is removed. Always qualify the rule with its use case id.
- **Tests:** the test class gets `@DisplayName("UC-XXX: <Use Case Name>")`, and each test method `@DisplayName("A<n>: …")`, `@DisplayName("BR-YYY: …")`, or `@DisplayName("Main: …")` for the flow or rule it covers. A test class covering several use cases puts the qualified id on each method instead (`"UC-009 BR-006: …"`). Existing tests predate the convention: add markers when you touch them, with no bulk retrofit.

### Spec checks (run after every spec edit; both must pass)
```bash
AIUP=$(ls -d ~/.claude/plugins/cache/ai-unified-process-marketplace/aiup-core/*/skills | sort -V | tail -1)
python3 $AIUP/use-case-spec/scripts/validate_use_case.py --strict docs/use_cases/UC-*.md
python3 $AIUP/spec-review/scripts/spec_lint.py --strict
```
Use `/spec-review UC-XXX` for the advisory semantic review before asking for approval of a new or substantially changed use case.

### Writing rules for specs
- Steps stay at the business level: no HTTP, SQL, class or framework names.
- `BR-YYY` ids restart at `BR-001` in every file. Reference another use case's rule as `UC-XXX BR-YYY`, never by copying it.
- Entity-model types come only from `Long, String, Integer, Decimal, Boolean, Date, DateTime, BLOB`. Validation Rules come only from the `/entity-model` vocabulary.
- Keep ids stable: never renumber or reuse `UC-*`, `BR-*`, or `A<n>` after commit. Mark a dropped use case `Obsolete` instead of deleting it.

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
- `V1__create_teams.sql` holds the initial schema and is deployed to production — **never edit V1 or any other applied migration**. Every schema change goes in a new `V<N>__description.sql` file (next free number). Flyway applies it on startup (`migrate-at-start=true`)
- Table order must respect foreign key dependencies
- All text columns are UTF-8 and support Bulgarian Cyrillic — DB must be created with `ENCODING 'UTF8'`
- Roles (`ADMIN`, `USER`) are seeded in the migration
- `%dev.quarkus.flyway.clean-at-start=true` and `%dev.quarkus.flyway.repair-at-start=true` are set so the dev DB is rebuilt from all migrations on every startup — `clean-at-start` drops and re-applies the whole schema (so a new migration that duplicates something already in an earlier one fails in dev, e.g. `already exists`), while `repair-at-start` fixes checksum bookkeeping. Neither protects prod: editing an applied migration there causes a checksum mismatch at startup. Because of `clean-at-start`, the docker-compose dev DB is disposable — never store data there you need to keep

## Resource / URL Conventions
- Management resources live at top-level paths: `/teams`, `/competitions`, `/team-formations`, `/participations`
- Security model is public-read / admin-write: list (`GET /x`) is unauthenticated and public; `GET /x/new`, `GET /x/{id}/edit`, `POST`, and `DELETE` are `@RolesAllowed("ADMIN")` — no URL-pattern config needed
- List templates must gate admin-only controls (add/edit links, delete buttons, import links) behind an `isAdmin` flag passed from the resource (`identity.hasRole("ADMIN")` via `CurrentUser`), since the list page itself is rendered for anonymous and USER-role visitors too
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

## Experience Playbook (Open Reasoning Format)
This project records domain-specific traps and validated fixes in `./experiences/`, following the [Open Reasoning Format](https://github.com/glaforge/open-reasoning-format) spec, via the `manage-experience` skill (`manage-experience/SKILL.md`).
- **Before tackling a non-trivial task** (Hibernate entity mapping, Qute templates, resource tests, scraping), run `python3 manage-experience/scripts/experiences.py list-categories`, then `get-frontmatter --category <domain-id>` for any matching category, then `read-experience --id <EXP-ID>` for a specific match. Apply the "Abstracted Insight" and "Validated Path" before writing code.
- **After resolving a multi-step debugging loop, an unexpected trap, or a domain-specific workaround**, record it with `python3 manage-experience/scripts/experiences.py create-experience --domain <domain-id> --title ... --description ... --keywords ... --complexity ... --objective ... --trap ... --insight ... --validated-path ... --checklist-item ...` — this writes a new playbook and updates `experiences/INDEX.md` automatically.
- Existing domains: `quarkus-hibernate`, `qute-templates`, `quarkus-testing`. Add new domain categories to `experiences/INDEX.md`'s frontmatter `categories` list as new problem areas emerge (e.g. `bfu-scraping`, `flyway-migrations`).

<!-- BACKLOG.MD MCP GUIDELINES START -->

<CRITICAL_INSTRUCTION>

## BACKLOG WORKFLOW INSTRUCTIONS

This project uses Backlog.md MCP for all task and project management activities.

**CRITICAL GUIDANCE**

- If your client supports MCP resources, read `backlog://workflow/overview` to understand when and how to use Backlog for this project.
- If your client only supports tools or the above request fails, call `backlog.get_backlog_instructions()` to load the tool-oriented overview. Use the `instruction` selector when you need `task-creation`, `task-execution`, or `task-finalization`.

- **First time working here?** Read the overview resource IMMEDIATELY to learn the workflow
- **Already familiar?** You should have the overview cached ("## Backlog.md Overview (MCP)")
- **When to read it**: BEFORE creating tasks, or when you're unsure whether to track work

These guides cover:
- Decision framework for when to create tasks
- Search-first workflow to avoid duplicates
- Links to detailed guides for task creation, execution, and finalization
- MCP tools reference

You MUST read the overview resource to understand the complete workflow. The information is NOT summarized here.

</CRITICAL_INSTRUCTION>

<!-- BACKLOG.MD MCP GUIDELINES END -->
