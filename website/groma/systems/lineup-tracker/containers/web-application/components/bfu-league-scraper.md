---
type: C4 Component
title: BFU league scraper
status: stable
groma:
  id: bfu-league-scraper
  parent: web-application
  code:
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/scraping/BfuLeagueScraperService.java
      symbol: BfuLeagueScraperService
  group: BFU scraping
description: Lists the team names on a BFU league page
---

Used by the participation import wizard (UC-007). Reads a league page and returns the distinct team names shown next to team logos, failing loudly when none are found so a changed page layout is noticed.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [src/main/java/com/nosoftskills/lineup/scraping/BfuLeagueScraperService.java](../../../../../../src/main/java/com/nosoftskills/lineup/scraping/BfuLeagueScraperService.java) | [src/main/java/com/nosoftskills/lineup/scraping/BfuHttp.java](../../../../../../src/main/java/com/nosoftskills/lineup/scraping/BfuHttp.java) | Fetches league pages | Java call |
