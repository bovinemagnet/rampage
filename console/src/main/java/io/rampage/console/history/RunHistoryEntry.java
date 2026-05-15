package io.rampage.console.history;

import java.time.Instant;

/**
 * One past Gatling run discovered under {@code build/reports/gatling/}.
 *
 * @param simulationDir name of the simulation output directory.
 * @param finishedAt    last-modified time of the directory (proxy for completion).
 * @param reportPath    path the console exposes the {@code index.html} at.
 * @param hasMetadata   whether {@code run-metadata.json} exists alongside the report.
 */
public record RunHistoryEntry(
        String simulationDir,
        Instant finishedAt,
        String reportPath,
        boolean hasMetadata) {}
