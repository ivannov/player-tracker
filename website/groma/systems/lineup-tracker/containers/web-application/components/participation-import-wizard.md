---
type: C4 Component
title: Participation import wizard
status: stable
groma:
  id: participation-import-wizard
  parent: web-application
  code:
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/participation/ParticipationImportResource.java
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/participation/ImportAction.java
      symbol: ImportAction
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/participation/ImportReviewRow.java
      symbol: ImportReviewRow
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/participation/TeamResolutionRow.java
      symbol: TeamResolutionRow
  group: League data pages
description: Admin wizard that creates participations from a BFU league page
---

Multi-step admin flow under /participations/import (UC-007). Scrapes the team names from a league page, resolves each to an existing team through team name resolution or proposes a new one, lets the administrator choose formations and review every row, then saves teams, formations, participations and aliases.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [src/main/java/com/nosoftskills/lineup/participation/ParticipationImportResource.java](../../../../../../src/main/java/com/nosoftskills/lineup/participation/ParticipationImportResource.java) | [src/main/java/com/nosoftskills/lineup/scraping/BfuLeagueScraperService.java](../../../../../../src/main/java/com/nosoftskills/lineup/scraping/BfuLeagueScraperService.java) | Scrapes team names | CDI call |
| [src/main/java/com/nosoftskills/lineup/participation/ParticipationImportResource.java](../../../../../../src/main/java/com/nosoftskills/lineup/participation/ParticipationImportResource.java) | [src/main/java/com/nosoftskills/lineup/matching/TeamResolutionService.java](../../../../../../src/main/java/com/nosoftskills/lineup/matching/TeamResolutionService.java) | Resolves team names | CDI call |
| [src/main/java/com/nosoftskills/lineup/participation/ParticipationImportResource.java](../../../../../../src/main/java/com/nosoftskills/lineup/participation/ParticipationImportResource.java) | [src/main/java/com/nosoftskills/lineup/model/Participation.java](../../../../../../src/main/java/com/nosoftskills/lineup/model/Participation.java) | Saves participations | Hibernate ORM Panache |
