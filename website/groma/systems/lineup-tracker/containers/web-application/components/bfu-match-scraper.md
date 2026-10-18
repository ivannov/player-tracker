---
type: C4 Component
title: BFU match scraper
status: stable
groma:
  id: bfu-match-scraper
  parent: web-application
  code:
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/scraping/BfuMatchScraperService.java
      symbol: BfuMatchScraperService
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/scraping/BfuMatchData.java
      symbol: BfuMatchData
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/scraping/BfuTeamLineup.java
      symbol: BfuTeamLineup
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/scraping/BfuLineupEntry.java
      symbol: BfuLineupEntry
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/scraping/BfuMatchEvent.java
      symbol: BfuMatchEvent
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/scraping/BfuMatchEventType.java
      symbol: BfuMatchEventType
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/scraping/BfuSubstitution.java
      symbol: BfuSubstitution
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/scraping/BfuMatchDataMapper.java
      symbol: BfuMatchDataMapper
  group: BFU scraping
description: Reads one match page into lineups, score, events and substitutions
---

Primary source for match detail. Parses a bfu-tournaments.com match page into home and away starters and reserves, the final score, goals and cards with minutes, and substitutions, then maps that site-specific shape onto the source-independent scraped-match contract.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [src/main/java/com/nosoftskills/lineup/scraping/BfuMatchScraperService.java](../../../../../../src/main/java/com/nosoftskills/lineup/scraping/BfuMatchScraperService.java) | [src/main/java/com/nosoftskills/lineup/scraping/BfuHttp.java](../../../../../../src/main/java/com/nosoftskills/lineup/scraping/BfuHttp.java) | Fetches match pages | Java call |
| [src/main/java/com/nosoftskills/lineup/scraping/BfuMatchDataMapper.java](../../../../../../src/main/java/com/nosoftskills/lineup/scraping/BfuMatchDataMapper.java) | [src/main/java/com/nosoftskills/lineup/scraping/ScrapedMatch.java](../../../../../../src/main/java/com/nosoftskills/lineup/scraping/ScrapedMatch.java) | Maps into contract | Java records |
