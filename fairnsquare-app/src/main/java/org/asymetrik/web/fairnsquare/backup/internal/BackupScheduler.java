package org.asymetrik.web.fairnsquare.backup.internal;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.asymetrik.web.fairnsquare.backup.BackupService;
import org.jboss.logging.Logger;

import io.quarkus.scheduler.Scheduled;

/**
 * The nightly backup ({@code fairnsquare.backup.cron}, 3 am by default): an archive if splits changed since the last
 * one. Nothing when backups are off.
 */
@ApplicationScoped
public class BackupScheduler {

    private static final Logger LOG = Logger.getLogger(BackupScheduler.class);

    private final BackupSettings settings;
    private final BackupService backups;

    @Inject
    public BackupScheduler(BackupSettings settings, BackupService backups) {
        this.settings = settings;
        this.backups = backups;
    }

    @Scheduled(cron = "{fairnsquare.backup.cron}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void backUpIfChanged() {
        if (!settings.enabled()) {
            return;
        }
        try {
            backups.backUpIfChanged();
        } catch (RuntimeException e) {
            LOG.errorf(e, "Nightly backup failed: %s", e.getMessage());
        }
    }
}
