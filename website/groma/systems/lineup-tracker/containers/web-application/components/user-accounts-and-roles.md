---
type: C4 Component
title: User accounts and roles
status: stable
groma:
  id: user-accounts-and-roles
  parent: web-application
  code:
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/security/CurrentUser.java
      symbol: CurrentUser
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/model/User.java
      symbol: User
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/model/Role.java
      symbol: Role
  group: Site shell and security
description: Stored accounts and roles, and the current user's identity
---

The User and Role entities that Quarkus Security JPA reads for form login, with ADMIN and USER seeded by migration. The request-scoped current-user helper is a named bean, so the shared navigation reads the username and the administrator role from it directly, and pages use it to decide whether to show admin-only controls; access itself is enforced by role annotations on each page.
