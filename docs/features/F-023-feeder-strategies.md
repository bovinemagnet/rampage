# F-023 — Feeder strategies: queue + true random

**Milestone:** M3 — Multi-scenario, Multi-environment
**PRD references:** FDR-004, §12, SCN-005
**Traces to:** D14, SCN-005 row in `docs/review/requirements-traceability.md`

## Summary

PRD §12.3 lists four strategies: `queue`, `shuffle`, `random`, `circular`. Today only `circular` and `random/shuffle` (which is actually shuffle-once-then-circular) are implemented.

## Acceptance Criteria

- [ ] `strategy: queue` — each row is consumed at most once. When the queue drains, the scenario stops emitting requests (or fails, depending on a new `onExhaustion: stop|fail` field; default `stop`). _(not implemented — see F-045)_
- [ ] `strategy: shuffle` — the dataset is shuffled once at preload, then iterated once (queue-like) or circularly (current behaviour), depending on `onExhaustion`. _(not implemented — see F-045)_
- [x] `strategy: random` — each pull selects a random row from the dataset (not "shuffle once").
- [x] `strategy: circular` — unchanged.
- [ ] Tests cover each strategy with `onExhaustion` set to both values. _(not implemented — see F-045)_
- [x] Documented in feature-table form in the Antora docs (F-033 will migrate it).

## Implementation Notes

- Map to Gatling feeder strategies where possible: `feeder.circular()`, `feeder.shuffle()`, `feeder.random()`, `feeder.queue()`. Avoid re-implementing iteration in `FeederFactory` if Gatling already provides it.
- `CircularIterator` in `FeederFactory` is custom-built; verify whether the Gatling helpers will satisfy our preload semantics.

## Out of scope

- Weighted random based on row contents.

## Suggested labels

`area:feeder`, `type:feature`, `milestone:M3`
