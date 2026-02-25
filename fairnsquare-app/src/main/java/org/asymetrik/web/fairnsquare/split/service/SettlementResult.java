package org.asymetrik.web.fairnsquare.split.service;

import java.util.List;

import org.asymetrik.web.fairnsquare.split.domain.participant.Participant;
import org.asymetrik.web.fairnsquare.split.domain.settlement.Settlement;

/**
 * Bundles a computed Settlement with the participants of the split, so that the API layer can resolve participant names
 * for DTO mapping without an additional repository call.
 */
public record SettlementResult(Settlement settlement, List<Participant> participants) {
}
