---
id: LT-009
title: Daily scheduled extraction job + admin ambiguity inbox
status: Done
assignee: []
created_date: '2026-07-11 15:44'
updated_date: '2026-07-30 09:41'
labels:
  - scheduler
  - htmx
  - backend
  - frontend
dependencies:
  - LT-008
priority: medium
ordinal: 19000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Quarkus @Scheduled job (quarkus-scheduler is already a pom dependency but unused -- CLAUDE.md documents a 23:00 daily job that doesn't exist in code yet) running the extraction logic from LT-008 unattended, at 23:00, for every competition that has at least one Participation, for 'today's' matches. It must never block on ambiguity -- unresolved player names are queued into ambiguity_reviews (LT-005) exactly like the on-demand wizard does.

New admin-only inbox screen (e.g. /inbox), linked from templates/tags/appNav.html, surfaced prominently right after an admin logs in -- lists pending ambiguity_reviews with their ranked candidate players (from ambiguity_candidates) so the admin can pick the correct one or confirm a brand-new player. Resolving an item writes back into player_aliases (LT-005) so the same ambiguity never recurs.

This is a stub epic -- break it down into subtasks (scheduled job, inbox backend, inbox UI) when picked up.
<!-- SECTION:DESCRIPTION:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 Tests are added for new functoinality and mvn verify is successfull
<!-- DOD:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
All three subtasks complete: LT-009.01 (scheduled daily extraction job), LT-009.02 (ambiguity inbox backend), LT-009.03 (ambiguity inbox UI). Epic done.
<!-- SECTION:NOTES:END -->
