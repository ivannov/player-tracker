---
id: LT-007
title: bfu-tournaments.com match-day scraper (primary source)
status: To Do
assignee: []
created_date: '2026-07-11 15:44'
labels:
  - scraping
  - backend
dependencies:
  - LT-003
priority: medium
ordinal: 17000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Primary data source per the product brief: bfu-tournaments.com carries full match detail. Given a match page URL, extract: final score, both sides' full lineups (starters + reserves + shirt numbers), goal scorers with minute, cards (yellow/second-yellow/red) with minute, and substitutions with minute. Maps onto the MatchEvent/MatchEventType model from LT-003 (Match score & events data model) -- depends on that migration existing.

Follow the Jsoup + checked-exception + fixture-based-test pattern from scraping/BfuLeagueScraperService.java (which only extracts a league's team list today -- this is the richer, match-level counterpart).

This is a stub epic -- break it down into scraper subtask(s) plus a mapping-to-entities subtask when picked up.
<!-- SECTION:DESCRIPTION:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 Tests are added for new functoinality and mvn verify is successfull
<!-- DOD:END -->
