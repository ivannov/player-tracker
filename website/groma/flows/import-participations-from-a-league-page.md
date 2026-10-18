---
type: Groma Flow
title: Import participations from a league page
groma:
  id: import-participations-from-a-league-page
---

An administrator fills a competition's season from its BFU league page (UC-007): scraped team names are matched to existing teams or proposed as new ones, then reviewed and saved as participations.

## Steps

| From | To | Action |
| --- | --- | --- |
| [Administrator](../actors/administrator.md) | [Participation import wizard](../systems/lineup-tracker/containers/web-application/components/participation-import-wizard.md) | Enter league URL, review rows, save |
| [Participation import wizard](../systems/lineup-tracker/containers/web-application/components/participation-import-wizard.md) | [BFU league scraper](../systems/lineup-tracker/containers/web-application/components/bfu-league-scraper.md) | Read the team names |
| [BFU league scraper](../systems/lineup-tracker/containers/web-application/components/bfu-league-scraper.md) | [BFU page fetcher](../systems/lineup-tracker/containers/web-application/components/bfu-page-fetcher.md) | Fetch the league page |
| [BFU page fetcher](../systems/lineup-tracker/containers/web-application/components/bfu-page-fetcher.md) | [BFU tournaments site](../externals/bfu-tournaments-site.md) | Download HTML |
| [Participation import wizard](../systems/lineup-tracker/containers/web-application/components/participation-import-wizard.md) | [Team name resolution](../systems/lineup-tracker/containers/web-application/components/team-name-resolution.md) | Match names to teams and record aliases |
| [Participation import wizard](../systems/lineup-tracker/containers/web-application/components/participation-import-wizard.md) | [League structure model](../systems/lineup-tracker/containers/web-application/components/league-structure-model.md) | Save teams, formations and participations |
