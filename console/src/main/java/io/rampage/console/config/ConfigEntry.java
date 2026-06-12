package io.rampage.console.config;

/**
 * Lightweight descriptor for a single YAML config file discovered under the
 * config root by {@code ConfigBrowser}.
 *
 * @param name         the filename (e.g. {@code local.yaml}).
 * @param relativePath the path relative to the config root (e.g. {@code environments/local.yaml}).
 * @param absolutePath the absolute filesystem path to the file.
 */
public record ConfigEntry(String name, String relativePath, String absolutePath) {}
