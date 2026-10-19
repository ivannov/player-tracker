---
type: C4 Component
title: Ambiguity inbox pages
status: stable
groma:
  id: ambiguity-inbox-pages
  parent: web-application
  code:
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/inbox/InboxResource.java
  group: Ambiguity inbox
description: Admin inbox page and the pending-count badge
---

Web front of the inbox at /inbox. Renders pending reviews and HTMX actions to resolve each one, plus a badge fragment that the shared navigation loads for administrators only to show the pending count; the inbox page, the badge and every action are restricted to administrators.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [src/main/java/com/nosoftskills/lineup/inbox/InboxResource.java](../../../../../../src/main/java/com/nosoftskills/lineup/inbox/InboxResource.java) | [src/main/java/com/nosoftskills/lineup/inbox/AmbiguityInboxService.java](../../../../../../src/main/java/com/nosoftskills/lineup/inbox/AmbiguityInboxService.java) | Lists and resolves reviews | CDI call |
