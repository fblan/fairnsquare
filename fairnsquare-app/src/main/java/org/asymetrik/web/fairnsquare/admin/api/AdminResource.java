package org.asymetrik.web.fairnsquare.admin.api;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.asymetrik.web.fairnsquare.admin.api.dto.AdminStatsResponse;
import org.asymetrik.web.fairnsquare.admin.service.AdminService;

/**
 * Admin REST resource. All endpoints require a valid {@code X-Admin-Token} header (enforced by
 * {@link org.asymetrik.web.fairnsquare.admin.api.internal.AdminTokenFilter}).
 */
@Path("/api/admin")
@AdminProtected
public class AdminResource {

    private final AdminService adminService;

    @Inject
    public AdminResource(AdminService adminService) {
        this.adminService = adminService;
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
}
