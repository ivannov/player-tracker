# Use Case: Extract Lineups Daily

## Overview

**Use Case ID:** UC-010  
**Use Case Name:** Extract Lineups Daily  
**Primary Actor:** Scheduler  
**Secondary Actors:** BFU Tournaments Site, EBFU Site, Embedding Service  
**Goal:** Load the day's matches of every opted-in competition automatically, without an Administrator present.  
**Trigger:** Every day at 23:00.  
**Status:** Implemented  

## Preconditions

- At least one competition has daily-extraction settings (UC-005).

## Main Success Scenario

1. System selects every competition that has daily-extraction settings and at least one participation.
2. For each selected competition, System extracts and saves the matches played today, using the competition's results page address and current season, exactly as on confirmation in UC-009.
3. System continues with the next competition until all are processed.

## Alternative Flows

### A1: Extraction fails for a competition

**Trigger:** The results page of a competition cannot be read (step 2)  
**Flow:**

1. System logs the failure for that competition and date.
2. Use case continues at step 3.

### A2: Previous run still in progress

**Trigger:** The previous daily run has not finished yet (step 1)  
**Flow:**

1. System skips this run.
2. Use case ends.

## Postconditions

### Success Postconditions

- Today's identified matches of every selected competition are saved as in UC-009.
- Unclear names are waiting in the inbox (UC-011).

### Failure Postconditions

- A competition whose extraction failed has no data stored for today by this run; other competitions are unaffected.

## Business Rules

### BR-001: Opt-in competitions only

A competition is extracted daily only when it has daily-extraction settings (UC-005 BR-002) and at least one participation.

### BR-002: No overlapping runs

A new daily run never starts while the previous one is still running.

### BR-003: Same rules as on-demand extraction

The daily run applies the rules of UC-009 BR-001 to UC-009 BR-008 unchanged; unclear names never stop the run (UC-009 BR-005).
