# F-034 — Scenario scaffolding Gradle task

**Milestone:** M4 — Reporting, CI, and DX
**PRD references:** Post-MVP-AC-6

## Summary

PRD post-MVP AC-6 expects scenario templates to be generated from a CLI task. Engineers should be able to bootstrap a new scenario without copy-pasting from `scenario-one.yaml`.

## Acceptance Criteria

- [ ] `gradle21w newScenario -PscenarioId=customer-search` produces:
  - `config/scenarios/customer-search.yaml`
  - `config/graphql/customer-search.graphql`
  - `config/queries/customer-search-data.sql`
- [ ] The generated files contain TODO comments where the user must fill in details (operation name, variables, feeder columns).
- [ ] The task refuses to overwrite an existing scenario file unless `-PallowOverwrite=true`.
- [ ] The task can also operate from templates under `config/templates/` (overridable defaults).
- [ ] Documented in the Antora "Getting Started" page.

## Implementation Notes

- Implement as a Gradle task in `build.gradle.kts` using `copy`/`expand` with template strings.
- Templates live under `config/templates/scenario.yaml.tpl` etc.

## Out of scope

- A web UI for scenario authoring.

## Suggested labels

`area:dx`, `type:feature`, `milestone:M4`
