---
type: C4 Actor
title: Administrator
status: stable
groma:
  id: administrator
---

A signed-in account with the ADMIN role. Maintains teams, competitions, participations, players and matches, runs the participation-import and lineup-extraction wizards, and resolves the names the app could not match in the ambiguity inbox (UC-004 to UC-012).

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [administrator](administrator.md) | [src/main/java/com/nosoftskills/lineup/resource/LoginResource.java](../../src/main/java/com/nosoftskills/lineup/resource/LoginResource.java) | Signs in | HTTPS, form login |
| [administrator](administrator.md) | [src/main/java/com/nosoftskills/lineup/resource/TeamResource.java](../../src/main/java/com/nosoftskills/lineup/resource/TeamResource.java) | Maintains teams | HTTPS, HTMX |
| [administrator](administrator.md) | [src/main/java/com/nosoftskills/lineup/resource/CompetitionResource.java](../../src/main/java/com/nosoftskills/lineup/resource/CompetitionResource.java) | Maintains competitions | HTTPS, HTMX |
| [administrator](administrator.md) | [src/main/java/com/nosoftskills/lineup/resource/ParticipationResource.java](../../src/main/java/com/nosoftskills/lineup/resource/ParticipationResource.java) | Maintains participations | HTTPS, HTMX |
| [administrator](administrator.md) | [src/main/java/com/nosoftskills/lineup/participation/ParticipationImportResource.java](../../src/main/java/com/nosoftskills/lineup/participation/ParticipationImportResource.java) | Imports participations | HTTPS, HTMX |
| [administrator](administrator.md) | [src/main/java/com/nosoftskills/lineup/resource/MatchResource.java](../../src/main/java/com/nosoftskills/lineup/resource/MatchResource.java) | Records matches | HTTPS, HTMX |
| [administrator](administrator.md) | [src/main/java/com/nosoftskills/lineup/resource/PlayerResource.java](../../src/main/java/com/nosoftskills/lineup/resource/PlayerResource.java) | Maintains players | HTTPS, HTMX |
| [administrator](administrator.md) | [src/main/java/com/nosoftskills/lineup/extraction/MatchExtractionResource.java](../../src/main/java/com/nosoftskills/lineup/extraction/MatchExtractionResource.java) | Extracts lineups | HTTPS, HTMX |
| [administrator](administrator.md) | [src/main/java/com/nosoftskills/lineup/inbox/InboxResource.java](../../src/main/java/com/nosoftskills/lineup/inbox/InboxResource.java) | Resolves ambiguities | HTTPS, HTMX |
