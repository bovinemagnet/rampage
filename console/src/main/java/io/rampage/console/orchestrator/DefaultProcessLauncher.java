package io.rampage.console.orchestrator;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class DefaultProcessLauncher implements ProcessLauncher {

    @Override
    public Process launch(List<String> command, Path workingDir) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(command)
                .directory(workingDir.toFile())
                .redirectErrorStream(true);
        return pb.start();
    }
}
