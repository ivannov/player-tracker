# Use Case: Extract Lineups for a Date

## Overview

**Use Case ID:** UC-009  
**Use Case Name:** Extract Lineups for a Date  
**Primary Actor:** Administrator  
**Secondary Actors:** BFU Tournaments Site, EBFU Site, Embedding Service  
**Goal:** Load all matches a competition played on a given date — scores, lineups, substitutions, goals and cards — from the official sites into the tracker, after previewing what will be saved.  
**Trigger:** The Administrator chooses "extract for a date" from the extraction menu.  
**Status:** Tested  

## Preconditions

- The Administrator is signed in (UC-003).
- The competition exists (UC-005) and the teams playing take part in it for the chosen season (UC-006 or UC-007).

## Main Success Scenario

1. Administrator selects the competition and enters the address of its results page, the season and the match date.
2. System finds all matches of that date on the results page of the BFU Tournaments Site.
3. For each match, System reads score, lineups, substitutions, goals and cards from the match page of the BFU Tournaments Site.
4. System identifies both teams of each match and their participation in the competition and season.
5. System identifies each listed player among the players who have appeared for that team, using name similarity and, when the Embedding Service is available, semantic name similarity.
6. System shows a preview with every match, whether both teams were identified, and how many players were identified or left unclear.
7. Administrator proceeds to the summary.
8. System shows the summary of matches that will be saved and matches that will be skipped.
9. Administrator confirms.
10. System extracts the same data again and, for every match whose teams were both identified, saves the match, the lineup entries of identified players, their substitution minutes and their goals and cards, keeps the lineup details of unclear names with their inbox items, and remembers the site's team names.
11. System shows the match list.

## Alternative Flows

### A1: Invalid date

**Trigger:** The entered date is not a valid date (step 1)  
**Flow:**

1. System shows the first step again with an "invalid date" message and the entered values.
2. Use case continues at step 1.

### A2: Results page cannot be read

**Trigger:** The BFU Tournaments Site does not deliver a readable results page (step 2)  
**Flow:**

1. System shows the first step again with the extraction error and the entered values.
2. Use case continues at step 1.

### A3: Match page unavailable

**Trigger:** The BFU Tournaments Site has no usable data for a match (step 3)  
**Flow:**

1. System looks up the match by date and team names on the EBFU Site and reads the lineups from there.
2. Use case continues at step 4.

### A4: Both sources fail for a match

**Trigger:** Neither the BFU Tournaments Site nor the EBFU Site delivers data for a match after the fallback (step 3)  
**Flow:**

1. System marks the match in the preview with the errors of both sources; the match will be skipped.
2. Use case continues at step 4 for the remaining matches.

### A5: Team cannot be identified

**Trigger:** A team name is unknown, or the team does not take part in the competition and season (step 4)  
**Flow:**

1. System marks the team as unidentified, does not try to identify its players and marks the match to be skipped.
2. Use case continues at step 5 for the remaining teams.

### A6: Player cannot be identified with confidence

**Trigger:** A listed player name has no single confident match (step 5)  
**Flow:**

1. System records the name as an unclear name for review in the inbox (UC-011), together with ranked candidates, and counts it as unclear in the preview.
2. Use case continues at step 6.

### A7: Match already saved earlier

**Trigger:** A match between the same two participations on the same date already exists (step 10)  
**Flow:**

1. System reuses the existing match and adds only lineup entries and events that are not yet stored.
2. Use case continues at step 11.

### A8: Extraction fails on confirmation

**Trigger:** The results page cannot be read when confirming (step 10)  
**Flow:**

1. System returns to the first step with the extraction error; the reviewed preview is lost.
2. Use case continues at step 1.

## Postconditions

### Success Postconditions

- Every match of the date whose teams were both identified exists with its score.
- Each identified player has one lineup entry for the match with starter flag, shirt number, substitution minutes and events.
- Unclear player names and conflicting team names are waiting in the inbox (UC-011) and linked to the match; each unclear player name carries its lineup details for the match.
- The site's team names are remembered for the identified teams.

### Failure Postconditions

- When confirmation fails, no match, lineup entry or event is stored by this run.

## Business Rules

### BR-001: Primary and fallback source

Match data comes from the BFU Tournaments Site. Only when that site has no usable data for a match are lineups taken from the EBFU Site, which provides lineups only.

### BR-002: Team identification

A scraped team name is identified first by a name previously remembered for that source, otherwise by an exact name match ignoring case. The team must take part in the selected competition and season.

### BR-003: Only fully identified matches are saved

A match is saved only when its data could be read and both teams were identified with their participation. Players are identified only for identified teams.

### BR-004: Confident player identification

A name already remembered for the same source and team is identified immediately. Otherwise candidates are only players who have appeared for that team. With semantic similarity available, a name is identified when the best candidate scores at least 0.8 and leads the next by at least 0.05; otherwise by name similarity, at least 0.6 with a lead of at least 0.05. An identified name is remembered for that source and team.

### BR-005: Unclear names never block extraction

A player name that is not identified confidently is left out of the lineup and placed in the inbox with up to five candidates from the team and up to three close name matches from other clubs (name similarity at least 0.5). Candidates from other clubs are never identified automatically. An unclear name already pending for the same team is updated instead of duplicated.

### BR-006: Repeated extraction does not duplicate data

Extracting the same date again reuses the existing match, keeps each player's existing lineup entry and skips events with the same player, type and minute.

### BR-007: Remembered team names are never silently changed

When a scraped team name is already remembered for a different team, the remembered mapping is kept and the name is placed in the inbox as a team review, once while it is pending.

### BR-008: One failing match does not stop the others

A match whose data cannot be read is skipped; the other matches of the date are still extracted.

### BR-009: The preview does not save matches

The preview saves no match, lineup entry or event, but names identified or found unclear during the preview are already remembered or placed in the inbox.

### BR-010: Unclear names keep their lineup details

For every saved match, each unclear player name that is pending in the inbox keeps its side, starter flag, shirt number, substitution minutes, goals and cards for that match with its inbox item, so that resolving the item adds the lineup entry (UC-011 BR-006). Extracting the same match again keeps the details already kept and adds only goals and cards not yet kept, as for lineup entries (BR-006). The preview keeps no lineup details (BR-009).
