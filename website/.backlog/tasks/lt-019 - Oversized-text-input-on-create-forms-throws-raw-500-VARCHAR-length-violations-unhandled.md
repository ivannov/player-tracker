---
id: LT-019
title: >-
  Oversized text input on create forms throws raw 500 (VARCHAR length violations
  unhandled)
status: In Progress
assignee: []
created_date: '2026-08-01 04:29'
updated_date: '2026-08-03 18:17'
labels:
  - bug
  - data-integrity
dependencies: []
priority: medium
ordinal: 42000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
MT-13.6 manual test execution (LT-011.02) confirmed this is a systemic pattern, not isolated to one resource: submitting a 2000-character 'name' on POST /teams (Team.name is VARCHAR(255)) throws a raw Hibernate DataException ('value too long for type character varying') as an unhandled 500, identical in shape to the season-length finding already tracked in LT-016 (Participation.season, VARCHAR(9)) and the delete-path FK violations in LT-015. None of TeamResource, CompetitionResource, ParticipationResource, or PlayerResource validate field length before calling .persist()/.create() -- they all rely entirely on the DB VARCHAR column limit to reject oversized input, which surfaces as a raw stack trace to the client instead of a friendly validation message.\n\nRecommend a single shared fix rather than four separate patches: either bean-validation annotations (@Size) on the entity/form fields with a global ConstraintViolationException mapper, or a shared helper the resource layer calls before persist. Whatever approach is chosen should also close the DataException gap noted in LT-016.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Oversized input on Team/Competition/Participation/Player create-or-edit forms is rejected with a friendly message, not a raw 500
- [ ] #2 Fix is applied consistently (shared validation mechanism), not patched per-resource
<!-- AC:END -->
