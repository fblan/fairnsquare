package org.asymetrik.web.fairnsquare.infrastructure.appinfo;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/info")
@Produces(MediaType.APPLICATION_JSON)
public class AppInfoResource {

    public record AppInfo(String version, String commit) {
    }

    @Inject
    @ConfigProperty(name = "quarkus.application.version")
    String version;

    @Inject
    @ConfigProperty(name = "app.git.commit")
    String gitCommit;

    @GET
    @Operation(summary = "Get application info")
    public AppInfo getInfo() {
        return new AppInfo(version, gitCommit);
    }
}
