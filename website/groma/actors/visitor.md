---
type: C4 Actor
title: Visitor
status: stable
groma:
  id: visitor
---

Anyone browsing without signing in. Reads every public list and detail page — teams, competitions, participations, matches and players — and searches a player's history (UC-001, UC-002).

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [visitor](visitor.md) | [src/main/java/com/nosoftskills/lineup/resource/LandingResource.java](../../src/main/java/com/nosoftskills/lineup/resource/LandingResource.java) | Opens landing page | HTTPS, browser |
| [visitor](visitor.md) | [src/main/java/com/nosoftskills/lineup/resource/PlayerResource.java](../../src/main/java/com/nosoftskills/lineup/resource/PlayerResource.java) | Searches player history | HTTPS, browser |
| [visitor](visitor.md) | [src/main/java/com/nosoftskills/lineup/resource/MatchResource.java](../../src/main/java/com/nosoftskills/lineup/resource/MatchResource.java) | Browses matches | HTTPS, browser |
| [visitor](visitor.md) | [src/main/java/com/nosoftskills/lineup/resource/TeamResource.java](../../src/main/java/com/nosoftskills/lineup/resource/TeamResource.java) | Browses teams | HTTPS, browser |
