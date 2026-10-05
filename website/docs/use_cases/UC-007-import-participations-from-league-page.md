# Use Case: Import Participations from League Page

## Overview

**Use Case ID:** UC-007  
**Use Case Name:** Import Participations from League Page  
**Primary Actor:** Administrator  
**Secondary Actors:** BFU Tournaments Site  
**Goal:** Register all teams of a league for a season in one go by reading the team list from the league's page on the BFU tournaments site, creating missing teams and formations along the way.  
**Trigger:** The Administrator starts the participation import from the participation list.  
**Status:** Implemented  

## Preconditions

- The Administrator is signed in (UC-003).
- The competition already exists (UC-005).

## Main Success Scenario

1. Administrator enters the address of the league page, selects the competition and enters the season.
2. System reads the names of all teams listed on the league page from the BFU Tournaments Site.
3. System proposes, for each listed name, the existing team with exactly the same name (ignoring case) and lists that team's formations.
4. Administrator decides for each row: keep or pick an existing team and formation, choose a formation type the team does not field yet, enter name and location of a new team, or leave the row empty to skip it.
5. System shows a review listing, per row, the team, the formation and whether the participation will be created, already exists or is skipped.
6. Administrator confirms the import.
7. System creates the new teams (each with a first-team formation), the new formations and the participations that do not exist yet, remembers each listed name as the site's name for the chosen team, and shows the participation list.

## Alternative Flows

### A1: League page cannot be read

**Trigger:** The BFU Tournaments Site does not deliver a readable team list (step 2)  
**Flow:**

1. System shows the first step again with the extraction error and the entered values.
2. Use case continues at step 1.

### A2: Row cannot be interpreted

**Trigger:** A row names an unknown team, a formation that does not belong to the chosen team, an invalid formation type, or a team without any formation choice (step 4)  
**Flow:**

1. System marks the row as skipped with the reason in the review.
2. Use case continues at step 5.

### A3: Competition does not exist

**Trigger:** The selected competition is not known to the system (step 5)  
**Flow:**

1. System reports that the page was not found.
2. Use case ends.

## Postconditions

### Success Postconditions

- Every non-skipped row has a participation of the chosen formation in the competition and season.
- New teams and formations requested in the rows exist.
- Each listed name is remembered as the BFU tournaments name of its team.

### Failure Postconditions

- When the import is not confirmed or fails, no team, formation, participation or name mapping is stored.

## Business Rules

### BR-001: New teams start with the first team

A team created during import gets a single first-team formation, which takes part in the competition.

### BR-002: Existing participations are kept

A row whose formation already takes part in the competition and season is shown as "exists" and does not create a duplicate (UC-006 BR-001).

### BR-003: Season normalization

A season entered with a dash (2024-2025) is stored with a slash (2024/2025).

### BR-004: Review and save interpret rows identically

The review step and the save step apply the same interpretation to every row, so what the review shows is exactly what is saved.

### BR-005: Team names learned from the import

Saving remembers each listed name as the BFU tournaments name of the chosen team; an existing name mapped to a different team is not changed but raised for review (UC-009 BR-007).

> Note: the initial team proposal (step 3) only uses exact name matching; it does not consult names remembered from earlier imports or extractions.
