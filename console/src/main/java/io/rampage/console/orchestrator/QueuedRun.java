package io.rampage.console.orchestrator;

import java.time.Instant;
import java.util.UUID;

public record QueuedRun(
        String id,
        String envPath,
        String runPath,
        Instant queuedAt) {

    public static QueuedRun create(String envPath, String runPath) {
        return new QueuedRun(UUID.randomUUID().toString(), envPath, runPath, Instant.now());
    }
}
