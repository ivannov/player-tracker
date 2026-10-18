---
type: C4 Component
title: Lineup extraction service
status: stable
groma:
  id: lineup-extraction-service
  parent: web-application
  code:
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/extraction/MatchExtractionService.java
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/extraction/MatchExtractionRow.java
      symbol: MatchExtractionRow
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/extraction/PlayerRowResolution.java
      symbol: PlayerRowResolution
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/extraction/TeamSideResolution.java
      symbol: TeamSideResolution
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/extraction/MatchExtractionSummary.java
      symbol: MatchExtractionSummary
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/extraction/ExtractionRequest.java
      symbol: ExtractionRequest
  group: Lineup extraction
description: Extracts, resolves and saves a competition's matches for a date
---

The core of UC-009 and UC-010. For a competition, results page, season and date it discovers the fixtures, scrapes each match from bfu-tournaments.com with ebfu.net as fallback, resolves both teams and their participations, and resolves every player name. A preview returns the rows without saving matches; confirm saves matches, appearances, events and substitutions for fully resolvable rows, reuses matches that already exist, and links pending reviews to the match. It never blocks on ambiguity.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [src/main/java/com/nosoftskills/lineup/extraction/MatchExtractionService.java](../../../../../../src/main/java/com/nosoftskills/lineup/extraction/MatchExtractionService.java) | [src/main/java/com/nosoftskills/lineup/scraping/BfuFixtureScraperService.java](../../../../../../src/main/java/com/nosoftskills/lineup/scraping/BfuFixtureScraperService.java) | Discovers fixtures | CDI call |
| [src/main/java/com/nosoftskills/lineup/extraction/MatchExtractionService.java](../../../../../../src/main/java/com/nosoftskills/lineup/extraction/MatchExtractionService.java) | [src/main/java/com/nosoftskills/lineup/scraping/BfuMatchScraperService.java](../../../../../../src/main/java/com/nosoftskills/lineup/scraping/BfuMatchScraperService.java) | Scrapes match detail | CDI call |
| [src/main/java/com/nosoftskills/lineup/extraction/MatchExtractionService.java](../../../../../../src/main/java/com/nosoftskills/lineup/extraction/MatchExtractionService.java) | [src/main/java/com/nosoftskills/lineup/scraping/EbfuMatchLineupScraperService.java](../../../../../../src/main/java/com/nosoftskills/lineup/scraping/EbfuMatchLineupScraperService.java) | Scrapes fallback lineups | CDI call |
| [src/main/java/com/nosoftskills/lineup/extraction/MatchExtractionService.java](../../../../../../src/main/java/com/nosoftskills/lineup/extraction/MatchExtractionService.java) | [src/main/java/com/nosoftskills/lineup/matching/TeamResolutionService.java](../../../../../../src/main/java/com/nosoftskills/lineup/matching/TeamResolutionService.java) | Resolves team names | CDI call |
| [src/main/java/com/nosoftskills/lineup/extraction/MatchExtractionService.java](../../../../../../src/main/java/com/nosoftskills/lineup/extraction/MatchExtractionService.java) | [src/main/java/com/nosoftskills/lineup/matching/PlayerMatchingService.java](../../../../../../src/main/java/com/nosoftskills/lineup/matching/PlayerMatchingService.java) | Resolves player names | CDI call |
| [src/main/java/com/nosoftskills/lineup/extraction/MatchExtractionService.java](../../../../../../src/main/java/com/nosoftskills/lineup/extraction/MatchExtractionService.java) | [src/main/java/com/nosoftskills/lineup/scraping/ScrapedMatch.java](../../../../../../src/main/java/com/nosoftskills/lineup/scraping/ScrapedMatch.java) | Consumes scraped matches | Java records |
| [src/main/java/com/nosoftskills/lineup/extraction/MatchExtractionService.java](../../../../../../src/main/java/com/nosoftskills/lineup/extraction/MatchExtractionService.java) | [src/main/java/com/nosoftskills/lineup/model/Match.java](../../../../../../src/main/java/com/nosoftskills/lineup/model/Match.java) | Saves matches and appearances | Hibernate ORM Panache |
| [src/main/java/com/nosoftskills/lineup/extraction/MatchExtractionService.java](../../../../../../src/main/java/com/nosoftskills/lineup/extraction/MatchExtractionService.java) | [src/main/java/com/nosoftskills/lineup/model/AmbiguityOccurrence.java](../../../../../../src/main/java/com/nosoftskills/lineup/model/AmbiguityOccurrence.java) | Records unresolved occurrences | Hibernate ORM Panache |
| [src/main/java/com/nosoftskills/lineup/extraction/MatchExtractionService.java](../../../../../../src/main/java/com/nosoftskills/lineup/extraction/MatchExtractionService.java) | [src/main/java/com/nosoftskills/lineup/model/Participation.java](../../../../../../src/main/java/com/nosoftskills/lineup/model/Participation.java) | Finds participations | Hibernate ORM Panache |
