package io.rampage.console.config;

public enum ConfigKind {
    ENVIRONMENT("environments"),
    RUN("runs"),
    SCENARIO("scenarios");

    private final String directory;

    ConfigKind(String directory) {
        this.directory = directory;
    }

    public String directory() {
        return directory;
    }

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
