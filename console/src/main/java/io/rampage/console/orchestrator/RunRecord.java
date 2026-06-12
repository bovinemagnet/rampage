package io.rampage.console.orchestrator;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Mutable runtime record that tracks the lifecycle of a single load-test run
 * from the moment it is queued through to completion or termination.
 *
 * <p>State transitions are enforced by {@link #transitionTo(RunStatus)} using a
 * compare-and-set operation, so the record is safe to read from multiple
 * threads.  Writes to the volatile timing and process fields are performed only
 * by the orchestrator dispatcher thread.</p>
 */
public final class RunRecord {

    private final QueuedRun queued;
    private final AtomicReference<RunStatus> status;
    private volatile Instant startedAt;
    private volatile Instant finishedAt;
    private volatile Integer exitCode;
    private volatile Process process;

    /**
     * Creates a new {@code RunRecord} for the given queued run.
     * The initial status is {@link RunStatus#QUEUED}.
     *
     * @param queued the immutable run parameters; must not be {@code null}
     */
    public RunRecord(QueuedRun queued) {
        this.queued = queued;
        this.status = new AtomicReference<>(RunStatus.QUEUED);
    }

    /**
     * Returns the immutable parameters that were submitted when this run was
     * queued.
     *
     * @return the {@link QueuedRun}; never {@code null}
     */
    public QueuedRun queued() {
        return queued;
    }

    /**
     * Returns the unique identifier of this run, delegating to
     * {@code queued().id()}.
     *
     * @return the run ID; never {@code null}
     */
    public String id() {
        return queued.id();
    }

    /**
     * Returns the current lifecycle status of this run.
     *
     * @return the current {@link RunStatus}; never {@code null}
     */
    public RunStatus status() {
        return status.get();
    }

    /**
     * Attempts to advance the status to {@code next}, subject to the legal
     * transition rules.
     *
     * <p>Legal transitions are:</p>
     * <ul>
     *   <li>{@code QUEUED} &rarr; {@code RUNNING} or {@code KILLED}</li>
     *   <li>{@code RUNNING} &rarr; {@code COMPLETED}, {@code FAILED}, or
     *       {@code KILLED}</li>
     * </ul>
     *
     * @param next the desired target status
     * @return {@code true} if the transition was applied; {@code false} if the
     *         transition is not permitted from the current state, or if another
     *         thread concurrently changed the status
     */
    public boolean transitionTo(RunStatus next) {
        RunStatus current = status.get();
        if (!isLegalTransition(current, next)) {
            return false;
        }
        return status.compareAndSet(current, next);
    }

    /**
     * Returns the instant at which the Gatling process was started, or
     * {@code null} if the run has not yet started.
     *
     * @return the start instant, or {@code null}
     */
    public Instant startedAt() {
        return startedAt;
    }

    /**
     * Records the OS process handle and stamps the start time.
     * Called by the orchestrator dispatcher thread immediately after the
     * process is successfully launched.
     *
     * @param process the running Gatling {@link Process}; must not be
     *                {@code null}
     */
    public void markStarted(Process process) {
        this.process = process;
        this.startedAt = Instant.now();
    }

    /**
     * Returns the instant at which the Gatling process exited, or {@code null}
     * if the run has not yet finished.
     *
     * @return the finish instant, or {@code null}
     */
    public Instant finishedAt() {
        return finishedAt;
    }

    /**
     * Records the exit code and stamps the finish time.
     * Called by the orchestrator dispatcher thread after
     * {@link Process#waitFor()} returns.
     *
     * @param exitCode the OS process exit code
     */
    public void markFinished(int exitCode) {
        this.exitCode = exitCode;
        this.finishedAt = Instant.now();
    }

    /**
     * Returns the OS exit code of the Gatling process, or {@code null} if the
     * process has not yet exited.
     *
     * @return the exit code, or {@code null}
     */
    public Integer exitCode() {
        return exitCode;
    }

    /**
     * Returns the OS process handle for the running Gatling JVM, or
     * {@code null} if the process has not yet been started.
     *
     * @return the {@link Process}, or {@code null}
     */
    public Process process() {
        return process;
    }

    private static boolean isLegalTransition(RunStatus from, RunStatus to) {
        return switch (from) {
            case QUEUED -> to == RunStatus.RUNNING || to == RunStatus.KILLED;
            case RUNNING -> to == RunStatus.COMPLETED || to == RunStatus.FAILED || to == RunStatus.KILLED;
            default -> false;
        };
    }
}
