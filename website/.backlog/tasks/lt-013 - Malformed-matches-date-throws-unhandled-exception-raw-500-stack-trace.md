---
id: LT-013
title: Malformed /matches?date= throws unhandled exception (raw 500 stack trace)
status: Done
assignee: []
created_date: '2026-07-31 19:30'
updated_date: '2026-08-03 18:06'
labels:
  - bug
  - matches
dependencies: []
priority: medium
ordinal: 36000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
MT-1.5 manual test execution (LT-011.02) confirmed the known risk: MatchResource.list(...) at MatchResource.java:90 does 'LocalDate parsedDate = (date == null || date.isBlank()) ? null : LocalDate.parse(date);' with no try/catch. Visiting /matches?date=not-a-date or /matches?date=2026-13-40 as any user (including anonymous) throws DateTimeParseException/DateTimeException which is unhandled and results in a raw Quarkus 'Internal Server Error' page with a full stack trace exposed to the client (verified both anonymous and pre-auth). Contrast with MatchExtractionResource's discover endpoint, which already catches this same failure mode and returns a friendly 400 'Невалидна дата: ...' message -- the same guard should be applied here. Empty date (?date=) is already handled fine via isBlank().
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Invalid date values on /matches (e.g. not-a-date, 2026-13-40) return a friendly error/empty state instead of a raw 500
- [ ] #2 No stack trace or internal exception detail is exposed to the client
- [ ] #3 1,2
<!-- AC:END -->
