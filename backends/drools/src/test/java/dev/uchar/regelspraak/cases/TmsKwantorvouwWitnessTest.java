package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import dev.uchar.regelspraak.transpiler.tms.HeeftKenmerk;

import org.drools.model.Rule;
import org.junit.jupiter.api.Test;
import org.kie.api.event.rule.DefaultRuleRuntimeEventListener;
import org.kie.api.event.rule.ObjectInsertedEvent;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocumenten.GEHEEL;
import static dev.uchar.regelspraak.cases.TmsDocumenten.attribuut;
import static dev.uchar.regelspraak.cases.TmsDocumenten.bronrol;
import static dev.uchar.regelspraak.cases.TmsDocumenten.check;
import static dev.uchar.regelspraak.cases.TmsDocumenten.consistent;
import static dev.uchar.regelspraak.cases.TmsDocumenten.feittypeEenNaarVeel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.getal;
import static dev.uchar.regelspraak.cases.TmsDocumenten.inconsistent;
import static dev.uchar.regelspraak.cases.TmsDocumenten.instantieMetRollen;
import static dev.uchar.regelspraak.cases.TmsDocumenten.kenmerkregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.lezing;
import static dev.uchar.regelspraak.cases.TmsDocumenten.rekenregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.samengesteld;
import static dev.uchar.regelspraak.cases.TmsDocumenten.vergelijking;
import static dev.uchar.regelspraak.cases.TmsDocumenten.verwacht;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The quantifier fold on the truth-maintenance branch (RUNTIME-SEMANTICS ch. 3.9a): one condition asked of every
 * member of a linked instance set, under all, none and the three counting quantifiers, over members whose derived
 * characteristic or attribute changes while the session fires.
 *
 * <p>The model: a Ploeg holds its Wachten (ploegindeling: ploeg, one Ploeg; wacht, many Wachten). "Een Wacht is
 * geschorst indien zijn klachten groter is dan 2" and "Een Wacht is bevoegd indien hij aan alle volgende voorwaarden
 * voldoet: zijn dienstjaren is groter dan 5; hij is niet geschorst", so in the orders where the bevoegdheid fires
 * before the schorsing, a Wacht with more than 2 klachten is bevoegd first and loses it afterwards. The folds read
 * that characteristic: sterk (all wachten bevoegd), zwak (none), bezet (at least 2), krap (at most 1) and paraat
 * (exactly 1), each in a document of its own with the two derivations, in every order of the three rules. Case A:
 * P1 with W1 (bevoegd) and W2 (8 dienstjaren, 3 klachten: geschorst), P2 with W3 and W4 (both bevoegd), P3 without
 * Wachten (the vacuous set) and P4 with W5 (geschorst); case B: P1 with W1, W2 and W6 (bevoegd), P2 with W7 alone,
 * geschorst as W2 is. Read on the transient bevoegdheid of W2 and W7, case B gives P1 sterk and P2 sterk, paraat and
 * not zwak, which the fixpoint does not. Then a fold with a filter over the same characteristic and a matrix over a derived attribute (punten,
 * computed from the opgave), in every order of four rules: gekwalificeerd, "de punten van alle wachten van de Ploeg
 * die bevoegd zijn groter is dan 5".
 *
 * <p>The short circuit (ch. 3.9a, 5.10): the contributors are settled in full first, filter included, and the
 * matrix is then read in the contributors' order and stops at the first member after which the verdict stands; a
 * member it does not reach raises no fault and deposits nothing. The matrix "zijn opgave gedeeld door zijn
 * deelnemers is groter dan 2" divides by a value derived from the aanmelding: A (10 by 1) holds, B (no aanmelding)
 * deposits the §6.5 inconsistency of a division by leeg and fails, C (aanmelding 0) faults. Under rustig (none) the
 * list A, B stops at A and B deposits nothing, while B, A deposits; under volledig (all) both lists deposit; A, C
 * under none plays and C, A faults. Krap and bezet over C alone settle on the empty prefix (at most 1 over one
 * contributor, at least 2 out of reach) and read nothing, so they neither fault nor deposit. Keurbaar (all wachten
 * whose quotient exceeds 2 are gediplomeerd) over D (10 by 1, not gediplomeerd) and B: the filter of every member is
 * read before the matrix, so B's filter deposits although D's matrix settles the fold.
 *
 * <p>The documents fix the order of the forms, not the order in which the agenda fires the generated rules, and in the
 * orders of the documents W2 need not pass through the transient bevoegdheid at all. So per quantifier the generated
 * rules of its document fire in 32 sampled orders over P1 of case B: the verdict is the fixpoint's in every one, and in
 * some W2 was bevoegd on the way.
 *
 * <p><b>Measured</b> (2026-10-09, on the documents of this class, every order; the verifier passes all 136). The
 * reference interpreter's runner passes every case the branch plays, in every order, and faults "EVALUATION FAULT:
 * division by numeric zero" on exactly the six the branch faults on (rustig over C, A; volledig over both lists; each
 * in both orders). The ASP runner passes the 110 documents without a leeg or zero divisor; on the other 26 it neither
 * stops short nor keys a deposit on the fold's subject: it faults "viol("division by numeric zero","C")" on all twelve
 * documents with C, also where the verdict stands before C (rustig over A, C; krap and bezet over C alone), and it
 * deposits on the member, "… × B", also where the matrix does not reach B (rustig over A, B; krap over B alone).
 *
 * <p><b>Red</b> on deeaaa26: all six tests, every play "Excluded: tms-transpiler SKIP: regel: … — een
 * QuantifierFold in de voorwaarde ('…'): buiten de proef van de truth-maintenance-tak", the sampled orders on the
 * build of their first document with the same sentence for sterkte. <b>Red by mutation</b> (not committed): with the
 * stop triggers taken out of the replay, the two tests of the short circuit fail.
 */
