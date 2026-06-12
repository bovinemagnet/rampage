package io.rampage.console.orchestrator;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable value object that captures the parameters of a load-test run at the
 * moment it is accepted into the orchestrator queue.
 *
 * @param id        a randomly generated UUID that uniquely identifies this run
 * @param envPath   filesystem or classpath path to the environment YAML file
 * @param runPath   filesystem or classpath path to the run YAML file
 * @param queuedAt  the instant at which the run was placed in the queue
 */
public record QueuedRun(
        String id,
        String envPath,
        String runPath,
        Instant queuedAt) {

    /**
     * Creates a new {@code QueuedRun} with a freshly generated UUID and the
     * current time as the queued instant.
     *
     * @param envPath filesystem or classpath path to the environment YAML file
     * @param runPath filesystem or classpath path to the run YAML file
     * @return a new {@code QueuedRun}; never {@code null}
     */
    public static QueuedRun create(String envPath, String runPath) {
        return new QueuedRun(UUID.randomUUID().toString(), envPath, runPath, Instant.now());
    }
}
