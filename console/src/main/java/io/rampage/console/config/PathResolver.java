package io.rampage.console.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Resolves the repo root from whatever directory the console JVM happens to
 * have been started in. Walks up from the supplied directory looking for
 * {@code settings.gradle.kts} (or {@code settings.gradle}) — the surest
 * indicator of a Gradle project root. This makes the console robust whether
 * it's launched from the repo root, the {@code console/} subproject, or
 * anywhere else.
 */
public final class PathResolver {

    private PathResolver() {}

    /**
     * Walks up the directory tree from {@code configured} looking for a
     * {@code settings.gradle.kts} or {@code settings.gradle} file, returning the
     * first directory that contains one.
     *
     * @param configured the configured starting point (typically {@code user.dir}
     *                   or an explicit {@code rampage.console.repo-root}).
     * @return the directory containing {@code settings.gradle.kts}, or the
     *         supplied path itself if none is found.
     */
    public static Path resolveRepoRoot(String configured) {
        Path start = Paths.get(configured == null || configured.isBlank() ? "." : configured)
                .toAbsolutePath().normalize();
        Path search = start;
        while (search != null) {
            if (Files.exists(search.resolve("settings.gradle.kts"))
                    || Files.exists(search.resolve("settings.gradle"))) {
                return search;
            }
            search = search.getParent();
        }
        return start;
    }
}
