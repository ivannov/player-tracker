---
id: LT-031
title: Administrators can delete a player without appearances
status: To Do
assignee: []
created_date: '2026-10-08 19:43'
labels:
  - aiup
  - UC-012
dependencies: []
documentation:
  - docs/use_cases/UC-012-manage-players.md
priority: low
ordinal: 54000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Covers UC-012. The only way to remove a player today is to merge it into another one (UC-012 > Note), so a player created by mistake (for example a wrong 'new player' in the inbox, UC-011 A1) cannot simply be deleted. Spec change: update UC-012 with /use-case-spec UC-012 and get approval, then implement and remove the Note. Decide in the spec what happens to the player's remembered names and inbox references.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 UC-012: an Administrator deletes a player who has no lineup entries
- [ ] #2 UC-012: deleting a player who has lineup entries is refused with a message; the player is unchanged
- [ ] #3 UC-012: the player's remembered names and inbox references are handled as the spec decides
- [ ] #4 UC-012: the > Note about deletion only through merging is removed from the spec
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 Tests are added for new functoinality and mvn verify is successfull
<!-- DOD:END -->
