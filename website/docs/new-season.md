# Rolling Over to a New Season

Seasons don't overwrite each other. Every participation carries its season string, so the
2025/2026 data stays queryable as you add 2026/2027. Players and teams are shared across seasons,
and so are the aliases learned for them. That's why the second season needs far less inbox work
than the first.

The examples below assume a move from `2025/2026` to `2026/2027`.

## The trap: stale extraction configs

The nightly job uses each competition's **results URL + current season** as-is. The fixtures
scraper finds a day's matches by **day and month only**. If a competition still points at last
season's results page when the new season starts, the job can pick up *last season's* matches from
the same calendar day. It would then save them under today's date.

So **pause extraction at the end of each season**. On each competition's edit page, clear the
**URL на календара** field and save. That deletes the extraction config, and the job skips the
competition until you set it again (step 3 below).

## Checklist

### 1. Finish the old season

- Make sure the last rounds are extracted (see
  [First season → Backfilling](first-season.md#backfilling-past-rounds)), the inbox is empty, and
  you've re-run extraction for any dates you resolved players on.
- Pause the extraction configs as described above.
- Take a backup: `pt exec backup /backup.sh` (see [Install & update](install-and-update.md)).

### 2. Import the new season's participations

For every competition, run **Участия → Импорт от БФС** with the new season:

- **URL на лигата в БФС**: the league page for the new season.
- **Лига / турнир**: the *same* competition record as last season. Don't create a new competition
  per season.
- **Сезон**: `2026/2027`.

What to expect in step 2:

- **Clubs that stayed in the league** are pre-selected if their BFU name equals your canonical
  team name exactly. Otherwise, search for and pick them again. Pick the **same formation** as
  last season (e.g. the existing `U17`).
- **Promoted or relegated clubs** already exist from the other league, so pick them. A club moving
  between men's tiers keeps its `FIRST` formation. Only the competition changes.
- **Brand-new clubs:** use **+ Нов отбор**. For youth leagues, create the club first at
  `/teams/new` with the right formation (same rule as in
  [First season](first-season.md#import-the-mens-leagues-first-then-the-youth-leagues)).
- **Age-group roll-over** in youth leagues needs no special handling. A club's `U17` formation is
  the same record every year. Only its participation is new.

Do the men's leagues first, then the youth leagues. This is the same reason as in the first
season: new clubs created by the wizard always get `FIRST`.

If you map a BFU name to a *different* club than it was mapped to before, the app keeps the old
mapping and puts a **team** review in the inbox. Resolve it there (**Свържи с отбор**) if the new
mapping is the correct one.

Check `/participations`. It's sorted newest season first, so the new season's rows appear at the
top.

### 3. Point the competitions at the new season

On each competition's edit page (`/competitions/{id}/edit`):

| Field | New value |
|-------|-----------|
| URL на календара | new season's results page, e.g. `…/leagues/first?season=2026-2027&view=past-matches` |
| Текущ сезон | `2026/2027` |

Do this **after** step 2. The nightly job only runs for competitions that have participations,
and teams without a new-season participation can't be matched, so their matches would be skipped
as "непознат отбор".

### 4. First rounds of the new season

Run the extraction wizard for the first round (or let the nightly job do it). Expect:

- **Returning players at the same club resolve automatically.** Their aliases are stored per club,
  not per season or formation, and that includes youth players stepping up an age group or into
  the first team.
- **Inbox entries** for new signings, debutants, and spelling variants. Use candidate buttons where
  they fit, and **+ Нов играч** otherwise.
- After resolving, **re-run the extraction for that date** so the players get added to the
  lineups.

**Transfers between clubs.** A player arriving from another club isn't offered as a candidate,
because candidates come only from the new club's own past players. **+ Нов играч** will create a
second record for that person, and the app has no merge function yet. If keeping careers in one
record matters for a particular player, add their appearances by hand on the match page
(`/matches/{id}` → pick the existing player) rather than resolving the inbox entry. Be aware that
the inbox entry then stays pending, and the same name will be queued again by later extractions.
In practice it's usually better to accept the duplicate for now and track it.

### 5. Verify

- `/matches?competitionId=<id>`: new-season matches appear with scores.
- Open one match: lineups, events and substitutions are filled in.
- Open a long-serving player at `/players/{id}`: the timeline continues across both seasons.
