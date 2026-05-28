package io.rampage.reporting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Moves run-metadata.json and config-snapshot.json from the Gatling reports root
 * into the simulation directory that was just created by Gatling. The simulation
 * writes these files at known root-level paths during {@code before()} because
 * the rampagesimulation-* directory name is not known until Gatling generates
 * its report. This class runs as a post-step (from {@code gatlingRun.doLast}) to
 * place each file inside the correct directory where the console ingestor reads it.
 */
public class RunMetadataPromoter {

    private static final Logger log = LoggerFactory.getLogger(RunMetadataPromoter.class);

    private static final List<String> FILES_TO_PROMOTE = List.of(
        "run-metadata.json", "config-snapshot.yaml");

    public void promote(Path reportsRoot) {
        if (reportsRoot == null || !Files.isDirectory(reportsRoot)) {
            log.debug("Reports root {} does not exist; skipping metadata promotion", reportsRoot);
            return;
        }

        Optional<Path> targetDir = newestSimulationDir(reportsRoot);
        if (targetDir.isEmpty()) {
            log.debug("No rampagesimulation-* directory found under {}; leaving staged files in place",
                reportsRoot);
            return;
        }

        Path destination = targetDir.get();
        for (String fileName : FILES_TO_PROMOTE) {
            Path source = reportsRoot.resolve(fileName);
            if (!Files.isRegularFile(source)) continue;
            Path target = destination.resolve(fileName);
            try {
                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
                log.info("Promoted {} into {}", fileName, destination.getFileName());
            } catch (IOException e) {
                log.warn("Failed to promote {} into {}: {}", fileName, destination, e.getMessage());
            }
        }
    }

    private Optional<Path> newestSimulationDir(Path reportsRoot) {
        try (Stream<Path> stream = Files.list(reportsRoot)) {
            return stream
                .filter(Files::isDirectory)
                .filter(p -> p.getFileName().toString().startsWith("rampagesimulation-"))
                .filter(p -> Files.isRegularFile(p.resolve("index.html")))
                .max(Comparator.comparing(p -> p.getFileName().toString()));
        } catch (IOException e) {
            log.warn("Failed to list {}: {}", reportsRoot, e.getMessage());
            return Optional.empty();
        }
    }

    public static void main(String[] args) {
        if (args.length < 1 || args[0].isBlank()) {
            System.err.println("Usage: RunMetadataPromoter <reportsRoot>");
            System.exit(2);
        }
        new RunMetadataPromoter().promote(Path.of(args[0]));
    }
}
