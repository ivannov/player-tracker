---
id: LT-018
title: >-
  TEAM-type ambiguity reviews have no way to resolve the team conflict -- only a
  wrong '+ Нов играч' that creates a bogus Player
status: Done
assignee: []
created_date: '2026-08-01 04:24'
updated_date: '2026-08-02 15:21'
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
- [x] #1 TEAM-type ambiguity reviews have a correct resolution path that does not create a Player
- [x] #2 The current '+ Нов играч' action is no longer reachable for TEAM-type reviews unless it is the correct action
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Added a real TEAM-type resolution path instead of the dangerous '+ Нов играч' shortcut: AmbiguityReview gained a resolved_team_id column; AmbiguityInboxService.resolveTeamReview() lets an admin pick an existing Team from a dropdown, which repoints the TeamAlias via a new TeamResolutionService.resolveAlias() and marks the review resolved without ever touching the players table. The inbox UI now branches on review type -- TEAM-type rows show a team picker (plus a link to create a missing team via the existing /teams/new form) instead of the player-candidate buttons, so '+ Нов играч' is no longer reachable for TEAM reviews at all. Added defense-in-depth type guards in resolveReview/confirmNewPlayer/resolveTeamReview so a PLAYER-only or TEAM-only action can never resolve the wrong review type even if the UI is mis-wired again. Verified with new tests in AmbiguityInboxServiceTest, InboxResourceTest, and existing TeamResolutionServiceTest coverage of resolveAlias(); full suite passes.
<!-- SECTION:FINAL_SUMMARY:END -->
