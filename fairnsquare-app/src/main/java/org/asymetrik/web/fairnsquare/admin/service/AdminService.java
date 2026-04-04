package org.asymetrik.web.fairnsquare.admin.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.asymetrik.web.fairnsquare.admin.api.dto.AdminSplitSummaryDTO;
import org.asymetrik.web.fairnsquare.admin.api.dto.AdminStatsResponse;
import org.asymetrik.web.fairnsquare.split.SplitAdminQuery;
import org.asymetrik.web.fairnsquare.split.SplitSummary;

/**
 * Computes admin statistics from all stored splits via the split module's exported query API. Intended for infrequent
 * admin access only.
 */
@ApplicationScoped
public class AdminService {

    private final SplitAdminQuery splitAdminQuery;

    @Inject
    public AdminService(SplitAdminQuery splitAdminQuery) {
        this.splitAdminQuery = splitAdminQuery;
    }

    public AdminStatsResponse getStats() {
        List<SplitSummary> summaries = splitAdminQuery.getAllSummaries();

        Instant lastUpdated = summaries.stream().map(SplitSummary::updatedAt).max(Comparator.naturalOrder())
                .orElse(null);

        List<AdminSplitSummaryDTO> dtos = summaries.stream().map(this::toDTO)
                .sorted(Comparator.comparing(AdminSplitSummaryDTO::createdAt)).toList();

        return new AdminStatsResponse(summaries.size(), lastUpdated != null ? lastUpdated.toString() : null, dtos);
    }

    private AdminSplitSummaryDTO toDTO(SplitSummary s) {
        return new AdminSplitSummaryDTO(hashId(s.id()), s.createdAt().toString(), s.updatedAt().toString(),
                s.participantCount(), s.expenseCount(), s.expenseTypes());
    }

    private static String hashId(String id) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(id.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
