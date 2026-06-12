package io.rampage.console.orchestrator;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Default {@link ProcessLauncher} implementation that delegates to
 * {@code ProcessBuilder}, merging stderr into stdout for a single unified
 * output stream.
 */
public final class DefaultProcessLauncher implements ProcessLauncher {

    /**
     * Constructs a new {@code DefaultProcessLauncher}.
     */
    public DefaultProcessLauncher() {
    }

    /**
     * {@inheritDoc}
     *
     * <p>Stderr is redirected into the process's stdout stream
     * ({@code redirectErrorStream(true)}), so the caller reads a single
     * combined output stream.</p>
     */
    @Override
    public Process launch(List<String> command, Path workingDir) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(command)
                .directory(workingDir.toFile())
                .redirectErrorStream(true);
        return pb.start();
    }
}
