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

import static dev.uchar.regelspraak.cases.TmsDocuments.WHOLE;
import static dev.uchar.regelspraak.cases.TmsDocuments.attribuut;
import static dev.uchar.regelspraak.cases.TmsDocuments.binary;
import static dev.uchar.regelspraak.cases.TmsDocuments.consistent;
import static dev.uchar.regelspraak.cases.TmsDocuments.factTypeOneToMany;
import static dev.uchar.regelspraak.cases.TmsDocuments.inconsistent;
import static dev.uchar.regelspraak.cases.TmsDocuments.instanceWithRoles;
import static dev.uchar.regelspraak.cases.TmsDocuments.channelTest;
import static dev.uchar.regelspraak.cases.TmsDocuments.characteristicRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.ownRead;
import static dev.uchar.regelspraak.cases.TmsDocuments.computingRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.verwacht;
import static dev.uchar.regelspraak.cases.TmsDocuments.waarde;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Uniqueness on the exec-model branch (RUNTIME-SEMANTICS ch. 5.6, 5.9): a duplicate non-empty key marks every
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
 * vondstnummer — een Uniqueness over meer bronnen: buiten de proef van de truth-maintenance-tak". <b>Red by
 * mutation</b> (not committed): with the violation inserted as a stated fact, the sampled orders fail, "expected:
 * &lt;[eenmalig nummer @ V1, eenmalig nummer @ V3]&gt; but was: &lt;[eenmalig nummer @ V1, eenmalig nummer @ V2,
 * eenmalig nummer @ V3]&gt;", while the documents in their own orders do not: there V2 never collided on the way.
 */
class TmsUniquenessWitnessTest {

    static final String MODEL = TmsDocuments.model(List.of(
            TmsDocuments.objecttype("Kust", List.of(), List.of("verdacht")),
            TmsDocuments.objecttype("Vondst", List.of(attribuut("basis", WHOLE), attribuut("opgave", WHOLE),
                    attribuut("factor", WHOLE), attribuut("toeslag", WHOLE), attribuut("nummer", WHOLE)), List.of())),
            List.of(factTypeOneToMany("ligging", "kust", "Kust", "vondst", "Vondst", "vondsten")));

    static final String FACTOR = computingRule("factor", "BEREKENING", "Vondst", "factor", ownRead("opgave", "Vondst"), "null");
    static final String TOESLAG = computingRule("toeslag", "BEREKENING", "Vondst", "toeslag", ownRead("factor", "Vondst"),
            "null");
    static final String NUMBER = computingRule("nummer", "BEREKENING", "Vondst", "nummer",
            binary("plus", ownRead("basis", "Vondst"), ownRead("toeslag", "Vondst")), "null");
    static final String SUSPECTED = characteristicRule("verdenking", "Kust", "verdacht", channelTest("eenmalig nummer"));

    /** A Uniqueness of one always-valid version over {@code bronnen} ({@link #bron}), scoped where {@code scope} is not
     *  null. */
    static String uniqueness(String naam, List<String> sources, String scope, String text) {
        return "{\"node\":\"Uniqueness\",\"name\":\"" + naam + "\",\"versions\":[{\"sources\":[" + String.join(",", sources)
                + "]" + (scope == null ? "" : ",\"scope\":" + scope) + ",\"validity\":{\"node\":\"Validity\"},"
                + "\"source\":{\"snippet\":\"" + text + "\"}}]}";
    }

    static String bron(String type, List<String> keyNames) {
        List<String> s = new ArrayList<>();
        for (String k : keyNames) s.add("\"" + k + "\"");
        return "{\"node\":\"Source\",\"objectType\":\"" + type + "\",\"keys\":[" + String.join(",", s) + "]}";
    }

    static final String EENMALIG = uniqueness("eenmalig nummer", List.of(bron("Vondst", List.of("nummer"))), null,
            "De nummers van alle Vondsten moeten uniek zijn.");
    static final String PER_KUST = uniqueness("nummer per kust", List.of(bron("Vondst", List.of("nummer"))),
            TmsDocuments.roleBinding("ligging", "kust"), "De nummers van alle Vondsten van de kust moeten uniek zijn.");

    /** A document with the uniquenesses beside the rules, each in its order. */
    static Path document(String model, List<String> rules, List<String> uniquenesses) throws Exception {
        String ir = "{\"format\":\"uchar-ir-v1\",\"model\":" + model + ",\"rules\":{\"rules\":[" + String.join(",", rules)
                + "],\"dayKindDefinitions\":[],\"objectCreations\":[],\"allocations\":[],\"consistencies\":[],"
                + "\"uniquenesses\":[" + String.join(",", uniquenesses) + "],\"factCreations\":[],"
                + "\"startPointDeterminations\":[],\"ruleGroups\":[],\"prioritizedDefaults\":[]},\"diagnostics\":[]}";
        Path tmp = Files.createTempFile("tms-uniciteit", ".json");
        tmp.toFile().deleteOnExit();
        return Files.writeString(tmp, ir);
    }

