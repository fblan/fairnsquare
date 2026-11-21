package org.asymetrik.web.fairnsquare.infrastructure.filesystem.internal;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Startup;
import jakarta.inject.Inject;

import org.asymetrik.web.fairnsquare.infrastructure.filesystem.FileSystemService;

/**
 * Removes the files older than the configured retention period, every night at midnight (the server's time zone).
 * vidocq has no scheduler: a single daemon thread, started with the application, runs it.
 *
 * @see FileSystemService#cleanOldFiles()
 */
@ApplicationScoped
public class StorageCleanupScheduler {

    private static final Logger LOG = System.getLogger(StorageCleanupScheduler.class.getName());

    private final FileSystemService fileSystemService;
    private ScheduledExecutorService executor;

    @Inject
    public StorageCleanupScheduler(FileSystemService fileSystemService) {
        this.fileSystemService = fileSystemService;
    }

    void onStart(@Observes Startup event) {
        executor = Executors
                .newSingleThreadScheduledExecutor(Thread.ofPlatform().name("storage-cleanup").daemon().factory());
        executor.scheduleAtFixedRate(this::runNightlyCleanup, untilMidnight(Clock.systemDefaultZone()).toMillis(),
                TimeUnit.DAYS.toMillis(1), TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    void stop() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    public void runNightlyCleanup() {
        LOG.log(Level.INFO, "Nightly storage cleanup started.");
        try {
            fileSystemService.cleanOldFiles();
        } catch (RuntimeException e) {
            // A failure must not cancel the following nights.
            LOG.log(Level.ERROR, "Nightly storage cleanup failed", e);
        }
    }

    /** The time from now to the next midnight. */
    static Duration untilMidnight(Clock clock) {
        ZonedDateTime now = ZonedDateTime.now(clock);
        ZonedDateTime midnight = LocalDate.now(clock).plusDays(1).atStartOfDay(clock.getZone());
        return Duration.between(now, midnight);
    }
}
