# Use Case: Browse League Data

## Overview

**Use Case ID:** UC-001  
**Use Case Name:** Browse League Data  
**Primary Actor:** Visitor, Registered User, Administrator  
**Goal:** See which teams, competitions and participations are tracked and look up the lineups, scores and events of played matches.  
**Trigger:** The actor opens the home page or one of the browse pages (teams, competitions, participations, matches).  
**Status:** Implemented  

## Preconditions

- _None — every browse page is public._

## Main Success Scenario

1. Actor opens the home page.
2. System shows how many teams, competitions and participations are tracked, with links to each list.
3. Actor opens the list of teams, competitions or participations.
4. System shows the requested list; participations are ordered by season (newest first) and team name.
5. Actor opens the list of matches and optionally filters it by competition, by match date, or by both.
6. System shows the matching matches, newest first, with home and away team, formation, competition and score.
7. Actor selects a match.
8. System shows the match with the home and away lineups (starters first, then by shirt number), each player's substitution minutes and the goals and cards recorded for that player.

## Alternative Flows

### A1: Invalid date filter

**Trigger:** The date entered as a filter is not a valid date (step 5)  
**Flow:**

1. System shows an "invalid date" message and ignores the date filter.
2. Use case continues at step 6.

### A2: Match does not exist

**Trigger:** The selected match is not known to the system (step 7)  
**Flow:**

1. System reports that the page was not found.
2. Use case ends.

### A3: Administrator is browsing

**Trigger:** The actor is an Administrator (step 8)  
**Flow:**

1. System additionally shows the controls to add, edit, import and delete data on every list and match page, which lead to UC-004 to UC-009 and UC-012.
2. Use case ends.

## Postconditions

### Success Postconditions

- The actor has seen the requested data; nothing is changed.

### Failure Postconditions

- No data is changed.

## Business Rules

### BR-001: Public read, administrator write

All browse pages are visible without signing in. Controls that change data are shown only to Administrators, and the system refuses such changes from anyone else.

### BR-002: Competition filter uses the home side

A match belongs to the competition of its home participation; both sides of a match always share competition and season (UC-008 BR-001).

### BR-003: Lineup order

Within each side, starters are listed before substitutes, and players within each group are ordered by shirt number. Events of a player are listed by minute.