class TmsKwantorvouwWitnessTest {

    static final String MODEL = TmsDocumenten.model(List.of(
            TmsDocumenten.objecttype("Ploeg", List.of(), List.of("sterk", "zwak", "bezet", "krap", "paraat",
                    "gekwalificeerd", "rustig", "volledig", "keurbaar")),
            TmsDocumenten.objecttype("Wacht", List.of(attribuut("dienstjaren", GEHEEL), attribuut("klachten", GEHEEL),
                    attribuut("opgave", GEHEEL), attribuut("punten", GEHEEL), attribuut("aanmelding", GEHEEL),
                    attribuut("deelnemers", GEHEEL)), List.of("geschorst", "bevoegd", "gediplomeerd"))),
            List.of(feittypeEenNaarVeel("ploegindeling", "ploeg", "Ploeg", "wacht", "Wacht", "wachten")));

    /** A quantifier fold over the Wachten of the Ploeg; a threshold of 0 writes none (all, none). */
    static String kwantorvouw(String kwantor, int drempel, String filter, String toets, String bron) {
        return "{\"node\":\"QuantifierFold\",\"quantifier\":\"" + kwantor + "\""
                + (drempel > 0 ? ",\"threshold\":" + drempel : "") + ",\"elementType\":\"Wacht\""
                + (filter == null ? "" : ",\"filter\":" + filter) + ",\"test\":" + toets + ","
                + bronrol("ploegindeling", "wacht") + ",\"source\":{\"snippet\":\"" + bron + "\"}}";
    }

    static final String SCHORSING = kenmerkregel("schorsing", "Wacht", "geschorst",
            vergelijking("greater", lezing("klachten", "Wacht"), getal("2")));
    static final String BEVOEGDHEID = kenmerkregel("bevoegdheid", "Wacht", "bevoegd", samengesteld("all", 0, List.of(
            vergelijking("greater", lezing("dienstjaren", "Wacht"), getal("5")), check("geschorst", true))));
    static final String PUNTEN = rekenregel("punten", "BEREKENING", "Wacht", "punten", lezing("opgave", "Wacht"),
            "null");
    static final String DEELNEMERS = rekenregel("deelnemers", "BEREKENING", "Wacht", "deelnemers",
            lezing("aanmelding", "Wacht"), "null");

