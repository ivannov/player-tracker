---
id: LT-026
title: Recompute a player's name embedding after a rename
status: To Do
assignee: []
created_date: '2026-10-08 19:39'
labels:
  - aiup
  - UC-013
  - UC-012
dependencies: []
documentation:
  - docs/use_cases/UC-013-compute-player-name-embeddings.md
  - docs/use_cases/UC-012-manage-players.md
priority: medium
ordinal: 49000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Covers UC-013 (step 1) and UC-012 (editing player names). A player whose names are edited keeps the semantic representation of the old name, because the scheduled run only processes players that have no representation. Semantic identification during extraction (UC-009 BR-004) then compares scraped names with the stale name. Spec change: update UC-013 (and UC-012 if renaming must clear the representation) with /use-case-spec and get approval, then implement and remove the Note.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 UC-013 step 1: a player whose name changed since the representation was computed is selected again
- [ ] #2 UC-013 BR-002: until the new representation exists, identification of that player falls back to name similarity, not the stale representation
- [ ] #3 UC-013: the > Note about renamed players keeping the old representation is removed from the spec
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 Tests are added for new functoinality and mvn verify is successfull
<!-- DOD:END -->
