package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import dev.uchar.regelspraak.transpiler.tms.Beslist;
import dev.uchar.regelspraak.transpiler.tms.Stuurfeit;
import org.drools.model.Rule;
import org.junit.jupiter.api.Test;
import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.DefaultAgendaEventListener;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocumenten.GEHEEL;
import static dev.uchar.regelspraak.cases.TmsDocumenten.attribuut;
import static dev.uchar.regelspraak.cases.TmsDocumenten.attribuutgroep;
import static dev.uchar.regelspraak.cases.TmsDocumenten.check;
import static dev.uchar.regelspraak.cases.TmsDocumenten.consistent;
import static dev.uchar.regelspraak.cases.TmsDocumenten.getal;
import static dev.uchar.regelspraak.cases.TmsDocumenten.instantie;
import static dev.uchar.regelspraak.cases.TmsDocumenten.kenmerkregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.lezing;
import static dev.uchar.regelspraak.cases.TmsDocumenten.rekenregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.samengesteld;
import static dev.uchar.regelspraak.cases.TmsDocumenten.standaard;
import static dev.uchar.regelspraak.cases.TmsDocumenten.waarde;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fall-through at an empty default where the higher default decides after all (the card's acceptance (b); the trial's
 * A10): two defaults on the {@code doel} of a Vondst. The higher one, justified where the Vondst is {@code c} and not
 * {@code afgewezen}, takes {@code bron1}, which a chain of two rules fills only where the Vondst is {@code b}; the lower
 * one, justified where it is {@code d}, takes {@code bron2}, which one rule fills. RUNTIME-SEMANTICS ch. 7.1: the
 * highest default that decides supplies the value, and a justified default whose consequence is leeg falls through.
 * Truth maintenance has to get there from either side: where the lower default decides before {@code bron1} arrives,
 * its write must go when the higher one decides (the takeover); where the higher one decides before {@code afgewezen}
 * is derived, its decision must go again and the lower one decide (the give-back). Every combination of the four input
 * characteristics is an object of one case, played in every order of the document's rules, and fired in a seeded
 * sample of orders of the rules the branch generates, which must take both transient paths somewhere.
 *
 * <p><b>Red before the lowering</b> (measured 2026-10-08, the branch refusing every defaults group): the replay and the
 * sample failed on the build, "elke_volgorde_van_de_regels_in_het_document:108 » Excluded tms-transpiler SKIP: defaults:
 * doel van een vondst — een PrioritizedDefaults: de truth-maintenance-tak draagt deze vorm nog niet"; the exec-model
 * branch passed the same case.
 *
 * <p><b>Red with a control fact or a write stated</b> (measured 2026-10-08 on two mutants of the simple form, not
 * committed; the document orders passed both, the sample did not). With {@code Beslist} inserted by {@code insert}
 * instead of {@code logicalInsert}, the give-back failed: "V1111 doel in volgorde [kenmerkstand Vondst.tussen: waar uit
 * een afleiding, …, defaults A:Vondst.doel: default 0, …, defaults A:Vondst.doel: default 1, tussenstap, afwijzing, …]
 * ==&gt; expected: &lt;200&gt; but was: &lt;null&gt;": the stated fact outlived the decision it recorded, and the lower
 * default stayed blocked after the higher one's justification went. With the {@code Waarde} of a decision stated, the
 * takeover failed: "IllegalState geen fixpunt: twee schrijvers van één doel geven verschillende waarden — de regel
 * "defaults A:Vondst.doel" schrijft 100 en de regel "defaults A:Vondst.doel" schrijft 200 in het attribuut doel van een
 * Vondst".
 */
class TmsDoorvalWitnessTest {

    private static final String MODEL = TmsDocumenten.model(List.of(TmsDocumenten.objecttype("Vondst",
            List.of(attribuut("bron1", GEHEEL), attribuut("bron2", GEHEEL), attribuut("doel", GEHEEL)),
            List.of("b", "c", "d", "f", "tussen", "afgewezen"))), List.of());

    private static final List<String> REGELS = List.of(
            kenmerkregel("tussenstap", "Vondst", "tussen", check("b", false)),
            rekenregel("eerste bron", "GELIJKSTELLING", "Vondst", "bron1", getal("100"), check("tussen", false)),
            rekenregel("tweede bron", "GELIJKSTELLING", "Vondst", "bron2", getal("200"), "null"),
            kenmerkregel("afwijzing", "Vondst", "afgewezen", check("f", false)));

    private static final String GROEP = attribuutgroep("doel van een vondst", "Vondst", "doel", List.of(
            standaard(samengesteld("all", 0, List.of(check("c", false), check("afgewezen", true))),
                    lezing("bron1", "Vondst")),
            standaard(check("d", false), lezing("bron2", "Vondst"))));

