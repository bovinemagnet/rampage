# F-002 — Fail-fast secret resolution

**Milestone:** M1 — MVP Honesty
**PRD references:** SEC-007, MVP-AC-7, §18 rule 5
**Traces to:** D8 in `docs/review/code-review.md`

## Summary

`SecretResolver` currently logs a warning and returns `""` when an env-var-sourced credential is missing. A missing `API_TOKEN` therefore produces `Authorization: Bearer ` (literal empty token) and the load test runs against an unauthenticated endpoint, or worse, against an endpoint that accepts it silently. PRD §18 requires this to be a hard fail before traffic starts.

## Acceptance Criteria

- [x] When a `CredentialConfig` or `TokenConfig` declares `source: env` and the named env var is unset, `SecretResolver` throws a checked-or-runtime exception carrying the env var name and the config path (e.g. `environment.security.token`).
- [x] The exception is surfaced by `ConfigValidator` so `validateLoadTest` exits non-zero with a list of unresolved secrets.
- [x] `RampageSimulation` initialisation halts before any Gatling injection step is configured.
- [x] `secret-manager` source remains a stub but throws if `secretPath` is unset.
- [x] A new optional field `required: false` on `CredentialConfig` / `TokenConfig` allows callers to opt out of fail-fast (used for optional headers).
- [x] Tests cover: missing env var with `required=true`, missing env var with `required=false`, unset `envVar` field, `secret-manager` stub.

## Status

Complete. The structured `resolveCredential` / `resolveToken` paths fail fast, `ConfigValidator`
aggregates unresolved secrets, and the `required` flag (default `true`) gates the behaviour.

Two refinements were made when closing this out:

- **Inline `ENV:` references now also fail fast.** `SecretResolver.resolve("ENV:NAME")` previously
  returned `""` with only a warning when the variable was unset. It now throws `SecretResolutionException`,
  closing the last silent-empty path. (`${env:NAME}` placeholders already failed fast via
  `PlaceholderSubstitutor`.)
- **Shipped default vs local-dev divergence.** Per the implementation notes, the shipped classpath
  default `src/gatling/resources/environment.yaml` marks token and database credentials `required: true`.
  `config/environments/local.yaml` is **deliberately kept `required: false`** (with an explanatory comment)
  so unauthenticated local development against `localhost` does not hard-fail; real environments are
  expected to set `required: true`.

## Implementation Notes

- Add a `required` flag (default `true`) to both `CredentialConfig` and `TokenConfig`.
- Add a `SecretResolutionException` that lists all unresolved references (so all missing secrets are reported in one error, matching `ConfigValidator`'s aggregation style).
- Wire the resolution check into `ConfigValidator.validate` so `validateLoadTest` and `gatlingRun` agree.
- Update `config/environments/local.yaml` and `src/gatling/resources/environment.yaml` to mark token/db credentials as `required: true`.

## Out of scope

- Real Secret Manager / Vault integration (later milestone).
- Token refresh (F-022).

## Suggested labels

`area:security`, `priority:high`, `type:bug`, `milestone:M1`
