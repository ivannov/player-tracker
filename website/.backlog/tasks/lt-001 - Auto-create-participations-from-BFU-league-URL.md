---
id: LT-001
title: Auto-create participations from BFU league URL
status: To Do
assignee: []
created_date: '2026-05-03 19:41'
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
- [ ] #1 Tests are added for new functoinality and mvn verify is successfull
<!-- DOD:END -->
