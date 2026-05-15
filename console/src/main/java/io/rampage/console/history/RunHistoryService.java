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
import java.util.stream.Stream;

/**
 * Lists previously completed Gatling runs by scanning {@code build/reports/gatling/}.
 * A directory is considered a finished run if it contains an {@code index.html}
 * (Gatling's stock report file).
 */
@ApplicationScoped
public class RunHistoryService {

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

    public List<RunHistoryEntry> listRecent(int limit) {
        Path root = Paths.get(reportsDir).toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(root)) {
            return stream
                    .filter(Files::isDirectory)
                    .filter(p -> Files.isRegularFile(p.resolve("index.html")))
                    .map(p -> toEntry(root, p))
                    .sorted(Comparator.comparing(RunHistoryEntry::finishedAt).reversed())
                    .limit(limit)
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException("Failed to scan " + reportsDir, e);
        }
    }

    public Path resolveReport(String relativePath) {
        Path root = Paths.get(reportsDir).toAbsolutePath().normalize();
        Path candidate = root.resolve(relativePath).normalize();
        if (!candidate.startsWith(root)) {
            throw new IllegalArgumentException("Path escapes reports root: " + relativePath);
        }
        return candidate;
    }

    /** Test seam — overrides the config-resolved reports dir. */
    public void setReportsDir(String dir) {
        this.reportsDir = dir;
    }

    private static RunHistoryEntry toEntry(Path root, Path dir) {
        Instant mtime;
        try {
            mtime = Files.getLastModifiedTime(dir).toInstant();
        } catch (IOException e) {
            mtime = Instant.EPOCH;
        }
        boolean hasMeta = Files.isRegularFile(dir.resolve("run-metadata.json"));
        String relative = root.relativize(dir).toString();
        return new RunHistoryEntry(
                dir.getFileName().toString(),
                mtime,
                "/reports/" + relative + "/index.html",
                hasMeta);
    }
}
