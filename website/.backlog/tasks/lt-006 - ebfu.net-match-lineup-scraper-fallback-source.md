---
id: LT-006
title: ebfu.net match-lineup scraper (fallback source)
status: Done
assignee: []
created_date: '2026-07-11 15:43'
updated_date: '2026-07-26 21:10'
labels:
  - scraping
  - backend
dependencies: []
priority: medium
ordinal: 16000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Fallback data source per the product brief: ebfu.net only exposes a match's starting lineup and reserves, no score/scorers/cards/subs. Mirror the shape of the existing scraping/BfuLeagueScraperService.java (Jsoup, a checked BfuScraperException-style exception, fixture-based unit test with a locally captured HTML page, no live network calls in tests) but point it at an ebfu.net match page and return both sides' starting lineup + reserves.

This is a stub epic -- break it down into scraper-only and model-mapping subtasks when picked up, following the same subtask split used for LT-001 (scraper / backend endpoints / UI) and LT-005 (schema / service / AI layer).
<!-- SECTION:DESCRIPTION:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 Tests are added for new functoinality and mvn verify is successfull
<!-- DOD:END -->
