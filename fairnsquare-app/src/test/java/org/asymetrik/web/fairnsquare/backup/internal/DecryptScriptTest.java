package org.asymetrik.web.fairnsquare.backup.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * {@code scripts/DecryptBackup.java}, run with the Java that runs the tests, reads what {@link ArchiveCipher} writes.
 */
class DecryptScriptTest {

    private static final Path SCRIPT = Path.of("..", "scripts", "DecryptBackup.java").toAbsolutePath().normalize();

    @TempDir
    Path folder;

    @Test
    void decryptsAnArchive() throws Exception {
        byte[] plain = ArchiveCipherTest.randomBytes(3 * ArchiveCipher.CHUNK_SIZE + 5);
        Path archive = folder.resolve("a.fnsbackup");
        Files.write(archive, ArchiveCipherTest.encrypt(ArchiveCipher.fromBase64Key(ArchiveCipherTest.KEY), plain));
        Path output = folder.resolve("a.zip");

        Result result = run(ArchiveCipherTest.KEY, archive, output);

        assertThat(result.exitCode()).as(result.output()).isZero();
        assertThat(Files.readAllBytes(output)).isEqualTo(plain);
    }

    @Test
    void failsWithoutOutputOnAnAlteredArchive() throws Exception {
        byte[] encrypted = ArchiveCipherTest.encrypt(ArchiveCipher.fromBase64Key(ArchiveCipherTest.KEY),
                ArchiveCipherTest.randomBytes(100));
        Path archive = folder.resolve("a.fnsbackup");
        Files.write(archive, Arrays.copyOf(encrypted, encrypted.length - 1));
        Path output = folder.resolve("a.zip");

        Result result = run(ArchiveCipherTest.KEY, archive, output);

        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(result.output()).contains("Decryption failed: wrong key, or the archive is truncated or altered");
        assertThat(output).doesNotExist();
        assertThat(folder.resolve("a.zip.part")).doesNotExist();
    }

    private record Result(int exitCode, String output) {
    }

    private Result run(String key, Path archive, Path output) throws IOException, InterruptedException {
        Path java = Path.of(System.getProperty("java.home"), "bin", "java");
        ProcessBuilder builder = new ProcessBuilder(java.toString(), SCRIPT.toString(), archive.toString(),
                output.toString()).redirectErrorStream(true);
        builder.environment().put("BACKUP_ENCRYPTION_KEY", key);
        Process process = builder.start();
        String text = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        return new Result(process.waitFor(), text);
    }
}
