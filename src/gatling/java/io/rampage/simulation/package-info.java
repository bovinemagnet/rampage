/**
 * The generic Gatling simulation executed by the Gatling Gradle plugin.
 *
 * <p>{@code RampageSimulation} contains no scenario-specific code; its initialiser
 * block loads the YAML configuration layers, validates them, and assembles protocols,
 * scenarios, feeders, workloads, and assertions via the {@code io.rampage.factory}
 * package. All behaviour comes from the configuration files.</p>
 */
package io.rampage.simulation;
