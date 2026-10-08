---
id: LT-023
title: Resolving a player item adds the player to the lineups it was left out of
status: Done
assignee: []
created_date: '2026-10-08 19:35'
updated_date: '2026-10-08 19:36'
labels:
  - aiup
  - UC-011
  - UC-009
dependencies: []
documentation:
  - docs/use_cases/UC-011-resolve-name-ambiguities.md
  - docs/use_cases/UC-009-extract-lineups-for-a-date.md
  - docs/entity_model.md
modified_files:
  - src/main/resources/db/migration/V3__ambiguity_occurrences.sql
  - src/main/java/com/nosoftskills/lineup/model/AmbiguityOccurrence.java
  - src/main/java/com/nosoftskills/lineup/model/AmbiguityOccurrenceEvent.java
  - src/main/java/com/nosoftskills/lineup/extraction/MatchExtractionService.java
  - src/main/java/com/nosoftskills/lineup/inbox/AmbiguityInboxService.java
priority: medium
ordinal: 46000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Covers UC-011 (step 4, A1, BR-006) and UC-009 (step 10, BR-010). Unclear player names keep their lineup details (side, starter flag, shirt number, substitution minutes, goals, cards) for each saved match; resolving the inbox item adds the lineup entries. Resolves the UC-011 > Note about no retroactive lineup entry. Adds tables ambiguity_occurrences and ambiguity_occurrence_events (Flyway V3).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 UC-009 BR-010: saving a match keeps side, starter flag, shirt number, substitution minutes, goals and cards for every pending unclear name
- [x] #2 UC-009 BR-010: re-extraction does not duplicate the kept details; the preview keeps none
- [x] #3 UC-011 BR-006: picking a candidate (step 4) adds the player to every match the name was raised in, with the kept details
- [x] #4 UC-011 BR-006: confirming a new player (A1) adds the new player to the lineups the same way
- [x] #5 UC-011 BR-006: an existing lineup entry is kept; only goals and cards not yet stored are added
- [x] #6 UC-011 BR-006: an item with no kept lineup details adds no lineup entry
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Lineup details of pending unclear names kept in new tables ambiguity_occurrences / ambiguity_occurrence_events (separate from player_appearances so no lineup query has to handle a missing player). Items pending before V3 have no kept details and add no entry. Open: UC-011 BR-006 does not say what happens when the chosen player already appears for the other side of the same match; the existing entry is kept.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
MatchExtractionService.confirm keeps lineup details of pending unclear names per saved match (UC-009 BR-010); AmbiguityInboxService.resolveReview/confirmNewPlayer add PlayerAppearance + MatchEvent rows from them (UC-011 BR-006). 7 tests added (MatchExtractionServiceTest, AmbiguityInboxServiceTest); ./mvnw verify: 260 tests, 0 failures.
<!-- SECTION:FINAL_SUMMARY:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [x] #1 Tests are added for new functoinality and mvn verify is successfull
<!-- DOD:END -->
