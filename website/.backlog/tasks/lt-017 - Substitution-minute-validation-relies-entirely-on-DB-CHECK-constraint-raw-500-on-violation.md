---
id: LT-017
title: >-
  Minute validation relies entirely on DB CHECK constraints (substitutions +
  events) -- raw 500 on violation
status: In Progress
assignee: []
created_date: '2026-08-01 04:20'
updated_date: '2026-08-03 18:11'
labels:
  - bug
  - matches
dependencies: []
priority: medium
ordinal: 40000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
MT-7.6 manual test execution (LT-011.02) confirmed: MatchResource.updateSubstitution() (MatchResource.java:239-246) writes substitutedInMinute/substitutedOutMinute directly from form input with zero application-level validation, relying entirely on the DB CHECK constraints (0-130 range, out > in) to reject bad data. Verified via browser: submitting an appearance's substitution with in=90/out=60 (out < in) throws org.hibernate... via io.quarkus.arc.ArcUndeclaredThrowableException, and the browser navigates fully to the raw Quarkus 500 dev error page (this endpoint returns Response.seeOther on success, so failures aren't HTMX-wrapped either -- same full-page-navigation-to-blank-error problem as MT-7.4/LT-014, but here it's a genuine unhandled 500 with a stack trace, not just an empty 400 body). Add range/ordering validation before persisting (matching the CHECK constraints: 0<=minute<=130, out>in) and return a friendly Bulgarian error instead.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Invalid substitution minutes (out<in, >130, negative) return a friendly error, not a raw 500
- [ ] #2 No stack trace exposed to the client
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Also confirmed during MT-7.8: MatchResource.addEvent() (MatchResource.java:263, event.minute = parseShort(f.minute)) has the identical gap -- no range check before persist. POST .../events with minute=999 throws the same raw 500 via the match_events minute CHECK constraint. Same fix should cover both addEvent() and updateSubstitution().
<!-- SECTION:NOTES:END -->
