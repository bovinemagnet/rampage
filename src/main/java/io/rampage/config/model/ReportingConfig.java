package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configuration controlling how the simulation report is written.
 *
 * <p>Bound to the {@code reporting} key in {@code run.yaml}.
 * Defaults: {@code writeRunMetadata} = {@code false}, {@code redactSecrets} = {@code true},
 * {@code includeConfigSnapshot} = {@code false}.</p>
 */
public class ReportingConfig {
    @JsonProperty("outputDirectory")
    private String outputDirectory;

    @JsonProperty("writeRunMetadata")
    private boolean writeRunMetadata = false;

    @JsonProperty("redactSecrets")
    private boolean redactSecrets = true;

    @JsonProperty("includeConfigSnapshot")
    private boolean includeConfigSnapshot = false;

    /**
     * Constructs a {@code ReportingConfig} with default flag values.
     */
    public ReportingConfig() {}

    /**
     * Returns the filesystem path to the directory where the Gatling report is written.
     *
     * @return the output directory path, or {@code null} if not configured
     */
    public String getOutputDirectory() { return outputDirectory; }

    /**
     * Sets the filesystem path to the directory where the Gatling report is written.
     * Bound to the {@code outputDirectory} key.
     *
     * @param outputDirectory the output directory path
     */
    public void setOutputDirectory(String outputDirectory) { this.outputDirectory = outputDirectory; }

    /**
     * Returns whether run metadata (owner, application, service, change reference) is written into the report.
     *
     * @return {@code true} if run metadata is included in the report; {@code false} by default
     */
    public boolean isWriteRunMetadata() { return writeRunMetadata; }

    /**
     * Sets whether run metadata is written into the report.
     * Bound to the {@code writeRunMetadata} key.
     *
     * @param writeRunMetadata {@code true} to include run metadata in the report
     */
    public void setWriteRunMetadata(boolean writeRunMetadata) { this.writeRunMetadata = writeRunMetadata; }

    /**
     * Returns whether secret values are redacted before being written to the report.
     *
     * @return {@code true} if secrets are redacted; {@code true} by default
     */
    public boolean isRedactSecrets() { return redactSecrets; }

    /**
     * Sets whether secret values are redacted before being written to the report.
     * Bound to the {@code redactSecrets} key.
     *
     * @param redactSecrets {@code true} to redact secret values
     */
    public void setRedactSecrets(boolean redactSecrets) { this.redactSecrets = redactSecrets; }

    /**
     * Returns whether a snapshot of the resolved configuration is included in the report output.
     *
     * @return {@code true} if the config snapshot is included; {@code false} by default
     */
    public boolean isIncludeConfigSnapshot() { return includeConfigSnapshot; }

    /**
     * Sets whether a snapshot of the resolved configuration is included in the report output.
     * Bound to the {@code includeConfigSnapshot} key.
     *
     * @param includeConfigSnapshot {@code true} to include the configuration snapshot
     */
    public void setIncludeConfigSnapshot(boolean includeConfigSnapshot) { this.includeConfigSnapshot = includeConfigSnapshot; }
}
