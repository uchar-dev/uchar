package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import org.drools.model.Rule;
import org.junit.jupiter.api.Test;
import org.kie.api.event.rule.DefaultRuleRuntimeEventListener;
import org.kie.api.event.rule.ObjectInsertedEvent;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocumenten.GEHEEL;
import static dev.uchar.regelspraak.cases.TmsDocumenten.attribuut;
import static dev.uchar.regelspraak.cases.TmsDocumenten.binair;
import static dev.uchar.regelspraak.cases.TmsDocumenten.consistent;
import static dev.uchar.regelspraak.cases.TmsDocumenten.feittypeEenNaarVeel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.inconsistent;
import static dev.uchar.regelspraak.cases.TmsDocumenten.instantieMetRollen;
import static dev.uchar.regelspraak.cases.TmsDocumenten.kanaaltoets;
import static dev.uchar.regelspraak.cases.TmsDocumenten.kenmerkregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.lezing;
import static dev.uchar.regelspraak.cases.TmsDocumenten.rekenregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.verwacht;
import static dev.uchar.regelspraak.cases.TmsDocumenten.waarde;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Uniqueness on the truth-maintenance branch (RUNTIME-SEMANTICS ch. 5.6, 5.9): a duplicate non-empty key marks every
 * instance that carries it on the §9.5 channel, under the form's name, and the form counts at the fixpoint of its
 * stratum, so a duplicate that arises on an intermediate state and passes is not on the channel.
 *
 * <p>The model: a Kust holds its Vondsten (ligging: kust, one Kust; vondst, many Vondsten). "De factor van een Vondst
 * moet berekend worden als zijn opgave", "De toeslag van een Vondst moet berekend worden als zijn factor" and "Het
 * nummer van een Vondst moet berekend worden als zijn basis plus zijn toeslag", so the nummer is first the basis and
 * then the basis plus the opgave in the orders where the nummer is computed before the toeslag (a leeg toeslag adds 0).
 * "De nummers van alle Vondsten moeten uniek zijn" (eenmalig nummer), and "Een Kust is verdacht indien regelversie
 * eenmalig nummer is inconsistent" reads its channel; a second document scopes the uniqueness per Kust instead ("De
 * nummers van alle Vondsten van de kust moeten uniek zijn", nummer per kust). World 1: K1 with V1 (basis 5, opgave 1)
 * and V2 (basis 5, opgave 2): nummers 6 and 7, consistent, though V1 and V2 both carry 5 while the toeslag is leeg.
 * World 2: V3 (basis 4, opgave 2) joins them: V1 and V3 carry 6, both marked, in either scope. World 3: V1 at K1, V3
 * at K2 and V4 (basis 3, opgave 3) at no Kust: all three carry 6, all marked by the unscoped form, none by the scoped
 * one, since each Kust holds one and V4 takes no part (ch. 5.9, rule 2). Every order of the rules of each document.
 * Beside the documents, the generated rules of the first in a hundred sampled orders: the channel at the fixpoint is
 * exactly V1 and V3 of world 2, and in some orders V2 was marked on the way, while it carried 5.
 *
 * <p>The union (ch. 5.9, rule 1): "Strandvondst verenigd met Wrakvondst moeten uniek zijn" over the vondstnummer, the
 * Strandvondst's computed from its opgave, and "Baken verenigd met Baken" over the landcode and the zeecode, so a
 * Baken whose two codes are equal duplicates against itself; both orders of the two forms.
 *
 * <p><b>Measured</b> (2026-10-09, on the documents of this class, every order; the verifier passes all 96): the
 * reference interpreter's runner passes every case in every order. The ASP runner passes the 24 documents without the
 * reader of the channel and refuses the 72 with it, "TRANSLATION FAULT: condition kind not translated (no corpus case
 * exercises it)": it does not translate "regelversie X is inconsistent"; with that rule and its expectations taken
 * out of the same 72 documents it passes them all.
 *
 * <p><b>Red</b> on dde9507d: the document with the reader in every order, "Excluded: tms-transpiler SKIP: regel:
 * verdenking — een Regelversie 'is inconsistent' over "eenmalig nummer" in de voorwaarde (…): de truth-maintenance-tak
 * draagt als producent van het §9.5-kanaal alleen een Consistency en een regel met een deling of een wortel …"; the
 * scoped one, "… SKIP: uniciteit: nummer per kust — een Uniqueness: buiten de proef van de truth-maintenance-tak"; the
 * build of the sampled orders, the same sentence for eenmalig nummer; the union, "… SKIP: uniciteit-unie: eenmalig
 * vondstnummer — een Uniqueness over meer bronnen: buiten de proef van de truth-maintenance-tak".
 */
