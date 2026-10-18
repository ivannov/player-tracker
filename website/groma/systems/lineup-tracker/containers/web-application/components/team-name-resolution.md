---
type: C4 Component
title: Team name resolution
status: stable
groma:
  id: team-name-resolution
  parent: web-application
  code:
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/matching/TeamResolutionService.java
      symbol: TeamResolutionService
  group: Name resolution
description: Maps a scraped team name to a canonical team through aliases
---

Shared by the participation import and lineup extraction. Preloads the team aliases for a batch of raw names, records new aliases, and never repoints an existing alias to a different team; such a conflict is queued as a TEAM ambiguity review instead.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [src/main/java/com/nosoftskills/lineup/matching/TeamResolutionService.java](../../../../../../src/main/java/com/nosoftskills/lineup/matching/TeamResolutionService.java) | [src/main/java/com/nosoftskills/lineup/model/TeamAlias.java](../../../../../../src/main/java/com/nosoftskills/lineup/model/TeamAlias.java) | Reads and writes aliases | Hibernate ORM Panache |
| [src/main/java/com/nosoftskills/lineup/matching/TeamResolutionService.java](../../../../../../src/main/java/com/nosoftskills/lineup/matching/TeamResolutionService.java) | [src/main/java/com/nosoftskills/lineup/model/AmbiguityReview.java](../../../../../../src/main/java/com/nosoftskills/lineup/model/AmbiguityReview.java) | Queues team reviews | Hibernate ORM Panache |
