package io.rampage.console.orchestrator;

import java.time.Instant;

public record RunStatusEvent(
        String runId,
        String envPath,
        String runPath,
        RunStatus status,
        Integer exitCode,
        Instant at) {

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
