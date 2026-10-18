---
id: LT-027
title: Signed-in users can change their own password
status: To Do
assignee: []
created_date: '2026-10-08 19:43'
labels:
  - aiup
  - UC-003
dependencies: []
documentation:
  - docs/use_cases/UC-003-sign-in-and-out.md
priority: medium
ordinal: 50000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Covers UC-003 (BR-004: the application offers no screen to change a password). Every signed-in user (USER and ADMIN) should be able to change their own password. New behaviour: update UC-003 with /use-case-spec UC-003 (new flow or use case, plus /use-case-diagram if a new use case) and get approval, then implement. Passwords stay in non-reversible form (UC-003 BR-003).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 UC-003: a signed-in user changes their own password by entering the current password and the new one
- [ ] #2 UC-003: a wrong current password is refused and the password stays unchanged
- [ ] #3 UC-003 BR-003: the new password is stored only in non-reversible form; the next sign-in works with the new password and not the old one
- [ ] #4 UC-003 BR-004: the rule is updated so it no longer says a password cannot be changed
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 Tests are added for new functoinality and mvn verify is successfull
<!-- DOD:END -->
