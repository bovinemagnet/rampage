# Design — Persistent results store, history and trends for the Rampage console

- **Author:** Paul Snow
- **Date:** 2026-05-16
- **Version:** 0.0.0
- **Status:** Approved design — ready for implementation planning

## Context

Rampage is a configuration-driven Gatling wrapper plus a thin live-orchestration
console (`console/`, Quarkus + HTMX). The console launches, queues, kills and
live-monitors runs, but it has no memory:

- `RunHistoryService` discovers past runs by scanning `build/reports/gatling/`
  for directories containing `index.html`, using directory mtime as a proxy for
  completion time.
- The in-process run registry (`RunOrchestrator.known`) is lost on restart.
- There is no way to query, filter, tag, compare or trend runs.

A "LoadRunner-type platform" is defined first and foremost by a *results
repository* — runs persisted over time, queryable, comparable, with trend
analysis. This increment delivers that foundation. Everything else on the
platform roadmap (regression gates surfaced in the UI, scheduled-run history,
live-dashboard depth) builds on it.

## Goals

- Persist every run — console-launched and CLI-only — to a durable store that
  survives console restarts.
- Replace the filesystem-scan history page with a queryable, searchable,
  taggable history.
- Add a side-by-side run comparison view.
- Add a trends view charting key metrics over time for a given run configuration.

## Non-goals (deferred to later roadmap themes)

- Per-tick / time-series metric storage (belongs to the live-dashboard theme).
- Authentication, RBAC, multi-user or workspace separation.
- Concurrent or distributed run execution.
- External metric export (Prometheus, Grafana, InfluxDB, Datadog).
- Persisting the *pending* queue across restarts — only finished runs are stored.

## Architecture

All changes are in the `console/` subproject. The engine (`project(":")`) stays
a pure Gatling compiler; the only engine change is one additive method on
`RunSummaryGenerator` (see Ingestion).

### Storage

Embedded **H2** in file mode via **Quarkus Hibernate ORM with Panache**.

- New dependencies in `console/build.gradle.kts`:
  `io.quarkus:quarkus-hibernate-orm-panache`, `io.quarkus:quarkus-jdbc-h2`.
- Datasource configured in `application.properties`; the DB file path resolves
  relative to the repo root (consistent with the existing `PathResolver`
  pattern), default `<root>/data/rampage.mv.db`, overridable via
  `rampage.console.results-db`.
- `quarkus.hibernate-orm.database.generation=update` — safe schema evolution for
  a single-file embedded DB.

### Entities

New package `io.rampage.console.results`.

**`StoredRun`** (`@Entity`, Panache):

| field | type | source |
|---|---|---|
| `id` | String (PK) | orchestrator run UUID, or `imported-<simDir>` for backfill |
| `name` | String | `run-metadata.json` `runName`, else run YAML basename |
| `environmentPath` / `runPath` | String | `RunRecord.queued()` / metadata |
| `environmentId` | String | `run-metadata.json` `environment` |
| `runConfigKey` | String | derived `envPath::runPath` — groups runs for trends |
| `status` | enum `COMPLETED` / `FAILED` / `KILLED` | `RunRecord.status()` / inferred |
| `startedAt` / `finishedAt` | Instant | `RunRecord`, else sim-dir mtime |
| `exitCode` | Integer | `RunRecord.exitCode()` |
| `gitCommit` / `gitBranch` | String | `run-metadata.json` |
| `simulationDir` | String | sim dir name — links to the Gatling report |
| `assertionsOk` | Boolean | `RunSummaryGenerator` status `PASS`/`FAIL` |
| `source` | enum `CONSOLE` / `IMPORTED` | how it was ingested |
| `notes` | String (nullable) | user annotation |
| `tags` | `Set<String>` `@ElementCollection` | user tags |
| `scenarioStats` | `List<ScenarioStat>` `@OneToMany(cascade)` | parsed metrics |

**`ScenarioStat`** (`@Entity`):
`id` (generated), `@ManyToOne StoredRun run`, `scenarioName` (the Gatling
stats-table request name), `scenarioId` (from `run-metadata.json` where
available), `requestCount`, `okCount`, `koCount`, `errorPercent`, `meanMs`,
`p50Ms`, `p75Ms`, `p95Ms`, `p99Ms`, `maxMs`, `requestsPerSecond`.

