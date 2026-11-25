package org.asymetrik.web.fairnsquare.backup;

import org.asymetrik.web.fairnsquare.sharedkernel.error.BaseError;

/**
 * A backup operation failed: backups are off or misconfigured (409), the archive doesn't exist (404), or the bucket or
 * the archive couldn't be read or written (502).
 */
public class BackupError extends BaseError {

    private BackupError(String type, String title, int status, String message, Throwable cause) {
        super("https://fairnsquare.app/errors/" + type, title, status, message, cause);
    }

    static BackupError unavailable(String message) {
        return new BackupError("backup-unavailable", "Backups Unavailable", 409, message, null);
    }

    static BackupError archiveNotFound(String name) {
        return new BackupError("backup-not-found", "Backup Not Found", 404, "No archive named " + name + ".", null);
    }

    static BackupError failed(String message, Throwable cause) {
        return new BackupError("backup-failed", "Backup Failed", 502, message, cause);
    }
}
