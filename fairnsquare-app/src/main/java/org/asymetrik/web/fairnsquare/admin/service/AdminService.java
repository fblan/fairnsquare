package org.asymetrik.web.fairnsquare.admin.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.asymetrik.web.fairnsquare.admin.api.dto.AdminSplitSummaryDTO;
import org.asymetrik.web.fairnsquare.admin.api.dto.AdminStatsResponse;
import org.asymetrik.web.fairnsquare.split.domain.Split;
import org.asymetrik.web.fairnsquare.split.domain.expenses.Expense;
import org.asymetrik.web.fairnsquare.split.domain.expenses.SplitMode;
import org.asymetrik.web.fairnsquare.split.persistence.SplitRepository;

/**
 * Computes admin statistics from all stored splits. Loads all splits from the repository on each call — intended for
 * infrequent admin access only.
 */
@ApplicationScoped
public class AdminService {

    private final SplitRepository splitRepository;

    @Inject
    public AdminService(SplitRepository splitRepository) {
        this.splitRepository = splitRepository;
    }

    public AdminStatsResponse getStats() {
        List<Split> splits = splitRepository.loadAll();

        Instant lastUpdated = splits.stream().map(Split::getUpdatedAt).max(Comparator.naturalOrder()).orElse(null);

        List<AdminSplitSummaryDTO> summaries = splits.stream().map(this::toSummary)
                .sorted(Comparator.comparing(AdminSplitSummaryDTO::createdAt)).toList();

        return new AdminStatsResponse(splits.size(), lastUpdated != null ? lastUpdated.toString() : null, summaries);
    }

    private AdminSplitSummaryDTO toSummary(Split split) {
        List<String> expenseTypes = split.getExpenses().stream().map(Expense::getSplitMode).map(SplitMode::name)
                .distinct().sorted().collect(Collectors.toList());

        return new AdminSplitSummaryDTO(hashId(split.getId().value()), split.getCreatedAt().toString(),
                split.getUpdatedAt().toString(), split.getParticipants().size(), split.getExpenses().size(),
                expenseTypes);
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
