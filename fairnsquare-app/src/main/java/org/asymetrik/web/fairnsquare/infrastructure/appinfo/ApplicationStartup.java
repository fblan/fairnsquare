package org.asymetrik.web.fairnsquare.infrastructure.appinfo;

import org.jboss.logging.Logger;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class ApplicationStartup {

    private static final Logger LOG = Logger.getLogger(ApplicationStartup.class);

    @Inject
    @ConfigProperty(name = "quarkus.application.name")
    String applicationName;

    @Inject
    @ConfigProperty(name = "quarkus.application.version")
    String applicationVersion;

    @Inject
    @ConfigProperty(name = "app.git.commit")
    String gitCommit;

    void onStart(@Observes StartupEvent event) {
        LOG.infof("application=%s version=%s commit=%s", applicationName, applicationVersion, gitCommit);
    }
}
