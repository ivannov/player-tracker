---
type: C4 Actor
title: Registered user
status: stable
groma:
  id: registered-user
---

A signed-in account with the USER role. Sees the same data as a visitor, plus the signed-in home page reached after login (UC-003). Accounts are created with SQL; there is no sign-up page.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [registered-user](registered-user.md) | [src/main/java/com/nosoftskills/lineup/resource/LoginResource.java](../../src/main/java/com/nosoftskills/lineup/resource/LoginResource.java) | Signs in | HTTPS, form login |
| [registered-user](registered-user.md) | [src/main/java/com/nosoftskills/lineup/resource/PlayerResource.java](../../src/main/java/com/nosoftskills/lineup/resource/PlayerResource.java) | Searches player history | HTTPS, browser |
