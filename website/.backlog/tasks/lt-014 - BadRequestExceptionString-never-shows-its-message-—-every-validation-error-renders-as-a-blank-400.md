---
id: LT-014
title: >-
  BadRequestException(String) never shows its message — every validation error
  renders as a blank 400
status: To Do
assignee: []
created_date: '2026-07-31 19:46'
updated_date: '2026-08-01 04:19'
labels:
  - bug
  - error-handling
dependencies: []
priority: high
ordinal: 37000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
MT-8.1 manual test execution (LT-011.02) surfaced this while running the Participation Import wizard against a real BFU URL: submitting step 1 (a genuinely valid league URL/team-list page, confirmed 200 and scrapeable via curl) returned a bare 400 with content-length: 0 and no visible feedback in the UI (HTMX just leaves the form as-is; browser console shows 'Response Status Error Code 400 ... [object HTMLDivElement]', confirmed via read_console_messages). Root cause: jakarta.ws.rs.BadRequestException(String message) does NOT put the message into the HTTP response entity -- that constructor only sets the exception's Java message (for logs/getMessage()) and builds a Response with status 400 and no body. There is no @ServerExceptionMapper/ExceptionMapper anywhere in the codebase to catch BadRequestException and render its message, so every one of these throws across the app produces a blank 400 that the user never sees any explanation for. Confirmed via curl (POST /participations/import/extract with a verified-working URL -> HTTP/1.1 400, content-length: 0, empty body).

All affected call sites (grep for 'throw new BadRequestException' returns exactly these 9):
- AmbiguityInboxService.java:122 (double-resolve race message)
- MatchResource.java:194 (invalid participation for match)
- MatchResource.java:206 (missing player selection)
- MatchResource.java:210 (duplicate player in match)
- MatchResource.java:261 (invalid event type)
- MatchExtractionResource.java:69 and :125 (scraper failure message)
- MatchExtractionResource.java:141 (invalid date at discover)
- ParticipationImportResource.java:80 (scraper failure message)

By contrast, MatchResource.create() (mismatched competition/season) correctly builds a full Response.status(422).entity(Templates.form(...)) with the message embedded in the re-rendered page -- that one call site is unaffected and shows its message correctly. The fix should follow that pattern (or add a shared ExceptionMapper for BadRequestException that renders the message as the response body/HTMX-swappable fragment) for all 9 sites above.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 All BadRequestException(String) call sites return a response whose body actually contains the Bulgarian message
- [ ] #2 Verified in-browser: each of the 9 affected flows shows a visible error message to the admin instead of silently doing nothing
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Also confirmed during MT-7.4: the 'add appearance' form (/matches/{id}/appearances) is a plain (non-HTMX) form POST, so a BadRequestException there doesn't just silently no-op like the HTMX cases -- the browser navigates away from the match page entirely to Chrome's generic blank 'HTTP ERROR 400' page. Same root cause (BadRequestException(String) body is empty), but a worse-looking failure mode for this specific call site since there's no page to swap the message into.
<!-- SECTION:NOTES:END -->
