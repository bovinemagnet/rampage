/**
 * Run reporting and metadata artefacts written alongside Gatling results.
 *
 * <p>Writers in this package produce machine-readable and human-readable artefacts for
 * each run: run metadata ({@code RunMetadataWriter}, promoted into the results
 * directory by {@code RunMetadataPromoter}), redacted configuration snapshots
 * ({@code ConfigSnapshotWriter}), dry-run summaries ({@code DryRunSummaryWriter}),
 * and post-run summaries with comparison support ({@code RunSummaryGenerator},
 * {@code RunSummaryComparator}, {@code RunSummaryMain}).</p>
 */
package io.rampage.reporting;
