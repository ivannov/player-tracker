---
id: LT-022
title: 'Transfers create duplicate players: cross-club inbox candidates + player merge'
status: Done
assignee: []
created_date: '2026-10-04 06:02'
updated_date: '2026-10-04 06:06'
labels: []
dependencies: []
priority: medium
ordinal: 45000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
When a player transfers, inbox candidates are scoped to the new club only, so the old Player record is never offered and '+ Нов играч' creates a duplicate. Fix: (1) inbox reviews also list top trigram matches from other clubs, labelled with their latest club (auto-resolve stays club-scoped); (2) admin merge action on the player detail page that moves appearances/aliases/candidates/review refs to the kept player and deletes the duplicate, refusing when both played the same match.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Inbox PLAYER reviews include similar players from other clubs, labelled with their latest club
- [x] #2 Auto-resolve stays scoped to the scraping club
- [x] #3 Admin can merge a duplicate player into another from the player detail page
- [x] #4 Merge refuses when both players appeared in the same match
- [x] #5 Tests cover cross-club candidates and merge
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Cross-club review candidates (trigram >= 0.5, top 3, never auto-resolved) labelled with latest club in the inbox. Admin merge at POST /players/{id}/merge with similar-name suggestions on the player page; refuses same-match conflicts; dedupes ambiguity candidates. Added GIN trigram index on players.names in V1. Docs updated (new-season.md, first-season.md). Full suite 253/253 green.
<!-- SECTION:NOTES:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 Tests are added for new functoinality and mvn verify is successfull
<!-- DOD:END -->
