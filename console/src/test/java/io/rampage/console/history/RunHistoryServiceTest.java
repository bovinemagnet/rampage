package io.rampage.console.history;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RunHistoryServiceTest {

    @TempDir
    Path reportsRoot;

    private RunHistoryService service;

    @BeforeEach
    void setUp() throws Exception {
        // Three simulation directories: two valid (have index.html), one not.
        Path simA = reportsRoot.resolve("rampagesimulation-20260515000724458");
        Path simB = reportsRoot.resolve("rampagesimulation-20260515000946259");
        Path notARun = reportsRoot.resolve("garbage");
        Files.createDirectories(simA);
        Files.createDirectories(simB);
        Files.createDirectories(notARun);

        Files.writeString(simA.resolve("index.html"), "<html>A</html>");
        Files.writeString(simB.resolve("index.html"), "<html>B</html>");
        Files.writeString(simB.resolve("run-metadata.json"), "{\"runId\":\"smoke\"}");
        Files.writeString(notARun.resolve("README"), "ignore");

        // Force B to have a later mtime so ordering is deterministic.
        Thread.sleep(20);
        Files.setLastModifiedTime(simB, java.nio.file.attribute.FileTime.fromMillis(System.currentTimeMillis()));

        service = new RunHistoryService();
        service.setReportsDir(reportsRoot.toString());
    }

    @Test
    void listsOnlyDirectoriesWithIndexHtml() {
        List<RunHistoryEntry> entries = service.listRecent(10);
        assertThat(entries).extracting(RunHistoryEntry::simulationDir)
                .containsExactlyInAnyOrder(
                        "rampagesimulation-20260515000724458",
                        "rampagesimulation-20260515000946259");
    }

    @Test
    void newestFirst() {
        List<RunHistoryEntry> entries = service.listRecent(10);
        assertThat(entries.get(0).simulationDir()).isEqualTo("rampagesimulation-20260515000946259");
    }

    @Test
    void hasMetadataReflectsFilePresence() {
        List<RunHistoryEntry> entries = service.listRecent(10);
        RunHistoryEntry withMeta = entries.stream()
                .filter(e -> e.simulationDir().endsWith("946259")).findFirst().orElseThrow();
        RunHistoryEntry withoutMeta = entries.stream()
                .filter(e -> e.simulationDir().endsWith("724458")).findFirst().orElseThrow();
        assertThat(withMeta.hasMetadata()).isTrue();
        assertThat(withoutMeta.hasMetadata()).isFalse();
    }

    @Test
    void reportPathPointsAtIndexHtml() {
        RunHistoryEntry entry = service.listRecent(10).get(0);
        assertThat(entry.reportPath()).endsWith("/index.html").contains("/reports/");
    }

    @Test
    void limitIsRespected() {
        List<RunHistoryEntry> entries = service.listRecent(1);
        assertThat(entries).hasSize(1);
    }

    @Test
    void missingReportsDirReturnsEmpty() {
        RunHistoryService empty = new RunHistoryService();
        empty.setReportsDir(reportsRoot.resolve("does-not-exist").toString());
        assertThat(empty.listRecent(10)).isEmpty();
    }

    @Test
    void resolveRejectsPathTraversal() {
        assertThatThrownBy(() -> service.resolveReport("../escape"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("escapes reports root");
    }

    @Test
    void scanSimulationDirsReturnsOnlyDirsWithIndexHtmlNewestFirst() {
        List<Path> dirs = service.scanSimulationDirs();
        assertThat(dirs).extracting(p -> p.getFileName().toString())
                .containsExactly(
                        "rampagesimulation-20260515000946259",
                        "rampagesimulation-20260515000724458");
    }

    @Test
    void latestSimulationDirSinceFiltersByModificationTime() {
        Instant future = Instant.now().plusSeconds(3600);
        assertThat(service.latestSimulationDirSince(future)).isEmpty();

        Instant past = Instant.now().minusSeconds(3600);
        assertThat(service.latestSimulationDirSince(past)).isPresent();
        assertThat(service.latestSimulationDirSince(past).get().getFileName().toString())
                .startsWith("rampagesimulation-");
    }
}
