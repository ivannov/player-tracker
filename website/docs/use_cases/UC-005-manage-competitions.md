# Use Case: Manage Competitions

## Overview

**Use Case ID:** UC-005  
**Use Case Name:** Manage Competitions  
**Primary Actor:** Administrator  
**Goal:** Maintain the leagues and tournaments that are tracked and decide which of them are extracted automatically every day, from which results page and for which season.  
**Trigger:** The Administrator chooses to add, edit or delete a competition from the competition list.  
**Status:** Implemented  

## Preconditions

- The Administrator is signed in (UC-003).

## Main Success Scenario

1. Administrator opens the competition list and chooses to add or edit a competition.
2. System shows the competition form, including the current daily-extraction settings when editing.
3. Administrator enters the name, an optional logo address and, to enable daily extraction, the results page address and the current season.
4. System saves the competition and its daily-extraction settings and shows the competition list.

## Alternative Flows

### A1: Daily extraction left incomplete

**Trigger:** The results page address or the current season is left blank (step 3)  
**Flow:**

1. System saves the competition and removes any existing daily-extraction settings, so the competition is no longer extracted daily.
2. Use case continues at step 4.

### A2: Delete a competition

**Trigger:** Administrator chooses to delete a competition (step 1)  
**Flow:**

1. System checks that no participation belongs to the competition.
2. System deletes the competition and removes it from the list.
3. Use case ends.

### A3: Competition has participations

**Trigger:** At least one participation belongs to the competition when deleting (step 1)  
**Flow:**

1. System refuses the deletion with the message that the competition still has participations.
2. Use case ends.

### A4: Competition still has daily-extraction settings

**Trigger:** The competition to delete still has daily-extraction settings when deleting (step 1)  
**Flow:**

1. System rejects the deletion with a data-constraint error message.
2. Use case ends.

## Postconditions

### Success Postconditions

- The competition exists with the entered data.
- The competition has daily-extraction settings if and only if both the results page address and the current season were given.

### Failure Postconditions

- The competition and its daily-extraction settings are unchanged.

## Business Rules

### BR-001: Required competition data

The name is required; the logo address is optional and a blank logo address is stored as "no logo".

### BR-002: Daily extraction is opt-in

A competition is extracted daily (UC-010) only when it has both a results page address and a current season. The two are maintained together once per season, since the results page address already encodes the season.

### BR-003: One set of daily-extraction settings per competition

A competition has at most one set of daily-extraction settings.

### BR-004: Only competitions without participations can be deleted

A competition with participations cannot be deleted.

> Note: deleting a competition that still has daily-extraction settings is rejected by the database, so in practice the Administrator must first clear those settings (A1) before deleting. This looks unintended rather than a deliberate rule.
