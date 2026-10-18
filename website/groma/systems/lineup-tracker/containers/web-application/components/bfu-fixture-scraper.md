---
type: C4 Component
title: BFU fixture scraper
status: stable
groma:
  id: bfu-fixture-scraper
  parent: web-application
  code:
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/scraping/BfuFixtureScraperService.java
      symbol: BfuFixtureScraperService
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/scraping/BfuFixture.java
      symbol: BfuFixture
  group: BFU scraping
description: Finds the matches played on a date in a competition
---

First step of lineup extraction (UC-009, UC-010). Reads a competition's bfu-tournaments.com results page, which renders every round of the season, parses the Bulgarian date headings, and returns the fixtures for the requested date with their team names and match-page links.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [src/main/java/com/nosoftskills/lineup/scraping/BfuFixtureScraperService.java](../../../../../../src/main/java/com/nosoftskills/lineup/scraping/BfuFixtureScraperService.java) | [src/main/java/com/nosoftskills/lineup/scraping/BfuHttp.java](../../../../../../src/main/java/com/nosoftskills/lineup/scraping/BfuHttp.java) | Fetches results pages | Java call |
