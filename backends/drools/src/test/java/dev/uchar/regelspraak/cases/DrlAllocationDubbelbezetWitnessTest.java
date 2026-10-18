package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.uchar.regelspraak.transpiler.tms.TextOrder;
import org.drools.model.Rule;
import org.junit.jupiter.api.Test;
import org.kie.api.KieBase;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Two allocations over one shared recipient with an empty weight, through both branches with truth maintenance (card
 * drl-verdeling-dubbelbezet, taken up as acceptance (a) of card drl-teksttak-op-truth-maintenance). The document, the
 * corpus golden {@code jutterswet-verdeling-dubbelbezet}, is part B of {@link TmsAllocationWitnessTest}: a Berging B1 with
 * opbrengst 1000 over the Bergers D1 (gewicht 3, jaar 2019), D2 (no gewicht, jaar 2020) and D3 (gewicht 1, jaar 2020),
 * each with maximum 600; "verdeel de opbrengst naar rato" over the gewicht into the bergingsaandeel, its rest into the
 * onverdeelde rest, and "verdeel de opbrengst op jaar" in the order of the jaar, weighted by the gewicht, into the
 * rangaandeel, its rest into the rangrest. D2 receives in neither, and its empty weight is the §9.5 inconsistency of
 * each allocation on D2; D1 receives 600 and 600, D3 250 and 400, and the rests are 150 and 0.
 *
 * <p>Both orders of the two allocations in the document play on the exec-model branch and on {@code DRL_TEXT}; and
 * every order of the four rules of the allocations, a recipient rule and a distribution rule each, the other rules in
 * their place, fires on both, on {@code DRL_TEXT} with the rules of the text in that order ({@code TextOrder}),
 * since the order of the rules in the text is the order in which Drools loads them.
 *
 * <p><b>Measured</b> (2026-10-13, when the document was the card's own and lay in the test resources): verifier PASS;
 * the reference interpreter and the ASP runner PASS with the ten expectations of the case; the exec-model branch
 * PLAYED 1. The old DRL-text branch, which card drl-teksttak-op-truth-maintenance left as it was until the switch-over,
 * failed the case with "ConsequenceException: Exception executing consequence for rule "verdeel de opbrengst naar rato"
 * … cannot invoke method: setBergingsaandeel", under it a NullPointerException on the empty weight of D2. Since step 6b
 * of the card the document is a golden of the corpus, which gates 5, 6 and 12 replay in the reference interpreter, the
 * ASP backend and both branches; its NOTE.md carries the hand computation. <b>Red</b> on {@code abe891c1}, before the
 * rendering carried the allocations: both tests failed on {@code DRL_TMS} with "drl-tms: de DRL-weergave draagt
 * Ontvanger nog niet".
 */
class DrlAllocationDubbelbezetWitnessTest {

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up
    private static final Path MAP = Path.of("..", "..", "corpus", "golden", "jutterswet-verdeling-dubbelbezet");

    private static JsonNode caseDocument() throws Exception {
        return TmsDocuments.JSON.readTree(Files.readString(MAP.resolve("case.json")));
    }

    /** The document with its allocations in the order {@code order} of their places in the file. */
    private static Path document(List<Integer> order) throws Exception {
        ObjectNode ir = (ObjectNode) TmsDocuments.JSON.readTree(Files.readString(MAP.resolve("ir.json")));
        ArrayNode allocations = (ArrayNode) ir.get("rules").get("allocations");
        ArrayNode ordered = TmsDocuments.JSON.createArrayNode();
        for (int i : order) ordered.add(allocations.get(i));
        ((ObjectNode) ir.get("rules")).set("allocations", ordered);
        Path tmp = Files.createTempFile("dubbelbezet", ".json");
        tmp.toFile().deleteOnExit();
        return Files.writeString(tmp, TmsDocuments.JSON.writeValueAsString(ir));
    }

    @Test void every_document_order_on_both_branches() throws Exception {
        int rows = 0;
        for (List<Integer> order : TmsDocuments.permutations(List.of(0, 1))) {
            Path ir = document(order);
            for (DroolsBackend.Branch branch : List.of(DroolsBackend.Branch.EXEC_MODEL, DroolsBackend.Branch.DRL_TEXT)) {
                assertNull(CaseRunner.playCase(ir, caseDocument(), branch), branch + ": de verdelingen in volgorde " + order);
                rows++;
            }
        }
        assertEquals(4, rows);
    }

    @Test void every_order_of_the_rules_of_the_allocations() throws Exception {
        Path ir = MAP.resolve("ir.json");
        DroolsBackend.Build b = DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL);
        List<Rule> gegenereerd = b.execModelTranspiler().model().getRules();
        DroolsBackend.Build t = DroolsBackend.build(ir, DroolsBackend.Branch.DRL_TEXT);
        List<String> rules = TextOrder.rules(t);
        assertEquals(gegenereerd.stream().map(Rule::getName).toList(), rules, "de regels van de tekst");
        List<Integer> places = new ArrayList<>();
        for (int i = 0; i < rules.size(); i++) if (rules.get(i).startsWith("verdeel de opbrengst")) places.add(i);
        assertEquals(4, places.size(), () -> "de regels van de twee verdelingen in " + rules);
        int orders = 0;
        for (List<Integer> p : TmsDocuments.permutations(places)) {
            List<Rule> model = new ArrayList<>(gegenereerd);
            List<String> text = new ArrayList<>(rules);
            for (int k = 0; k < places.size(); k++) {
                model.set(places.get(k), gegenereerd.get(p.get(k)));
                text.set(places.get(k), rules.get(p.get(k)));
            }
            vuur(b, TmsDocuments.kieBase(b, model), text);
            vuur(t, TextOrder.kieBase(t, text), text);
            orders++;
        }
        assertEquals(24, orders);
    }

    /** Fires B1 and its three Bergers through {@code kieBase}, the rules of {@code b} in the order {@code namen}, and
     *  writes back: the shares, the rests and the two inconsistencies on D2 of the class comment. */
    private static void vuur(DroolsBackend.Build b, KieBase kieBase, List<String> names) throws Exception {
        Object b1 = TmsDocuments.beanObject(b.beanLoader(), "Berging", Map.of("opbrengst", "1000"), List.of());
        Object d1 = TmsDocuments.beanObject(b.beanLoader(), "Berger", Map.of("gewicht", "3", "jaar", "2019", "maximum", "600"),
                List.of());
        Object d2 = TmsDocuments.beanObject(b.beanLoader(), "Berger", Map.of("jaar", "2020", "maximum", "600"), List.of());
        Object d3 = TmsDocuments.beanObject(b.beanLoader(), "Berger", Map.of("gewicht", "1", "jaar", "2020", "maximum", "600"),
                List.of());
        for (Object d : List.of(d1, d2, d3)) TmsAllocationWitnessTest.join(d, b1);
        Map<Object, String> labels = TmsAllocationWitnessTest.labels(List.of(b1, d1, d2, d3),
                List.of("B1", "D1", "D2", "D3"));
        List<String> channel = new ArrayList<>();
        String where = " op " + b.branch() + " in volgorde " + names;
        TmsDocuments.vuur(b, kieBase, names, List.of(b1, d1, d2, d3), sessie -> { },
                sessie -> channel.addAll(TmsDocuments.inconsistenties(sessie, labels)));
        assertEquals(List.of("verdeel de opbrengst naar rato @ D2", "verdeel de opbrengst op jaar @ D2"), channel,
                "de inconsistenties" + where);
        TmsFaultWitnessTest.assertNumber("600", TmsDocuments.lees(d1, "bergingsaandeel"), "D1 bergingsaandeel" + where);
        assertNull(TmsDocuments.lees(d2, "bergingsaandeel"), "D2 bergingsaandeel" + where);
        TmsFaultWitnessTest.assertNumber("250", TmsDocuments.lees(d3, "bergingsaandeel"), "D3 bergingsaandeel" + where);
        TmsFaultWitnessTest.assertNumber("150", TmsDocuments.lees(b1, "onverdeelde rest"), "B1 onverdeelde rest" + where);
        TmsFaultWitnessTest.assertNumber("600", TmsDocuments.lees(d1, "rangaandeel"), "D1 rangaandeel" + where);
        TmsFaultWitnessTest.assertNumber("0", TmsDocuments.lees(d2, "rangaandeel"), "D2 rangaandeel" + where);
        TmsFaultWitnessTest.assertNumber("400", TmsDocuments.lees(d3, "rangaandeel"), "D3 rangaandeel" + where);
        TmsFaultWitnessTest.assertNumber("0", TmsDocuments.lees(b1, "rangrest"), "B1 rangrest" + where);
    }
}
