---
id: LT-016
title: >-
  Duplicate participation (team-formation+competition+season) throws raw 500
  instead of friendly error
status: To Do
assignee: []
created_date: '2026-08-01 04:16'
labels:
  - bug
  - data-integrity
dependencies: []
priority: medium
ordinal: 39000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
MT-5.2 manual test execution (LT-011.02) confirmed the known risk: ParticipationResource.create() (around ParticipationResource.java:124, p.persist()) has no pre-check against the existing unique constraint participations_team_formation_id_competition_id_season_key before persisting. Submitting the /participations/new form twice for the identical team+formation+competition+season combination throws org.hibernate.exception.ConstraintViolationException, which propagates as a raw Quarkus 500 dev error page with a full stack trace (DB error text, query executor internals, etc.) shown directly to the admin instead of a friendly 'this participation already exists' message. Add a pre-check (or catch the constraint violation and re-render the form with a Bulgarian error message, consistent with how MatchResource.create() already handles its own validation failure).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Submitting a duplicate team-formation+competition+season participation shows a friendly error, not a raw 500
- [ ] #2 No stack trace or internal exception detail is exposed to the client
<!-- AC:END -->
