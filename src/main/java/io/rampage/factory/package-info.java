/**
 * Factories that compile Rampage YAML configuration into runnable Gatling concepts.
 *
 * <p>This package is the heart of the runtime engine. {@code ConfigLoader} parses the
 * YAML layers, {@code ConfigValidator} aggregates validation errors across all of them,
 * and the remaining factories each translate one aspect of the configuration:</p>
 *
 * <ul>
 *   <li>{@code HttpProtocolFactory} — base URL selection, bearer auth, and
 *       observability headers.</li>
 *   <li>{@code ScenarioFactory} — GraphQL request scenarios with feeder placeholder
 *       rewriting.</li>
 *   <li>{@code WorkloadFactory} — injection profiles (smoke, constant, soak,
 *       ramp-and-hold).</li>
 *   <li>{@code FeederFactory} — SQL-backed feeders preloaded into memory.</li>
 *   <li>{@code SecretResolver} and the token providers — credential and bearer-token
 *       resolution.</li>
 * </ul>
 *
 * <p>The canonical wiring of these factories is the initialiser block of
 * {@code io.rampage.simulation.RampageSimulation}.</p>
 */
package io.rampage.factory;
