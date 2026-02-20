package org.asymetrik.web.fairnsquare.split.dev;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.asymetrik.web.fairnsquare.split.domain.Split;
import org.asymetrik.web.fairnsquare.split.domain.expenses.Expense;
import org.asymetrik.web.fairnsquare.split.domain.expenses.ExpenseByNight;
import org.asymetrik.web.fairnsquare.split.domain.expenses.ExpenseEqual;
import org.asymetrik.web.fairnsquare.split.domain.expenses.ExpenseFree;
import org.asymetrik.web.fairnsquare.split.domain.participant.Participant;
import org.asymetrik.web.fairnsquare.split.persistence.SplitRepository;

import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * Seeds a realistic sample split on application startup in dev mode. The split is always recreated with the same
 * predictable ID so developers can bookmark it. Scenario: one week vacation in Provence with 10 participants having
 * different stay durations.
 */
@ApplicationScoped
@IfBuildProfile("dev")
public class DevDataSeeder {

    static final String DEV_SPLIT_ID = "devSampleSplit2026001";

    private final SplitRepository splitRepository;

    @Inject
    public DevDataSeeder(SplitRepository splitRepository) {
        this.splitRepository = splitRepository;
    }

    void onStart(@Observes StartupEvent event) {
        if (splitRepository.exists(DEV_SPLIT_ID)) {
            splitRepository.delete(DEV_SPLIT_ID);
        }

        Split split = new Split(Split.Id.of(DEV_SPLIT_ID), new Split.Name("Vacances en Provence - Été 2025"),
                Instant.parse("2025-07-12T18:00:00Z"));

        // --- Participants ---
        // Full week (7 nights): arrived Saturday, left Saturday
        Participant alice = Participant.create("Alice", 7, 2.0); // couple
        Participant bob = Participant.create("Bob", 7);
        Participant charlie = Participant.create("Charlie", 7, 2.5); // couple + child
        Participant diana = Participant.create("Diana", 7);
        // Arrived Sunday, left Saturday (5 nights)
        Participant eve = Participant.create("Eve", 5);
        Participant frank = Participant.create("Frank", 5, 1.5); // parent + child
        // Joined Wednesday, left Saturday (3 nights)
        Participant grace = Participant.create("Grace", 3);
        Participant henry = Participant.create("Henry", 3);
        // Arrived Sunday, left Thursday (4 nights)
        Participant isabelle = Participant.create("Isabelle", 4);
        // Arrived Saturday, left Wednesday (4 nights)
        Participant jack = Participant.create("Jack", 4);

        split.addParticipant(alice);
        split.addParticipant(bob);
        split.addParticipant(charlie);
        split.addParticipant(diana);
        split.addParticipant(eve);
        split.addParticipant(frank);
        split.addParticipant(grace);
        split.addParticipant(henry);
        split.addParticipant(isabelle);
        split.addParticipant(jack);

        // --- BY_NIGHT expenses ---
        // Accommodation
        split.addExpense(ExpenseByNight.create(bd("1400.00"), "Location maison - semaine entière", alice.id()));
        split.addExpense(ExpenseByNight.create(bd("210.00"), "Forfait entretien piscine", bob.id()));

        // Groceries - daily food shopping (proportional to stay)
        split.addExpense(ExpenseByNight.create(bd("186.50"), "Supermarché jour 1 - arrivée", charlie.id()));
        split.addExpense(ExpenseByNight.create(bd("94.80"), "Marché local jour 2", diana.id()));
        split.addExpense(ExpenseByNight.create(bd("213.20"), "Supermarché jour 2 - réapprovisionnement", alice.id()));
        split.addExpense(ExpenseByNight.create(bd("67.40"), "Épicerie jour 3 - petit déjeuner", eve.id()));
        split.addExpense(ExpenseByNight.create(bd("245.60"), "Supermarché jour 3 - repas semaine", frank.id()));
        split.addExpense(ExpenseByNight.create(bd("128.90"), "Boucherie jour 4 - viandes BBQ", grace.id()));
        split.addExpense(ExpenseByNight.create(bd("156.40"), "Supermarché jour 5 - weekend", henry.id()));
        split.addExpense(ExpenseByNight.create(bd("88.30"), "Boulangerie & fromagerie jour 5", isabelle.id()));
        split.addExpense(ExpenseByNight.create(bd("172.10"), "Supermarché jour 6 - repas de fête", jack.id()));
        split.addExpense(ExpenseByNight.create(bd("64.50"), "Épicerie jour 7 - petit déjeuner départ", alice.id()));

        // Home-cooked meals & drinks
        split.addExpense(ExpenseByNight.create(bd("94.80"), "Pique-nique & sandwichs", bob.id()));
        split.addExpense(ExpenseByNight.create(bd("138.60"), "Boissons & apéritifs semaine", charlie.id()));
        split.addExpense(ExpenseByNight.create(bd("54.60"), "Cafés & viennoiseries jour 1", diana.id()));
        split.addExpense(ExpenseByNight.create(bd("48.30"), "Cafés & viennoiseries jour 2", eve.id()));
        split.addExpense(ExpenseByNight.create(bd("51.80"), "Cafés & viennoiseries jour 3", frank.id()));
        split.addExpense(ExpenseByNight.create(bd("46.20"), "Cafés & viennoiseries jour 4", grace.id()));
        split.addExpense(ExpenseByNight.create(bd("52.40"), "Cafés & viennoiseries jour 5", henry.id()));
        split.addExpense(ExpenseByNight.create(bd("62.40"), "Glaces & snacks plage", isabelle.id()));
        split.addExpense(ExpenseByNight.create(bd("78.90"), "Vins & rosé locaux", jack.id()));
        split.addExpense(ExpenseByNight.create(bd("44.50"), "Jus de fruits & sodas", alice.id()));

        // Daily household supplies
        split.addExpense(ExpenseByNight.create(bd("68.20"), "Produits ménagers & entretien", bob.id()));
        split.addExpense(ExpenseByNight.create(bd("78.40"), "Crème solaire & soins", charlie.id()));
        split.addExpense(ExpenseByNight.create(bd("38.60"), "Pharmacie & premiers secours", diana.id()));
        split.addExpense(ExpenseByNight.create(bd("28.50"), "Journaux & magazines", eve.id()));
        split.addExpense(ExpenseByNight.create(bd("42.00"), "Bougies & déco soirée", frank.id()));
        split.addExpense(ExpenseByNight.create(bd("124.50"), "Matériel de plage (serviettes, parasols)", grace.id()));
        split.addExpense(ExpenseByNight.create(bd("48.00"), "Jeux de société & cartes", henry.id()));

        // Meals out (home-base dinners, proportional to presence)
        split.addExpense(
                ExpenseByNight.create(bd("287.00"), "Dîner de bienvenue - restaurant du village", isabelle.id()));
        split.addExpense(ExpenseByNight.create(bd("94.30"), "Fournitures BBQ - soir 4", jack.id()));
        split.addExpense(ExpenseByNight.create(bd("264.80"), "Dîner d'adieu - terrasse", alice.id()));
        split.addExpense(ExpenseByNight.create(bd("156.40"), "Déjeuner sur place jour 2", bob.id()));
        split.addExpense(ExpenseByNight.create(bd("189.50"), "Pizzeria à emporter soir 4", charlie.id()));
        split.addExpense(ExpenseByNight.create(bd("86.40"), "Location sono soirée", diana.id()));

        // --- EQUAL expenses ---
        // One-off activities where all participants share equally regardless of stay
        split.addExpense(ExpenseEqual.create(bd("180.00"), "Dégustation vins domaine local", eve.id()));
        split.addExpense(ExpenseEqual.create(bd("240.00"), "Location kayaks - demi-journée", frank.id()));
        split.addExpense(ExpenseEqual.create(bd("420.00"), "Promenade en bateau dans les calanques", grace.id()));
        split.addExpense(ExpenseEqual.create(bd("160.00"), "Escape game - Avignon", henry.id()));
        split.addExpense(ExpenseEqual.create(bd("84.00"), "Visite musée & sites antiques d'Arles", isabelle.id()));
        split.addExpense(ExpenseEqual.create(bd("200.00"), "Concert jazz au festival d'Arles", jack.id()));
        split.addExpense(ExpenseEqual.create(bd("144.00"), "Location vélos - journée", alice.id()));

        // --- FREE expenses (travel - only participants who traveled together) ---
        // Group 1: Alice, Bob, Charlie, Diana drove from Paris
        Participant.Id aliceId = alice.id();
        Participant.Id bobId = bob.id();
        Participant.Id charlieId = charlie.id();
        Participant.Id dianaId = diana.id();
        Participant.Id eveId = eve.id();
        Participant.Id frankId = frank.id();
        Participant.Id graceId = grace.id();
        Participant.Id henryId = henry.id();
        Participant.Id isabelleId = isabelle.id();
        Participant.Id jackId = jack.id();

        split.addExpense(ExpenseFree.create(bd("186.40"), "Carburant Paris → Provence (voiture groupe 1)", aliceId,
                equalParts(aliceId, bobId, charlieId, dianaId)));
        split.addExpense(ExpenseFree.create(bd("178.20"), "Carburant Provence → Paris (voiture groupe 1)", bobId,
                equalParts(aliceId, bobId, charlieId, dianaId)));

        // Group 2: Eve & Frank shared a ride
        split.addExpense(ExpenseFree.create(bd("134.80"), "Covoiturage Lyon → Provence aller (Eve & Frank)", eveId,
                equalParts(eveId, frankId)));
        split.addExpense(ExpenseFree.create(bd("128.50"), "Covoiturage Provence → Lyon retour (Eve & Frank)", frankId,
                equalParts(eveId, frankId)));

        // Group 3: Grace & Henry drove from Bordeaux for the weekend
        split.addExpense(ExpenseFree.create(bd("78.40"), "Essence Bordeaux → Provence aller (Grace & Henry)", graceId,
                equalParts(graceId, henryId)));
        split.addExpense(ExpenseFree.create(bd("72.60"), "Essence Provence → Bordeaux retour (Grace & Henry)", henryId,
                equalParts(graceId, henryId)));

        // Isabelle & Jack took the train together
        split.addExpense(ExpenseFree.create(bd("248.00"), "Billets TGV aller (Isabelle & Jack)", isabelleId,
                equalParts(isabelleId, jackId)));
        split.addExpense(ExpenseFree.create(bd("248.00"), "Billets TGV retour (Isabelle & Jack)", jackId,
                equalParts(isabelleId, jackId)));

        splitRepository.save(split);
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private static List<Expense.Share> equalParts(Participant.Id... participantIds) {
        return java.util.Arrays.stream(participantIds).map(id -> Expense.Share.withParts(id, BigDecimal.ONE)).toList();
    }
}
