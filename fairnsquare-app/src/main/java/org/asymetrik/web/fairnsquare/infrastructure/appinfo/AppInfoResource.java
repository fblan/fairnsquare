package org.asymetrik.web.fairnsquare.infrastructure.appinfo;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@ApplicationScoped
@Path("/info")
@Produces(MediaType.APPLICATION_JSON)
public class AppInfoResource {

    public record AppInfo(String version, String commit) {
    }

    @Inject
    @ConfigProperty(name = "fairnsquare.application.version")
    String version;

    @Inject
    @ConfigProperty(name = "app.git.commit")
    String gitCommit;

    @GET
    public AppInfo getInfo() {
        return new AppInfo(version, gitCommit);
    }
}
