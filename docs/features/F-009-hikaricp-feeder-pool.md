# F-009 — HikariCP-backed JDBC feeder pool

**Milestone:** M2 — Operational Readiness
**PRD references:** §4 (Recommended Technology Stack), ENV-004
**Traces to:** D7 in `docs/review/code-review.md`

## Summary

HikariCP is declared in `build.gradle.kts` but `FeederFactory.loadFromSql` uses `DriverManager.getConnection(...)` directly. `PoolConfig` (`maximumPoolSize`, `connectionTimeoutMillis`, `idleTimeoutMillis`) is parsed but never honoured. For preload mode this only matters if many scenarios share a database, but it matters acutely for direct (streaming) JDBC mode in F-024.

## Acceptance Criteria

- [x] `FeederFactory` obtains connections from a `HikariDataSource` constructed from `DatabaseConfig`.
- [x] `PoolConfig.maximumPoolSize`, `connectionTimeoutMillis`, `idleTimeoutMillis` are applied to the Hikari config.
- [x] One `HikariDataSource` per unique `databaseRef`, cached for the simulation lifetime; closed at simulation end.
- [x] Pool stats (active, idle, awaiting) are logged at INFO at simulation start and end.
- [x] Tests cover: pool construction from `DatabaseConfig`, reuse of pool across scenarios, no double-close.

## Implementation Notes

- Add a `DataSourceRegistry` class in `io.rampage.factory` (or `io.rampage.feeder`) that maps `databaseRef -> HikariDataSource`. `FeederFactory` looks the source up by ref.
- Apply Hikari's pool name = `databaseRef` for log clarity.
- Honour `safety` settings: do not open pools for databases referenced only by disabled scenarios.

## Out of scope

- Direct (streaming) JDBC feeder (F-024) — that depends on this.

## Suggested labels

`area:feeder`, `priority:medium`, `type:enhancement`, `milestone:M2`
