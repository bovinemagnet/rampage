package io.rampage.console.web;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.rampage.console.results.RunResultIngestor;
import io.rampage.console.results.StoredRun;
import io.rampage.console.results.StoredRunRepository;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
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
    }

    @Inject
    StoredRunRepository repository;

    @Inject
    RunResultIngestor ingestor;

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
}
