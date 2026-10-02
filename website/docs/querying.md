# Querying Players, Teams and Matches

All list and detail pages are public, so you don't need to log in to browse. This guide covers
what the UI answers, then SQL recipes for questions it doesn't (yet).

## In the UI

### Players: `/players`

- **Search:** type any part of a name, e.g. `петров`, at **Играчи** (`/players?q=петров`). The
  search is a case-insensitive substring match. Logged-in users also get the same search box on the
  `/app` home page.
- **Career timeline:** click a player (`/players/{id}`) for every recorded appearance, oldest
  first. Each row shows the date, the team plus formation (`Левски U17`, `Левски II`; men's first
  team has no suffix), the competition, the season, whether they started (`Титуляр`), and their
  events in that match (`10' Гол, 55' Жълт картон`).

Search matches the stored spelling only. If you can't find someone, try a shorter fragment (just
the family name), or check for the duplicate-player case described in
[First season → Known limitations](first-season.md#known-limitations).

### Matches: `/matches`

- **Filter** by **Лига / турнир**, by **Дата**, or both. Results are sorted newest first.
  URL form: `/matches?competitionId=3&date=2026-03-14`.
- **Match page** (`/matches/{id}`): the score, plus both lineups with starters and reserves, shirt
  numbers, substitution minutes, goals and cards.
- A score shown as `—` means none was scraped. That's typical for matches that came from the
  ebfu.net fallback.

### Teams, competitions, participations

- `/teams`: every club, with its city and logo.
- `/competitions`: every league.
- `/participations`: which formation of which club played in which league and season, newest
  season first. This is the closest thing to a "league membership by season" view.

There's no team-statistics page yet. Use the SQL below for squads, records and scorers.

## SQL recipes

Open a psql shell:

```bash
# local production stack (pt helper from install-and-update.md)
pt exec db sh -c 'psql -U "$POSTGRES_USER" -d lineup'

# dev
docker compose exec db psql -U lineup -d lineup
```

These are read-only queries. Replace the literal values (ids, names, seasons) with your own.
Names must match the stored spelling exactly. Find ids first with the lookup queries.

### Schema cheat-sheet

```
teams(id, name, location) ─< team_formations(id, team_id, type)          type: U15..U19, FIRST, SECOND, THIRD
team_formations ─< participations(id, team_formation_id, competition_id, season)   season: 'YYYY/YYYY'
competitions(id, name) ─< participations
participations ─< matches(id, home_team_id, away_team_id, date, home_score, away_score)   (FKs point at participations)
players(id, names) ─< player_appearances(id, player_id, match_id, participation_id, starter, number,
                                         substituted_in_minute, substituted_out_minute)
player_appearances ─< match_events(id, player_appearance_id, type, minute)
                      type: GOAL, PENALTY_GOAL, OWN_GOAL, YELLOW_CARD, SECOND_YELLOW_CARD, RED_CARD
```

An appearance means the player was named in the matchday squad. A reserve who never came on
still has an appearance (`starter = false`, no `substituted_in_minute`). The recipes below count
**played** as `starter OR substituted_in_minute IS NOT NULL`.

### Look-ups

```sql
-- Players by name fragment
SELECT id, names FROM players WHERE names ILIKE '%петров%' ORDER BY names;

-- Fuzzy (typos, transliteration). Uses the pg_trgm extension the app already installs
SELECT id, names, round(similarity(names, 'Иван Петроф')::numeric, 2) AS score
FROM players WHERE names % 'Иван Петроф' ORDER BY score DESC LIMIT 10;

-- Teams and their formations
SELECT t.id, t.name, t.location, string_agg(tf.type, ', ' ORDER BY tf.type) AS formations
FROM teams t LEFT JOIN team_formations tf ON tf.team_id = t.id
GROUP BY t.id ORDER BY t.name;
```

### A player's career, by season

```sql
WITH ev AS (
  SELECT player_appearance_id,
         COUNT(*) FILTER (WHERE type IN ('GOAL', 'PENALTY_GOAL'))          AS goals,
         COUNT(*) FILTER (WHERE type = 'YELLOW_CARD')                       AS yellows,
         COUNT(*) FILTER (WHERE type IN ('SECOND_YELLOW_CARD', 'RED_CARD')) AS reds
  FROM match_events GROUP BY player_appearance_id
)
SELECT p.season, c.name AS competition, t.name AS team, tf.type AS formation,
       COUNT(*) FILTER (WHERE pa.starter OR pa.substituted_in_minute IS NOT NULL) AS played,
       COUNT(*) FILTER (WHERE pa.starter)                                         AS started,
       COALESCE(SUM(ev.goals), 0)   AS goals,
       COALESCE(SUM(ev.yellows), 0) AS yellows,
       COALESCE(SUM(ev.reds), 0)    AS reds
FROM player_appearances pa
JOIN participations p   ON p.id = pa.participation_id
JOIN competitions c     ON c.id = p.competition_id
JOIN team_formations tf ON tf.id = p.team_formation_id
JOIN teams t            ON t.id = tf.team_id
LEFT JOIN ev            ON ev.player_appearance_id = pa.id
WHERE pa.player_id = 1                       -- player id
GROUP BY p.season, c.name, t.name, tf.type
ORDER BY p.season, c.name;
```

### A team's squad in a season

```sql
WITH ev AS (
  SELECT player_appearance_id,
         COUNT(*) FILTER (WHERE type IN ('GOAL', 'PENALTY_GOAL')) AS goals,
         COUNT(*) FILTER (WHERE type = 'YELLOW_CARD')              AS yellows
  FROM match_events GROUP BY player_appearance_id
)
SELECT pl.id, pl.names,
       COUNT(*)                                                                   AS in_squad,
       COUNT(*) FILTER (WHERE pa.starter OR pa.substituted_in_minute IS NOT NULL) AS played,
       COUNT(*) FILTER (WHERE pa.starter)                                         AS started,
       COALESCE(SUM(ev.goals), 0)   AS goals,
       COALESCE(SUM(ev.yellows), 0) AS yellows
FROM player_appearances pa
JOIN players pl         ON pl.id = pa.player_id
JOIN participations p   ON p.id = pa.participation_id
JOIN team_formations tf ON tf.id = p.team_formation_id
JOIN teams t            ON t.id = tf.team_id
LEFT JOIN ev            ON ev.player_appearance_id = pa.id
WHERE t.name = 'Левски' AND tf.type = 'U17' AND p.season = '2025/2026'
GROUP BY pl.id, pl.names
ORDER BY played DESC, pl.names;
```

### A team's record in a season (W/D/L, goals)

```sql
WITH tm AS (
  SELECT m.home_score, m.away_score, hp.team_formation_id = tf.id AS is_home
  FROM matches m
  JOIN participations hp  ON hp.id = m.home_team_id
  JOIN participations ap  ON ap.id = m.away_team_id
  JOIN team_formations tf ON tf.id IN (hp.team_formation_id, ap.team_formation_id)
  JOIN teams t            ON t.id = tf.team_id
  WHERE t.name = 'Левски' AND tf.type = 'U17' AND hp.season = '2025/2026'
    AND m.home_score IS NOT NULL               -- skip matches without a score
)
SELECT COUNT(*) AS played,
       COUNT(*) FILTER (WHERE (is_home AND home_score > away_score) OR (NOT is_home AND away_score > home_score)) AS won,
       COUNT(*) FILTER (WHERE home_score = away_score)                                                          AS drawn,
       COUNT(*) FILTER (WHERE (is_home AND home_score < away_score) OR (NOT is_home AND away_score < home_score)) AS lost,
       SUM(CASE WHEN is_home THEN home_score ELSE away_score END) AS goals_for,
       SUM(CASE WHEN is_home THEN away_score ELSE home_score END) AS goals_against
FROM tm;
```

### Matches with readable team names

```sql
SELECT m.id, m.date, c.name AS competition, hp.season,
       ht.name || CASE htf.type WHEN 'FIRST' THEN '' WHEN 'SECOND' THEN ' II' WHEN 'THIRD' THEN ' III' ELSE ' ' || htf.type END AS home,
       m.home_score, m.away_score,
       awt.name || CASE atf.type WHEN 'FIRST' THEN '' WHEN 'SECOND' THEN ' II' WHEN 'THIRD' THEN ' III' ELSE ' ' || atf.type END AS away
FROM matches m
JOIN participations hp   ON hp.id = m.home_team_id
JOIN participations ap   ON ap.id = m.away_team_id
JOIN competitions c      ON c.id = hp.competition_id
JOIN team_formations htf ON htf.id = hp.team_formation_id
JOIN team_formations atf ON atf.id = ap.team_formation_id
JOIN teams ht            ON ht.id = htf.team_id
JOIN teams awt           ON awt.id = atf.team_id
WHERE (ht.name, awt.name) IN (('Левски', 'ЦСКА'), ('ЦСКА', 'Левски'))   -- head-to-head; drop for all matches
ORDER BY m.date DESC;
```

### One match's lineups

```sql
SELECT t.name AS team, pa.starter, pa.number, pl.names,
       pa.substituted_in_minute AS sub_in, pa.substituted_out_minute AS sub_out,
       string_agg(concat(e.minute, ''' ', e.type), ', ' ORDER BY e.minute)
         FILTER (WHERE e.id IS NOT NULL) AS events
FROM player_appearances pa
JOIN players pl         ON pl.id = pa.player_id
JOIN participations p   ON p.id = pa.participation_id
JOIN team_formations tf ON tf.id = p.team_formation_id
JOIN teams t            ON t.id = tf.team_id
LEFT JOIN match_events e ON e.player_appearance_id = pa.id
WHERE pa.match_id = 1                        -- match id (from the /matches/{id} URL)
GROUP BY t.name, pa.id, pl.names
ORDER BY t.name, pa.starter DESC, pa.number;
```

### Top scorers in a competition and season

```sql
SELECT pl.id, pl.names, t.name AS team, COUNT(*) AS goals
FROM match_events e
JOIN player_appearances pa ON pa.id = e.player_appearance_id
JOIN players pl            ON pl.id = pa.player_id
JOIN participations p      ON p.id = pa.participation_id
JOIN competitions c        ON c.id = p.competition_id
JOIN team_formations tf    ON tf.id = p.team_formation_id
JOIN teams t               ON t.id = tf.team_id
WHERE e.type IN ('GOAL', 'PENALTY_GOAL')
  AND c.name = 'Елитна U17' AND p.season = '2025/2026'
GROUP BY pl.id, pl.names, t.name
ORDER BY goals DESC, pl.names
LIMIT 20;
```

### Data-health checks

```sql
-- Matches with no lineup at all: re-run extraction for these dates after clearing the inbox
SELECT m.id, m.date FROM matches m
WHERE NOT EXISTS (SELECT 1 FROM player_appearances pa WHERE pa.match_id = m.id)
ORDER BY m.date;

-- Pending inbox items per team
SELECT t.name, r.type, COUNT(*) FROM ambiguity_reviews r JOIN teams t ON t.id = r.team_id
WHERE r.status = 'PENDING' GROUP BY t.name, r.type ORDER BY COUNT(*) DESC;

-- Possible duplicate players (same name, several records, e.g. after a transfer)
SELECT names, COUNT(*) AS records, array_agg(id ORDER BY id) AS ids
FROM players GROUP BY names HAVING COUNT(*) > 1 ORDER BY names;
```
