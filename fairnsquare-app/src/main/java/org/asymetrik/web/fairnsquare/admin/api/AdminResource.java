package org.asymetrik.web.fairnsquare.admin.api;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.asymetrik.web.fairnsquare.admin.api.dto.AdminBackupsResponse;
import org.asymetrik.web.fairnsquare.admin.api.dto.AdminStatsResponse;
import org.asymetrik.web.fairnsquare.admin.service.AdminService;
import org.asymetrik.web.fairnsquare.admin.service.ModuleReportService;
import org.asymetrik.web.fairnsquare.backup.BackupComparison;
import org.asymetrik.web.fairnsquare.backup.BackupService;
import org.asymetrik.web.fairnsquare.backup.BackupStatus;
import org.asymetrik.web.fairnsquare.sharedkernel.error.BaseError;

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
    private final BackupService backups;

    @Inject
    public AdminResource(AdminService adminService, ModuleReportService moduleReport, BackupService backups) {
        this.adminService = adminService;
        this.moduleReport = moduleReport;
        this.backups = backups;
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

    /**
     * The backups: whether they are on and the archives, newest first. When the archives can't be listed, the reason is
     * the {@code problem}.
     */
    @GET
    @Path("/backups")
    @Produces(MediaType.APPLICATION_JSON)
    public AdminBackupsResponse getBackups() {
        BackupStatus status = backups.status();
        if (!status.enabled() || status.problem() != null) {
            return new AdminBackupsResponse(status.enabled(), status.problem(), List.of());
        }
        try {
            return new AdminBackupsResponse(true, null, backups.archives());
        } catch (BaseError e) {
            return new AdminBackupsResponse(true, e.getMessage(), List.of());
        }
    }

    /** Makes an archive now, even if no split changed since the last one. */
    @POST
    @Path("/backups")
    @Produces(MediaType.APPLICATION_JSON)
    public Response backUpNow() {
        return Response.status(Response.Status.CREATED).entity(backups.backUpNow()).build();
    }

    /** How the current splits differ from an archive's: totals only. */
    @GET
    @Path("/backups/{name}/comparison")
    @Produces(MediaType.APPLICATION_JSON)
    public BackupComparison compareWithBackup(@PathParam("name") String name) {
        return backups.compare(name);
    }
}
