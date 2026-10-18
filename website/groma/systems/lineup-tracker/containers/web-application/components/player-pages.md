---
type: C4 Component
title: Player pages
status: stable
groma:
  id: player-pages
  parent: web-application
  code:
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/resource/PlayerResource.java
  group: League data pages
description: Search players, show a career, and maintain or merge players
---

Public player search and career page under /players (UC-002) and the admin forms (UC-012). The detail page lists every appearance with its events and suggests likely duplicates, typically a record created after a transfer, which an administrator can merge.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [src/main/java/com/nosoftskills/lineup/resource/PlayerResource.java](../../../../../../src/main/java/com/nosoftskills/lineup/resource/PlayerResource.java) | [src/main/java/com/nosoftskills/lineup/matching/PlayerMatchingService.java](../../../../../../src/main/java/com/nosoftskills/lineup/matching/PlayerMatchingService.java) | Looks up latest clubs | CDI call |
| [src/main/java/com/nosoftskills/lineup/resource/PlayerResource.java](../../../../../../src/main/java/com/nosoftskills/lineup/resource/PlayerResource.java) | [src/main/java/com/nosoftskills/lineup/model/Player.java](../../../../../../src/main/java/com/nosoftskills/lineup/model/Player.java) | Searches and merges players | Hibernate ORM Panache, native SQL |
