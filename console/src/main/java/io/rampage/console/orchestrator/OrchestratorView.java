package io.rampage.console.orchestrator;

import java.util.List;

/**
 * Render-friendly snapshot of the orchestrator state used by the queue panel
 * template. {@code active} is null when nothing is currently running.
 */
public record OrchestratorView(RunRecord active, List<RunRecord> queue) {

    public boolean hasActive() {
        return active != null;
    }

    public boolean hasQueue() {
        return !queue.isEmpty();
    }
}
