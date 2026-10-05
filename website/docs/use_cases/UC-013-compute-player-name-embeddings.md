# Use Case: Compute Player Name Embeddings

## Overview

**Use Case ID:** UC-013  
**Use Case Name:** Compute Player Name Embeddings  
**Primary Actor:** Scheduler  
**Secondary Actors:** Embedding Service  
**Goal:** Give every player a semantic representation of the name, so that player identification during extraction can recognise nicknames and spelling variants.  
**Trigger:** Every minute.  
**Status:** Implemented  

## Preconditions

- _None — a run without pending players does nothing._

## Main Success Scenario

1. System selects up to 25 players that have no semantic name representation yet.
2. For each selected player, System asks the Embedding Service for the semantic representation of the player's name.
3. System stores the representation with the player.

## Alternative Flows

### A1: Embedding Service unavailable

**Trigger:** The Embedding Service does not return a representation for a player (step 2)  
**Flow:**

1. System leaves the player without a representation; it is selected again by a later run.
2. Use case continues at step 2 for the next player.

### A2: Previous run still in progress

**Trigger:** The previous run has not finished yet (step 1)  
**Flow:**

1. System skips this run.
2. Use case ends.

## Postconditions

### Success Postconditions

- The processed players have a semantic name representation.

### Failure Postconditions

- Players without a representation keep none and are retried later; no other data is changed.

## Business Rules

### BR-001: Batch size

A run processes at most 25 players.

### BR-002: Never in the way of users

Representations are computed in the background, never while an Administrator creates a player or extracts lineups; until a player has one, identification falls back to name similarity (UC-009 BR-004).

### BR-003: No overlapping runs

A new run never starts while the previous one is still running.

> Note: a renamed player keeps the representation of the old name; only players without one are processed.
