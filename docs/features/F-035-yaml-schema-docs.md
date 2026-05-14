# F-035 — Auto-generated YAML schema docs from model classes

**Milestone:** M4 — Reporting, CI, and DX
**PRD references:** Post-MVP-AC-8

## Summary

PRD post-MVP AC-8 expects config schema documentation to be generated automatically. Model classes already carry `@JsonProperty` annotations; a small reflection-based generator can emit a JSON Schema and a human-readable reference page.

## Acceptance Criteria

- [ ] A new Gradle task `gradle21w generateSchemaDocs` produces:
  - `build/schema/environment.schema.json`, `run.schema.json`, `scenario.schema.json` (JSON Schema draft 2020-12).
  - `build/schema/reference.adoc` listing every field with type, default, required-or-optional, and any `@JsonProperty` description.
- [ ] The generated AsciiDoc is wired into the Antora site under `src/docs/.../config-reference.adoc` (committed at release time, or referenced as a generated include).
- [ ] CI runs the task and fails if the generated schemas differ from the committed copy (drift detection).

## Implementation Notes

- Use `jackson-module-jsonSchema` or a hand-rolled reflection walker — Jackson's built-in schema generator may be sufficient for these POJOs.
- Reflection should pick up: field name, type, default value (from the field initialiser), nullability (via `@JsonProperty` `required` if added).
- Long-term, drive the model classes from a schema rather than the other way around — out of scope.

## Out of scope

- Live in-IDE YAML completion (would benefit from publishing the JSON Schema with `$id`, but tooling integration is the user's responsibility).

## Suggested labels

`area:docs`, `area:tooling`, `type:feature`, `milestone:M4`
