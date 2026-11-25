/*
 * Decrypts a FairNSquare backup archive (downloaded from the S3 bucket) into a zip of the splits.
 *
 * Usage (Java 25+, nothing to install):
 *   java scripts/DecryptBackup.java <archive.fnsbackup> <output.zip>
 *
 * The key is read from the BACKUP_ENCRYPTION_KEY environment variable, or asked for (hidden input) when it isn't set.
 *
 * The output zip holds splits/<id>.zip, the split files as stored in the data directory (copy them back into
 * <data>/default/ to restore splits), and manifest.json, the SHA-256 of each one.
 *
 * Format: see ArchiveCipher in the application. A wrong key, or a truncated or altered archive, fails with an error
 * and leaves no output file.
 */

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

void main(String[] args) throws Exception {
    if (args.length != 2) {
        System.err.println("Usage: java scripts/DecryptBackup.java <archive.fnsbackup> <output.zip>");
        System.exit(2);
    }
    String key = System.getenv("BACKUP_ENCRYPTION_KEY");
    if (key == null || key.isBlank()) {
        Console console = System.console();
        if (console == null) {
            System.err.println("Set BACKUP_ENCRYPTION_KEY.");
            System.exit(2);
        }
        key = new String(console.readPassword("Backup key: "));
    }
    byte[] keyBytes;
    try {
        keyBytes = Base64.getDecoder().decode(key.trim());
    } catch (IllegalArgumentException e) {
        keyBytes = new byte[0];
    }
    if (keyBytes.length != 32) {
        System.err.println("The backup key must be 32 random bytes, base64-encoded.");
        System.exit(2);
    }
    Path output = Path.of(args[1]);
    Path partial = output.resolveSibling(output.getFileName() + ".part");
    try (InputStream in = new BufferedInputStream(Files.newInputStream(Path.of(args[0])));
            OutputStream out = new BufferedOutputStream(Files.newOutputStream(partial))) {
        decrypt(new SecretKeySpec(keyBytes, "AES"), in, out);
    } catch (IOException e) {
        Files.deleteIfExists(partial);
        System.err.println("Decryption failed: " + e.getMessage());
        System.exit(1);
    }
    Files.move(partial, output, StandardCopyOption.REPLACE_EXISTING);
    System.out.println("Decrypted into " + output);
}

static final byte[] MAGIC = { 'F', 'N', 'S', 'B' };
static final int CHUNK_SIZE = 64 * 1024;
static final int TAG_BYTES = 16;
static final int HEADER_BYTES = 13;

static void decrypt(SecretKeySpec key, InputStream in, OutputStream out) throws Exception {
    byte[] header = in.readNBytes(HEADER_BYTES);
    if (header.length != HEADER_BYTES || !Arrays.equals(Arrays.copyOf(header, 4), MAGIC)) {
        throw new IOException("not a FairNSquare backup archive");
    }
    if (header[4] != 1) {
        throw new IOException("unsupported archive version " + header[4]);
    }
    for (int index = 0;; index++) {
        byte[] chunk = in.readNBytes(CHUNK_SIZE + TAG_BYTES);
        if (chunk.length < TAG_BYTES) {
            throw new IOException("the archive is truncated");
        }
        boolean last = chunk.length < CHUNK_SIZE + TAG_BYTES;
        byte[] iv = ByteBuffer.allocate(12).put(header, 5, 8).putInt(index).array();
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BYTES * 8, iv));
        cipher.updateAAD(header);
        cipher.updateAAD(new byte[] { (byte) (last ? 1 : 0) });
        try {
            out.write(cipher.doFinal(chunk));
        } catch (AEADBadTagException e) {
            throw new IOException("wrong key, or the archive is truncated or altered");
        }
        if (last) {
            if (in.read() != -1) {
                throw new IOException("the archive has data after its end");
            }
            return;
        }
    }
}
