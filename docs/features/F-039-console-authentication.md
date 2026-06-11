# F-039 — Console authentication

**Milestone:** Platform evolution — Theme 4 (Test management)
**PRD references:** §13 (security posture, by extension)
**Traces to:** 2026-06 codebase review

## Summary

The web console has no authentication on any endpoint. It can edit configuration files on disk (`ConfigEditor`), browse the config tree, launch Gradle/Gatling processes (`RunOrchestrator`), and serve report files. Anyone with network access to the console port has all of those capabilities. Path traversal and command injection are already defended, but authorisation is absent entirely.

## Acceptance Criteria

- [ ] All console HTTP endpoints (REST resources, SSE streams, static dashboard) require authentication.
- [ ] Quarkus OIDC is supported for organisations with an identity provider; HTTP Basic (single configured user) is available as a zero-infrastructure fallback.
- [ ] Authentication mode is selected via `application.properties`; the default for a fresh checkout remains "open" only when explicitly set (e.g. `rampage.console.auth=none`) so local development is not broken silently.
- [ ] Unauthenticated requests receive 401 without leaking config or run data.
- [ ] SSE streams (`StreamResource`) enforce the same policy as the REST resources.
- [ ] Documented in the Antora console pages, including a warning about running the console unauthenticated on shared networks.

## Implementation Notes

- Quarkus security extensions: `quarkus-oidc` and `quarkus-elytron-security-properties-file` (or `quarkus-security-jpa` if user storage is wanted later — out of scope here).
- The console launches processes and writes files; treat it as an admin tool — a single `admin` role is sufficient, no fine-grained roles needed yet.

## Out of scope

- Multi-user accounts, role hierarchies, audit logging.
- TLS termination (deployment concern).

## Suggested labels

`area:security`, `type:enhancement`, `priority:high`
