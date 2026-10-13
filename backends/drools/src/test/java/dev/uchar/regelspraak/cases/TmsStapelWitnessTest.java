package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import dev.uchar.regelspraak.transpiler.tms.Tekstvolgorde;
import org.drools.model.Rule;
import org.kie.api.KieBase;
import org.junit.jupiter.api.Test;
import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.DefaultAgendaEventListener;

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
import static dev.uchar.regelspraak.cases.TmsDocumenten.binair;
import static dev.uchar.regelspraak.cases.TmsDocumenten.consistent;
import static dev.uchar.regelspraak.cases.TmsDocumenten.getal;
import static dev.uchar.regelspraak.cases.TmsDocumenten.instantie;
import static dev.uchar.regelspraak.cases.TmsDocumenten.lezing;
import static dev.uchar.regelspraak.cases.TmsDocumenten.rekenregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.samengesteld;
import static dev.uchar.regelspraak.cases.TmsDocumenten.standaard;
import static dev.uchar.regelspraak.cases.TmsDocumenten.vergelijking;
import static dev.uchar.regelspraak.cases.TmsDocumenten.waarde;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The effective stack of RUNTIME-SEMANTICS ch. 7.10 in the shape of {@code jutterswet-wrakinitialisatie}: an
 * initialisation "het bergloon van een Vondst = 2 maal zijn gewicht indien zijn gewicht groter is dan 10" on Vondst,
 * and on {@code Wrakvondst is een Vondst} a defaults group, 9 where the gewicht is above 50 and above 100, 5 where it is
 * above 50. On a Wrakvondst the initialisation is folded into the stack below the group, with its condition, and is
 * read on Vondst, the level that declared it; there it must not fire as a rule as well, while on a Vondst it fires as
 * the ordinary initialisation it is. Played through the replay, and fired in a seeded sample of orders of the
 * generated rules, in which the rule never fires on a Wrakvondst and the stack's initialisation default does fire on
 * the Wrakvondst it fills.
 *
 * <p><b>Red before the lowering</b> (measured 2026-10-08, the branch refusing a rule on a target a defaults group
 * writes): every test on the branch failed on the build, "de_gevouwen_initialisatie_vult_onder_haar_voorwaarde:103 »
 * Excluded tms-transpiler SKIP: regel: standaardbergloon — een doel dat ook een defaultsgroep schrijft ('bergloon' van
 * Vondst): de truth-maintenance-tak draagt deze vorm nog niet"; the exec-model branch passed the same case.
 *
 * <p><b>Red with the rule left on the subtype</b> (measured 2026-10-08 on a mutant that dropped the exclusion of the
 * stack's type from the rule's pattern, not committed): the values held, since the initialisation's guard yields to
 * the stack's write, but the sample failed on "de initialisatie vuurde als regel op [W4, W3, W2] in volgorde
 * [standaardbergloon, defaults A:Wrakvondst.bergloon: default 0, defaults A:Wrakvondst.bergloon: default 1, defaults
 * A:Wrakvondst.bergloon: default 2] ==&gt; expected: &lt;true&gt; but was: &lt;false&gt;".
 *
 * <p><b>Both branches of the analysis</b> (card drl-teksttak-op-truth-maintenance, step 4): the replay runs on the
 * exec-model branch and on {@code DRL_TMS}, its rendering as DRL text, and the sample of orders on both: on
 * {@code DRL_TMS} the rules of the plan rendered as text in each order ({@code Tekstvolgorde}), since the order of the
 * rules in the text is the order in which Drools loads them. <b>Red</b> on {@code e2bd685e}, before the step: both rows
 * failed on {@code DRL_TMS} with "drl-tms: de DRL-weergave draagt Afloop nog niet", the replay through the case and the
 * sample where it builds the branch.
 */
class TmsStapelWitnessTest {

    private static final String MODEL = TmsDocumenten.model(List.of(
            TmsDocumenten.objecttype("Vondst", List.of(attribuut("gewicht", GEHEEL), attribuut("bergloon", GEHEEL)),
                    List.of()),
            TmsDocumenten.subtype("Wrakvondst", "Vondst", List.of(), List.of())), List.of());

    private static final String INITIALISATIE = rekenregel("standaardbergloon", "INITIALISATIE", "Vondst", "bergloon",
            binair("times", getal("2"), lezing("gewicht", "Vondst")),
            vergelijking("greater", lezing("gewicht", "Vondst"), getal("10")));

    private static final String GROEP = attribuutgroep("wrakbergloon", "Wrakvondst", "bergloon", List.of(
            standaard(samengesteld("all", 0, List.of(
                            vergelijking("greater", lezing("gewicht", "Wrakvondst"), getal("50")),
                            vergelijking("greater", lezing("gewicht", "Wrakvondst"), getal("100")))),
                    getal("9")),
            standaard(vergelijking("greater", lezing("gewicht", "Wrakvondst"), getal("50")), getal("5"))));

    /** Per object its type, gewicht, bergloon in the input (null for none), and bergloon at the end (null for leeg). */
    private record Geval(String type, String gewicht, String invoer, String bergloon) {}

    private static final Map<String, Geval> GEVALLEN = new LinkedHashMap<>();
    static {
        GEVALLEN.put("W1", new Geval("Wrakvondst", "8", null, null));
        GEVALLEN.put("W2", new Geval("Wrakvondst", "12", null, "24"));
        GEVALLEN.put("W3", new Geval("Wrakvondst", "60", null, "5"));
        GEVALLEN.put("W4", new Geval("Wrakvondst", "120", null, "9"));
        GEVALLEN.put("W5", new Geval("Wrakvondst", "12", "40", "40"));
        GEVALLEN.put("V1", new Geval("Vondst", "12", null, "24"));
        GEVALLEN.put("V2", new Geval("Vondst", "8", null, null));
        GEVALLEN.put("V3", new Geval("Vondst", "60", null, "120"));
        GEVALLEN.put("V4", new Geval("Vondst", "12", "40", "40"));
    }

