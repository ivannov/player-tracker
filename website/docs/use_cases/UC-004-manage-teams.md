# Use Case: Manage Teams

## Overview

**Use Case ID:** UC-004  
**Use Case Name:** Manage Teams  
**Primary Actor:** Administrator  
**Goal:** Keep the list of clubs and the formations (age groups and senior teams) each club fields up to date.  
**Trigger:** The Administrator chooses to add, edit or delete a team from the team list.  
**Status:** Implemented  

## Preconditions

- The Administrator is signed in (UC-003).

## Main Success Scenario

1. Administrator opens the team list and chooses to add a team.
2. System shows the team form with the youth formations U15 to U19 and the senior first team preselected.
3. Administrator enters name, location, an optional logo address and adjusts the selected formations.
4. System saves the team together with one formation per selected type and shows the team list.
5. Administrator chooses to edit an existing team.
6. System shows the form filled with the team's data and its current formations.
7. Administrator changes the data and the formation selection and saves.
8. System updates the team, adds the newly selected formations and removes the deselected ones together with their participations, and shows the team list.

## Alternative Flows

### A1: Delete a team

**Trigger:** Administrator chooses to delete a team instead of editing it (step 5)  
**Flow:**

1. System checks that the team has no formations.
2. System deletes the team and removes it from the list.
3. Use case ends.

### A2: Team still has formations

**Trigger:** The team to delete still has formations when deleting instead of editing (step 5)  
**Flow:**

1. System refuses the deletion with the message that the team still has formations or participations.
2. Use case ends.

### A3: Deselected formation is still in use

**Trigger:** A deselected formation has a participation that is referenced by a match or lineup (step 8)  
**Flow:**

1. System rejects the change with a data-constraint error message and keeps the team unchanged.
2. Use case ends.

### A4: Team does not exist

**Trigger:** The team to edit or delete is not known to the system (step 5)  
**Flow:**

1. System reports that the page was not found.
2. Use case ends.

## Postconditions

### Success Postconditions

- The team and exactly the selected formations exist.
- Participations of deselected formations no longer exist.

### Failure Postconditions

- The team, its formations and its participations are unchanged.

## Business Rules

### BR-001: Required team data

Name and location are required; the logo address is optional and a blank logo address is stored as "no logo".

### BR-002: Formation types

A team fields at most one formation of each type: U15, U16, U17, U18, U19, first team (shown as "Мъже" without suffix), second team ("II") and third team ("III").

### BR-003: Default formations

A new team starts with U15, U16, U17, U18, U19 and the first team preselected.

### BR-004: Only teams without formations can be deleted

A team can be deleted only after all of its formations have been removed.
