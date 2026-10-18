---
type: C4 Component
title: Team pages
status: stable
groma:
  id: team-pages
  parent: web-application
  code:
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/resource/TeamResource.java
  group: League data pages
description: List, create, edit and delete teams and their formations
---

Public team list and admin forms under /teams (UC-001, UC-004). A team is a club with a city, an optional logo and the formations (youth, men's first, second, third) it fields.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [src/main/java/com/nosoftskills/lineup/resource/TeamResource.java](../../../../../../src/main/java/com/nosoftskills/lineup/resource/TeamResource.java) | [src/main/java/com/nosoftskills/lineup/model/Team.java](../../../../../../src/main/java/com/nosoftskills/lineup/model/Team.java) | Maintains teams | Hibernate ORM Panache |
| [src/main/java/com/nosoftskills/lineup/resource/TeamResource.java](../../../../../../src/main/java/com/nosoftskills/lineup/resource/TeamResource.java) | [src/main/java/com/nosoftskills/lineup/security/CurrentUser.java](../../../../../../src/main/java/com/nosoftskills/lineup/security/CurrentUser.java) | Gates admin controls | CDI call |