    static String vondst(String label, int base, int input) {
        return instanceWithRoles(label, "Vondst", Map.of("basis", "" + base, "opgave", "" + input), List.of(),
                Map.of());
    }

    static String kust(String label, List<String> vondsten) {
        return instanceWithRoles(label, "Kust", Map.of(), List.of(), Map.of("vondsten", vondsten));
    }

    static List<String> world(int n) {
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
    static List<String> numbersOfWorld(int n) {
        return switch (n) {
            case 1 -> List.of(waarde("V1", "nummer", "6"), waarde("V2", "nummer", "7"));
            case 2 -> List.of(waarde("V1", "nummer", "6"), waarde("V2", "nummer", "7"), waarde("V3", "nummer", "6"));
            case 3 -> List.of(waarde("V1", "nummer", "6"), waarde("V3", "nummer", "6"), waarde("V4", "nummer", "6"));
            default -> throw new IllegalArgumentException("wereld " + n);
        };
    }

    @Test void the_violation_stands_only_where_it_stands_at_the_fixpoint() throws Exception {
        List<List<String>> channel = List.of(List.of(consistent(), verwacht("K1", "verdacht", false)),
                List.of(inconsistent("eenmalig nummer", "V1"), inconsistent("eenmalig nummer", "V3"),
                        verwacht("K1", "verdacht", true)),
                List.of(inconsistent("eenmalig nummer", "V1"), inconsistent("eenmalig nummer", "V3"),
                        inconsistent("eenmalig nummer", "V4"), verwacht("K1", "verdacht", true),
                        verwacht("K2", "verdacht", true)));
        List<String> outcomes = new ArrayList<>(), verwacht = new ArrayList<>();
        for (List<String> order : TmsDocuments.permutations(List.of(FACTOR, TOESLAG, NUMBER, SUSPECTED))) {
            List<String> names = TmsDocuments.ruleNames(order);
            Path ir = document(MODEL, order, List.of(EENMALIG));
            for (int n = 1; n <= 3; n++) {
                List<String> v = new ArrayList<>(numbersOfWorld(n));
                v.addAll(channel.get(n - 1));
                JsonNode caseDocument = TmsDocuments.caseDocument(world(n), v);
                outcomes.add(names + " " + n + " " + TmsFaultWitnessTest.play(ir, caseDocument,
                        DroolsBackend.Branch.EXEC_MODEL));
                verwacht.add(names + " " + n + " gespeeld");
            }
        }
        assertEquals(verwacht, outcomes);
    }

    @Test void the_scope_divides_and_who_has_no_scope_takes_no_part() throws Exception {
        List<List<String>> channel = List.of(List.of(consistent()),
                List.of(inconsistent("nummer per kust", "V1"), inconsistent("nummer per kust", "V3")),
                List.of(consistent()));
        List<String> outcomes = new ArrayList<>(), verwacht = new ArrayList<>();
        for (List<String> order : TmsDocuments.permutations(List.of(FACTOR, TOESLAG, NUMBER))) {
            List<String> names = TmsDocuments.ruleNames(order);
            Path ir = document(MODEL, order, List.of(PER_KUST));
            for (int n = 1; n <= 3; n++) {
                List<String> v = new ArrayList<>(numbersOfWorld(n));
                v.addAll(channel.get(n - 1));
                JsonNode caseDocument = TmsDocuments.caseDocument(world(n), v);
                outcomes.add(names + " " + n + " " + TmsFaultWitnessTest.play(ir, caseDocument,
                        DroolsBackend.Branch.EXEC_MODEL));
                verwacht.add(names + " " + n + " gespeeld");
            }
        }
        assertEquals(verwacht, outcomes);
    }

    @Test void a_violation_on_an_intermediate_state_disappears_again_in_every_order_of_the_rules() throws Exception {
        Path ir = document(MODEL, List.of(FACTOR, TOESLAG, NUMBER), List.of(EENMALIG));
        DroolsBackend.Build b = DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL);
        int intermediateState = 0;
        for (List<Rule> order : TmsDocuments.sample(b.execModelTranspiler().model().getRules(), 20261009L, 100)) {
            List<String> names = order.stream().map(Rule::getName).toList();
            Object v1 = TmsDocuments.beanObject(b.beanLoader(), "Vondst", Map.of("basis", "5", "opgave", "1"), List.of());
            Object v2 = TmsDocuments.beanObject(b.beanLoader(), "Vondst", Map.of("basis", "5", "opgave", "2"), List.of());
            Object v3 = TmsDocuments.beanObject(b.beanLoader(), "Vondst", Map.of("basis", "4", "opgave", "2"), List.of());
            Map<Object, String> labels = new IdentityHashMap<>(Map.of(v1, "V1", v2, "V2", v3, "V3"));
            int[] intermediate = {0};
            List<String> channel = new ArrayList<>();
            TmsDocuments.vuur(b, order, List.of(v1, v2, v3), sessie -> sessie.addEventListener(
                    new DefaultRuleRuntimeEventListener() {
                        @Override public void objectInserted(ObjectInsertedEvent e) {
                            Object f = e.getObject();
                            if (f.getClass().getSimpleName().equals("Inconsistentie")) {
                                try {
                                    if (f.getClass().getMethod("getSubject").invoke(f) == v2) intermediate[0]++;
                                } catch (ReflectiveOperationException x) {
                                    throw new IllegalStateException(x);
                                }
                            }
                        }
                    }), sessie -> channel.addAll(TmsDocuments.inconsistenties(sessie, labels)));
            assertEquals(List.of("eenmalig nummer @ V1", "eenmalig nummer @ V3"), channel, () -> "volgorde " + names);
            assertEquals(0, new BigDecimal("6").compareTo((BigDecimal) TmsDocuments.lees(v1, "nummer")),
                    () -> "nummer van V1 in volgorde " + names);
            assertEquals(0, new BigDecimal("7").compareTo((BigDecimal) TmsDocuments.lees(v2, "nummer")),
                    () -> "nummer van V2 in volgorde " + names);
            if (intermediate[0] > 0) intermediateState++;
        }
        assertTrue(intermediateState > 0, "geen volgorde liet V2 onderweg met V1 botsen");
    }