class TmsUniciteitWitnessTest {

    static final String MODEL = TmsDocumenten.model(List.of(
            TmsDocumenten.objecttype("Kust", List.of(), List.of("verdacht")),
            TmsDocumenten.objecttype("Vondst", List.of(attribuut("basis", GEHEEL), attribuut("opgave", GEHEEL),
                    attribuut("factor", GEHEEL), attribuut("toeslag", GEHEEL), attribuut("nummer", GEHEEL)), List.of())),
            List.of(feittypeEenNaarVeel("ligging", "kust", "Kust", "vondst", "Vondst", "vondsten")));

    static final String FACTOR = rekenregel("factor", "BEREKENING", "Vondst", "factor", lezing("opgave", "Vondst"), "null");
    static final String TOESLAG = rekenregel("toeslag", "BEREKENING", "Vondst", "toeslag", lezing("factor", "Vondst"),
            "null");
    static final String NUMMER = rekenregel("nummer", "BEREKENING", "Vondst", "nummer",
            binair("plus", lezing("basis", "Vondst"), lezing("toeslag", "Vondst")), "null");
    static final String VERDACHT = kenmerkregel("verdenking", "Kust", "verdacht", kanaaltoets("eenmalig nummer"));

    /** A Uniqueness of one always-valid version over {@code bronnen} ({@link #bron}), scoped where {@code scope} is not
     *  null. */
    static String uniciteit(String naam, List<String> bronnen, String scope, String tekst) {
        return "{\"node\":\"Uniqueness\",\"name\":\"" + naam + "\",\"versions\":[{\"sources\":[" + String.join(",", bronnen)
                + "]" + (scope == null ? "" : ",\"scope\":" + scope) + ",\"validity\":{\"node\":\"Validity\"},"
                + "\"source\":{\"snippet\":\"" + tekst + "\"}}]}";
    }

    static String bron(String type, List<String> sleutels) {
        List<String> s = new ArrayList<>();
        for (String k : sleutels) s.add("\"" + k + "\"");
        return "{\"node\":\"Source\",\"objectType\":\"" + type + "\",\"keys\":[" + String.join(",", s) + "]}";
    }

    static final String EENMALIG = uniciteit("eenmalig nummer", List.of(bron("Vondst", List.of("nummer"))), null,
            "De nummers van alle Vondsten moeten uniek zijn.");
    static final String PER_KUST = uniciteit("nummer per kust", List.of(bron("Vondst", List.of("nummer"))),
            TmsDocumenten.rolbinding("ligging", "kust"), "De nummers van alle Vondsten van de kust moeten uniek zijn.");

    /** A document with the uniquenesses beside the rules, each in its order. */
    static Path document(String model, List<String> regels, List<String> uniciteiten) throws Exception {
        String ir = "{\"format\":\"uchar-ir-v1\",\"model\":" + model + ",\"rules\":{\"rules\":[" + String.join(",", regels)
                + "],\"dayKindDefinitions\":[],\"objectCreations\":[],\"allocations\":[],\"consistencies\":[],"
                + "\"uniquenesses\":[" + String.join(",", uniciteiten) + "],\"factCreations\":[],"
                + "\"startPointDeterminations\":[],\"ruleGroups\":[],\"prioritizedDefaults\":[]},\"diagnostics\":[]}";
        Path tmp = Files.createTempFile("tms-uniciteit", ".json");
        tmp.toFile().deleteOnExit();
        return Files.writeString(tmp, ir);
    }

