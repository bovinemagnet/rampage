package io.rampage.reporting;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class RunMetadataPromoterTest {

    private final RunMetadataPromoter promoter = new RunMetadataPromoter();

    @Test
    void promote_movesMetadataIntoNewestSimulationDir(@TempDir Path reportsRoot) throws IOException {
        Path stagedMetadata = reportsRoot.resolve("run-metadata.json");
        Files.writeString(stagedMetadata, "{\"runId\":\"r1\"}");

        Path olderSim = reportsRoot.resolve("rampagesimulation-20260101010101000");
        Files.createDirectory(olderSim);
        Files.writeString(olderSim.resolve("index.html"), "<html/>");

        Path newestSim = reportsRoot.resolve("rampagesimulation-20260201010101000");
        Files.createDirectory(newestSim);
        Files.writeString(newestSim.resolve("index.html"), "<html/>");

        promoter.promote(reportsRoot);

        assertFalse(Files.exists(stagedMetadata), "Staged metadata should have been moved");
        Path moved = newestSim.resolve("run-metadata.json");
        assertTrue(Files.exists(moved), "Metadata should be in newest sim dir");
        assertEquals("{\"runId\":\"r1\"}", Files.readString(moved));
        assertFalse(Files.exists(olderSim.resolve("run-metadata.json")),
            "Older sim dir should not receive metadata");
    }

    @Test
    void promote_alsoMovesConfigSnapshot(@TempDir Path reportsRoot) throws IOException {
        Files.writeString(reportsRoot.resolve("run-metadata.json"), "{}");
        Files.writeString(reportsRoot.resolve("config-snapshot.yaml"), "{\"env\":\"local\"}");

        Path sim = reportsRoot.resolve("rampagesimulation-20260101010101000");
        Files.createDirectory(sim);
        Files.writeString(sim.resolve("index.html"), "<html/>");

        promoter.promote(reportsRoot);

        assertTrue(Files.exists(sim.resolve("run-metadata.json")));
        assertTrue(Files.exists(sim.resolve("config-snapshot.yaml")));
        assertFalse(Files.exists(reportsRoot.resolve("config-snapshot.yaml")));
    }

    @Test
    void promote_isNoOpWhenStagedFilesMissing(@TempDir Path reportsRoot) throws IOException {
        Path sim = reportsRoot.resolve("rampagesimulation-20260101010101000");
        Files.createDirectory(sim);
        Files.writeString(sim.resolve("index.html"), "<html/>");

        assertDoesNotThrow(() -> promoter.promote(reportsRoot));
        assertFalse(Files.exists(sim.resolve("run-metadata.json")));
    }

    @Test
    void promote_isNoOpWhenNoSimDirsExist(@TempDir Path reportsRoot) throws IOException {
        Path stagedMetadata = reportsRoot.resolve("run-metadata.json");
        Files.writeString(stagedMetadata, "{}");

        assertDoesNotThrow(() -> promoter.promote(reportsRoot));
        // Leave the staged file where it was; do not delete data we cannot place.
        assertTrue(Files.exists(stagedMetadata), "Should not lose the metadata when no sim dir present");
    }

    @Test
    void promote_isNoOpWhenReportsRootMissing(@TempDir Path workDir) {
        Path missing = workDir.resolve("does-not-exist");
        assertDoesNotThrow(() -> promoter.promote(missing));
    }

    @Test
    void promote_picksNewestByDirectoryNameNotMtime(@TempDir Path reportsRoot) throws IOException {
        Files.writeString(reportsRoot.resolve("run-metadata.json"), "{}");

        Path nameNewer = reportsRoot.resolve("rampagesimulation-20260201010101000");
        Files.createDirectory(nameNewer);
        Files.writeString(nameNewer.resolve("index.html"), "<html/>");

        Path mtimeNewerNameOlder = reportsRoot.resolve("rampagesimulation-20260101010101000");
        Files.createDirectory(mtimeNewerNameOlder);
        Files.writeString(mtimeNewerNameOlder.resolve("index.html"), "<html/>");
        Files.setLastModifiedTime(mtimeNewerNameOlder, FileTime.from(Instant.now().plusSeconds(60)));

        promoter.promote(reportsRoot);

        assertTrue(Files.exists(nameNewer.resolve("run-metadata.json")),
            "Newest by lexicographic name (timestamp-encoded) should win");
        assertFalse(Files.exists(mtimeNewerNameOlder.resolve("run-metadata.json")));
    }
}
