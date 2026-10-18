---
type: C4 Component
title: League structure model
status: stable
groma:
  id: league-structure-model
  parent: web-application
  code:
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/model/Team.java
      symbol: Team
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/model/TeamFormation.java
      symbol: TeamFormation
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/model/FormationType.java
      symbol: FormationType
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/model/Competition.java
      symbol: Competition
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/model/CompetitionExtractionConfig.java
      symbol: CompetitionExtractionConfig
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/model/Participation.java
      symbol: Participation
  group: Persistence model
description: Teams, formations, competitions, extraction configs and participations
---

Persistent records for who plays where: clubs, the formations they field, competitions with their optional extraction config, and the participation of a formation in a competition and season.
