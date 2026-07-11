---
id: LT-002
title: Public read / admin-only write security model
status: Done
assignee: []
created_date: '2026-07-11 15:37'
updated_date: '2026-07-11 19:43'
labels:
  - security
  - access
dependencies: []
priority: high
ordinal: 4000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Teams, Competitions and Participations resources (TeamResource, CompetitionResource, ParticipationResource under src/main/java/com/nosoftskills/lineup/resource/) all carry class-level @Authenticated, so even GET requests require login. This contradicts the product requirement: a regular user (no login) must be able to search/browse players, teams and competitions; only an administrator (logged in, ADMIN role) can create/edit/delete.

Fix: remove class-level @Authenticated from these resources. Keep @RolesAllowed("ADMIN") only on POST/DELETE and on the /new and /{id}/edit form-rendering endpoints (editing is a write concern). GET list/detail actions become unauthenticated. templates/tags/appNav.html already takes a username param -- make it accept a null username and hide admin-only links (edit buttons, 'Import from BFU') when anonymous. ParticipationImportResource (the BFU import wizard) stays fully admin-only since it's inherently a write flow.

This epic sets the security pattern that every future resource (Players, Matches -- see the epic for Player & Match manual management) must follow from the start.
<!-- SECTION:DESCRIPTION:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 Tests are added for new functoinality and mvn verify is successfull
<!-- DOD:END -->
