---
type: Groma Flow
title: Nightly lineup extraction
groma:
  id: nightly-lineup-extraction
---

Every day at 23:00 the app runs the same extraction as the wizard, unattended, for each competition with an extraction config and at least one participation (UC-010). Ambiguous names are queued for the inbox; a failing competition is logged and the others continue.

## Steps

| From | To | Action |
| --- | --- | --- |
| [Daily extraction job](../systems/lineup-tracker/containers/web-application/components/daily-extraction-job.md) | [League structure model](../systems/lineup-tracker/containers/web-application/components/league-structure-model.md) | Find configured competitions with participations |
| [Daily extraction job](../systems/lineup-tracker/containers/web-application/components/daily-extraction-job.md) | [Lineup extraction service](../systems/lineup-tracker/containers/web-application/components/lineup-extraction-service.md) | Confirm extraction for today |
| [Lineup extraction service](../systems/lineup-tracker/containers/web-application/components/lineup-extraction-service.md) | [BFU fixture scraper](../systems/lineup-tracker/containers/web-application/components/bfu-fixture-scraper.md) | Find today's fixtures |
| [Lineup extraction service](../systems/lineup-tracker/containers/web-application/components/lineup-extraction-service.md) | [BFU match scraper](../systems/lineup-tracker/containers/web-application/components/bfu-match-scraper.md) | Scrape each match |
| [Lineup extraction service](../systems/lineup-tracker/containers/web-application/components/lineup-extraction-service.md) | [Player name matching](../systems/lineup-tracker/containers/web-application/components/player-name-matching.md) | Resolve players or queue reviews |
| [Lineup extraction service](../systems/lineup-tracker/containers/web-application/components/lineup-extraction-service.md) | [Match record model](../systems/lineup-tracker/containers/web-application/components/match-record-model.md) | Save resolvable matches |
