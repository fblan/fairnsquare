package org.asymetrik.web.fairnsquare.infrastructure.filesystem.internal;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.scheduler.Scheduled;
import org.jboss.logging.Logger;

import org.asymetrik.web.fairnsquare.infrastructure.filesystem.FileSystemService;

/**
 * Scheduled job that runs nightly at midnight to remove files older than the configured retention period.
 *
 * @see FileSystemService#cleanOldFiles()
 */
@ApplicationScoped
public class StorageCleanupScheduler {

    private static final Logger LOG = Logger.getLogger(StorageCleanupScheduler.class);

    private final FileSystemService fileSystemService;

    @Inject
    public StorageCleanupScheduler(FileSystemService fileSystemService) {
        this.fileSystemService = fileSystemService;
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void runNightlyCleanup() {
        LOG.info("Nightly storage cleanup started.");
        fileSystemService.cleanOldFiles();
    }
}
