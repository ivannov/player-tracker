---
id: LT-008
title: On-demand match extraction wizard
status: To Do
assignee: []
created_date: '2026-07-11 15:44'
labels:
  - scraping
  - htmx
  - backend
  - frontend
dependencies:
  - LT-005
  - LT-006
  - LT-007
priority: medium
ordinal: 18000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Admin picks a competition (or 'all competitions') plus a date; the system finds the matches played/scheduled that day, pulls full detail primarily via the bfu-tournaments.com scraper (LT-007), falling back to the ebfu.net scraper (LT-006, lineups only) when the primary source has nothing for a given match. Every scraped player name is resolved through PlayerMatchingService (LT-005) -- confidently-resolved rows are shown for a final admin confirmation, ambiguous rows are queued to ambiguity_reviews instead of blocking the save.

Note: finding 'the matches for a competition on a given day' needs a source of match fixtures/results, which is likely part of the LT-007 scraper's output (a competition+date listing page) rather than a separate concern -- resolve this when the scraper's actual page structure is known.

UI shape should mirror the existing three-step BFU import wizard (LT-001: extract -> resolve -> review/confirm, see participation/ParticipationImportResource.java and templates/ParticipationImportResource/step{1,2,3}.html) applied to matches instead of teams.

This is a stub epic -- break it down into subtasks (fixture discovery, extraction+resolution backend, wizard UI) when picked up.
<!-- SECTION:DESCRIPTION:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 Tests are added for new functoinality and mvn verify is successfull
<!-- DOD:END -->
