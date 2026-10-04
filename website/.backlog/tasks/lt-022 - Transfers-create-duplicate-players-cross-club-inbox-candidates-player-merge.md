---
id: LT-022
title: 'Transfers create duplicate players: cross-club inbox candidates + player merge'
status: To Do
assignee: []
created_date: '2026-10-04 06:02'
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
- [ ] #1 Inbox PLAYER reviews include similar players from other clubs, labelled with their latest club
- [ ] #2 Auto-resolve stays scoped to the scraping club
- [ ] #3 Admin can merge a duplicate player into another from the player detail page
- [ ] #4 Merge refuses when both players appeared in the same match
- [ ] #5 Tests cover cross-club candidates and merge
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 Tests are added for new functoinality and mvn verify is successfull
<!-- DOD:END -->
