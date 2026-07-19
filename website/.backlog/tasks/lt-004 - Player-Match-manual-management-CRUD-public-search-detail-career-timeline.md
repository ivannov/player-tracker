---
id: LT-004
title: >-
  Player & Match manual management (CRUD + public search/detail + career
  timeline)
status: Done
assignee: []
created_date: '2026-07-11 15:39'
updated_date: '2026-07-19 12:24'
labels:
  - frontend
  - backend
  - player
  - match
dependencies:
  - LT-002
  - LT-003
priority: high
ordinal: 8000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
There is no PlayerResource or MatchResource today -- a Player or Match can only be created as a side effect of code that doesn't exist yet either (PlayerAppearance has no UI). There is no player search and no way to see 'which teams did this player play for, from U15 to senior' -- the core promise of the whole product.

Scope, split across three subtasks:
- PlayerResource (/players): public list+search by name, public detail page showing the career timeline (every PlayerAppearance joined through Match -> Participation -> TeamFormation -> Team/Competition, ordered by match date, with team/competition/season/starter-or-reserve/goals/cards). Admin-only create/edit (the Player model only has a 'names' field today).
- MatchResource (/matches): public list (filterable by competition and date) + detail page (score, both full lineups with reserves, goals/cards/subs per player, linking to player detail pages). Admin-only create (homeTeam/awayTeam picked from Participations in the same competition+season, plus date and optional score).
- Manual lineup/event entry UI on the match detail page (admin-only view): add/remove PlayerAppearance rows (search existing player or create one inline), mark MatchEvents (goal/card types + minute) and substitution minutes for any player already on the sheet.

Follow existing conventions exactly: @CheckedTemplate + Qute native templates, @RestForm form beans, JOIN FETCH list/detail queries (see ParticipationResource.list() in src/main/java/com/nosoftskills/lineup/resource/ for the pattern), TrackerEntity base, @JoinColumn on every multi-word FK. Security follows the public-read/admin-write pattern established in the security epic (LT-002) -- do not put class-level @Authenticated on these new resources.
<!-- SECTION:DESCRIPTION:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 Tests are added for new functoinality and mvn verify is successfull
<!-- DOD:END -->
