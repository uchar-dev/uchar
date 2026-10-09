package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import dev.uchar.regelspraak.transpiler.kern.Namen;
import dev.uchar.regelspraak.transpiler.tms.Stand;
import dev.uchar.regelspraak.transpiler.tms.Waarde;

import org.drools.model.Rule;
import org.junit.jupiter.api.Test;
import org.kie.api.event.rule.DefaultRuleRuntimeEventListener;
import org.kie.api.event.rule.ObjectInsertedEvent;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static dev.uchar.regelspraak.cases.TmsDocumenten.aggregatie;
import static dev.uchar.regelspraak.cases.TmsDocumenten.bronrol;
import static dev.uchar.regelspraak.cases.TmsDocumenten.check;
import static dev.uchar.regelspraak.cases.TmsDocumenten.consistent;
import static dev.uchar.regelspraak.cases.TmsDocumenten.feittypeEenNaarVeel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.lezing;
import static dev.uchar.regelspraak.cases.TmsDocumenten.rekenregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.waarde;
import static dev.uchar.regelspraak.cases.TmsTijdlijnDocumenten.GEHEEL;
import static dev.uchar.regelspraak.cases.TmsTijdlijnDocumenten.instantie;
import static dev.uchar.regelspraak.cases.TmsTijdlijnDocumenten.maanden;
import static dev.uchar.regelspraak.cases.TmsTijdlijnDocumenten.objecttype;
import static dev.uchar.regelspraak.cases.TmsTijdlijnDocumenten.periode;
import static dev.uchar.regelspraak.cases.TmsTijdlijnDocumenten.segment;
import static dev.uchar.regelspraak.cases.TmsTijdlijnDocumenten.tijdattribuut;
import static dev.uchar.regelspraak.cases.TmsTijdlijnDocumenten.tijdkenmerk;
import static dev.uchar.regelspraak.cases.TmsTijdlijnDocumenten.tijdlijn;
import static dev.uchar.regelspraak.cases.TmsTijdlijnDocumenten.tijdsevenredig;
import static dev.uchar.regelspraak.cases.TmsTijdlijnDocumenten.verwachteTijdlijn;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A total and a sum per member over a timeline on the truth-maintenance branch (RUNTIME-SEMANTICS ch. 3.6, the
 * per-element fold): each member's timeline collapses over time first, at its own granularity, in the member's
 * contribution rule, and the scalars fold over the set as any fold does; a member without a timeline contributes
 * nothing, so over no contribution the total is 0 and the sum leeg.
 *
 * <p>The model: a Jutter keeps his Vondsten (bewaring: jutter, one Jutter; vondst, many Vondsten). "Het tarief van een
 * Vondst moet berekend worden als het tijdsevenredig deel per maand van zijn jaartarief gedurende de tijd dat hij
 * bewaard is", a target on a month grid that only that rule writes, so it stands where the pro-rata part is empty
 * (ch. 7.8); "De last van een Jutter moet berekend worden als het totaal van de tarieven van zijn vondsten" and "De
 * som van een Jutter moet berekend worden als de som van de tarieven van zijn vondsten". So the timeline the two folds
 * collapse is derived by another rule. By hand: V1 (jaartarief 12 in 2024, bewaard on [2024-01-01, 2024-04-01)) gets
 * the tarief 12 × 31/31, 12 × 29/29 and 12 × 31/31 in January, February and March 2024, three segments, which total
 * 36; V2 (tarief 5 on [2024-01-01, 2024-03-01) as input, no jaartarief, no true period) keeps its input, whose total
 * is 2 × 5 = 10; V3 (jaartarief 12 in 2024, no true period) and V4 (bewaard in May 2024, no jaartarief) get no
 * segment and hold no timeline, so they contribute nothing. J1 keeps V1, V2 and V3: last 36 + 10 = 46, som 46. J2
 * keeps none: last 0, som leeg. J3 keeps V4 alone: last 0, som leeg, where a member that contributed 0 would make the
 * sum 0. Every order of the three rules; beside them the generated rules in a hundred sampled orders, where at the
 * fixpoint every object holds at most one {@link Waarde} per target, and in some order the last of J1 was 10 on the
 * way, before the tarief of V1 was derived.
 *
 * <p><b>Measured</b> (2026-10-09, every document of {@link #main}, all six orders): the verifier passes all six, and the
 * reference interpreter's runner passes every one, "oracle: PASS (11 expectations)". The ASP runner refuses all six,
 * "TRANSLATION FAULT: the per-element temporal fold of "tarief" over a DERIVED member timeline is not translated: a
 * rule writes "tarief" on "Vondst", so its segment bounds are not case data and the whole-unit facts this fold
 * integrates against cannot be computed (no corpus case)"; with the rule of the tarief taken out and V1's three
 * segments of 12 given as input, it passes the case with the same totals and sums, "clingo: PASS (7 expectations)",
 * and so does the reference interpreter.
 *
 * <p><b>Red</b> on b9f77aa7, the code before the change: in every order the first rule of the three is refused, "but
 * was: &lt;[[tarief, last, som] Excluded: tms-transpiler SKIP: regel: tarief — het tijdsafhankelijke attribuut 'tarief'
 * van Vondst: tijdlijnen zijn buiten de proef van de truth-maintenance-tak, …, [last, tarief, som] Excluded: … regel:
 * last — een Aggregation TOTAAL over het tijdsafhankelijke attribuut 'tarief' in een waarde ('het totaal van de
 * tarieven van zijn vondsten'): tijdlijnen zijn buiten de proef van de truth-maintenance-tak, …, [som, last, tarief]
 * Excluded: … regel: som — een Aggregation SOM over het tijdsafhankelijke attribuut 'tarief' …&gt;", and the build of
 * the sampled orders on the refusal of the tarief. <b>Red by mutation</b> (not committed): with a member without a
 * timeline contributing 0, every order fails on J3, "case: verwacht leeg, werkelijk 0".
 */
class TmsTijdlijnVouwWitnessTest {

    static final String MODEL = TmsDocumenten.model(List.of(
            objecttype("Jutter", List.of(TmsDocumenten.attribuut("last", GEHEEL), TmsDocumenten.attribuut("som", GEHEEL)),
                    List.of()),
            objecttype("Vondst", List.of(tijdattribuut("jaartarief", GEHEEL, "year"),
                    tijdattribuut("tarief", GEHEEL, "month")), List.of(tijdkenmerk("bewaard", "month")))),
            List.of(feittypeEenNaarVeel("bewaring", "jutter", "Jutter", "vondst", "Vondst", "vondsten")));

    static final String TARIEF = rekenregel("tarief", "BEREKENING", "Vondst", "tarief",
            tijdsevenredig("jaartarief", "Vondst", check("bewaard", false), "month"), "null");
    static final String LAST = rekenregel("last", "BEREKENING", "Jutter", "last", aggregatie("total",
            lezing("tarief", "Vondst"), "Vondst", bronrol("bewaring", "vondst"), "null", "null", GEHEEL,
            "het totaal van de tarieven van zijn vondsten"), "null");
    static final String SOM = rekenregel("som", "BEREKENING", "Jutter", "som", aggregatie("sum",
            lezing("tarief", "Vondst"), "Vondst", bronrol("bewaring", "vondst"), "null", "null", GEHEEL,
            "de som van de tarieven van zijn vondsten"), "null");

    static final List<String> REGELS = List.of(TARIEF, LAST, SOM);

    static final String JAARTARIEF = tijdlijn(List.of(segment("2024-01-01", "2025-01-01", "12")));

    static JsonNode zaak() throws Exception {
        return TmsTijdlijnDocumenten.zaak(null, List.of(
                        instantie("J1", "Jutter", Map.of(), List.of(), Map.of(),
                                Map.of("vondsten", List.of("V1", "V2", "V3"))),
                        instantie("J2", "Jutter", Map.of(), List.of(), Map.of(), Map.of()),
                        instantie("J3", "Jutter", Map.of(), List.of(), Map.of(), Map.of("vondsten", List.of("V4"))),
                        instantie("V1", "Vondst", Map.of("jaartarief", JAARTARIEF), List.of(),
                                Map.of("bewaard", List.of(periode("2024-01-01", "2024-04-01"))), Map.of()),
                        instantie("V2", "Vondst", Map.of("tarief", tijdlijn(List.of(
                                segment("2024-01-01", "2024-03-01", "5")))), List.of(), Map.of(), Map.of()),
                        instantie("V3", "Vondst", Map.of("jaartarief", JAARTARIEF), List.of(), Map.of(), Map.of()),
                        instantie("V4", "Vondst", Map.of(), List.of(),
                                Map.of("bewaard", List.of(periode("2024-05-01", "2024-06-01"))), Map.of())),
                List.of(verwachteTijdlijn("V1", "tarief", maanden(2024, 1, 3, "12")),
                        verwachteTijdlijn("V2", "tarief", List.of(segment("2024-01-01", "2024-03-01", "5"))),
                        verwachteTijdlijn("V3", "tarief", List.of()), verwachteTijdlijn("V4", "tarief", List.of()),
                        waarde("J1", "last", "46"), waarde("J1", "som", "46"),
                        waarde("J2", "last", "0"), waarde("J2", "som", null),
                        waarde("J3", "last", "0"), waarde("J3", "som", null), consistent()));
    }

    @Test void een_totaal_en_een_som_per_lid_over_een_afgeleide_tijdlijn_in_elke_volgorde_van_de_regels()
            throws Exception {
        JsonNode zaak = zaak();
        List<String> uitkomsten = new ArrayList<>(), verwacht = new ArrayList<>();
        for (List<String> volgorde : TmsDocumenten.permutaties(REGELS)) {
            List<String> namen = TmsDocumenten.regelnamen(volgorde);
            Path ir = TmsTijdlijnDocumenten.document(MODEL, volgorde, List.of());
            uitkomsten.add(namen + " " + TmsFoutWitnessTest.speel(ir, zaak, DroolsBackend.Branch.TRUTH_MAINTENANCE));
            verwacht.add(namen + " gespeeld");
        }
        assertEquals(verwacht, uitkomsten);
    }

    @Test void aan_het_fixpunt_hoogstens_een_waarde_per_doel_en_de_last_onderweg_zonder_het_afgeleide_tarief()
            throws Exception {
        Path ir = TmsTijdlijnDocumenten.document(MODEL, REGELS, List.of());
        DroolsBackend.Build b = DroolsBackend.build(ir, DroolsBackend.Branch.TRUTH_MAINTENANCE);
        int tussenstand = 0;
        for (List<Rule> volgorde : TmsDocumenten.steekproef(b.tmsTranspiler().model().getRules(), 20261009L, 100)) {
            List<String> namen = volgorde.stream().map(Rule::getName).toList();
            ClassLoader l = b.beanLoader();
            Object j1 = TmsDocumenten.boon(l, "Jutter", Map.of(), List.of());
            Object j2 = TmsDocumenten.boon(l, "Jutter", Map.of(), List.of());
            Object v1 = TmsDocumenten.boon(l, "Vondst", Map.of(), List.of());
            Object v2 = TmsDocumenten.boon(l, "Vondst", Map.of(), List.of());
            Object v3 = TmsDocumenten.boon(l, "Vondst", Map.of(), List.of());
            TmsTijdlijnDocumenten.zet(v1, "jaartarief", List.of(
                    TmsTijdlijnDocumenten.boonsegment(l, "2024-01-01", "2025-01-01", "12")));
            TmsTijdlijnDocumenten.zet(v1, "bewaard", List.of(
                    TmsTijdlijnDocumenten.boonsegment(l, "2024-01-01", "2024-04-01", null)));
            TmsTijdlijnDocumenten.zet(v2, "tarief", List.of(
                    TmsTijdlijnDocumenten.boonsegment(l, "2024-01-01", "2024-03-01", "5")));
            TmsTijdlijnDocumenten.zet(v3, "jaartarief", List.of(
                    TmsTijdlijnDocumenten.boonsegment(l, "2024-01-01", "2025-01-01", "12")));
            for (Object v : List.of(v1, v2, v3)) bewaar(v, j1);
            Map<Object, String> labels = new IdentityHashMap<>(Map.of(j1, "J1", j2, "J2", v1, "V1", v2, "V2", v3, "V3"));
            int[] onderweg = {0};
            Map<String, Integer> waarden = new TreeMap<>();
            List<String> standen = new ArrayList<>();
            TmsDocumenten.vuur(b, volgorde, List.of(j1, j2, v1, v2, v3), sessie -> sessie.addEventListener(
                    new DefaultRuleRuntimeEventListener() {
                        @Override public void objectInserted(ObjectInsertedEvent e) {
                            if (e.getObject() instanceof Waarde w && w.onderwerp() == j1
                                    && w.attribuut().naam().equals("last")
                                    && ((BigDecimal) w.waarde()).compareTo(new BigDecimal("10")) == 0) onderweg[0]++;
                        }
                    }), sessie -> {
                for (Object f : sessie.getObjects(o -> o instanceof Waarde))
                    waarden.merge(labels.get(((Waarde) f).onderwerp()) + "." + ((Waarde) f).attribuut().naam(), 1,
                            Integer::sum);
                for (Object f : sessie.getObjects(o -> o instanceof Stand))
                    standen.add(labels.get(((Stand) f).onderwerp()) + "." + ((Stand) f).attribuut().naam());
            });
            assertEquals(Map.of("J1.last", 1, "J1.som", 1, "J2.last", 1, "V1.tarief", 1), waarden,
                    () -> "waarden in volgorde " + namen);
            standen.sort(null);
            // the jaartarief no form writes stands as well: every attribute on a timeline a rule reads has a standing,
            // which carries its input in the convention the case gives it (RUNTIME-SEMANTICS ch. 8.4)
            assertEquals(List.of("V1.jaartarief", "V1.tarief", "V2.jaartarief", "V2.tarief", "V3.jaartarief",
                    "V3.tarief"), standen, () -> "standen in volgorde " + namen);
            assertEquals(0, new BigDecimal("46").compareTo((BigDecimal) TmsDocumenten.lees(j1, "last")),
                    () -> "last van J1 in volgorde " + namen);
            assertEquals(0, new BigDecimal("46").compareTo((BigDecimal) TmsDocumenten.lees(j1, "som")),
                    () -> "som van J1 in volgorde " + namen);
            assertEquals(0, BigDecimal.ZERO.compareTo((BigDecimal) TmsDocumenten.lees(j2, "last")),
                    () -> "last van J2 in volgorde " + namen);
            assertNull(TmsDocumenten.lees(j2, "som"), () -> "som van J2 in volgorde " + namen);
            assertEquals(List.of("[2024-01-01, 2024-02-01)=12", "[2024-02-01, 2024-03-01)=12",
                    "[2024-03-01, 2024-04-01)=12"), TmsTijdlijnWitnessTest.tijdlijnVan(v1, "tarief"),
                    () -> "tarief van V1 in volgorde " + namen);
            assertEquals(List.of("[2024-01-01, 2024-03-01)=5"), TmsTijdlijnWitnessTest.tijdlijnVan(v2, "tarief"),
                    () -> "tarief van V2 in volgorde " + namen);
            assertNull(TmsDocumenten.lees(v3, "tarief"), () -> "tarief van V3 in volgorde " + namen);
            if (onderweg[0] > 0) tussenstand++;
        }
        assertTrue(tussenstand > 0, "geen volgorde gaf J1 onderweg de last 10, zonder het tarief van V1");
    }

    /** Links the Vondst bean {@code vondst} to the Jutter bean {@code jutter}, as the case runner writes the role
     *  "vondsten" of a Jutter: on the to-one field of the Vondst. */
    private static void bewaar(Object vondst, Object jutter) throws Exception {
        for (java.lang.reflect.Method m : vondst.getClass().getMethods())
            if (m.getName().equals(Namen.setter("jutter")) && m.getParameterCount() == 1) {
                m.invoke(vondst, jutter);
                return;
            }
        throw new AssertionError("geen setter voor jutter op " + vondst.getClass());
    }

    /** Writes the document in every order of its rules, with its case, under {@code args[0]}: a map per order holding
     *  {@code ir.json} and {@code case.json}, for the verifier, ref and clingo. */
    public static void main(String[] args) throws Exception {
        Path uit = Path.of(args[0]);
        Map<String, Object[]> delen = new LinkedHashMap<>();
        delen.put("vouw-per-lid", new Object[]{MODEL, REGELS, zaak()});
        for (Map.Entry<String, Object[]> d : delen.entrySet()) {
            @SuppressWarnings("unchecked") List<String> regels = (List<String>) d.getValue()[1];
            int n = 0;
            for (List<String> volgorde : TmsDocumenten.permutaties(regels))
                TmsTijdlijnWitnessTest.schrijf(uit.resolve(d.getKey()).resolve(String.format("v%03d", n++)),
                        TmsTijdlijnDocumenten.ir((String) d.getValue()[0], volgorde, List.of()),
                        (JsonNode) d.getValue()[2]);
        }
    }
}
