package io.rampage.console.orchestrator;

/**
 * Lifecycle states of a Gatling load-test run managed by the orchestrator.
 *
 * <p>Permitted state transitions are enforced by
 * {@code RunRecord#transitionTo(RunStatus)}.</p>
 */
public enum RunStatus {

    /** The run has been accepted and is waiting for the execution slot to become free. */
    QUEUED,

    /** The Gatling JVM process has been started and is executing. */
    RUNNING,

    /** The Gatling process exited with code {@code 0}. */
    COMPLETED,

    /** The Gatling process exited with a non-zero exit code. */
    FAILED,

    /** The run was terminated by an explicit kill request before or during execution. */
    KILLED
}
