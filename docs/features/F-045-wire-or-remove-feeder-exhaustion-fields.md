# F-045 — Wire or remove `feeder.onExhaustion`

**Milestone:** Platform evolution — Theme 6 (Engine-level gaps)
**PRD references:** §12, FDR-004
**Traces to:** 2026-06 codebase review; F-023 acceptance criteria

## Summary

`FeederConfig` models `onExhaustion` (default `stop`), and F-023 described its semantics, but nothing in the engine reads the field. It is dead configuration: a user setting `onExhaustion: fail` gets no behaviour change and no error. The validator logs a warning when a non-default value is set; this brief is the follow-through.

(`onMissingRequired` was originally in scope here but is in fact honoured by `FeederFactory` in both preload and streaming modes — `fail` unless `skip` — and is now enum-validated by `ConfigValidator`.)

## Acceptance Criteria

Either wire it:

- [ ] `onExhaustion: stop` — with `strategy: queue`, the scenario stops emitting requests when rows run out (current Gatling queue behaviour); `onExhaustion: fail` — the run fails with a clear message.
- [ ] Validator accepts only the supported values; the "ignored field" warning is removed.
- [ ] The corresponding F-023 acceptance bullets are ticked.

Or remove it:

- [ ] Field deleted from `FeederConfig`, schema docs regenerated, any sample YAML cleaned up, and the F-023 brief annotated.

Decide which path when picking the issue up; wiring is preferred if `strategy: queue` sees real use.

## Implementation Notes

- Gatling's `queue()` strategy already stops the injector when exhausted; "fail" needs a row-count vs expected-iterations guard, which may only be approximable — document the limitation if so.

## Out of scope

- New exhaustion strategies (recycle-with-warning, etc.).

## Suggested labels

`area:feeder`, `type:enhancement`, `priority:low`
