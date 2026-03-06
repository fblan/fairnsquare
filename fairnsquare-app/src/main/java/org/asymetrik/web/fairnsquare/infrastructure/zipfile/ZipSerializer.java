package org.asymetrik.web.fairnsquare.infrastructure.zipfile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import jakarta.enterprise.context.ApplicationScoped;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Converts between raw data bytes and a ZIP archive containing a {@code metadata.json} entry and a {@code data.bin}
 * entry. Pure byte transformation — no file system access.
 * <p>
 * Format:
 * <ul>
 * <li>{@code metadata.json} — {@link ZipMetadata} with format version and deserializer code</li>
 * <li>{@code data.bin} — the caller-provided data bytes</li>
 * </ul>
 */
@ApplicationScoped
public class ZipSerializer {

    public static final String METADATA_ENTRY = "metadata.json";
    public static final String DATA_ENTRY = "data.bin";

    private final ObjectMapper objectMapper;

    public ZipSerializer() {
        this.objectMapper = createObjectMapper();
    }

    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return mapper;
    }

    /**
     * Wraps {@code data} in a ZIP archive using the current format version.
     *
     * @param data
     *            the bytes to store as {@code data.bin}
     *
     * @return the ZIP archive bytes
     *
     * @throws ZipException
     *             if archive creation fails
     */
    public byte[] toZip(byte[] data) {
        return toZip(data, ZipMetadata.CURRENT_VERSION);
    }

    /**
     * Wraps {@code data} in a ZIP archive with an explicit format version.
     *
     * @param data
     *            the bytes to store as {@code data.bin}
     * @param version
     *            the format version to record in {@code metadata.json}
     *
     * @return the ZIP archive bytes
     *
     * @throws ZipException
     *             if archive creation fails
     */
    public byte[] toZip(byte[] data, String version) {
        try {
            byte[] metadataBytes = objectMapper
                    .writeValueAsBytes(new ZipMetadata(version, ZipMetadata.CLEAR_DESERIALIZER));

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                zos.putNextEntry(new ZipEntry(METADATA_ENTRY));
                zos.write(metadataBytes);
                zos.closeEntry();

                zos.putNextEntry(new ZipEntry(DATA_ENTRY));
                zos.write(data);
                zos.closeEntry();
            }
            return baos.toByteArray();
        } catch (IOException e) {
            throw new ZipException("Failed to create ZIP archive", e);
        }
    }

    /**
     * Extracts the {@code data.bin} bytes from a ZIP archive produced by {@link #toZip}.
     *
     * @param zipBytes
     *            the ZIP archive bytes
     *
     * @return the extracted data bytes
     *
     * @throws ZipException
     *             if the archive is malformed, missing required entries, or uses an unsupported deserializer
     */
    public byte[] fromZip(byte[] zipBytes) {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipMetadata metadata = null;
            byte[] dataBytes = null;

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                byte[] content = readAllBytes(zis);
                switch (entry.getName()) {
                    case METADATA_ENTRY -> metadata = objectMapper.readValue(content, ZipMetadata.class);
                    case DATA_ENTRY -> dataBytes = content;
                }
                zis.closeEntry();
            }

            if (metadata == null) {
                throw new ZipException("Missing " + METADATA_ENTRY + " in ZIP archive", null);
            }
            if (dataBytes == null) {
                throw new ZipException("Missing " + DATA_ENTRY + " in ZIP archive", null);
            }
            if (!ZipMetadata.CLEAR_DESERIALIZER.equals(metadata.deserializer())) {
                throw new ZipException("Unsupported deserializer '" + metadata.deserializer() + "' in ZIP archive",
                        null);
            }

            return dataBytes;
        } catch (ZipException e) {
            throw e;
        } catch (IOException e) {
            throw new ZipException("Failed to read ZIP archive", e);
        }
    }

    private byte[] readAllBytes(ZipInputStream zis) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int len;
        while ((len = zis.read(buffer)) != -1) {
            baos.write(buffer, 0, len);
        }
        return baos.toByteArray();
    }

    /**
     * Runtime exception for ZIP serialization/deserialization failures.
     */
    public static class ZipException extends RuntimeException {
        public ZipException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
