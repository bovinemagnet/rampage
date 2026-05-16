package io.rampage.console.orchestrator;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

public final class RunRecord {

    private final QueuedRun queued;
    private final AtomicReference<RunStatus> status;
    private volatile Instant startedAt;
    private volatile Instant finishedAt;
    private volatile Integer exitCode;
    private volatile Process process;

    public RunRecord(QueuedRun queued) {
        this.queued = queued;
        this.status = new AtomicReference<>(RunStatus.QUEUED);
    }

    public QueuedRun queued() {
        return queued;
    }

    public String id() {
        return queued.id();
    }

    public RunStatus status() {
        return status.get();
    }

    public boolean transitionTo(RunStatus next) {
        RunStatus current = status.get();
        if (!isLegalTransition(current, next)) {
            return false;
        }
        return status.compareAndSet(current, next);
    }

    public Instant startedAt() {
        return startedAt;
    }

    public void markStarted(Process process) {
        this.process = process;
        this.startedAt = Instant.now();
    }

    public Instant finishedAt() {
        return finishedAt;
    }

    public void markFinished(int exitCode) {
        this.exitCode = exitCode;
        this.finishedAt = Instant.now();
    }

    public Integer exitCode() {
        return exitCode;
    }

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