    /** The five quantifiers over "hij is bevoegd": the Ploeg's characteristic, the rule, the fold. */
    static final List<List<String>> KWANTOREN = List.of(
            List.of("sterk", kenmerkregel("sterkte", "Ploeg", "sterk", kwantorvouw("all", 0, null, check("bevoegd", false),
                    "alle wachten van de Ploeg bevoegd zijn")), "all"),
            List.of("zwak", kenmerkregel("zwakte", "Ploeg", "zwak", kwantorvouw("none", 0, null, check("bevoegd", false),
                    "geen van de wachten van de Ploeg bevoegd is")), "none"),
            List.of("bezet", kenmerkregel("bezetting", "Ploeg", "bezet", kwantorvouw("atLeast", 2, null,
                    check("bevoegd", false), "ten minste 2 van de wachten van de Ploeg bevoegd zijn")), "atLeast"),
            List.of("krap", kenmerkregel("krapte", "Ploeg", "krap", kwantorvouw("atMost", 1, null, check("bevoegd", false),
                    "ten hoogste 1 van de wachten van de Ploeg bevoegd is")), "atMost"),
            List.of("paraat", kenmerkregel("paraatheid", "Ploeg", "paraat", kwantorvouw("exactly", 1, null,
                    check("bevoegd", false), "precies 1 van de wachten van de Ploeg bevoegd is")), "exactly"));

    static final String GEKWALIFICEERD = kenmerkregel("kwalificatie", "Ploeg", "gekwalificeerd", kwantorvouw("all", 0,
            check("bevoegd", false), vergelijking("greater", lezing("punten", "Wacht"), getal("5")),
            "de punten van alle wachten van de Ploeg die bevoegd zijn groter is dan 5"));

    /** "zijn opgave gedeeld door zijn deelnemers is groter dan 2", a new node on every call. */
    static String perDeelnemer() {
        return vergelijking("greater", TmsFilterfoutWitnessTest.quotient(lezing("opgave", "Wacht"),
                lezing("deelnemers", "Wacht")), getal("2"));
    }

    static final String RUSTIG = kenmerkregel("rustigheid", "Ploeg", "rustig", kwantorvouw("none", 0, null,
            perDeelnemer(), "geen van de wachten van de Ploeg aan de volgende voorwaarde voldoet: zijn opgave gedeeld door"
                    + " zijn deelnemers is groter dan 2"));
    static final String VOLLEDIG = kenmerkregel("volledigheid", "Ploeg", "volledig", kwantorvouw("all", 0, null,
            perDeelnemer(), "alle wachten van de Ploeg aan de volgende voorwaarde voldoen: zijn opgave gedeeld door zijn"
                    + " deelnemers is groter dan 2"));
    static final String KRAP_PER_DEELNEMER = kenmerkregel("krapte", "Ploeg", "krap", kwantorvouw("atMost", 1, null,
            perDeelnemer(), "ten hoogste 1 van de wachten van de Ploeg aan de volgende voorwaarde voldoet: zijn opgave"
                    + " gedeeld door zijn deelnemers is groter dan 2"));
    static final String BEZET_PER_DEELNEMER = kenmerkregel("bezetting", "Ploeg", "bezet", kwantorvouw("atLeast", 2,
            null, perDeelnemer(), "ten minste 2 van de wachten van de Ploeg aan de volgende voorwaarde voldoen: zijn"
                    + " opgave gedeeld door zijn deelnemers is groter dan 2"));
    static final String KEURBAAR = kenmerkregel("keurbaarheid", "Ploeg", "keurbaar", kwantorvouw("all", 0,
            perDeelnemer(), check("gediplomeerd", false), "alle wachten van de Ploeg waarvan de opgave gedeeld door de"
                    + " deelnemers groter is dan 2, gediplomeerd zijn"));

    static String wacht(String label, int dienstjaren, int klachten, int opgave) {
        return instantieMetRollen(label, "Wacht", Map.of("dienstjaren", "" + dienstjaren, "klachten", "" + klachten,
                "opgave", "" + opgave), List.of(), Map.of());
    }

    /** A Wacht with an opgave and, where not null, an aanmelding; gediplomeerd or not. */
    static String melder(String label, int opgave, Integer aanmelding, boolean gediplomeerd) {
        return instantieMetRollen(label, "Wacht", aanmelding == null ? Map.of("opgave", "" + opgave)
                        : Map.of("opgave", "" + opgave, "aanmelding", "" + aanmelding),
                gediplomeerd ? List.of("gediplomeerd") : List.of(), Map.of());
    }

    static String ploeg(String label, List<String> wachten) {
        return instantieMetRollen(label, "Ploeg", Map.of(), List.of(),
                wachten.isEmpty() ? Map.of() : Map.of("wachten", wachten));
    }

