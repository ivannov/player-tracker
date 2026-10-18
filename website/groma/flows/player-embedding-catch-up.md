---
type: Groma Flow
title: Player embedding catch-up
groma:
  id: player-embedding-catch-up
---

Once a minute the app fills in name embeddings for players that lack one (UC-013), so semantic matching covers new and older players without slowing any page. If Ollama is unavailable the run does nothing and retries.

## Steps

| From | To | Action |
| --- | --- | --- |
| [Player embedding sync job](../systems/lineup-tracker/containers/web-application/components/player-embedding-sync-job.md) | [Name embedding client](../systems/lineup-tracker/containers/web-application/components/name-embedding-client.md) | Request each missing vector |
| [Name embedding client](../systems/lineup-tracker/containers/web-application/components/name-embedding-client.md) | [Ollama embedding service](../externals/ollama-embedding-service.md) | Compute the name embedding |
