---
type: C4 Component
title: Name embedding client
status: stable
groma:
  id: name-embedding-client
  parent: web-application
  code:
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/matching/OllamaEmbeddingClient.java
      symbol: OllamaEmbeddingClient
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/matching/OllamaRestApi.java
      symbol: OllamaRestApi
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/matching/OllamaEmbeddingRequest.java
      symbol: OllamaEmbeddingRequest
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/matching/OllamaEmbeddingResponse.java
      symbol: OllamaEmbeddingResponse
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/matching/PgVector.java
      symbol: PgVector
  group: Name resolution
description: Gets name embeddings from Ollama and formats them for pgvector
---

Calls the Ollama embeddings API through a Quarkus REST client with short timeouts. It never throws: an unreachable service or unusable answer yields an empty result, so callers fall back to trigram-only matching. Also formats vectors as pgvector literals for native SQL, because the embedding column has no ORM mapping.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [src/main/java/com/nosoftskills/lineup/matching/OllamaRestApi.java](../../../../../../src/main/java/com/nosoftskills/lineup/matching/OllamaRestApi.java) | [ollama-embedding-service](../../../../../externals/ollama-embedding-service.md) | Requests embeddings | HTTP JSON, Quarkus REST Client |
