---
type: C4 Component
title: Player embedding sync job
status: stable
groma:
  id: player-embedding-sync-job
  parent: web-application
  code:
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/matching/PlayerEmbeddingSyncJob.java
      symbol: PlayerEmbeddingSyncJob
  group: Name resolution
description: Fills in missing player name embeddings every minute
---

Scheduled job (UC-013) that picks up to 25 players without a name embedding, asks the embedding client for each vector and writes it back. Database work runs in short transactions of its own so no connection is held during Ollama calls; failures simply retry on the next run. Disabled in tests.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [src/main/java/com/nosoftskills/lineup/matching/PlayerEmbeddingSyncJob.java](../../../../../../src/main/java/com/nosoftskills/lineup/matching/PlayerEmbeddingSyncJob.java) | [src/main/java/com/nosoftskills/lineup/matching/OllamaEmbeddingClient.java](../../../../../../src/main/java/com/nosoftskills/lineup/matching/OllamaEmbeddingClient.java) | Requests name embeddings | Quarkus Scheduler, CDI call |
