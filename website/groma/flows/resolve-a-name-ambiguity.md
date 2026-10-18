---
type: Groma Flow
title: Resolve a name ambiguity
groma:
  id: resolve-a-name-ambiguity
---

An administrator settles a name the app could not match with confidence (UC-011). The decision becomes an alias, so the same name resolves automatically next time, and recorded occurrences become real appearances in the matches already saved.

## Steps

| From | To | Action |
| --- | --- | --- |
| [Administrator](../actors/administrator.md) | [Ambiguity inbox pages](../systems/lineup-tracker/containers/web-application/components/ambiguity-inbox-pages.md) | Pick a candidate, confirm a new player, or pick a team |
| [Ambiguity inbox pages](../systems/lineup-tracker/containers/web-application/components/ambiguity-inbox-pages.md) | [Ambiguity inbox service](../systems/lineup-tracker/containers/web-application/components/ambiguity-inbox-service.md) | Apply the decision |
| [Ambiguity inbox service](../systems/lineup-tracker/containers/web-application/components/ambiguity-inbox-service.md) | [Ambiguity review model](../systems/lineup-tracker/containers/web-application/components/ambiguity-review-model.md) | Mark the review resolved |
| [Ambiguity inbox service](../systems/lineup-tracker/containers/web-application/components/ambiguity-inbox-service.md) | [Player name matching](../systems/lineup-tracker/containers/web-application/components/player-name-matching.md) | Remember the player alias |
| [Ambiguity inbox service](../systems/lineup-tracker/containers/web-application/components/ambiguity-inbox-service.md) | [Team name resolution](../systems/lineup-tracker/containers/web-application/components/team-name-resolution.md) | Remember the team alias |
| [Ambiguity inbox service](../systems/lineup-tracker/containers/web-application/components/ambiguity-inbox-service.md) | [Match record model](../systems/lineup-tracker/containers/web-application/components/match-record-model.md) | Turn occurrences into appearances |
