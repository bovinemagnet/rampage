# F-040 — Console resource bounds: run queue and process output

**Milestone:** Platform evolution — Theme 4 (Test management)
**PRD references:** —
**Traces to:** 2026-06 codebase review

## Summary

Two unbounded growth paths exist in the console:

1. `RunOrchestrator`'s queue (`ConcurrentLinkedDeque`) accepts enqueued runs without limit. If enqueue rate exceeds execution rate, memory grows linearly and the queue silently becomes hours deep.
2. Process stdout pumping buffers Gatling output lines without a cap. A pathologically chatty run (debug logging enabled, huge error storms) can balloon memory.

## Acceptance Criteria

- [ ] A configurable maximum queue depth (default e.g. 20) with a rejection response: `POST /runs` returns 429 (or 409) with a clear message when the queue is full.
- [ ] Queue depth and rejection count are visible in the orchestrator view.
- [ ] Process stdout retention is capped (line count and/or total bytes, configurable); when the cap is hit, oldest lines are dropped and a marker line records the truncation.
- [ ] Dropped log lines and dropped metrics from the lossy broadcasters are counted and the counts exposed (log line at run completion is sufficient).
- [ ] Unit tests cover queue rejection and stdout truncation.

## Implementation Notes

- `RunOrchestrator.enqueue()` is the single entry point — bound it there under the existing lock.
- For stdout, a bounded ring buffer (e.g. `ArrayDeque` with eviction) in the pump loop is enough; the SSE broadcast path is already lossy by design.

## Out of scope

- Distributed/concurrent injector orchestration (separate Theme 4 capability).

## Suggested labels

`area:tooling`, `type:enhancement`, `priority:medium`
