---
type: C4 Component
title: EBFU lineup scraper
status: stable
groma:
  id: ebfu-lineup-scraper
  parent: web-application
  code:
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/scraping/EbfuMatchLineupScraperService.java
      symbol: EbfuMatchLineupScraperService
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/scraping/EbfuMatchLineup.java
      symbol: EbfuMatchLineup
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/scraping/EbfuTeamLineup.java
      symbol: EbfuTeamLineup
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/scraping/EbfuLineupEntry.java
      symbol: EbfuLineupEntry
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/scraping/EbfuScraperException.java
      symbol: EbfuScraperException
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/scraping/EbfuMatchLineupMapper.java
      symbol: EbfuMatchLineupMapper
  group: BFU scraping
description: Fallback that reads a match's lineups from ebfu.net
---

Used when bfu-tournaments.com has nothing for a match. Looks up the day's matches on ebfu.net by date and team names, reads both lineups including goalkeeper and captain tags, and maps them onto the scraped-match contract with no score, events or substitutions.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [src/main/java/com/nosoftskills/lineup/scraping/EbfuMatchLineupScraperService.java](../../../../../../src/main/java/com/nosoftskills/lineup/scraping/EbfuMatchLineupScraperService.java) | [ebfu-site](../../../../../externals/ebfu-site.md) | Scrapes lineups | HTTPS, Jsoup |
| [src/main/java/com/nosoftskills/lineup/scraping/EbfuMatchLineupMapper.java](../../../../../../src/main/java/com/nosoftskills/lineup/scraping/EbfuMatchLineupMapper.java) | [src/main/java/com/nosoftskills/lineup/scraping/ScrapedMatch.java](../../../../../../src/main/java/com/nosoftskills/lineup/scraping/ScrapedMatch.java) | Maps into contract | Java records |
