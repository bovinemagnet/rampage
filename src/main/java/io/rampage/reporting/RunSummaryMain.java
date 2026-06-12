package io.rampage.reporting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * CLI entry point invoked by the {@code summariseRun} Gradle task.
 *
 * <p>Reads the latest Gatling report under {@code -Drampage.report.dir=...}
 * (default {@code build/reports/gatling}), writes {@code run-summary.json} next to it,
 * and a Markdown summary to {@code run-summary.md}. If
 * {@code -Drampage.baseline.json=...} is provided and the file exists, the Markdown
 * includes per-request deltas vs. that baseline.
 *
 * <p>The exit code is 0 if all parsed assertions passed, 1 otherwise — so CI can fail
 * the build on regression without re-running Gatling.
 */
public final class RunSummaryMain {

    private static final Logger log = LoggerFactory.getLogger(RunSummaryMain.class);

    private RunSummaryMain() {}

    /**
     * Entry point for the {@code summariseRun} Gradle task. Reads system properties
     * {@code rampage.report.dir} (default {@code build/reports/gatling}),
     * {@code rampage.baseline.json} (optional path to a previous summary), and
     * {@code rampage.fail.on.regression} (default {@code true}) to control behaviour.
     *
     * <p>Exits with code 1 if the run status is not PASS and fail-on-regression is enabled,
     * or with code 2 if summary generation fails entirely.
     *
     * @param args unused command-line arguments; configuration is via system properties
     */
    public static void main(String[] args) {
        Path reportDir = Paths.get(System.getProperty("rampage.report.dir", "build/reports/gatling"));
        Path summaryJson = reportDir.resolve("run-summary.json");
        Path summaryMd = reportDir.resolve("run-summary.md");
        String baselineProp = System.getProperty("rampage.baseline.json");
        Path baseline = (baselineProp != null && !baselineProp.isBlank()) ? Paths.get(baselineProp) : null;
        boolean failOnRegression = Boolean.parseBoolean(
            System.getProperty("rampage.fail.on.regression", "true"));

        try {
            Map<String, Object> summary = RunSummaryGenerator.generate(reportDir, summaryJson);
            String markdown = RunSummaryComparator.renderMarkdown(summaryJson, baseline);
            Files.writeString(summaryMd, markdown);
            log.info("Markdown summary written to {}", summaryMd);

            String status = String.valueOf(summary.get("status"));
            if (!"PASS".equals(status) && failOnRegression) {
                log.error("Run status is {} — exiting non-zero", status);
                System.exit(1);
            }
        } catch (Exception e) {
            log.error("Failed to generate run summary", e);
            System.exit(2);
        }
    }
}
