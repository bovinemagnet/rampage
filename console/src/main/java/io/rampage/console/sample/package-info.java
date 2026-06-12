/**
 * Built-in sample target endpoints for exercising the load-test pipeline.
 *
 * <p>{@code EchoResource} (REST) and {@code EchoGraphQL} (GraphQL) provide
 * predictable local endpoints — echo, delay, failure, and throttle behaviours —
 * so the full Rampage pipeline can be smoke-tested without an external system.
 * {@code EchoPayload} is the shared response shape.</p>
 */
package io.rampage.console.sample;
