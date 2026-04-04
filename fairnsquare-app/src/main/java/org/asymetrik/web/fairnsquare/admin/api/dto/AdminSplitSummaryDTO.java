package org.asymetrik.web.fairnsquare.admin.api.dto;

import java.util.List;

/**
 * Summary of a single split for the admin view. The real split ID is never exposed — only a truncated SHA-256 hash.
 */
public record AdminSplitSummaryDTO(String idHash, String createdAt, String updatedAt, int participantCount,
        int expenseCount, List<String> expenseTypes) {
}
