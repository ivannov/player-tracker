# Use Case: Manage Participations

## Overview

**Use Case ID:** UC-006  
**Use Case Name:** Manage Participations  
**Primary Actor:** Administrator  
**Goal:** Record which formations of a team take part in which competition in a given season, so that matches and lineups can be attributed to them.  
**Trigger:** The Administrator chooses to add, edit or delete a participation from the participation list.  
**Status:** Implemented  

## Preconditions

- The Administrator is signed in (UC-003).
- The team and the competition already exist (UC-004, UC-005).

## Main Success Scenario

1. Administrator opens the participation list and chooses to add participations.
2. System shows the form with team, competition, season and the formation types, with U15 to U19 and the first team preselected.
3. Administrator selects team, competition and season, adjusts the formation types and saves.
4. System checks that none of the selected formations already takes part in that competition and season.
5. System creates any missing formation of the team, creates one participation per selected formation and shows the participation list.

## Alternative Flows

### A1: Invalid season

**Trigger:** The season is missing or longer than nine characters (step 3)  
**Flow:**

1. System shows the form again with the message that the season must have the format YYYY/YYYY.
2. Use case continues at step 3.

### A2: Participation already exists

**Trigger:** One of the selected formations already takes part in the competition and season (step 4)  
**Flow:**

1. System shows the form again with the message that the participation already exists; nothing is saved.
2. Use case continues at step 3.

### A3: Change the formations of a participation

**Trigger:** Administrator chooses to edit an existing participation (step 1)  
**Flow:**

1. System shows the team, competition and season as fixed values and the formations of that team that currently take part.
2. Administrator changes the formation selection and saves.
3. System removes the participations of deselected formations and creates participations (and missing formations) for newly selected ones.
4. Use case ends.

### A4: Delete a participation

**Trigger:** Administrator chooses to delete a participation (step 1)  
**Flow:**

1. System checks that no match and no lineup entry refers to the participation.
2. System deletes the participation.
3. Use case ends.

### A5: Participation is in use

**Trigger:** A match or lineup entry refers to the participation when deleting (step 1)  
**Flow:**

1. System refuses the deletion with a message that the participation still has matches or lineups.
2. Use case ends.

### A6: Deselected participation is in use

**Trigger:** A match or lineup entry refers to a participation whose formation was deselected when editing (step 1)  
**Flow:**

1. System rejects the change with a data-constraint error message; no participation is changed.
2. Use case ends.

## Postconditions

### Success Postconditions

- Each selected formation has exactly one participation in the chosen competition and season.

### Failure Postconditions

- No participation or formation is created, changed or removed.

## Business Rules

### BR-001: One participation per formation, competition and season

A formation takes part in a given competition at most once per season.

### BR-002: Season format

A season is written as YYYY/YYYY (at most nine characters), for example 2024/2025.

### BR-003: Formations are created on demand

Selecting a formation type the team does not field yet creates that formation for the team.

### BR-004: Participations in use cannot be deleted

A participation that is referenced by a match or a lineup entry cannot be deleted.

### BR-005: Editing works on the whole group

A participation is edited together with all participations of the same team, competition and season; team, competition and season cannot be changed.
