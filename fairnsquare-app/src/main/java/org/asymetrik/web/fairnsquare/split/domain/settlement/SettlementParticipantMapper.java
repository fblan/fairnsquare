package org.asymetrik.web.fairnsquare.split.domain.settlement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.asymetrik.web.fairnsquare.split.domain.participant.Participant;

/**
 * Converts a flat list of {@link Participant} records into a list of {@link SettlementParticipant}s by grouping
 * participants that share the same {@link Participant.SharedAccountId}.
 * <p>
 * Mapping rules:
 * <ul>
 * <li>Participants with {@code sharedAccountId == null} → {@link SettlementParticipant.Standard}.</li>
 * <li>Participants whose {@code sharedAccountId} is shared by exactly one participant (singleton group) →
 * {@link SettlementParticipant.Standard} (the group has no effect).</li>
 * <li>Participants whose {@code sharedAccountId} is shared by two or more participants → one
 * {@link SettlementParticipant.SharedAccount} followed by one {@link SettlementParticipant.SharedAccountMember} per
 * member, preserving the original participant order within the group.</li>
 * </ul>
 * The returned list preserves the relative ordering of participants: individual participants appear at their original
 * position; for groups, the {@link SettlementParticipant.SharedAccount} appears at the position of the first group
 * member, followed immediately by the {@link SettlementParticipant.SharedAccountMember}s.
 */
public class SettlementParticipantMapper {

    private SettlementParticipantMapper() {
    }

    /**
     * Maps a list of participants to their settlement representation.
     *
     * @param participants
     *            the participants of a split (with computed balance fields)
     *
     * @return the corresponding list of {@link SettlementParticipant}s
     */
    public static List<SettlementParticipant> from(List<Participant> participants) {
        // Group participants by sharedAccountId (preserving insertion order)
        Map<Participant.SharedAccountId, List<Participant>> groups = new LinkedHashMap<>();
        for (Participant p : participants) {
            if (p.sharedAccountId() != null) {
                groups.computeIfAbsent(p.sharedAccountId(), k -> new ArrayList<>()).add(p);
            }
        }

        List<SettlementParticipant> result = new ArrayList<>();
        for (Participant p : participants) {
            if (p.sharedAccountId() == null) {
                result.add(new SettlementParticipant.Standard(p));
            } else {
                List<Participant> groupMembers = groups.get(p.sharedAccountId());
                if (groupMembers.size() == 1) {
                    // Singleton group — treat as standard individual
                    result.add(new SettlementParticipant.Standard(p));
                } else if (groupMembers.get(0).id().equals(p.id())) {
                    // First member of a real group — emit SharedAccount then all SharedAccountMembers
                    result.add(new SettlementParticipant.SharedAccount(p.sharedAccountId(), groupMembers));
                    for (Participant member : groupMembers) {
                        result.add(new SettlementParticipant.SharedAccountMember(member));
                    }
                }
                // Non-first members are already emitted as SharedAccountMember above; skip them here
            }
        }
        return result;
    }
}