    static List<String> wereldA() {
        return List.of(ploeg("P1", List.of("W1", "W2")), ploeg("P2", List.of("W3", "W4")), ploeg("P3", List.of()),
                ploeg("P4", List.of("W5")), wacht("W1", 8, 0, 7), wacht("W2", 9, 3, 2), wacht("W3", 6, 1, 6),
                wacht("W4", 7, 0, 3), wacht("W5", 2, 5, 9));
    }

    static List<String> wereldB() {
        return List.of(ploeg("P1", List.of("W1", "W2", "W6")), ploeg("P2", List.of("W7")), wacht("W1", 8, 0, 7),
                wacht("W2", 9, 3, 2), wacht("W6", 10, 0, 8), wacht("W7", 9, 3, 2));
    }

    /** The verdict of each Ploeg in world A and in world B, per quantifier, in the order of {@link #KWANTOREN}. */
    static final Map<String, List<Boolean>> UITKOMST_A = Map.of("sterk", List.of(false, true, true, false),
            "zwak", List.of(false, false, true, true), "bezet", List.of(false, true, false, false),
            "krap", List.of(true, false, true, true), "paraat", List.of(true, false, false, false),
            "gekwalificeerd", List.of(true, false, true, true));
    static final Map<String, List<Boolean>> UITKOMST_B = Map.of("sterk", List.of(false, false),
            "zwak", List.of(false, true), "bezet", List.of(true, false), "krap", List.of(false, true),
            "paraat", List.of(false, false), "gekwalificeerd", List.of(true, true));

    static JsonNode zaak(List<String> wereld, String kenmerk, List<Boolean> uitkomst) throws Exception {
        List<String> verwachtingen = new ArrayList<>();
        for (int i = 0; i < uitkomst.size(); i++) verwachtingen.add(verwacht("P" + (i + 1), kenmerk, uitkomst.get(i)));
        verwachtingen.add(verwacht("W1", "bevoegd", true));
        verwachtingen.add(verwacht("W2", "geschorst", true));
        verwachtingen.add(verwacht("W2", "bevoegd", false));
        verwachtingen.add(consistent());
        return TmsDocumenten.zaak(wereld, verwachtingen);
    }

    @Test void elke_kwantor_over_een_kenmerk_dat_later_vervalt() throws Exception {
        List<String> uitkomsten = new ArrayList<>(), verwacht = new ArrayList<>();
        for (List<String> k : KWANTOREN) {
            String kenmerk = k.get(0);
            for (List<String> volgorde : TmsDocumenten.permutaties(List.of(SCHORSING, BEVOEGDHEID, k.get(1)))) {
                List<String> namen = TmsDocumenten.regelnamen(volgorde);
                Path ir = TmsDocumenten.document(MODEL, volgorde);
                for (JsonNode zaak : List.of(zaak(wereldA(), kenmerk, UITKOMST_A.get(kenmerk)),
                        zaak(wereldB(), kenmerk, UITKOMST_B.get(kenmerk)))) {
                    uitkomsten.add(k.get(2) + " " + namen + " "
                            + TmsFoutWitnessTest.speel(ir, zaak, DroolsBackend.Branch.TRUTH_MAINTENANCE));
                    verwacht.add(k.get(2) + " " + namen + " gespeeld");
                }
            }
        }
        assertEquals(verwacht, uitkomsten);
    }

    @Test void een_filter_en_een_matrix_over_later_afgeleide_waarden() throws Exception {
        List<String> uitkomsten = new ArrayList<>(), verwacht = new ArrayList<>();
        for (List<String> volgorde : TmsDocumenten.permutaties(List.of(SCHORSING, BEVOEGDHEID, PUNTEN, GEKWALIFICEERD))) {
            List<String> namen = TmsDocumenten.regelnamen(volgorde);
            Path ir = TmsDocumenten.document(MODEL, volgorde);
            for (JsonNode zaak : List.of(zaak(wereldA(), "gekwalificeerd", UITKOMST_A.get("gekwalificeerd")),
                    zaak(wereldB(), "gekwalificeerd", UITKOMST_B.get("gekwalificeerd")))) {
                uitkomsten.add(namen + " " + TmsFoutWitnessTest.speel(ir, zaak, DroolsBackend.Branch.TRUTH_MAINTENANCE));
                verwacht.add(namen + " gespeeld");
            }
        }
        assertEquals(verwacht, uitkomsten);
    }

