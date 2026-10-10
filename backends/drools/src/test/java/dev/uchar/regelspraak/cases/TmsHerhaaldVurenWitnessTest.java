package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import dev.uchar.regelspraak.transpiler.kern.Namen;
import dev.uchar.regelspraak.transpiler.tms.Fixpunt;
import dev.uchar.regelspraak.transpiler.tms.Fout;

import org.junit.jupiter.api.Test;
import org.kie.api.runtime.KieSession;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocumenten.GEHEEL;
import static dev.uchar.regelspraak.cases.TmsDocumenten.aggregatie;
import static dev.uchar.regelspraak.cases.TmsDocumenten.attribuut;
import static dev.uchar.regelspraak.cases.TmsDocumenten.attribuutgroep;
import static dev.uchar.regelspraak.cases.TmsDocumenten.binair;
import static dev.uchar.regelspraak.cases.TmsDocumenten.bronrol;
import static dev.uchar.regelspraak.cases.TmsDocumenten.check;
import static dev.uchar.regelspraak.cases.TmsDocumenten.feittypeEenNaarVeel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.getal;
import static dev.uchar.regelspraak.cases.TmsDocumenten.kenmerkregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.lezing;
import static dev.uchar.regelspraak.cases.TmsDocumenten.objecttype;
import static dev.uchar.regelspraak.cases.TmsDocumenten.rekenregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.standaard;
import static dev.uchar.regelspraak.cases.TmsDocumenten.vergelijking;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Two fires in one session of the truth-maintenance branch, through the entry a consumer has ({@link DroolsBackend#voegIn},
 * {@link DroolsBackend#vuur}, {@link DroolsBackend#lees}), with an input change between them (card tms-omschakeling,
 * points 4 and 11). The beans hold the input alone between the fires, so a derivation whose reads change lapses, a
 * fault arises or lapses, and an initialisation fires again, and the second reading equals that of a fresh session of
 * the new input.
 *
 * <p>The values document: a Jutter J1 with Vondsten V1 (gewicht 10) and V2 (gewicht 4), "De uitkering van een Vondst
 * moet berekend worden als zijn gewicht plus 10", "Een Vondst is zwaar indien zijn gewicht groter is dan 5", "De bonus
 * van een Vondst moet gesteld worden op 7 indien hij zwaar is", "Het totaalgewicht van een Jutter moet berekend worden
 * als de som van de gewichten van zijn vondsten", "Het vindloon van een Vondst moet geïnitialiseerd worden op zijn
 * gewicht plus 5", and a defaults group that gives the heffing 40 where the Vondst is zwaar. At the first fixpoint: V1
 * uitkering 20, zwaar, bonus 7, vindloon 15, heffing 40; V2 uitkering 14, vindloon 9; J1 totaalgewicht 14. Then V1's
 * gewicht becomes 3 ({@code update}): uitkering 13, not zwaar, no bonus and no heffing, vindloon 8, J1 totaalgewicht 7,
 * the values of a fresh session. The faults document: "Het aandeel van een Vondst moet berekend worden als 100 gedeeld
 * door zijn noemer", V1 noemer 0 and V2 noemer 5, then V1 noemer 4 and V2 noemer 0: the fault on V1 lapses and V1 has
 * aandeel 25, and a fault on V2 arises. The verifier passes both documents, and the reference interpreter and the ASP
 * runner give these values on both inputs of the values document, and on the faults document "division by numeric
 * zero" for a Vondst of noemer 0 and aandeel 20 and 25 for noemer 5 and 4, one Vondst per case, since there a fault
 * ends the evaluation (measured 2026-10-10).
 *
 * <p>After the write-back ({@link DroolsBackend#zetAfgeleideWaarden}) the beans hold derived values beside their input,
 * and {@link DroolsBackend#vuur} refuses the session with the sentence that says what to do instead.
 *
 * <p>A session of a model with a creation fires once (the fix round of step 3, point 1). On golden
 * {@code anchor/jutterswet-rekenkamer}, case 1, the object creation makes a Rekenkamer whose grondslag and aftrek it
 * initialises from J1's, 100 and 30, and J1's heffing is 70. Then J1's grondslag becomes 150 ({@code update}), and
 * {@code vuur} refuses the session, with the sentence that what a creation made is stated and that the way is a fresh
 * session per input. A fresh session of grondslag 150 gives heffing 120, as the reference interpreter and the ASP runner
 * give it on that case (measured 2026-10-10). <b>Red before the change</b> (measured 2026-10-10 on {@code f4b83d7b}):
 * the second {@code vuur} raised nothing, "Expected java.lang.IllegalStateException to be thrown, but nothing was
 * thrown.", and the old form of the row, fire, change, fire and read, gave the state of the first input: "expected:
 * &lt;[…, J1.heffing = 120, gemaakt: richt de rekenkamer op voor J1, gemaakte Rekenkamer.heffing = 120]&gt; but was:
 * &lt;[…, J1.heffing = 70, gemaakt: richt de rekenkamer op voor J1, gemaakte Rekenkamer.heffing = 70]&gt;", since the
 * made Rekenkamer kept the grondslag it was initialised with.
 *
 * <p><b>Red before the change</b> (measured 2026-10-10 on {@code 095a586a}, the old form of the witness, in which a
 * consumer could only fire, write back and read the beans): after the second fire and write-back V1 still carried
 * zwaar, bonus 7, heffing 40 and vindloon 15, "second reading ==&gt; expected: &lt;[J1.totaalgewicht = 7, V1.uitkering =
 * 13, V1.vindloon = 8, V2.uitkering = 14, V2.vindloon = 9]&gt; but was: &lt;[J1.totaalgewicht = 7, V1 is zwaar, V1.bonus
 * = 7, V1.heffing = 40, V1.uitkering = 13, V1.vindloon = 15, V2.uitkering = 14, V2.vindloon = 9]&gt;", and the fire
 * after the write-back raised nothing, "Expected java.lang.IllegalStateException to be thrown, but nothing was thrown." The witness itself does not compile there: the entry had no {@code voegIn}, no public {@code vuur} and no
 * {@code lees}.
 */
class TmsHerhaaldVurenWitnessTest {

    static final String MODEL = TmsDocumenten.model(List.of(
            objecttype("Jutter", List.of(attribuut("totaalgewicht", GEHEEL)), List.of()),
            objecttype("Vondst", List.of(attribuut("gewicht", GEHEEL), attribuut("uitkering", GEHEEL),
                    attribuut("bonus", GEHEEL), attribuut("vindloon", GEHEEL), attribuut("heffing", GEHEEL),
                    attribuut("noemer", GEHEEL), attribuut("aandeel", TmsFilterfoutWitnessTest.BREUK)), List.of("zwaar"))),
            List.of(feittypeEenNaarVeel("vinding", "vinder", "Jutter", "vondst", "Vondst", "vondsten")));

    static final List<String> WAARDEREGELS = List.of(
            rekenregel("uitkering", "BEREKENING", "Vondst", "uitkering",
                    binair("plus", lezing("gewicht", "Vondst"), getal("10")), "null"),
            kenmerkregel("zwaarte", "Vondst", "zwaar", vergelijking("greater", lezing("gewicht", "Vondst"), getal("5"))),
            rekenregel("bonus", "GELIJKSTELLING", "Vondst", "bonus", getal("7"), check("zwaar", false)),
            rekenregel("totaalgewicht", "BEREKENING", "Jutter", "totaalgewicht",
                    aggregatie("sum", lezing("gewicht", "Vondst"), "Vondst", bronrol("vinding", "vondst"), "null",
                            "null", GEHEEL, "de som van de gewichten van zijn vondsten"), "null"),
            rekenregel("standaardvindloon", "INITIALISATIE", "Vondst", "vindloon",
                    binair("plus", lezing("gewicht", "Vondst"), getal("5")), "null"));

    static final List<String> GROEPEN = List.of(attribuutgroep("heffing van een vondst", "Vondst", "heffing",
            List.of(standaard(check("zwaar", false), getal("40")))));

    static final List<String> FOUTREGELS = List.of(rekenregel("aandeel", "BEREKENING", "Vondst", "aandeel",
            TmsFilterfoutWitnessTest.quotient(getal("100"), lezing("noemer", "Vondst")), "null"));

    static Path waardedocument() throws Exception {
        return TmsDocumenten.document(MODEL, WAARDEREGELS, List.of(), GROEPEN);
    }

    static Path foutdocument() throws Exception {
        return TmsDocumenten.document(MODEL, FOUTREGELS);
    }

    static final List<String> EERSTE_WAARDEN = List.of("J1.totaalgewicht = 14", "V1 is zwaar", "V1.bonus = 7",
            "V1.heffing = 40", "V1.uitkering = 20", "V1.vindloon = 15", "V2.uitkering = 14", "V2.vindloon = 9");
    static final List<String> TWEEDE_WAARDEN = List.of("J1.totaalgewicht = 7", "V1.uitkering = 13", "V1.vindloon = 8",
            "V2.uitkering = 14", "V2.vindloon = 9");

    private static final String NUL = "gedeeld door 0 — een numerieke nul als deler is een fout, geen leeg"
            + " (RUNTIME-SEMANTICS ch. 1.4, punt 4)";

    @Test void een_afleiding_vervalt_en_een_initialisatie_vuurt_opnieuw_tussen_twee_fires() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(waardedocument(), DroolsBackend.Branch.TRUTH_MAINTENANCE);
        inSessie(b, () -> {
            Map<String, Object> bonen = waardebonen(b, "10");
            KieSession sessie = b.kieBase().newKieSession();
            try {
                DroolsBackend.voegIn(b, sessie, new Invoer().bonen(bonen.values()));
                assertNull(DroolsBackend.vuur(b, sessie).geenFixpunt(), "first fire");
                assertEquals(EERSTE_WAARDEN, beschrijf(DroolsBackend.lees(b, sessie), bonen), "first reading");
                Object v1 = bonen.get("V1");
                // between the fires the beans hold the input alone
                assertNull(TmsDocumenten.lees(v1, "uitkering"), "V1 uitkering on the bean between the fires");
                assertNull(TmsDocumenten.lees(v1, "vindloon"), "V1 vindloon on the bean between the fires");
                assertFalse(TmsDocumenten.heeft(v1, "zwaar"), "V1 zwaar on the bean between the fires");
                zet(v1, "gewicht", "3");
                sessie.update(sessie.getFactHandle(v1), v1);
                assertNull(DroolsBackend.vuur(b, sessie).geenFixpunt(), "second fire");
                List<String> tweede = beschrijf(DroolsBackend.lees(b, sessie), bonen);
                assertEquals(TWEEDE_WAARDEN, tweede, "second reading: the characteristic, the conditional value and the"
                        + " default lapse, the sum follows, and the initialisation fires again, since its target stayed"
                        + " empty on the bean");
                assertEquals(verseLezing(b, bonen), tweede, "the second reading is that of a fresh session");
            } finally {
                sessie.dispose();
            }
        });
    }

    @Test void een_fout_vervalt_en_een_fout_ontstaat_tussen_twee_fires() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(foutdocument(), DroolsBackend.Branch.TRUTH_MAINTENANCE);
        inSessie(b, () -> {
            Map<String, Object> bonen = new LinkedHashMap<>();
            bonen.put("V1", TmsDocumenten.boon(b.beanLoader(), "Vondst", Map.of("noemer", "0"), List.of()));
            bonen.put("V2", TmsDocumenten.boon(b.beanLoader(), "Vondst", Map.of("noemer", "5"), List.of()));
            KieSession sessie = b.kieBase().newKieSession();
            try {
                DroolsBackend.voegIn(b, sessie, new Invoer().bonen(bonen.values()));
                assertNull(DroolsBackend.vuur(b, sessie).geenFixpunt(), "first fire");
                assertEquals(List.of("V1: deling door nul in de regel \"aandeel\": 100 " + NUL, "V2.aandeel = 20"),
                        beschrijf(DroolsBackend.lees(b, sessie), bonen), "first reading");
                zet(bonen.get("V1"), "noemer", "4");
                zet(bonen.get("V2"), "noemer", "0");
                for (Object v : bonen.values()) sessie.update(sessie.getFactHandle(v), v);
                assertNull(DroolsBackend.vuur(b, sessie).geenFixpunt(), "second fire");
                List<String> tweede = beschrijf(DroolsBackend.lees(b, sessie), bonen);
                assertEquals(List.of("V1.aandeel = 25", "V2: deling door nul in de regel \"aandeel\": 100 " + NUL), tweede,
                        "second reading: the fault on V1 lapsed, the one on V2 arose");
                assertEquals(verseLezing(b, bonen), tweede, "the second reading is that of a fresh session");
            } finally {
                sessie.dispose();
            }
        });
    }

    @Test void na_het_terugzetten_weigert_vuur_de_sessie() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(waardedocument(), DroolsBackend.Branch.TRUTH_MAINTENANCE);
        inSessie(b, () -> {
            Map<String, Object> bonen = waardebonen(b, "10");
            KieSession sessie = b.kieBase().newKieSession();
            try {
                DroolsBackend.voegIn(b, sessie, new Invoer().bonen(bonen.values()));
                assertNull(DroolsBackend.vuur(b, sessie).geenFixpunt(), "first fire");
                DroolsBackend.zetAfgeleideWaarden(b, sessie);
                assertGetal("15", TmsDocumenten.lees(bonen.get("V1"), "vindloon"), "V1 vindloon written back");
                IllegalStateException e = assertThrows(IllegalStateException.class, () -> DroolsBackend.vuur(b, sessie));
                assertEquals("deze sessie is teruggezet (zetAfgeleideWaarden): haar bonen dragen afgeleide waarden naast"
                        + " de invoer, en een volgende fire zou die als invoer lezen; lees tussen twee fires met"
                        + " DroolsBackend.lees en zet pas na de laatste fire terug, of open een verse sessie met bonen die"
                        + " alleen invoer dragen", e.getMessage());
                assertEquals(EERSTE_WAARDEN, beschrijf(DroolsBackend.lees(b, sessie), bonen),
                        "the reading after the write-back is the one it wrote");
            } finally {
                sessie.dispose();
            }
        });
    }

    @Test void een_model_met_creatie_weigert_een_tweede_vuur() throws Exception {
        Path ir = Path.of("..", "..", "corpus", "golden", "anchor", "jutterswet-rekenkamer", "ir.json");
        String zaak = Files.readString(ir.resolveSibling("case1-berekend.json"));
        CaseRunner runner = CaseRunner.voorMeting(ir, DroolsBackend.Branch.TRUTH_MAINTENANCE);
        runner.metSessie(TmsDocumenten.JSON.readTree(zaak), (bouw, sessie, labels) -> {
            Object j1 = labels.get("J1");
            assertNull(DroolsBackend.vuur(bouw, sessie).geenFixpunt(), "first fire");
            assertEquals("70", heffing(DroolsBackend.lees(bouw, sessie), j1), "J1 heffing, first fire");
            try {
                zet(j1, "grondslag", "150");
            } catch (Exception e) {
                throw new AssertionError(e);
            }
            sessie.update(sessie.getFactHandle(j1), j1);
            IllegalStateException e = assertThrows(IllegalStateException.class, () -> DroolsBackend.vuur(bouw, sessie));
            assertEquals("dit model heeft creatieregels, en deze sessie is al via DroolsBackend.vuur gevuurd: wat een"
                    + " creatie maakte, met de initialisaties, de koppelingen en de fout van een mislukte initialisatie,"
                    + " is gesteld en vervalt niet (RUNTIME-SEMANTICS ch. 5.5), dus een volgende fire zou na een"
                    + " invoerwijziging een toestand geven die geen verse evaluatie geeft; open per invoer een verse"
                    + " sessie", e.getMessage());
            return null;
        });
        // the way the sentence names: a fresh session of the changed input
        JsonNode veranderd = TmsDocumenten.JSON.readTree(zaak.replace("\"100\"", "\"150\""));
        runner.metSessie(veranderd, (bouw, sessie, labels) -> {
            assertNull(DroolsBackend.vuur(bouw, sessie).geenFixpunt(), "fresh fire");
            assertEquals("120", heffing(DroolsBackend.lees(bouw, sessie), labels.get("J1")),
                    "J1 heffing in a fresh session of grondslag 150");
            return null;
        });
    }

    // ---------------- helpers ----------------

    /** The heffing {@code f} derives for {@code jutter}, as a plain number, or null. */
    private static String heffing(Fixpunt f, Object jutter) {
        for (Fixpunt.AfgeleideWaarde w : f.waarden())
            if (w.object() == jutter && w.attribuut().equals("heffing"))
                return ((BigDecimal) w.waarde()).stripTrailingZeros().toPlainString();
        return null;
    }

    /** J1, and V1 of {@code gewichtV1} and V2 of 4, each found by J1. */
    static Map<String, Object> waardebonen(DroolsBackend.Build b, String gewichtV1) throws Exception {
        Map<String, Object> bonen = new LinkedHashMap<>();
        Object j1 = TmsDocumenten.boon(b.beanLoader(), "Jutter", Map.of(), List.of());
        bonen.put("J1", j1);
        bonen.put("V1", TmsDocumenten.boon(b.beanLoader(), "Vondst", Map.of("gewicht", gewichtV1), List.of()));
        bonen.put("V2", TmsDocumenten.boon(b.beanLoader(), "Vondst", Map.of("gewicht", "4"), List.of()));
        for (String v : List.of("V1", "V2")) zetRol(bonen.get(v), "vinder", j1);
        return bonen;
    }

    /** The reading of a fresh session of {@code b} over the same beans, which hold the input alone. */
    private static List<String> verseLezing(DroolsBackend.Build b, Map<String, Object> bonen) {
        KieSession vers = b.kieBase().newKieSession();
        try {
            DroolsBackend.voegIn(b, vers, new Invoer().bonen(bonen.values()));
            assertNull(DroolsBackend.vuur(b, vers).geenFixpunt(), "fresh fire");
            return beschrijf(DroolsBackend.lees(b, vers), bonen);
        } finally {
            vers.dispose();
        }
    }

    /** {@code f} as sorted lines over the labels of {@code bonen}: "V1.uitkering = 20", "V1 is zwaar", "V1: &lt;fault&gt;",
     *  "gemaakt: …", "koppeling: …" and "inconsistent: …". */
    static List<String> beschrijf(Fixpunt f, Map<String, Object> bonen) {
        Map<Object, String> label = new IdentityHashMap<>();
        bonen.forEach((l, boon) -> label.put(boon, l));
        List<String> uit = new ArrayList<>();
        for (Fixpunt.AfgeleideWaarde w : f.waarden())
            uit.add(label.get(w.object()) + "." + w.attribuut() + " = "
                    + (w.waarde() instanceof BigDecimal d ? d.stripTrailingZeros().toPlainString() : w.waarde()));
        for (Fixpunt.AfgeleidKenmerk k : f.kenmerken()) uit.add(label.get(k.object()) + " is " + k.kenmerk());
        for (Fixpunt.GemaaktObject o : f.objecten())
            uit.add("gemaakt: " + o.creatie() + " voor " + label.get(o.onderwerp()));
        for (Fixpunt.GemaakteKoppeling k : f.koppelingen())
            uit.add("koppeling: " + k.feittype() + " " + label.get(k.eerste()) + " " + label.get(k.tweede()));
        for (Fout fout : f.fouten()) uit.add(label.get(fout.onderwerp()) + ": " + fout.melding());
        for (Object i : f.inconsistenties()) uit.add("inconsistent: " + i);
        uit.sort(null);
        return uit;
    }

    static void zet(Object boon, String attribuut, String lexeem) throws Exception {
        boon.getClass().getMethod(Namen.setter(attribuut), BigDecimal.class).invoke(boon, new BigDecimal(lexeem));
    }

    static void zetRol(Object boon, String rol, Object doel) throws Exception {
        for (java.lang.reflect.Method m : boon.getClass().getMethods())
            if (m.getName().equals(Namen.setter(rol)) && m.getParameterCount() == 1) {
                m.invoke(boon, doel);
                return;
            }
        throw new AssertionError("geen setter voor " + rol + " op " + boon.getClass());
    }

    /** Work that may throw, run with the bean loader of the build as the thread's context class loader. */
    interface Werk {
        void doe() throws Exception;
    }

    static void inSessie(DroolsBackend.Build b, Werk werk) throws Exception {
        ClassLoader vorige = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(b.beanLoader());
        try {
            werk.doe();
        } finally {
            Thread.currentThread().setContextClassLoader(vorige);
        }
    }

    private static void assertGetal(String verwacht, Object werkelijk, String wat) {
        assertTrue(werkelijk instanceof BigDecimal w && new BigDecimal(verwacht).compareTo(w) == 0,
                () -> wat + ": verwacht " + verwacht + ", werkelijk " + werkelijk);
    }
}
