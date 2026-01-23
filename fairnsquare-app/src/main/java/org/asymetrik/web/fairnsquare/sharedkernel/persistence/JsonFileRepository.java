package org.asymetrik.web.fairnsquare.sharedkernel.persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Generic JSON file repository for persisting entities to the file system. Provides automatic directory creation and
 * JSON serialization/deserialization.
 */
@ApplicationScoped
public class JsonFileRepository {

    private final ObjectMapper objectMapper;
    private final TenantPathResolver pathResolver;

    @Inject
    public JsonFileRepository(TenantPathResolver pathResolver) {
        this.pathResolver = pathResolver;
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
     * Saves an entity to a JSON file for the default tenant. Creates parent directories if they don't exist.
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
    public <T> void save(String splitId, T entity) {
        Path filePath = pathResolver.resolve(splitId);
        saveToPath(filePath, entity);
    }

    /**
     * Saves an entity to a JSON file for a specific tenant. Creates parent directories if they don't exist.
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
    public <T> void save(String tenantId, String splitId, T entity) {
        Path filePath = pathResolver.resolve(tenantId, splitId);
        saveToPath(filePath, entity);
    }

    /**
     * Loads an entity from a JSON file for the default tenant.
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
    public <T> Optional<T> load(String splitId, Class<T> entityClass) {
        Path filePath = pathResolver.resolve(splitId);
        return loadFromPath(filePath, entityClass);
    }

    /**
     * Loads an entity from a JSON file for a specific tenant.
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
    public <T> Optional<T> load(String tenantId, String splitId, Class<T> entityClass) {
        Path filePath = pathResolver.resolve(tenantId, splitId);
        return loadFromPath(filePath, entityClass);
    }

    /**
     * Checks if a split file exists for the default tenant.
     *
     * @param splitId
     *            the split identifier
     *
     * @return true if the file exists, false otherwise
     */
    public boolean exists(String splitId) {
        Path filePath = pathResolver.resolve(splitId);
        return Files.exists(filePath);
    }

    /**
     * Checks if a split file exists for a specific tenant.
     *
     * @param tenantId
     *            the tenant identifier
     * @param splitId
     *            the split identifier
     *
     * @return true if the file exists, false otherwise
     */
    public boolean exists(String tenantId, String splitId) {
        Path filePath = pathResolver.resolve(tenantId, splitId);
        return Files.exists(filePath);
    }

    private <T> void saveToPath(Path filePath, T entity) {
        try {
            // Create parent directories if they don't exist
            Path parentDir = filePath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }

            // Write entity as JSON
            String json = objectMapper.writeValueAsString(entity);
            Files.writeString(filePath, json);
        } catch (IOException e) {
            throw new PersistenceException("Failed to save entity to " + filePath, e);
        }
    }

    private <T> Optional<T> loadFromPath(Path filePath, Class<T> entityClass) {
        if (!Files.exists(filePath)) {
            return Optional.empty();
        }

        try {
            String json = Files.readString(filePath);
            T entity = objectMapper.readValue(json, entityClass);
            return Optional.of(entity);
        } catch (IOException e) {
            throw new PersistenceException("Failed to load entity from " + filePath, e);
        }
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
