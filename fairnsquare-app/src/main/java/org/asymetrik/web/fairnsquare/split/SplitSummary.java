package org.asymetrik.web.fairnsquare.split;

import java.time.Instant;
import java.util.List;

/**
 * Read-only projection of a split, intended for administrative consumers outside the split module.
 */
public record SplitSummary(String id, Instant createdAt, Instant updatedAt, int participantCount, int expenseCount,
        List<String> expenseTypes) {
}
