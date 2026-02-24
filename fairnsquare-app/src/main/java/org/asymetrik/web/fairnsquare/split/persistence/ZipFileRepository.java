package org.asymetrik.web.fairnsquare.split.persistence;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * ZIP-based file repository for persisting entities to the file system. Each entity is stored as a ZIP archive
 * containing:
 * <ul>
 * <li>{@code metadata.json} — format version and deserializer code</li>
 * <li>{@code data.bin} — the JSON-serialized entity data</li>
 * </ul>
 */
@ApplicationScoped
public class ZipFileRepository {

    static final String METADATA_ENTRY = "metadata.json";
    static final String DATA_ENTRY = "data.bin";

    private final ObjectMapper objectMapper;
    private final TenantPathResolver pathResolver;
    private final StorageConstraintsService storageConstraints;

    @Inject
    public ZipFileRepository(TenantPathResolver pathResolver, StorageConstraintsService storageConstraints) {
        this.pathResolver = pathResolver;
        this.storageConstraints = storageConstraints;
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
     * Saves an entity to a ZIP archive for the default tenant.
     *
     * @param splitId
     *            the split identifier (used as filename)
     * @param entity
     *            the entity to save
     * @param <T>
     *            the entity type
     *
     * @throws PersistenceException
     *             if the save operation fails
     */
    <T> void save(String splitId, T entity) {
        Path filePath = pathResolver.resolve(splitId);
        saveToPath(filePath, entity);
    }

    /**
     * Saves an entity to a ZIP archive for a specific tenant.
     *
     * @param tenantId
     *            the tenant identifier
     * @param splitId
     *            the split identifier (used as filename)
     * @param entity
     *            the entity to save
     * @param <T>
     *            the entity type
     *
     * @throws PersistenceException
     *             if the save operation fails
     */
    <T> void save(String tenantId, String splitId, T entity) {
        Path filePath = pathResolver.resolve(tenantId, splitId);
        saveToPath(filePath, entity);
    }

    /**
     * Loads an entity from a ZIP archive for the default tenant.
     *
     * @param splitId
     *            the split identifier
     * @param entityClass
     *            the class of the entity to load
     * @param <T>
     *            the entity type
     *
     * @return an Optional containing the entity if found, empty otherwise
     *
     * @throws PersistenceException
     *             if the load operation fails (other than file not found)
     */
    <T> Optional<T> load(String splitId, Class<T> entityClass) {
        Path filePath = pathResolver.resolve(splitId);
        return loadFromPath(filePath, entityClass);
    }

    /**
     * Loads an entity from a ZIP archive for a specific tenant.
     *
     * @param tenantId
     *            the tenant identifier
     * @param splitId
     *            the split identifier
     * @param entityClass
     *            the class of the entity to load
     * @param <T>
     *            the entity type
     *
     * @return an Optional containing the entity if found, empty otherwise
     *
     * @throws PersistenceException
     *             if the load operation fails (other than file not found)
     */
    <T> Optional<T> load(String tenantId, String splitId, Class<T> entityClass) {
        Path filePath = pathResolver.resolve(tenantId, splitId);
        return loadFromPath(filePath, entityClass);
    }

    /**
     * Deletes a split archive for the default tenant.
     *
     * @param splitId
     *            the split identifier
     *
     * @throws PersistenceException
     *             if the delete operation fails
     */
    void delete(String splitId) {
        Path filePath = pathResolver.resolve(splitId);
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new PersistenceException("Failed to delete entity at " + filePath, e);
        }
    }

    /**
     * Checks if a split archive exists for the default tenant.
     *
     * @param splitId
     *            the split identifier
     *
     * @return true if the file exists, false otherwise
     */
    boolean exists(String splitId) {
        Path filePath = pathResolver.resolve(splitId);
        return Files.exists(filePath);
    }

    /**
     * Checks if a split archive exists for a specific tenant.
     *
     * @param tenantId
     *            the tenant identifier
     * @param splitId
     *            the split identifier
     *
     * @return true if the file exists, false otherwise
     */
    boolean exists(String tenantId, String splitId) {
        Path filePath = pathResolver.resolve(tenantId, splitId);
        return Files.exists(filePath);
    }

    private <T> void saveToPath(Path filePath, T entity) {
        try {
            Path parentDir = filePath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }

            byte[] metadataBytes = objectMapper.writeValueAsBytes(PersistenceMetadata.current());
            byte[] dataBytes = objectMapper.writeValueAsString(entity).getBytes(StandardCharsets.UTF_8);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                zos.putNextEntry(new ZipEntry(METADATA_ENTRY));
                zos.write(metadataBytes);
                zos.closeEntry();

                zos.putNextEntry(new ZipEntry(DATA_ENTRY));
                zos.write(dataBytes);
                zos.closeEntry();
            }

            byte[] zipBytes = baos.toByteArray();
            storageConstraints.checkSizeLimitBeforeSave(filePath, zipBytes.length);
            Files.write(filePath, zipBytes);
        } catch (IOException e) {
            throw new PersistenceException("Failed to save entity to " + filePath, e);
        }
    }

    private <T> Optional<T> loadFromPath(Path filePath, Class<T> entityClass) {
        if (!Files.exists(filePath)) {
            return Optional.empty();
        }

        try (InputStream fis = Files.newInputStream(filePath);
                ZipInputStream zis = new ZipInputStream(fis)) {

            PersistenceMetadata metadata = null;
            byte[] dataBytes = null;

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                byte[] content = readAllBytes(zis);
                switch (entry.getName()) {
                    case METADATA_ENTRY -> metadata = objectMapper.readValue(content, PersistenceMetadata.class);
                    case DATA_ENTRY -> dataBytes = content;
                }
                zis.closeEntry();
            }

            if (metadata == null) {
                throw new PersistenceException("Missing " + METADATA_ENTRY + " in " + filePath, null);
            }
            if (dataBytes == null) {
                throw new PersistenceException("Missing " + DATA_ENTRY + " in " + filePath, null);
            }
            if (!PersistenceMetadata.CLEAR_DESERIALIZER.equals(metadata.deserializer())) {
                throw new PersistenceException(
                        "Unsupported deserializer '" + metadata.deserializer() + "' in " + filePath, null);
            }

            T entity = objectMapper.readValue(dataBytes, entityClass);
            return Optional.of(entity);
        } catch (PersistenceException e) {
            throw e;
        } catch (IOException e) {
            throw new PersistenceException("Failed to load entity " + entityClass.getName() + " from " + filePath, e);
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
     * Runtime exception for persistence operations.
     */
    public static class PersistenceException extends RuntimeException {
        public PersistenceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
