package org.asymetrik.web.fairnsquare.split.persistence;

import java.nio.file.Path;
import java.nio.file.Paths;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Resolves file paths for tenant-scoped data storage. Pattern: {dataPath}/{tenantId}/{splitId}.zip
 */
@ApplicationScoped
public class TenantPathResolver {

    private static final String DEFAULT_TENANT = "default";
    private static final String ZIP_EXTENSION = ".zip";

    @ConfigProperty(name = "fairnsquare.data.path", defaultValue = "data")
    String dataPath;

    /**
     * Resolves the file path for a split within the default tenant.
     *
     * @param splitId
     *            the split identifier
     *
     * @return the resolved file path
     */
    public Path resolve(String splitId) {
        return resolve(DEFAULT_TENANT, splitId);
    }

    /**
     * Resolves the file path for a split within a specific tenant.
     *
     * @param tenantId
     *            the tenant identifier
     * @param splitId
     *            the split identifier
     *
     * @return the resolved file path
     */
    public Path resolve(String tenantId, String splitId) {
        return Paths.get(dataPath, tenantId, splitId + ZIP_EXTENSION);
    }

    /**
     * Resolves the directory path for a tenant.
     *
     * @param tenantId
     *            the tenant identifier
     *
     * @return the tenant directory path
     */
    public Path resolveTenantDirectory(String tenantId) {
        return Paths.get(dataPath, tenantId);
    }

    /**
     * Resolves the directory path for the default tenant.
     *
     * @return the default tenant directory path
     */
    public Path resolveDefaultTenantDirectory() {
        return resolveTenantDirectory(DEFAULT_TENANT);
    }
}
