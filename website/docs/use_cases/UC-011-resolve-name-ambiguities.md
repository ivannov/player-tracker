# Use Case: Resolve Name Ambiguities

## Overview

**Use Case ID:** UC-011  
**Use Case Name:** Resolve Name Ambiguities  
**Primary Actor:** Administrator  
**Goal:** Decide which player or team an unclear scraped name refers to, so that the player appears in the lineups the name was left out of and future extractions identify the name automatically.  
**Trigger:** The Administrator opens the inbox, typically after the navigation shows a count of pending items.  
**Status:** Tested  

## Preconditions

- The Administrator is signed in (UC-003).
- At least one unclear name is pending (raised by UC-009, UC-010 or UC-007).

## Main Success Scenario

1. Administrator opens the inbox.
2. System lists the pending items, oldest first, each with the scraped name and the team it was scraped under; player items show ranked candidate players with their score and, when different from that team, the candidate's latest club.
3. Administrator picks the candidate player the name refers to.
4. System remembers the name as that player's name for the source and team, adds the player to the lineup of every match the name was raised in, marks the item resolved with the Administrator's name and the time, and refreshes the list.

## Alternative Flows

### A1: Name belongs to a new player

**Trigger:** None of the candidates is the right player (step 3)  
**Flow:**

1. Administrator confirms the name as a new player.
2. System creates a player with the scraped name, remembers the name for the source and team, adds the player to the lineup of every match the name was raised in, marks the item resolved and refreshes the list.
3. Use case ends.

### A2: Team name conflict

**Trigger:** The item is a team item (step 3)  
**Flow:**

1. Administrator picks the correct team from the list of all teams.
2. System remembers the scraped name as that team's name for the source, replacing any earlier mapping, marks the item resolved and refreshes the list.
3. Use case ends.

### A3: Item already resolved

**Trigger:** Another Administrator resolved the item in the meantime (step 4)  
**Flow:**

1. System shows the message that the item was already resolved and refreshes the list.
2. Use case ends.

### A4: Action does not fit the item

**Trigger:** A player action is applied to a team item or vice versa (step 4)  
**Flow:**

1. System shows the message that the item is of another type; nothing is changed.
2. Use case ends.

## Postconditions

### Success Postconditions

- The item is resolved with the chosen player or team, the resolver and the time.
- The scraped name is remembered, so the next extraction identifies it automatically.
- For a player item, the player has a lineup entry with its goals and cards in every match the name was raised in.

### Failure Postconditions

- The item stays pending, no name mapping is changed and no lineup entry or event is added.

## Business Rules

### BR-001: Only pending items can be resolved

An item that is already resolved cannot be resolved again.

### BR-002: Player items resolve to players, team items to teams

A player item is resolved by choosing a candidate or confirming a new player; a team item is resolved only by choosing an existing team.

### BR-003: Resolution is remembered

Resolving a player item remembers the name for the same source and team (UC-009 BR-004); resolving a team item remembers or replaces the team name for the source (the only way a remembered team name changes, see UC-009 BR-007).

### BR-004: Resolution is traceable

Every resolution records who resolved the item and when.

### BR-005: Pending count visible to Administrators only

The navigation shows the number of pending items to Administrators; other signed-in users see no count.

### BR-006: Resolution completes the lineups

Resolving a player item adds the chosen or new player to the lineup of every saved match the name was raised in while the item was pending (UC-009 BR-010), with the starter flag, shirt number, substitution minutes, goals and cards read for the name in that match. When the player already has a lineup entry in such a match, that entry is kept and only the goals and cards not yet stored for it are added (UC-009 BR-006). A match for which no lineup details were kept with the item gets no lineup entry; the player appears there from the next extraction of that match.

> Note: A "dismissed" state exists in the data model but no screen sets it.
