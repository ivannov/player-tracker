# Use Case: Sign In and Out

## Overview

**Use Case ID:** UC-003  
**Use Case Name:** Sign In and Out  
**Primary Actor:** Registered User, Administrator  
**Goal:** Prove one's identity so that the system grants the permissions of one's role, and end that session again.  
**Trigger:** The actor chooses to sign in from the navigation.  
**Status:** Implemented  

## Preconditions

- The actor has a user account with at least one role (Administrator or User).

## Main Success Scenario

1. Actor opens the sign-in page.
2. Actor enters username and password and submits.
3. System verifies the credentials.
4. System shows the personal start page greeting the actor by username; the navigation now shows data-entry, extraction and account menus.
5. Actor works with the system according to the actor's role.
6. Actor chooses to sign out.
7. System ends the session and shows the public home page.

## Alternative Flows

### A1: Wrong credentials

**Trigger:** The username or password is incorrect (step 3)  
**Flow:**

1. System shows the sign-in page again with an error message.
2. Use case continues at step 2.

## Postconditions

### Success Postconditions

- While signed in, the actor holds the permissions of the actor's role.
- After signing out, the actor is treated as a Visitor again.

### Failure Postconditions

- The actor remains unauthenticated and has only Visitor permissions.

## Business Rules

### BR-001: Roles

There are two roles: Administrator, who may change all data, and User, who may only read. A signed-in User sees the same data as a Visitor.

### BR-002: Personal start page requires sign-in

Only the personal start page requires a signed-in actor; every browse page is public (UC-001 BR-001).

### BR-003: Passwords are never stored in readable form

Passwords are stored only in a non-reversible form and checked against it at sign-in.

### BR-004: Accounts are provisioned outside the application

The application offers no screen to register, create users, assign roles or change a password; one initial Administrator account is provided with the installation.
