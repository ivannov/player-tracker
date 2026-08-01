---
id: LT-015
title: >-
  Deleting Team/Competition/Participation with dependents throws raw 500 (FK
  violation exposed)
status: To Do
assignee: []
created_date: '2026-07-31 20:21'
updated_date: '2026-08-01 04:33'
labels:
  - bug
  - data-integrity
dependencies: []
priority: high
ordinal: 38000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
MT-3.3/MT-4.4/MT-5.5 manual test execution (LT-011.02) confirmed the known code-review risk: TeamResource.delete(), CompetitionResource.delete(), and (by the same pattern) ParticipationResource's delete all call t.delete()/c.delete()/p.delete() directly with no check for dependent rows first. Verified via raw DELETE requests as ADMIN: DELETE /teams/{id} for a team with a participation returns HTTP 500 with a plain-text stack trace body (io.quarkus.arc.ArcUndeclaredThrowableException wrapping a Hibernate constraint violation, ~8.9KB of internal exception detail sent to the client). DELETE /competitions/{id} for a competition with participations reproduces the same 500. This leaks internal implementation detail (class names, exception chains) to an authenticated admin client and gives no actionable message ('this team/competition still has N participations, remove them first' or similar). Add a dependent-row guard (or FK-violation-specific exception handling) before delete on all three resources.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Deleting a Team/Competition/Participation with dependent rows returns a friendly error, not a raw 500
- [ ] #2 No stack trace or internal exception detail is exposed to the client
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
MT-5.5 also empirically confirmed (previously only inferred by code pattern): DELETE /participations/{id} for a participation referenced by a Match throws the same raw 500 (FK violation), verified via curl.
<!-- SECTION:NOTES:END -->
