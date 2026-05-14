# F-022 — Token refresh for long-running tests

**Milestone:** M3 — Multi-scenario, Multi-environment
**PRD references:** SEC-006, Post-MVP-AC-4

## Summary

Tokens captured at simulation init may expire during long soak or stress runs. Every request after expiry then receives a `401` and reports a spurious failure unrelated to the SUT.

## Acceptance Criteria

- [ ] When the token provider reports an expiry (`expires_in` in OAuth response or a static TTL configured via `SecurityConfig.token.ttlSeconds`), the framework refreshes the token before expiry.
- [ ] Refresh happens in a background scheduled executor; refresh failures are logged as ERROR and trigger a configurable behaviour: `onRefreshFailure: continue|stop` (default `continue`).
- [ ] Once refreshed, subsequent Gatling requests pick up the new token via session attribute or a builder-level header function.
- [ ] Tests cover: refresh before expiry, refresh failure with `continue`, refresh failure with `stop`.

## Implementation Notes

- Gatling builders are constructed once; the `Authorization` header value must be a function of the current token, not a captured constant. Use a session attribute `authToken` populated by a background task into the Gatling session, or use Gatling EL with a server-side variable.
- A simpler model: each scenario starts with an `exec` step that reads the current token from a shared `AtomicReference<String>` and writes it into the session. Header becomes `Authorization: Bearer #{authToken}`.

## Out of scope

- Per-VU tokens (different users with different tokens). Out of scope; token is a framework-level credential.

## Suggested labels

`area:security`, `type:feature`, `milestone:M3`
