package org.asymetrik.web.fairnsquare.split.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.scheduler.Scheduled;
import org.jboss.logging.Logger;

/**
 * Scheduled job that runs nightly at midnight to remove split files older than the configured retention period.
 *
 * @see StorageConstraintsService#cleanOldFiles()
 */
@ApplicationScoped
public class StorageCleanupScheduler {

    private static final Logger LOG = Logger.getLogger(StorageCleanupScheduler.class);

    private final StorageConstraintsService storageConstraints;

    @Inject
    public StorageCleanupScheduler(StorageConstraintsService storageConstraints) {
        this.storageConstraints = storageConstraints;
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void runNightlyCleanup() {
        LOG.info("Nightly storage cleanup started.");
        storageConstraints.cleanOldFiles();
    }
}
