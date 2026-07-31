---
id: LT-012
title: Failed login never shows error banner (bare ?error query param binds to null)
status: To Do
assignee: []
created_date: '2026-07-31 19:30'
labels:
  - bug
  - auth
dependencies: []
priority: high
ordinal: 35000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
MT-2.2/MT-2.3 manual test execution (LT-011.02) found that a real failed login never shows the 'Грешно потребителско име или парола' message. quarkus.http.auth.form.error-page=/login?error (application.properties:31) redirects to a bare, valueless ?error query param on auth failure. LoginResource.login(@QueryParam("error") String error) binds this bare param to null (confirmed: curl /login?error and /login?error= both render identically to plain /login with no banner; only /login?error=1 or /login?error=anything render the banner). Since error != null is false for the bare-param case, the {#if error} block in login.html never renders on the actual real-world failure path -- users get silently bounced back to a blank login form with zero feedback on every wrong password attempt.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 error-page redirect (bare ?error) renders the Bulgarian error banner
- [ ] #2 manually verified via browser: submitting a wrong password on /login shows the error message
<!-- AC:END -->
