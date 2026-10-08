---
name: change
description: >
  One-command AI Unified Process change for Lineup Tracker: updates the specification first,
  stops for the user's approval, then implements, tests and closes the change. Use when the user
  runs /change UC-XXX <what should change>, /change LT-NNN (work an existing Backlog task),
  /change <new capability> (no id = new use case), or asks to "fix a gap in UC-XXX", "change the
  behaviour of UC-XXX", "add a feature through the AI UP workflow", or to fix a bug "the AI UP way".
---

# Change Through the AI Unified Process

Arguments: `$ARGUMENTS` — an optional use case id (`UC-005`) or Backlog task id (`LT-023`) followed
by the wanted change in plain words. No id means a new use case. This skill only orchestrates; the
work is done by the skills it calls, and `CLAUDE.md` ("AI Unified Process") holds the rules.

The spec in `docs/` says *what* the system does; the Backlog task tracks *this change* (scope,
plan, progress, verification). Every Backlog write goes through the `backlog` CLI with `--plain`
for reads — never edit `.backlog/` files by hand. Run `backlog <command> --help` before an
unfamiliar option.

## Phase 0 — Backlog guard

1. `backlog config get autoCommit` must print `false`. If it prints `true`, stop and ask the user
   to run `backlog config set autoCommit false` — the CLI would otherwise commit (including
   anything already staged), and only the user commits.
2. Find the task: with an `LT-NNN` argument, `backlog task view LT-NNN --plain` and take the use
   case and change from it. Otherwise `backlog search "<UC-XXX or key words>" --plain` and
   `backlog task list --status "To Do" --plain`; reuse a matching open task instead of creating a
   duplicate, and say which one you reuse.

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
5. Create (or update) the Backlog task in status `To Do`:
   ```bash
   backlog task create "UC-XXX: <change in a few words>" \
     -d "<outcome and why; names every affected UC-XXX>" \
     --doc docs/use_cases/UC-XXX-<name>.md \
     --ac "UC-XXX A3: <flow outcome, testable>" \
     --ac "UC-XXX BR-002: <rule outcome, testable>"
   ```
   One `--ac` per changed step, flow or rule, prefixed with its qualified id so it maps 1:1 to a
   test `@DisplayName`. For an existing task use `backlog task edit LT-NNN --ac ...` instead.
   *Code deviates*: the criteria are the violated step, flow or rule. *Purely technical*: the
   criteria are the measurable technical goal, and the description says "no spec change".
6. **STOP** (spec change only — the other triage results go straight to Phase 2). Show the user:
   the triage result, `git diff -- docs/`, the task id with its acceptance criteria, any open
   questions, and the proposed status. Ask for approval and end your turn. Do not touch code in
   this phase.

## Phase 2 — Construction (only after explicit approval)

Approval means the user said so after seeing the diff — never infer it from an earlier message.
If the user asks for spec changes instead, return to Phase 1 step 3 and keep the task's acceptance
criteria in step with the spec.

1. `backlog task edit LT-NNN -s "In Progress" -a @claude`.
2. Run the `implement` skill for each affected use case. It reconciles the existing code, adds
   `UC-XXX BR-YYY` markers and reports open questions — relay those to the user before going on.
   Record its plan with `backlog task edit LT-NNN --plan "1. ..."`, and decisions or open
   questions with `--append-notes`. Work outside the acceptance criteria: stop and ask, never
   widen the task silently.
3. Run the `quarkus-test` skill for each affected use case. Every changed or added flow and rule
   gets a test; a bug fix gets a regression test named after the violated flow or rule.
4. Run the full suite: `./mvnw verify` (or the Dev MCP test runner when dev mode is running).
   Report failures with the decisive output; do not weaken a test to make it pass.
5. Check each acceptance criterion whose test passes: `backlog task edit LT-NNN --check-ac N`.

## Phase 3 — Close

1. Finish the Backlog task (only when every acceptance criterion is checked and the suite is
   green; otherwise leave it `In Progress` and say what is missing):
   ```bash
   backlog task edit LT-NNN --check-dod 1 \
     --append-notes "<test result, e.g. ./mvnw verify 260/260 green>" \
     --final-summary "<triage; spec elements changed; how verified>" \
     -s Done
   ```
2. Record an experience (`manage-experience`) only if the change hit a non-obvious trap.
3. Summarize: task id, triage, spec elements changed, files changed (each tied to its spec element), tests
   added, test results, open questions, and the proposed new status for each use case (the user
   sets it).
4. Do not commit — the user commits each change (spec, code and `.backlog/` together).
