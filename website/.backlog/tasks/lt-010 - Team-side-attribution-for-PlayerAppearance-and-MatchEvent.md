---
id: LT-010
title: Team-side attribution for PlayerAppearance and MatchEvent
status: To Do
assignee: []
created_date: '2026-07-12 21:45'
updated_date: '2026-07-19 10:32'
labels:
  - backend
  - model
dependencies:
  - LT-003
priority: medium
ordinal: 20000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
LT-003's description states that MatchEvent doesn't need to record team/side because "that's implied by the [player] appearance it references." That isn't actually true of the model as built: PlayerAppearance (src/main/java/com/nosoftskills/lineup/model/PlayerAppearance.java) only has player, match, starter, number, substituted_in_minute, substituted_out_minute -- no side/team field -- and Player (model/Player.java) has no relationship to Team/TeamFormation anywhere in the schema. A player could in principle appear for either side across different matches (or, at minimum, nothing in the schema rules that out), so there is currently no way to derive from a MatchEvent (or its PlayerAppearance) which of Match.homeTeam / Match.awayTeam a goal counts toward.

Two concrete consequences:
- Match.home_score / away_score (LT-003) can never be computed from or reconciled against the MatchEvent log -- they are an independent, manually-entered source of truth with no cross-check.
- LT-004.03 ("Manual lineup & event entry on match detail page") AC #1 says "Admin can add a PlayerAppearance to a match side" -- that side has nowhere to be persisted today.

Needs a design decision and a migration, most likely one of:
- Add a `participation_id` (or explicit `home`/`away` enum) column to player_appearances, referencing the Participation the player represented in that specific match, so side can be derived by comparing against Match.homeTeam/awayTeam.
- Or add a team-side enum column directly to player_appearances.

Should land before or alongside LT-004.03, since that task's manual-entry UI is the first place "which side" actually needs to be captured.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Decide and document the team-side attribution approach (participation reference vs. explicit side enum) for PlayerAppearance
- [ ] #2 Migration adds the chosen column(s) to player_appearances
- [ ] #3 PlayerAppearance entity gains the corresponding field(s), following existing @JoinColumn/@Column conventions
- [ ] #4 Tests cover deriving/storing team side for both home and away appearances
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Decision: added a participation_id FK column to player_appearances (references participations),
rather than an explicit home/away enum. This directly gives the join path PlayerAppearance ->
Participation -> TeamFormation -> Team/Competition needed by LT-004.01's career timeline, and
side (home/away) is derived by comparing participation.id against Match.homeTeam.id /
Match.awayTeam.id -- no separate enum needed. Covered by
MatchEventTest#playerAppearanceParticipationDerivesHomeOrAwaySide.
<!-- SECTION:NOTES:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 Tests are added for new functoinality and mvn verify is successfull
<!-- DOD:END -->
