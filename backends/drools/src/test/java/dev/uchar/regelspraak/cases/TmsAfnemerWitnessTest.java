package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import dev.uchar.regelspraak.runtime.RegelSpraakRuntime;
import dev.uchar.regelspraak.transpiler.tms.Fixpunt;
import dev.uchar.regelspraak.transpiler.tms.Fout;
import dev.uchar.regelspraak.transpiler.tms.Schrijversconflict;

import org.junit.jupiter.api.Test;
import org.kie.api.event.rule.AgendaEventListener;
import org.kie.api.event.rule.BeforeMatchFiredEvent;
import org.kie.api.event.rule.DefaultAgendaEventListener;
import org.kie.api.runtime.KieSession;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The entry for a consumer of the truth-maintenance branch (card tms-omschakeling, point 12): a model with a creation
 * fires through {@link DroolsBackend#vuur} in generations, {@link DroolsBackend#lees} gives what the creations made, and
 * {@link DroolsBackend#zetAfgeleideWaarden} puts the links they laid on the reference fields of the beans, so that a
 * consumer that reads the beans reaches a made object as it reaches an object of the input.
 *
 * <ul>
 *   <li>Golden {@code anchor/jutterswet-rekenkamer}, case 1: the object creation "richt de rekenkamer op" makes a
 *       Rekenkamer R for J1 over the 1:1 relation taxatieopdracht, and J1's heffing is 70. Before the write-back the
 *       session holds R, its link and the heffing, and J1's bean holds neither; after it J1's reference field
 *       rekenkamer holds R, R's field opdrachtgever holds J1, and J1's heffing is 70.</li>
 *   <li>Golden {@code jutterswet-dubbelverbinding}, case 1: the fact creation "winterberging door de haven" links every
 *       ligger of a veilige haven over winterberging. After the write-back S1's field winterhaven holds H1, the field of
 *       its own fact type, while thuishaven, the relation the creation joined over, still holds what the case gave, and
 *       S4, whose H3 is not veilig, has none: the row {@code HerhaaldTypepaarWitnessTest} plays on the exec-model
 *       branch, "the creation wrote the winterberging leg".</li>
 *   <li>A session of that model fired with a plain {@code fireAllRules} holds no creation: {@code lees} and
 *       {@code zetAfgeleideWaarden} refuse it, and the sentence names the creations.</li>
 *   <li>So on every model of the branch (the fix round of step 3, point 4): on the values document of
 *       {@link TmsHerhaaldVurenWitnessTest} both refuse a session that did not fire, or fired with a plain
 *       {@code fireAllRules}, and write nothing; both refuse a session whose last {@code vuur} stopped at the cap, with
 *       the sentence of that {@code vuur}, and read the next {@code vuur} that reaches the fixpoint; and {@code lees}
 *       refuses a session whose last {@code vuur} broke off with an exception, here the filter document of
 *       {@link TmsFilterfoutWitnessTest} with its beans inserted without {@code voegIn}, so that the filter fault rule
 *       finds no place. What {@code lees} gives is thus always a fixpoint.</li>
 *   <li>And after a {@code vuur} that reached its fixpoint (the second fix round of step 3, point 1), both refuse a
 *       session in which something happened outside {@code vuur}, naming what happened and the way on, and write
 *       nothing. On {@code anchor/jutterswet-rekenkamer}, case 1, J1's heffing is 70 at the fixpoint; J1's grondslag
 *       becomes 150 ({@code update}) and a plain {@code fireAllRules} follows, which leaves the made Rekenkamer on the
 *       grondslag it was initialised with; the sentence names the update and the first rule the plain fire fired, and
 *       sends the consumer to a fresh session, since {@code vuur} refuses a second fire on a model with a creation; a
 *       fresh session of grondslag 150 gives 120 ({@link TmsHerhaaldVurenWitnessTest}, measured in the reference
 *       interpreter and the ASP runner). On the values document, without a creation, both refuse after an update
 *       alone, with the sentence that asks a {@code vuur} first; after that {@code vuur} the reading is that of a fresh
 *       session of the changed input. A plain fire between two fires makes {@code vuur} refuse as well, so there the
 *       sentence sends the consumer to a fresh session ({@link TmsHerhaaldVurenWitnessTest}, the row of the plain fire;
 *       until the addition to step 5a1 of 2026-10-10 this row fired again after the plain fire and read the fresh
 *       reading). After the write-back, an update makes
 *       both refuse with the way to a fresh session, since {@code vuur} refuses a written-back session. <b>Red before
 *       the change</b> (measured 2026-10-10 on {@code 32a649c4}, the tip of the first fix round): each of the three rows
 *       failed on "Expected java.lang.IllegalStateException to be thrown, but nothing was thrown."; their old form, which
 *       read where these rows expect a refusal, gave the state of the first input: after the update and a plain fire of
 *       two firings, J1's heffing 70, which the write-back then wrote on the bean, where a fresh session gives 120; and
 *       after an update of V1 without a further {@code vuur}, the first reading, J1's totaalgewicht 14 and V1 zwaar with
 *       bonus 7, heffing 40, uitkering 20 and vindloon 15.</li>
 *   <li>{@code lees} with labels (the fix round of step 3, point 5): on the red case
 *       {@code corpus/rood/jutterswet-dubbele-melding}, case 1, two writers give V's meldingsdatum two dates, and the
 *       fault of the fixpoint names V as "V, een Vondst", where {@code lees} without labels names it by type and
 *       identity; the write-back with the same labels throws the fault in the same sentence. The reference interpreter
 *       names V alike, "V, a Vondst", and the ASP runner gives "V" beside the fault (measured 2026-10-10; gate 8 pins
 *       both). <b>Red before the change</b> (measured 2026-10-10 on {@code f4b83d7b}): the row does not compile there,
 *       "method lees in class dev.uchar.regelspraak.cases.DroolsBackend cannot be applied to given types", since the
 *       entry read without labels only.</li>
 *   <li>On the exec-model and the DRL-text branch {@code lees} refuses: their rules write the beans as they fire.</li>
 *   <li>The entry is public, the seams of the scale harness and the witnesses are not.</li>
 * </ul>
 *
 * <p><b>Red before the change</b> (measured 2026-10-10 on {@code 095a586a}, the old form of the first two rows, in which a
 * consumer fired through the package-private driver and wrote back): "J1's field rekenkamer holds the made Rekenkamer
 * ==&gt; expected: not &lt;null&gt;" and "the creation wrote the winterberging leg ==&gt; expected:
 * &lt;dev.uchar.regelspraak.facts.Haven@35267fd4&gt; but was: &lt;null&gt;". The witness itself does not compile
 * there. The rows of point 4 (measured 2026-10-10 on {@code f4b83d7b}, where only a model with a creation was refused,
 * by the {@code Creatiefase} the driver leaves): each of the three new rows failed on "Expected
 * java.lang.IllegalStateException to be thrown, but nothing was thrown.", the reading handing out what stood, and the
 * row of the plain fire on "expected: &lt;geen fixpunt om te lezen of terug te zetten: deze sessie is niet via
 * DroolsBackend.vuur gevuurd, …&gt; but was: &lt;dit model heeft creatieregels en vuurt in generaties
 * (DroolsBackend.vuur); de sessie is zonder creatiefase gevuurd, dus zonder de creaties&gt;".
 */
class TmsAfnemerWitnessTest {

    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    @Test void een_objectcreatie_staat_na_het_terugzetten_aan_beide_kanten_op_de_bonen() throws Exception {
        speel("anchor/jutterswet-rekenkamer", "case1-berekend.json", (bouw, sessie, labels) -> {
            Object j1 = labels.get("J1");
            assertNull(DroolsBackend.vuur(bouw, sessie).geenFixpunt(), "the fixpoint");
            Fixpunt f = DroolsBackend.lees(bouw, sessie);
            assertEquals(1, f.objecten().size(), () -> "one made object: " + f.objecten());
            Fixpunt.GemaaktObject r = f.objecten().get(0);
            assertEquals("richt de rekenkamer op", r.creatie());
            assertSame(j1, r.onderwerp());
            assertEquals("Rekenkamer", r.object().getClass().getSimpleName());
            assertEquals(List.of(new Fixpunt.GemaakteKoppeling("taxatieopdracht", "opdrachtgever", j1, "rekenkamer",
                    r.object())), f.koppelingen());
            assertTrue(f.waarden().stream().anyMatch(w -> w.object() == j1 && w.attribuut().equals("heffing")
                    && w.waarde() instanceof BigDecimal d && d.compareTo(new BigDecimal("70")) == 0),
                    () -> "J1 heffing 70: " + f.waarden());
            assertNull(RegelSpraakRuntime.get(j1, "rekenkamer"), "before the write-back J1's bean holds the input alone");
            assertNull(RegelSpraakRuntime.get(j1, "heffing"), "before the write-back J1's bean holds the input alone");
            DroolsBackend.zetAfgeleideWaarden(bouw, sessie);
            assertSame(r.object(), RegelSpraakRuntime.get(j1, "rekenkamer"), "J1's field rekenkamer holds the made Rekenkamer");
            assertSame(j1, RegelSpraakRuntime.get(r.object(), "opdrachtgever"), "R's field opdrachtgever holds J1");
            assertEquals(0, new BigDecimal("70").compareTo((BigDecimal) RegelSpraakRuntime.get(j1, "heffing")), "J1 heffing");
            // the write-back set a reference field of a bean of the input, its own work, which the watch does not count
            // as a changed relation
            assertEquals(f.koppelingen(), DroolsBackend.lees(bouw, sessie).koppelingen(),
                    "the reading after the write-back");
            return null;
        });
    }

    @Test void een_feitcreatie_staat_na_het_terugzetten_op_het_veld_van_haar_eigen_feittype() throws Exception {
        speel("jutterswet-dubbelverbinding", "case1-binnen-venster.json", (bouw, sessie, labels) -> {
            Object h1 = labels.get("H1"), s1 = labels.get("S1"), s4 = labels.get("S4");
            assertNull(DroolsBackend.vuur(bouw, sessie).geenFixpunt(), "the fixpoint");
            Fixpunt f = DroolsBackend.lees(bouw, sessie);
            assertTrue(f.koppelingen().contains(new Fixpunt.GemaakteKoppeling("winterberging", "winterhaven", h1,
                    "winterschuit", s1)), () -> "the link of S1 to H1 over winterberging: " + f.koppelingen());
            assertNull(RegelSpraakRuntime.get(s1, "winterhaven"), "before the write-back S1's bean holds the input alone");
            DroolsBackend.zetAfgeleideWaarden(bouw, sessie);
            assertSame(h1, RegelSpraakRuntime.get(s1, "winterhaven"), "the creation wrote the winterberging leg");
            assertSame(h1, RegelSpraakRuntime.get(s1, "thuishaven"),
                    "and the joined-over thuisligging leg still says what the case said");
            assertNull(RegelSpraakRuntime.get(s4, "winterhaven"), "H3 is not veilig, so its ligger got nothing");
            assertEquals(f.koppelingen(), DroolsBackend.lees(bouw, sessie).koppelingen(),
                    "the reading after the write-back");
            return null;
        });
    }

    @Test void een_model_met_creatie_vuurt_via_vuur_en_niet_met_een_kale_fire() throws Exception {
        speel("jutterswet-dubbelverbinding", "case1-binnen-venster.json", (bouw, sessie, labels) -> {
            sessie.fireAllRules();
            String zin = "geen fixpunt om te lezen of terug te zetten: deze sessie is niet via DroolsBackend.vuur gevuurd,"
                    + " dat tot het fixpunt vuurt en de creaties van dit model in generaties laat vuren";
            assertEquals(zin, assertThrows(IllegalStateException.class, () -> DroolsBackend.lees(bouw, sessie))
                    .getMessage());
            assertEquals(zin, assertThrows(IllegalStateException.class,
                    () -> DroolsBackend.zetAfgeleideWaarden(bouw, sessie)).getMessage());
            return null;
        });
    }

    @Test void lees_en_terugzetten_weigeren_een_sessie_die_niet_via_vuur_vuurde() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(TmsHerhaaldVurenWitnessTest.waardedocument(),
                DroolsBackend.Branch.EXEC_MODEL);
        TmsHerhaaldVurenWitnessTest.inSessie(b, () -> {
            Map<String, Object> bonen = TmsHerhaaldVurenWitnessTest.waardebonen(b, "10");
            KieSession sessie = b.kieBase().newKieSession();
            try {
                DroolsBackend.voegIn(b, sessie, new Invoer().beans(bonen.values()));
                String zin = "geen fixpunt om te lezen of terug te zetten: deze sessie is niet via DroolsBackend.vuur"
                        + " gevuurd, dat tot het fixpunt vuurt";
                assertEquals(zin, assertThrows(IllegalStateException.class, () -> DroolsBackend.lees(b, sessie))
                        .getMessage(), "a session that did not fire");
                sessie.fireAllRules();
                assertEquals(zin, assertThrows(IllegalStateException.class, () -> DroolsBackend.lees(b, sessie))
                        .getMessage(), "a session fired with a plain fireAllRules");
                assertEquals(zin, assertThrows(IllegalStateException.class,
                        () -> DroolsBackend.zetAfgeleideWaarden(b, sessie)).getMessage(), "the write-back");
                assertNull(TmsDocumenten.lees(bonen.get("V1"), "uitkering"), "the write-back wrote nothing");
            } finally {
                sessie.dispose();
            }
        });
    }

    @Test void lees_en_terugzetten_weigeren_een_sessie_waarvan_de_laatste_vuur_geen_fixpunt_bereikte() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(TmsHerhaaldVurenWitnessTest.waardedocument(),
                DroolsBackend.Branch.EXEC_MODEL);
        TmsHerhaaldVurenWitnessTest.inSessie(b, () -> {
            Map<String, Object> bonen = TmsHerhaaldVurenWitnessTest.waardebonen(b, "10");
            KieSession sessie = b.kieBase().newKieSession();
            try {
                DroolsBackend.voegIn(b, sessie, new Invoer().beans(bonen.values()));
                DroolsBackend.Vuring afgekapt = DroolsBackend.vuur(b, sessie, 0.01, 1);
                assertNotNull(afgekapt.geenFixpunt(), "the run stops at a cap of a few firings");
                String zin = "geen fixpunt om te lezen of terug te zetten: de laatste DroolsBackend.vuur van deze sessie"
                        + " bereikte er geen — " + afgekapt.geenFixpunt();
                assertEquals(zin, assertThrows(IllegalStateException.class, () -> DroolsBackend.lees(b, sessie))
                        .getMessage(), "the reading");
                assertEquals(zin, assertThrows(IllegalStateException.class,
                        () -> DroolsBackend.zetAfgeleideWaarden(b, sessie)).getMessage(), "the write-back");
                assertNull(DroolsBackend.vuur(b, sessie).geenFixpunt(), "a further fire under the public cap");
                assertEquals(TmsHerhaaldVurenWitnessTest.EERSTE_WAARDEN,
                        TmsHerhaaldVurenWitnessTest.beschrijf(DroolsBackend.lees(b, sessie), bonen),
                        "the reading of the last fire, which reached the fixpoint");
            } finally {
                sessie.dispose();
            }
        });
    }

    @Test void lees_weigert_een_sessie_waarvan_de_laatste_vuur_afbrak() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(TmsFilterfoutWitnessTest.document(TmsFilterfoutWitnessTest.ZWARE),
                DroolsBackend.Branch.EXEC_MODEL);
        TmsHerhaaldVurenWitnessTest.inSessie(b, () -> {
            Object j1 = TmsDocumenten.boon(b.beanLoader(), "Jutter", Map.of(), List.of("actief"));
            Object v1 = TmsDocumenten.boon(b.beanLoader(), "Vondst", Map.of("gewicht", "9", "deelnemers", "0"), List.of());
            TmsHerhaaldVurenWitnessTest.zetRol(v1, "aanmelder", j1);
            KieSession sessie = b.kieBase().newKieSession();
            try {
                // inserted without voegIn, so the member has no place, and the filter fault rule stops the fire
                sessie.insert(j1);
                sessie.insert(v1);
                RuntimeException e = assertThrows(RuntimeException.class, () -> DroolsBackend.vuur(b, sessie));
                String zin = assertThrows(IllegalStateException.class, () -> DroolsBackend.lees(b, sessie)).getMessage();
                assertTrue(zin.startsWith("geen fixpunt om te lezen of terug te zetten: de laatste DroolsBackend.vuur van"
                        + " deze sessie bereikte er geen — de fire brak af met " + e.getClass().getSimpleName() + ": "), zin);
            } finally {
                sessie.dispose();
            }
        });
    }

    @Test void een_wijziging_en_een_kale_fire_na_vuur_weigeren_lees_en_terugzetten_op_een_model_met_creatie()
            throws Exception {
        speel("anchor/jutterswet-rekenkamer", "case1-berekend.json", (bouw, sessie, labels) -> {
            Object j1 = labels.get("J1");
            assertNull(DroolsBackend.vuur(bouw, sessie).geenFixpunt(), "the fixpoint");
            assertEquals("70", heffing(DroolsBackend.lees(bouw, sessie), j1), "J1 heffing at the fixpoint");
            RegelSpraakRuntime.set(j1, "grondslag", new BigDecimal("150"));
            sessie.update(sessie.getFactHandle(j1), j1);
            String regel = kaleFire(sessie);
            String zin = buitenVuurOm(naam(j1) + " werd gewijzigd met update"
                    + (regel == null ? "" : " en de regel \"" + regel + "\" vuurde"), VERS_BIJ_CREATIE);
            assertEquals(zin, assertThrows(IllegalStateException.class, () -> DroolsBackend.lees(bouw, sessie))
                    .getMessage(), "the reading");
            assertEquals(zin, assertThrows(IllegalStateException.class,
                    () -> DroolsBackend.zetAfgeleideWaarden(bouw, sessie)).getMessage(), "the write-back");
            assertNull(RegelSpraakRuntime.get(j1, "heffing"), "the write-back wrote nothing");
            return null;
        });
    }

    @Test void na_een_update_lezen_lees_en_terugzetten_pas_na_een_volgende_vuur() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(TmsHerhaaldVurenWitnessTest.waardedocument(),
                DroolsBackend.Branch.EXEC_MODEL);
        TmsHerhaaldVurenWitnessTest.inSessie(b, () -> {
            Map<String, Object> bonen = TmsHerhaaldVurenWitnessTest.waardebonen(b, "10");
            KieSession sessie = b.kieBase().newKieSession();
            try {
                DroolsBackend.voegIn(b, sessie, new Invoer().beans(bonen.values()));
                assertNull(DroolsBackend.vuur(b, sessie).geenFixpunt(), "the first fire");
                Object v1 = bonen.get("V1");
                TmsHerhaaldVurenWitnessTest.zet(v1, "gewicht", "3");
                sessie.update(sessie.getFactHandle(v1), v1);
                String gewijzigd = naam(v1) + " werd gewijzigd met update";
                assertEquals(buitenVuurOm(gewijzigd, VUUR_EERST), assertThrows(IllegalStateException.class,
                        () -> DroolsBackend.lees(b, sessie)).getMessage(), "the reading after the update");
                assertEquals(buitenVuurOm(gewijzigd, VUUR_EERST), assertThrows(IllegalStateException.class,
                        () -> DroolsBackend.zetAfgeleideWaarden(b, sessie)).getMessage(), "the write-back after the update");
                assertNull(TmsDocumenten.lees(v1, "uitkering"), "the write-back wrote nothing");
                assertNull(DroolsBackend.vuur(b, sessie).geenFixpunt(), "the fire the sentence asks for");
                assertEquals(TmsHerhaaldVurenWitnessTest.TWEEDE_WAARDEN,
                        TmsHerhaaldVurenWitnessTest.beschrijf(DroolsBackend.lees(b, sessie), bonen),
                        "the reading after that fire, that of a fresh session of the changed input");
            } finally {
                sessie.dispose();
            }
        });
    }

    @Test void een_update_na_het_terugzetten_wijst_lees_naar_een_verse_sessie() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(TmsHerhaaldVurenWitnessTest.waardedocument(),
                DroolsBackend.Branch.EXEC_MODEL);
        TmsHerhaaldVurenWitnessTest.inSessie(b, () -> {
            Map<String, Object> bonen = TmsHerhaaldVurenWitnessTest.waardebonen(b, "10");
            KieSession sessie = b.kieBase().newKieSession();
            try {
                DroolsBackend.voegIn(b, sessie, new Invoer().beans(bonen.values()));
                assertNull(DroolsBackend.vuur(b, sessie).geenFixpunt(), "the fire");
                DroolsBackend.zetAfgeleideWaarden(b, sessie);
                assertEquals(TmsHerhaaldVurenWitnessTest.EERSTE_WAARDEN,
                        TmsHerhaaldVurenWitnessTest.beschrijf(DroolsBackend.lees(b, sessie), bonen),
                        "the write-back is the entry's own: the session reads after it as before it");
                Object v1 = bonen.get("V1");
                TmsHerhaaldVurenWitnessTest.zet(v1, "gewicht", "3");
                sessie.update(sessie.getFactHandle(v1), v1);
                assertEquals(buitenVuurOm(naam(v1) + " werd gewijzigd met update", VERS_NA_TERUGZETTEN),
                        assertThrows(IllegalStateException.class, () -> DroolsBackend.lees(b, sessie)).getMessage(),
                        "the reading after an update of a written-back session");
            } finally {
                sessie.dispose();
            }
        });
    }

    @Test void lees_met_labels_noemt_het_object_van_een_fout_bij_zijn_label() throws Exception {
        Path ir = Path.of("..", "..", "corpus", "rood", "jutterswet-dubbele-melding", "ir.json");
        JsonNode c = TmsDocumenten.JSON.readTree(Files.readString(ir.resolveSibling("case1-beide-schrijvers-gelden.json")));
        CaseRunner.voorMeting(ir, DroolsBackend.Branch.EXEC_MODEL).metSessie(c, (bouw, sessie, labels) -> {
            Map<Object, String> naarLabel = new IdentityHashMap<>();
            labels.forEach((l, boon) -> naarLabel.put(boon, l));
            Object v = labels.get("V");
            assertNull(DroolsBackend.vuur(bouw, sessie).geenFixpunt(), "the fixpoint, at which the fault stands");
            String zin = "geen fixpunt: twee schrijvers van één doel geven verschillende waarden — de regel \"melding op"
                    + " vinddag\" schrijft 2026-03-04 en de regel \"melding op aanspoeldag\" schrijft 2026-03-01 in het"
                    + " attribuut meldingsdatum van %s; divergentie is een fout (RUNTIME-SEMANTICS ch. 5.7)";
            List<Fout> gelabeld = DroolsBackend.lees(bouw, sessie, naarLabel::get).fouten();
            assertEquals(1, gelabeld.size(), () -> "one fault: " + gelabeld);
            assertEquals(String.format(zin, "V, een Vondst"), gelabeld.get(0).melding(), "named by its label");
            assertEquals(String.format(zin, "een Vondst (" + v.getClass().getSimpleName() + "@"
                    + Integer.toHexString(System.identityHashCode(v)) + ")"),
                    DroolsBackend.lees(bouw, sessie).fouten().get(0).melding(), "without labels, by type and identity");
            assertEquals(gelabeld.get(0).melding(), assertThrows(Schrijversconflict.class,
                    () -> DroolsBackend.zetAfgeleideWaarden(bouw, sessie, naarLabel::get)).getMessage(),
                    "the fault the write-back throws, named alike");
            return null;
        });
    }

    @Test void lees_weigert_op_de_andere_takken() throws Exception {
        // the DRL-text branch alone: the exec-model branch becomes the branch with truth maintenance at this card
        for (DroolsBackend.Branch tak : List.of(DroolsBackend.Branch.DRL_TEXT)) {
            DroolsBackend.Build b = DroolsBackend.build(TmsDocumenten.document(TmsDocumenten.model(List.of("erkend",
                    "vergund")), List.of(TmsDocumenten.kenmerkregel("vergund indien erkend", "vergund",
                    TmsDocumenten.check("erkend", false)))), tak);
            KieSession sessie = b.kieBase().newKieSession();
            try {
                assertEquals("lees leest wat de tak met truth maintenance afleidde; op de " + tak + "-tak schrijven de"
                        + " regels de afgeleide waarden al tijdens het vuren op de beans",
                        assertThrows(UnsupportedOperationException.class, () -> DroolsBackend.lees(b, sessie))
                                .getMessage());
            } finally {
                sessie.dispose();
            }
        }
    }

    @Test void de_ingang_is_publiek_en_de_naden_niet() throws Exception {
        Class<?> d = DroolsBackend.class;
        for (Method m : List.of(
                d.getDeclaredMethod("voegIn", DroolsBackend.Build.class, KieSession.class, Invoer.class),
                d.getDeclaredMethod("vuur", DroolsBackend.Build.class, KieSession.class),
                d.getDeclaredMethod("lees", DroolsBackend.Build.class, KieSession.class),
                d.getDeclaredMethod("lees", DroolsBackend.Build.class, KieSession.class, Function.class),
                d.getDeclaredMethod("zetAfgeleideWaarden", DroolsBackend.Build.class, KieSession.class),
                d.getDeclaredMethod("zetAfgeleideWaarden", DroolsBackend.Build.class, KieSession.class, Function.class)))
            assertTrue(Modifier.isPublic(m.getModifiers()) && Modifier.isStatic(m.getModifiers()), m.toString());
        for (Class<?> c : List.of(DroolsBackend.Vuring.class, Invoer.class, Fixpunt.class))
            assertTrue(Modifier.isPublic(c.getModifiers()), c.toString());
        for (Method m : List.of(
                d.getDeclaredMethod("vuur", DroolsBackend.Build.class, KieSession.class, int.class),
                d.getDeclaredMethod("vuur", DroolsBackend.Build.class, KieSession.class, double.class, int.class)))
            assertFalse(Modifier.isPublic(m.getModifiers()), m.toString());
    }

    // ---------------- helpers ----------------

    /** Sets up case {@code zaak} of golden {@code golden} on the truth-maintenance branch as the replay does, through
     *  {@link DroolsBackend#voegIn}, and hands the session, not yet fired, to {@code meting}. */
    private static void speel(String golden, String zaak, CaseRunner.Sessiemeting<Void> meting) throws Exception {
        Path ir = CORPUS.resolve(golden).resolve("ir.json");
        JsonNode c = TmsDocumenten.JSON.readTree(Files.readString(ir.resolveSibling(zaak)));
        CaseRunner.voorMeting(ir, DroolsBackend.Branch.EXEC_MODEL).metSessie(c, meting);
    }

    /** The way on where something happened outside {@code vuur} after it: a further {@code vuur}, or a fresh session
     *  where {@code vuur} refuses the session. */
    static final String VUUR_EERST = "vuur haar eerst opnieuw met DroolsBackend.vuur";
    static final String VERS_BIJ_CREATIE = "dit model heeft creatieregels, dus DroolsBackend.vuur weigert een tweede"
            + " fire: open een verse sessie";
    static final String VERS_NA_TERUGZETTEN = "deze sessie is teruggezet, dus DroolsBackend.vuur weigert haar: open een"
            + " verse sessie met beans die alleen invoer dragen";

    /** The sentence of {@code lees} and {@code zetAfgeleideWaarden} where {@code wat} happened outside {@code vuur}
     *  after the last one, with {@code weg} the way on. */
    static String buitenVuurOm(String wat, String weg) {
        return "geen fixpunt om te lezen of terug te zetten: sinds de laatste DroolsBackend.vuur van deze sessie gebeurde"
                + " er iets buiten vuur om, wat het fixpunt van die vuur kan veranderen (" + wat + "); " + weg;
    }

    /** Fires {@code sessie} with a plain {@code fireAllRules}, as a consumer can, and gives the name of the first rule
     *  that fired, or null. */
    static String kaleFire(KieSession sessie) {
        String[] eerste = {null};
        AgendaEventListener l = new DefaultAgendaEventListener() {
            @Override public void beforeMatchFired(BeforeMatchFiredEvent e) {
                if (eerste[0] == null) eerste[0] = e.getMatch().getRule().getName();
            }
        };
        sessie.addEventListener(l);
        try {
            sessie.fireAllRules();
        } finally {
            sessie.removeEventListener(l);
        }
        return eerste[0];
    }

    /** An object as the sentences of the entry name it: its type and its identity. */
    static String naam(Object o) {
        return o.getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(o));
    }

    /** The heffing {@code f} derives for {@code jutter}, as a plain number, or null. */
    private static String heffing(Fixpunt f, Object jutter) {
        for (Fixpunt.AfgeleideWaarde w : f.waarden())
            if (w.object() == jutter && w.attribuut().equals("heffing"))
                return ((BigDecimal) w.waarde()).stripTrailingZeros().toPlainString();
        return null;
    }
}
