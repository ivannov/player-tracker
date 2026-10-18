---
type: C4 Component
title: Daily extraction job
status: stable
groma:
  id: daily-extraction-job
  parent: web-application
  code:
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/extraction/ScheduledExtractionJob.java
      symbol: ScheduledExtractionJob
  group: Lineup extraction
description: Runs lineup extraction for every configured competition at 23:00
---

Scheduled job (UC-010) that, every day at 23:00, confirms extraction for today's date for each competition that has an extraction config and at least one participation. A failure for one competition is logged and the rest still run; overlapping runs are skipped.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [src/main/java/com/nosoftskills/lineup/extraction/ScheduledExtractionJob.java](../../../../../../src/main/java/com/nosoftskills/lineup/extraction/ScheduledExtractionJob.java) | [src/main/java/com/nosoftskills/lineup/extraction/MatchExtractionService.java](../../../../../../src/main/java/com/nosoftskills/lineup/extraction/MatchExtractionService.java) | Confirms daily extraction | Quarkus Scheduler, CDI call |
| [src/main/java/com/nosoftskills/lineup/extraction/ScheduledExtractionJob.java](../../../../../../src/main/java/com/nosoftskills/lineup/extraction/ScheduledExtractionJob.java) | [src/main/java/com/nosoftskills/lineup/model/CompetitionExtractionConfig.java](../../../../../../src/main/java/com/nosoftskills/lineup/model/CompetitionExtractionConfig.java) | Reads extraction configs | Hibernate ORM Panache |
