/**
 * Jackson-bound model classes for the three layers of Rampage YAML configuration.
 *
 * <p>The layers are loaded and validated together, then compiled into Gatling concepts
 * by the {@code io.rampage.factory} package:</p>
 *
 * <ul>
 *   <li>{@code environment.yaml} ({@code EnvironmentConfig}) — the target system: base
 *       URLs, HTTP defaults, security, databases, observability, and safety flags.</li>
 *   <li>{@code run.yaml} ({@code RunConfig}) — what to execute: scenario references,
 *       execution mode and workload, global assertions, and reporting.</li>
 *   <li>{@code scenarios/*.yaml} ({@code ScenarioConfig}) — per-scenario requests,
 *       checks, feeders, and optional workload overrides.</li>
 * </ul>
 *
 * <p>All classes are JavaBean-style with no-argument constructors; unknown YAML keys
 * are silently ignored during binding.</p>
 */
package io.rampage.config.model;
