# F-037 — Gatling plugin upgrade to current stable

**Milestone:** M4 — Reporting, CI, and DX
**PRD references:** §16

## Summary

PRD §16 references Gatling plugin `3.15.0.2`. We are on `3.13.5`. Upgrade is low-risk but worth doing while the surface area is small.

## Acceptance Criteria

- [ ] `build.gradle.kts` sets the Gatling plugin and `gatling-charts-highcharts` to the latest stable version on Maven Central at the time of the upgrade.
- [ ] All existing unit tests pass.
- [ ] A real `gatlingRun` against the sample smoke config succeeds end-to-end.
- [ ] Any deprecation warnings are addressed or documented.

## Implementation Notes

- Check release notes between `3.13.5` and the target version for breaking changes — Java DSL has been stable in this range, but `feed`, `injectClosed`, and `assertion` shapes occasionally shift.
- This sometimes lands first when migrating to newer Java DSL features that depend on it (e.g. specific `incrementUsersPerSec` builders); consider doing this before F-011 if those helpers are needed.

## Out of scope

- Migration to Gatling 4.x (major version) — out of scope for this milestone.

## Suggested labels

`area:build`, `type:chore`, `milestone:M4`
