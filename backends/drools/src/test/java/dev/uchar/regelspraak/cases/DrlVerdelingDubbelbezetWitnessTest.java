package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.uchar.regelspraak.transpiler.tms.Tekstvolgorde;
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
 * drl-verdeling-dubbelbezet, taken up as acceptance (a) of card drl-teksttak-op-truth-maintenance). The document of its
 * own, {@code src/test/resources/verdeling/dubbelbezet}, is part B of {@link TmsVerdelingWitnessTest}: a Berging B1 with
 * opbrengst 1000 over the Bergers D1 (gewicht 3, jaar 2019), D2 (no gewicht, jaar 2020) and D3 (gewicht 1, jaar 2020),
 * each with maximum 600; "verdeel de opbrengst naar rato" over the gewicht into the bergingsaandeel, its rest into the
 * onverdeelde rest, and "verdeel de opbrengst op jaar" in the order of the jaar, weighted by the gewicht, into the
 * rangaandeel, its rest into the rangrest. D2 receives in neither, and its empty weight is the §9.5 inconsistency of
 * each allocation on D2; D1 receives 600 and 600, D3 250 and 400, and the rests are 150 and 0.
 *
 * <p>Both orders of the two allocations in the document play on the exec-model branch and on {@code DRL_TEXT}; and
 * every order of the four rules of the allocations, a recipient rule and a distribution rule each, the other rules in
 * their place, fires on both, on {@code DRL_TEXT} with the rules of the text in that order ({@code Tekstvolgorde}),
 * since the order of the rules in the text is the order in which Drools loads them.
 *
 * <p><b>Measured</b> (2026-10-13; raw output with the measurements of the card, {@code stap5/dubbelbezet-ruw.txt}):
 * verifier PASS; the reference interpreter and the ASP runner PASS with the ten expectations of the case; the
 * exec-model branch PLAYED 1. The old DRL-text branch, which card drl-teksttak-op-truth-maintenance leaves as it is until
 * the switch-over, fails the case with "ConsequenceException: Exception executing consequence for rule "verdeel de
 * opbrengst naar rato" … cannot invoke method: setBergingsaandeel", under it a NullPointerException on the empty weight
 * of D2. <b>Red</b> on {@code abe891c1}, before the rendering carried the allocations: both tests failed on
 * {@code DRL_TMS} with "drl-tms: de DRL-weergave draagt Ontvanger nog niet".
 */
class DrlVerdelingDubbelbezetWitnessTest {

    // cwd under surefire is the module dir (backends/drools)
    private static final Path MAP = Path.of("src", "test", "resources", "verdeling", "dubbelbezet");

    private static JsonNode zaak() throws Exception {
        return TmsDocumenten.JSON.readTree(Files.readString(MAP.resolve("case.json")));
    }

    /** The document with its allocations in the order {@code volgorde} of their places in the file. */
    private static Path document(List<Integer> volgorde) throws Exception {
        ObjectNode ir = (ObjectNode) TmsDocumenten.JSON.readTree(Files.readString(MAP.resolve("ir.json")));
        ArrayNode verdelingen = (ArrayNode) ir.get("rules").get("allocations");
        ArrayNode geordend = TmsDocumenten.JSON.createArrayNode();
        for (int i : volgorde) geordend.add(verdelingen.get(i));
        ((ObjectNode) ir.get("rules")).set("allocations", geordend);
        Path tmp = Files.createTempFile("dubbelbezet", ".json");
        tmp.toFile().deleteOnExit();
        return Files.writeString(tmp, TmsDocumenten.JSON.writeValueAsString(ir));
    }

    @Test void elke_documentvolgorde_op_beide_takken() throws Exception {
        int rijen = 0;
        for (List<Integer> volgorde : TmsDocumenten.permutaties(List.of(0, 1))) {
            Path ir = document(volgorde);
            for (DroolsBackend.Branch tak : List.of(DroolsBackend.Branch.EXEC_MODEL, DroolsBackend.Branch.DRL_TEXT)) {
                assertNull(CaseRunner.speelCase(ir, zaak(), tak), tak + ": de verdelingen in volgorde " + volgorde);
                rijen++;
            }
        }
        assertEquals(4, rijen);
    }

