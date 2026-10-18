---
type: C4 Component
title: Home and sign-in pages
status: stable
groma:
  id: home-and-sign-in-pages
  parent: web-application
  code:
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/resource/LandingResource.java
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/resource/LoginResource.java
    - scanner: java
      file: src/main/java/com/nosoftskills/lineup/resource/AppResource.java
  group: Site shell and security
description: Public landing page, login page and signed-in home
---

The landing page at / with team, competition and participation counts, the login form used by Quarkus form authentication (UC-003), and the signed-in home at /app with player search and logout.
