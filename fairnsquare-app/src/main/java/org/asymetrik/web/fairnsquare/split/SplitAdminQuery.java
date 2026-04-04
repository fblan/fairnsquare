package org.asymetrik.web.fairnsquare.split;

import java.util.Comparator;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.asymetrik.web.fairnsquare.split.domain.Split;
import org.asymetrik.web.fairnsquare.split.domain.expenses.Expense;
import org.asymetrik.web.fairnsquare.split.domain.expenses.SplitMode;
import org.asymetrik.web.fairnsquare.split.persistence.SplitRepository;

/**
 * Read-only query service for administrative consumers. Returns {@link SplitSummary} projections — split internals are
 * never leaked outside the split module.
 */
@ApplicationScoped
public class SplitAdminQuery {

    private final SplitRepository splitRepository;

    @Inject
    public SplitAdminQuery(SplitRepository splitRepository) {
        this.splitRepository = splitRepository;
    }

    /**
     * Loads all splits and returns a summary projection for each one.
     *
     * @return list of {@link SplitSummary}, one per stored split
     */
    public List<SplitSummary> getAllSummaries() {
        return splitRepository.loadAll().stream().map(this::toSummary).toList();
    }

    private SplitSummary toSummary(Split split) {
        List<String> expenseTypes = split.getExpenses().stream().map(Expense::getSplitMode).map(SplitMode::name)
                .distinct().sorted().toList();

        return new SplitSummary(split.getId().value(), split.getCreatedAt(), split.getUpdatedAt(),
                split.getParticipants().size(), split.getExpenses().size(), expenseTypes);
    }
}
