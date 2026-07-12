---
id: LT-001
title: Auto-create participations from BFU league URL
status: Done
assignee: []
created_date: '2026-05-03 19:41'
updated_date: '2026-07-12 08:03'
labels:
  - feature
  - participation
  - scraping
  - htmx
dependencies: []
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Replace the tedious manual participation workflow with an import wizard. The admin pastes a BFU league URL (e.g. `https://bfu-tournaments.com/leagues/first?season=2024-2025`), picks the competition and season, and the system scrapes team names from the page. It then walks the admin through resolving each scraped team to a DB team (with a searchable picker for unmatched names) and choosing the correct TeamFormation. After reviewing a summary the admin confirms and all Participations (and any missing Teams/TeamFormations) are saved in one shot.

This replaces the current flow of: manually creating teams → manually creating team formations → manually linking each to the competition one-by-one.

Three subtasks cover (1) the BFU scraper, (2) the wizard backend, and (3) the wizard UI.
<!-- SECTION:DESCRIPTION:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [x] #1 Tests are added for new functoinality and mvn verify is successfull
<!-- DOD:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Delivered across three subtasks: LT-001.01 (BFU scraper), LT-001.02 (wizard backend endpoints), LT-001.03 (wizard UI: step1 URL/competition form, step2 team-resolution with searchable datalist picker + new-team/new-formation affordances, step3 create/exists/skip review, confirm-and-save).
<!-- SECTION:FINAL_SUMMARY:END -->
