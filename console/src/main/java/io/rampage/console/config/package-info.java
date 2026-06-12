/**
 * Browsing, editing, and validating Rampage configuration files from the console.
 *
 * <p>{@code ConfigBrowser} lists the environment, run, and scenario YAML files under
 * the configured config root, {@code ConfigEditor} reads and saves them with
 * validation ({@code ValidationResult}), and {@code ConfigKind} classifies entries
 * ({@code ConfigEntry}) by directory. {@code PathResolver} locates the repository
 * root so paths work in both dev mode and packaged runs.</p>
 */
package io.rampage.console.config;
