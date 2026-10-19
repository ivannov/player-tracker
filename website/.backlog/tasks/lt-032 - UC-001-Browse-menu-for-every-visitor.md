---
id: LT-032
title: 'UC-001: Browse menu for every visitor'
status: Done
assignee:
  - '@claude'
created_date: '2026-10-19 08:06'
updated_date: '2026-10-19 15:28'
labels: []
dependencies: []
references:
  - user-accounts-and-roles
  - home-and-sign-in-pages
  - ambiguity-inbox-pages
  - player-pages
  - team-pages
  - competition-pages
  - participation-pages
  - match-pages
  - lineup-extraction-wizard
  - participation-import-wizard
documentation:
  - docs/use_cases/UC-001-browse-league-data.md
  - docs/use_cases/UC-003-sign-in-and-out.md
modified_files:
  - docs/first-season.md
  - docs/manual-test-plan.md
  - docs/use_cases/UC-001-browse-league-data.md
  - docs/use_cases/UC-003-sign-in-and-out.md
  - >-
    groma/systems/lineup-tracker/containers/web-application/components/ambiguity-inbox-pages.md
  - >-
    groma/systems/lineup-tracker/containers/web-application/components/user-accounts-and-roles.md
  - >-
    src/main/java/com/nosoftskills/lineup/extraction/MatchExtractionResource.java
  - src/main/java/com/nosoftskills/lineup/inbox/InboxResource.java
  - >-
    src/main/java/com/nosoftskills/lineup/participation/ParticipationImportResource.java
  - src/main/java/com/nosoftskills/lineup/resource/AppResource.java
  - src/main/java/com/nosoftskills/lineup/resource/CompetitionResource.java
  - src/main/java/com/nosoftskills/lineup/resource/LandingResource.java
  - src/main/java/com/nosoftskills/lineup/resource/MatchResource.java
  - src/main/java/com/nosoftskills/lineup/resource/ParticipationResource.java
  - src/main/java/com/nosoftskills/lineup/resource/PlayerResource.java
  - src/main/java/com/nosoftskills/lineup/resource/TeamResource.java
  - src/main/java/com/nosoftskills/lineup/security/CurrentUser.java
  - src/main/resources/templates/AppResource/index.html
  - src/main/resources/templates/CompetitionResource/form.html
  - src/main/resources/templates/CompetitionResource/list.html
  - src/main/resources/templates/InboxResource/page.html
  - src/main/resources/templates/LandingResource/landing.html
  - src/main/resources/templates/MatchExtractionResource/step1.html
  - src/main/resources/templates/MatchResource/detail.html
  - src/main/resources/templates/MatchResource/form.html
  - src/main/resources/templates/MatchResource/list.html
  - src/main/resources/templates/ParticipationImportResource/step1.html
  - src/main/resources/templates/ParticipationResource/form.html
  - src/main/resources/templates/ParticipationResource/list.html
  - src/main/resources/templates/PlayerResource/detail.html
  - src/main/resources/templates/PlayerResource/form.html
  - src/main/resources/templates/PlayerResource/list.html
  - src/main/resources/templates/TeamResource/form.html
  - src/main/resources/templates/TeamResource/list.html
  - src/main/resources/templates/tags/appNav.html
  - src/test/java/com/nosoftskills/lineup/inbox/InboxResourceTest.java
  - src/test/java/com/nosoftskills/lineup/resource/LandingResourceTest.java
priority: medium
ordinal: 55000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The match list and other browse pages were only reachable through the data-entry dropdown, which visitors who are not signed in never see. Every actor now gets a browse menu (teams, competitions, participations, players, matches); the data-entry menu is removed; extraction and inbox are admin-only; the home page also counts and links matches. Affects UC-001 (main steps 2/3/5, new BR-004) and UC-003 step 4.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 UC-001 Main step 2: the home page shows the number of matches with a link to the match list
- [x] #2 UC-001 BR-004: every visitor, signed in or not, sees a browse menu with teams, competitions, participations, players and matches
- [x] #3 UC-001 BR-004: no data-entry menu is shown to anyone
- [x] #4 UC-001 BR-004: the extraction menu and the inbox are shown only to Administrators
- [x] #5 UC-003 Main step 4: a signed-in User sees the browse menu and the account menu, but no extraction menu or inbox
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [x] #1 Tests are added for new functoinality and mvn verify is successfull
<!-- DOD:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. CurrentUser @Named so appNav can read the role via inject:currentUser.isAdmin (no change to the 17 page templates).
2. appNav: Преглед browse dropdown + burger section for everyone; remove Въвеждане; Извличане, inbox badge and inbox link only for Administrators.
3. LandingResource + landing.html: matches count card linking to /matches.
4. Tests in LandingResourceTest for UC-001 step 2 / BR-004 and UC-003 step 4.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
./mvnw verify: 262/262 green. Role read in appNav through @Named CurrentUser (inject:currentUser.isAdmin), so the 17 page templates stay unchanged. /inbox/badge stays @Authenticated; only its comment changed.

Code review follow-up: /inbox/badge is now @RolesAllowed("ADMIN") (UC-001 BR-004); the test is renamed to badgeForbiddenForNonAdmin and expects 403. appNav reads the username via inject:currentUser.username, so the username parameter is gone from all 17 page templates and their resources; the account menu and the sign-in link share one {#if}/{#else}. Spec: BR-004 narrowed to exclude the sign-in and error pages; 'players' removed from the UC-001 trigger (the player list belongs to UC-002). Docs: first-season.md and manual-test-plan.md (MT-2.6, MT-11.7, MT-12.1) updated for the new nav. Groma overviews of ambiguity-inbox-pages and user-accounts-and-roles updated. ./mvnw verify: 262/262 green; spec validate and lint clean.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Spec change. UC-001: trigger, main steps 2/3/5 and new BR-004 (navigation by role, except on the sign-in and error pages); UC-003 main step 4. appNav has a Преглед browse menu for every actor, no Въвеждане menu, and Извличане plus the inbox for Administrators only; it reads the username and role from the named CurrentUser bean. The inbox badge endpoint is admin-only; the home page adds a matches card. Verified by LandingResourceTest (anonymous, USER, ADMIN), InboxResourceTest (badge 403 for USER) and the full ./mvnw verify (262/262).
<!-- SECTION:FINAL_SUMMARY:END -->