    private static Map<String, String> getallen(Geval g) {
        Map<String, String> uit = new LinkedHashMap<>();
        uit.put("gewicht", g.gewicht());
        if (g.invoer() != null) uit.put("bergloon", g.invoer());
        return uit;
    }

    private static JsonNode zaak() throws Exception {
        List<String> instanties = new ArrayList<>(), verwachtingen = new ArrayList<>();
        GEVALLEN.forEach((label, g) -> {
            instanties.add(instantie(label, g.type(), getallen(g), List.of(), Map.of()));
            verwachtingen.add(waarde(label, "bergloon", g.bergloon()));
        });
        verwachtingen.add(consistent());
        return TmsDocumenten.zaak(instanties, verwachtingen);
    }

    private static Path document() throws Exception {
        return TmsDocumenten.document(MODEL, List.of(INITIALISATIE), List.of(), List.of(GROEP));
    }

    @Test void de_gevouwen_initialisatie_vult_onder_haar_voorwaarde() throws Exception {
        for (DroolsBackend.Branch tak : List.of(DroolsBackend.Branch.EXEC_MODEL, DroolsBackend.Branch.DRL_TMS))
            assertNull(CaseRunner.speelCase(document(), zaak(), tak), tak.toString());
    }

    @Test void steekproef_van_volgordes_van_de_gegenereerde_regels() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(document(), DroolsBackend.Branch.EXEC_MODEL);
        List<Rule> gegenereerd = b.execModelTranspiler().model().getRules();
        int[] gevouwen = {0};
        for (List<Rule> volgorde : TmsDocumenten.steekproef(gegenereerd, 20261008L, 100))
            vuurSteekproef(b, TmsDocumenten.kieBase(b, volgorde), volgorde.stream().map(Rule::getName).toList(), gevouwen);
        assertTrue(gevouwen[0] > 0, "de initialisatiedefault van de stapel vuurde nooit op W2");
        DroolsBackend.Build t = DroolsBackend.build(document(), DroolsBackend.Branch.DRL_TMS);
        List<String> regels = Tekstvolgorde.regels(t);
        assertEquals(gegenereerd.stream().map(Rule::getName).toList(), regels, "de regels van de tekst");
        int[] inDeTekst = {0};
        for (List<String> volgorde : TmsDocumenten.steekproef(regels, 20261008L, 100))
            vuurSteekproef(t, Tekstvolgorde.kieBase(t, volgorde), volgorde, inDeTekst);
        assertTrue(inDeTekst[0] > 0, "de initialisatiedefault van de stapel vuurde in de tekst nooit op W2");
    }

    /** Fires the objects of {@link #GEVALLEN} through {@code kieBase}, the rules of {@code b} in the order {@code namen}:
     *  the rule never fires on a Wrakvondst, and every bergloon ends as it should; counts in {@code gevouwen} the
     *  firings of the stack's initialisation default on W2. */
    private static void vuurSteekproef(DroolsBackend.Build b, KieBase kieBase, List<String> namen, int[] gevouwen)
            throws Exception {
        Class<?> wrakvondst = Class.forName("dev.uchar.regelspraak.facts.Wrakvondst", true, b.beanLoader());
        {
            Map<Object, String> labels = new IdentityHashMap<>();
            List<Object> bonen = new ArrayList<>();
            for (Map.Entry<String, Geval> e : GEVALLEN.entrySet()) {
                Object boon = TmsDocumenten.boon(b.beanLoader(), e.getValue().type(), getallen(e.getValue()), List.of());
                labels.put(boon, e.getKey());
                bonen.add(boon);
            }
            List<String> opHetSubtype = new ArrayList<>();
            TmsDocumenten.vuur(b, kieBase, namen, bonen, sessie -> sessie.addEventListener(new DefaultAgendaEventListener() {
                @Override public void afterMatchFired(AfterMatchFiredEvent e) {
                    String regel = e.getMatch().getRule().getName();
                    for (Object o : e.getMatch().getObjects()) {
                        if (!wrakvondst.isInstance(o)) continue;
                        if (regel.equals("standaardbergloon")) opHetSubtype.add(labels.get(o));
                        if (regel.endsWith("default 2") && "W2".equals(labels.get(o))) gevouwen[0]++;
                    }
                }
            }), sessie -> { });
            assertTrue(opHetSubtype.isEmpty(), () -> "de initialisatie vuurde als regel op " + opHetSubtype + " op "
                    + b.branch() + " in volgorde " + namen);
            for (Object boon : bonen) {
                Geval g = GEVALLEN.get(labels.get(boon));
                assertEquals(g.bergloon() == null ? null : new BigDecimal(g.bergloon()),
                        TmsDocumenten.lees(boon, "bergloon"), () -> labels.get(boon) + " bergloon op " + b.branch()
                                + " in volgorde " + namen);
            }
        }
    }

    /** The fold is per concrete type: the Wrakvondst carries one stack, and the Vondst none. */
    @Test void een_stapel_per_concreet_type() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(document(), DroolsBackend.Branch.EXEC_MODEL);
        List<String> namen = b.execModelTranspiler().defaultsgroepen().stream().map(g -> g.naam()).toList();
        assertEquals(List.of("defaults A:Wrakvondst.bergloon"), namen);
        assertFalse(b.execModelTranspiler().defaultsgroepen().get(0).algemeen());
    }
}
