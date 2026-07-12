---
id: LT-003
title: 'Match score & events data model (goals, cards, substitutions)'
status: Done
assignee: []
created_date: '2026-07-11 15:38'
updated_date: '2026-07-12 21:50'
labels:
  - backend
  - model
dependencies: []
priority: high
ordinal: 7000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Match (src/main/java/com/nosoftskills/lineup/model/Match.java) has no score. PlayerAppearance (.../model/PlayerAppearance.java) only has starter/number -- there is no way to represent a goal, a card, or a substitution anywhere in the model. This is pure foundation for the manual entry screens (see the Player & Match manual management epic) and later for the match-day scrapers -- it intentionally has no UI of its own in this task.

Data model, folded into src/main/resources/db/migration/V1__create_teams.sql (post-review: kept to a single pre-production migration file per CLAUDE.md's "keep appending to V1 until first production deployment" convention, rather than the originally-planned separate V2__match_events.sql):
- matches: add home_score, away_score (nullable SMALLINT -- unplayed/unknown matches have no score yet)
- player_appearances: add substituted_in_minute, substituted_out_minute (nullable SMALLINT; both null means played the whole match or was an unused reserve, which the existing starter flag already distinguishes), plus CHECK constraints on minute range (0-130) and in/out ordering
- new table match_events: standard id/version/created_at/last_updated columns, player_appearance_id BIGINT NOT NULL REFERENCES player_appearances(id) ON DELETE CASCADE, type VARCHAR(20) NOT NULL, minute SMALLINT (nullable -- not always scraped, CHECK 0-130), indexed on player_appearance_id

Card semantics: a SECOND_YELLOW_CARD event implies the player was sent off -- do not also write a RED_CARD row for that dismissal. A RED_CARD row on its own means a direct red.

Why events hang off player_appearance_id and not match_id+player_id directly: PlayerAppearance already uniquely ties one player to one side of one match, so MatchEvent doesn't need to duplicate match/player identity.

Post-review correction: the claim that team-side is "implied by the appearance it references" does not hold with the model as built -- PlayerAppearance has no side/team field and Player has no relationship to Team/TeamFormation, so which side a goal counts toward cannot actually be derived today. Tracked as a follow-up: LT-010.

New files: model/MatchEvent.java, model/MatchEventType.java (enum, sits alongside FormationType.java). Modified: model/Match.java, model/PlayerAppearance.java.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Migration V2 adds nullable home_score SMALLINT and away_score SMALLINT to matches
- [x] #2 Migration V2 adds nullable substituted_in_minute SMALLINT and substituted_out_minute SMALLINT to player_appearances
- [x] #3 Migration V2 creates match_events table: id/version/created_at/last_updated, player_appearance_id BIGINT NOT NULL REFERENCES player_appearances(id), type VARCHAR(20) NOT NULL, minute SMALLINT (nullable)
- [x] #4 New MatchEvent entity (extends TrackerEntity) and MatchEventType enum with values GOAL, PENALTY_GOAL, OWN_GOAL, YELLOW_CARD, SECOND_YELLOW_CARD, RED_CARD, following the @JoinColumn-for-camelCase-FK convention
- [x] #5 Match.java and PlayerAppearance.java gain the new fields; mvn verify passes with Hibernate schema validation green against the new migration
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [x] #1 Tests are added for new functoinality and mvn verify is successfull
<!-- DOD:END -->
