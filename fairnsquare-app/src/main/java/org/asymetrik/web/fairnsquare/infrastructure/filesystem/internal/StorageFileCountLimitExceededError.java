package org.asymetrik.web.fairnsquare.infrastructure.filesystem.internal;

import org.asymetrik.web.fairnsquare.sharedkernel.error.BaseError;

/**
 * Thrown when saving a new file would exceed the configured maximum number of stored files. Maps to HTTP 507
 * Insufficient Storage.
 */
public class StorageFileCountLimitExceededError extends BaseError {

    public StorageFileCountLimitExceededError(int currentCount, int maxCount) {
        super("https://fairnsquare.app/errors/storage-file-count-limit-exceeded", "Storage File Count Limit Exceeded",
                507, String.format("Cannot save: storage already contains %d file(s) and the configured limit is %d.",
                        currentCount, maxCount));
    }
}
