package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import org.drools.model.Rule;
import org.junit.jupiter.api.Test;
import org.kie.api.event.rule.DefaultRuleRuntimeEventListener;
import org.kie.api.event.rule.ObjectInsertedEvent;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocumenten.check;
import static dev.uchar.regelspraak.cases.TmsDocumenten.getal;
import static dev.uchar.regelspraak.cases.TmsDocumenten.getalwaarde;
import static dev.uchar.regelspraak.cases.TmsDocumenten.inconsistent;
import static dev.uchar.regelspraak.cases.TmsDocumenten.kenmerkregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.lezing;
import static dev.uchar.regelspraak.cases.TmsDocumenten.rekenregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.vergelijking;
import static dev.uchar.regelspraak.cases.TmsTijdlijnDocumenten.GEHEEL;
import static dev.uchar.regelspraak.cases.TmsTijdlijnDocumenten.dagentelling;
import static dev.uchar.regelspraak.cases.TmsTijdlijnDocumenten.instantie;
import static dev.uchar.regelspraak.cases.TmsTijdlijnDocumenten.kenmerk;
import static dev.uchar.regelspraak.cases.TmsTijdlijnDocumenten.objecttype;
import static dev.uchar.regelspraak.cases.TmsTijdlijnDocumenten.periode;
import static dev.uchar.regelspraak.cases.TmsTijdlijnDocumenten.segment;
import static dev.uchar.regelspraak.cases.TmsTijdlijnDocumenten.tijdattribuut;
import static dev.uchar.regelspraak.cases.TmsTijdlijnDocumenten.tijdkenmerk;
import static dev.uchar.regelspraak.cases.TmsTijdlijnDocumenten.tijdlijn;
import static dev.uchar.regelspraak.cases.TmsTijdlijnDocumenten.verwachteTijdlijn;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Uniqueness over a key on a timeline on the exec-model branch, ever-uniqueness (RUNTIME-SEMANTICS ch. 5.9, rule
 * 1): every value the key's timeline ever carries is claimed, so two instances collide where they carried one value
 * in different periods, and an instance that carries one value in two periods claims it once and does not collide
 * with itself. The key is a timeline a rule derives, behind a gate another rule derives, so it stands only at the
 * fixpoint.
 *
 * <p>The model: "Een Vondst is geregistreerd indien zijn code groter is dan 0"; "Het nummer van een Vondst moet
 * berekend worden als het aantal dagen in het jaar dat hij bewaard is, indien hij geregistreerd is", a day count onto
 * a target on the year grid that only this rule writes, so it stands where the count is empty or the rule does not
 * apply (ch. 7.8); and "De nummers van alle Vondsten moeten uniek zijn" (eenmalig nummer). By hand, with code 1
 * unless said otherwise: V1, bewaard on [2024-01-01, 2024-01-11), gets nummer 10 in 2024; V2, on [2025-03-01,
 * 2025-03-11), 10 in 2025; V3, on [2024-01-01, 2024-01-06), 5 in 2024; V4, nummer 5 in 2024 and 7 in 2025 as input
 * and no true period, keeps its input; V5, nummer 3 in 2024 and 3 in 2025 as input, keeps it and claims 3 once; V6,
 * on [2026-01-01, 2026-01-08) over the input 10 in 2023, gets 7 in 2026, which replaces the input whole; V7, on
 * [2027-01-01, 2027-01-03) over the input 3 in 2023, gets 2 in 2027; V8, code 0 and so not geregistreerd, on
 * [2024-01-01, 2024-01-11) over the input 4 in 2028, keeps its input. So 10 is claimed by V1 and V2, 5 by V3 and V4,
 * 7 by V4 and V6, and 3, 2 and 4 once each: eenmalig nummer is inconsistent at V1, V2, V3, V4 and V6, and only there.
 * Both orders of the two rules; beside them the generated rules in a hundred sampled orders, where the channel at the
 * fixpoint is exactly those five, and in some order V5 was marked on the way, while V7 still held its input 3.
 *
 * <p><b>Measured</b> (2026-10-09, the document of {@link #main} in both orders): the verifier passes both, and the
 * reference interpreter's runner and the ASP runner pass the case in both, "PASS (13 expectations)". An expectation
 * "inconsistent" asks only that the channel carries the fact, so beside the case three probes add one that V5, V7 or
 * V8 is marked: both runners answer MISMATCH on each, the channel carrying exactly "eenmalig nummer × V1; … × V2; …
 * × V3; … × V4; … × V6".
 *
 * <p><b>Red</b> on b9f77aa7, the code before the change: both orders, "but was: &lt;[[registratie, nummer] Excluded:
 * tms-transpiler SKIP: regel: nummer — het tijdsafhankelijke attribuut 'nummer' van Vondst: tijdlijnen zijn buiten de
 * proef van de truth-maintenance-tak, …&gt;", and the build of the sampled orders on the same refusal; the
 * uniqueness itself was refused there in the same words ("uniciteit: eenmalig nummer — het tijdsafhankelijke
 * attribuut 'nummer' van Vondst: …", every refusal of the document listed from that build's manifest). <b>Red by
 * mutation</b> (not committed): with the key rule claiming only the least value of a timeline, both orders fail,
 * "case: verwacht inconsistent (eenmalig nummer @ V6), niet gevonden onder …", and the sampled orders find the
 * channel without V6.
 */
class TmsOoitUniciteitWitnessTest {

    static final String MODEL = TmsDocumenten.model(List.of(objecttype("Vondst", List.of(
                    TmsDocumenten.attribuut("code", GEHEEL), tijdattribuut("nummer", GEHEEL, "year")),
            List.of(tijdkenmerk("bewaard", "month"), kenmerk("geregistreerd")))), List.of());

    static final String REGISTRATIE = kenmerkregel("registratie", "Vondst", "geregistreerd",
            vergelijking("greater", lezing("code", "Vondst"), getal("0")));
    static final String NUMMER = rekenregel("nummer", "BEREKENING", "Vondst", "nummer",
            dagentelling(check("bewaard", false), "year"), check("geregistreerd", false));
    static final String EENMALIG = TmsUniciteitWitnessTest.uniciteit("eenmalig nummer",
            List.of(TmsUniciteitWitnessTest.bron("Vondst", List.of("nummer"))), null,
            "De nummers van alle Vondsten moeten uniek zijn.");

    static final List<String> REGELS = List.of(REGISTRATIE, NUMMER);

    /** The year {@code jaar} as a segment with the number {@code lexeem}. */
    static String jaar(int jaar, String lexeem) {
        return segment(jaar + "-01-01", (jaar + 1) + "-01-01", lexeem);
    }

    /** A Vondst of the case with the code {@code code}, bewaard on {@code [van, tot)} where {@code van} is not null, and
     *  the nummer {@code invoer} as input where it is not empty. */
    static String vondst(String label, String code, String van, String tot, List<String> invoer) {
        Map<String, String> waarden = new LinkedHashMap<>();
        waarden.put("code", getalwaarde(code));
        if (!invoer.isEmpty()) waarden.put("nummer", tijdlijn(invoer));
        return instantie(label, "Vondst", waarden, List.of(),
                van == null ? Map.of() : Map.of("bewaard", List.of(periode(van, tot))), Map.of());
    }

    static JsonNode zaak() throws Exception {
        return TmsTijdlijnDocumenten.zaak(null, List.of(
                        vondst("V1", "1", "2024-01-01", "2024-01-11", List.of()),
                        vondst("V2", "1", "2025-03-01", "2025-03-11", List.of()),
                        vondst("V3", "1", "2024-01-01", "2024-01-06", List.of()),
                        vondst("V4", "1", null, null, List.of(jaar(2024, "5"), jaar(2025, "7"))),
                        vondst("V5", "1", null, null, List.of(jaar(2024, "3"), jaar(2025, "3"))),
                        vondst("V6", "1", "2026-01-01", "2026-01-08", List.of(jaar(2023, "10"))),
                        vondst("V7", "1", "2027-01-01", "2027-01-03", List.of(jaar(2023, "3"))),
                        vondst("V8", "0", "2024-01-01", "2024-01-11", List.of(jaar(2028, "4")))),
                List.of(verwachteTijdlijn("V1", "nummer", List.of(jaar(2024, "10"))),
                        verwachteTijdlijn("V2", "nummer", List.of(jaar(2025, "10"))),
                        verwachteTijdlijn("V3", "nummer", List.of(jaar(2024, "5"))),
                        verwachteTijdlijn("V4", "nummer", List.of(jaar(2024, "5"), jaar(2025, "7"))),
                        verwachteTijdlijn("V5", "nummer", List.of(jaar(2024, "3"), jaar(2025, "3"))),
                        verwachteTijdlijn("V6", "nummer", List.of(jaar(2026, "7"))),
                        verwachteTijdlijn("V7", "nummer", List.of(jaar(2027, "2"))),
                        verwachteTijdlijn("V8", "nummer", List.of(jaar(2028, "4"))),
                        inconsistent("eenmalig nummer", "V1"), inconsistent("eenmalig nummer", "V2"),
                        inconsistent("eenmalig nummer", "V3"), inconsistent("eenmalig nummer", "V4"),
                        inconsistent("eenmalig nummer", "V6")));
    }

    @Test void een_waarde_die_ooit_op_de_tijdlijn_stond_botst_in_beide_volgordes_van_de_regels() throws Exception {
        JsonNode zaak = zaak();
        List<String> uitkomsten = new ArrayList<>(), verwacht = new ArrayList<>();
        for (List<String> volgorde : TmsDocumenten.permutaties(REGELS)) {
            List<String> namen = TmsDocumenten.regelnamen(volgorde);
            Path ir = TmsTijdlijnDocumenten.document(MODEL, volgorde, List.of(EENMALIG));
            uitkomsten.add(namen + " " + TmsFoutWitnessTest.speel(ir, zaak, DroolsBackend.Branch.EXEC_MODEL));
            verwacht.add(namen + " gespeeld");
        }
        assertEquals(verwacht, uitkomsten);
    }

    @Test void het_kanaal_houdt_aan_het_fixpunt_precies_de_botsingen_van_de_afgeleide_sleutels() throws Exception {
        Path ir = TmsTijdlijnDocumenten.document(MODEL, REGELS, List.of(EENMALIG));
        DroolsBackend.Build b = DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL);
        int tussenstand = 0;
        for (List<Rule> volgorde : TmsDocumenten.steekproef(b.execModelTranspiler().model().getRules(), 20261009L, 100)) {
            List<String> namen = volgorde.stream().map(Rule::getName).toList();
            ClassLoader l = b.beanLoader();
            Map<Object, String> labels = new IdentityHashMap<>();
            List<Object> bonen = new ArrayList<>();
            Object[][] opgave = {
                    {"V1", "1", "2024-01-01", "2024-01-11", new String[0]},
                    {"V2", "1", "2025-03-01", "2025-03-11", new String[0]},
                    {"V3", "1", "2024-01-01", "2024-01-06", new String[0]},
                    {"V4", "1", null, null, new String[]{"2024", "5", "2025", "7"}},
                    {"V5", "1", null, null, new String[]{"2024", "3", "2025", "3"}},
                    {"V6", "1", "2026-01-01", "2026-01-08", new String[]{"2023", "10"}},
                    {"V7", "1", "2027-01-01", "2027-01-03", new String[]{"2023", "3"}},
                    {"V8", "0", "2024-01-01", "2024-01-11", new String[]{"2028", "4"}}};
            for (Object[] o : opgave) {
                Object v = TmsDocumenten.boon(l, "Vondst", Map.of("code", (String) o[1]), List.of());
                if (o[2] != null) TmsTijdlijnDocumenten.zet(v, "bewaard",
                        List.of(TmsTijdlijnDocumenten.boonsegment(l, (String) o[2], (String) o[3], null)));
                String[] invoer = (String[]) o[4];
                List<Object> segmenten = new ArrayList<>();
                for (int i = 0; i < invoer.length; i += 2) {
                    int j = Integer.parseInt(invoer[i]);
                    segmenten.add(TmsTijdlijnDocumenten.boonsegment(l, j + "-01-01", (j + 1) + "-01-01", invoer[i + 1]));
                }
                if (!segmenten.isEmpty()) TmsTijdlijnDocumenten.zet(v, "nummer", segmenten);
                labels.put(v, (String) o[0]);
                bonen.add(v);
            }
            Object v5 = bonen.get(4);
            int[] onderweg = {0};
            List<String> kanaal = new ArrayList<>();
            TmsDocumenten.vuur(b, volgorde, bonen, sessie -> sessie.addEventListener(
                    new DefaultRuleRuntimeEventListener() {
                        @Override public void objectInserted(ObjectInsertedEvent e) {
                            Object f = e.getObject();
                            if (f.getClass().getSimpleName().equals("Inconsistentie")) {
                                try {
                                    if (f.getClass().getMethod("getSubject").invoke(f) == v5) onderweg[0]++;
                                } catch (ReflectiveOperationException x) {
                                    throw new IllegalStateException(x);
                                }
                            }
                        }
                    }), sessie -> kanaal.addAll(TmsDocumenten.inconsistenties(sessie, labels)));
            assertEquals(List.of("eenmalig nummer @ V1", "eenmalig nummer @ V2", "eenmalig nummer @ V3",
                    "eenmalig nummer @ V4", "eenmalig nummer @ V6"), kanaal, () -> "volgorde " + namen);
            assertEquals(List.of("[2027-01-01, 2028-01-01)=2"), TmsTijdlijnWitnessTest.tijdlijnVan(bonen.get(6), "nummer"),
                    () -> "nummer van V7 in volgorde " + namen);
            assertEquals(List.of("[2028-01-01, 2029-01-01)=4"), TmsTijdlijnWitnessTest.tijdlijnVan(bonen.get(7), "nummer"),
                    () -> "nummer van V8 in volgorde " + namen);
            if (onderweg[0] > 0) tussenstand++;
        }
        assertTrue(tussenstand > 0, "geen volgorde liet V5 onderweg met de invoer van V7 botsen");
    }

    /** Writes the document in both orders of its rules, with its case, under {@code args[0]}: a map per order holding
     *  {@code ir.json} and {@code case.json}, for the verifier, ref and clingo. */
    public static void main(String[] args) throws Exception {
        Path uit = Path.of(args[0]);
        int n = 0;
        for (List<String> volgorde : TmsDocumenten.permutaties(REGELS))
            TmsTijdlijnWitnessTest.schrijf(uit.resolve("ooit-uniciteit").resolve(String.format("v%03d", n++)),
                    TmsTijdlijnDocumenten.ir(MODEL, volgorde, List.of(EENMALIG)), zaak());
    }
}
