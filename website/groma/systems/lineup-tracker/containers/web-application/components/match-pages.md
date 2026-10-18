---
type: C4 Component
title: Match pages
status: stable
groma:
  id: match-pages
  parent: web-application
  code:
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/resource/MatchResource.java
  group: League data pages
description: Browse matches and record lineups and events by hand
---

Public match list and detail pages and the admin forms under /matches (UC-001, UC-008): create a match, add and remove appearances, set substitution minutes, and add or remove goals and cards.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [src/main/java/com/nosoftskills/lineup/resource/MatchResource.java](../../../../../../src/main/java/com/nosoftskills/lineup/resource/MatchResource.java) | [src/main/java/com/nosoftskills/lineup/model/Match.java](../../../../../../src/main/java/com/nosoftskills/lineup/model/Match.java) | Records matches | Hibernate ORM Panache |
