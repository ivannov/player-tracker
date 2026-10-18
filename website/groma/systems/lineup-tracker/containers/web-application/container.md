---
type: C4 Container
title: Web application
status: draft
groma:
  id: web-application
  parent: lineup-tracker
  technology: Java 21, Quarkus 3, Quarkus REST, Qute, HTMX, Hibernate ORM Panache, Jsoup
description: Quarkus server that renders the UI, scrapes BFU sites and runs scheduled jobs
---

One deployable Quarkus application (built with src/main/docker/Dockerfile.jvm and run as the app service in docker-compose.prod.yml). It serves server-rendered Qute pages enhanced with HTMX and Pico CSS, enforces form login with ADMIN and USER roles, scrapes the BFU sites with the JDK HttpClient and Jsoup, and runs two in-process scheduled jobs: the 23:00 lineup extraction and the once-a-minute player-embedding catch-up. It stores everything in the Lineup database through Hibernate ORM with Panache, plus native SQL for trigram and vector similarity.
