/**
 * Zero-config self-test support for the console.
 *
 * <p>{@code VerificationConfig} stages a bundled environment, run, scenario, and
 * GraphQL query from the classpath onto disk at startup, so the dashboard's Verify
 * button can enqueue a run against the console's own sample endpoints and prove the
 * full pipeline works.</p>
 */
package io.rampage.console.verification;
