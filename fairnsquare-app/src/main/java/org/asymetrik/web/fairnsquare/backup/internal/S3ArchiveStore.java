package org.asymetrik.web.fairnsquare.backup.internal;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

/**
 * The archives in an S3 bucket, under a prefix. Works with AWS and S3-compatible providers (endpoint, path-style
 * access). The client is created on first use, so that the application starts whatever the backup configuration.
 */
@ApplicationScoped
public class S3ArchiveStore implements ArchiveStore {

    private final BackupSettings settings;
    private S3Client client;

    @Inject
    public S3ArchiveStore(BackupSettings settings) {
        this.settings = settings;
    }

    private synchronized S3Client client() {
        if (client == null) {
            client = createClient(settings);
        }
        return client;
    }

    private String bucket() {
        return settings.bucket().orElseThrow(() -> new IllegalStateException("No backup bucket configured."));
    }

    private String prefix() {
        return settings.prefix();
    }

    private static S3Client createClient(BackupSettings settings) {
        var builder = S3Client.builder().httpClient(UrlConnectionHttpClient.create())
                .region(Region.of(settings.region())).credentialsProvider(credentials(settings))
                .forcePathStyle(settings.pathStyle())
                // S3-compatible providers don't all accept the SDK's default CRC checksums.
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED);
        settings.endpoint().ifPresent(endpoint -> builder.endpointOverride(URI.create(endpoint)));
        return builder.build();
    }

    private static AwsCredentialsProvider credentials(BackupSettings settings) {
        if (settings.accessKey().isPresent() && settings.secretKey().isPresent()) {
            return StaticCredentialsProvider
                    .create(AwsBasicCredentials.create(settings.accessKey().get(), settings.secretKey().get()));
        }
        return DefaultCredentialsProvider.builder().build();
    }

    @Override
    public List<StoredObject> list() throws IOException {
        try {
            String prefix = prefix();
            return client().listObjectsV2Paginator(request -> request.bucket(bucket()).prefix(prefix)).contents()
                    .stream().map(object -> new StoredObject(object.key().substring(prefix.length()), object.size()))
                    .toList();
        } catch (SdkException e) {
            throw failure("list the archives", e);
        }
    }

    @Override
    public void put(String name, Path file) throws IOException {
        try {
            client().putObject(
                    request -> request.bucket(bucket()).key(prefix() + name).contentType("application/octet-stream"),
                    RequestBody.fromFile(file));
        } catch (SdkException e) {
            throw failure("upload " + name, e);
        }
    }

    @Override
    public InputStream open(String name) throws IOException {
        try {
            return client().getObject(request -> request.bucket(bucket()).key(prefix() + name));
        } catch (NoSuchKeyException e) {
            throw new FileNotFoundException(name);
        } catch (SdkException e) {
            throw failure("download " + name, e);
        }
    }

    @Override
    public void delete(String name) throws IOException {
        try {
            client().deleteObject(request -> request.bucket(bucket()).key(prefix() + name));
        } catch (SdkException e) {
            throw failure("delete " + name, e);
        }
    }

    private IOException failure(String action, SdkException e) {
        return new IOException("Could not " + action + " in the S3 bucket: " + e.getMessage(), e);
    }

    @PreDestroy
    synchronized void close() {
        if (client != null) {
            client.close();
        }
    }
}
