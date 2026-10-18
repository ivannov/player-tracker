---
type: C4 System
title: Lineup Tracker
status: stable
groma:
  id: lineup-tracker
description: Tracks BFU match lineups and player histories
---

Collects who played in which match for BFU competitions and makes that history browsable. Data comes in three ways: an administrator enters it by hand, runs an extraction wizard for a competition and date, or lets the nightly job extract the day's matches unattended. Scraped team and player names are mapped to canonical records through remembered aliases, trigram similarity and optional name embeddings. Anything the app cannot match with confidence waits in an ambiguity inbox for an administrator; the app never guesses. Reading is public; every change requires the ADMIN role.
