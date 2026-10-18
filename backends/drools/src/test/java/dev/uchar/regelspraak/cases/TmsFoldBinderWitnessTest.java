package dev.uchar.regelspraak.cases;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocuments.WHOLE;
import static dev.uchar.regelspraak.cases.TmsDocuments.aggregation;
import static dev.uchar.regelspraak.cases.TmsDocuments.attribuut;
import static dev.uchar.regelspraak.cases.TmsDocuments.binderRead;
import static dev.uchar.regelspraak.cases.TmsDocuments.sourceRole;
import static dev.uchar.regelspraak.cases.TmsDocuments.factTypeOneToMany;
import static dev.uchar.regelspraak.cases.TmsDocuments.number;
import static dev.uchar.regelspraak.cases.TmsDocuments.instanceWithRoles;
import static dev.uchar.regelspraak.cases.TmsDocuments.let;
import static dev.uchar.regelspraak.cases.TmsDocuments.ownRead;
import static dev.uchar.regelspraak.cases.TmsDocuments.computingRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.comparison;
import static dev.uchar.regelspraak.cases.TmsDocuments.waarde;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A BinderRef of a Let around a fold, read in the filter or the matrix of that fold: the exec-model branch does
 * not carry it yet and refuses it as such, not as an ill-formed document. The branch computes a fold's filter and
 * matrix per member in the fold's contribution rules, which bind the member's reads but not the value of a Let in the
 * rule that reads the fold; carrying the binder would bind the Let's value, read on the fold's subject, in every
 * contribution rule, and let a leeg value end the Let across the rule boundary (RUNTIME-SEMANTICS ch. 10.6). A
 * BinderRef that no Let encloses keeps its WF-064 refusal, and a Let inside the filter, which the contribution rule
 * binds itself, plays.
 *
 * <p>The documents, each one rule: "Het zwaar van een Jutter moet berekend worden als, met de grens zijn drempel, het
 * aantal vondsten van de Jutter met een gewicht groter dan de grens", the document of the lens that found the refusal's
 * wrong reason; the same count over the vondsten all of whose brokstukken have a gewicht groter dan de grens, the
 * matrix of a quantifier fold in the count's filter; over the vondsten all of whose brokstukken with a gewicht groter
 * dan de grens weigh more than 6, the quantifier fold's filter; the first one without the Let; and the first one with
 * "met de grens 6, de grens" as the right operand of the filter's comparison, a Let that shadows the outer one.
 *
 * <p><b>Measured</b> (2026-10-09, J1 drempel 5 over V1 3, V2 7 and V3 9, J2 drempel 2 over V4 3, the brokstukken B1 3
 * and B2 7 of V1, B3 7 and B4 9 of V2, B5 3 of V4): the verifier passes every document but the one without the Let
 * ("WF-064 FAIL …: binder reference 'grens' resolves to no enclosing binder"). The reference interpreter gives J1 2 and
 * J2 1 for the count's filter, 2 and 1 for the matrix, 3 and 0 for the quantifier fold's filter, and 2 and 0 for the
 * shadowing Let. The ASP runner agrees on the count's filter and the shadowing Let, and gives J1 3 and 2 in the two
 * quantifier documents, where it reads J2's drempel as well: with J1 alone in the case it gives 2 and 3. The exec-model
 * and the DRL-text branch refuse every one of these documents.
 *
 * <p><b>Red before the change</b> (measured 2026-10-09 on e92d5fee): the three documents with the Let were refused as
 * "een BinderRef 'grens' zonder omsluitende Let die hem bindt ('de grens', WF-064)".
 */
class TmsFoldBinderWitnessTest {

    private static final String MODEL = TmsDocuments.model(List.of(
            TmsDocuments.objecttype("Jutter", List.of(attribuut("drempel", WHOLE), attribuut("zwaar", WHOLE)),
                    List.of()),
            TmsDocuments.objecttype("Vondst", List.of(attribuut("gewicht", WHOLE)), List.of()),
            TmsDocuments.objecttype("Brokstuk", List.of(attribuut("gewicht", WHOLE)), List.of())),
            List.of(factTypeOneToMany("vinding", "vinder", "Jutter", "vondst", "Vondst", "vondsten"),
                    factTypeOneToMany("samenstelling", "geheel", "Vondst", "brokstuk", "Brokstuk", "brokstukken")));

    private static final String BOUND = binderRead("grens");
    private static final String THRESHOLD = ownRead("drempel", "Jutter");

