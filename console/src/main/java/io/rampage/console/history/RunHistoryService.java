package io.rampage.console.history;

import io.rampage.console.config.PathResolver;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Lists previously completed Gatling runs by scanning {@code build/reports/gatling/}.
 * A directory is considered a finished run if it contains an {@code index.html}
 * (Gatling's stock report file).
 */
@ApplicationScoped
public class RunHistoryService {

    /**
     * Creates a new {@code RunHistoryService} instance.
     * The CDI container calls this constructor; the actual reports directory is
     * resolved in the {@link jakarta.annotation.PostConstruct} {@code init} method.
     */
    public RunHistoryService() {}

    @ConfigProperty(name = "rampage.console.repo-root")
    java.util.Optional<String> repoRootRaw;

    @ConfigProperty(name = "rampage.console.reports-dir")
    java.util.Optional<String> reportsDirOverride;

    private volatile String reportsDir;

    @PostConstruct
    void init() {
        if (reportsDir != null) return;
        reportsDir = reportsDirOverride
                .filter(s -> !s.isBlank())
                .orElseGet(() -> {
                    String start = repoRootRaw.filter(s -> !s.isBlank())
                            .orElseGet(() -> System.getProperty("user.dir"));
                    return PathResolver.resolveRepoRoot(start)
                            .resolve("build/reports/gatling").toString();
                });
    }

    /**
     * Returns every Gatling simulation directory under the reports root that contains
     * an {@code index.html}. Sorted newest first by directory name (the name embeds
     * a fixed-width timestamp).
     *
     * @return an immutable list of simulation directories sorted newest first;
     *         never null, empty when the reports root does not exist.
     */
    public List<Path> scanSimulationDirs() {
        Path root = Paths.get(reportsDir).toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(root)) {
            return stream
                    .filter(Files::isDirectory)
                    .filter(p -> Files.isRegularFile(p.resolve("index.html")))
                    .sorted(Comparator.comparing((Path p) -> p.getFileName().toString()).reversed())
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException("Failed to scan " + reportsDir, e);
        }
    }

    /**
     * Returns the newest simulation directory modified at or after {@code since}
     * (with a five-second slack for clock/mtime granularity). Used to attribute a
     * report directory to the console run that just finished.
     *
     * @param since the earliest acceptable last-modified instant; {@code null} matches
     *              any directory.
     * @return the most recent qualifying directory, or an empty {@code Optional} when
     *         no run produced a report (e.g. the process was killed before Gatling
     *         rendered output).
     */
    public Optional<Path> latestSimulationDirSince(Instant since) {
        return scanSimulationDirs().stream()
                .filter(p -> modifiedAtOrAfter(p, since))
                .findFirst();
    }

    private static boolean modifiedAtOrAfter(Path dir, Instant since) {
        if (since == null) {
            return true;
        }
        try {
            return !Files.getLastModifiedTime(dir).toInstant().isBefore(since.minusSeconds(5));
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Resolves a path relative to the reports root, rejecting traversal attempts
     * that would escape the root directory.
     *
     * @param relativePath the path relative to the reports root
     *                     (e.g. {@code rampagesimulation-20240101123456789/index.html}).
     * @return the absolute, normalised {@code Path}.
     * @throws IllegalArgumentException if {@code relativePath} escapes the reports root.
     */
    public Path resolveReport(String relativePath) {
        Path root = Paths.get(reportsDir).toAbsolutePath().normalize();
        Path candidate = root.resolve(relativePath).normalize();
        if (!candidate.startsWith(root)) {
            throw new IllegalArgumentException("Path escapes reports root: " + relativePath);
        }
        return candidate;
    }

    /**
     * Test seam — overrides the config-resolved reports dir.
     *
     * @param dir the reports directory path to use
     */
    public void setReportsDir(String dir) {
        this.reportsDir = dir;
    }

}
