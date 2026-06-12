package io.rampage.console.orchestrator;

import java.time.Instant;

/**
 * Immutable event emitted by the {@link RunStatusBroadcaster} each time the
 * lifecycle status of a load-test run changes.
 *
 * @param runId    the unique identifier of the run whose status changed
 * @param envPath  the environment YAML path associated with the run
 * @param runPath  the run YAML path associated with the run
 * @param status   the new {@link RunStatus} of the run
 * @param exitCode the OS process exit code, or {@code null} if the process has
 *                 not yet exited
 * @param at       the instant at which this event was created
 */
public record RunStatusEvent(
        String runId,
        String envPath,
        String runPath,
        RunStatus status,
        Integer exitCode,
        Instant at) {

    /**
     * Creates a {@code RunStatusEvent} from the current state of a
     * {@link RunRecord}, stamping the event with the current instant.
     *
     * @param record the run record to snapshot; must not be {@code null}
     * @return a new {@code RunStatusEvent}; never {@code null}
     */
    public static RunStatusEvent of(RunRecord record) {
        return new RunStatusEvent(
                record.id(),
                record.queued().envPath(),
                record.queued().runPath(),
                record.status(),
                record.exitCode(),
                Instant.now());
    }
}
