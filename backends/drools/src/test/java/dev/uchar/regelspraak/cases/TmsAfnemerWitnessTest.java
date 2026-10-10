package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import dev.uchar.regelspraak.runtime.RegelSpraakRuntime;
import dev.uchar.regelspraak.transpiler.tms.Fixpunt;

import org.junit.jupiter.api.Test;
import org.kie.api.runtime.KieSession;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
 *       {@code zetAfgeleideWaarden} refuse it.</li>
 *   <li>On the exec-model and the DRL-text branch {@code lees} refuses: their rules write the beans as they fire.</li>
 *   <li>The entry is public, the seams of the scale harness and the witnesses are not.</li>
 * </ul>
 *
 * <p><b>Red before the change</b> (measured 2026-10-10 on {@code 095a586a}, the old form of the first two rows, in which a
 * consumer fired through the package-private driver and wrote back): "J1's field rekenkamer holds the made Rekenkamer
 * ==&gt; expected: not &lt;null&gt;" and "the creation wrote the winterberging leg ==&gt; expected:
 * &lt;dev.uchar.regelspraak.facts.Haven@35267fd4&gt; but was: &lt;null&gt;". The witness itself does not compile
 * there.
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
            return null;
        });
    }

    @Test void een_model_met_creatie_vuurt_via_vuur_en_niet_met_een_kale_fire() throws Exception {
        speel("jutterswet-dubbelverbinding", "case1-binnen-venster.json", (bouw, sessie, labels) -> {
            sessie.fireAllRules();
            String zin = "dit model heeft creatieregels en vuurt in generaties (DroolsBackend.vuur); de sessie is zonder"
                    + " creatiefase gevuurd, dus zonder de creaties";
            assertEquals(zin, assertThrows(IllegalStateException.class, () -> DroolsBackend.lees(bouw, sessie))
                    .getMessage());
            assertEquals(zin, assertThrows(IllegalStateException.class,
                    () -> DroolsBackend.zetAfgeleideWaarden(bouw, sessie)).getMessage());
            return null;
        });
    }

    @Test void lees_weigert_op_de_andere_takken() throws Exception {
        for (DroolsBackend.Branch tak : List.of(DroolsBackend.Branch.EXEC_MODEL, DroolsBackend.Branch.DRL_TEXT)) {
            DroolsBackend.Build b = DroolsBackend.build(TmsDocumenten.document(TmsDocumenten.model(List.of("erkend",
                    "vergund")), List.of(TmsDocumenten.kenmerkregel("vergund indien erkend", "vergund",
                    TmsDocumenten.check("erkend", false)))), tak);
            KieSession sessie = b.kieBase().newKieSession();
            try {
                assertEquals("lees leest wat de tak met truth maintenance afleidde; op de " + tak + "-tak schrijven de"
                        + " regels de afgeleide waarden al tijdens het vuren op de bonen",
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
        CaseRunner.voorMeting(ir, DroolsBackend.Branch.TRUTH_MAINTENANCE).metSessie(c, meting);
    }
}
