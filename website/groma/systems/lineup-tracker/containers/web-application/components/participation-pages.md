---
type: C4 Component
title: Participation pages
status: stable
groma:
  id: participation-pages
  parent: web-application
  code:
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/resource/ParticipationResource.java
  group: League data pages
description: List and maintain which formation plays in which competition and season
---

Public list and admin forms under /participations (UC-006). Matches hang off participations, so a team needs one for the season before its matches can be recorded.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [src/main/java/com/nosoftskills/lineup/resource/ParticipationResource.java](../../../../../../src/main/java/com/nosoftskills/lineup/resource/ParticipationResource.java) | [src/main/java/com/nosoftskills/lineup/model/Participation.java](../../../../../../src/main/java/com/nosoftskills/lineup/model/Participation.java) | Maintains participations | Hibernate ORM Panache |
