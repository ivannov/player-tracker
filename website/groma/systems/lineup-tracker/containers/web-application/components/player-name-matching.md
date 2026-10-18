---
type: C4 Component
title: Player name matching
status: stable
groma:
  id: player-name-matching
  parent: web-application
  code:
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/matching/PlayerMatchingService.java
  group: Name resolution
description: Maps a scraped player name to a canonical player, or queues it for review
---

Resolves a raw name scraped under a team (UC-009, UC-011). It first looks for a remembered player alias, then scores candidates among players who have appeared for that team, using pg_trgm similarity and, when an embedding is available, pgvector cosine similarity. A single confident candidate is auto-resolved and remembered as an alias; anything less certain becomes a pending ambiguity review with ranked candidates, including close matches from other clubs that are never auto-resolved. Also writes aliases for inbox decisions and reports each player's latest club.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [src/main/java/com/nosoftskills/lineup/matching/PlayerMatchingService.java](../../../../../../src/main/java/com/nosoftskills/lineup/matching/PlayerMatchingService.java) | [src/main/java/com/nosoftskills/lineup/matching/OllamaEmbeddingClient.java](../../../../../../src/main/java/com/nosoftskills/lineup/matching/OllamaEmbeddingClient.java) | Embeds scraped names | CDI call |
| [src/main/java/com/nosoftskills/lineup/matching/PlayerMatchingService.java](../../../../../../src/main/java/com/nosoftskills/lineup/matching/PlayerMatchingService.java) | [src/main/java/com/nosoftskills/lineup/model/PlayerAlias.java](../../../../../../src/main/java/com/nosoftskills/lineup/model/PlayerAlias.java) | Reads and writes aliases | Hibernate ORM Panache |
| [src/main/java/com/nosoftskills/lineup/matching/PlayerMatchingService.java](../../../../../../src/main/java/com/nosoftskills/lineup/matching/PlayerMatchingService.java) | [src/main/java/com/nosoftskills/lineup/model/AmbiguityReview.java](../../../../../../src/main/java/com/nosoftskills/lineup/model/AmbiguityReview.java) | Queues player reviews | Hibernate ORM Panache |
