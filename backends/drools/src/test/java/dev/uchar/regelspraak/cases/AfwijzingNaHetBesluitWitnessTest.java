package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.uchar.regelspraak.transpiler.tms.Tekstvolgorde;
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
 * {@code bron.rgs} beside it: "wijs af zonder besluit" makes an Afwijzing for a Jutter that is no begunstigde, and "stel
 * het penningbesluit vast" makes the Penningbesluit that makes a penninggerechtigde Jutter one. The first reads the
 * relation the second derives from its type pair, so it stands a stratum above it (RUNTIME-SEMANTICS ch. 6.1, 6.3): J1
 * (14 vondsten against the vondstdrempel 12) is penninggerechtigd and gets no Afwijzing, J2 (11) gets one.
 *
 * <p>Both orders of the two creations in the document play on the exec-model branch and on {@code DRL_TEXT}; and both
 * orders of the two creation rules fire on both, on {@code DRL_TEXT} with the rules of the text in that order
 * ({@code Tekstvolgorde}), since the order of the rules in the text is the order in which Drools loads them, and with it
 * the order of the agenda among the creations of one generation.
 *
 * <p><b>Measured</b> by the builder of card 20261010-objectcreatie-zonder-connectingrole-binding: verifier PASS, the
 * reference interpreter and the ASP runner PASS (J1 0 afwijzingen); the old DRL-text branch, which this card leaves as it
 * is until the switch-over, gives "FAIL case.json: attribuut aantal afwijzingen: verwacht 0, werkelijk 1".
 * <b>Red</b> on {@code b4d83a69}, before the rendering carried the creations: both tests failed on {@code DRL_TMS} with
 * "drl-tms: de DRL-weergave draagt Creatie nog niet".
 */
class AfwijzingNaHetBesluitWitnessTest {

    // cwd under surefire is the module dir (backends/drools)
    private static final Path MAP = Path.of("src", "test", "resources", "objectcreatie", "afwijzing-na-het-besluit");

    /** The two creation rules, by the names of their plans. */
    private static final List<String> CREATIES = List.of("wijs af zonder besluit", "stel het penningbesluit vast");

    private static JsonNode zaak() throws Exception {
        return TmsDocumenten.JSON.readTree(Files.readString(MAP.resolve("case.json")));
    }

    /** The document with its object creations in the order {@code volgorde} of their places in the file. */
    private static Path document(List<Integer> volgorde) throws Exception {
        ObjectNode ir = (ObjectNode) TmsDocumenten.JSON.readTree(Files.readString(MAP.resolve("ir.json")));
        ArrayNode creaties = (ArrayNode) ir.get("rules").get("objectCreations");
        ArrayNode geordend = TmsDocumenten.JSON.createArrayNode();
        for (int i : volgorde) geordend.add(creaties.get(i));
        ((ObjectNode) ir.get("rules")).set("objectCreations", geordend);
        Path tmp = Files.createTempFile("afwijzing", ".json");
        tmp.toFile().deleteOnExit();
        return Files.writeString(tmp, TmsDocumenten.JSON.writeValueAsString(ir));
    }

    @Test void elke_documentvolgorde_op_beide_takken() throws Exception {
        int rijen = 0;
        for (List<Integer> volgorde : TmsDocumenten.permutaties(List.of(0, 1))) {
            Path ir = document(volgorde);
            for (DroolsBackend.Branch tak : List.of(DroolsBackend.Branch.EXEC_MODEL, DroolsBackend.Branch.DRL_TEXT)) {
                assertNull(CaseRunner.speelCase(ir, zaak(), tak), tak + ": de creaties in volgorde " + volgorde);
                rijen++;
            }
        }
        assertEquals(4, rijen);
    }

    @Test void elke_volgorde_van_de_creatieregels_ook_in_de_tekst() throws Exception {
        Path ir = MAP.resolve("ir.json");
        DroolsBackend.Build b = DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL);
        List<Rule> gegenereerd = b.execModelTranspiler().model().getRules();
        DroolsBackend.Build t = DroolsBackend.build(ir, DroolsBackend.Branch.DRL_TEXT);
        List<String> regels = Tekstvolgorde.regels(t);
        assertEquals(gegenereerd.stream().map(Rule::getName).toList(), regels, "de regels van de tekst");
        List<Integer> plaatsen = new ArrayList<>();
        for (int i = 0; i < regels.size(); i++) if (CREATIES.contains(regels.get(i))) plaatsen.add(i);
        assertEquals(2, plaatsen.size(), () -> "de twee creatieregels in " + regels);
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
        assertEquals(2, volgordes);
    }

    /** Fires J1 and J2 of the case through {@code kieBase}, the rules of {@code b} in the order {@code namen}, with the
     *  parameters of the case, and writes back: J1 penninggerechtigd without an Afwijzing, J2 with one. */
    private static void vuur(DroolsBackend.Build b, KieBase kieBase, List<String> namen) throws Exception {
        Object j1 = TmsDocumenten.boon(b.beanLoader(), "Jutter", Map.of("aantal aangemelde vondsten", "14",
                "penningjaar", "2025"), List.of());
        Object j2 = TmsDocumenten.boon(b.beanLoader(), "Jutter", Map.of("aantal aangemelde vondsten", "11",
                "penningjaar", "2025"), List.of());
        String waar = " op " + b.branch() + " in volgorde " + namen;
        TmsDocumenten.vuur(b, kieBase, namen, List.of(j1, j2), sessie -> {
            sessie.setGlobal("vondstdrempel", new BigDecimal("12"));
            sessie.setGlobal("penningbedrag", new BigDecimal("250"));
        }, sessie -> { });
        assertTrue(TmsDocumenten.heeft(j1, "penninggerechtigd"), "J1 penninggerechtigd" + waar);
        assertFalse(TmsDocumenten.heeft(j2, "penninggerechtigd"), "J2 penninggerechtigd" + waar);
        TmsFoutWitnessTest.assertGetal("0", TmsDocumenten.lees(j1, "aantal afwijzingen"), "J1 aantal afwijzingen" + waar);
        TmsFoutWitnessTest.assertGetal("1", TmsDocumenten.lees(j2, "aantal afwijzingen"), "J2 aantal afwijzingen" + waar);
    }
}
