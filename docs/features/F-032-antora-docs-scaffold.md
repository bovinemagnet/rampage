# F-032 — Antora docs site scaffold under src/docs/

**Milestone:** M4 — Reporting, CI, and DX
**PRD references:** §6 (Repository Layout) — docs/

## Summary

Project conventions specify that final documentation lives in `src/docs/` as an Antora component, validated with `./gradlew antora`. No such scaffold exists. The current `docs/` tree is plain Markdown (this file included), which is appropriate for planning artefacts but not for the final user-facing docs.

## Acceptance Criteria

- [x] `src/docs/antora.yml` declares a component named `rampage` versioned with the project (currently `0.1.0`).
- [x] `src/docs/modules/ROOT/nav.adoc` lists at least: Index, Getting Started, Configuration Reference, Scenario Authoring, Security, Troubleshooting.
- [x] `src/docs/modules/ROOT/pages/index.adoc` and each navigation entry exist as stub pages.
- [x] An `antora-playbook.yml` (or `./gradlew antora` task wiring) builds the site without errors.
- [ ] `./gradlew antora` runs in CI as part of `F-031`. _(not implemented)_

## Implementation Notes

- Use the `asciidoc-antora-writer` agent in parallel for `.adoc` file authoring per project convention.
- Externalise any Mermaid diagrams into `src/docs/modules/ROOT/examples/`.
- Author lines should say "Paul Snow" by default.

## Out of scope

- Migrating the existing review/roadmap docs (covered by F-033).

## Suggested labels

`area:docs`, `type:feature`, `milestone:M4`
