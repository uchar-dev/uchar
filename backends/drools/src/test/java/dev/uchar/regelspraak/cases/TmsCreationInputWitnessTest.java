package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocuments.WHOLE;
import static dev.uchar.regelspraak.cases.TmsDocuments.attribuut;
import static dev.uchar.regelspraak.cases.TmsDocuments.binary;
import static dev.uchar.regelspraak.cases.TmsDocuments.number;
import static dev.uchar.regelspraak.cases.TmsDocuments.instance;
import static dev.uchar.regelspraak.cases.TmsDocuments.ownRead;
import static dev.uchar.regelspraak.cases.TmsDocuments.computingRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.waarde;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The member values of an object creation are the input of the object it makes, as the reference interpreter reads
 * them (RUNTIME-SEMANTICS ch. 5.5, 1.2g, 7.8), and a division in an initialisation beside a writer counts that writer
 * per concrete type (WF-079, ch. 7.10). Fix round 1 of step 3 of card tms-expressievormen-en-rekendatum.
 *
 * <p>The creation document: "Een Jutter heeft een melding (een Jutmelding, a subtype of Melding) met de waarde zijn
 * vangst", and "De terug van een Jutter moet berekend worden als de som van de waarde van al zijn meldingen, of 0",
 * through which the case reads the made object. J1 has vangst 7. Without a writer of the member the value of the
 * making stands: terug 7. A standing rule on the supertype, "De waarde van een Melding moet gesteld worden op 99",
 * displaces it as it displaces case input: 99. A rule on the created type itself is outside the standing domain (the
 * creation is another writer, ch. 7.8), and where it computes leeg it erases the value: 0. An initialisation on the
 * member finds it filled: 7; where the creation's own value is leeg (it reads the bonus, which no case sets), the
 * initialisation fills it: 5. Two rules on the member that write 98 and 97 are the fault of ch. 5.7, the writers in the
 * order of the document, the object named as made by a creation. Every order of the rules in the document.
 *
 * <p>The sister document: "Het doel van een Drijfgoed moet geïnitialiseerd worden op zijn a gedeeld door 2" beside "Het
 * doel van een Wrakgoed moet gesteld worden op 7", with the a derived from c ("De a van een Vondst = zijn c plus 2"). The
 * two writers never write one instance, so the division is no fault on an intermediate state: D1 (c 2) 2, W1 7,
 * consistent, in every order of the three rules. A writer on the supertype beside it writes the Drijfgoed too, and the
 * refusal that waits on card fout-op-tussenstand names it.
 *
 * <p><b>Measured</b> (2026-10-09, these documents in every order, written out with their cases and run through
 * {@code ref/bin/runner.exe} and {@code backends/clingo/bin/clingo_runner.exe}): ref gives terug 99, 7, 0, 7 and 5,
 * faults on the two rules, "two writers of one target disagree: rule "waarde volgens de vangst" writes 98 and rule
 * "waarde volgens het tarief" writes 97 to the attribute waarde of oc0::J1, a Jutmelding", the writers in the order of
 * the document, and passes the sister document in all six orders. clingo agrees on 7 without a writer, on 5, on the
 * fault and on the sister document, and differs from ref on the made object where a writer meets the creation's value:
 * 106 with the standing rule, 12 with the initialisation and 7 with the rule that computes leeg; the main session takes
 * the first to a card of its own.
 *
 * <p><b>Red before the fix</b> (measured 2026-10-09 on {@code 1738bf61}, the tests unchanged): four of the seven
 * tests. The standing rule: "Schrijversconflict geen fixpunt: twee schrijvers van één doel geven verschillende waarden
 * — de regel "waarde van een melding" schrijft 99 en de regel "meld de vangst" schrijft 7 in het attribuut waarde van
 * een aangemaakte Jutmelding", the creation's stated {@code Waarde} meeting the rule's. The rule that computes leeg and
 * the two rules: refused, "tms-transpiler SKIP: regel: waarde van een jutmelding — een rekenende regel buiten het
 * staande domein op een lid dat een objectcreatie bij het maken zet ('waarde' van Melding, gezet door "meld de
 * vangst"): …". The sister document: "tms-transpiler SKIP: regel: doel van drijfgoed — een deling in een
 * initialisatie waarvan het doel nog een schrijver heeft ('dividedBy'; 'doel' van Vondst wordt ook geschreven door
 * "doel van wrakgoed"): …". The creation without a writer, the initialisation beside it and the creation's leeg were
 * green already: they pin what the fix keeps.
 */
class TmsCreationInputWitnessTest {

    // ---------------- the member values of a creation are the input of the made object ----------------

    private static final String MODEL = TmsDocuments.model(List.of(
            TmsDocuments.objecttype("Jutter", List.of(attribuut("vangst", WHOLE), attribuut("bonus", WHOLE),
                    attribuut("terug", WHOLE)), List.of()),
            TmsDocuments.objecttype("Melding", List.of(attribuut("waarde", WHOLE), attribuut("toeslag", WHOLE)),
                    List.of()),
            TmsDocuments.subtype("Jutmelding", "Melding", List.of(), List.of())),
            List.of(TmsDocuments.factTypeOneToMany("jutmelding", "melder", "Jutter", "melding", "Jutmelding",
                    "meldingen")));

    /** "De terug van een Jutter moet berekend worden als de som van de waarde van al zijn meldingen, of 0": the case
     *  reads the member of the made object through it. */
    private static final String REFUND = computingRule("terug van een jutter", "BEREKENING", "Jutter", "terug",
            TmsDocuments.aggregation("sum", ownRead("waarde", "Jutmelding"), "Jutmelding",
                    TmsDocuments.sourceRole("jutmelding", "melding"), "null", number("0"), WHOLE,
                    "de som van de waarde van al zijn meldingen of 0"), "null");

    /** On the supertype, so the creation does not take it out of the standing domain (ch. 7.8, "other writer"). */
    private static final String STANDING = computingRule("waarde van een melding", "GELIJKSTELLING", "Melding", "waarde",
            number("99"), "null");

    /** On the created type itself, so outside the standing domain; the toeslag is never set, so it computes leeg. */
    private static final String LEEG = computingRule("waarde van een jutmelding", "GELIJKSTELLING", "Jutmelding", "waarde",
            ownRead("toeslag", "Jutmelding"), "null");

    private static final String INITIALISATION = computingRule("standaardwaarde", "INITIALISATIE", "Jutmelding", "waarde",
            number("5"), "null");

    private static final List<String> TWO = List.of(
            computingRule("waarde volgens de vangst", "GELIJKSTELLING", "Jutmelding", "waarde",
                    binary("plus", ownRead("toeslag", "Jutmelding"), number("98")), "null"),
            computingRule("waarde volgens het tarief", "GELIJKSTELLING", "Jutmelding", "waarde", number("97"), "null"));

    /** The creation, its waarde the vangst of its subject, or the bonus, which no case sets. */
    private static String creatie(String bron) {
        return TmsCreationWitnessTest.objectCreation("meld de vangst", "Jutter", "Jutmelding",
                List.of(TmsCreationWitnessTest.init("waarde", ownRead(bron, "Jutter"))), "null", "jutmelding", "melding");
    }

    private static Path document(List<String> rules, String bron) throws Exception {
        return TmsCreationWitnessTest.document(MODEL, rules, List.of(), List.of(creatie(bron)), List.of(), List.of());
    }

    private static JsonNode caseDocument(String refund) throws Exception {
        return TmsDocuments.caseDocument(List.of(instance("J1", "Jutter", Map.of("vangst", "7"), List.of(), Map.of())),
                List.of(waarde("J1", "terug", refund)));
    }

    /** Every order of {@code rules} in the document plays {@code caseDocument(terug)}. */
    private static void plays(List<String> rules, String bron, String refund) throws Exception {
        int orders = 0;
        for (List<String> order : TmsDocuments.permutations(rules)) {
            List<String> names = TmsDocuments.ruleNames(order);
            assertEquals("gespeeld", TmsFaultWitnessTest.playBoth(document(order, bron), caseDocument(refund)),
                    () -> "volgorde " + names);
            orders++;
        }
        assertTrue(orders > 0);
    }

    @Test void a_standing_rule_displaces_the_value_of_the_creation() throws Exception {
        plays(List.of(REFUND, STANDING), "vangst", "99");
    }

    @Test void without_a_writer_the_value_of_the_creation_stays_standing() throws Exception {
        plays(List.of(REFUND), "vangst", "7");
    }

    @Test void a_rule_outside_the_standing_domain_that_computes_empty_clears_the_value_of_the_creation() throws Exception {
        plays(List.of(REFUND, LEEG), "vangst", "0");
    }

    @Test void an_initialisation_finds_the_member_of_the_creation_filled() throws Exception {
        plays(List.of(REFUND, INITIALISATION), "vangst", "7");
    }

    @Test void an_empty_value_of_the_creation_lets_the_initialisation_fill() throws Exception {
        plays(List.of(REFUND, INITIALISATION), "bonus", "5");
    }

    @Test void two_writers_on_the_member_of_the_creation_with_different_values_are_the_fault() throws Exception {
        List<String> rules = new ArrayList<>(TWO);
        rules.add(REFUND);
        int orders = 0;
        for (List<String> order : TmsDocuments.permutations(rules)) {
            List<String> names = TmsDocuments.ruleNames(order);
            Path ir = document(order, "vangst");
            RuntimeException e = assertThrows(RuntimeException.class,
                    () -> CaseRunner.playCase(ir, caseDocument("0"), DroolsBackend.Branch.EXEC_MODEL), () -> "volgorde " + names);
            boolean vangstFirst = names.indexOf("waarde volgens de vangst") < names.indexOf("waarde volgens het tarief");
            assertEquals("geen fixpunt: twee schrijvers van één doel geven verschillende waarden — de regel \""
                    + (vangstFirst ? "waarde volgens de vangst\" schrijft 98 en de regel \"waarde volgens het tarief\" schrijft 97"
                                   : "waarde volgens het tarief\" schrijft 97 en de regel \"waarde volgens de vangst\" schrijft 98")
                    + " in het attribuut waarde van een aangemaakte Jutmelding; divergentie is een fout (RUNTIME-SEMANTICS"
                    + " ch. 5.7)", e.getMessage(), () -> "volgorde " + names);
            orders++;
        }
        assertEquals(6, orders);
    }

    // ---------------- a division in an initialisation, and a writer on a sister subtype ----------------

    private static final String SISTER_MODEL = TmsDocuments.model(List.of(
            TmsDocuments.objecttype("Vondst", List.of(attribuut("a", WHOLE), attribuut("c", WHOLE),
                    attribuut("doel", WHOLE)), List.of()),
            TmsDocuments.subtype("Drijfgoed", "Vondst", List.of(), List.of()),
            TmsDocuments.subtype("Wrakgoed", "Vondst", List.of(), List.of())), List.of());

    private static final List<String> SISTERS = List.of(
            computingRule("doel van drijfgoed", "INITIALISATIE", "Drijfgoed", "doel",
                    binary("dividedBy", ownRead("a", "Drijfgoed"), number("2")), "null"),
            computingRule("doel van wrakgoed", "GELIJKSTELLING", "Wrakgoed", "doel", number("7"), "null"),
            computingRule("a van een vondst", "BEREKENING", "Vondst", "a", binary("plus", ownRead("c", "Vondst"), number("2")),
                    "null"));

    @Test void a_division_in_an_initialisation_beside_a_writer_on_a_sister_subtype() throws Exception {
        JsonNode caseDocument = TmsDocuments.caseDocument(
                List.of(instance("D1", "Drijfgoed", Map.of("c", "2"), List.of(), Map.of()),
                        instance("W1", "Wrakgoed", Map.of(), List.of(), Map.of())),
                List.of(waarde("D1", "doel", "2"), waarde("W1", "doel", "7"), TmsDocuments.consistent()));
        int orders = 0;
        for (List<String> order : TmsDocuments.permutations(SISTERS)) {
            List<String> names = TmsDocuments.ruleNames(order);
            assertEquals("gespeeld", TmsFaultWitnessTest.playBoth(TmsDocuments.document(SISTER_MODEL, order), caseDocument), () -> "volgorde " + names);
            orders++;
        }
        assertEquals(6, orders);
        List<String> withSupertype = new ArrayList<>(SISTERS);
        withSupertype.add(computingRule("doel van een vondst", "GELIJKSTELLING", "Vondst", "doel", number("9"),
                TmsDocuments.comparison("greater", ownRead("a", "Vondst"), number("100"))));
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class, () -> DroolsBackend.build(
                TmsDocuments.document(SISTER_MODEL, withSupertype), DroolsBackend.Branch.EXEC_MODEL));
        assertTrue(e.getMessage().startsWith("transpiler SKIP: regel: doel van drijfgoed — een deling in een"
                + " initialisatie waarvan het doel nog een schrijver heeft ('dividedBy'; 'doel' van Vondst wordt ook"
                + " geschreven door \"doel van een vondst\"): "), e.getMessage());
    }
}
