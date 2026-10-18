---
type: C4 Component
title: Ambiguity review model
status: stable
groma:
  id: ambiguity-review-model
  parent: web-application
  code:
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/model/AmbiguityReview.java
      symbol: AmbiguityReview
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/model/AmbiguityCandidate.java
      symbol: AmbiguityCandidate
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/model/AmbiguityOccurrence.java
      symbol: AmbiguityOccurrence
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/model/AmbiguityOccurrenceEvent.java
      symbol: AmbiguityOccurrenceEvent
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/model/AmbiguityReviewStatus.java
      symbol: AmbiguityReviewStatus
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/model/AmbiguityReviewType.java
      symbol: AmbiguityReviewType
  group: Persistence model
description: Pending name decisions, their candidates and occurrences
---

Player and team ambiguity reviews with status and decision, the ranked candidate players, and the occurrences that record where an unresolved name appeared in a match, including its events, so the match data can be completed once the name is resolved.
