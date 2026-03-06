package org.asymetrik.web.fairnsquare.infrastructure.filesystem;

import java.nio.file.Path;
import java.nio.file.Paths;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Resolves file paths for tenant-scoped data storage. Pattern: {dataPath}/{tenantId}/{filename}
 */
@ApplicationScoped
public class TenantPathResolver {

    static final String DEFAULT_TENANT = "default";
    private static final String ZIP_EXTENSION = ".zip";

    @ConfigProperty(name = "fairnsquare.data.path", defaultValue = "data")
    String dataPath;

    /**
     * Resolves the file path for the default tenant.
     *
     * @param filename
     *            the file name (including extension)
     *
     * @return the resolved file path
     */
    public Path resolve(Filename filename) {
        return resolve(new PathId(DEFAULT_TENANT), filename);
    }

    /**
     * Resolves the file path for a given path segment and file name.
     *
     * @param pathId
     *            the directory path segment (e.g. tenant identifier)
     * @param filename
     *            the file name (including extension)
     *
     * @return the resolved file path
     */
    public Path resolve(PathId pathId, Filename filename) {
        return Paths.get(dataPath, pathId.value(), filename.value());
    }

    /**
     * Resolves the file path for a split in the default tenant. Convenience method that automatically appends the ZIP
     * extension.
     *
     * @param splitId
     *            the split identifier (without extension)
     *
     * @return the resolved file path
     */
    public Path resolve(String splitId) {
        return resolve(new PathId(DEFAULT_TENANT), new Filename(splitId + ZIP_EXTENSION));
    }

    /**
     * Resolves the directory path for a given path segment.
     *
     * @param pathId
     *            the directory path segment
     *
     * @return the directory path
     */
    public Path resolveDirectory(PathId pathId) {
        return Paths.get(dataPath, pathId.value());
    }

    /**
     * Resolves the directory path for the default tenant.
     *
     * @return the default tenant directory path
     */
    public Path resolveDefaultTenantDirectory() {
        return resolveDirectory(new PathId(DEFAULT_TENANT));
    }

    /**
     * Resolves the root data directory containing all tenant directories.
     *
     * @return the root data directory path
     */
    public Path resolveRootDirectory() {
        return Paths.get(dataPath);
    }
}
