---
type: C4 Component
title: Competition pages
status: stable
groma:
  id: competition-pages
  parent: web-application
  code:
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/resource/CompetitionResource.java
  group: League data pages
description: List and maintain competitions and their extraction config
---

Public competition list and admin forms under /competitions (UC-005). The form also holds the optional extraction config (results URL and current season) that makes a competition eligible for the daily extraction job.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [src/main/java/com/nosoftskills/lineup/resource/CompetitionResource.java](../../../../../../src/main/java/com/nosoftskills/lineup/resource/CompetitionResource.java) | [src/main/java/com/nosoftskills/lineup/model/Competition.java](../../../../../../src/main/java/com/nosoftskills/lineup/model/Competition.java) | Maintains competitions | Hibernate ORM Panache |
