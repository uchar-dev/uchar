package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import org.drools.model.Rule;
import org.junit.jupiter.api.Test;
import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.DefaultAgendaEventListener;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocumenten.besluit;
import static dev.uchar.regelspraak.cases.TmsDocumenten.check;
import static dev.uchar.regelspraak.cases.TmsDocumenten.consistent;
import static dev.uchar.regelspraak.cases.TmsDocumenten.kenmerkgroep;
import static dev.uchar.regelspraak.cases.TmsDocumenten.kenmerkregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.verwacht;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A defaults group on a characteristic (RUNTIME-SEMANTICS ch. 7.1 and 7.8): {@code vrijgesteld} of a Jutter, decided
 * do-not-attribute where it is {@code ny} and attribute where it is {@code nx}, in that order. A justified
 * characteristic default always decides, so the do-not-attribute decision displaces the attribution below it and the
 * group derives nothing; that is not a retraction, so a {@code vrijgesteld} the case input sets stays set. {@code ny}
 * is derived from an input by a rule, so the lower default can attribute before the higher one decides, and that
 * attribution has to go again. Played through the replay, and fired in a seeded sample of orders of the generated
 * rules, which must take that path somewhere.
 *
 * <p><b>Red before the lowering</b> (measured 2026-10-08, the branch refusing every defaults group): the replay and the
 * sample failed on the build, "het_besluit_onwaar_verdringt_en_de_invoer_blijft_staan:79 » Excluded tms-transpiler SKIP:
 * defaults: vrijstelling van een jutter — een PrioritizedDefaults: de truth-maintenance-tak draagt deze vorm nog niet";
 * the exec-model branch passed the same case.
 */
class TmsKenmerkbesluitWitnessTest {

    private static final String MODEL = TmsDocumenten.model(List.of("nx", "ny", "y0", "vrijgesteld"));

    private static final List<String> REGELS = List.of(kenmerkregel("grond ny", "ny", check("y0", false)));

    private static final String GROEP = kenmerkgroep("vrijstelling van een jutter", "Jutter", "vrijgesteld", List.of(
            besluit(check("ny", false), false),
            besluit(check("nx", false), true)));

    /** Per object its input characteristics and whether it ends {@code vrijgesteld}. */
    private static final Map<String, List<String>> INVOER = new LinkedHashMap<>();
    private static final Map<String, Boolean> VRIJGESTELD = new LinkedHashMap<>();
    static {
        geval("J1", List.of("nx"), true);
        geval("J2", List.of("nx", "y0"), false);
        geval("J3", List.of("y0", "vrijgesteld"), true);
        geval("J4", List.of("nx", "y0", "vrijgesteld"), true);
        geval("J5", List.of(), false);
        geval("J6", List.of("nx", "vrijgesteld"), true);
    }

    private static void geval(String label, List<String> kenmerken, boolean vrijgesteld) {
        INVOER.put(label, kenmerken);
        VRIJGESTELD.put(label, vrijgesteld);
    }

    private static JsonNode zaak() throws Exception {
        List<String> instanties = new ArrayList<>(), verwachtingen = new ArrayList<>();
        INVOER.forEach((label, k) -> {
            instanties.add(TmsDocumenten.jutter(label, k));
            verwachtingen.add(verwacht(label, "vrijgesteld", VRIJGESTELD.get(label)));
        });
        verwachtingen.add(consistent());
        return TmsDocumenten.zaak(instanties, verwachtingen);
    }

    private static Path document() throws Exception {
        return TmsDocumenten.document(MODEL, REGELS, List.of(), List.of(GROEP));
    }

    @Test void het_besluit_onwaar_verdringt_en_de_invoer_blijft_staan() throws Exception {
        assertNull(CaseRunner.speelCase(document(), zaak(), DroolsBackend.Branch.EXEC_MODEL));
    }

    @Test void steekproef_van_volgordes_van_de_gegenereerde_regels() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(document(), DroolsBackend.Branch.EXEC_MODEL);
        List<Rule> gegenereerd = b.execModelTranspiler().model().getRules();
        int[] verdrongen = {0};
        for (List<Rule> volgorde : TmsDocumenten.steekproef(gegenereerd, 20261008L, 100)) {
            List<String> namen = volgorde.stream().map(Rule::getName).toList();
            Map<Object, String> labels = new IdentityHashMap<>();
            List<Object> bonen = new ArrayList<>();
            for (Map.Entry<String, List<String>> e : INVOER.entrySet()) {
                Object boon = TmsDocumenten.jutterBoon(b.beanLoader(), e.getValue());
                labels.put(boon, e.getKey());
                bonen.add(boon);
            }
            Object j2 = bonen.get(1);
            TmsDocumenten.vuur(b, volgorde, bonen, sessie -> sessie.addEventListener(new DefaultAgendaEventListener() {
                @Override public void afterMatchFired(AfterMatchFiredEvent e) {
                    if (e.getMatch().getRule().getName().endsWith("default 1")
                            && e.getMatch().getObjects().stream().anyMatch(o -> o == j2))
                        verdrongen[0]++;
                }
            }), sessie -> { });
            for (Object boon : bonen)
                assertEquals(VRIJGESTELD.get(labels.get(boon)), TmsDocumenten.heeft(boon, "vrijgesteld"),
                        () -> labels.get(boon) + " vrijgesteld in volgorde " + namen);
        }
        assertTrue(verdrongen[0] > 0,
                "geen volgorde liet de toekenning eerst vuren en het besluit onwaar haar verdringen");
    }
}
