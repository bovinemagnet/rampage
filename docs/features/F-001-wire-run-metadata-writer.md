# F-001 — Wire RunMetadataWriter into the simulation lifecycle

**Milestone:** M1 — MVP Honesty
**PRD references:** RUN-005, MVP-AC-10, §19
**Traces to:** D1 in `docs/review/code-review.md`

## Summary

`RunMetadataWriter` exists and is unit-tested but is never invoked by `RampageSimulation`. Setting `reporting.writeRunMetadata: true` in `run.yaml` therefore has no effect. This blocks MVP acceptance criterion 10 and partially blocks AC-8.

## Acceptance Criteria

- [ ] After a successful `gradle21w gatlingRun`, a file `run-metadata.json` is written under the resolved `reporting.outputDirectory` (default `build/reports/gatling/`).
- [ ] The file is written only when `reporting.writeRunMetadata: true`. When `false`, no file is produced.
- [ ] The metadata includes: `runId`, `runName`, `environment`, `startedAt`, `gitCommit`, `gitBranch`, `scenarios[]` (id + name + tags), and `runMetadata` (owner/application/service/changeReference/description).
- [ ] When `reporting.redactSecrets: true`, the metadata file contains no raw secret values — secret-sourced fields are absent or set to `***REDACTED***`.
- [ ] A unit or integration test asserts the file is produced and contains the expected top-level keys.

## Implementation Notes

- Call the writer from the end of the `RampageSimulation` initializer block, **after** `setUp(...)` returns. If that runs into Gatling lifecycle ordering issues, use a Gatling `before {}` or `after {}` hook (Java DSL: override `before()` / `after()` on the `Simulation` class).
- Capture `startedAt` at the start of the initializer, not at write time.
- Add `gitBranch` to `RunMetadataWriter` (currently only `gitCommit`).
- Move the cached `gitCommit` lookup out of every-write into the constructor or a memoised field (see D13).

## Out of scope

- Sanitised config snapshot (covered by F-029).
- CI artifact upload (F-031).

## Suggested labels

`area:reporting`, `priority:high`, `type:bug`, `milestone:M1`
