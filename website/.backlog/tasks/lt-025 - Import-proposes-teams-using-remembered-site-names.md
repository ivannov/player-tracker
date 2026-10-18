---
id: LT-025
title: Import proposes teams using remembered site names
status: To Do
assignee: []
created_date: '2026-10-08 19:39'
labels:
  - aiup
  - UC-007
dependencies: []
documentation:
  - docs/use_cases/UC-007-import-participations-from-league-page.md
priority: low
ordinal: 48000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Covers UC-007 (step 3, BR-005). The first team proposal on import only matches team names exactly, ignoring case. It does not use the names remembered for the BFU Tournaments Site from earlier imports or extractions (UC-009 BR-002, UC-009 BR-007), so a team the site writes differently must be picked by hand on every import, even though its name is already known. Spec change: update UC-007 with /use-case-spec UC-007 and get approval, then implement and remove the Note.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 UC-007 step 3: a listed name already remembered for the BFU Tournaments Site proposes the team it is remembered for
- [ ] #2 UC-007 step 3: a listed name with no remembered mapping still proposes the team with exactly the same name, ignoring case
- [ ] #3 UC-007 BR-004: the review and the save interpret rows proposed from remembered names in the same way
- [ ] #4 UC-007: the > Note about exact-only matching is removed from the spec
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 Tests are added for new functoinality and mvn verify is successfull
<!-- DOD:END -->