### Ingestion — `RunResultIngestor` (`@ApplicationScoped`)

Two entry points, both transactional and idempotent (skip when a `StoredRun`
with that id / `simulationDir` already exists):

1. **On completion** — called from `RunOrchestrator.runOne()` immediately after
   the terminal `transitionTo(...)` (around line 273). The orchestrator is
   single-slot, so once a run finishes the newest `rampagesimulation-*`
   directory under `build/reports/gatling/` is unambiguously that run's output.
   The ingestor resolves that sim dir, reads `run-metadata.json` (Jackson) for
   run identity, calls the engine's `RunSummaryGenerator` to parse per-request
   stats and assertion outcomes, and maps the result to a `StoredRun` +
   `ScenarioStat` rows keyed on `RunRecord.id()`, `source=CONSOLE`.
2. **Backfill scan** — `importFromFilesystem()`, run on `@Observes StartupEvent`
   and via a `POST /history/rescan` endpoint. Walks `build/reports/gatling/`
   (the directory `RunHistoryService` scans today) and, for every sim dir not
   already stored, ingests it with a synthetic id `imported-<simDir>` and
   `source=IMPORTED`.

**Engine touch-point:** `RunSummaryGenerator` currently exposes only
`generate(reportRoot, outputFile)` (writes JSON for the *latest* sim dir) and
package-private parse helpers, which the console cannot reach across packages.
Add one additive public method — `static Map<String,Object> summarise(Path
simulationDir)` — that parses a *specific* sim dir's `index.html` and returns
the map without writing a file. `generate(...)` is refactored to delegate to it.
No behaviour change for existing callers.

### Orchestrator wiring

`RunOrchestrator` gains a `RunResultIngestor` collaborator via CDI injection and
calls `ingestor.ingestCompleted(record)` after the terminal transition.
Ingestion failures are caught and logged *inside the ingestor* — a parse error
must never mark a run `FAILED` or stall the queue. The orchestrator's
package-private test constructor is extended to accept the ingestor (a no-op
ingestor is acceptable in tests that do not exercise persistence).

### UI — web layer

`HistoryResource` is extended; new Qute templates under `templates/History/`.
HTMX as today.

- **`GET /history`** — DB-backed list. Query params `q` (free-text over
  name/env/commit), `tag`, `status`. Table columns: name, environment, status
  badge, started, P95 (worst scenario), error %, tags, report link. Filters
  re-issue via `hx-get` for snappy filtering.
- **`POST /history/{id}/tags`** and **`DELETE /history/{id}/tags/{tag}`** —
  add/remove a tag; HTMX swaps the row's tag cell.
- **`POST /history/{id}/notes`** — save the annotation.
- **`GET /history/{id}`** — run detail: metadata, per-scenario `ScenarioStat`
  table, tags, notes, Gatling report link.
- **`GET /history/compare?a=<id>&b=<id>`** — comparison. A new
  `RunComparisonService` joins the two runs' `ScenarioStat` rows by
  `scenarioName`, computes deltas and %-change for count/RPS/error%/P50/P95/P99/
  mean, and flags regressions. Delta-direction and threshold conventions are
  lifted from the engine's existing `RunSummaryComparator` (latency: a positive
  Δ is bad). Rows present on only one side are marked added/removed.
- **`GET /history/trends?runConfigKey=<key>`** — selects all `StoredRun` rows
  with that `runConfigKey`, ordered by `startedAt`, and charts P95, RPS and
  error rate over time.
- **`POST /history/rescan`** — triggers `importFromFilesystem()`, returns to the
  list.

### Charts — uPlot

The console ships no JavaScript except HTMX (loaded from unpkg). The trends view
adds **uPlot** (~40 KB, time-series focused), loaded the same way from unpkg,
plus a small inline init script that reads a `<script type="application/json">`
data island rendered by Qute. uPlot is initialised on `htmx:load` so it survives
HTMX navigation. No npm, no build tooling.

(Alternative considered: server-rendered SVG sparklines for a strict no-JS
stance — rejected because interactive hover/zoom on trend charts is core to the
"LoadRunner feel" and uPlot is tiny.)

