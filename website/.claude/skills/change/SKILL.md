---
name: change
description: >
  One-command AI Unified Process change for Lineup Tracker: updates the specification first,
  stops for the user's approval, then implements, tests and closes the change. Use when the user
  runs /change UC-XXX <what should change>, /change <new capability> (no id = new use case), or asks
  to "fix a gap in UC-XXX", "change the behaviour of UC-XXX", "add a feature through the AI UP
  workflow", or to fix a bug "the AI UP way".
---

# Change Through the AI Unified Process

Arguments: `$ARGUMENTS` — an optional use case id (`UC-005`) followed by the wanted change in plain
words. No id means a new use case. This skill only orchestrates; the work is done by the skills it
calls, and `CLAUDE.md` ("AI Unified Process") holds the rules.

## Phase 1 — Specification (always first)

1. Read `docs/use_cases.puml`, the named spec in full (or grep `docs/use_cases/` to find the
   affected ones when no id is given), the rules it cites, and `docs/entity_model.md`.
2. **Triage** and state the result in one line:
   - *Spec change* — new or changed behaviour, or a bug the spec is silent or wrong about (including
     anything a `> Note:` describes). Continue with step 3.
   - *Code deviates from the spec* — the spec already describes the wanted behaviour. Say which step,
     flow or rule the code violates, skip to Phase 2 (no spec change, no approval stop).
   - *Purely technical* — no visible behaviour change. Say so, skip to Phase 2.
3. Update the specification with the AI Unified Process skills (Skill tool):
   - `aiup-core:use-case-spec` for the changed or new use case. Pass the use case id and the change.
     Let it ask its questions — relay them to the user; do not answer them yourself.
   - `aiup-core:use-case-diagram` when a use case or actor is added, removed or renamed.
   - `aiup-core:entity-model` when data changes.
   - Remove every `> Note:` the change resolves. A new use case gets `**Status:** Draft`; never
     change the status of an existing one.
4. Run the spec checks from `CLAUDE.md` (both `--strict`) and fix what they report. For a new use
   case or a change to more than one flow, also run `aiup-core:spec-review` for that use case.
5. **STOP.** Show the user: the triage result, `git diff -- docs/`, any open questions, and the
   proposed status. Ask for approval and end your turn. Do not touch code in this phase.

## Phase 2 — Construction (only after explicit approval)

Approval means the user said so after seeing the diff — never infer it from an earlier message.
If the user asks for spec changes instead, return to Phase 1 step 3.

1. Run the `implement` skill for each affected use case. It reconciles the existing code, adds
   `UC-XXX BR-YYY` markers and reports open questions — relay those to the user before going on.
2. Run the `quarkus-test` skill for each affected use case. Every changed or added flow and rule
   gets a test; a bug fix gets a regression test named after the violated flow or rule.
3. Run the full suite: `./mvnw test` (or the Dev MCP test runner when dev mode is running). Report
   failures with the decisive output; do not weaken a test to make it pass.

## Phase 3 — Close

1. Create the Backlog task (Backlog MCP, following its workflow instructions) naming the use cases,
   with acceptance criteria mapped to the changed steps, flows and rules. If Backlog is not
   available, say so and print the task text instead.
2. Record an experience (`manage-experience`) only if the change hit a non-obvious trap.
3. Summarize: triage, spec elements changed, files changed (each tied to its spec element), tests
   added, test results, open questions, and the proposed new status for each use case (the user
   sets it).
4. Do not commit — the user commits each change separately.
