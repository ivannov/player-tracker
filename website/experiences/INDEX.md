---
spec_version: "0.1"
last_updated: "2026-07-25"
categories:
  - id: "quarkus-hibernate"
    name: "Quarkus Hibernate ORM Panache"
    description: "Traps and validated patterns for JPA entity mapping, FK column naming, and schema validation in this Quarkus/Hibernate/Flyway stack."
  - id: "qute-templates"
    name: "Qute Templates"
    description: "Traps and validated patterns for writing Qute HTML templates, including expression parsing quirks and HTMX integration."
  - id: "quarkus-testing"
    name: "Quarkus Testing"
    description: "Traps and validated patterns for @QuarkusTest + REST Assured resource tests, including redirect handling and status code assertions."
  - id: "bfu-scraping"
    name: "BFU Site Scraping"
    description: "Traps and validated patterns for scraping the BFU-affiliated sites (bfu-tournaments.com, ebfu.net), including recon technique and real page structure."
---

# Experiences Index

## Category: Quarkus Hibernate ORM Panache (`quarkus-hibernate`)
* [EXP-20260725-0001](quarkus-hibernate/EXP-20260725-0001.md): Explicit @JoinColumn required for camelCase FK fields on @ManyToOne relationships.
* [EXP-20260725-0002](quarkus-hibernate/EXP-20260725-0002.md): @Column(length=...) must match the VARCHAR(N) declared in the Flyway migration DDL.

## Category: Qute Templates (`qute-templates`)
* [EXP-20260725-0001](qute-templates/EXP-20260725-0001.md): Avoid `{N}` regex quantifiers in Qute HTML attributes — Qute parses them as expressions.

## Category: Quarkus Testing (`quarkus-testing`)
* [EXP-20260725-0001](quarkus-testing/EXP-20260725-0001.md): REST Assured follows redirects by default — disable and assert 303 for Response.seeOther.


## Category: BFU Site Scraping (`bfu-scraping`)
* [EXP-20260725-0001](bfu-scraping/EXP-20260725-0001.md): Recon a new scrape target via claude-in-chrome before writing selectors -- but outerHTML/innerHTML extraction is blocked
