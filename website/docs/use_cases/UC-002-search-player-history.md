# Use Case: Search Player History

## Overview

**Use Case ID:** UC-002  
**Use Case Name:** Search Player History  
**Primary Actor:** Visitor, Registered User, Administrator  
**Goal:** Find a player and see every match the player appeared in, for which team and competition, and what happened to the player in each match.  
**Trigger:** The actor opens the player list.  
**Status:** Implemented  

## Preconditions

- _None — the player pages are public._

## Main Success Scenario

1. Actor opens the player list.
2. System shows all players ordered by name.
3. Actor enters part of a player's name and searches.
4. System shows the players whose name contains the entered text, ordered by name.
5. Actor selects a player.
6. System shows the player's career timeline in chronological order: for each appearance the match date, opponent, team and formation, competition, starter or substitute, shirt number, substitution minutes and the goals and cards of that match.

## Alternative Flows

### A1: No player matches the search

**Trigger:** No player name contains the entered text (step 4)  
**Flow:**

1. System shows an empty list.
2. Use case continues at step 3.

### A2: Player does not exist

**Trigger:** The selected player is not known to the system (step 5)  
**Flow:**

1. System reports that the page was not found.
2. Use case ends.

### A3: Administrator views a player

**Trigger:** The actor is an Administrator (step 6)  
**Flow:**

1. System additionally lists likely duplicate records of the player and offers to edit or merge them (UC-012).
2. Use case ends.

## Postconditions

### Success Postconditions

- The actor has seen the player's history; nothing is changed.

### Failure Postconditions

- No data is changed.

## Business Rules

### BR-001: Name search

The search is case-insensitive and matches any part of the player's name. An empty search shows all players.

### BR-002: Chronological timeline

Appearances are ordered by match date, oldest first; the timeline spans all teams and competitions the player has appeared for.