    /** The input characteristics of object {@code bits}: b, c, d and f by its four bits. */
    private static List<String> kenmerken(int bits) {
        List<String> k = new ArrayList<>();
        String[] namen = {"b", "c", "d", "f"};
        for (int i = 0; i < 4; i++) if ((bits & (1 << i)) != 0) k.add(namen[i]);
        return k;
    }

    /** The label of object {@code bits}, its four bits as b, c, d, f. */
    private static String label(int bits) {
        StringBuilder s = new StringBuilder("V");
        for (int i = 0; i < 4; i++) s.append((bits & (1 << i)) != 0 ? '1' : '0');
        return s.toString();
    }

    /** RUNTIME-SEMANTICS ch. 7.1 by hand: the higher default where it is justified and bron1 is filled, else the lower
     *  where it is justified (bron2 is always filled), else leeg. */
    private static String verwacht(int bits) {
        boolean b = (bits & 1) != 0, c = (bits & 2) != 0, d = (bits & 4) != 0, f = (bits & 8) != 0;
        return c && !f && b ? "100" : d ? "200" : null;
    }

    private static JsonNode zaak() throws Exception {
        List<String> instanties = new ArrayList<>(), verwachtingen = new ArrayList<>();
        for (int bits = 0; bits < 16; bits++) {
            instanties.add(instantie(label(bits), "Vondst", Map.of(), kenmerken(bits), Map.of()));
            verwachtingen.add(waarde(label(bits), "doel", verwacht(bits)));
        }
        verwachtingen.add(consistent());
        return TmsDocumenten.zaak(instanties, verwachtingen);
    }

    private static Path document(List<String> regels) throws Exception {
        return TmsDocumenten.document(MODEL, regels, List.of(), List.of(GROEP));
    }

    @Test void elke_volgorde_van_de_regels_in_het_document() throws Exception {
        int volgordes = 0;
        for (List<String> volgorde : TmsDocumenten.permutaties(REGELS)) {
            List<String> namen = TmsDocumenten.regelnamen(volgorde);
            assertNull(CaseRunner.speelCase(document(volgorde), zaak(), DroolsBackend.Branch.EXEC_MODEL),
                    () -> "volgorde " + namen);
            volgordes++;
        }
        assertEquals(24, volgordes);
    }

    /** The two defaults, the four rules and the standings of tussen, afgewezen, bron1 and bron2: too many for every
     *  order, so a seeded sample, which must take the takeover and the give-back each at least once. */
    @Test void steekproef_van_volgordes_van_de_gegenereerde_regels() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(document(REGELS), DroolsBackend.Branch.EXEC_MODEL);
        List<Rule> gegenereerd = b.execModelTranspiler().model().getRules();
        assertEquals(16, gegenereerd.size(),
                () -> "gegenereerde regels: " + gegenereerd.stream().map(Rule::getName).toList());
        int[] overname = {0}, teruggave = {0};
        for (List<Rule> volgorde : TmsDocumenten.steekproef(gegenereerd, 20261008L, 100)) {
            List<String> namen = volgorde.stream().map(Rule::getName).toList();
            List<Object> bonen = new ArrayList<>();
            for (int bits = 0; bits < 16; bits++)
                bonen.add(TmsDocumenten.boon(b.beanLoader(), "Vondst", Map.of(), kenmerken(bits)));
            Map<Object, List<String>> gevuurd = new IdentityHashMap<>();
            TmsDocumenten.vuur(b, volgorde, bonen, sessie -> sessie.addEventListener(new DefaultAgendaEventListener() {
                @Override public void afterMatchFired(AfterMatchFiredEvent e) {
                    String regel = e.getMatch().getRule().getName();
                    for (Object o : e.getMatch().getObjects())
                        if (bonen.stream().anyMatch(x -> x == o))
                            gevuurd.computeIfAbsent(o, x -> new ArrayList<>()).add(regel);
                }
            }), sessie -> {
                for (Object f : sessie.getObjects(o -> o instanceof Stuurfeit))
                    assertTrue(f instanceof Beslist, () -> "een eenvoudige groep zet alleen Beslist in: " + f);
            });
            for (int bits = 0; bits < 16; bits++) {
                Object boon = bonen.get(bits);
                String waar = label(bits) + " doel in volgorde " + namen;
                String v = verwacht(bits);
                assertEquals(v == null ? null : new BigDecimal(v), TmsDocumenten.lees(boon, "doel"), waar);
                List<String> regels = gevuurd.getOrDefault(boon, List.of());
                if ("100".equals(v) && regels.stream().anyMatch(r -> r.endsWith("default 1"))) overname[0]++;
                if ("200".equals(v) && regels.stream().anyMatch(r -> r.endsWith("default 0"))) teruggave[0]++;
            }
        }
        assertTrue(overname[0] > 0, "geen volgorde liet de lagere default eerst beslissen en de hogere overnemen");
        assertTrue(teruggave[0] > 0, "geen volgorde liet de hogere default beslissen en zijn besluit weer verliezen");
    }
}
