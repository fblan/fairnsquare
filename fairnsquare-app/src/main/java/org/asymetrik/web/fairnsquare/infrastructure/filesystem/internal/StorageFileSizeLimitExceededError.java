package org.asymetrik.web.fairnsquare.infrastructure.filesystem.internal;

import org.asymetrik.web.fairnsquare.sharedkernel.error.BaseError;

/**
 * Thrown when a file being saved exceeds the configured maximum single-file size. Maps to HTTP 507 Insufficient
 * Storage.
 */
public class StorageFileSizeLimitExceededError extends BaseError {

    public StorageFileSizeLimitExceededError(long fileSizeBytes, long maxFileSizeBytes) {
        super("https://fairnsquare.app/errors/storage-file-size-limit-exceeded", "Storage File Size Limit Exceeded",
                507, String.format("Cannot save: file size is %d bytes but the configured limit is %d bytes.",
                        fileSizeBytes, maxFileSizeBytes));
    }
}
