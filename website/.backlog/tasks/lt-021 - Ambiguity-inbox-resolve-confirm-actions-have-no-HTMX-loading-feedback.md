---
id: LT-021
title: Ambiguity inbox resolve/confirm actions have no HTMX loading feedback
status: Done
assignee: []
created_date: '2026-08-03 18:59'
updated_date: '2026-08-03 19:08'
labels:
  - bug
  - ux
dependencies: []
priority: low
ordinal: 44000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
MT-12.5 manual QA pass (LT-011.02 follow-up) found: InboxResource/list.html's three resolve forms (hx-post to /inbox/{id}/resolve, /resolve-team, /confirm-new) have no hx-indicator attribute and no [aria-busy]/.htmx-indicator styling anywhere -- confirmed via grep across all templates. Contrast with the Participation Import and Match Extraction wizards, which both wire hx-indicator to their submit button plus a matching '[aria-busy="true"] { opacity: 0.7; pointer-events: none; }' CSS rule (step1.html in both wizards). The wizard behavior was confirmed live: the discover/extract forms use hx-indicator pointing at the submit button itself, consistent with the intentional aria-busy CSS. The Inbox resolve buttons give zero visual feedback during the request and aren't disabled while in flight -- a user could plausibly double-click before the swap completes (server-side idempotency already covers this per LT-011.02/MT-11.4 -- a second resolve attempt on an already-resolved review correctly 400s -- so no data-integrity risk, this is purely a missing-feedback UX gap).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Inbox resolve/resolve-team/confirm-new buttons visibly indicate a pending request (opacity dim or spinner), consistent with the wizard steps' existing hx-indicator+aria-busy pattern
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [x] #1 Tests are added for new functoinality and mvn verify is successfull
<!-- DOD:END -->
