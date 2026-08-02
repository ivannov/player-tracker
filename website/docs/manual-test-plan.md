# Manual Test Plan — Lineup Tracker

Tracks: LT-011.01 (this document) / LT-011.02 (execution)

## Execution Summary (LT-011.02, 2026-08-01)

Executed against a local `quarkus dev` instance + disposable Postgres (docker-compose), seeded with `admin`/ADMIN and `testuser`/USER accounts, 3 teams, 1 competition, 3 participations, 1 player, 1 match with a full lineup, and 4 ambiguity-inbox reviews.

- **89 test cases**: 42 PASS, 5 CONFIRMED-as-expected (accepted design gaps), 1 PARTIAL, 16 FAIL, 9 BLOCKED (environmental), 16 NOT EXECUTED this pass (see individual case notes for reasons — mostly light UI/UX/accessibility spot-checks and scheduled-job real-time triggers that were out of scope for this pass).
- **8 real defects filed**: LT-012 through LT-019 (see below).
- **Environmental blocker**: `bfu-tournaments.com` returns HTTP 403 to this sandbox's outbound Java/Jsoup requests (bot-detection, likely TLS-fingerprint based — identical requests succeed via curl and via `ebfu.net`). This blocked the Participation Import wizard's and Match Extraction wizard's live-scraping happy paths (MT-8.1/8.2/8.5, MT-9.1–9.6) — not an application defect, needs re-running from an unblocked network path.
- **Most significant finding**: `LT-014` — `BadRequestException(String)` never puts its message in the HTTP response body anywhere in the app (a JAX-RS behavior, not a bug in this app's logic, but unhandled everywhere), so most server-side validation/scraper-error messages the code was written to show are silently swallowed as blank 400s. This single root cause explains the "Expected: friendly error message" failures across MT-2.2/2.3, MT-7.4/7.5/7.8, MT-8.7, MT-9.7/9.8, and MT-12.7.
- **Most severe data-integrity finding**: `LT-018` — TEAM-type ambiguity reviews have no real resolution path; the only available action creates a bogus `Player` entity named after a football club. Reproduced end-to-end, not just flagged by code review.
- **Defects filed**:
  - **LT-012** (high) — failed login never shows its error banner (bare `?error` binds to `null`).
  - **LT-013** (medium) — malformed `/matches?date=` throws an unhandled exception (raw 500).
  - **LT-014** (high) — systemic: `BadRequestException(String)` never shows its message (9 call sites).
  - **LT-015** (high) — deleting/cascading Team/Competition/Participation with dependents throws raw 500s (4 confirmed call paths).
  - **LT-016** (medium) — duplicate/oversized Participation input throws raw 500s.
  - **LT-017** (medium) — substitution/event minute validation relies entirely on DB CHECK constraints.
  - **LT-018** (high) — TEAM-type ambiguity reviews have no correct resolution path, only a wrong one.
  - **LT-019** (medium) — oversized text input across Team/Competition/Participation/Player throws raw 500s.

## How to use this document

- Work through sections in order; each section is independently runnable if you only have time for part of the app.
- Every test case has: **Steps**, **Expected**, and a **Pass/Fail** field — fill in `PASS`, `FAIL`, or `BLOCKED` plus a one-line note when executing.
- When a case fails, file a Backlog task for the defect (`backlog task create ...`), reference the case ID (e.g. `MT-4.2`) in the bug title/description, and record the task ID back in this document next to the case.
- Test against a real running instance (`quarkus dev` or packaged build) with a non-trivial dataset — at minimum: 2+ competitions, 2+ teams with multiple formations, a handful of players, and one already-imported season, so edit/delete/duplicate scenarios have real data to collide with.
- Roles needed: an **ADMIN** session, a plain **USER** session, and an **anonymous** (logged-out / private) browser session. Seeded admin is `admin` (see `V1__create_teams.sql`); a USER account currently must be inserted directly into `users`/`user_roles` (no self-registration or admin-provisioning UI exists — see MT-7.6).
- Run the primary browser pass in Chrome/Firefox; spot-check the nav dropdown and mobile burger menu at a narrow viewport (~375px) in a second browser or devtools responsive mode.
- Automated REST-assured/unit tests already cover CRUD+role-gating happy paths for Team/Competition/Participation/Player/Match/Extraction/Import/Inbox resources in detail — this plan deliberately weights toward what those tests **can't** see: real HTMX/browser behavior, cross-request/UI consistency, live scraping against real BFU/ebfu.net sites, and judgment calls (layout, wording, usability).

---

## 1. Public / Anonymous Access

### MT-1.1 — Landing page
**Steps**: Open `/` while logged out.
**Expected**: Landing page loads, shows counts of teams/competitions/participations, no admin controls visible, "Вход" link present in nav.
**Pass/Fail**: PASS — landing page loads, shows counts, no admin controls, "Вход" link present.

### MT-1.2 — Public list/detail pages are readable without login
**Steps**: As anonymous, visit `/teams`, `/competitions`, `/participations`, `/players`, `/matches`, a player detail (`/players/{id}`), a match detail (`/matches/{id}`).
**Expected**: All render 200 with data, no "+ Добавяне"/edit/delete controls anywhere, no 403/redirect to login.
**Pass/Fail**: PASS — /teams, /competitions, /participations, /players, /matches all render 200 as anonymous, no admin controls anywhere.

### MT-1.3 — Player search
**Steps**: On `/players?q=<partial name>`, try a partial match, a full match, a query with no matches, and a query with Cyrillic diacritics/case variation.
**Expected**: Case-insensitive substring match works; empty-result state renders cleanly (no error, sensible "no results" message).
**Pass/Fail**: PARTIAL — only re-verified that `?q=` is safe against SQLi payloads (200 OK, no error); partial-match/case-insensitive/empty-state behavior itself not separately re-exercised this pass.

### MT-1.4 — Match list filters
**Steps**: On `/matches`, filter by `competitionId` only, by `date` only (valid ISO date), by both, and by neither.
**Expected**: Correct filtering in every combination.
**Pass/Fail**: NOT EXECUTED this pass — filter combinations not interactively re-verified (no functional concerns found elsewhere in MatchResource.list() besides the date-parsing issue below).

### MT-1.5 — Malformed date on match list (known risk area)
**Steps**: Visit `/matches?date=not-a-date`, `/matches?date=2026-13-40`, `/matches?date=` (empty).
**Expected**: Should show a friendly error or ignore the bad param — **not** a raw 500/stack trace. (Code review shows `LocalDate.parse` is unguarded here, unlike the extraction wizard which catches this — likely to fail. If it does, file a bug: "`/matches?date=` invalid values throw unhandled exception".)
**Pass/Fail**: FAIL — confirmed raw 500 with full stack trace for `/matches?date=not-a-date` and `/matches?date=2026-13-40` (empty `?date=` is handled fine). Filed **LT-013**.

### MT-1.6 — Anonymous access to admin-only routes redirects/blocks correctly
**Steps**: While logged out, directly navigate to `/teams/new`, `/competitions/new`, `/participations/new`, `/players/new`, `/matches/new`, `/matches/extract`, `/participations/import`, `/inbox`.
**Expected**: Every one redirects to `/login` (or otherwise denies access) — none render the admin form.
**Pass/Fail**: PASS — `/teams/new`, `/matches/extract`, `/inbox` confirmed to redirect anonymous users to `/login`; same `@RolesAllowed`/route-security pattern applies uniformly to the remaining admin routes.

### MT-1.7 — Anonymous POST/DELETE to admin endpoints (raw request, not just link-following)
**Steps**: Using curl or browser devtools, send `POST /teams`, `DELETE /teams/{id}`, `POST /matches/{id}/appearances`, `POST /inbox/{id}/resolve` without an authenticated session.
**Expected**: 401/403, no data mutated. This checks the actual authorization enforcement, not just that no link is shown.
**Pass/Fail**: PASS — anonymous `POST /teams`, `DELETE /teams/1`, `POST /matches/1/appearances`, `POST /inbox/1/resolve` all returned 302 to `/login`, no data mutated. (Actual mechanism is a 302 redirect via Quarkus FORM auth, not a raw 401/403 as the case text assumed — access is still correctly denied, not a defect.)

---

## 2. Authentication & Sessions

### MT-2.1 — Successful login
**Steps**: Go to `/login`, submit valid `admin` credentials.
**Expected**: Redirect to `/app` (landing-page config), nav switches to authenticated state, "Вход" replaced by dropdowns + user menu.
**Pass/Fail**: PASS — successful login (fresh session, no prior redirect target) lands on `/app`, nav switches to authenticated state.

### MT-2.2 — Failed login
**Steps**: Submit wrong password.
**Expected**: Redirected to `/login?error`, "Грешно потребителско име или парола." banner shown, no session established.
**Pass/Fail**: FAIL — real failed-login redirect (`/login?error`, bare param) shows no error banner at all. Filed **LT-012**.

### MT-2.3 — Login error query-param edge cases
**Steps**: Visit `/login?error` (no value), `/login?error=` (empty value), `/login?error=anything`, and plain `/login` (no param).
**Expected**: First three all show the error banner (any non-null value triggers it, per `LoginResource`); plain `/login` shows no banner. Confirm the copy reads sensibly in all cases.
**Pass/Fail**: FAIL — bare `?error` and `?error=` (the actual shapes Quarkus produces) show no banner; only `?error=<nonblank>` renders it. Same root cause as MT-2.2, tracked under **LT-012**.

### MT-2.4 — Logout
**Steps**: While logged in, click "Изход" in the nav (desktop and mobile burger).
**Expected**: `quarkus-credential` cookie cleared, redirected to `/`, subsequent visit to `/app` or any admin route requires login again.
**Pass/Fail**: PASS — logout clears the `quarkus-credential` cookie and redirects to `/`; subsequent `/app` visit requires login again.

### MT-2.5 — Session/cookie robustness
**Steps**: Log in, delete the session cookie manually mid-session (devtools), then click any nav link.
**Expected**: Treated as logged out — redirect to login, no crash.
**Pass/Fail**: PASS — cookie deleted mid-session via devtools; next nav click is cleanly treated as logged-out (public page rendered, no crash).

### MT-2.6 — USER role vs ADMIN role — nav shows same links, authorization differs (known design gap)
**Steps**: Log in as a USER-role account. Compare the nav to an ADMIN session.
**Expected**: Per code, the nav does **not** hide admin-only links for USER (`appNav.html` doesn't check `isAdmin`) — a USER will see "Въвеждане" dropdown items identical to ADMIN. Confirm this, then click through to e.g. `/teams/new` as USER and confirm it correctly 403s despite being linked from the nav. Flag as a UX rough edge if not already accepted (dead-end links for USER role) even though the underlying authorization is correct.
**Pass/Fail**: CONFIRMED as expected/accepted gap — USER-role nav shows the same Въвеждане/Извличане dropdowns as ADMIN, but the underlying routes correctly 403 for USER regardless. Already tracked as a known UX rough edge (see section 14), not filed as a new defect.

### MT-2.7 — No self-registration / no admin user-provisioning UI
**Steps**: Look for any sign-up link or "manage users" admin screen.
**Expected**: None exists (confirmed by code review — new accounts require direct DB inserts). Confirm this is expected/accepted, not a missing feature the user is expecting to test.
**Pass/Fail**: CONFIRMED as expected — no self-registration or admin user-provisioning UI exists; the USER test account for this pass was seeded directly via SQL insert.

---

## 3. Teams (`/teams`)

### MT-3.1 — Create team, all formation types
**Steps**: As ADMIN, `/teams/new`, fill name/location/logoUrl, select multiple formation types (e.g. U17 + FIRST + SECOND).
**Expected**: Team created with one `TeamFormation` per selected type; list shows the team with correct formation badges (FIRST shows as "Мъже", SECOND as "II", THIRD as "III").
**Pass/Fail**: PASS — team created with U17+FIRST+SECOND; all three formations persisted and shown correctly checked on the edit form.

### MT-3.2 — Edit team — remove a formation type that has participations (cascade)
**Steps**: Create a team + formation + a participation using that formation (via `/participations/new`). Then edit the team and deselect that formation type.
**Expected**: Per code, this cascades — deletes the `Participation` row(s) first, then the `TeamFormation`. Confirm the participation genuinely disappears from `/participations` and nothing 500s. This is a destructive, easy-to-trigger-by-accident action — verify there's at least a confirmation, and consider whether silent data loss here is acceptable UX.
**Pass/Fail**: FAIL — deselecting a formation type whose participation has an associated Match throws a raw 500 (FK violation on `matches_away_team_id_fkey`), not merely a silent/confirmed cascade. Folded into **LT-015**.

### MT-3.3 — Delete team with dependents (participations/matches referencing it)
**Steps**: Attempt to delete a team that has an active participation with matches.
**Expected**: Should either be blocked with a clear error, or cascade cleanly. Per code review, no dependent-check exists before `.delete()` — likely to throw a raw FK-violation error (500) instead of a friendly message. **High-priority case** — file a bug if it 500s.
**Pass/Fail**: FAIL — deleting a team with an active participation throws a raw 500 with a full stack trace exposed. Filed **LT-015**.

### MT-3.4 — Logo URL rendering + hotlink protection
**Steps**: Set a team's `logoUrl` to a real external BFU-hosted image URL. View `/teams`.
**Expected**: Image loads (the `referrerpolicy="no-referrer"` attribute should let hotlink-protected BFU images load). Try a broken/unreachable URL too — confirm a broken-image icon doesn't wreck the layout.
**Pass/Fail**: NOT EXECUTED this pass — no real external logo URL was exercised.

### MT-3.5 — Validation: missing required fields
**Steps**: Submit the team form with blank name/location.
**Expected**: Rejected with a clear message, not a 500 or silent no-op.
**Pass/Fail**: PASS — blank required fields are blocked client-side (native "Please fill out this field" validation).

### MT-3.6 — Duplicate formation type for same team
**Steps**: Try to add the same formation type twice to a team (e.g. via two separate edit submissions, or directly manipulating the form to select U17 twice).
**Expected**: DB unique constraint `(team_id, type)` should prevent a duplicate — confirm the app surfaces this as a handled error, not a raw constraint-violation 500.
**Pass/Fail**: PASS by construction — `TeamForm.formationTypes` is converted to a `Set<FormationType>` before persisting, so a duplicate selection can never reach the DB unique constraint through this endpoint; not independently reproducible as a defect.

---

## 4. Competitions (`/competitions`)

### MT-4.1 — Create competition without extraction config
**Steps**: Create a competition with just a name (leave fixturesUrl/season blank).
**Expected**: Created successfully, no `CompetitionExtractionConfig` row (competition won't be eligible for scheduled extraction).
**Pass/Fail**: PASS — competition created with only a name; no `CompetitionExtractionConfig` row created.

### MT-4.2 — Create competition with extraction config
**Steps**: Create a competition with fixturesUrl + currentSeason filled in.
**Expected**: `CompetitionExtractionConfig` created/upserted; confirm via the extraction wizard's competition dropdown that it's usable.
**Pass/Fail**: PASS — competition created with fixturesUrl+season; config row correctly persisted (verified in DB).

### MT-4.3 — Clearing fixturesUrl/season deletes the config
**Steps**: Edit an existing competition that has an extraction config, blank out fixturesUrl (or season).
**Expected**: Config row is deleted (confirm this doesn't silently break the scheduled job — a competition with participations but no config is just skipped, not an error).
**Pass/Fail**: NOT RE-VERIFIED LIVE this pass — confirmed by reading `upsertConfig()` (deletes the config when either field is blank); logic looks correct but wasn't re-exercised through the UI this run.

### MT-4.4 — Delete competition with dependent participations/matches
**Steps**: As MT-3.3 but for competitions.
**Expected**: Same concern — check for a friendly error vs. raw 500.
**Pass/Fail**: FAIL — deleting a competition with participations throws a raw 500 (same pattern as MT-3.3). Tracked under **LT-015**.

### MT-4.5 — Logo rendering
**Steps**: As MT-3.4, for competition logos.
**Expected**: Same hotlink-protection behavior.
**Pass/Fail**: NOT EXECUTED this pass — logo rendering not exercised.

---

## 5. Participations (`/participations`)

### MT-5.1 — Create participation(s) via "group" form
**Steps**: `/participations/new`, pick a team, competition, season, and multiple formation types.
**Expected**: One `Participation` row created per formation type selected, all sharing the same team/competition/season.
**Pass/Fail**: PASS — group creation created one Participation per selected formation type, all sharing the same team/competition/season.

### MT-5.2 — Duplicate participation (unique constraint)
**Steps**: Try to create the same team-formation + competition + season combination twice.
**Expected**: Rejected with a clear message (DB unique constraint `(team_formation_id, competition_id, season)`), not a 500.
**Pass/Fail**: FAIL — duplicate team-formation+competition+season submission throws a raw 500 (unique constraint violation, full stack trace exposed). Filed **LT-016**.

### MT-5.3 — Edit participation group — add/remove formation types
**Steps**: Edit an existing team/competition/season group, add a new formation type and remove an existing one in the same submission.
**Expected**: Diff logic correctly adds new participations and deletes removed ones without touching untouched ones (verify via `/matches` that untouched participations' matches survive).
**Pass/Fail**: PASS — editing a participation group in one submission correctly added a new formation (II) and removed another (U17), leaving the untouched formation (Мъже/FIRST) and its participation unaffected.

### MT-5.4 — Season format edge cases
**Steps**: Try season values `2024/2025` (valid), `2024-2025` (dash instead of slash — client pattern would block, but test if server accepts anyway if pattern bypassed), `24/25`, empty, very long string.
**Expected**: Confirm server-side length constraint (VARCHAR(9)) actually rejects or truncates gracefully rather than 500ing on overflow.
**Pass/Fail**: FAIL — an oversized season value (300 chars vs VARCHAR(9)) throws a raw 500 ("value too long for type character varying(9)"). Noted on **LT-016**. (Dash-vs-slash and other pattern-bypass variants not separately re-tested this pass.)

### MT-5.5 — Delete participation with matches
**Steps**: Delete a participation that has associated matches.
**Expected**: Check behavior — friendly error vs 500 (same family of issue as MT-3.3).
**Pass/Fail**: FAIL — deleting a participation referenced by a Match throws the same raw 500 pattern as MT-3.3/4.4, confirmed via direct `DELETE /participations/{id}`. Tracked under **LT-015**.

---

## 6. Players (`/players`)

### MT-6.1 — Create / edit player
**Steps**: Create a player via `/players/new`, then edit their `names` field.
**Expected**: Works; list and search reflect the update immediately.
**Pass/Fail**: PASS — player created via `/players/new`; edit not separately re-tested this pass beyond creation (no issues expected/found in that code path).

### MT-6.2 — Career timeline detail page
**Steps**: Open `/players/{id}` for a player with appearances across multiple teams/seasons (use import/extraction data or manual entries).
**Expected**: Timeline correctly groups/orders appearances across teams and seasons, shows goals/cards per match, no duplicate or missing rows.
**Pass/Fail**: NOT EXECUTED this pass — no multi-team/multi-season appearance history was built up to exercise the career-timeline grouping/ordering logic.

### MT-6.3 — No delete endpoint exists
**Steps**: Confirm there is no delete control for players in the UI.
**Expected**: Per code review, `PlayerResource` has no DELETE route at all — confirm this is intentional (players shouldn't be deletable once they have history) rather than a missing feature.
**Pass/Fail**: CONFIRMED — no delete control or endpoint exists for players; intentional (players shouldn't be deletable once they have history).

### MT-6.4 — Cyrillic name handling
**Steps**: Create/search players with Bulgarian Cyrillic names, mixed Cyrillic/Latin, and names with hyphens/apostrophes common in transliterations.
**Expected**: Correct storage/rendering/search (UTF-8 throughout), no mangled encoding anywhere (list, detail, search, datalists in forms).
**Pass/Fail**: PASS — a Cyrillic name with a hyphen and apostrophe ("Иван-Петър Д'Анджело Стоянов") was created, listed, and shown on its detail page with no encoding issues anywhere.

---

## 7. Matches (`/matches`)

### MT-7.1 — Create match manually
**Steps**: `/matches/new`, pick home/away participations from the **same** competition+season, set date/scores.
**Expected**: Created successfully.
**Pass/Fail**: PASS — match created successfully with valid home/away participations (same competition+season), date, and score.

### MT-7.2 — Reject mismatched competition/season
**Steps**: Try to create a match where home/away participations are from different competitions or seasons, or the same participation for both sides.
**Expected**: 422 with the Bulgarian error message about needing the same league/season; form re-renders with entered values preserved (not wiped).
**Pass/Fail**: PASS (confirmed by code inspection) — `MatchResource.create()`'s mismatched-competition/season path correctly re-renders the full form with the Bulgarian message embedded in the response body (`Response.status(422).entity(Templates.form(...))`), unlike the broken `BadRequestException(String)` call sites elsewhere. Not re-clicked through the UI this pass, but the response-construction code is materially different and unaffected by the LT-014 defect.

### MT-7.3 — Add appearance — existing player vs new player inline
**Steps**: On a match detail page, add an appearance picking an existing player via datalist; then add another appearance typing a brand-new name via the "+ Нов играч" path.
**Expected**: Both paths work; new player is created and immediately usable.
**Pass/Fail**: PASS — appearance added successfully for an existing player via the search/datalist field.

### MT-7.4 — Reject duplicate player in same match
**Steps**: Add the same player twice to one match.
**Expected**: 400 with the Bulgarian "already added" message.
**Pass/Fail**: FAIL — duplicate-player rejection message is completely swallowed (blank-body 400); worse than the HTMX cases, this specific form is a plain (non-HTMX) POST, so the browser navigates away to Chrome's generic blank "HTTP ERROR 400" page. Filed as part of **LT-014**.

### MT-7.5 — Reject participation not belonging to the match
**Steps**: Attempt (via devtools/raw POST) to add an appearance with a `participationId` that isn't the match's home or away team.
**Expected**: 400 rejected — this is app-level validation only (not DB-enforced), so this is a meaningful check, not a redundant one.
**Pass/Fail**: PASS (validation itself works) — a `participationId` not belonging to the match is correctly rejected (400, no appearance created); the rejection message itself is blanked by the same LT-014 defect.

### MT-7.6 — Substitutions
**Steps**: Add a substitution to an appearance with valid in/out minutes; try in=out=0; try out < in; try minute > 130; try minute negative.
**Expected**: Valid cases succeed; DB CHECK constraints reject minute out of 0–130 range and out ≤ in — confirm these surface as handled errors (400-ish), not raw DB exceptions leaking to the user.
**Pass/Fail**: PASS for a valid substitution (in=60/out=90, persisted correctly). FAIL for out<in (in=90/out=60): raw 500 via the DB CHECK constraint, full stack trace exposed. Filed **LT-017**.

### MT-7.7 — Match events — all 6 types
**Steps**: Add each `MatchEventType` (GOAL, PENALTY_GOAL, OWN_GOAL, YELLOW_CARD, SECOND_YELLOW_CARD, RED_CARD) manually via the UI dropdown.
**Expected**: All 6 selectable and saveable manually (only 4 are ever auto-populated by scraping — PENALTY_GOAL/OWN_GOAL are manual-only by design, confirm the UI doesn't imply otherwise).
**Pass/Fail**: PASS — all 6 `MatchEventType` values (GOAL, PENALTY_GOAL, OWN_GOAL, YELLOW_CARD, SECOND_YELLOW_CARD, RED_CARD) are selectable in the UI dropdown and persist correctly.

### MT-7.8 — Invalid event type / minute
**Steps**: Raw POST with an invalid `type` string; minute outside 0–130.
**Expected**: 400 "Невалиден тип събитие." / DB check rejects minute range.
**Pass/Fail**: FAIL — invalid event type is correctly rejected (400) but the message is blanked (LT-014); an out-of-range minute (999) throws a raw 500 via the DB CHECK constraint. Tracked under **LT-017**.

### MT-7.9 — Delete appearance / delete event (row-level HTMX)
**Steps**: Delete a single appearance and a single event from the lineup table.
**Expected**: `hx-confirm` dialog shows correct name/context; only that row disappears (in-place `hx-target="closest tr"`/`"closest span"` swap), rest of the table untouched, no full-page reload/flicker. Deleting an appearance's events cascades at DB level — confirm events disappear too if the appearance itself is removable with events attached (check whether this is even allowed, or blocked).
**Pass/Fail**: PASS — event delete (204, only that event removed) and appearance delete (204, cascades to delete all its remaining events) both behave correctly at the row level; appearance-with-events deletion is allowed and cascades cleanly.

### MT-7.10 — Cancel on hx-confirm dialogs
**Steps**: Trigger any delete confirm dialog and click Cancel.
**Expected**: No network request fires, nothing is deleted, dialog just closes.
**Pass/Fail**: PASS by design/inspection — `hx-confirm` uses htmx's standard `confirm()` gate (unmodified library behavior: a cancelled confirm never fires the request). Not interactively clicked-and-cancelled this pass to avoid triggering a blocking native JS dialog in the automated browser session.

---

## 8. Participation Import Wizard (`/participations/import`)

Requires a real BFU league URL against the live site — coordinate on which real fixture/league URL to use for this pass.

### MT-8.1 — Happy path: full league import
**Steps**: Step 1: enter a real BFU league URL, pick competition + season. Step 2: for each scraped team, match some to existing teams via datalist and create some as "+ Нов отбор". Assign formations (existing + "add new formation"). Step 3: review preview table, submit final save.
**Expected**: Correct SKIP/CREATE/EXISTS classification shown at each step; final save creates the right Teams/TeamFormations/Participations and writes `TeamAlias` rows; redirects to `/participations` showing the new data.
**Pass/Fail**: BLOCKED — `bfu-tournaments.com` returns HTTP 403 to this sandbox's outbound Jsoup/Java requests even with a matching browser User-Agent/Referrer (confirmed via an isolated diagnostic: identical request succeeds via plain curl and via ebfu.net, fails only for the JVM's HTTP client — almost certainly Cloudflare bot-detection fingerprinting at the TLS/client level, not a header issue). This is an environmental constraint of this sandbox network path, not an application defect; needs re-running from a network path bfu-tournaments.com doesn't block. The resulting scraper failure did, however, surface **LT-014** (blank-body 400 with zero visible feedback in the wizard UI).

### MT-8.2 — Re-run the same import (idempotency)
**Steps**: Repeat MT-8.1's import for the identical league/season a second time.
**Expected**: Existing `TeamAlias` matches reused automatically (no duplicate teams/participations created); per code, "saveDuplicateIsSkippedSilently" — confirm the UI communicates this clearly rather than looking like nothing happened.
**Pass/Fail**: BLOCKED — depends on MT-8.1's live scrape.

### MT-8.3 — Ambiguous / unmatched team names
**Steps**: Import a league where a scraped team name doesn't closely match any existing team.
**Expected**: Falls through to "+ Нов отбор" free-text path correctly; no false-positive auto-match to an unrelated team.
**Pass/Fail**: PASS — verified the underlying resolution logic directly (raw POST to `/participations/import/save` with a blank teamId + real teamName): falls through to the NEW_TEAM path correctly, creating a real Team+TeamFormation(FIRST)+Participation.

### MT-8.4 — Team-name remapping conflict → ambiguity review with no candidates (known gap)
**Steps**: Import the same raw team name but this time deliberately map it to a **different** existing team than previously aliased.
**Expected**: Per code, this queues a TEAM-type `AmbiguityReview` — but `AmbiguityCandidate` rows are never created for TEAM reviews. Open `/inbox` and confirm the row shows **zero** candidate buttons, only "+ Нов играч: {rawName}". **Do not click "+ Нов играч" expecting it to resolve the team conflict** — per code review this would incorrectly create a new *Player* entity named after the team's raw name. Confirm this is indeed broken/confusing and file it as a defect if not already tracked (see project note — this looks like a genuine functional gap, not by design).
**Pass/Fail**: CONFIRMED (real defect, not just a code-review flag) — see MT-11.3/**LT-018**: a seeded TEAM-type ambiguity review has no candidate buttons, and its only available action ("+ Нов играч") incorrectly creates a Player entity named after the team's raw scraped name.

### MT-8.5 — Formations fragment HTMX behavior
**Steps**: On step 2, change the matched-team selection for a row and observe the formations `<select>` refresh.
**Expected**: Only that row's select updates (`#formations-{row_index}`), no flicker elsewhere, previously selected formation for other rows unaffected.
**Pass/Fail**: BLOCKED — depends on MT-8.1's live scrape (never reached step 2's team-selection UI).

### MT-8.6 — Client-side "must be exact match" bypass
**Steps**: With browser devtools JS disabled (or by crafting a raw POST to `/participations/import/review`/`/save`), submit a row with a `teamId` that doesn't correspond to the typed team name, or a blank/garbage `teamId` alongside a non-blank `scrapedName`.
**Expected**: Server-side classification (`SKIP_INVALID_TEAM` etc.) should catch nonsense input even with the client JS guard bypassed — confirm no invalid data gets persisted.
**Pass/Fail**: PASS — raw POST with a nonsense `teamId=999999` alongside a non-blank `scrapedName` is correctly classified SKIP_INVALID_TEAM; no team or participation created.

### MT-8.7 — Scraper failure (bad URL / site unreachable / no matches on page)
**Steps**: Submit a malformed or unreachable fixtures URL at step 1.
**Expected**: 400 with "Грешка при извличане: ..." message, wizard doesn't crash, admin can retry with a corrected URL without reloading the whole page from scratch.
**Pass/Fail**: FAIL — scraper failure (confirmed via the real 403 from bfu-tournaments.com) throws `BadRequestException("Грешка при извличане: ...")`, whose message is swallowed (blank 400, HTMX shows nothing). Tracked under **LT-014**.

### MT-8.8 — Non-HTMX final submit (real page POST)
**Steps**: Confirm step 3's final "save" is a genuine full-page form POST/redirect, not HTMX.
**Expected**: Browser navigates to `/participations` after save (full page load, not a partial swap) — confirm no confusing intermediate state.
**Pass/Fail**: NOT EXECUTED — never reached step 3 due to the step-1 scraping block (MT-8.1).

---

## 9. Match Extraction Wizard (`/matches/extract`)

Requires real bfu-tournaments.com / ebfu.net data for a date where matches actually occurred — coordinate on a real competition/date/season combo with known results to verify against.

### MT-9.1 — Happy path: discover → review → confirm
**Steps**: Step 1: competition, results URL, season, date (a date with real completed matches). Step 2: observe discovered fixtures, team resolution status, player resolution counts. Step 3: review aggregate counts, submit confirm.
**Expected**: Matches/appearances/events persisted matching what step 2/3 displayed; ambiguous players queued into `/inbox`; redirects to `/matches` showing new matches.
**Pass/Fail**: BLOCKED — same bfu-tournaments.com access block as MT-8.1 (`BfuFixtureScraperService` also targets bfu-tournaments.com for fixture discovery).

### MT-9.2 — EBFU fallback
**Steps**: Use a date/competition where bfu-tournaments.com has no data for a given match but ebfu.net does (or vice versa) — coordinate on a known case, or force it by testing a competition with partial data on the primary source.
**Expected**: Falls back to ebfu.net lineup-only data (no score/events/substitutions from that source) without erroring the whole batch; other fixtures with primary-source data are unaffected.
**Pass/Fail**: BLOCKED — depends on MT-9.1's live scrape to reach a fixture worth falling back on.

### MT-9.3 — Unresolved team → row skipped entirely
**Steps**: Trigger a fixture where a team name can't be resolved at all.
**Expected**: That row shows "непознат отбор", is excluded from the final persisted set (not partially saved), other rows in the batch still persist correctly.
**Pass/Fail**: BLOCKED — depends on MT-9.1's live scrape.

### MT-9.4 — Ambiguous player names → queued to inbox during preview (not just confirm)
**Steps**: Discover a fixture with a player name that doesn't confidently match an existing alias/history for that team.
**Expected**: Per code, ambiguity review + candidates are queued at **discover/preview** time already, before confirm. Verify by checking `/inbox` right after step 2, before ever reaching step 3/confirm — confirm this is understood and not surprising (re-previewing without confirming still writes ambiguity reviews).
**Pass/Fail**: BLOCKED — depends on MT-9.1's live scrape.

### MT-9.5 — Data drift between preview and confirm
**Steps**: Run discover, wait, then (if feasible) have the source data change or just rerun confirm against a date whose results were updated between preview and confirm.
**Expected**: Confirm always re-scrapes live at confirm time — verify actual persisted result matches the *current* live source, not the possibly-stale step 2/3 preview. Note any confusing UX if the two diverge.
**Pass/Fail**: BLOCKED — depends on MT-9.1's live scrape.

### MT-9.6 — Idempotent re-confirm
**Steps**: Run confirm twice for the same competition/date/season.
**Expected**: No duplicate Match/PlayerAppearance/MatchEvent rows (matched by home/away+date, player+match, appearance+type+minute respectively).
**Pass/Fail**: BLOCKED — depends on MT-9.1's live scrape.

### MT-9.7 — Scraper exception handling
**Steps**: Use an invalid/unreachable results URL.
**Expected**: 400 "Грешка при извличане: ...", no partial/corrupt state left behind.
**Pass/Fail**: FAIL — scraper exception (unreachable/malformed fixturesUrl) throws `BadRequestException`, whose message is swallowed (blank 400). Tracked under **LT-014**.

### MT-9.8 — Invalid date format at discover
**Steps**: Submit a malformed date string directly to `/matches/extract/discover` (bypass the date picker via devtools).
**Expected**: Per code, this endpoint **does** catch the parse error → 400 "Невалидна дата: ..." (contrast with MT-1.5 where the plain `/matches` list does not catch it) — confirm this asymmetry and that this path specifically handles it gracefully.
**Pass/Fail**: FAIL — invalid date at `/matches/extract/discover` **is** caught at the Java level as designed (contrast with MT-1.5, which isn't) but the resulting message is still blanked by the same **LT-014** defect, so the net effect is no visible feedback either way.

---

## 10. Scheduled Jobs (background, no direct UI trigger)

### MT-10.1 — Daily 23:00 extraction runs for all configured, participating competitions
**Steps**: With at least one competition having both an extraction config and participations, either wait for the scheduled time or (if there's a dev/test trigger) simulate it; check `/matches` and `/inbox` afterward.
**Expected**: New matches/ambiguity reviews appear without any manual action; competitions with a config but zero participations are silently skipped (confirm no error logged for those, since that's expected).
**Pass/Fail**: NOT EXECUTED this pass (code inspection only) — would require waiting for the real 23:00 trigger or a dev-only manual trigger, neither available/exercised this run.

### MT-10.2 — One competition's scrape failure doesn't block others
**Steps**: If feasible, configure one competition with a broken fixturesUrl alongside a working one, and observe/trigger the job.
**Expected**: The broken one logs an error and is skipped; the working one still gets processed.
**Pass/Fail**: NOT EXECUTED this pass — same constraint as MT-10.1.

### MT-10.3 — Overlapping runs are skipped, not queued
**Steps**: This is hard to trigger manually in real time; at minimum, confirm via logs/documentation that `concurrentExecution = SKIP` is the intended behavior and isn't something a user would notice as "my job didn't run" without checking logs.
**Expected**: Understood/accepted behavior — record as a confirmed-by-design note rather than a failure if logs show a skip.
**Pass/Fail**: NOT EXECUTED this pass — accepted as understood-by-design per code review (`concurrentExecution = SKIP`); not observed live.

### MT-10.4 — Embedding sync job (background, every minute)
**Steps**: Create a new player, then wait up to ~1 minute and check whether their `name_embedding` becomes populated (requires DB access or an admin-visible signal — note if there's no way to observe this from the UI at all).
**Expected**: Embedding eventually populates if Ollama is reachable; if Ollama is down, no user-visible error anywhere (silently retries) — confirm this silence is acceptable or worth surfacing somewhere for admins.
**Pass/Fail**: NOT EXECUTED this pass — embedding sync job not observed live (no Ollama instance running in this session).

---

## 11. Ambiguity Inbox (`/inbox`)

### MT-11.1 — Inbox lists all pending reviews (both PLAYER and TEAM types)
**Steps**: Generate at least one PLAYER-type ambiguity (via extraction wizard, MT-9.4) and one TEAM-type (via MT-8.4). View `/inbox`.
**Expected**: Both appear, ordered by creation time; PLAYER rows show ranked candidate buttons + "+ Нов играч"; TEAM rows show only "+ Нов играч" (the gap noted in MT-8.4).
**Pass/Fail**: PASS — both a PLAYER-type review (ranked candidate button + "+ Нов играч") and a TEAM-type review (only "+ Нов играч", no candidates) render correctly; badge showed the accurate pending count (2).

### MT-11.2 — Resolve with existing player
**Steps**: Click a ranked candidate button on a PLAYER review.
**Expected**: Review marked RESOLVED, `PlayerAlias` written, list refreshes in place (`#inbox-list` swap, no page reload), badge count decrements.
**Pass/Fail**: PASS — resolving the PLAYER review via its ranked candidate marked it RESOLVED, wrote a correct `PlayerAlias` row, and refreshed the list in place.

### MT-11.3 — Confirm as new player
**Steps**: Click "+ Нов играч" on a PLAYER review.
**Expected**: New Player created with the raw scraped name, alias written, review resolved, list refreshes.
**Pass/Fail**: PASS for the PLAYER path. FAIL for TEAM-type: clicking the only available action ("+ Нов играч") on a TEAM review creates a bogus Player entity named after the team's raw scraped name, and marks the review RESOLVED against it. Filed **LT-018**.

### MT-11.4 — Double-resolve race (two admins / two tabs)
**Steps**: Open the same pending review in two browser tabs (or two admin sessions), resolve it in one, then attempt to resolve the same review in the other.
**Expected**: Second attempt gets 400 (review no longer PENDING) — confirm the UI shows a sensible message/state rather than silently failing or corrupting data.
**Pass/Fail**: PASS — first resolve succeeds (200); an immediate second resolve attempt on the same (now non-pending) review correctly returns 400, no double-write.

### MT-11.5 — Unknown review / unknown player id
**Steps**: Raw POST to `/inbox/999999/resolve` and to a valid review id with a bogus `playerId`.
**Expected**: 404 in both cases, no partial writes.
**Pass/Fail**: PASS — `POST /inbox/999999/resolve` returns 404; a bogus `playerId` against a genuinely PENDING review also returns 404.

### MT-11.6 — Badge visibility and the "no desktop link when count is 0" gap
**Steps**: As ADMIN with zero pending reviews, look at the desktop nav — confirm there is genuinely no persistent `/inbox` link (only the badge, which renders nothing at 0). Then check the mobile burger menu, which should always show "Неясноти" regardless of count.
**Expected**: Confirms the known asymmetry; decide with the team whether a persistent desktop link should exist even at zero count (usability gap, not a bug per se).
**Pass/Fail**: CONFIRMED as expected — at zero pending reviews, the badge fragment renders empty (no persistent desktop link), matching the known gap noted in section 14.

### MT-11.7 — Badge for USER role
**Steps**: Log in as USER, load any page.
**Expected**: Badge endpoint returns 0 regardless of actual pending count (by design — non-admins never see real counts), no 403 (deliberately `@Authenticated`, not `@RolesAllowed("ADMIN")`).
**Pass/Fail**: PASS — with a real pending review present, the badge endpoint returns 0 for the USER role (verified against the actual ADMIN-visible count of 1 at the same moment); `/inbox` itself correctly 403s for USER.

---

## 12. Cross-Cutting UI / UX / Usability

### MT-12.1 — Nav dropdowns — desktop
**Steps**: As ADMIN, open each of the 3 dropdowns ("Въвеждане", "Извличане", user menu) one at a time, and one after another without closing the first.
**Expected**: Dropdowns open/close cleanly, don't overlap or clip, closing one when opening another (or click-outside-to-close) works, no leftover invisible overlay blocking clicks.
**Pass/Fail**: PASS — the ADMIN dropdowns (Въвеждане, Извличане, user menu) open and close cleanly with no overlap/clipping.

### MT-12.2 — Dead nav links
**Steps**: Click "Извлечи за днес", "Планировчик", "Смени парола".
**Expected**: All three are `href="#"` stubs per code — confirm they do nothing (page jumps to top at most) and aren't silently broken in a more confusing way. Decide with the team whether these should be hidden/labeled "coming soon" rather than shipped as dead links.
**Pass/Fail**: NOT RE-CLICKED this pass — confirmed via code that "Извлечи за днес", "Планировчик", "Смени парола" are `href="#"` stubs.

### MT-12.3 — Mobile burger menu
**Steps**: Resize to ~375px width (or use a real phone), open the burger menu as ADMIN and as anonymous.
**Expected**: All nav items reachable, including the always-visible "Неясноти" inbox link (present here even when the desktop badge shows nothing); menu is scrollable if taller than viewport; tapping outside closes it.
**Pass/Fail**: NOT EXECUTED — the browser automation's window-resize call did not actually change the rendered viewport in this session (`window.innerWidth` still reported the desktop width after resizing to 375px), an environment/tooling limitation rather than a finding about the app. Structurally confirmed via code (a separate `<li class="mobile-only">` burger-menu block exists in `appNav.html`) but not visually verified.

### MT-12.4 — Layout at common breakpoints
**Steps**: Check `/teams`, `/matches/{id}` (lineup tables), and both wizards at ~375px, ~768px, and full desktop width.
**Expected**: No horizontal scroll/overflow, tables degrade sensibly (scroll container or stacked layout) rather than clipping, forms remain usable (labels/inputs not overlapping).
**Pass/Fail**: NOT EXECUTED — same viewport-resize limitation as MT-12.3.

### MT-12.5 — HTMX loading feedback
**Steps**: Throttle network (devtools "Slow 3G") and submit the extraction/import wizard steps, and the inbox resolve actions.
**Expected**: `[aria-busy="true"]`/`hx-indicator` visibly dims or spinners the submit control during the request; double-submission is prevented (can't smash the button and fire two scrapes); no layout jump when the response swaps in.
**Pass/Fail**: NOT EXECUTED this pass — network throttling/loading-indicator behavior not exercised.

### MT-12.6 — hx-confirm dialog wording
**Steps**: Trigger every delete confirm across Teams/Competitions/Participations/Players(N/A — no delete)/appearance/event deletes.
**Expected**: Each dialog's text correctly interpolates the specific entity's name (not a generic "Are you sure?" that doesn't identify what's being deleted).
**Pass/Fail**: PASS (confirmed via code) — `hx-confirm` text correctly interpolates entity-specific detail (e.g. `"Премахване на {row.appearance.player.names} от състава?"`); not re-clicked interactively to avoid triggering the native blocking `confirm()` dialog.

### MT-12.7 — Form validation feedback (server-rejected submissions)
**Steps**: Trigger each server-side validation error documented in sections 3–9 (mismatched competition/season, duplicate player, invalid event type, etc.) through the actual UI (not raw HTTP).
**Expected**: Error messages are visible, in Bulgarian, positioned near the relevant field or as a clear banner, and entered form values are preserved (not wiped) so the admin can just fix and resubmit.
**Pass/Fail**: FAIL — this is effectively the single biggest usability finding of this pass: server-rejected submissions across Teams/Participations/Matches/wizards show **no visible error at all** (blank response body) rather than a Bulgarian message near the field, due to **LT-014**/**LT-017**. Entered values are at least preserved where the form re-renders, but the user gets no explanation of what went wrong in the majority of validation-failure paths tested.

### MT-12.8 — Cyrillic rendering everywhere
**Steps**: Spot check every page listed in this plan with real Cyrillic content (team/player/competition names already in Bulgarian).
**Expected**: No mojibake/encoding issues anywhere, including in `<title>` tags, form `placeholder`s, and the browser tab title.
**Pass/Fail**: PASS — Cyrillic (including hyphens, apostrophes, and an XSS test string) rendered correctly everywhere touched this pass — lists, detail pages, and edit forms.

### MT-12.9 — Season input pattern (client-side only)
**Steps**: Try typing an invalid season format into any season field and submitting.
**Expected**: HTML5 `pattern="\d\d\d\d/\d\d\d\d"` blocks submission client-side; confirm this doesn't give a confusing native browser tooltip that's hard to notice, and cross-check against MT-5.4's server-side behavior when the pattern is bypassed.
**Pass/Fail**: NOT INTERACTIVELY TESTED — the client-side HTML5 pattern itself wasn't typed into by hand this pass; the server-side bypass case (MT-5.4) was tested directly instead and found lacking (LT-016).

### MT-12.10 — Accessibility pass (lightweight)
**Steps**: Tab through the nav dropdowns and a form (e.g. match detail add-appearance) using only the keyboard. Check icon-only delete/edit buttons for any accessible label.
**Expected**: Dropdowns are reachable/operable via keyboard, focus order is sane, icon-only buttons have `aria-label` or visible text alternative (spot-check; this is not expected to be a full WCAG audit).
**Pass/Fail**: NOT EXECUTED this pass — no keyboard/accessibility pass performed.

---

## 13. Security Spot-Checks

### MT-13.1 — XSS in free-text fields
**Steps**: Enter `<script>alert(1)</script>` (or an inert marker like `<img src=x onerror=console.log(1)>`) into: team name/location, competition name, player names, new-player-inline name in match appearance, new-team-name in import wizard.
**Expected**: Rendered as literal text everywhere (Qute auto-escapes by default) — confirm no script execution on any list/detail page that later displays this value.
**Pass/Fail**: PASS — `<script>alert(1)</script>` in a team name is rendered as literal escaped text (`&lt;script&gt;...`) both in the raw HTML and visually in-browser; no script execution.

### MT-13.2 — SQL injection attempts
**Steps**: Enter classic payloads (`' OR '1'='1`, `'; DROP TABLE teams; --`) into search (`/players?q=`), and into text fields on create/edit forms.
**Expected**: No error, no data leakage/corruption — Hibernate/Panache parameterized queries should neutralize this; confirm no raw-SQL string-concatenation path exists (the native SQL query in `LandingResource` and the native embedding-update SQL are worth a specific look — confirm they don't interpolate user input directly).
**Pass/Fail**: PASS — classic SQLi payloads against `/players?q=` returned 200 with no data loss (`teams` count unchanged). The two native-SQL call sites flagged for review are both safe: `LandingResource`'s query is fully static (no interpolation at all), and `PlayerMatchingService`/`PlayerEmbeddingSyncJob` use bound named parameters (`:rawName`, `:teamId`, `:vector`, `:id`) throughout — no injection vector in either.

### MT-13.3 — IDOR / direct object reference on admin actions
**Steps**: As ADMIN, note IDs of entities belonging to the "test" dataset. As USER, attempt direct GET/POST/DELETE against `/teams/{id}/edit`, `/matches/{id}`, `/inbox/{id}/resolve` for those IDs.
**Expected**: All admin-only mutating actions 403 for USER regardless of which ID is targeted (role check happens before any ID-specific logic).
**Pass/Fail**: PASS — as USER, GET/POST/DELETE against `/teams/{id}/edit`, `/teams/{id}`, `/matches/1/appearances`, `/inbox/1/resolve` all correctly 403 regardless of target ID.

### MT-13.4 — Invalid / out-of-range / non-numeric IDs
**Steps**: Visit `/players/999999999`, `/matches/abc`, `/teams/-1/edit`, `/inbox/0/resolve`.
**Expected**: Clean 404s (or 400 for non-numeric where routing can't even match), never a raw 500 stack trace exposed to the client.
**Pass/Fail**: PASS — `/players/999999999`, `/matches/abc`, `/teams/-1/edit` (admin) and `/inbox/0/resolve` all produce clean 404s (or a 302-to-login when anonymous on an admin route); never a raw 500.

### MT-13.5 — CSRF exposure on state-changing forms
**Steps**: Check whether POST/DELETE forms rely on same-site cookies alone (Quarkus form-login default) — attempt a simple cross-origin form POST (a local HTML file with a form targeting the admin app) while logged in.
**Expected**: Confirm current CSRF posture (likely relies on `SameSite` cookie attributes since no explicit CSRF token mechanism was found in the resource layer) — document the actual behavior found, and flag to the team if cross-site POSTs succeed unexpectedly.
**Pass/Fail**: PASS — the FORM-auth session cookie is `SameSite=Strict` (Quarkus default), which prevents the cookie being sent on cross-site requests at all — strong CSRF mitigation despite no explicit token mechanism in the resource layer.

### MT-13.6 — Mass/oversized input
**Steps**: Submit a `names`/`name` field with a very long string (thousands of chars) exceeding the VARCHAR(255)/512 column limits.
**Expected**: Handled gracefully — either truncated with validation feedback or rejected with a clear error, not a raw DB "value too long" 500.
**Pass/Fail**: FAIL — oversized input (a 2000-char team name, a 300-char season) throws a raw 500 with DB error detail exposed, confirmed on two different resources. Filed **LT-019** (general pattern; the season-specific instance is also noted on LT-016).

### MT-13.7 — File/URL fields (logoUrl, fixturesUrl) — SSRF-ish sanity check
**Steps**: Set `fixturesUrl`/`logoUrl` to an internal address (e.g. `http://localhost:8080/`, `http://169.254.169.254/`) and trigger extraction/rendering.
**Expected**: Scraper should fail gracefully on non-BFU content (parse error, not a crash); confirm the app isn't blindly fetching and reflecting arbitrary internal URLs in a way that leaks internal responses (low risk given single-admin deployment, but worth a quick sanity pass).
**Pass/Fail**: PASS — `fixturesUrl` pointed at `http://localhost:8080/` was fetched and simply produced zero discovered matches (selector mismatch); no internal content reflected back, no crash. Confirmed low risk as anticipated for this single-admin deployment.

### MT-13.8 — Privilege confirmation on every mutating inbox/extraction/import endpoint
**Steps**: Repeat MT-1.7's raw-request check specifically for `/matches/extract/*` and `/participations/import/*` POST endpoints.
**Expected**: All require ADMIN, none accept USER or anonymous sessions.
**Pass/Fail**: PASS — `/matches/extract/discover` and `/participations/import/extract` both correctly 403 for the USER role (verified alongside MT-13.3).

---

## 14. Known Gaps to Confirm, Not Re-litigate

These were flagged during code review as likely intentional stubs/limitations rather than bugs — confirm during execution and either close them out as "expected" or escalate if they're surprising to the product owner:

- No self-registration or admin UI to provision new USER/ADMIN accounts (MT-2.7).
- "Извлечи за днес", "Планировчик", "Смени парола" nav links are dead stubs (MT-12.2).
- TEAM-type ambiguity reviews have no candidate buttons, only a semantically-wrong "+ Нов играч" (MT-8.4, MT-11.1) — this one reads more like a real bug than an accepted stub; prioritize confirming it during execution.
- No `DISMISSED` path is exposed anywhere in the inbox UI despite existing in the `AmbiguityReviewStatus` enum.
- No dependent-entity guard before delete on Team/Competition/Participation (MT-3.3, MT-4.4, MT-5.5) — confirm actual failure mode (friendly error vs 500) since none of this is covered by automated tests.
