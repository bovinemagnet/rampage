package io.rampage.console.orchestrator;

import java.util.List;

/**
 * Render-friendly snapshot of the orchestrator state used by the queue panel
 * template. {@code active} is {@code null} when nothing is currently running.
 *
 * @param active the currently executing run, or {@code null} if the slot is idle
 * @param queue  the ordered list of runs waiting to be executed; never {@code null}
 */
public record OrchestratorView(RunRecord active, List<RunRecord> queue) {

    /**
     * Returns {@code true} if there is a run currently executing.
     *
     * @return {@code true} when {@code active} is non-null
     */
    public boolean hasActive() {
        return active != null;
    }

    /**
     * Returns {@code true} if there is at least one run waiting in the queue.
     *
     * @return {@code true} when the queue is non-empty
     */
    public boolean hasQueue() {
        return !queue.isEmpty();
    }
}