    @Test void elke_volgorde_van_de_regels_van_de_verdelingen() throws Exception {
        Path ir = MAP.resolve("ir.json");
        DroolsBackend.Build b = DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL);
        List<Rule> gegenereerd = b.execModelTranspiler().model().getRules();
        DroolsBackend.Build t = DroolsBackend.build(ir, DroolsBackend.Branch.DRL_TEXT);
        List<String> regels = Tekstvolgorde.regels(t);
        assertEquals(gegenereerd.stream().map(Rule::getName).toList(), regels, "de regels van de tekst");
        List<Integer> plaatsen = new ArrayList<>();
        for (int i = 0; i < regels.size(); i++) if (regels.get(i).startsWith("verdeel de opbrengst")) plaatsen.add(i);
        assertEquals(4, plaatsen.size(), () -> "de regels van de twee verdelingen in " + regels);
        int volgordes = 0;
        for (List<Integer> p : TmsDocumenten.permutaties(plaatsen)) {
            List<Rule> model = new ArrayList<>(gegenereerd);
            List<String> tekst = new ArrayList<>(regels);
            for (int k = 0; k < plaatsen.size(); k++) {
                model.set(plaatsen.get(k), gegenereerd.get(p.get(k)));
                tekst.set(plaatsen.get(k), regels.get(p.get(k)));
            }
            vuur(b, TmsDocumenten.kieBase(b, model), tekst);
            vuur(t, Tekstvolgorde.kieBase(t, tekst), tekst);
            volgordes++;
        }
        assertEquals(24, volgordes);
    }

    /** Fires B1 and its three Bergers through {@code kieBase}, the rules of {@code b} in the order {@code namen}, and
     *  writes back: the shares, the rests and the two inconsistencies on D2 of the class comment. */
    private static void vuur(DroolsBackend.Build b, KieBase kieBase, List<String> namen) throws Exception {
        Object b1 = TmsDocumenten.boon(b.beanLoader(), "Berging", Map.of("opbrengst", "1000"), List.of());
        Object d1 = TmsDocumenten.boon(b.beanLoader(), "Berger", Map.of("gewicht", "3", "jaar", "2019", "maximum", "600"),
                List.of());
        Object d2 = TmsDocumenten.boon(b.beanLoader(), "Berger", Map.of("jaar", "2020", "maximum", "600"), List.of());
        Object d3 = TmsDocumenten.boon(b.beanLoader(), "Berger", Map.of("gewicht", "1", "jaar", "2020", "maximum", "600"),
                List.of());
        for (Object d : List.of(d1, d2, d3)) TmsVerdelingWitnessTest.koppel(d, b1);
        Map<Object, String> labels = TmsVerdelingWitnessTest.labels(List.of(b1, d1, d2, d3),
                List.of("B1", "D1", "D2", "D3"));
        List<String> kanaal = new ArrayList<>();
        String waar = " op " + b.branch() + " in volgorde " + namen;
        TmsDocumenten.vuur(b, kieBase, namen, List.of(b1, d1, d2, d3), sessie -> { },
                sessie -> kanaal.addAll(TmsDocumenten.inconsistenties(sessie, labels)));
        assertEquals(List.of("verdeel de opbrengst naar rato @ D2", "verdeel de opbrengst op jaar @ D2"), kanaal,
                "de inconsistenties" + waar);
        TmsFoutWitnessTest.assertGetal("600", TmsDocumenten.lees(d1, "bergingsaandeel"), "D1 bergingsaandeel" + waar);
        assertNull(TmsDocumenten.lees(d2, "bergingsaandeel"), "D2 bergingsaandeel" + waar);
        TmsFoutWitnessTest.assertGetal("250", TmsDocumenten.lees(d3, "bergingsaandeel"), "D3 bergingsaandeel" + waar);
        TmsFoutWitnessTest.assertGetal("150", TmsDocumenten.lees(b1, "onverdeelde rest"), "B1 onverdeelde rest" + waar);
        TmsFoutWitnessTest.assertGetal("600", TmsDocumenten.lees(d1, "rangaandeel"), "D1 rangaandeel" + waar);
        TmsFoutWitnessTest.assertGetal("0", TmsDocumenten.lees(d2, "rangaandeel"), "D2 rangaandeel" + waar);
        TmsFoutWitnessTest.assertGetal("400", TmsDocumenten.lees(d3, "rangaandeel"), "D3 rangaandeel" + waar);
        TmsFoutWitnessTest.assertGetal("0", TmsDocumenten.lees(b1, "rangrest"), "B1 rangrest" + waar);
    }
}
