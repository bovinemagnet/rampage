package io.rampage.console.config;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Read-only enumeration of the YAML files under {@code config/}. Used by the
 * launch dropdowns and (later milestones) the editor index.
 */
@ApplicationScoped
public class ConfigBrowser {

    private static final Logger log = LoggerFactory.getLogger(ConfigBrowser.class);

    @ConfigProperty(name = "rampage.console.repo-root")
    Optional<String> repoRootRaw;

    @ConfigProperty(name = "rampage.console.config-dir")
    Optional<String> configDirOverride;

    private volatile String configDir;

    @PostConstruct
    void init() {
        if (configDir != null) return; // test seam already populated it
        configDir = configDirOverride
                .filter(s -> !s.isBlank())
                .orElseGet(() -> {
                    String start = repoRootRaw.filter(s -> !s.isBlank())
                            .orElseGet(() -> System.getProperty("user.dir"));
                    return PathResolver.resolveRepoRoot(start).resolve("config").toString();
                });
        log.info("ConfigBrowser using configDir={}", configDir);
    }

    public List<ConfigEntry> environments() {
        return list("environments");
    }

    public List<ConfigEntry> runs() {
        return list("runs");
    }

    public List<ConfigEntry> scenarios() {
        return list("scenarios");
    }

    public Path resolve(String relativePath) {
        Path root = Paths.get(configDir).toAbsolutePath().normalize();
        Path candidate = root.resolve(relativePath).normalize();
        if (!candidate.startsWith(root)) {
            throw new IllegalArgumentException("Path escapes config root: " + relativePath);
        }
        return candidate;
    }

    /** Test seam — override the config dir without going through MP Config. */
    public void setConfigDir(String dir) {
        this.configDir = dir;
    }

    public String configDir() {
        return configDir;
    }

    private List<ConfigEntry> list(String subdir) {
        Path root = Paths.get(configDir).toAbsolutePath().normalize();
        Path dir = root.resolve(subdir);
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> {
                        String n = p.getFileName().toString();
                        return n.endsWith(".yaml") || n.endsWith(".yml");
                    })
                    .map(p -> new ConfigEntry(
                            p.getFileName().toString(),
                            root.relativize(p).toString(),
                            p.toString()))
                    .sorted(Comparator.comparing(ConfigEntry::name))
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException("Failed to list " + subdir + " in " + configDir, e);
        }
    }
}
