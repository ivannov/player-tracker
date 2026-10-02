# First Season: Getting Data In

This guide takes a fresh, empty install to a working dataset. You log in as `admin` for all of it.
Read the [Concepts](README.md#concepts-in-one-minute) first if the terms *formation* and
*participation* are new.

Overview:

1. Create the competitions.
2. Import each competition's teams (participations) from BFU.
3. Extract matches, round by round.
4. Clear the ambiguity inbox, then re-run extraction to fill in the lineups.
5. Let the nightly job take over.

The order matters. **Matches can only be saved for teams that have a participation in that
competition and season**, and **players can only be matched automatically against players who have
already appeared for that team.**

## 1. Create competitions

**Въвеждане → Лиги / турнири → + Добави** (`/competitions/new`)

| Field | Value |
|-------|-------|
| Наименование (name) | e.g. `Първа лига`, `Елитна юношеска група U17` |
| Лого URL | optional; BFU-hosted image URLs work |
| URL на календара | the league's **results** page on bfu-tournaments.com with the season in the query, e.g. `https://bfu-tournaments.com/leagues/first?season=2025-2026&view=past-matches` |
| Текущ сезон | `2025/2026` |

The last two fields together form the competition's **extraction config**. If both are filled, the
nightly job processes this competition (once it also has participations). If you leave either one
blank, the competition is never extracted automatically, but you can still use the wizard on it.

The results page's season parameter uses a dash (`2025-2026`). The app's season field uses a slash
(`2025/2026`).

## 2. Import teams and participations

**Въвеждане → Участия → Импорт от БФС** (`/participations/import`)

### Import the men's leagues first, then the youth leagues

When the wizard creates a brand-new team, it always gives that team a **`FIRST` (Мъже)**
formation and puts that formation in the competition. That's right for men's leagues. For a youth
league it's wrong: you'd get "Club X Мъже" playing in the U17 league.

Do it in this order:

1. Import the three men's leagues. New clubs are created with `FIRST`, which is correct.
2. Import the youth leagues. Most clubs already exist by then. For each one, pick the existing team
   and choose **— добави нова формация —** → `U17` (or whichever age group).
3. For a youth-only club or academy that doesn't exist yet, create it first at
   **Отбори → + Добави** with the right formation ticked (e.g. `U17`), then run the import.
   (Alternative: create it during import, then, *before* extracting any matches, edit that
   participation group at **Участия → ✎** and swap `Мъже` for the age group.)

### Step 1: source

- **URL на лигата в БФС**: the league's page on bfu-tournaments.com that lists all of its teams
  (the scraper reads every team logo and name on the page).
- **Лига / турнир**: the competition you created in step 1.
- **Сезон**: `2025/2026`.

Click **Извлечи отбори**.

### Step 2: map each scraped team

Each row shows the team name as BFU writes it (*Извлечен отбор*):

- If a team with exactly that name (case-insensitive) already exists, it's pre-selected.
- Otherwise, either **search** for the existing club (type part of the name and pick from the list),
  or click **+ Нов отбор** and enter the canonical name and city.
- **Формация**: once a team is chosen, pick one of its formations, or pick
  **— добави нова формация —** and choose a type. New teams always get `FIRST`.

Leaving a row empty skips it.

### Step 3: review

Every row shows what saving will do:

| Action | Meaning |
|--------|---------|
| `CREATE` | a new participation (and a new team/formation, if marked "нов"/"нова") |
| `EXISTS` | this formation already participates in this competition and season. Nothing to do |
| `SKIP` | the row is empty or invalid (no formation chosen, unknown team, …). Go back and fix it if it wasn't intended |

Save. You land on `/participations`, which should now list the league's teams for the season.

Saving also records a **team alias** for every row (BFU name → your team). Match extraction relies
on it, so a club whose BFU name differs from your canonical name still resolves later. Re-running
the same import is safe: existing participations show as `EXISTS` and aren't duplicated.

If the page shows *"Грешка при извличане: …"*, the URL is wrong or bfu-tournaments.com refused the
request. The site is known to block some networks/clients with HTTP 403. Retry from another network,
or create participations by hand at **Участия → + Добави** (team, competition, season, formation
types).

## 3. Extract matches

**Извличане → Извлечи за дата…** (`/matches/extract`)

### Step 1

- **Лига / турнир**: the competition.
- **URL на резултатите в БФС**: the same results URL as in the competition's config. It isn't
  pre-filled.
- **Сезон**: `2025/2026`. This must match the participations' season exactly.
- **Дата на кръга**: the date matches were played. Only matches on that exact day are picked up.

Click **Открий мачове**.

### Step 2: discovered matches

The app shows one row per match found on that date. For each row it gives:

- **Home / away team**: resolved to your team, or **"непознат отбор"** (unknown team). A side
  counts as resolved only if the team exists *and* has a participation in this competition and
  season.
- **Players (разпознати / неясни)**: how many lineup names matched an existing player, and how many
  are ambiguous.
- Any per-match scraping error.

Match details come from bfu-tournaments.com: lineups, reserves, score, goals, cards and
substitutions. If that fails for a match, the app falls back to **ebfu.net**, which gives **lineups
only** (no score, events or substitutions).

**This step already writes to the inbox.** Ambiguous player names are queued for review as soon as
you preview, even if you never confirm.

### Step 3: confirm

Rows marked **Готов за запис** are saved. Rows marked **Не може да се запише** (an unresolved team)
are skipped entirely. **Потвърди и запази** scrapes the source again live and saves:

- the **match** (home/away participation, date, score), reused if it already exists for those
  teams on that date;
- an **appearance** for every *resolved* player (starter or reserve, shirt number, substitution
  minutes);
- **events** (goals, cards) for resolved players.

Players who are still unresolved aren't saved yet. They wait in the inbox.

Confirming is **idempotent**. Running it again for the same competition and date doesn't duplicate
matches, appearances or events. It only adds what's newly resolvable. The next step relies on this.

## 4. The first-round inbox, then re-extract

On the very first extraction for a team, **no player is known yet**. Candidates are drawn only
from players who already appeared for that team. So every player of every team in the round lands
in the inbox, and the saved matches have scores but empty lineups. That's expected.

**Неясноти** (`/inbox`; on desktop, the count badge appears in the nav when something is pending):

| Row type | Actions |
|----------|---------|
| Player, with candidates | click the right candidate button `Name (score)` to link this spelling to that player |
| Player, any | **+ Нов играч: <name>** creates a new player with the scraped name |
| Team (a BFU name now maps to a different club than before) | pick the correct club from the list → **Свържи с отбор** |

For the first round, it's almost all **+ Нов играч**. Each decision is remembered as an alias
(scraped name + team → player), so the same spelling for the same team resolves automatically
from then on.

**Resolving in the inbox doesn't add the player to the already-saved match.** Afterwards, run the
extraction wizard again for the same competition and date and confirm. The now-known players are
added to the lineups, together with their events and substitutions.

The routine, per round:

```
extract date D → clear inbox → extract date D again → (check a match at /matches/{id})
```

The inbox shrinks fast after the first round or two. From then on it mostly holds spelling
variants ("Иван Петров" vs "Ив. Петров") with ranked candidates. If Ollama is running, semantic
similarity helps rank those candidates.

## Backfilling past rounds

The nightly job only processes *today*. To load rounds that were already played (or days when the
machine was off), run the wizard once per round date. List the round dates from the competition's
results page on bfu-tournaments.com. Work oldest to newest, so each round's inbox decisions help
the next.

## 5. Let the nightly job take over

Every day at **23:00**, the app extracts *today's* matches and confirms them. It does this for
every competition that has **both** an extraction config (results URL + current season) **and** at
least one participation. Competitions that fail are logged and skipped. The rest continue.

The job uses the server's clock. The Docker container runs on UTC, so the job fires at
01:00–02:00 Sofia time and processes the UTC date, which is the Bulgarian match day that just
ended.

Your daily or weekly routine:

1. Open `/inbox` and resolve what's pending.
2. For each date you resolved anything on, re-run **Извлечи за дата…** so those players appear in
   the lineups.
3. Spot-check new matches at `/matches`.

## Manual entry and corrections

- **New match:** **Мачове → + Нов мач**. Both participations must be in the same competition and
  season.
- **Lineup edits:** on a match page (`/matches/{id}`), add a player (an existing one via search,
  or **+ Нов играч**), set substitution minutes, add or delete events, remove an appearance.
- **Rename a player:** **Играчи → ✎**. Players can't be deleted.
- **Rename a team:** edit it at **Отбори → ✎**. Deselecting a formation on a team also deletes
  that formation's participations. The database rejects this, with an error page, if matches
  reference them.
- Deletes are refused while anything depends on the record. A competition can't be deleted while
  it has participations. A participation can't be deleted while it has matches or lineups. A team
  can't be deleted while it has formations (deselect them all first).

## Known limitations

- **Transfers create duplicates.** When a player moves to another club, their name is scraped
  under the new club. Inbox candidates come only from players who already played for *that* club,
  so the old player record isn't offered, and **+ Нов играч** creates a second record. There's
  currently no merge function. Moves *within* a club (U17 → U19 → Мъже) are fine, because
  candidates are scoped to the club, not the formation.
- **ebfu.net fallback and team names.** Aliases learned by the import are bfu-tournaments.com
  aliases. A match that only ebfu.net could provide resolves its teams by exact canonical name
  (case-insensitive), or by aliases from earlier ebfu-sourced matches.
- The **"Извлечи за днес"**, **"Планировчик"** and **"Смени парола"** menu items are placeholders
  and do nothing yet.
