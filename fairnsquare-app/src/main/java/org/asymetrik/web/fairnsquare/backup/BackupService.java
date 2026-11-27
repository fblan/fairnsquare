package org.asymetrik.web.fairnsquare.backup;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.asymetrik.web.fairnsquare.backup.internal.ArchiveCipher;
import org.asymetrik.web.fairnsquare.backup.internal.ArchiveStore;
import org.asymetrik.web.fairnsquare.backup.internal.BackupSettings;
import org.asymetrik.web.fairnsquare.infrastructure.filesystem.FileSystemService;
import org.asymetrik.web.fairnsquare.infrastructure.filesystem.Filename;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Backs up all the splits to the bucket as encrypted archives, lists the archives, and compares one with the current
 * splits.
 * <p>
 * An archive, once decrypted ({@code scripts/DecryptBackup.java}), is a zip: {@code splits/<id>.zip}, the split files
 * as stored in the data directory, then {@code manifest.json}, the SHA-256 of each one.
 */
@ApplicationScoped
public class BackupService {

    private static final Logger LOG = Logger.getLogger(BackupService.class);

    private static final String SUFFIX = ".fnsbackup";
    private static final Pattern NAME = Pattern
            .compile("splits-\\d{4}-\\d{2}-\\d{2}T\\d{2}-\\d{2}-\\d{2}-\\d{3}Z\\.fnsbackup");
    private static final DateTimeFormatter NAME_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss-SSS'Z'");
    private static final String SPLITS_FOLDER = "splits/";
    private static final String SPLIT_EXTENSION = ".zip";
    private static final String MANIFEST = "manifest.json";
    private static final TypeReference<Manifest> MANIFEST_TYPE = new TypeReference<>() {
    };

    /** The archive's table of contents: the SHA-256 of each split file, by split id. */
    record Manifest(int format, Instant createdAt, Map<String, String> splits) {
    }

    private final BackupSettings settings;
    private final ArchiveStore store;
    private final FileSystemService files;
    private final ObjectMapper json;
    private final Clock clock;

    @Inject
    public BackupService(BackupSettings settings, ArchiveStore store, FileSystemService files, ObjectMapper json) {
        this(settings, store, files, json, Clock.systemUTC());
    }

    BackupService(BackupSettings settings, ArchiveStore store, FileSystemService files, ObjectMapper json,
            Clock clock) {
        this.settings = settings;
        this.store = store;
        this.files = files;
        this.json = json;
        this.clock = clock;
    }

    public BackupStatus status() {
        if (!settings.enabled()) {
            return new BackupStatus(false, null);
        }
        try {
            cipher();
            return new BackupStatus(true, null);
        } catch (BackupError e) {
            return new BackupStatus(true, e.getMessage());
        }
    }

    /** The archives, newest first. */
    public List<BackupArchive> archives() {
        cipher();
        try {
            return store.list().stream().filter(object -> NAME.matcher(object.name()).matches())
                    .map(object -> new BackupArchive(object.name(), createdAt(object.name()), object.sizeBytes()))
                    .sorted(Comparator.comparing(BackupArchive::createdAt).reversed()).toList();
        } catch (IOException e) {
            throw BackupError.failed(e.getMessage(), e);
        }
    }

    /** Makes an archive now, even if no split changed since the last one. */
    public BackupArchive backUpNow() {
        return backUp(true).orElseThrow();
    }

    /** Makes an archive if splits changed since the last one; the nightly backup. */
    public Optional<BackupArchive> backUpIfChanged() {
        return backUp(false);
    }

    private synchronized Optional<BackupArchive> backUp(boolean evenIfUnchanged) {
        ArchiveCipher cipher = cipher();
        if (!evenIfUnchanged && unchangedSinceLastArchive(cipher)) {
            LOG.info("Backup skipped: no split changed since the last archive.");
            return Optional.empty();
        }
        Instant now = clock.instant();
        String name = "splits-" + NAME_TIME.format(LocalDateTime.ofInstant(now, ZoneOffset.UTC)) + SUFFIX;
        Path archive = null;
        try {
            archive = Files.createTempFile("fairnsquare-backup-", SUFFIX);
            int count = write(cipher, archive, now);
            store.put(name, archive);
            long size = Files.size(archive);
            LOG.infof("Backup %s saved: %d splits, %d bytes.", name, count, size);
            prune();
            return Optional.of(new BackupArchive(name, now, size));
        } catch (IOException e) {
            LOG.errorf(e, "Backup %s failed.", name);
            throw BackupError.failed("The backup failed: " + e.getMessage(), e);
        } finally {
            if (archive != null) {
                try {
                    Files.deleteIfExists(archive);
                } catch (IOException e) {
                    LOG.warnf("Could not delete the temporary archive %s: %s", archive, e.getMessage());
                }
            }
        }
    }