    static String vondst(String label, int basis, int opgave) {
        return instantieMetRollen(label, "Vondst", Map.of("basis", "" + basis, "opgave", "" + opgave), List.of(),
                Map.of());
    }

    static String kust(String label, List<String> vondsten) {
        return instantieMetRollen(label, "Kust", Map.of(), List.of(), Map.of("vondsten", vondsten));
    }

    static List<String> wereld(int n) {
        return switch (n) {
            case 1 -> List.of(kust("K1", List.of("V1", "V2")), vondst("V1", 5, 1), vondst("V2", 5, 2));
            case 2 -> List.of(kust("K1", List.of("V1", "V2", "V3")), vondst("V1", 5, 1), vondst("V2", 5, 2),
                    vondst("V3", 4, 2));
            case 3 -> List.of(kust("K1", List.of("V1")), kust("K2", List.of("V3")), vondst("V1", 5, 1),
                    vondst("V3", 4, 2), vondst("V4", 3, 3));
            default -> throw new IllegalArgumentException("wereld " + n);
        };
    }

    /** The nummers of world {@code n} at the fixpoint. */
    static List<String> nummers(int n) {
        return switch (n) {
            case 1 -> List.of(waarde("V1", "nummer", "6"), waarde("V2", "nummer", "7"));
            case 2 -> List.of(waarde("V1", "nummer", "6"), waarde("V2", "nummer", "7"), waarde("V3", "nummer", "6"));
            case 3 -> List.of(waarde("V1", "nummer", "6"), waarde("V3", "nummer", "6"), waarde("V4", "nummer", "6"));
            default -> throw new IllegalArgumentException("wereld " + n);
        };
    }

    @Test void de_schending_staat_alleen_waar_ze_aan_het_fixpunt_staat() throws Exception {
        List<List<String>> kanaal = List.of(List.of(consistent(), verwacht("K1", "verdacht", false)),
                List.of(inconsistent("eenmalig nummer", "V1"), inconsistent("eenmalig nummer", "V3"),
                        verwacht("K1", "verdacht", true)),
                List.of(inconsistent("eenmalig nummer", "V1"), inconsistent("eenmalig nummer", "V3"),
                        inconsistent("eenmalig nummer", "V4"), verwacht("K1", "verdacht", true),
                        verwacht("K2", "verdacht", true)));
        List<String> uitkomsten = new ArrayList<>(), verwacht = new ArrayList<>();
        for (List<String> volgorde : TmsDocumenten.permutaties(List.of(FACTOR, TOESLAG, NUMMER, VERDACHT))) {
            List<String> namen = TmsDocumenten.regelnamen(volgorde);
            Path ir = document(MODEL, volgorde, List.of(EENMALIG));
            for (int n = 1; n <= 3; n++) {
                List<String> v = new ArrayList<>(nummers(n));
                v.addAll(kanaal.get(n - 1));
                JsonNode zaak = TmsDocumenten.zaak(wereld(n), v);
                uitkomsten.add(namen + " " + n + " " + TmsFoutWitnessTest.speel(ir, zaak,
                        DroolsBackend.Branch.TRUTH_MAINTENANCE));
                verwacht.add(namen + " " + n + " gespeeld");
            }
        }
        assertEquals(verwacht, uitkomsten);
    }

