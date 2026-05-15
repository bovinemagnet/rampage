package io.rampage.console.web;

import io.rampage.console.history.RunHistoryService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

import java.io.IOException;
import java.nio.file.Files;

/**
 * Serves files under the Gatling reports directory so the dashboard can deep
 * link into a past run's HTML report and {@code run-metadata.json}.
 */
@Path("/reports")
public class ReportsResource {

    @Inject
    RunHistoryService history;

    @GET
    @Path("/{path:.+}")
    public Response serve(@PathParam("path") String path) {
        java.nio.file.Path file;
        try {
            file = history.resolveReport(path);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
        if (!Files.isRegularFile(file)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        try {
            byte[] bytes = Files.readAllBytes(file);
            return Response.ok(bytes)
                    .type(contentType(file.getFileName().toString()))
                    .build();
        } catch (IOException e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    private static String contentType(String name) {
        if (name.endsWith(".html"))  return "text/html; charset=utf-8";
        if (name.endsWith(".css"))   return "text/css; charset=utf-8";
        if (name.endsWith(".js"))    return "application/javascript; charset=utf-8";
        if (name.endsWith(".json"))  return "application/json; charset=utf-8";
        if (name.endsWith(".svg"))   return "image/svg+xml";
        if (name.endsWith(".png"))   return "image/png";
        if (name.endsWith(".ico"))   return "image/x-icon";
        if (name.endsWith(".woff"))  return "font/woff";
        if (name.endsWith(".woff2")) return "font/woff2";
        if (name.endsWith(".log"))   return "text/plain; charset=utf-8";
        return "application/octet-stream";
    }
}
