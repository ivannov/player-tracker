---
id: LT-005
title: >-
  Team/player disambiguation memory + matching service (pg_trgm + local AI
  embeddings)
status: Done
assignee: []
created_date: '2026-07-11 15:43'
updated_date: '2026-07-22 15:41'
labels:
  - backend
  - matching
  - ai
dependencies:
  - LT-001
  - LT-004
priority: medium
ordinal: 12000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Nothing remembers a resolved name today. The BFU import wizard (LT-001.02, done -- participation/ParticipationImportResource.java) matches scraped team names to Team.name case-insensitively and forgets the resolution, so next season the admin resolves the same team again. There is no equivalent mechanism for players at all, which is a hard prerequisite before any match-scraping automation (see the match-day scraper and extraction-wizard epics) can be trusted to run unattended: player names scraped off a match page must be matched to existing Player records, and ambiguous cases must be queued for an admin rather than guessed.

Decision: use Postgres pg_trgm trigram similarity for fuzzy matching, plus a local AI embedding model (via Ollama) layered on top for semantic matching (transliteration variants, nicknames) that trigram similarity alone would miss -- confirmed with the user as an explicit 'from the start' requirement, not a later add-on.

Split across three subtasks: (1) schema for external refs / aliases / ambiguity reviews, (2) the trigram-based matching service plus retrofitting the import wizard to write TeamExternalRef, (3) the Ollama embedding layer on top. No UI in this epic -- the admin inbox screen that surfaces ambiguity_reviews to a human is the daily-scheduler epic; this epic is the storage-and-matching foundation everything downstream calls into.
<!-- SECTION:DESCRIPTION:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [x] #1 Tests are added for new functoinality and mvn verify is successfull
<!-- DOD:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Completed across three subtasks: LT-005.01 added the disambiguation schema (team_external_refs, player_aliases, ambiguity_reviews/ambiguity_candidates, pg_trgm + vector extensions, players.name_embedding vector(768)); LT-005.02 added PlayerMatchingService (trigram-based, team-scoped) and wired TeamExternalRef capture into the BFU import wizard's save step; LT-005.03 layered local AI embedding re-ranking via Ollama (OllamaEmbeddingClient + PlayerEmbeddingSyncJob) on top, with a hard fallback to trigram-only matching when Ollama is unreachable. No UI in this epic by design -- the admin ambiguity inbox is the daily-scheduler epic (LT-009). mvn verify: 137/137 tests pass across all three subtasks.
<!-- SECTION:FINAL_SUMMARY:END -->
