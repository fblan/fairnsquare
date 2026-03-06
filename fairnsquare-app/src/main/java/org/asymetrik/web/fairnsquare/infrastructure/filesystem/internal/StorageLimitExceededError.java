package org.asymetrik.web.fairnsquare.infrastructure.filesystem.internal;

import org.asymetrik.web.fairnsquare.sharedkernel.error.BaseError;

/**
 * Thrown when saving a file would cause the total storage to exceed the configured limit. Maps to HTTP 507 Insufficient
 * Storage.
 */
public class StorageLimitExceededError extends BaseError {

    public StorageLimitExceededError(long currentBytes, long maxBytes) {
        super("https://fairnsquare.app/errors/storage-limit-exceeded", "Storage Limit Exceeded", 507,
                String.format("Cannot save: current storage is %d bytes and the configured limit is %d bytes.",
                        currentBytes, maxBytes));
    }
}
