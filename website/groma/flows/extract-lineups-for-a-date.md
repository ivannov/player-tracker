---
type: Groma Flow
title: Extract lineups for a date
groma:
  id: extract-lineups-for-a-date
---

An administrator extracts one competition's matches for a date (UC-009). Fixtures come from the bfu-tournaments.com results page; each match's detail comes from its match page, with ebfu.net as a lineup-only fallback. Team and player names are resolved; uncertain names are queued for the inbox rather than guessed. Confirming saves the fully resolvable matches.

## Steps

| From | To | Action |
| --- | --- | --- |
| [Administrator](../actors/administrator.md) | [Lineup extraction wizard](../systems/lineup-tracker/containers/web-application/components/lineup-extraction-wizard.md) | Choose competition and date, review, confirm |
| [Lineup extraction wizard](../systems/lineup-tracker/containers/web-application/components/lineup-extraction-wizard.md) | [Lineup extraction service](../systems/lineup-tracker/containers/web-application/components/lineup-extraction-service.md) | Preview, then confirm the extraction |
| [Lineup extraction service](../systems/lineup-tracker/containers/web-application/components/lineup-extraction-service.md) | [BFU fixture scraper](../systems/lineup-tracker/containers/web-application/components/bfu-fixture-scraper.md) | Find the fixtures played that date |
| [BFU fixture scraper](../systems/lineup-tracker/containers/web-application/components/bfu-fixture-scraper.md) | [BFU page fetcher](../systems/lineup-tracker/containers/web-application/components/bfu-page-fetcher.md) | Fetch the results page |
| [BFU page fetcher](../systems/lineup-tracker/containers/web-application/components/bfu-page-fetcher.md) | [BFU tournaments site](../externals/bfu-tournaments-site.md) | Download HTML |
| [Lineup extraction service](../systems/lineup-tracker/containers/web-application/components/lineup-extraction-service.md) | [BFU match scraper](../systems/lineup-tracker/containers/web-application/components/bfu-match-scraper.md) | Scrape each match's lineups, score and events |
| [Lineup extraction service](../systems/lineup-tracker/containers/web-application/components/lineup-extraction-service.md) | [EBFU lineup scraper](../systems/lineup-tracker/containers/web-application/components/ebfu-lineup-scraper.md) | Fall back for matches the primary site lacks |
| [EBFU lineup scraper](../systems/lineup-tracker/containers/web-application/components/ebfu-lineup-scraper.md) | [EBFU site](../externals/ebfu-site.md) | Read lineups by date and teams |
| [Lineup extraction service](../systems/lineup-tracker/containers/web-application/components/lineup-extraction-service.md) | [Team name resolution](../systems/lineup-tracker/containers/web-application/components/team-name-resolution.md) | Resolve both teams |
| [Lineup extraction service](../systems/lineup-tracker/containers/web-application/components/lineup-extraction-service.md) | [Player name matching](../systems/lineup-tracker/containers/web-application/components/player-name-matching.md) | Resolve each player or queue a review |
| [Player name matching](../systems/lineup-tracker/containers/web-application/components/player-name-matching.md) | [Name embedding client](../systems/lineup-tracker/containers/web-application/components/name-embedding-client.md) | Embed the scraped name when available |
| [Lineup extraction service](../systems/lineup-tracker/containers/web-application/components/lineup-extraction-service.md) | [Match record model](../systems/lineup-tracker/containers/web-application/components/match-record-model.md) | Save matches, appearances and events |
| [Lineup extraction service](../systems/lineup-tracker/containers/web-application/components/lineup-extraction-service.md) | [Ambiguity review model](../systems/lineup-tracker/containers/web-application/components/ambiguity-review-model.md) | Record where unresolved names appeared |