    @Test void de_scope_verdeelt_en_wie_geen_scope_heeft_doet_niet_mee() throws Exception {
        List<List<String>> kanaal = List.of(List.of(consistent()),
                List.of(inconsistent("nummer per kust", "V1"), inconsistent("nummer per kust", "V3")),
                List.of(consistent()));
        List<String> uitkomsten = new ArrayList<>(), verwacht = new ArrayList<>();
        for (List<String> volgorde : TmsDocumenten.permutaties(List.of(FACTOR, TOESLAG, NUMMER))) {
            List<String> namen = TmsDocumenten.regelnamen(volgorde);
            Path ir = document(MODEL, volgorde, List.of(PER_KUST));
            for (int n = 1; n <= 3; n++) {
                List<String> v = new ArrayList<>(nummers(n));
                v.addAll(kanaal.get(n - 1));
                JsonNode zaak = TmsDocumenten.zaak(wereld(n), v);
                uitkomsten.add(namen + " " + n + " " + TmsFoutWitnessTest.speel(ir, zaak,
                        DroolsBackend.Branch.TRUTH_MAINTENANCE));
                verwacht.add(namen + " " + n + " gespeeld");
            }
        }
        assertEquals(verwacht, uitkomsten);
    }

    @Test void een_schending_op_een_tussenstand_verdwijnt_weer_in_elke_volgorde_van_de_regels() throws Exception {
        Path ir = document(MODEL, List.of(FACTOR, TOESLAG, NUMMER), List.of(EENMALIG));
        DroolsBackend.Build b = DroolsBackend.build(ir, DroolsBackend.Branch.TRUTH_MAINTENANCE);
        int tussenstand = 0;
        for (List<Rule> volgorde : TmsDocumenten.steekproef(b.tmsTranspiler().model().getRules(), 20261009L, 100)) {
            List<String> namen = volgorde.stream().map(Rule::getName).toList();
            Object v1 = TmsDocumenten.boon(b.beanLoader(), "Vondst", Map.of("basis", "5", "opgave", "1"), List.of());
            Object v2 = TmsDocumenten.boon(b.beanLoader(), "Vondst", Map.of("basis", "5", "opgave", "2"), List.of());
            Object v3 = TmsDocumenten.boon(b.beanLoader(), "Vondst", Map.of("basis", "4", "opgave", "2"), List.of());
            Map<Object, String> labels = new IdentityHashMap<>(Map.of(v1, "V1", v2, "V2", v3, "V3"));
            int[] onderweg = {0};
            List<String> kanaal = new ArrayList<>();
            TmsDocumenten.vuur(b, volgorde, List.of(v1, v2, v3), sessie -> sessie.addEventListener(
                    new DefaultRuleRuntimeEventListener() {
                        @Override public void objectInserted(ObjectInsertedEvent e) {
                            Object f = e.getObject();
                            if (f.getClass().getSimpleName().equals("Inconsistentie")) {
                                try {
                                    if (f.getClass().getMethod("getSubject").invoke(f) == v2) onderweg[0]++;
                                } catch (ReflectiveOperationException x) {
                                    throw new IllegalStateException(x);
                                }
                            }
                        }
                    }), sessie -> kanaal.addAll(TmsDocumenten.inconsistenties(sessie, labels)));
            assertEquals(List.of("eenmalig nummer @ V1", "eenmalig nummer @ V3"), kanaal, () -> "volgorde " + namen);
            assertEquals(0, new BigDecimal("6").compareTo((BigDecimal) TmsDocumenten.lees(v1, "nummer")),
                    () -> "nummer van V1 in volgorde " + namen);
            assertEquals(0, new BigDecimal("7").compareTo((BigDecimal) TmsDocumenten.lees(v2, "nummer")),
                    () -> "nummer van V2 in volgorde " + namen);
            if (onderweg[0] > 0) tussenstand++;
        }
        assertTrue(tussenstand > 0, "geen volgorde liet V2 onderweg met V1 botsen");
    }

    // ---------------- the union ----------------

