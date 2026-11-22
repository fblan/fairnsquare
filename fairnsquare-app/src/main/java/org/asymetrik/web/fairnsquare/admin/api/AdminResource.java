package org.asymetrik.web.fairnsquare.admin.api;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.asymetrik.web.fairnsquare.admin.api.dto.AdminStatsResponse;
import org.asymetrik.web.fairnsquare.admin.service.AdminService;
import org.asymetrik.web.fairnsquare.admin.service.ModuleReportService;

/**
 * Admin REST resource. All endpoints require a valid {@code X-Admin-Token} header (enforced by
 * {@link org.asymetrik.web.fairnsquare.admin.api.internal.AdminTokenFilter}).
 */
@Path("/api/admin")
@AdminProtected
public class AdminResource {

    /**
     * The report runs scripts: sandboxed, it can't reach the application's origin, its storage included, if it is ever
     * opened directly.
     */
    static final String MODULE_REPORT_POLICY = "sandbox allow-scripts; default-src 'none'; "
            + "script-src 'unsafe-inline'; style-src 'unsafe-inline'; img-src data:";

    private final AdminService adminService;
    private final ModuleReportService moduleReport;

    @Inject
    public AdminResource(AdminService adminService, ModuleReportService moduleReport) {
        this.adminService = adminService;
        this.moduleReport = moduleReport;
    }

    /**
     * Returns global admin statistics: total split count, timestamp of last update, and the full list of split
     * summaries (sorted by creation date). Split IDs are never exposed — only truncated SHA-256 hashes.
     */
    @GET
    @Path("/stats")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStats() {
        AdminStatsResponse stats = adminService.getStats();
        return Response.ok(stats).build();
    }

    /**
     * modular's module report of the running build, a whole HTML page; 404 when the build has none.
     */
    @GET
    @Path("/module-report")
    @Produces(MediaType.TEXT_HTML)
    public Response getModuleReport() {
        return moduleReport.page()
                .map(page -> Response.ok(page).header("Content-Security-Policy", MODULE_REPORT_POLICY).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).type(MediaType.TEXT_PLAIN)
                        .entity("No module report in this build.").build());
    }
}
