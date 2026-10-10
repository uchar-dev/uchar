package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import dev.uchar.regelspraak.transpiler.tms.Stuurfeit;
import dev.uchar.regelspraak.transpiler.tms.Uitkomst;
import org.drools.model.Rule;
import org.junit.jupiter.api.Test;
import org.kie.api.event.rule.ObjectDeletedEvent;
import org.kie.api.event.rule.ObjectInsertedEvent;
import org.kie.api.event.rule.ObjectUpdatedEvent;
import org.kie.api.event.rule.RuleRuntimeEventListener;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocumenten.GEHEEL;
import static dev.uchar.regelspraak.cases.TmsDocumenten.attribuut;
import static dev.uchar.regelspraak.cases.TmsDocumenten.attribuutgroep;
import static dev.uchar.regelspraak.cases.TmsDocumenten.check;
import static dev.uchar.regelspraak.cases.TmsDocumenten.getal;
import static dev.uchar.regelspraak.cases.TmsDocumenten.groepsdefault;
import static dev.uchar.regelspraak.cases.TmsDocumenten.inconsistent;
import static dev.uchar.regelspraak.cases.TmsDocumenten.instantie;
import static dev.uchar.regelspraak.cases.TmsDocumenten.kenmerkregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.metUitzonderingen;
import static dev.uchar.regelspraak.cases.TmsDocumenten.standaard;
import static dev.uchar.regelspraak.cases.TmsDocumenten.waarde;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Nested exception groups (RUNTIME-SEMANTICS ch. 7.9) through the branch's control facts, in the shapes of the anchors
 * {@code jutterswet-nestuitzondering} and {@code jutterswet-nestdiepte} on one target: default 0 decides 40 where the
 * Jutter is {@code grondX}, with three unordered exception groups, 10 on {@code grondY}, 10 on {@code grondW}, and 30 on
 * {@code grondP}, which itself carries the unordered groups 5 on {@code grondQ} and 25 on {@code grondR}; default 1 is a
 * group-default over 15 on {@code grondB1} and 25 on {@code grondB2}. {@code grondY} and {@code grondQ} are derived from
 * inputs by a rule each, so a reconciliation can see a member decide before another one does.
 *
 * <p>What the objects pin, each against the conflict facts exactly, not only the ones expected: agreeing members (10
 * and 10) are one decision; a deeper conflict ({@code Jutter.heffing/0/0}) passes up unchanged, also over a member that
 * decides; differing members conflict at their own path ({@code Jutter.heffing/0}); a form-a default falls back to its
 * own value where no member settles; and a higher default that settles displaces the exception groups of a lower one,
 * conflict included (no {@code Jutter.heffing/1} fact beside 40). Played in every order of the document's rules and
 * fired in a seeded sample of orders of the generated rules, where truth maintenance must withdraw a control fact
 * somewhere on the way.
 *
 * <p><b>Red before the lowering</b> (measured 2026-10-08, the branch refusing every defaults group): the replay and the
 * sample failed on the build, "elke_volgorde_van_de_regels_in_het_document:113 » Excluded tms-transpiler SKIP: defaults:
 * heffing van een jutter — een PrioritizedDefaults: de truth-maintenance-tak draagt deze vorm nog niet"; the exec-model
 * branch passed the same case.
 */
class TmsUitzonderingWitnessTest {

    private static final String MODEL = TmsDocumenten.model(List.of(TmsDocumenten.objecttype("Jutter",
            List.of(attribuut("heffing", GEHEEL)),
            List.of("grondX", "grondY", "grondW", "grondP", "grondQ", "grondR", "grondB1", "grondB2", "y0", "q0"))),
            List.of());

    private static final List<String> REGELS = List.of(
            kenmerkregel("grond y", "Jutter", "grondY", check("y0", false)),
            kenmerkregel("grond q", "Jutter", "grondQ", check("q0", false)));

    private static final String GROEP = attribuutgroep("heffing van een jutter", "Jutter", "heffing", List.of(
            metUitzonderingen(check("grondX", false), getal("40"), List.of(
                    List.of(standaard(check("grondY", false), getal("10"))),
                    List.of(standaard(check("grondW", false), getal("10"))),
                    List.of(metUitzonderingen(check("grondP", false), getal("30"), List.of(
                            List.of(standaard(check("grondQ", false), getal("5"))),
                            List.of(standaard(check("grondR", false), getal("25")))))))),
            groepsdefault(List.of(
                    List.of(standaard(check("grondB1", false), getal("15"))),
                    List.of(standaard(check("grondB2", false), getal("25")))))));

    /** Per object its input characteristics, its heffing (null for leeg) and its conflict key (null for none). */
    private record Geval(List<String> kenmerken, String heffing, String conflict) {}

