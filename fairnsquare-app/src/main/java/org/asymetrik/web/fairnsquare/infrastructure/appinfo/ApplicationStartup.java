package org.asymetrik.web.fairnsquare.infrastructure.appinfo;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import jakarta.enterprise.event.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class ApplicationStartup {

    private static final Logger LOG = System.getLogger(ApplicationStartup.class.getName());

    @Inject
    @ConfigProperty(name = "fairnsquare.application.name")
    String applicationName;

    @Inject
    @ConfigProperty(name = "fairnsquare.application.version")
    String applicationVersion;

    @Inject
    @ConfigProperty(name = "app.git.commit")
    String gitCommit;

    void onStart(@Observes Startup event) {
        LOG.log(Level.INFO,
                "application=%s version=%s commit=%s".formatted(applicationName, applicationVersion, gitCommit));
    }
}
