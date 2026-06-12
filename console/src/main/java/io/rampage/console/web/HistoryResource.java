package io.rampage.console.web;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.RawString;
import io.quarkus.qute.TemplateInstance;
import io.rampage.console.results.RunComparison;
import io.rampage.console.results.RunComparisonService;
import io.rampage.console.results.RunResultIngestor;
import io.rampage.console.results.StoredRun;
import io.rampage.console.results.StoredRunRepository;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

/** The run-history pages, backed by the results store. */
@Path("/history")
public class HistoryResource {

    /**
     * Creates a new {@code HistoryResource} instance. CDI-managed; no arguments required.
     */
    public HistoryResource() {}

    @CheckedTemplate(basePath = "History")
    static class Templates {
        public static native TemplateInstance index(List<StoredRun> runs, List<String> allTags,
                String query, String tag, String status);

        public static native TemplateInstance rows(List<StoredRun> runs);

        public static native TemplateInstance tagCell(StoredRun run);

        public static native TemplateInstance detail(StoredRun run);

        public static native TemplateInstance compare(List<StoredRun> allRuns,
                String idA, String idB, RunComparison comparison);

        public static native TemplateInstance trends(List<String> configKeys,
                String selectedKey, RawString chartJson, boolean hasData);
    }

    @Inject
    StoredRunRepository repository;

    @Inject
    RunResultIngestor ingestor;

    @Inject
    RunComparisonService comparisonService;

    /**
     * Renders the run-history index page, filtered by the supplied query parameters.
     * Any combination of parameters may be omitted; absent parameters are treated as
     * unfiltered.
     *
     * @param query  free-text search string; may be {@code null}
     * @param tag    tag to filter by; may be {@code null}
     * @param status run status to filter by; may be {@code null}
     * @return the rendered {@code History/index} template
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance list(@QueryParam("q") String query,
                                 @QueryParam("tag") String tag,
                                 @QueryParam("status") String status) {
        List<StoredRun> runs = repository.search(query, tag, status);
        return Templates.index(runs, repository.distinctTags(), query, tag, status);
    }

    /**
     * Renders only the run-list rows fragment, suitable for HTMX partial updates.
     * Accepts the same filter parameters as {@link #list}.
     *
     * @param query  free-text search string; may be {@code null}
     * @param tag    tag to filter by; may be {@code null}
     * @param status run status to filter by; may be {@code null}
     * @return the rendered {@code History/rows} template fragment
     */
    @GET
    @Path("/rows")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance rows(@QueryParam("q") String query,
                                 @QueryParam("tag") String tag,
                                 @QueryParam("status") String status) {
        return Templates.rows(repository.search(query, tag, status));
    }

    /**
     * Triggers a filesystem rescan to import any run results not yet in the results store,
     * then renders the updated run-list rows fragment.
     *
     * @return the rendered {@code History/rows} template fragment after re-importing
     */
    @POST
    @Path("/rescan")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance rescan() {
        ingestor.importFromFilesystem();
        return Templates.rows(repository.listNewestFirst());
    }

    /**
     * Adds a tag to the run identified by {@code id}.
     * Tags must match {@code [A-Za-z0-9._-]+} and be at most 100 characters; invalid or
     * blank tags are silently ignored. Renders the updated tag cell fragment.
     *
     * @param id  the run identifier
     * @param tag the tag value to add; may be {@code null} or blank (no-op in that case)
     * @return the rendered {@code History/tagCell} template fragment
     */
    @POST
    @Path("/{id}/tags")
    @Transactional
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance addTag(@PathParam("id") String id, @FormParam("tag") String tag) {
        StoredRun run = require(id);
        if (tag != null) {
            String trimmed = tag.trim();
            // Tags must be URL-path-safe single segments — the delete route is
            // /history/{id}/tags/{tag}, so a slash (or other special char) would
            // make the tag impossible to remove. Restrict to a safe character set.
            if (!trimmed.isEmpty() && trimmed.length() <= 100
                    && trimmed.matches("[A-Za-z0-9._-]+")) {
                run.tags.add(trimmed);
            }
        }
        return Templates.tagCell(run);
    }

    /**
     * Removes a tag from the run identified by {@code id} and renders the updated tag
     * cell fragment. No-op when the tag is not present on the run.
     *
     * @param id  the run identifier
     * @param tag the tag value to remove
     * @return the rendered {@code History/tagCell} template fragment
     */
    @DELETE
    @Path("/{id}/tags/{tag}")
    @Transactional
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance removeTag(@PathParam("id") String id, @PathParam("tag") String tag) {
        StoredRun run = require(id);
        run.tags.remove(tag);
        return Templates.tagCell(run);
    }

    /**
     * Persists free-text notes on the run identified by {@code id} and returns an
     * inline confirmation fragment.
     *
     * @param id    the run identifier
     * @param notes the notes to store; may be {@code null}
     * @return an HTML span confirming that the notes were saved
     */
    @POST
    @Path("/{id}/notes")
    @Transactional
    @Produces(MediaType.TEXT_HTML)
    public String saveNotes(@PathParam("id") String id, @FormParam("notes") String notes) {
        StoredRun run = require(id);
        run.notes = notes;
        return "<span class=\"validation-ok\">Notes saved.</span>";
    }

    /**
     * Renders the run-comparison page for the two runs identified by {@code idA} and
     * {@code idB}. Renders an empty-state page when either id is absent or blank.
     * Falls back to an empty-state page (instead of HTTP 500) when either id is
     * unrecognised, so stale bookmarked URLs remain usable.
     *
     * @param idA identifier of the first run to compare; may be {@code null}
     * @param idB identifier of the second run to compare; may be {@code null}
     * @return the rendered {@code History/compare} template
     */
    @GET
    @Path("/compare")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance compare(@QueryParam("a") String idA, @QueryParam("b") String idB) {
        RunComparison comparison = null;
        if (idA != null && !idA.isBlank() && idB != null && !idB.isBlank()) {
            try {
                comparison = comparisonService.compare(idA, idB);
            } catch (IllegalArgumentException unknownId) {
                // One or both ids are unknown (e.g. a stale bookmarked URL):
                // fall through with a null comparison so the page renders its
                // empty state instead of returning HTTP 500.
                comparison = null;
            }
        }
        return Templates.compare(repository.listNewestFirst(), idA, idB, comparison);
    }

    /**
     * Renders the trend chart page for runs grouped by the given run-config key.
     * Passes an empty series when {@code runConfigKey} is absent or blank, which
     * causes the template to render an empty-state chart.
     *
     * @param runConfigKey the run-config key whose historical runs should be charted;
     *                     may be {@code null}
     * @return the rendered {@code History/trends} template
     */
    @GET
    @Path("/trends")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance trends(@QueryParam("runConfigKey") String runConfigKey) {
        List<StoredRun> series = (runConfigKey != null && !runConfigKey.isBlank())
                ? repository.byRunConfigKey(runConfigKey)
                : List.of();
        RawString chartJson = new RawString(TrendData.toJson(series));
        return Templates.trends(repository.distinctRunConfigKeys(), runConfigKey,
                chartJson, !series.isEmpty());
    }

    /**
     * Renders the detail page for a single run. Returns HTTP 404 when the run is not found.
     *
     * @param id the run identifier
     * @return the rendered {@code History/detail} template
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance detail(@PathParam("id") String id) {
        return Templates.detail(require(id));
    }

    private StoredRun require(String id) {
        StoredRun run = repository.findById(id);
        if (run == null) {
            throw new WebApplicationException("Unknown run: " + id, 404);
        }
        return run;
    }
}
