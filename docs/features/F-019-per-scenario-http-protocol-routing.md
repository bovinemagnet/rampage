# F-019 — Per-scenario HTTP protocol routing

**Milestone:** M3 — Multi-scenario, Multi-environment
**PRD references:** Post-MVP-AC-1
**Traces to:** D3 in `docs/review/code-review.md`

## Summary

`RampageSimulation` builds one `HttpProtocolBuilder` from the **first** scenario's `endpointRef` and applies it to all scenarios via `.protocols(httpProtocol)`. When scenarios target different base URLs, all but the first are routed incorrectly.

## Acceptance Criteria

- [ ] Each `PopulationBuilder` may be associated with its own `HttpProtocolBuilder` derived from its scenario's `endpointRef`.
- [ ] In Gatling Java DSL: `population.protocols(scenarioProtocol)` (per-population), not `setUp(...).protocols(...)`.
- [ ] Tests cover a two-scenario run with different `baseUrls`.
- [ ] Documentation: a single shared protocol is still emitted when all scenarios share the same `endpointRef` (small optimisation, not required).

## Implementation Notes

- Cache `HttpProtocolBuilder` per `endpointRef` so two scenarios with the same ref share the builder.
- Authorization header injection happens once per protocol, not per scenario — preserve that.

## Out of scope

- Per-scenario auth credentials (would require multiple `SecurityConfig` entries).

## Suggested labels

`area:simulation`, `type:bug`, `milestone:M3`
