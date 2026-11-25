package org.asymetrik.web.fairnsquare.admin;

import java.util.Map;
import java.util.Set;

import org.asymetrik.web.fairnsquare.backup.internal.InMemoryArchiveStore;

import io.quarkus.test.junit.QuarkusTestProfile;

/**
 * Backups on, into a bucket in memory ({@link InMemoryArchiveStore}).
 */
public class AdminBackupsProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("fairnsquare.backup.s3.bucket", "test-bucket", "fairnsquare.backup.encryption-key",
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");
    }

    @Override
    public Set<Class<?>> getEnabledAlternatives() {
        return Set.of(InMemoryArchiveStore.class);
    }
}
