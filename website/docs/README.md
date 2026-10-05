# Lineup Tracker — Documentation

Lineup Tracker scrapes match lineups, scores and events for Bulgarian Football Union (BFU)
competitions from `bfu-tournaments.com` (with `ebfu.net` as a lineup-only fallback). It stores
them in PostgreSQL and serves them through a Bulgarian-language web UI.

| Guide | Read it when you want to… |
|-------|---------------------------|
| [Developer setup](developer-setup.md) | build, run, test, or change the code |
| [Install & update](install-and-update.md) | run the app for real on your machine, upgrade it, back it up |
| [First season: getting data in](first-season.md) | go from an empty database to competitions, teams and matches |
| [Rolling over to a new season](new-season.md) | start the next season without breaking the previous one |
| [Querying players, teams and matches](querying.md) | find things in the UI, or run SQL for questions the UI can't answer |
| [Use cases](use_cases.puml) ([specs](use_cases/)) and [entity model](entity_model.md) | know what the system must do: the AI Unified Process specification every change starts from |

## Concepts in one minute

The data model is easier to follow once you know these terms. The rest of the docs use them
throughout.

- **Team** (`Отбор`): a club, e.g. "Левски София". It has a name, a city and an optional logo.
- **Formation** (`Формация`): one squad of a club. The types are `U15`–`U19`, `FIRST` (shown as
  "Мъже", the men's first team), `SECOND` ("II") and `THIRD` ("III"). A club can have several.
- **Competition** (`Лига / турнир`): a league, e.g. "Елитна юношеска група U17". It can carry an
  *extraction config* (a results URL plus the current season), which makes it eligible for the
  nightly job.
- **Participation** (`Участие`): one formation playing in one competition in one season, e.g.
  "Левски U17 in Елитна U17, 2025/2026". **Matches hang off participations, not teams.** A team
  with no participation for the season can't have matches recorded.
- **Season** is always the string `YYYY/YYYY`, e.g. `2025/2026`.
- **Match**: a home and an away participation, a date and a score. Each match has
  **appearances** (a player in the lineup, starter or reserve, with substitution minutes) and
  **events** on an appearance (goal, penalty goal, own goal, yellow card, second yellow, red card).
- **Aliases**: the remembered mappings from a name as written on a BFU site to your canonical team
  or player. They're learned during imports and inbox decisions, and they're what lets later
  extractions run hands-free.
- **Ambiguity inbox** (`/inbox`, "Неясноти"): scraped names that the app couldn't map with
  confidence wait here for an admin to decide. The app never guesses.

## Roles

| Who | Can |
|-----|-----|
| Anonymous | Read every list and detail page (teams, competitions, participations, players, matches). |
| `USER` | Same as anonymous. They also get the `/app` search home page after login. |
| `ADMIN` | Everything: create, edit and delete, both wizards, the inbox. |

There's no sign-up page or user-management UI. Accounts are created with SQL (see
[Install & update → Managing accounts](install-and-update.md#managing-accounts)).