    // ---------------- the union ----------------

    static final String UNION_MODEL = TmsDocuments.model(List.of(
            TmsDocuments.objecttype("Strandvondst", List.of(attribuut("opgave", WHOLE), attribuut("vondstnummer", WHOLE)),
                    List.of()),
            TmsDocuments.objecttype("Wrakvondst", List.of(attribuut("vondstnummer", WHOLE)), List.of()),
            TmsDocuments.objecttype("Baken", List.of(attribuut("landcode", WHOLE), attribuut("zeecode", WHOLE)),
                    List.of())), List.of());

    static final String STRAND_NUMBER = computingRule("strandnummer", "BEREKENING", "Strandvondst", "vondstnummer",
            ownRead("opgave", "Strandvondst"), "null");
    static final String VONDSTUNIE = uniqueness("eenmalig vondstnummer", List.of(bron("Strandvondst",
            List.of("vondstnummer")), bron("Wrakvondst", List.of("vondstnummer"))), null,
            "Strandvondst verenigd met Wrakvondst moeten uniek zijn.");
    static final String BAKEN_UNION = uniqueness("eenmalige bakencode", List.of(bron("Baken", List.of("landcode")),
            bron("Baken", List.of("zeecode"))), null, "Baken verenigd met Baken moeten uniek zijn.");

    static String inst(String label, String type, Map<String, String> numbers) {
        return instanceWithRoles(label, type, numbers, List.of(), Map.of());
    }

    @Test void the_union_shares_a_key_domain_and_counts_an_instance_per_source() throws Exception {
        List<JsonNode> cases = List.of(
                TmsDocuments.caseDocument(List.of(inst("S1", "Strandvondst", Map.of("opgave", "7")),
                        inst("W1", "Wrakvondst", Map.of("vondstnummer", "8")),
                        inst("B1", "Baken", Map.of("landcode", "3", "zeecode", "4")),
                        inst("B2", "Baken", Map.of("landcode", "5", "zeecode", "6"))),
                        List.of(waarde("S1", "vondstnummer", "7"), consistent())),
                TmsDocuments.caseDocument(List.of(inst("S1", "Strandvondst", Map.of("opgave", "7")),
                        inst("W1", "Wrakvondst", Map.of("vondstnummer", "7")),
                        inst("B1", "Baken", Map.of("landcode", "3", "zeecode", "3"))),
                        List.of(inconsistent("eenmalig vondstnummer", "S1"), inconsistent("eenmalig vondstnummer", "W1"),
                                inconsistent("eenmalige bakencode", "B1"))),
                TmsDocuments.caseDocument(List.of(inst("S1", "Strandvondst", Map.of("opgave", "7")),
                        inst("S2", "Strandvondst", Map.of("opgave", "7")),
                        inst("B1", "Baken", Map.of("landcode", "3", "zeecode", "4")),
                        inst("B2", "Baken", Map.of("landcode", "4", "zeecode", "5"))),
                        List.of(inconsistent("eenmalig vondstnummer", "S1"), inconsistent("eenmalig vondstnummer", "S2"),
                                inconsistent("eenmalige bakencode", "B1"), inconsistent("eenmalige bakencode", "B2"))));
        List<String> outcomes = new ArrayList<>(), verwacht = new ArrayList<>();
        for (List<String> uniquenesses : TmsDocuments.permutations(List.of(VONDSTUNIE, BAKEN_UNION))) {
            Path ir = document(UNION_MODEL, List.of(STRAND_NUMBER), uniquenesses);
            for (int n = 0; n < cases.size(); n++) {
                outcomes.add(uniquenesses.indexOf(VONDSTUNIE) + " " + n + " " + TmsFaultWitnessTest.play(ir, cases.get(n),
                        DroolsBackend.Branch.EXEC_MODEL));
                verwacht.add(uniquenesses.indexOf(VONDSTUNIE) + " " + n + " gespeeld");
            }
        }
        assertEquals(verwacht, outcomes);
    }
}