    /** Compares the current splits with an archive's. */
    public BackupComparison compare(String name) {
        ArchiveCipher cipher = cipher();
        if (!NAME.matcher(name).matches()) {
            throw BackupError.archiveNotFound(name);
        }
        Map<String, String> archived = manifest(cipher, name).splits();
        Map<String, String> current = currentSplits();
        int added = 0;
        int changed = 0;
        int unchanged = 0;
        for (var split : current.entrySet()) {
            String archivedHash = archived.get(split.getKey());
            if (archivedHash == null) {
                added++;
            } else if (archivedHash.equals(split.getValue())) {
                unchanged++;
            } else {
                changed++;
            }
        }
        int deleted = (int) archived.keySet().stream().filter(id -> !current.containsKey(id)).count();
        return new BackupComparison(archived.size(), current.size(), added, deleted, changed, unchanged);
    }

    private ArchiveCipher cipher() {
        if (!settings.enabled()) {
            throw BackupError.unavailable("Backups are off: no S3 bucket is configured.");
        }
        try {
            return ArchiveCipher.fromBase64Key(settings.encryptionKey().orElse(""));
        } catch (IllegalArgumentException e) {
            throw BackupError.unavailable("BACKUP_ENCRYPTION_KEY "
                    + (settings.encryptionKey().isEmpty() ? "is missing: it must be 32 random bytes, base64-encoded."
                            : "is invalid: it must be 32 random bytes, base64-encoded."));
        }
    }

    private boolean unchangedSinceLastArchive(ArchiveCipher cipher) {
        List<BackupArchive> archives = archives();
        if (archives.isEmpty()) {
            return false;
        }
        try {
            return manifest(cipher, archives.getFirst().name()).splits().equals(currentSplits());
        } catch (BackupError e) {
            LOG.warnf("Could not read the last archive %s, backing up anyway: %s", archives.getFirst().name(),
                    e.getMessage());
            return false;
        }
    }

    /** The SHA-256 of each split file now, by split id. */
    private Map<String, String> currentSplits() {
        Map<String, String> splits = new TreeMap<>();
        for (String id : files.listAllSplitIds()) {
            files.readFile(new Filename(id + SPLIT_EXTENSION)).ifPresent(bytes -> splits.put(id, sha256(bytes)));
        }
        return splits;
    }

    /** Writes the encrypted archive of the current splits; returns how many it holds. */
    private int write(ArchiveCipher cipher, Path archive, Instant now) throws IOException {
        Map<String, String> written = new LinkedHashMap<>();
        try (OutputStream encrypted = cipher.encrypting(new BufferedOutputStream(Files.newOutputStream(archive)));
                ZipOutputStream zip = new ZipOutputStream(encrypted)) {
            for (String id : files.listAllSplitIds().stream().sorted().toList()) {
                Optional<byte[]> bytes = files.readFile(new Filename(id + SPLIT_EXTENSION));
                if (bytes.isEmpty()) {
                    continue; // deleted meanwhile
                }
                zip.putNextEntry(new ZipEntry(SPLITS_FOLDER + id + SPLIT_EXTENSION));
                zip.write(bytes.get());
                zip.closeEntry();
                written.put(id, sha256(bytes.get()));
            }
            zip.putNextEntry(new ZipEntry(MANIFEST));
            zip.write(json.writeValueAsBytes(new Manifest(1, now, written)));
            zip.closeEntry();
        }
        return written.size();
    }

    private Manifest manifest(ArchiveCipher cipher, String name) {
        try (InputStream in = store.open(name);
                ZipInputStream zip = new ZipInputStream(cipher.decrypting(new BufferedInputStream(in)))) {
            Manifest manifest = null;
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                if (MANIFEST.equals(entry.getName())) {
                    manifest = json.readValue(zip.readAllBytes(), MANIFEST_TYPE);
                }
            }
            if (manifest == null) {
                throw new IOException("it has no manifest");
            }
            return manifest;
        } catch (FileNotFoundException e) {
            throw BackupError.archiveNotFound(name);
        } catch (IOException e) {
            throw BackupError.failed("Could not read the archive " + name + ": " + e.getMessage(), e);
        }
    }

    /** Deletes the oldest archives beyond the configured number; a failure only logs. */
    private void prune() {
        if (settings.keep() <= 0) {
            return;
        }
        try {
            List<BackupArchive> archives = archives();
            for (BackupArchive old : archives.subList(Math.min(settings.keep(), archives.size()), archives.size())) {
                store.delete(old.name());
                LOG.infof("Old backup %s deleted.", old.name());
            }
        } catch (IOException | BackupError e) {
            LOG.warnf("Could not delete the old backups: %s", e.getMessage());
        }
    }

    private static Instant createdAt(String name) {
        String time = name.substring("splits-".length(), name.length() - SUFFIX.length());
        return LocalDateTime.parse(time, NAME_TIME).toInstant(ZoneOffset.UTC);
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
