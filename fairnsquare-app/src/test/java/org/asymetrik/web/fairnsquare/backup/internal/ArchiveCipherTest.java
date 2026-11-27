package org.asymetrik.web.fairnsquare.backup.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.Random;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

class ArchiveCipherTest {

    static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);
    private static final int CHUNK = ArchiveCipher.CHUNK_SIZE;

    private final ArchiveCipher cipher = ArchiveCipher.fromBase64Key(KEY);

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, CHUNK - 1, CHUNK, CHUNK + 1, 3 * CHUNK, 3 * CHUNK + 17 })
    void decryptsWhatItEncrypted(int size) throws IOException {
        byte[] plain = randomBytes(size);

        byte[] encrypted = encrypt(cipher, plain);

        assertThat(decrypt(cipher, encrypted)).isEqualTo(plain);
        assertThat(encrypted).hasSize(13 + size + 16 * (size / CHUNK + 1));
    }

    @Test
    void encryptsTheSameDataDifferentlyEachTime() throws IOException {
        byte[] plain = randomBytes(100);

        assertThat(encrypt(cipher, plain)).isNotEqualTo(encrypt(cipher, plain));
    }

    @Test
    void refusesAnotherKey() throws IOException {
        byte[] encrypted = encrypt(cipher, randomBytes(100));
        byte[] otherKey = new byte[32];
        otherKey[0] = 1;
        ArchiveCipher other = ArchiveCipher.fromBase64Key(Base64.getEncoder().encodeToString(otherKey));

        assertThatThrownBy(() -> decrypt(other, encrypted)).isInstanceOf(IOException.class)
                .hasMessageContaining("Wrong backup key");
    }

    @Test
    void refusesAnAlteredArchive() throws IOException {
        byte[] encrypted = encrypt(cipher, randomBytes(2 * CHUNK));
        encrypted[CHUNK + 100] ^= 1;

        assertThatThrownBy(() -> decrypt(cipher, encrypted)).isInstanceOf(IOException.class);
    }

    @Test
    void refusesAnArchiveWithoutItsLastChunk() throws IOException {
        // Two full chunks, then the empty last one (its tag only): dropping it must be noticed.
        byte[] encrypted = encrypt(cipher, randomBytes(2 * CHUNK));
        byte[] truncated = Arrays.copyOf(encrypted, encrypted.length - 16);

        assertThatThrownBy(() -> decrypt(cipher, truncated)).isInstanceOf(IOException.class)
                .hasMessageContaining("truncated");
    }

    @Test
    void refusesAnArchiveCutInAChunk() throws IOException {
        byte[] encrypted = encrypt(cipher, randomBytes(CHUNK + 500));
        byte[] truncated = Arrays.copyOf(encrypted, encrypted.length - 200);

        assertThatThrownBy(() -> decrypt(cipher, truncated)).isInstanceOf(IOException.class);
    }

    @Test
    void refusesDataAfterTheEnd() throws IOException {
        byte[] encrypted = encrypt(cipher, randomBytes(10));
        // A byte more makes the last chunk's tag wrong; a whole chunk more comes after the last chunk.
        byte[] oneMore = Arrays.copyOf(encrypted, encrypted.length + 1);
        byte[] chunkMore = Arrays.copyOf(encrypted, encrypted.length + CHUNK + 16);

        assertThatThrownBy(() -> decrypt(cipher, oneMore)).isInstanceOf(IOException.class);
        assertThatThrownBy(() -> decrypt(cipher, chunkMore)).isInstanceOf(IOException.class);
    }

    @Test
    void refusesWhatIsNotAnArchive() {
        assertThatThrownBy(() -> decrypt(cipher, "PK\u0003\u0004 a zip".getBytes())).isInstanceOf(IOException.class)
                .hasMessageContaining("Not a FairNSquare backup archive");
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "not base64!", "c2hvcnQ=" })
    void wantsA32ByteBase64Key(String key) {
        assertThatThrownBy(() -> ArchiveCipher.fromBase64Key(key)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The backup key must be 32 random bytes, base64-encoded.");
    }

    static byte[] encrypt(ArchiveCipher cipher, byte[] plain) throws IOException {
        ByteArrayOutputStream encrypted = new ByteArrayOutputStream();
        try (OutputStream out = cipher.encrypting(encrypted)) {
            // Uneven writes, to cross the chunk boundaries anywhere.
            for (int offset = 0; offset < plain.length; offset += 1000) {
                out.write(plain, offset, Math.min(1000, plain.length - offset));
            }
        }
        return encrypted.toByteArray();
    }

    static byte[] decrypt(ArchiveCipher cipher, byte[] encrypted) throws IOException {
        try (InputStream in = cipher.decrypting(new ByteArrayInputStream(encrypted))) {
            return in.readAllBytes();
        }
    }

    static byte[] randomBytes(int size) {
        byte[] bytes = new byte[size];
        new Random(size).nextBytes(bytes);
        return bytes;
    }
}
