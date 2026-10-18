---
type: C4 Component
title: Lineup extraction wizard
status: stable
groma:
  id: lineup-extraction-wizard
  parent: web-application
  code:
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/extraction/MatchExtractionResource.java
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/extraction/ExtractionRowView.java
      symbol: ExtractionRowView
  group: Lineup extraction
description: Admin pages to discover, review and confirm a day's matches
---

Three-step admin flow under /matches/extract (UC-009): choose a competition and date, review what the extraction service found and how names resolved, then confirm to save.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [src/main/java/com/nosoftskills/lineup/extraction/MatchExtractionResource.java](../../../../../../src/main/java/com/nosoftskills/lineup/extraction/MatchExtractionResource.java) | [src/main/java/com/nosoftskills/lineup/extraction/MatchExtractionService.java](../../../../../../src/main/java/com/nosoftskills/lineup/extraction/MatchExtractionService.java) | Previews and confirms extraction | CDI call |
