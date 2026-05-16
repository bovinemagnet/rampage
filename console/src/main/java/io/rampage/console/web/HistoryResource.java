package io.rampage.console.web;

import io.quarkus.qute.CheckedTemplate;
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

    @CheckedTemplate(basePath = "History")
    static class Templates {
        public static native TemplateInstance index(List<StoredRun> runs, List<String> allTags,
                String query, String tag, String status);

        public static native TemplateInstance rows(List<StoredRun> runs);

        public static native TemplateInstance tagCell(StoredRun run);

        public static native TemplateInstance detail(StoredRun run);

        public static native TemplateInstance compare(List<StoredRun> allRuns,
                String idA, String idB, RunComparison comparison);
    }

    @Inject
    StoredRunRepository repository;

    @Inject
    RunResultIngestor ingestor;

    @Inject
    RunComparisonService comparisonService;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance list(@QueryParam("q") String query,
                                 @QueryParam("tag") String tag,
                                 @QueryParam("status") String status) {
        List<StoredRun> runs = repository.search(query, tag, status);
        return Templates.index(runs, repository.distinctTags(), query, tag, status);
    }

    @GET
    @Path("/rows")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance rows(@QueryParam("q") String query,
                                 @QueryParam("tag") String tag,
                                 @QueryParam("status") String status) {
        return Templates.rows(repository.search(query, tag, status));
    }

    @POST
    @Path("/rescan")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance rescan() {
        ingestor.importFromFilesystem();
        return Templates.rows(repository.listNewestFirst());
    }

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

    @DELETE
    @Path("/{id}/tags/{tag}")
    @Transactional
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance removeTag(@PathParam("id") String id, @PathParam("tag") String tag) {
        StoredRun run = require(id);
        run.tags.remove(tag);
        return Templates.tagCell(run);
    }

    @POST
    @Path("/{id}/notes")
    @Transactional
    @Produces(MediaType.TEXT_HTML)
    public String saveNotes(@PathParam("id") String id, @FormParam("notes") String notes) {
        StoredRun run = require(id);
        run.notes = notes;
        return "<span class=\"validation-ok\">Notes saved.</span>";
    }

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
