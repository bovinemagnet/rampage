package io.rampage.console.orchestrator;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Strategy for launching an operating-system process.
 *
 * <p>Implementations are responsible for constructing and starting a child
 * process from the supplied command tokens and working directory.  The default
 * production implementation is {@code DefaultProcessLauncher}; tests may
 * substitute a stub via
 * {@code RunOrchestrator#setProcessLauncher(ProcessLauncher)}.</p>
 */
@FunctionalInterface
public interface ProcessLauncher {

    /**
     * Launches a new child process.
     *
     * @param command    the command tokens (executable followed by arguments);
     *                   must not be {@code null} or empty
     * @param workingDir the working directory for the child process;
     *                   must not be {@code null}
     * @return the started {@link Process}; never {@code null}
     * @throws IOException if the process cannot be started
     */
    Process launch(List<String> command, Path workingDir) throws IOException;
}
