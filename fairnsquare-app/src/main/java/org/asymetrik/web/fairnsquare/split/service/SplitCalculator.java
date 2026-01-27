package org.asymetrik.web.fairnsquare.split.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import org.asymetrik.web.fairnsquare.split.domain.Expense;
import org.asymetrik.web.fairnsquare.split.domain.Participant;
import org.asymetrik.web.fairnsquare.split.domain.SplitMode;

/**
 * Service for calculating expense shares based on split mode.
 */
@ApplicationScoped
public class SplitCalculator {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    /**
     * Calculate shares for each participant based on the split mode.
     *
     * @param amount
     *            the total expense amount
     * @param mode
     *            the split mode determining distribution
     * @param participants
     *            the list of participants to split among
     *
     * @return list of calculated shares, one per participant
     */
    public List<Expense.Share> calculateShares(BigDecimal amount, SplitMode mode, List<Participant> participants) {
        if (participants == null || participants.isEmpty()) {
            return Collections.emptyList();
        }

        return switch (mode) {
            case BY_NIGHT -> calculateByNight(amount, participants);
            case EQUAL -> calculateEqual(amount, participants);
            case FREE -> Collections.emptyList(); // Caller provides shares for FREE mode
        };
    }

    /**
     * Calculate shares proportionally based on nights stayed.
     */
    private List<Expense.Share> calculateByNight(BigDecimal amount, List<Participant> participants) {
        int totalNights = participants.stream().mapToInt(p -> p.nights().value()).sum();

        if (totalNights == 0) {
            return Collections.emptyList();
        }

        List<Expense.Share> shares = new ArrayList<>();
        BigDecimal totalAssigned = BigDecimal.ZERO;

        for (int i = 0; i < participants.size(); i++) {
            Participant p = participants.get(i);
            BigDecimal share;

            if (i == participants.size() - 1) {
                // Last participant gets the remainder to ensure sum = amount
                share = amount.subtract(totalAssigned);
            } else {
                share = amount.multiply(BigDecimal.valueOf(p.nights().value())).divide(BigDecimal.valueOf(totalNights),
                        SCALE, ROUNDING_MODE);
                totalAssigned = totalAssigned.add(share);
            }

            shares.add(new Expense.Share(p.id(), share));
        }

        return shares;
    }

    /**
     * Calculate shares equally among all participants.
     */
    private List<Expense.Share> calculateEqual(BigDecimal amount, List<Participant> participants) {
        int count = participants.size();
        BigDecimal baseShare = amount.divide(BigDecimal.valueOf(count), SCALE, ROUNDING_MODE);

        List<Expense.Share> shares = new ArrayList<>();
        BigDecimal totalAssigned = BigDecimal.ZERO;

        for (int i = 0; i < count; i++) {
            Participant p = participants.get(i);
            BigDecimal share;

            if (i == count - 1) {
                // Last participant gets the remainder to ensure sum = amount
                share = amount.subtract(totalAssigned);
            } else {
                share = baseShare;
                totalAssigned = totalAssigned.add(share);
            }

            shares.add(new Expense.Share(p.id(), share));
        }

        return shares;
    }
}
