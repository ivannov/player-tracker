# Use Case: Record Match Manually

## Overview

**Use Case ID:** UC-008  
**Use Case Name:** Record Match Manually  
**Primary Actor:** Administrator  
**Goal:** Enter or correct a match, its lineups, substitutions, goals and cards by hand when automatic extraction is unavailable or wrong.  
**Trigger:** The Administrator chooses to add a match from the match list, or opens an existing match to edit its lineups.  
**Status:** Implemented  

## Preconditions

- The Administrator is signed in (UC-003).
- Both teams have a participation in the same competition and season (UC-006).

## Main Success Scenario

1. Administrator chooses to add a match.
2. System offers all participations, grouped by competition and season.
3. Administrator selects home and away participation, the match date and optionally the score, and saves.
4. System creates the match and shows its detail page.
5. Administrator adds a player to the home or away lineup by choosing an existing player or entering the name of a new player, and sets whether the player started and the shirt number.
6. System adds the player to that side of the lineup, creating the player if a new name was entered.
7. Administrator records the minutes a player was substituted in or out.
8. System stores the substitution minutes.
9. Administrator records an event for a player in the lineup with its type and an optional minute.
10. System stores the event and shows it next to the player.

## Alternative Flows

### A1: Participations do not match

**Trigger:** Home and away are the same participation, or belong to different competitions or seasons (step 3)  
**Flow:**

1. System shows the form again with the message that both sides must belong to the same competition and season.
2. Use case continues at step 3.

### A2: Invalid lineup entry

**Trigger:** The chosen side does not play in this match, no player was chosen or entered, or the player is already in this match (step 5)  
**Flow:**

1. System shows the match again with a message describing the problem; nothing is added.
2. Use case continues at step 5.

### A3: Invalid substitution minutes

**Trigger:** A minute is not a number, is outside 0 to 130, or the minute substituted out is not after the minute substituted in (step 7)  
**Flow:**

1. System shows the match again with a message describing the problem; the minutes are unchanged.
2. Use case continues at step 7.

### A4: Invalid event

**Trigger:** The event type is unknown or the minute is not a number between 0 and 130 (step 9)  
**Flow:**

1. System shows the match again with a message describing the problem; no event is added.
2. Use case continues at step 9.

### A5: Remove a lineup entry or event

**Trigger:** Administrator chooses to remove a player from the lineup or to remove an event (step 5 or step 9)  
**Flow:**

1. System removes the lineup entry together with all of its events, or removes the single event.
2. Use case continues at step 5.

## Postconditions

### Success Postconditions

- The match exists with its date, sides and score.
- The lineups, substitution minutes and events reflect the Administrator's entries.

### Failure Postconditions

- A rejected entry leaves the match, its lineups and events unchanged.

## Business Rules

### BR-001: Both sides share competition and season

Home and away must be two different participations of the same competition and season.

### BR-002: One lineup entry per player and match

A player appears at most once in a match, on one side only, and that side must be the home or away participation of the match.

### BR-003: Valid minutes

Event and substitution minutes are optional; when given they lie between 0 and 130.

### BR-004: Substituted out after substituted in

When both substitution minutes are set, the minute substituted out is later than the minute substituted in.

### BR-005: Event types

An event is one of: goal, penalty goal, own goal, yellow card, second yellow card, red card.

### BR-006: Removing a lineup entry removes its events

Removing a player from a lineup also removes all goals and cards recorded for that player in the match.

> Note: the application offers no way to edit or delete the match itself (date, sides, score) once it is created.
