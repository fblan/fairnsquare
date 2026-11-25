package org.asymetrik.web.fairnsquare.backup.internal;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * The backup configuration ({@code fairnsquare.backup.*}, from the {@code BACKUP_*} environment variables). Backups are
 * on when a bucket is configured.
 */
@ApplicationScoped
public class BackupSettings {

    private final Optional<String> bucket;
    private final Optional<String> endpoint;
    private final String region;
    private final boolean pathStyle;
    private final Optional<String> accessKey;
    private final Optional<String> secretKey;
    private final String prefix;
    private final Optional<String> encryptionKey;
    private final int keep;

    @Inject
    public BackupSettings(@ConfigProperty(name = "fairnsquare.backup.s3.bucket") Optional<String> bucket,
            @ConfigProperty(name = "fairnsquare.backup.s3.endpoint") Optional<String> endpoint,
            @ConfigProperty(name = "fairnsquare.backup.s3.region", defaultValue = "us-east-1") String region,
            @ConfigProperty(name = "fairnsquare.backup.s3.path-style", defaultValue = "false") boolean pathStyle,
            @ConfigProperty(name = "fairnsquare.backup.s3.access-key") Optional<String> accessKey,
            @ConfigProperty(name = "fairnsquare.backup.s3.secret-key") Optional<String> secretKey,
            @ConfigProperty(name = "fairnsquare.backup.s3.prefix", defaultValue = "fairnsquare/") String prefix,
            @ConfigProperty(name = "fairnsquare.backup.encryption-key") Optional<String> encryptionKey,
            @ConfigProperty(name = "fairnsquare.backup.keep", defaultValue = "30") int keep) {
        this.bucket = nonBlank(bucket);
        this.endpoint = nonBlank(endpoint);
        this.region = region;
        this.pathStyle = pathStyle;
        this.accessKey = nonBlank(accessKey);
        this.secretKey = nonBlank(secretKey);
        this.prefix = prefix.isEmpty() || prefix.endsWith("/") ? prefix : prefix + "/";
        this.encryptionKey = nonBlank(encryptionKey);
        this.keep = keep;
    }

    private static Optional<String> nonBlank(Optional<String> value) {
        return value.map(String::trim).filter(text -> !text.isEmpty());
    }

    public boolean enabled() {
        return bucket.isPresent();
    }

    public Optional<String> bucket() {
        return bucket;
    }

    public Optional<String> endpoint() {
        return endpoint;
    }

    public String region() {
        return region;
    }

    public boolean pathStyle() {
        return pathStyle;
    }

    public Optional<String> accessKey() {
        return accessKey;
    }

    public Optional<String> secretKey() {
        return secretKey;
    }

    /** The folder of the archives in the bucket, empty or ending with {@code /}. */
    public String prefix() {
        return prefix;
    }

    public Optional<String> encryptionKey() {
        return encryptionKey;
    }

    /** How many archives to keep; 0 keeps them all. */
    public int keep() {
        return keep;
    }
}
