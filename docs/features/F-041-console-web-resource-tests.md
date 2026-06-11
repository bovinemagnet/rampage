# F-041 — Console web-resource test coverage

**Milestone:** Platform evolution — Theme 4 (Test management)
**PRD references:** —
**Traces to:** 2026-06 codebase review

## Summary

The console's JAX-RS resources (`ConfigsResource`, `RunsResource`, `ReportsResource`, `HistoryResource`, `DashboardResource`, `StreamResource`) have no dedicated tests. The underlying services are tested, but the HTTP layer — status codes, content types, path-traversal rejection at the resource boundary, SSE stream behaviour — is not. The path-traversal defences in `ConfigBrowser`/`ConfigEditor`/`RunHistoryService` are correct today but have no regression tests pinning them.

## Acceptance Criteria

- [ ] `@QuarkusTest` coverage for each REST resource: happy path plus the key failure paths (unknown run id, missing report file, invalid config payload).
- [ ] Explicit path-traversal regression tests: requests containing `../`, absolute paths, and encoded traversal sequences against `GET /configs/*` and `GET /reports/*` are rejected (4xx, no file content leaked).
- [ ] `POST /configs/save` rejects writes that resolve outside the config root.
- [ ] An SSE smoke test asserts that `StreamResource` emits run-status events end-to-end (RestAssured or a raw client; one event is enough).
- [ ] Tests run in the standard `:console:test` task without external dependencies.

## Implementation Notes

- RestAssured ships with `quarkus-junit5`; the existing `ConsoleE2eTest` shows the bootstrap pattern.
- Keep file-system fixtures under `@TempDir` and point `PathResolver` at them via test configuration.

## Out of scope

- Browser/UI testing of the dashboard JavaScript.

## Suggested labels

`area:test`, `type:test`, `priority:medium`
