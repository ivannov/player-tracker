---
id: LT-030
title: Make the Extract today and Scheduler menu links work
status: To Do
assignee: []
created_date: '2026-10-08 19:43'
labels:
  - aiup
  - UC-009
  - UC-010
dependencies: []
documentation:
  - docs/use_cases/UC-009-extract-lineups-for-a-date.md
  - docs/use_cases/UC-010-extract-lineups-daily.md
priority: low
ordinal: 53000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Covers UC-009 and UC-010. The extraction menu in templates/tags/appNav.html has two dead links (href="#"): "Извлечи за днес" and "Планировчик". "Extract today" should start the UC-009 extraction for today. "Scheduler" should show the state of the daily extraction (UC-010): which competitions take part, and when it last ran and with what result. Neither is in the specs today. Spec change: update UC-009 and UC-010 (and UC-005 if the scheduler page edits the daily-extraction settings) with /use-case-spec and get approval, then implement.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 UC-009: "Extract today" starts the extraction with today's date already filled in
- [ ] #2 UC-010: "Scheduler" shows the competitions taking part in the daily extraction and the outcome of the last run
- [ ] #3 UC-009, UC-010: no extraction menu link points to an empty address any more
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 Tests are added for new functoinality and mvn verify is successfull
<!-- DOD:END -->
