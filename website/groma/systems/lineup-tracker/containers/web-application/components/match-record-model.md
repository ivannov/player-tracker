---
type: C4 Component
title: Match record model
status: stable
groma:
  id: match-record-model
  parent: web-application
  code:
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/model/Match.java
      symbol: Match
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/model/PlayerAppearance.java
      symbol: PlayerAppearance
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/model/MatchEvent.java
      symbol: MatchEvent
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/model/MatchEventType.java
      symbol: MatchEventType
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/model/Player.java
      symbol: Player
  group: Persistence model
description: Players, matches, appearances and match events
---

Persistent records of what happened: canonical players, matches between two participations with a date and score, player appearances as starter or reserve with substitution minutes, and goal and card events on an appearance.