    private static final Map<String, Geval> GEVALLEN = new LinkedHashMap<>();
    static {
        GEVALLEN.put("J1", new Geval(List.of("grondX", "y0"), "10", null));
        GEVALLEN.put("J2", new Geval(List.of("grondX", "y0", "grondW"), "10", null));
        GEVALLEN.put("J3", new Geval(List.of("grondX", "y0", "grondP"), null, "Jutter.heffing/0"));
        GEVALLEN.put("J4", new Geval(List.of("grondX", "grondP", "q0"), "5", null));
        GEVALLEN.put("J5", new Geval(List.of("grondX", "grondP", "q0", "grondR"), null, "Jutter.heffing/0/0"));
        GEVALLEN.put("J6", new Geval(List.of("grondX"), "40", null));
        GEVALLEN.put("J7", new Geval(List.of("grondX", "grondB1", "grondB2"), "40", null));
        GEVALLEN.put("J8", new Geval(List.of("grondB1", "grondB2"), null, "Jutter.heffing/1"));
        GEVALLEN.put("J9", new Geval(List.of("grondB1"), "15", null));
        GEVALLEN.put("J10", new Geval(List.of(), null, null));
        GEVALLEN.put("J11", new Geval(List.of("grondX", "y0", "grondP", "q0"), null, "Jutter.heffing/0"));
        GEVALLEN.put("J12", new Geval(List.of("grondX", "y0", "grondP", "q0", "grondR"), null, "Jutter.heffing/0/0"));
    }

    private static JsonNode zaak() throws Exception {
        List<String> instanties = new ArrayList<>(), verwachtingen = new ArrayList<>();
        GEVALLEN.forEach((label, g) -> {
            instanties.add(instantie(label, "Jutter", Map.of(), g.kenmerken(), Map.of()));
            verwachtingen.add(waarde(label, "heffing", g.heffing()));
            if (g.conflict() != null) verwachtingen.add(inconsistent(g.conflict(), label));
        });
        return TmsDocumenten.zaak(instanties, verwachtingen);
    }

    private static Path document(List<String> regels) throws Exception {
        return TmsDocumenten.document(MODEL, regels, List.of(), List.of(GROEP));
    }

    @Test void elke_volgorde_van_de_regels_in_het_document() throws Exception {
        for (List<String> volgorde : TmsDocumenten.permutaties(REGELS)) {
            List<String> namen = TmsDocumenten.regelnamen(volgorde);
            assertNull(CaseRunner.speelCase(document(volgorde), zaak(), DroolsBackend.Branch.EXEC_MODEL),
                    () -> "volgorde " + namen);
        }
    }

    @Test void de_exec_modeltak_geeft_dezelfde_eindtoestand() throws Exception {
        assertNull(CaseRunner.speelCase(document(REGELS), zaak(), DroolsBackend.Branch.EXEC_MODEL));
    }

    @Test void steekproef_van_volgordes_van_de_gegenereerde_regels() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(document(REGELS), DroolsBackend.Branch.EXEC_MODEL);
        List<Rule> gegenereerd = b.execModelTranspiler().model().getRules();
        int[] ingetrokken = {0};
        for (List<Rule> volgorde : TmsDocumenten.steekproef(gegenereerd, 20261008L, 100)) {
            List<String> namen = volgorde.stream().map(Rule::getName).toList();
            Map<Object, String> labels = new IdentityHashMap<>();
            List<Object> bonen = new ArrayList<>();
            for (Map.Entry<String, Geval> e : GEVALLEN.entrySet()) {
                Object boon = TmsDocumenten.boon(b.beanLoader(), "Jutter", Map.of(), e.getValue().kenmerken());
                labels.put(boon, e.getKey());
                bonen.add(boon);
            }
            List<String> conflicten = new ArrayList<>();
            TmsDocumenten.vuur(b, volgorde, bonen, sessie -> sessie.addEventListener(new RuleRuntimeEventListener() {
                @Override public void objectInserted(ObjectInsertedEvent e) { }
                @Override public void objectUpdated(ObjectUpdatedEvent e) { }
                @Override public void objectDeleted(ObjectDeletedEvent e) {
                    if (e.getOldObject() instanceof Uitkomst) ingetrokken[0]++;
                }
            }), sessie -> {
                conflicten.addAll(TmsDocumenten.inconsistenties(sessie, labels));
                for (Object f : sessie.getObjects(o -> o instanceof Stuurfeit))
                    assertTrue(f instanceof Uitkomst, () -> "een algemene groep zet alleen Uitkomst in: " + f);
            });
            List<String> verwacht = new ArrayList<>();
            GEVALLEN.forEach((label, g) -> { if (g.conflict() != null) verwacht.add(g.conflict() + " @ " + label); });
            verwacht.sort(null);
            assertEquals(verwacht, conflicten, () -> "conflicten in volgorde " + namen);
            for (Object boon : bonen) {
                Geval g = GEVALLEN.get(labels.get(boon));
                assertEquals(g.heffing() == null ? null : new BigDecimal(g.heffing()), TmsDocumenten.lees(boon, "heffing"),
                        () -> labels.get(boon) + " heffing in volgorde " + namen);
            }
        }
        assertTrue(ingetrokken[0] > 0, "geen volgorde trok een stuurfeit in: de steekproef liep geen tussenstand langs");
    }
}
