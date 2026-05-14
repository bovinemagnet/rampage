# F-033 — Migrate review, roadmap, and features into Antora

**Milestone:** M4 — Reporting, CI, and DX
**Depends on:** F-032

## Summary

Once `src/docs/` exists (F-032), the planning artefacts in `docs/review/`, `docs/roadmap/`, and `docs/features/` should be migrated to `.adoc` and become part of the user-facing site. Keep the original Markdown in `docs/` as planning history.

## Acceptance Criteria

- [ ] `src/docs/modules/ROOT/pages/reference/code-review.adoc` mirrors `docs/review/code-review.md`.
- [ ] `src/docs/modules/ROOT/pages/reference/requirements-traceability.adoc` mirrors `docs/review/requirements-traceability.md`.
- [ ] `src/docs/modules/ROOT/pages/reference/roadmap.adoc` mirrors `docs/roadmap/roadmap.md`.
- [ ] Each feature brief becomes a page under `src/docs/modules/ROOT/pages/features/F-NNN-*.adoc`.
- [ ] `nav.adoc` is updated with the new sections.
- [ ] Cross-references between pages use `xref:module:page.adoc[]` syntax (per project convention).
- [ ] `gradle21w antora` succeeds with no errors.

## Implementation Notes

- Use the `asciidoc-antora-writer` agent for the conversion to preserve British spelling, admonitions, and code-block syntax.
- Tables in Markdown map to AsciiDoc tables; preserve the column structure.
- Keep the Markdown versions in `docs/` — they are the planning input; AsciiDoc versions are the published artefact.

## Out of scope

- A migration script — one-off manual conversion.

## Suggested labels

`area:docs`, `type:chore`, `milestone:M4`
