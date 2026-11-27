package org.asymetrik.web.fairnsquare.backup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.asymetrik.web.fairnsquare.backup.internal.ArchiveCipher;
import org.asymetrik.web.fairnsquare.backup.internal.BackupSettings;
import org.asymetrik.web.fairnsquare.backup.internal.InMemoryArchiveStore;
import org.asymetrik.web.fairnsquare.infrastructure.filesystem.FileSystemService;
import org.asymetrik.web.fairnsquare.infrastructure.filesystem.Filename;
import org.asymetrik.web.fairnsquare.infrastructure.filesystem.internal.TenantPathResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

class BackupServiceTest {

    private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);

    @TempDir
    Path data;

    private final InMemoryArchiveStore store = new InMemoryArchiveStore();
    private final MovingClock clock = new MovingClock(Instant.parse("2026-11-25T03:00:00Z"));
    private FileSystemService files;
    private BackupService backups;

    @BeforeEach
    void setUp() {
        TenantPathResolver resolver = new TenantPathResolver();
        resolver.dataPath = data.toString();
        files = new FileSystemService(resolver, 100, 1024 * 1024, 90);
        backups = service(settings("my-bucket", KEY, 3));
    }

    @Test
    void anArchiveHoldsTheSplitFilesEncrypted() throws IOException {
        saveSplit("alpha", "first split");
        saveSplit("beta", "second split");

        BackupArchive archive = backups.backUpNow();

        assertThat(archive.name()).isEqualTo("splits-2026-11-25T03-00-00-000Z.fnsbackup");
        assertThat(archive.createdAt()).isEqualTo(clock.instant());
        byte[] stored = store.objects().get(archive.name());
        assertThat(archive.sizeBytes()).isEqualTo(stored.length);
        assertThat(new String(stored, StandardCharsets.ISO_8859_1)).doesNotContain("first split", "alpha");
        Map<String, String> entries = unzip(
                ArchiveCipher.fromBase64Key(KEY).decrypting(new ByteArrayInputStream(stored)));
        assertThat(entries).containsOnlyKeys("splits/alpha.zip", "splits/beta.zip", "manifest.json");
        assertThat(entries.get("splits/alpha.zip")).isEqualTo("first split");
        assertThat(entries.get("manifest.json")).contains("\"alpha\"", "\"beta\"");
    }

    @Test
    void listsTheArchivesNewestFirst() {
        saveSplit("alpha", "v1");
        BackupArchive first = backups.backUpNow();
        clock.advance(Duration.ofDays(1));
        BackupArchive second = backups.backUpNow();
        store.objects().put("not-an-archive.txt", new byte[1]);

        assertThat(backups.archives()).containsExactly(second, first);
    }

    @Test
    void theNightlyBackupIsSkippedWhenNoSplitChanged() {
        saveSplit("alpha", "v1");
        assertThat(backups.backUpIfChanged()).isPresent();
        clock.advance(Duration.ofDays(1));

        assertThat(backups.backUpIfChanged()).isEmpty();

        saveSplit("alpha", "v2");
        clock.advance(Duration.ofDays(1));
        assertThat(backups.backUpIfChanged()).isPresent();
        assertThat(backups.archives()).hasSize(2);
    }

    @Test
    void theNightlyBackupRunsWhenTheLastArchiveCantBeRead() {
        saveSplit("alpha", "v1");
        BackupArchive archive = backups.backUpNow();
        store.objects().put(archive.name(), "garbage".getBytes(StandardCharsets.UTF_8));
        clock.advance(Duration.ofDays(1));

        assertThat(backups.backUpIfChanged()).isPresent();
    }

    @Test
    void keepsTheConfiguredNumberOfArchives() {
        saveSplit("alpha", "v1");
        for (int day = 0; day < 5; day++) {
            backups.backUpNow();
            clock.advance(Duration.ofDays(1));
        }

        assertThat(backups.archives()).extracting(BackupArchive::name).containsExactly(
                "splits-2026-11-29T03-00-00-000Z.fnsbackup", "splits-2026-11-28T03-00-00-000Z.fnsbackup",
                "splits-2026-11-27T03-00-00-000Z.fnsbackup");
    }

    @Test
    void keepsEveryArchiveWithKeep0() {
        backups = service(settings("my-bucket", KEY, 0));
        for (int day = 0; day < 5; day++) {
            backups.backUpNow();
            clock.advance(Duration.ofDays(1));
        }

        assertThat(backups.archives()).hasSize(5);
    }

    @Test
    void comparesTheCurrentSplitsWithAnArchive() {
        saveSplit("kept", "same");
        saveSplit("edited", "before");
        saveSplit("removed", "gone soon");
        BackupArchive archive = backups.backUpNow();
        saveSplit("edited", "after");
        files.deleteFile(new Filename("removed.zip"));
        saveSplit("created", "new");

        assertThat(backups.compare(archive.name())).isEqualTo(new BackupComparison(3, 3, 1, 1, 1, 1));
    }

    @Test
    void anUnknownArchiveIsNotFound() {
        assertThatThrownBy(() -> backups.compare("splits-2020-01-01T00-00-00-000Z.fnsbackup"))
                .isInstanceOf(BackupError.class).extracting("status").isEqualTo(404);
        assertThatThrownBy(() -> backups.compare("../secret")).isInstanceOf(BackupError.class).extracting("status")
                .isEqualTo(404);
    }

    @Test
    void anArchiveEncryptedWithAnotherKeyCantBeCompared() {
        BackupArchive archive = backups.backUpNow();
        byte[] otherKey = new byte[32];
        otherKey[31] = 7;
        backups = service(settings("my-bucket", Base64.getEncoder().encodeToString(otherKey), 3));

        assertThatThrownBy(() -> backups.compare(archive.name())).isInstanceOf(BackupError.class)
                .hasMessageContaining("Wrong backup key").extracting("status").isEqualTo(502);
    }

    @Test
    void aBucketFailureFailsTheBackup() {
        store.failWith(new IOException("Could not upload: Access Denied"));

        assertThatThrownBy(() -> backups.backUpNow()).isInstanceOf(BackupError.class)
                .hasMessageContaining("Access Denied").extracting("status").isEqualTo(502);
    }

    @Test
    void isOffWithoutABucket() {
        backups = service(settings("", KEY, 3));

        assertThat(backups.status()).isEqualTo(new BackupStatus(false, null));
        assertThatThrownBy(() -> backups.backUpNow()).isInstanceOf(BackupError.class)
                .hasMessage("Backups are off: no S3 bucket is configured.").extracting("status").isEqualTo(409);
    }

    @Test
    void saysWhatIsWrongWithTheKey() {
        assertThat(service(settings("my-bucket", "", 3)).status().problem())
                .isEqualTo("BACKUP_ENCRYPTION_KEY is missing: it must be 32 random bytes, base64-encoded.");
        assertThat(service(settings("my-bucket", "too short", 3)).status().problem())
                .isEqualTo("BACKUP_ENCRYPTION_KEY is invalid: it must be 32 random bytes, base64-encoded.");
        assertThat(backups.status()).isEqualTo(new BackupStatus(true, null));
    }

    private BackupService service(BackupSettings settings) {
        ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule());
        return new BackupService(settings, store, files, json, clock);
    }

    private static BackupSettings settings(String bucket, String key, int keep) {
        return new BackupSettings(Optional.of(bucket), Optional.empty(), "us-east-1", false, Optional.empty(),
                Optional.empty(), "fairnsquare/", Optional.of(key), keep);
    }

    private void saveSplit(String id, String content) {
        files.saveFile(new Filename(id + ".zip"), content.getBytes(StandardCharsets.UTF_8));
    }

    private static Map<String, String> unzip(InputStream in) throws IOException {
        Map<String, String> entries = new TreeMap<>();
        try (ZipInputStream zip = new ZipInputStream(in)) {
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                entries.put(entry.getName(), new String(zip.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        return entries;
    }

    private static final class MovingClock extends Clock {

        private Instant now;

        MovingClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