    @Test void een_kenmerk_dat_onderweg_vervalt_telt_niet_in_elke_volgorde_van_de_gegenereerde_regels() throws Exception {
        for (List<String> k : KWANTOREN) {
            String kenmerk = k.get(0);
            DroolsBackend.Build b = DroolsBackend.build(TmsDocumenten.document(MODEL,
                    List.of(SCHORSING, BEVOEGDHEID, k.get(1))), DroolsBackend.Branch.TRUTH_MAINTENANCE);
            int onderweg = 0;
            for (List<Rule> volgorde : TmsDocumenten.steekproef(b.tmsTranspiler().model().getRules(), 20261009L, 30)) {
                List<String> namen = volgorde.stream().map(Rule::getName).toList();
                Object p1 = TmsDocumenten.boon(b.beanLoader(), "Ploeg", Map.of(), List.of());
                Object w1 = TmsDocumenten.boon(b.beanLoader(), "Wacht", Map.of("dienstjaren", "8", "klachten", "0"), List.of());
                Object w2 = TmsDocumenten.boon(b.beanLoader(), "Wacht", Map.of("dienstjaren", "9", "klachten", "3"), List.of());
                Object w6 = TmsDocumenten.boon(b.beanLoader(), "Wacht", Map.of("dienstjaren", "10", "klachten", "0"),
                        List.of());
                for (Object w : List.of(w1, w2, w6)) zetRol(w, "ploeg", p1);
                int[] bevoegd = {0};
                TmsDocumenten.vuur(b, volgorde, List.of(p1, w1, w2, w6), sessie -> sessie.addEventListener(
                        new DefaultRuleRuntimeEventListener() {
                            @Override public void objectInserted(ObjectInsertedEvent e) {
                                if (e.getObject() instanceof HeeftKenmerk h && h.onderwerp() == w2
                                        && h.kenmerk().naam().equals("bevoegd")) bevoegd[0]++;
                            }
                        }), sessie -> { });
                assertEquals(UITKOMST_B.get(kenmerk).get(0), TmsDocumenten.heeft(p1, kenmerk),
                        () -> kenmerk + " van P1 in volgorde " + namen);
                assertEquals(false, TmsDocumenten.heeft(w2, "bevoegd"), () -> "W2 in volgorde " + namen);
                if (bevoegd[0] > 0) onderweg++;
            }
            assertTrue(onderweg > 0, "geen volgorde liet W2 onderweg bevoegd worden onder " + k.get(2));
        }
    }

    private static void zetRol(Object boon, String rol, Object doel) throws Exception {
        for (java.lang.reflect.Method m : boon.getClass().getMethods())
            if (m.getName().equals(dev.uchar.regelspraak.transpiler.kern.Namen.setter(rol)) && m.getParameterCount() == 1) {
                m.invoke(boon, doel);
                return;
            }
        throw new AssertionError("geen setter voor " + rol + " op " + boon.getClass());
    }

    static final String A = melder("A", 10, 1, false), B = melder("B", 10, null, false),
            C = melder("C", 10, 0, false), D = melder("D", 10, 1, false);

    /** The fault of a division by zero in the condition of {@code regel}. */
    static String deling(String regel) {
        return "DelingDoorNul: deling door nul in de regel \"" + regel + "\": 10 gedeeld door 0 — een numerieke nul als"
                + " deler is een fout, geen leeg (RUNTIME-SEMANTICS ch. 1.4, punt 4)";
    }

    /** {@code regel} beside the derivation of the deelnemers, in both orders, over P1 with {@code leden} in that order
     *  (and the members' instances {@code instanties}), with the expectations {@code verwachtingen}; each outcome is
     *  {@code uitkomst}. */
    private static void speelBeide(String regel, List<String> leden, List<String> instanties, List<String> verwachtingen,
                                   String uitkomst, List<String> uitkomsten, List<String> verwacht) throws Exception {
        for (List<String> volgorde : TmsDocumenten.permutaties(List.of(DEELNEMERS, regel))) {
            List<String> namen = TmsDocumenten.regelnamen(volgorde);
            Path ir = TmsDocumenten.document(MODEL, volgorde);
            List<String> wereld = new ArrayList<>(List.of(ploeg("P1", leden)));
            wereld.addAll(instanties);
            JsonNode zaak = TmsDocumenten.zaak(wereld, verwachtingen);
            uitkomsten.add(namen + " " + leden + " " + TmsFoutWitnessTest.speel(ir, zaak,
                    DroolsBackend.Branch.TRUTH_MAINTENANCE));
            verwacht.add(namen + " " + leden + " " + uitkomst);
        }
    }

