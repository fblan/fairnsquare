package org.asymetrik.web.fairnsquare.backup.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * {@link S3ArchiveStore} against a real S3 API: Adobe's S3Mock in a container. Skipped without Docker.
 */
class S3ArchiveStoreTest {

    private static final String BUCKET = "backups";

    private static GenericContainer<?> s3;

    @TempDir
    Path folder;

    @BeforeAll
    static void startS3() {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "Docker is needed for the S3 server");
        s3 = new GenericContainer<>("adobe/s3mock:5.2.3").withExposedPorts(9090).waitingFor(Wait.forListeningPort());
        s3.start();
        try (S3Client client = S3Client.builder().httpClient(UrlConnectionHttpClient.create())
                .endpointOverride(endpoint()).region(Region.US_EAST_1).forcePathStyle(true)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("key", "secret")))
                .build()) {
            client.createBucket(request -> request.bucket(BUCKET));
        }
    }

    @AfterAll
    static void stopS3() {
        if (s3 != null) {
            s3.stop();
        }
    }

    private static URI endpoint() {
        return URI.create("http://" + s3.getHost() + ":" + s3.getMappedPort(9090));
    }

    private static S3ArchiveStore store(String prefix) {
        return new S3ArchiveStore(new BackupSettings(Optional.of(BUCKET), Optional.of(endpoint().toString()),
                "eu-west-1", true, Optional.of("key"), Optional.of("secret"), prefix, Optional.empty(), 30));
    }

    @Test
    void storesListsReadsAndDeletesArchivesUnderItsPrefix() throws IOException {
        S3ArchiveStore staging = store("staging");
        S3ArchiveStore production = store("production/");
        Path file = folder.resolve("archive");
        Files.writeString(file, "encrypted bytes");

        staging.put("a.fnsbackup", file);
        production.put("b.fnsbackup", file);

        assertThat(staging.list()).containsExactly(new ArchiveStore.StoredObject("a.fnsbackup", 15));
        try (InputStream in = staging.open("a.fnsbackup")) {
            assertThat(new String(in.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("encrypted bytes");
        }
        staging.delete("a.fnsbackup");
        assertThat(staging.list()).isEmpty();
        assertThat(production.list()).extracting(ArchiveStore.StoredObject::name).containsExactly("b.fnsbackup");
    }

    @Test
    void aMissingArchiveIsNotFound() {
        assertThatThrownBy(() -> store("staging/").open("missing.fnsbackup")).isInstanceOf(FileNotFoundException.class);
    }

    @Test
    void aMissingBucketFailsWithItsReason() {
        S3ArchiveStore store = new S3ArchiveStore(
                new BackupSettings(Optional.of("no-such-bucket"), Optional.of(endpoint().toString()), "eu-west-1", true,
                        Optional.of("key"), Optional.of("secret"), "", Optional.empty(), 30));

        assertThatThrownBy(store::list).isInstanceOf(IOException.class)
                .hasMessageStartingWith("Could not list the archives in the S3 bucket: ");
    }
}
