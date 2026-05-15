package io.rampage.console.orchestrator;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@FunctionalInterface
public interface ProcessLauncher {
    Process launch(List<String> command, Path workingDir) throws IOException;
}
