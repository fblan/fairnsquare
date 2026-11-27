package org.asymetrik.web.fairnsquare.backup.internal;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Encrypts and decrypts backup archives with AES-256-GCM, in chunks, so that an archive of any size streams through
 * without being held in memory.
 * <p>
 * Format (also implemented by {@code scripts/DecryptBackup.java}):
 * <ul>
 * <li>Header, 13 bytes: {@code FNSB}, version {@code 1}, then an 8-byte random nonce prefix.</li>
 * <li>Chunks: each plaintext chunk of at most {@value #CHUNK_SIZE} bytes is encrypted with AES-GCM (128-bit tag). The
 * IV is the nonce prefix followed by the chunk's index (4 bytes, big endian); the additional data is the header
 * followed by 1 for the last chunk, 0 for the others.</li>
 * <li>Every chunk but the last holds exactly {@value #CHUNK_SIZE} plaintext bytes; the last one holds fewer, possibly
 * none. A truncated, reordered or altered archive, or the wrong key, fails to decrypt.</li>
 * </ul>
 */
public final class ArchiveCipher {

    static final byte[] MAGIC = { 'F', 'N', 'S', 'B' };
    static final byte VERSION = 1;
    static final int CHUNK_SIZE = 64 * 1024;
    private static final int TAG_BYTES = 16;
    private static final int PREFIX_BYTES = 8;
    private static final int HEADER_BYTES = MAGIC.length + 1 + PREFIX_BYTES;
    private static final int KEY_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SecretKey key;

    private ArchiveCipher(byte[] key) {
        this.key = new SecretKeySpec(key, "AES");
    }

    /**
     * @param base64Key
     *            32 random bytes, base64-encoded (e.g. {@code openssl rand -base64 32})
     *
     * @throws IllegalArgumentException
     *             if the key isn't 32 bytes of base64
     */
    public static ArchiveCipher fromBase64Key(String base64Key) {
        byte[] key;
        try {
            key = Base64.getDecoder().decode(base64Key == null ? "" : base64Key.trim());
        } catch (IllegalArgumentException e) {
            key = new byte[0];
        }
        if (key.length != KEY_BYTES) {
            throw new IllegalArgumentException("The backup key must be 32 random bytes, base64-encoded.");
        }
        return new ArchiveCipher(key);
    }

    /**
     * An output stream that encrypts what is written to it into {@code encrypted}. Closing it writes the last chunk and
     * closes {@code encrypted}.
     */
    public OutputStream encrypting(OutputStream encrypted) throws IOException {
        byte[] header = new byte[HEADER_BYTES];
        System.arraycopy(MAGIC, 0, header, 0, MAGIC.length);
        header[MAGIC.length] = VERSION;
        byte[] prefix = new byte[PREFIX_BYTES];
        RANDOM.nextBytes(prefix);
        System.arraycopy(prefix, 0, header, MAGIC.length + 1, PREFIX_BYTES);
        encrypted.write(header);
        return new EncryptingStream(header, encrypted);
    }

    /**
     * An input stream of the plaintext of the archive {@code encrypted} gives. Reading fails with an
     * {@link IOException} if it isn't a backup archive, is truncated or altered, or was encrypted with another key.
     */
    public InputStream decrypting(InputStream encrypted) throws IOException {
        byte[] header = encrypted.readNBytes(HEADER_BYTES);
        if (header.length != HEADER_BYTES || !Arrays.equals(Arrays.copyOf(header, MAGIC.length), MAGIC)) {
            throw new IOException("Not a FairNSquare backup archive.");
        }
        if (header[MAGIC.length] != VERSION) {
            throw new IOException("Unsupported backup archive version " + header[MAGIC.length] + ".");
        }
        return new DecryptingStream(header, encrypted);
    }

    private final class EncryptingStream extends OutputStream {

        private final byte[] header;
        private final OutputStream out;
        private final byte[] chunk = new byte[CHUNK_SIZE];
        private int length;
        private int index;
        private boolean closed;

        EncryptingStream(byte[] header, OutputStream out) {
            this.header = header;
            this.out = out;
        }

        @Override
        public void write(int b) throws IOException {
            write(new byte[] { (byte) b }, 0, 1);
        }

        @Override
        public void write(byte[] data, int offset, int count) throws IOException {
            if (closed) {
                throw new IOException("Stream closed.");
            }
            while (count > 0) {
                // A full chunk is only written once more data comes: the last chunk must be the short one.
                if (length == CHUNK_SIZE) {
                    out.write(crypt(Cipher.ENCRYPT_MODE, header, index++, false, chunk, CHUNK_SIZE));
                    length = 0;
                }
                int copied = Math.min(count, CHUNK_SIZE - length);
                System.arraycopy(data, offset, chunk, length, copied);
                length += copied;
                offset += copied;
                count -= copied;
            }
        }

        @Override
        public void close() throws IOException {
            if (closed) {
                return;
            }
            closed = true;
            if (length == CHUNK_SIZE) {
                out.write(crypt(Cipher.ENCRYPT_MODE, header, index++, false, chunk, CHUNK_SIZE));
                length = 0;
            }
            out.write(crypt(Cipher.ENCRYPT_MODE, header, index, true, chunk, length));
            out.close();
        }
    }

    private final class DecryptingStream extends InputStream {

        private final byte[] header;
        private final InputStream in;
        private byte[] plain = new byte[0];
        private int position;
        private int index;
        private boolean ended;

        DecryptingStream(byte[] header, InputStream in) {
            this.header = header;
            this.in = in;
        }

        @Override
        public int read() throws IOException {
            byte[] one = new byte[1];
            return read(one, 0, 1) == -1 ? -1 : one[0] & 0xff;
        }

        @Override
        public int read(byte[] buffer, int offset, int count) throws IOException {
            if (count == 0) {
                return 0;
            }
            while (position == plain.length) {
                if (ended) {
                    return -1;
                }
                nextChunk();
            }
            int copied = Math.min(count, plain.length - position);
            System.arraycopy(plain, position, buffer, offset, copied);
            position += copied;
            return copied;
        }

        private void nextChunk() throws IOException {
            byte[] chunk = in.readNBytes(CHUNK_SIZE + TAG_BYTES);
            if (chunk.length < TAG_BYTES) {
                throw new EOFException("The backup archive is truncated.");
            }
            boolean last = chunk.length < CHUNK_SIZE + TAG_BYTES;
            plain = crypt(Cipher.DECRYPT_MODE, header, index++, last, chunk, chunk.length);
            position = 0;
            if (last) {
                if (in.read() != -1) {
                    throw new IOException("The backup archive has data after its end.");
                }
                ended = true;
            }
        }

        @Override
        public void close() throws IOException {
            in.close();
        }
    }

    private byte[] crypt(int mode, byte[] header, int index, boolean last, byte[] data, int length) throws IOException {
        try {
            byte[] iv = ByteBuffer.allocate(12).put(header, MAGIC.length + 1, PREFIX_BYTES).putInt(index).array();
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(mode, key, new GCMParameterSpec(TAG_BYTES * 8, iv));
            cipher.updateAAD(header);
            cipher.updateAAD(new byte[] { (byte) (last ? 1 : 0) });
            return cipher.doFinal(data, 0, length);
        } catch (javax.crypto.AEADBadTagException e) {
            throw new IOException("Wrong backup key, or the archive is truncated or altered.", e);
        } catch (GeneralSecurityException e) {
            throw new IOException("Backup archive encryption failed.", e);
        }
    }
}
