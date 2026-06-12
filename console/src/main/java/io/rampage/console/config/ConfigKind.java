package io.rampage.console.config;

/**
 * The three categories of YAML configuration file recognised by the Rampage console.
 * Each constant maps to the subdirectory under {@code config/} where those files live.
 */
public enum ConfigKind {
    /** Environment configuration files stored under {@code config/environments/}. */
    ENVIRONMENT("environments"),
    /** Run configuration files stored under {@code config/runs/}. */
    RUN("runs"),
    /** Scenario configuration files stored under {@code config/scenarios/}. */
    SCENARIO("scenarios");

    private final String directory;

    ConfigKind(String directory) {
        this.directory = directory;
    }

    /**
     * Returns the subdirectory name associated with this config kind.
     *
     * @return the subdirectory name (e.g. {@code "environments"}).
     */
    public String directory() {
        return directory;
    }

    /**
     * Derives the config kind from a path relative to the config root.
     *
     * @param relativePath the path to classify (e.g. {@code environments/local.yaml}).
     * @return the matching {@code ConfigKind}.
     * @throws IllegalArgumentException if {@code relativePath} is null or does not match
     *         any known subdirectory prefix.
     */
    public static ConfigKind fromRelativePath(String relativePath) {
        if (relativePath == null) {
            throw new IllegalArgumentException("relativePath is null");
        }
        String normalised = relativePath.replace('\\', '/');
        for (ConfigKind k : values()) {
            if (normalised.startsWith(k.directory + "/")) {
                return k;
            }
        }
        throw new IllegalArgumentException(
                "Path does not match a known config kind (" + relativePath
                        + "); expected to start with environments/, runs/, or scenarios/.");
    }
}
