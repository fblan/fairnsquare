# Encrypted backups of the splits to an S3 bucket

Issue #195.

## What, Why and Constraints

All the splits are backed up to an external S3 bucket, encrypted with a key we provide. In admin, the archives are
listed and one can be compared with the current state. Until now, the splits only lived on the server's disk.

Choices:

- **Daily, skipped when unchanged:** a nightly backup at 3 am (`BACKUP_CRON`). It makes no archive when no split
  changed since the last one. Admin also has a "Back up now" button, which always makes an archive.
- **Retention:** the 30 most recent archives are kept (`BACKUP_KEEP`; 0 keeps them all). Older ones are deleted after
  each backup.
- **Comparison:** totals only. Splits new, deleted, changed and unchanged since the archive.

Constraints:

- **Encryption on the server, with the provided key:** the bucket never sees plaintext, not even metadata.
  `BACKUP_ENCRYPTION_KEY` is 32 random bytes in base64, used directly as the AES-256 key, so there's no key derivation.
- **Readable without the app:** `scripts/DecryptBackup.java` decrypts an archive. It is a single Java source file with
  no dependencies.
- **Streaming:** the data can reach 5000 files of 512 KB, so archives are written and read as streams, never held in
  memory. Writing goes through a temporary file, which S3 needs for the size.
- **Optional:** without `BACKUP_S3_BUCKET` nothing runs, and the app starts whatever the backup configuration.
- **Any S3 provider:** AWS, or S3-compatible ones (OVH, Scaleway, MinIO…) through the endpoint and path-style access.

## How

**Dependencies**

- AWS SDK v2 `s3` (BOM `2.57.13`), on the JDK-based `url-connection-client`. The Netty and Apache HTTP clients are
  excluded, which adds 8.5 MB of jars.
- The SDK's default CRC checksums are turned off (`WHEN_REQUIRED`), because some S3-compatible providers refuse them.
- Test only: Testcontainers, for an S3 server in Docker.

**Module `backup`** (`org.asymetrik.web.fairnsquare.backup`)

- `internal/ArchiveCipher` encrypts with AES-256-GCM in chunks of 64 KiB (an "online AE" construction like age's
  STREAM):
  - The header is `FNSB`, version 1, and an 8-byte random nonce prefix.
  - Each chunk's IV is the prefix plus the chunk index. The additional data is the header plus a "last chunk" flag.
  - Every chunk but the last is full.
  - So a wrong key, an altered byte, reordered chunks, a truncation (even at a chunk boundary) or extra data all fail.
  - It exposes `encrypting(OutputStream)` and `decrypting(InputStream)`.
- `internal/BackupSettings` holds `fairnsquare.backup.*`, from the `BACKUP_*` variables. A bucket turns backups on.
- `internal/ArchiveStore` is the interface for objects under a prefix (list, put, open, delete).
- `internal/S3ArchiveStore` implements it with the SDK. The client is created on first use. The prefix lets staging and
  production share a bucket.
- `BackupService` (exported):
  - `backUpNow()` / `backUpIfChanged()`:
    - It hashes each split file (SHA-256).
    - Unless forced, it compares the hashes with the manifest of the newest archive. If they're equal, it skips. If
      that archive can't be read, it backs up anyway.
    - It writes the plaintext zip straight into the cipher: `splits/<id>.zip`, the raw split files, then
      `manifest.json` (`{format, createdAt, splits: {id: sha256}}`).
    - It uploads the result as `splits-<yyyy-MM-dd'T'HH-mm-ss-SSS'Z'>.fnsbackup`, then prunes beyond `keep`. A pruning
      failure only logs.
    - It is `synchronized`, so the button and the nightly job never overlap.
  - `archives()`: the objects matching the name pattern, newest first, with the date taken from the name.
  - `compare(name)`: decrypts the archive's manifest and counts added, deleted, changed and unchanged against the
    current hashes. Names are checked against the pattern, so no other key can be read.
  - `status()`: off, or on with a problem when the key is missing or invalid.
  - Errors are a `BackupError` (problem details): 409 when off or misconfigured, 404 for an unknown archive, 502 when
    the bucket or the archive fails.
- `internal/BackupScheduler`: `@Scheduled(cron = "{fairnsquare.backup.cron}", concurrentExecution = SKIP)` calls
  `backUpIfChanged()` when backups are on, and logs failures.

**Admin**

- `AdminResource`, behind the admin sign-in (password or OIDC):
  - `GET /api/admin/backups` returns `{enabled, problem, archives}`. A failure to list the archives (e.g. the bucket is
    unreachable) becomes the `problem`, so the page shows it.
  - `POST /api/admin/backups` backs up now and returns 201 with the archive.
  - `GET /api/admin/backups/{name}/comparison` returns the totals.
- `admin-page.js` gets a new "Backups" section, loaded after the stats:
  - a status line (off / problem / "N archives in the bucket.") and the "Back up now" button, with a toast on success
    or failure;
  - the archives table (date, size, "Compare");
  - the comparison: New / Deleted / Changed / Unchanged, plus "N splits in the archive, M now."
- The admin module's `requires` add `backup` and `error`.

**Other**

- The README documents the variables, backups and restoring.
- `scripts/hash-password.sh --secret` now says it also makes a `BACKUP_ENCRYPTION_KEY`.

## Tests

- `ArchiveCipherTest` (17 tests):
  - round trips at sizes around the chunk boundaries;
  - random IVs;
  - refusals: wrong key, altered data, a missing last chunk, a cut in a chunk, extra data, not an archive, bad keys.
- `DecryptScriptTest`: runs `scripts/DecryptBackup.java` with the test JVM. It decrypts what the app encrypts. An
  altered archive fails and leaves no output file.
- `BackupServiceTest` (12 tests), with an in-memory bucket and a real `FileSystemService` on a temporary folder:
  - the archive contents are encrypted;
  - listing order;
  - skip when unchanged, and backup anyway when the last archive can't be read;
  - retention, and keep 0;
  - the comparison totals;
  - unknown and unsafe names;
  - another key;
  - a bucket failure;
  - off and key problems.
- `S3ArchiveStoreTest`: put, list, open and delete under two prefixes, a missing object, and a missing bucket, against
  Adobe's S3Mock (`adobe/s3mock:5.2.3`) in Docker. It is skipped without Docker. MinIO's images are no longer published.
- `AdminBackupsResourceTest` (profile `AdminBackupsProfile`: a bucket and a key, with `InMemoryArchiveStore` as a CDI
  alternative):
  - back up, list, compare;
  - an unreachable bucket shows as the problem, and the backup fails with 502;
  - an unknown archive gives 404;
  - admins only.
- `AdminBackupsOffTest`: without a bucket, backups are off and POST gives 409.
- Playwright:
  - `AdminBackupsPageTest`: back up, then compare after creating a split (New 1); an unreachable bucket shows its
    reason, and the backup's toast does too.
  - `AdminPageTest.saysWhenBackupsAreOff`.
- Manual check with the packaged app against S3Mock:
  - a backup;
  - the scheduled run every 20 s skipping, unchanged;
  - the archive downloaded from the bucket starts with `FNSB` and decrypts with the script into the split files and
    the manifest;
  - the comparison after changing one split and adding another gives `added 1, changed 1`.
- Screenshots of the Backups section at 390×844 and 1280×800.
- Full build: 509 tests.