### `RunHistoryService` migration

`RunHistoryService.listRecent(...)` is reimplemented to query `StoredRun`
instead of scanning the filesystem. `resolveReport(...)` (the path-safety logic
for the `/reports` static handler) stays unchanged. The filesystem-scan logic
moves into `RunResultIngestor.importFromFilesystem()`. `RunHistoryEntry` is
retired; templates render `StoredRun` directly.

## Data flow

```
console run ──RunOrchestrator.runOne()──▶ terminal transition
                                             │
                                             ▼
                          RunResultIngestor.ingestCompleted(record)
                             │  newest rampagesimulation-* dir
                             │  run-metadata.json + RunSummaryGenerator.summarise()
                             ▼
                        StoredRun + ScenarioStat ──▶ H2 (data/rampage.mv.db)
                                                         │
CLI-only run ──build/reports/gatling/── StartupEvent ────┘
              importFromFilesystem()                     │
                                                         ▼
                     /history   /history/{id}   /history/compare   /history/trends
```

## Error handling

- Ingestion is best-effort and isolated: any failure (missing
  `run-metadata.json`, unparsable report, a `KILLED` run with no report) is
  caught, logged at WARN, and the run is still stored with whatever fields are
  available — `scenarioStats` may be empty.
- Idempotency: re-ingesting an already-stored id / `simulationDir` is a no-op,
  so the startup backfill and the per-run hook never duplicate a run.
- A corrupt H2 file surfaces as a loud Quarkus startup failure — acceptable.
- `/reports` continues to go through `RunHistoryService.resolveReport`'s
  existing path-containment check.

## Testing (TDD)

- `RunResultIngestorTest` — a fixture report directory (a captured
  `rampagesimulation-*` dir with `index.html` + `run-metadata.json` under
  `src/test/resources`) → assert the `StoredRun` + `ScenarioStat` rows; assert
  idempotency; assert a `KILLED`-with-no-report run still stores.
- `RunComparisonServiceTest` — two `StoredRun` fixtures → assert deltas,
  %-change, regression flags, added/removed rows.
- `StoredRunRepositoryTest` (`@QuarkusTest`, H2 in-memory) — persist/query, tag
  add/remove, `runConfigKey` grouping, free-text filter.
- `RunSummaryGeneratorTest` (engine) — the new `summarise(Path)` overload parses
  a specific sim dir.
- Playwright e2e (`@Tag("e2e")`) — filter history by tag, open a run detail,
  run a comparison. Joins the existing `:console:e2eTest` suite.

## Build / config changes

- `console/build.gradle.kts` — add `quarkus-hibernate-orm-panache`,
  `quarkus-jdbc-h2`.
- `console/src/main/resources/application.properties` — H2 datasource
  (`quarkus.datasource.db-kind=h2`, JDBC URL at the resolved file path),
  `quarkus.hibernate-orm.database.generation=update`; document the
  `rampage.console.results-db` override alongside the existing path overrides.
- `.gitignore` — ignore `data/*.mv.db` (local state, not committed).
- Engine `RunSummaryGenerator` — additive `summarise(Path)` method.

## Roadmap documentation

`src/docs/modules/ROOT/pages/reference/roadmap.adoc` gains a new **"Platform
evolution"** section after the milestones, covering the six platform themes —
results store, trend analytics, live-dashboard depth, test management,
integrations, engine gaps — with impact/effort notes, and marking the results
store as the milestone in progress. Edited via the `asciidoc-antora-writer`
agent and validated with `gradle21w antora`.

## Verification

1. `gradle21w build test` — engine + console unit tests green.
2. `gradle21w :console:quarkusDev`, open `http://localhost:8090/history` — the
   startup backfill has imported existing `build/reports/gatling/` runs.
3. Launch a run from the dashboard; on completion it appears in `/history` with
   `source=CONSOLE` and P95 / error % populated.
4. Add a tag and filter by it; open `/history/{id}`; compare two runs at
   `/history/compare`; open `/history/trends` for a config with ≥2 runs and
   confirm the chart renders.
5. Restart the console — history survives (the DB file persists).
6. `gradle21w :console:e2eTest` — Playwright suite green.
7. `gradle21w antora` — docs build clean.
