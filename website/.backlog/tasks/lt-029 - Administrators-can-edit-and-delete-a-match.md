---
id: LT-029
title: Administrators can edit and delete a match
status: To Do
assignee: []
created_date: '2026-10-08 19:43'
labels:
  - aiup
  - UC-008
dependencies: []
documentation:
  - docs/use_cases/UC-008-record-match-manually.md
priority: medium
ordinal: 52000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Covers UC-008. Once a match is created, its date, sides and score cannot be changed, and the match cannot be deleted (UC-008 > Note). Administrators need both to correct wrong extractions or manual entries. Spec change: update UC-008 with /use-case-spec UC-008 and get approval, then implement and remove the Note. Decide in the spec what happens to the match's lineup entries, events and linked inbox items (UC-011, UC-009 BR-010) when it is deleted.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 UC-008: an Administrator edits a match's date, sides and score; the sides stay two different participations of the same competition and season
- [ ] #2 UC-008: an Administrator deletes a match after confirming; its lineup entries and events are removed with it
- [ ] #3 UC-008: deleting a match handles the inbox items linked to it and their kept lineup details as the spec decides
- [ ] #4 UC-008: visitors and signed-in users without the Administrator role see no edit or delete controls
- [ ] #5 UC-008: the > Note about matches being impossible to edit or delete is removed from the spec
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 Tests are added for new functoinality and mvn verify is successfull
<!-- DOD:END -->
