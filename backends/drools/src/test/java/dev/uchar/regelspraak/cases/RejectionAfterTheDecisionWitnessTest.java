package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.uchar.regelspraak.transpiler.tms.TextOrder;
import org.drools.model.Rule;
import org.junit.jupiter.api.Test;
import org.kie.api.KieBase;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Two object creations without a connectingRole binding, the first of which reads, negatively, the relation the second
 * makes (card drl-teksttak-op-truth-maintenance, section "Na 20261010-objectcreatie-zonder-connectingrole-binding"). The
 * document {@code src/test/resources/objectcreatie/afwijzing-na-het-besluit} comes from the RegelSpraak frontend, its
 * {@code source.rgs} beside it: "wijs af zonder besluit" makes an Afwijzing for a Jutter that is no begunstigde, and "stel
 * het penningbesluit vast" makes the Penningbesluit that makes a penninggerechtigde Jutter one. The first reads the
 * relation the second derives from its type pair, so it stands a stratum above it (RUNTIME-SEMANTICS ch. 6.1, 6.3): J1
 * (14 vondsten against the vondstdrempel 12) is penninggerechtigd and gets no Afwijzing, J2 (11) gets one.
 *
 * <p>Both orders of the two creations in the document play on the exec-model branch and on {@code DRL_TEXT}; and both
 * orders of the two creation rules fire on both, on {@code DRL_TEXT} with the rules of the text in that order
 * ({@code TextOrder}), since the order of the rules in the text is the order in which Drools loads them, and with it
 * the order of the agenda among the creations of one generation.
 *
 * <p><b>Measured</b> by the builder of card 20261010-objectcreatie-zonder-connectingrole-binding: verifier PASS, the
 * reference interpreter and the ASP runner PASS (J1 0 afwijzingen); the old DRL-text branch, which this card leaves as it
 * is until the switch-over, gives "FAIL case.json: attribuut aantal afwijzingen: verwacht 0, werkelijk 1".
 * <b>Red</b> on {@code b4d83a69}, before the rendering carried the creations: both tests failed on {@code DRL_TMS} with
 * "drl-tms: de DRL-weergave draagt Creatie nog niet".
 */
class RejectionAfterTheDecisionWitnessTest {

    // cwd under surefire is the module dir (backends/drools)
    private static final Path MAP = Path.of("src", "test", "resources", "object-creation", "rejection-after-the-decision");

    /** The two creation rules, by the names of their plans. */
    private static final List<String> CREATIONS = List.of("wijs af zonder besluit", "stel het penningbesluit vast");

    private static JsonNode caseDocument() throws Exception {
        return TmsDocuments.JSON.readTree(Files.readString(MAP.resolve("case.json")));
    }

    /** The document with its object creations in the order {@code order} of their places in the file. */
    private static Path document(List<Integer> order) throws Exception {
        ObjectNode ir = (ObjectNode) TmsDocuments.JSON.readTree(Files.readString(MAP.resolve("ir.json")));
        ArrayNode creations = (ArrayNode) ir.get("rules").get("objectCreations");
        ArrayNode ordered = TmsDocuments.JSON.createArrayNode();
        for (int i : order) ordered.add(creations.get(i));
        ((ObjectNode) ir.get("rules")).set("objectCreations", ordered);
        Path tmp = Files.createTempFile("afwijzing", ".json");
        tmp.toFile().deleteOnExit();
        return Files.writeString(tmp, TmsDocuments.JSON.writeValueAsString(ir));
    }

    @Test void every_document_order_on_both_branches() throws Exception {
        int rows = 0;
        for (List<Integer> order : TmsDocuments.permutations(List.of(0, 1))) {
            Path ir = document(order);
            for (DroolsBackend.Branch branch : List.of(DroolsBackend.Branch.EXEC_MODEL, DroolsBackend.Branch.DRL_TEXT)) {
                assertNull(CaseRunner.playCase(ir, caseDocument(), branch), branch + ": de creaties in volgorde " + order);
                rows++;
            }
        }
        assertEquals(4, rows);
    }

    @Test void every_order_of_the_creation_rules_also_in_the_text() throws Exception {
        Path ir = MAP.resolve("ir.json");
        DroolsBackend.Build b = DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL);
        List<Rule> generated = b.execModelTranspiler().model().getRules();
        DroolsBackend.Build t = DroolsBackend.build(ir, DroolsBackend.Branch.DRL_TEXT);
        List<String> rules = TextOrder.rules(t);
        assertEquals(generated.stream().map(Rule::getName).toList(), rules, "de regels van de tekst");
        List<Integer> places = new ArrayList<>();
        for (int i = 0; i < rules.size(); i++) if (CREATIONS.contains(rules.get(i))) places.add(i);
        assertEquals(2, places.size(), () -> "de twee creatieregels in " + rules);
        int orders = 0;
        for (List<Integer> p : TmsDocuments.permutations(places)) {
            List<Rule> model = new ArrayList<>(generated);
            List<String> text = new ArrayList<>(rules);
            for (int k = 0; k < places.size(); k++) {
                model.set(places.get(k), generated.get(p.get(k)));
                text.set(places.get(k), rules.get(p.get(k)));
            }
            fire(b, TmsDocuments.kieBase(b, model), text);
            fire(t, TextOrder.kieBase(t, text), text);
            orders++;
        }
        assertEquals(2, orders);
    }

    /** Fires J1 and J2 of the case through {@code kieBase}, the rules of {@code b} in the order {@code namen}, with the
     *  parameters of the case, and writes back: J1 penninggerechtigd without an Afwijzing, J2 with one. */
    private static void fire(DroolsBackend.Build b, KieBase kieBase, List<String> names) throws Exception {
        Object j1 = TmsDocuments.beanObject(b.beanLoader(), "Jutter", Map.of("aantal aangemelde vondsten", "14",
                "penningjaar", "2025"), List.of());
        Object j2 = TmsDocuments.beanObject(b.beanLoader(), "Jutter", Map.of("aantal aangemelde vondsten", "11",
                "penningjaar", "2025"), List.of());
        String where = " op " + b.branch() + " in volgorde " + names;
        TmsDocuments.fire(b, kieBase, names, List.of(j1, j2), session -> {
            session.setGlobal("vondstdrempel", new BigDecimal("12"));
            session.setGlobal("penningbedrag", new BigDecimal("250"));
        }, session -> { });
        assertTrue(TmsDocuments.has(j1, "penninggerechtigd"), "J1 penninggerechtigd" + where);
        assertFalse(TmsDocuments.has(j2, "penninggerechtigd"), "J2 penninggerechtigd" + where);
        TmsFaultWitnessTest.assertNumber("0", TmsDocuments.read(j1, "aantal afwijzingen"), "J1 aantal afwijzingen" + where);
        TmsFaultWitnessTest.assertNumber("1", TmsDocuments.read(j2, "aantal afwijzingen"), "J2 aantal afwijzingen" + where);
    }
}