    /** "het aantal vondsten van de Jutter" whose member passes {@code filter}. */
    private static String count(String filter) {
        return aggregation("count", "{\"node\":\"AttributeReference\",\"attribute\":\"vondsten\",\"subjectType\":"
                + "\"Jutter\",\"source\":{\"snippet\":\"alle vondsten van de Jutter\"}}", "Vondst",
                sourceRole("vinding", "vondst"), filter, "null", WHOLE, "het aantal vondsten van de Jutter met …");
    }

    /** "alle brokstukken van de Vondst [die {@code filter}] voldoen aan {@code toets}". */
    private static String quantifierFold(String filter, String test) {
        return "{\"node\":\"QuantifierFold\",\"quantifier\":\"all\",\"elementType\":\"Brokstuk\""
                + (filter == null ? "" : ",\"filter\":" + filter) + ",\"test\":" + test + ","
                + sourceRole("samenstelling", "brokstuk")
                + ",\"source\":{\"snippet\":\"alle brokstukken van de Vondst\"}}";
    }

    private static String greater(String left, String right) {
        return comparison("greater", left, right);
    }

    private static final String VONDSTGEWICHT = ownRead("gewicht", "Vondst");
    private static final String BROK_WEIGHT = ownRead("gewicht", "Brokstuk");

    private static Path document(String waarde) throws Exception {
        return TmsDocuments.document(MODEL, List.of(computingRule("zware vondsten van een jutter", "BEREKENING", "Jutter",
                "zwaar", waarde, "null")));
    }

    private static String refusal(Path ir) throws Exception {
        try {
            DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL);
            return "gebouwd";
        } catch (CaseRunner.Excluded e) {
            return e.getMessage();
        }
    }

    private static final String STILL_NOT = "transpiler SKIP: regel: zware vondsten van een jutter — een"
            + " BinderRef 'grens' van een Let om een vouw, in het filter of de matrix van die vouw ('grens'): de"
            + " bijdrageregels van de vouw rekenen het filter en de matrix per lid uit en binden de waarde van de Let"
            + " niet: deze tak draagt deze vorm nog niet";

    @Test void a_binder_in_the_filter_or_the_matrix_of_a_fold_waits_for_a_later_card() throws Exception {
        List<String> werkelijk = new ArrayList<>();
        for (String waarde : List.of(
                let("grens", THRESHOLD, count(greater(VONDSTGEWICHT, BOUND)), WHOLE),
                let("grens", THRESHOLD, count(quantifierFold(null, greater(BROK_WEIGHT, BOUND))), WHOLE),
                let("grens", THRESHOLD, count(quantifierFold(greater(BROK_WEIGHT, BOUND), greater(BROK_WEIGHT, number("6")))),
                        WHOLE)))
            werkelijk.add(refusal(document(waarde)));
        assertEquals(List.of(STILL_NOT, STILL_NOT, STILL_NOT), werkelijk);
    }

    @Test void a_binder_without_enclosing_let_keeps_the_reason_of_wf064() throws Exception {
        assertEquals("transpiler SKIP: regel: zware vondsten van een jutter — een BinderRef 'grens' zonder"
                + " omsluitende Let die hem bindt ('grens', WF-064)",
                refusal(document(count(greater(VONDSTGEWICHT, BOUND)))));
    }

    @Test void a_let_in_the_filter_plays() throws Exception {
        String six = let("grens", number("6"), BOUND, WHOLE);
        Path ir = document(let("grens", THRESHOLD, count(greater(VONDSTGEWICHT, six)), WHOLE));
        List<String> instanties = new ArrayList<>(List.of(
                instanceWithRoles("J1", "Jutter", Map.of("drempel", "5"), List.of(), Map.of("vondsten",
                        List.of("V1", "V2", "V3"))),
                instanceWithRoles("J2", "Jutter", Map.of("drempel", "2"), List.of(), Map.of("vondsten",
                        List.of("V4")))));
        Map<String, String> gewichten = Map.of("V1", "3", "V2", "7", "V3", "9", "V4", "3");
        gewichten.forEach((label, weight) -> instanties.add(instanceWithRoles(label, "Vondst",
                Map.of("gewicht", weight), List.of(), Map.of())));
        assertNull(CaseRunner.playCase(ir, TmsDocuments.caseDocument(instanties, List.of(waarde("J1", "zwaar", "2"),
                waarde("J2", "zwaar", "0"))), DroolsBackend.Branch.EXEC_MODEL));
    }
}
