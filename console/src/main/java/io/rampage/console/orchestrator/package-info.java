/**
 * Queueing, launching, and tracking Gatling run processes.
 *
 * <p>{@code RunOrchestrator} accepts queued runs ({@code QueuedRun}), launches them one
 * at a time through a {@code ProcessLauncher} (defaulting to
 * {@code DefaultProcessLauncher}), and tracks each as a mutable {@code RunRecord}
 * progressing through the {@code RunStatus} lifecycle. Status changes are published as
 * {@code RunStatusEvent} records via {@code RunStatusBroadcaster}, and
 * {@code OrchestratorView} summarises the active run and queue for the dashboard.</p>
 */
package io.rampage.console.orchestrator;
