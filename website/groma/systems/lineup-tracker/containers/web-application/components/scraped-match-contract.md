---
type: C4 Component
title: Scraped match contract
status: stable
groma:
  id: scraped-match-contract
  parent: web-application
  code:
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/scraping/ScrapedMatch.java
      symbol: ScrapedMatch
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/scraping/ScrapedTeamLineup.java
      symbol: ScrapedTeamLineup
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/scraping/ScrapedLineupEntry.java
      symbol: ScrapedLineupEntry
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/scraping/ScrapedMatchEvent.java
      symbol: ScrapedMatchEvent
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/scraping/ScrapedMatchEventType.java
      symbol: ScrapedMatchEventType
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/scraping/ScrapedSubstitution.java
      symbol: ScrapedSubstitution
  group: BFU scraping
description: Source-independent shape of a scraped match
---

The records both match scrapers produce and lineup extraction consumes: two team lineups of starters and reserves, an optional score, events and substitutions. It lets extraction treat bfu-tournaments.com and ebfu.net interchangeably.