    static final String UNIEMODEL = TmsDocumenten.model(List.of(
            TmsDocumenten.objecttype("Strandvondst", List.of(attribuut("opgave", GEHEEL), attribuut("vondstnummer", GEHEEL)),
                    List.of()),
            TmsDocumenten.objecttype("Wrakvondst", List.of(attribuut("vondstnummer", GEHEEL)), List.of()),
            TmsDocumenten.objecttype("Baken", List.of(attribuut("landcode", GEHEEL), attribuut("zeecode", GEHEEL)),
                    List.of())), List.of());

    static final String STRANDNUMMER = rekenregel("strandnummer", "BEREKENING", "Strandvondst", "vondstnummer",
            lezing("opgave", "Strandvondst"), "null");
    static final String VONDSTUNIE = uniciteit("eenmalig vondstnummer", List.of(bron("Strandvondst",
            List.of("vondstnummer")), bron("Wrakvondst", List.of("vondstnummer"))), null,
            "Strandvondst verenigd met Wrakvondst moeten uniek zijn.");
    static final String BAKENUNIE = uniciteit("eenmalige bakencode", List.of(bron("Baken", List.of("landcode")),
            bron("Baken", List.of("zeecode"))), null, "Baken verenigd met Baken moeten uniek zijn.");

    static String inst(String label, String type, Map<String, String> getallen) {
        return instantieMetRollen(label, type, getallen, List.of(), Map.of());
    }

    @Test void de_unie_deelt_een_sleuteldomein_en_telt_een_instantie_per_bron() throws Exception {
        List<JsonNode> zaken = List.of(
                TmsDocumenten.zaak(List.of(inst("S1", "Strandvondst", Map.of("opgave", "7")),
                        inst("W1", "Wrakvondst", Map.of("vondstnummer", "8")),
                        inst("B1", "Baken", Map.of("landcode", "3", "zeecode", "4")),
                        inst("B2", "Baken", Map.of("landcode", "5", "zeecode", "6"))),
                        List.of(waarde("S1", "vondstnummer", "7"), consistent())),
                TmsDocumenten.zaak(List.of(inst("S1", "Strandvondst", Map.of("opgave", "7")),
                        inst("W1", "Wrakvondst", Map.of("vondstnummer", "7")),
                        inst("B1", "Baken", Map.of("landcode", "3", "zeecode", "3"))),
                        List.of(inconsistent("eenmalig vondstnummer", "S1"), inconsistent("eenmalig vondstnummer", "W1"),
                                inconsistent("eenmalige bakencode", "B1"))),
                TmsDocumenten.zaak(List.of(inst("S1", "Strandvondst", Map.of("opgave", "7")),
                        inst("S2", "Strandvondst", Map.of("opgave", "7")),
                        inst("B1", "Baken", Map.of("landcode", "3", "zeecode", "4")),
                        inst("B2", "Baken", Map.of("landcode", "4", "zeecode", "5"))),
                        List.of(inconsistent("eenmalig vondstnummer", "S1"), inconsistent("eenmalig vondstnummer", "S2"),
                                inconsistent("eenmalige bakencode", "B1"), inconsistent("eenmalige bakencode", "B2"))));
        List<String> uitkomsten = new ArrayList<>(), verwacht = new ArrayList<>();
        for (List<String> uniciteiten : TmsDocumenten.permutaties(List.of(VONDSTUNIE, BAKENUNIE))) {
            Path ir = document(UNIEMODEL, List.of(STRANDNUMMER), uniciteiten);
            for (int n = 0; n < zaken.size(); n++) {
                uitkomsten.add(uniciteiten.indexOf(VONDSTUNIE) + " " + n + " " + TmsFoutWitnessTest.speel(ir, zaken.get(n),
                        DroolsBackend.Branch.TRUTH_MAINTENANCE));
                verwacht.add(uniciteiten.indexOf(VONDSTUNIE) + " " + n + " gespeeld");
            }
        }
        assertEquals(verwacht, uitkomsten);
    }
}
