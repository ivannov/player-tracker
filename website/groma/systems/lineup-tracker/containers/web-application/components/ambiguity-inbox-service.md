---
type: C4 Component
title: Ambiguity inbox service
status: stable
groma:
  id: ambiguity-inbox-service
  parent: web-application
  code:
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/inbox/AmbiguityInboxService.java
  group: Ambiguity inbox
description: Lists pending name reviews and applies an admin's decision
---

Backs UC-011. Lists pending player and team reviews with their ranked candidates and latest clubs, and resolves them: pick an existing player, confirm a new player, or pick a team. Resolving a player review turns each recorded occurrence into a real appearance with its events in the matches already saved. Each decision writes the alias through player or team name resolution, so later extractions match automatically, and records who decided.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [src/main/java/com/nosoftskills/lineup/inbox/AmbiguityInboxService.java](../../../../../../src/main/java/com/nosoftskills/lineup/inbox/AmbiguityInboxService.java) | [src/main/java/com/nosoftskills/lineup/matching/PlayerMatchingService.java](../../../../../../src/main/java/com/nosoftskills/lineup/matching/PlayerMatchingService.java) | Writes player aliases | CDI call |
| [src/main/java/com/nosoftskills/lineup/inbox/AmbiguityInboxService.java](../../../../../../src/main/java/com/nosoftskills/lineup/inbox/AmbiguityInboxService.java) | [src/main/java/com/nosoftskills/lineup/matching/TeamResolutionService.java](../../../../../../src/main/java/com/nosoftskills/lineup/matching/TeamResolutionService.java) | Writes team aliases | CDI call |
| [src/main/java/com/nosoftskills/lineup/inbox/AmbiguityInboxService.java](../../../../../../src/main/java/com/nosoftskills/lineup/inbox/AmbiguityInboxService.java) | [src/main/java/com/nosoftskills/lineup/model/AmbiguityReview.java](../../../../../../src/main/java/com/nosoftskills/lineup/model/AmbiguityReview.java) | Resolves reviews | Hibernate ORM Panache |
| [src/main/java/com/nosoftskills/lineup/inbox/AmbiguityInboxService.java](../../../../../../src/main/java/com/nosoftskills/lineup/inbox/AmbiguityInboxService.java) | [src/main/java/com/nosoftskills/lineup/model/PlayerAppearance.java](../../../../../../src/main/java/com/nosoftskills/lineup/model/PlayerAppearance.java) | Completes appearances | Hibernate ORM Panache |
| [src/main/java/com/nosoftskills/lineup/inbox/AmbiguityInboxService.java](../../../../../../src/main/java/com/nosoftskills/lineup/inbox/AmbiguityInboxService.java) | [src/main/java/com/nosoftskills/lineup/security/CurrentUser.java](../../../../../../src/main/java/com/nosoftskills/lineup/security/CurrentUser.java) | Records the decider | CDI call |
