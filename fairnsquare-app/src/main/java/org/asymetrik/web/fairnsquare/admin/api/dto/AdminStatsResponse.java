package org.asymetrik.web.fairnsquare.admin.api.dto;

import java.util.List;

/**
 * Admin statistics response. Contains global stats and the full list of split summaries.
 */
public record AdminStatsResponse(int totalSplits, String lastUpdated, List<AdminSplitSummaryDTO> splits) {
}
