---
type: C4 Container
title: Lineup database
status: draft
groma:
  id: lineup-database
  parent: lineup-tracker
  technology: PostgreSQL 17, pgvector, pg_trgm, Flyway
description: Stores teams, competitions, matches, players, aliases, reviews and accounts
---

The application's only data store. Flyway migrations in src/main/resources/db/migration create the schema at startup; V1 enables the pgvector extension and later migrations add a trigram index on player names and the ambiguity-occurrence tables. Player names carry a 768-dimension embedding column that is read and written only through native SQL. In production a sidecar backup container dumps it daily. The dev database is rebuilt from migrations on every startup, so it is disposable.
