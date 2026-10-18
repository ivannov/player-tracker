---
type: C4 Component
title: Database error page
status: stable
groma:
  id: database-error-page
  parent: web-application
  code:
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/resource/JdbcExceptionMapper.java
  group: Site shell and security
description: Turns rejected database writes into a friendly error page
---

Application-wide fallback for JDBC exceptions, so a constraint violation that a page did not handle shows an error page instead of a stack trace.
