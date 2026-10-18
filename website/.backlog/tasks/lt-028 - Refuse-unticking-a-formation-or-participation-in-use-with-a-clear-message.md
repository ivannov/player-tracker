---
id: LT-028
title: Refuse unticking a formation or participation in use with a clear message
status: To Do
assignee: []
created_date: '2026-10-08 19:43'
labels:
  - aiup
  - UC-004
  - UC-006
dependencies: []
documentation:
  - docs/use_cases/UC-004-manage-teams.md
  - docs/use_cases/UC-006-manage-participations.md
priority: medium
ordinal: 51000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Covers UC-004 (step 8, A3) and UC-006 (A3). Unticking a formation on the team form, or a formation on the participation edit form, whose participation is still referenced by a match or lineup entry fails today with a generic data-constraint error (UC-004 A3). UC-006 A3 has no alternative flow for it at all. Both should refuse the change with a clear message naming the formation, and keep the data unchanged. Spec change: update UC-004 A3 and add the missing UC-006 flow with /use-case-spec and get approval, then implement.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 UC-004 A3: unticking a formation whose participation is used by a match or lineup entry is refused with a message naming the formation; the team is unchanged
- [ ] #2 UC-006 A3: unticking a formation whose participation is used by a match or lineup entry is refused with a message naming the formation; the participations are unchanged
- [ ] #3 UC-006 BR-004: unticking formations whose participations are not in use still removes them
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 Tests are added for new functoinality and mvn verify is successfull
<!-- DOD:END -->