    @Test void de_matrix_deponeert_alleen_voor_een_lid_dat_de_kortsluiting_bereikt() throws Exception {
        List<String> uitkomsten = new ArrayList<>(), verwacht = new ArrayList<>();
        speelBeide(RUSTIG, List.of("A", "B"), List.of(A, B), List.of(verwacht("P1", "rustig", false), consistent()),
                "gespeeld", uitkomsten, verwacht);
        speelBeide(RUSTIG, List.of("B", "A"), List.of(A, B), List.of(verwacht("P1", "rustig", false),
                inconsistent("rustigheid", "P1")), "gespeeld", uitkomsten, verwacht);
        speelBeide(VOLLEDIG, List.of("A", "B"), List.of(A, B), List.of(verwacht("P1", "volledig", false),
                inconsistent("volledigheid", "P1")), "gespeeld", uitkomsten, verwacht);
        speelBeide(VOLLEDIG, List.of("B", "A"), List.of(A, B), List.of(verwacht("P1", "volledig", false),
                inconsistent("volledigheid", "P1")), "gespeeld", uitkomsten, verwacht);
        assertEquals(verwacht, uitkomsten);
    }

    @Test void de_matrix_faalt_alleen_voor_een_lid_dat_de_kortsluiting_bereikt() throws Exception {
        List<String> uitkomsten = new ArrayList<>(), verwacht = new ArrayList<>();
        speelBeide(RUSTIG, List.of("A", "C"), List.of(A, C), List.of(verwacht("P1", "rustig", false), consistent()),
                "gespeeld", uitkomsten, verwacht);
        speelBeide(RUSTIG, List.of("C", "A"), List.of(A, C), List.of(consistent()), deling("rustigheid"), uitkomsten,
                verwacht);
        speelBeide(VOLLEDIG, List.of("A", "C"), List.of(A, C), List.of(consistent()), deling("volledigheid"), uitkomsten,
                verwacht);
        speelBeide(VOLLEDIG, List.of("C", "A"), List.of(A, C), List.of(consistent()), deling("volledigheid"), uitkomsten,
                verwacht);
        // the empty prefix (ch. 5.10): at most 1 over one contributor cannot be exceeded, at least 2 is out of reach
        speelBeide(KRAP_PER_DEELNEMER, List.of("C"), List.of(C), List.of(verwacht("P1", "krap", true), consistent()),
                "gespeeld", uitkomsten, verwacht);
        speelBeide(BEZET_PER_DEELNEMER, List.of("C"), List.of(C), List.of(verwacht("P1", "bezet", false), consistent()),
                "gespeeld", uitkomsten, verwacht);
        speelBeide(KRAP_PER_DEELNEMER, List.of("B"), List.of(B), List.of(verwacht("P1", "krap", true), consistent()),
                "gespeeld", uitkomsten, verwacht);
        assertEquals(verwacht, uitkomsten);
    }

    @Test void het_filter_van_elk_lid_wordt_gelezen_voor_de_matrix() throws Exception {
        List<String> uitkomsten = new ArrayList<>(), verwacht = new ArrayList<>();
        for (List<String> leden : List.of(List.of("D", "B"), List.of("B", "D")))
            speelBeide(KEURBAAR, leden, List.of(D, B), List.of(verwacht("P1", "keurbaar", false),
                    inconsistent("keurbaarheid", "P1")), "gespeeld", uitkomsten, verwacht);
        // without B the filter deposits nothing, and D alone decides
        speelBeide(KEURBAAR, List.of("D"), List.of(D), List.of(verwacht("P1", "keurbaar", false), consistent()),
                "gespeeld", uitkomsten, verwacht);
        assertEquals(verwacht, uitkomsten);
    }
}
