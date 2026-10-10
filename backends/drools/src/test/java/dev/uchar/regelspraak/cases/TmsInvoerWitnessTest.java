package dev.uchar.regelspraak.cases;

import dev.uchar.regelspraak.transpiler.tms.Fixpunt;
import dev.uchar.regelspraak.transpiler.tms.Fout;

import org.junit.jupiter.api.Test;
import org.kie.api.runtime.KieSession;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The input of a session through {@link DroolsBackend#voegIn} (card tms-omschakeling, point 12): the beans, and what the
 * truth-maintenance branch reads beside them, named by the consumer in the words of the model ({@link Invoer}), so that
 * no consumer states a fact class of that branch.
 *
 * <ul>
 *   <li>The order of a to-many list decides which member a fault names where a fold reads its members one by one and
 *       stops at the first fault (RUNTIME-SEMANTICS ch. 5.11). On the document of {@link TmsFilterfoutWitnessTest} the
 *       filter of the fold faults for both Vondsten of J1, V1 dividing 9 and V2 4 by their deelnemers 0: in the order
 *       of the beans V1, V2 the fault names 9, in the order V2, V1 it names 4, and a list given as V2, V1 names 4 with
 *       the beans in the order V1, V2.</li>
 *   <li>A given list that misses a member whose reference field holds the holder leaves that member without a place:
 *       {@code voegIn} refuses it by name, and inserts nothing, where the fire would have stopped with an
 *       {@code IllegalStateException} at the first rule that needs the place. So does every other statement that does
 *       not fit the beans or the model, and a session that is not fresh.</li>
 *   <li>A bucket scalar ({@link Invoer#emmerscalair}): on the document of {@link TmsInvoervormWitnessTest} part C, the
 *       subject's own total without a clip of J3, whose bedrag the bean holds as the one segment of June 2024, is 7 with
 *       the statement and faults without it, since the branch then reads the segment as a full timeline (ch. 8.4).</li>
 *   <li>On the exec-model and the DRL-text branch it inserts the beans alone.</li>
 * </ul>
 *
 * <p>The values of the fold order and of the bucket scalar are those the replay pins on the same documents
 * ({@code TmsFilterfoutWitnessTest}, {@code TmsInvoervormWitnessTest}), there confirmed against the reference
 * interpreter.
 *
 * <p><b>Red before the change</b> (measured 2026-10-10 on {@code 095a586a}): the witness does not compile there, since
 * the entry had no {@code voegIn} and no {@code Invoer}; a consumer stated {@code Emmerinvoer} and {@code Lijstplaats}
 * itself, or the fire stopped at the first rule that needed a place. <b>Red by mutation</b> (measured the same day, not
 * committed): without the check that a given list names every member, the row of the forgotten member failed on
 * "Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown."
 */
class TmsInvoerWitnessTest {

    private static final String NUL = " gedeeld door 0 — een numerieke nul als deler is een fout, geen leeg"
            + " (RUNTIME-SEMANTICS ch. 1.4, punt 4)";

    @Test void een_lijst_heeft_standaard_de_volgorde_van_de_bonen() throws Exception {
        DroolsBackend.Build b = vondstbouw();
        TmsHerhaaldVurenWitnessTest.inSessie(b, () -> {
            List<Object> jv = jutterMetVondsten(b);
            Object j1 = jv.get(0), v1 = jv.get(1), v2 = jv.get(2);
            assertEquals(List.of("deling door nul in de regel \"zware vondsten\": 9" + NUL),
                    fouten(b, new Invoer().bonen(List.of(j1, v1, v2))), "V1 before V2");
            assertEquals(List.of("deling door nul in de regel \"zware vondsten\": 4" + NUL),
                    fouten(b, new Invoer().bonen(List.of(j1, v2, v1))), "V2 before V1");
        });
    }

    @Test void een_gegeven_lijst_legt_de_volgorde_vast() throws Exception {
        DroolsBackend.Build b = vondstbouw();
        TmsHerhaaldVurenWitnessTest.inSessie(b, () -> {
            List<Object> jv = jutterMetVondsten(b);
            Object j1 = jv.get(0), v1 = jv.get(1), v2 = jv.get(2);
            assertEquals(List.of("deling door nul in de regel \"zware vondsten\": 4" + NUL),
                    fouten(b, new Invoer().bonen(List.of(j1, v1, v2)).lijst(j1, "aanmelder", List.of(v2, v1))),
                    "the list V2, V1 with the beans in the order V1, V2");
        });
    }

    @Test void een_vergeten_lid_in_een_gegeven_lijst_weigert_voegIn_luid() throws Exception {
        DroolsBackend.Build b = vondstbouw();
        TmsHerhaaldVurenWitnessTest.inSessie(b, () -> {
            List<Object> jv = jutterMetVondsten(b);
            Object j1 = jv.get(0), v1 = jv.get(1), v2 = jv.get(2);
            KieSession sessie = b.kieBase().newKieSession();
            try {
                IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> DroolsBackend.voegIn(b,
                        sessie, new Invoer().bonen(List.of(j1, v1, v2)).lijst(j1, "aanmelder", List.of(v1))));
                assertEquals("voegIn: de lijst van " + naam(j1) + " onder de rol 'aanmelder' mist " + naam(v2) + ", dat"
                        + " met zijn veld 'aanmelder' naar de houder wijst: een lid buiten de gegeven volgorde heeft geen"
                        + " plaats", e.getMessage());
                assertEquals(0, sessie.getFactCount(), "nothing was inserted");
            } finally {
                sessie.dispose();
            }
        });
    }

    @Test void wat_niet_bij_de_bonen_of_het_model_past_weigert_voegIn_luid() throws Exception {
        DroolsBackend.Build b = vondstbouw();
        TmsHerhaaldVurenWitnessTest.inSessie(b, () -> {
            List<Object> jv = jutterMetVondsten(b);
            Object j1 = jv.get(0), v1 = jv.get(1), v2 = jv.get(2);
            Object j2 = TmsDocumenten.boon(b.beanLoader(), "Jutter", Map.of(), List.of());
            Object v3 = TmsDocumenten.boon(b.beanLoader(), "Vondst", Map.of(), List.of());
            TmsHerhaaldVurenWitnessTest.zetRol(v3, "aanmelder", j2);
            List<Object> bonen = List.of(j1, v1, v2);
            String lijst = "voegIn: de lijst van " + naam(j1) + " onder de rol 'aanmelder'";
            List<String> zinnen = new ArrayList<>();
            zinnen.add(weigering(b, new Invoer().bonen(bonen).lijst(j1, "aanmelder", List.of(v1, v2, v3))));
            zinnen.add(weigering(b, new Invoer().bonen(List.of(j1, v1, v2, j2, v3))
                    .lijst(j1, "aanmelder", List.of(v1, v2, v3))));
            zinnen.add(weigering(b, new Invoer().bonen(bonen).lijst(j1, "aanmelder", List.of(v1, v2, v1))));
            zinnen.add(weigering(b, new Invoer().bonen(bonen).lijst(j1, "vondst", List.of(v1, v2))));
            zinnen.add(weigering(b, new Invoer().bonen(bonen).lijst(j1, "aanmelder", List.of(v1, v2))
                    .lijst(j1, "aanmelder", List.of(v1, v2))));
            zinnen.add(weigering(b, new Invoer().bonen(bonen).lijst(j1, "aanmelder", List.of())));
            zinnen.add(weigering(b, new Invoer().bonen(bonen).emmerscalair(v3, "gewicht")));
            zinnen.add(weigering(b, new Invoer().bonen(bonen).emmerscalair(v1, "gewicht")));
            zinnen.add(weigering(b, new Invoer().bonen(bonen).emmerscalair(v1, "lengte")));
            zinnen.add(weigering(b, new Invoer().bonen(List.of(j1, v1, v1))));
            // a bean model of another document: the bean compiler shares the classes of identical models only
            DroolsBackend.Build ander = DroolsBackend.build(TmsHerhaaldVurenWitnessTest.waardedocument(),
                    DroolsBackend.Branch.TRUTH_MAINTENANCE);
            Object vreemd = TmsDocumenten.boon(ander.beanLoader(), "Vondst", Map.of(), List.of());
            zinnen.add(weigering(b, new Invoer().bonen(List.of(j1, vreemd))));
            assertEquals(List.of(
                    lijst + " noemt " + naam(v3) + ", dat geen boon van de invoer is",
                    lijst + " noemt " + naam(v3) + ", maar diens veld 'aanmelder' wijst naar " + naam(j2),
                    lijst + " noemt " + naam(v1) + " twee keer",
                    "voegIn: de lijst van " + naam(j1) + " onder de rol 'vondst' noemt " + naam(v1) + ", maar een Vondst"
                            + " heeft geen referentieveld 'vondst'; de rol is die van de houder, de naam van het veld"
                            + " waarmee een lid naar de houder wijst",
                    lijst + " is twee keer gegeven",
                    lijst + " is leeg gegeven, maar " + naam(v1) + " wijst met zijn veld 'aanmelder' naar de houder",
                    "voegIn: emmerscalair 'gewicht' op " + naam(v3) + ", een boon die niet in de invoer staat",
                    "voegIn: emmerscalair 'gewicht' op " + naam(v1) + ", maar dat attribuut van Vondst ligt niet op een"
                            + " tijdlijn; alleen de invoer van een tijdlijn kent de emmerconventie (RUNTIME-SEMANTICS ch."
                            + " 8.4)",
                    "voegIn: emmerscalair 'lengte' op " + naam(v1) + ", maar Vondst heeft geen attribuut van die naam",
                    "voegIn: de boon " + naam(v1) + " staat twee keer in de invoer",
                    "voegIn: " + naam(vreemd) + " is geen boon van dit model: een boon is een instantie van een klasse die"
                            + " Build.beanLoader() laadt"), zinnen);
            KieSession sessie = b.kieBase().newKieSession();
            try {
                DroolsBackend.voegIn(b, sessie, new Invoer().bonen(bonen));
                IllegalStateException e = assertThrows(IllegalStateException.class,
                        () -> DroolsBackend.voegIn(b, sessie, new Invoer().boon(j2)));
                assertEquals("voegIn vult een verse sessie, maar deze houdt al " + sessie.getFactCount() + " feiten: de"
                        + " plaatsen van een lijst worden over de bonen van één invoer geteld, en een boon die zonder"
                        + " voegIn in de sessie kwam, heeft er geen; geef alle bonen in één Invoer aan een verse sessie,"
                        + " en verander tussen twee fires hun waarden met update", e.getMessage());
            } finally {
                sessie.dispose();
            }
        });
    }

    @Test void een_emmerscalair_leest_de_invoer_als_emmer_en_anders_als_volledige_tijdlijn() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(TmsTijdlijnDocumenten.document(TmsInvoervormWitnessTest.MODEL_C,
                TmsInvoervormWitnessTest.REGELS_C, List.of()), DroolsBackend.Branch.TRUTH_MAINTENANCE);
        TmsHerhaaldVurenWitnessTest.inSessie(b, () -> {
            Object j3 = TmsDocumenten.boon(b.beanLoader(), "Jutter", Map.of("score", "10"), List.of());
            TmsTijdlijnDocumenten.zet(j3, "bedrag", List.of(
                    TmsTijdlijnDocumenten.boonsegment(b.beanLoader(), "2024-06-01", "2024-07-01", "7")));
            Fixpunt emmer = lees(b, new Invoer().boon(j3).emmerscalair(j3, "bedrag"));
            assertEquals(List.of(), emmer.fouten(), "no fault over the bucket scalar");
            assertTrue(emmer.waarden().stream().anyMatch(w -> w.object() == j3 && w.attribuut().equals("topsom")
                    && w.waarde() instanceof BigDecimal d && d.compareTo(new BigDecimal("7")) == 0),
                    () -> "J3 topsom 7: " + emmer.waarden());
            Fixpunt volledig = lees(b, new Invoer().boon(j3));
            assertEquals(1, volledig.fouten().size(), () -> "one fault over a full timeline: " + volledig.fouten());
            assertTrue(volledig.fouten().get(0).melding().startsWith("fout in de regel \"topsom\": the temporal total of"
                    + " \"bedrag\" over a case-supplied full timeline is not exercised by any corpus case"),
                    volledig.fouten().get(0).melding());
        });
    }

    @Test void op_de_andere_takken_voegt_voegIn_alleen_de_bonen_in() throws Exception {
        for (DroolsBackend.Branch tak : List.of(DroolsBackend.Branch.EXEC_MODEL, DroolsBackend.Branch.DRL_TEXT)) {
            DroolsBackend.Build b = DroolsBackend.build(TmsFilterfoutWitnessTest.document(TmsFilterfoutWitnessTest.ZWARE),
                    tak);
            TmsHerhaaldVurenWitnessTest.inSessie(b, () -> {
                List<Object> jv = jutterMetVondsten(b);
                KieSession sessie = b.kieBase().newKieSession();
                try {
                    DroolsBackend.voegIn(b, sessie, new Invoer().bonen(jv).lijst(jv.get(0), "aanmelder",
                            List.of(jv.get(2), jv.get(1))).emmerscalair(jv.get(1), "gewicht"));
                    assertEquals(jv.size(), sessie.getFactCount(), tak + ": the beans alone");
                } finally {
                    sessie.dispose();
                }
            });
        }
    }

    // ---------------- helpers ----------------

    private static DroolsBackend.Build vondstbouw() throws Exception {
        return DroolsBackend.build(TmsFilterfoutWitnessTest.document(TmsFilterfoutWitnessTest.ZWARE),
                DroolsBackend.Branch.TRUTH_MAINTENANCE);
    }

    /** J1 actief, V1 (gewicht 9, deelnemers 0) and V2 (gewicht 4, deelnemers 0), both reported by J1. */
    private static List<Object> jutterMetVondsten(DroolsBackend.Build b) throws Exception {
        Object j1 = TmsDocumenten.boon(b.beanLoader(), "Jutter", Map.of(), List.of("actief"));
        Object v1 = TmsDocumenten.boon(b.beanLoader(), "Vondst", Map.of("gewicht", "9", "deelnemers", "0"), List.of());
        Object v2 = TmsDocumenten.boon(b.beanLoader(), "Vondst", Map.of("gewicht", "4", "deelnemers", "0"), List.of());
        TmsHerhaaldVurenWitnessTest.zetRol(v1, "aanmelder", j1);
        TmsHerhaaldVurenWitnessTest.zetRol(v2, "aanmelder", j1);
        return List.of(j1, v1, v2);
    }

    /** The sentences of the faults that stand after {@code invoer} is put in a fresh session of {@code b} and fired. */
    private static List<String> fouten(DroolsBackend.Build b, Invoer invoer) {
        List<String> uit = new ArrayList<>();
        for (Fout f : lees(b, invoer).fouten()) uit.add(f.melding());
        return uit;
    }

    /** What stands after {@code invoer} is put in a fresh session of {@code b}, with the evaluation date of
     *  {@code TmsInvoervormWitnessTest} where the model reads it, and fired. */
    private static Fixpunt lees(DroolsBackend.Build b, Invoer invoer) {
        KieSession sessie = b.kieBase().newKieSession();
        try {
            try {
                sessie.setGlobal("rekendatum", LocalDate.parse(TmsInvoervormWitnessTest.DATUM));
            } catch (RuntimeException geenGlobal) {
                // the model reads no evaluation date
            }
            DroolsBackend.voegIn(b, sessie, invoer);
            DroolsBackend.Vuring vuring = DroolsBackend.vuur(b, sessie);
            assertEquals(null, vuring.geenFixpunt(), "the fixpoint");
            return DroolsBackend.lees(b, sessie);
        } finally {
            sessie.dispose();
        }
    }

    /** The sentence with which {@code voegIn} refuses {@code invoer} in a fresh session of {@code b}. */
    private static String weigering(DroolsBackend.Build b, Invoer invoer) {
        KieSession sessie = b.kieBase().newKieSession();
        try {
            return assertThrows(IllegalArgumentException.class, () -> DroolsBackend.voegIn(b, sessie, invoer)).getMessage();
        } finally {
            sessie.dispose();
        }
    }

    private static String naam(Object o) {
        return o.getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(o));
    }
}
