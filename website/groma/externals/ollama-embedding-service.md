---
type: C4 System
title: Ollama embedding service
status: stable
groma:
  id: ollama-embedding-service
  technology: Ollama HTTP API, nomic-embed-text model
---

A self-hosted Ollama instance (run alongside the app in Docker Compose) that turns player names into 768-dimension embedding vectors. It is an optional enhancement: when it is down or the model is not pulled, name matching falls back to trigram similarity only.
