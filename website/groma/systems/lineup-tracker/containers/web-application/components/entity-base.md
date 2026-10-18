---
type: C4 Component
title: Entity base
status: stable
groma:
  id: entity-base
  parent: web-application
  code:
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/model/TrackerEntity.java
      symbol: TrackerEntity
  group: Persistence model
description: Common id, version and timestamps for every entity
---

Base class that every entity extends: identity, optimistic-locking version, and created and last-updated timestamps, matching the four base columns of every table.
