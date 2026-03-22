package org.asymetrik.web.fairnsquare.split.domain.settlement;

import java.math.BigDecimal;
import java.util.List;

import org.asymetrik.web.fairnsquare.split.domain.participant.Participant;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SettlementParticipantMapper}.
 */
class SettlementParticipantMapperTest {

    @Test
    void from_noSharedAccountId_allStandard() {
        Participant alice = Participant.create("Alice", 1);
        Participant bob = Participant.create("Bob", 1);

        List<SettlementParticipant> result = SettlementParticipantMapper.from(List.of(alice, bob));

        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isInstanceOf(SettlementParticipant.Standard.class);
        assertThat(result.get(1)).isInstanceOf(SettlementParticipant.Standard.class);
        assertThat(((SettlementParticipant.Standard) result.get(0)).participant()).isEqualTo(alice);
        assertThat(((SettlementParticipant.Standard) result.get(1)).participant()).isEqualTo(bob);
    }

    @Test
    void from_singletonGroup_treatedAsStandard() {
        Participant alice = Participant.create("Alice", 1);
        Participant bob = withSharedAccountId(Participant.create("Bob", 1), Participant.SharedAccountId.generate());

        List<SettlementParticipant> result = SettlementParticipantMapper.from(List.of(alice, bob));

        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isInstanceOf(SettlementParticipant.Standard.class);
        // Singleton group → Standard
        assertThat(result.get(1)).isInstanceOf(SettlementParticipant.Standard.class);
    }

    @Test
    void from_twoMembersInGroup_emitsSharedAccountAndTwoMembers() {
        Participant.SharedAccountId groupId = Participant.SharedAccountId.generate();
        Participant alice = Participant.create("Alice", 1);
        Participant bob = withSharedAccountId(Participant.create("Bob", 1), groupId);
        Participant charlie = withSharedAccountId(Participant.create("Charlie", 1), groupId);

        List<SettlementParticipant> result = SettlementParticipantMapper.from(List.of(alice, bob, charlie));

        // alice → Standard
        // bob (first group member) → SharedAccount, then SharedAccountMember(bob), SharedAccountMember(charlie)
        assertThat(result).hasSize(4);
        assertThat(result.get(0)).isInstanceOf(SettlementParticipant.Standard.class);
        assertThat(result.get(1)).isInstanceOf(SettlementParticipant.SharedAccount.class);
        assertThat(result.get(2)).isInstanceOf(SettlementParticipant.SharedAccountMember.class);
        assertThat(result.get(3)).isInstanceOf(SettlementParticipant.SharedAccountMember.class);

        SettlementParticipant.SharedAccount group = (SettlementParticipant.SharedAccount) result.get(1);
        assertThat(group.id()).isEqualTo(groupId);
        assertThat(group.members()).containsExactly(bob, charlie);
    }

    @Test
    void from_sharedAccountName_alphabeticallySorted() {
        Participant.SharedAccountId groupId = Participant.SharedAccountId.generate();
        Participant zoe = withSharedAccountId(Participant.create("Zoe", 1), groupId);
        Participant alice = withSharedAccountId(Participant.create("Alice", 1), groupId);

        List<SettlementParticipant> result = SettlementParticipantMapper.from(List.of(zoe, alice));

        SettlementParticipant.SharedAccount group = (SettlementParticipant.SharedAccount) result.get(0);
        assertThat(group.name()).isEqualTo("Alice & Zoe");
    }

    @Test
    void from_sharedAccountBalance_isSumOfMemberBalances() {
        Participant.SharedAccountId groupId = Participant.SharedAccountId.generate();
        // We can't set balances directly (they're computed by Split), so we test with zero balances
        Participant bob = withSharedAccountId(Participant.create("Bob", 1), groupId);
        Participant charlie = withSharedAccountId(Participant.create("Charlie", 1), groupId);

        List<SettlementParticipant> result = SettlementParticipantMapper.from(List.of(bob, charlie));

        SettlementParticipant.SharedAccount group = (SettlementParticipant.SharedAccount) result.get(0);
        assertThat(group.balance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void from_emptyList_returnsEmpty() {
        assertThat(SettlementParticipantMapper.from(List.of())).isEmpty();
    }

    @Test
    void from_twoIndependentGroups_emitsBothSharedAccounts() {
        Participant.SharedAccountId group1 = Participant.SharedAccountId.generate();
        Participant.SharedAccountId group2 = Participant.SharedAccountId.generate();
        Participant alice = withSharedAccountId(Participant.create("Alice", 1), group1);
        Participant bob = withSharedAccountId(Participant.create("Bob", 1), group1);
        Participant charlie = withSharedAccountId(Participant.create("Charlie", 1), group2);
        Participant dave = withSharedAccountId(Participant.create("Dave", 1), group2);

        List<SettlementParticipant> result = SettlementParticipantMapper.from(List.of(alice, bob, charlie, dave));

        // group1 → SharedAccount + 2 members, group2 → SharedAccount + 2 members
        assertThat(result).hasSize(6);
        assertThat(result.get(0)).isInstanceOf(SettlementParticipant.SharedAccount.class);
        assertThat(result.get(1)).isInstanceOf(SettlementParticipant.SharedAccountMember.class);
        assertThat(result.get(2)).isInstanceOf(SettlementParticipant.SharedAccountMember.class);
        assertThat(result.get(3)).isInstanceOf(SettlementParticipant.SharedAccount.class);
        assertThat(result.get(4)).isInstanceOf(SettlementParticipant.SharedAccountMember.class);
        assertThat(result.get(5)).isInstanceOf(SettlementParticipant.SharedAccountMember.class);
    }

    /**
     * Helper: returns a new Participant record with the given sharedAccountId (overrides the field since Participant is
     * a record with computed balance fields set to zero).
     */
    private static Participant withSharedAccountId(Participant p, Participant.SharedAccountId groupId) {
        return new Participant(p.id(), p.name(), p.nights(), p.share(), groupId, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO);
    }
}
