# F-031 — GitHub Actions workflow + artifact upload

**Milestone:** M4 — Reporting, CI, and DX
**PRD references:** Post-MVP-AC-5, §21 Phase 6

## Summary

There is no `.github/workflows/` directory in the repo. To meet PRD post-MVP AC-5 ("CI/CD pipeline integration publishes reports as build artifacts"), add a baseline workflow.

## Acceptance Criteria

- [ ] `.github/workflows/ci.yml` runs on push and pull request to `main`:
  - Set up Java 25 (Adoptium) via `actions/setup-java`.
  - Run `./gradlew build test` (using the wrapper, not `./gradlew`, since CI does not have the user's symlink).
  - Upload `build/reports/tests/test/` as an artefact on failure.
- [ ] `.github/workflows/smoke.yml` (manual `workflow_dispatch`):
  - Same setup.
  - Runs `./gradlew gatlingRun -Dloadtest.env=config/environments/local.yaml -Dloadtest.run=config/runs/smoke.yaml`.
  - Requires `API_TOKEN` and DB credentials as repo secrets — fails fast (via F-002) if not present.
  - Uploads `build/reports/gatling/` (HTML report, `run-metadata.json`, config snapshot) as artefacts.
- [ ] Both workflows pass at least once on `main`.

## Implementation Notes

- Use the wrapper (`./gradlew`), not `./gradlew`. The user's CLAUDE.md preference is for local development.
- Cache Gradle dependencies via `actions/setup-java`'s built-in cache.
- The smoke workflow needs a running target API — for the MVP, point it at a stub container (or skip with a `if:` guard until F-038 introduces WireMock).

## Out of scope

- Scheduled load test runs (cron) — handled at the team level, not the framework.
- Multi-OS matrix.

## Suggested labels

`area:ci`, `type:feature`, `milestone:M4`
