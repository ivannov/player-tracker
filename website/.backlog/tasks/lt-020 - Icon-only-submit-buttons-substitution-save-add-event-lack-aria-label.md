---
id: LT-020
title: 'Icon-only submit buttons (substitution save, add-event) lack aria-label'
status: To Do
assignee: []
created_date: '2026-08-03 18:58'
labels:
  - bug
  - accessibility
dependencies: []
priority: low
ordinal: 43000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
MT-12.10 manual QA pass (LT-011.02 follow-up) found: on the match detail page (MatchResource/detail.html), the substitution-save button ("✓") and the add-event button ("+") are icon-only <button type=submit> elements with no aria-label -- their accessible name is just the literal glyph. This is inconsistent with the same page's delete/remove links (e.g. "Премахни {player}", "Изтрий събитие"), which already carry descriptive accessible names via their link text. Confirmed live via accessibility tree (read_page) on a seeded match: button "✓" and button "+" both report their raw glyph as the only accessible name, no aria-label attribute present. Keyboard tab order through the page is otherwise sane (verified via Tab key).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Substitution-save button (✓) has a descriptive aria-label (e.g. "Запази смяна")
- [ ] #2 Add-event button (+) has a descriptive aria-label (e.g. "Добави събитие")
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 Tests are added for new functoinality and mvn verify is successfull
<!-- DOD:END -->
