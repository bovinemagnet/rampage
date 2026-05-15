package io.rampage.console.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfigBrowserTest {

    @TempDir
    Path root;

    private ConfigBrowser browser;

    @BeforeEach
    void setUp() throws Exception {
        Files.createDirectories(root.resolve("environments"));
        Files.createDirectories(root.resolve("runs"));
        Files.createDirectories(root.resolve("scenarios"));

        Files.writeString(root.resolve("environments/local.yaml"), "name: local\n");
        Files.writeString(root.resolve("environments/prod.yml"), "name: prod\n");
        Files.writeString(root.resolve("environments/README.md"), "ignore me");

        Files.writeString(root.resolve("runs/load.yaml"), "name: load\n");
        Files.writeString(root.resolve("scenarios/checkout.yaml"), "id: checkout\n");

        browser = new ConfigBrowser();
        browser.setConfigDir(root.toString());
    }

    @Test
    void listsEnvironmentsSortedByName() {
        List<ConfigEntry> envs = browser.environments();
        assertThat(envs).extracting(ConfigEntry::name)
                .containsExactly("local.yaml", "prod.yml");
    }

    @Test
    void runsAndScenariosListed() {
        assertThat(browser.runs()).extracting(ConfigEntry::name)
                .containsExactly("load.yaml");
        assertThat(browser.scenarios()).extracting(ConfigEntry::name)
                .containsExactly("checkout.yaml");
    }

    @Test
    void missingDirReturnsEmptyListNotError() {
        ConfigBrowser empty = new ConfigBrowser();
        empty.setConfigDir(root.resolve("does-not-exist").toString());
        assertThat(empty.environments()).isEmpty();
        assertThat(empty.runs()).isEmpty();
        assertThat(empty.scenarios()).isEmpty();
    }

    @Test
    void resolveStaysWithinRoot() {
        Path p = browser.resolve("environments/local.yaml");
        assertThat(p).isEqualTo(root.resolve("environments/local.yaml").toAbsolutePath().normalize());
    }

    @Test
    void resolveRejectsPathTraversal() {
        assertThatThrownBy(() -> browser.resolve("../escape.yaml"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("escapes config root");
    }
}
