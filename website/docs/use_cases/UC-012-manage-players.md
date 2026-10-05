# Use Case: Manage Players

## Overview

**Use Case ID:** UC-012  
**Use Case Name:** Manage Players  
**Primary Actor:** Administrator  
**Goal:** Keep exactly one record per real player, with a correct name, by adding, renaming and merging duplicate players (typically created after a transfer).  
**Trigger:** The Administrator chooses to add a player from the player list or opens a player's page.  
**Status:** Implemented  

## Preconditions

- The Administrator is signed in (UC-003).

## Main Success Scenario

1. Administrator opens a player's page.
2. System shows the player's history (UC-002) and up to ten other players with a similar name as likely duplicates, each with the club of the duplicate's latest appearance.
3. Administrator selects the duplicate that is the same person and chooses to merge it into this player.
4. System checks that the two players never appeared in the same match.
5. System moves all appearances, remembered names and inbox references of the duplicate to this player, deletes the duplicate and shows the player's page again.

## Alternative Flows

### A1: Add or rename a player

**Trigger:** Administrator chooses to add a player or to edit the player's name (step 1)  
**Flow:**

1. Administrator enters the player's names and saves.
2. System creates or updates the player and shows the player list.
3. Use case ends.

### A2: Players share a match

**Trigger:** Both players appeared in the same match (step 4)  
**Flow:**

1. System refuses the merge with the message that the two are different people.
2. Use case ends.

### A3: Merge with itself

**Trigger:** The duplicate chosen is the player itself (step 3)  
**Flow:**

1. System refuses the merge with the message that a player cannot be merged with itself.
2. Use case ends.

### A4: Player does not exist

**Trigger:** The player or the chosen duplicate is not known to the system (step 3)  
**Flow:**

1. System reports that the page was not found.
2. Use case ends.

## Postconditions

### Success Postconditions

- Only the kept player exists; it carries all appearances, events, remembered names and inbox references of both records.

### Failure Postconditions

- Both player records and all their references are unchanged.

## Business Rules

### BR-001: Duplicate suggestions

Likely duplicates are other players whose name similarity to this player is at least 0.4, at most ten, ordered by similarity.

### BR-002: No merge of players who met

Two players who appeared in the same match are different people and cannot be merged.

### BR-003: Merge keeps all history

A merge keeps every appearance and remembered name of the duplicate under the kept player; an inbox item listing both players as candidates keeps only the kept player.

### BR-004: Required name

A player always has a name; it is stored as a single text holding all of the player's names.

> Note: there is no way to delete a player other than merging it into another one.
