---
id: LT-024
title: Deleting a competition also removes its daily-extraction settings
status: To Do
assignee: []
created_date: '2026-10-08 19:39'
labels:
  - aiup
  - UC-005
dependencies: []
documentation:
  - docs/use_cases/UC-005-manage-competitions.md
priority: medium
ordinal: 47000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Covers UC-005 (BR-004 and the delete flow). Today a competition without participations but with daily-extraction settings cannot be deleted: the database rejects it, so the Administrator must first clear the settings (A1). The UC-005 > Note calls this unintended. Bug fix where the spec is silent: update the spec first with /use-case-spec UC-005 and get approval, then fix the code and remove the Note.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 UC-005 BR-004: a competition without participations can be deleted even when it has daily-extraction settings; the settings are removed with it
- [ ] #2 UC-005 BR-004: a competition with participations still cannot be deleted, and its daily-extraction settings stay unchanged
- [ ] #3 UC-005: the > Note about deletion being rejected because of daily-extraction settings is removed from the spec
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 Tests are added for new functoinality and mvn verify is successfull
<!-- DOD:END -->
