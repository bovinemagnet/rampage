package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Execution settings for a run, including the execution mode and run-level workload.
 *
 * <p>Bound to the {@code execution} key in {@code run.yaml}. The {@code workload}
 * defined here is used as the default for all scenarios unless a scenario overrides
 * it with {@code inheritFromRun: false}.</p>
 */
public class ExecutionConfig {
    @JsonProperty("mode")
    private String mode;

    @JsonProperty("workload")
    private WorkloadConfig workload;

    /**
     * Constructs an {@code ExecutionConfig} with all fields at their default values.
     */
    public ExecutionConfig() {}

    /**
     * Returns the execution mode for the run (for example, {@code "sequential"} or {@code "parallel"}).
     *
     * @return the execution mode, or {@code null} if not configured
     */
    public String getMode() { return mode; }

    /**
     * Sets the execution mode for the run.
     *
     * @param mode the execution mode to use
     */
    public void setMode(String mode) { this.mode = mode; }

    /**
     * Returns the run-level workload configuration applied to scenarios by default.
     *
     * @return the workload configuration, or {@code null} if not configured
     */
    public WorkloadConfig getWorkload() { return workload; }

    /**
     * Sets the run-level workload configuration.
     *
     * @param workload the workload configuration to use
     */
    public void setWorkload(WorkloadConfig workload) { this.workload = workload; }
}
