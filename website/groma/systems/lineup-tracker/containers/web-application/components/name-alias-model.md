---
type: C4 Component
title: Name alias model
status: stable
groma:
  id: name-alias-model
  parent: web-application
  code:
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/model/TeamAlias.java
      symbol: TeamAlias
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/model/PlayerAlias.java
      symbol: PlayerAlias
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/model/ExternalRefSource.java
      symbol: ExternalRefSource
  group: Persistence model
description: Remembered mappings from scraped names to teams and players
---

Team and player aliases keyed by source site and raw name. They are learned during imports, auto-resolution and inbox decisions, and are what lets later extractions run hands-free.
