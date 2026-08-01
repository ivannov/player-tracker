---
id: LT-018
title: >-
  TEAM-type ambiguity reviews have no way to resolve the team conflict -- only a
  wrong '+ Нов играч' that creates a bogus Player
status: To Do
assignee: []
created_date: '2026-08-01 04:24'
updated_date: '2026-08-01 04:25'
labels:
  - bug
  - data-integrity
  - inbox
dependencies: []
priority: high
ordinal: 41000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
MT-8.4/MT-11.1/MT-11.3 manual test execution (LT-011.02) confirmed by reproduction (not just code review) the gap flagged in the test plan's Known Gaps section: AmbiguityCandidate rows are only ever created for PLAYER-type reviews (see TeamResolutionService / the extraction and import services) -- TEAM-type AmbiguityReview rows never get candidates. The /inbox UI has exactly one action available for any review regardless of type: '+ Нов играч: {rawName}', which always creates a new Player entity and resolves the review by linking resolved_player_id to it.

Reproduced end-to-end: seeded a TEAM-type review with raw_name 'ФК Тестов Конфликт' (simulating a team-name remapping conflict), then clicked its only available action in /inbox. Result: a Player row was created with names='ФК Тестов Конфликт' (a football club's name, stored as if it were a person), and the review was marked RESOLVED pointing at this bogus Player. There is no UI path to actually resolve a TEAM ambiguity (e.g. picking the correct existing Team, or creating a new Team) -- admins have no way to do the right thing here, and the only button available actively corrupts data (a team ends up as a fake 'player').

Needs either: (a) a TEAM-specific resolution UI (list candidate Teams, or '+ Нов отбор' to create a real Team) mirroring the PLAYER flow, or at minimum (b) hide/disable the '+ Нов играч' action for TEAM-type reviews until real resolution UI exists, so admins can't accidentally trigger this.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 TEAM-type ambiguity reviews have a correct resolution path that does not create a Player
- [ ] #2 The current '+ Нов играч' action is no longer reachable for TEAM-type reviews unless it is the correct action
<!-- AC:END -->
