package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import dev.uchar.regelspraak.engine.ScenarioResultaat;
import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.runtime.RegelSpraakRuntime;
import dev.uchar.regelspraak.transpiler.kern.Namen;
import dev.uchar.regelspraak.transpiler.tms.Fixpunt;

import org.junit.jupiter.api.Test;
import org.kie.api.runtime.KieSession;

import java.math.BigDecimal;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The input step of a case as a consumer calls it ({@link CaseInvoer}): the case read onto beans, the role links set
 * after RUNTIME-SEMANTICS ch. 5.7a, the {@link Invoer} that {@link DroolsBackend#voegIn} takes, and the refusals with
 * the link they name. Every row reads its case through {@link CaseInvoer#van} and fires, where it fires, through
 * {@link DroolsBackend}, as a consumer with a request of its own does; no row goes through the replay's seams, so what
 * holds here holds for a consumer outside this package.
 *
 * <p>The models are corpus goldens ({@code jutterswet-ketenband}, a self-typed one-to-many relation with the to-one
 * {@code voorligger} and the to-many {@code opvolger}; {@code jutterswet-kavelgrens}, a self-typed one-to-one relation,
 * {@code noordbuur} and {@code zuidbuur}; {@code anchor/jutterswet-maatschap}, the reciprocal {@code maat};
 * {@code jutterswet-penningjaar}, an attribute on a year timeline), and the fold document of
 * {@link TmsFilterfoutWitnessTest}. The cases that collide are authored here; the corpus is not touched.
 */
class CaseInvoerWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    private static final String KETENBAND = "jutterswet-ketenband";
    private static final String KAVELGRENS = "jutterswet-kavelgrens";
    private static final String MAATSCHAP = "anchor/jutterswet-maatschap";

    // ---------------- ch. 5.7a: one leg, both legs ----------------

    @Test void een_naar_veel_relatie_staat_in_het_veld_van_het_lid_hoe_de_case_haar_ook_schrijft() throws Exception {
        // case1 writes both legs, case2 the to-many leg on W1 only, case3 the to-one legs on W2 and W3 only: one
        // relation, so one fact base. The relation has ONE field, voorligger on the member, which serves both
        // directions; nothing fills it toward W1.
        DroolsBackend.Build b = bouw(KETENBAND);
        for (String c : List.of("case1", "case2", "case3")) {
            CaseInvoer invoer = CaseInvoer.van(b, caseFile(KETENBAND, c));
            Map<String, Object> beans = invoer.beans();
            assertEquals(List.of("W1", "W2", "W3"), List.copyOf(beans.keySet()), c + ": the labels in case order");
            assertSame(beans.get("W1"), veld(beans.get("W2"), "voorligger"), c);
            assertSame(beans.get("W1"), veld(beans.get("W3"), "voorligger"), c);
            assertNull(veld(beans.get("W1"), "voorligger"), c + ": no link toward W1 is invented");
            for (String label : beans.keySet()) assertEquals("Wachter", invoer.typen().get(label).naam, c);
        }
    }

    @Test void een_op_een_relatie_uit_een_poot_vult_beide_velden() throws Exception {
        // case2 writes zuidbuur only (K1 -> K2, K2 -> K3). A one-to-one relation owns a field per direction, so the
        // entry writes the far field too: K2's noordbuur is K1, K3's is K2. case1 writes both legs: the same beans.
        DroolsBackend.Build b = bouw(KAVELGRENS);
        for (String c : List.of("case1", "case2")) {
            Map<String, Object> k = CaseInvoer.van(b, caseFile(KAVELGRENS, c)).beans();
            assertSame(k.get("K2"), veld(k.get("K1"), "zuidbuur"), c);
            assertSame(k.get("K3"), veld(k.get("K2"), "zuidbuur"), c);
            assertSame(k.get("K1"), veld(k.get("K2"), "noordbuur"), c + ": the far field of K1's link");
            assertSame(k.get("K2"), veld(k.get("K3"), "noordbuur"), c + ": the far field of K2's link");
            assertNull(veld(k.get("K1"), "noordbuur"), c);
            assertNull(veld(k.get("K3"), "zuidbuur"), c);
        }
    }

    @Test void een_wederkerige_rol_uit_een_poot_geldt_beide_kanten_op_en_twee_poten_zijn_een_relatie() throws Exception {
        DroolsBackend.Build b = bouw(MAATSCHAP);
        Map<String, Object> een = CaseInvoer.van(b, caseFile(MAATSCHAP, "case1")).beans();
        assertSame(een.get("J2"), veld(een.get("J1"), "maat"), "the leg the case writes");
        assertSame(een.get("J1"), veld(een.get("J2"), "maat"), "the reverse leg of the separate pass");
        assertNull(veld(een.get("J3"), "maat"), "the pass writes no more than the relation");
        Map<String, Object> twee = CaseInvoer.van(b, maatschap(List.of("J1", "J2", "J3"),
                Map.of("J1", List.of("J2"), "J2", List.of("J1")))).beans();
        assertSame(twee.get("J2"), veld(twee.get("J1"), "maat"));
        assertSame(twee.get("J1"), veld(twee.get("J2"), "maat"));
    }

    // ---------------- WF-013: a second, different target on a to-one field, at each of the four places ----------------

    @Test void een_tweede_doel_op_de_eigen_naar_een_rol_noemt_die_link() throws Exception {
        CaseInvoer.Rolfout f = rolfout(() -> CaseInvoer.van(bouw(MAATSCHAP), maatschap(List.of("J1", "J2", "J3"),
                Map.of("J1", List.of("J2", "J3")))));
        botsing(f, "J1", "maat", "J3");
    }

    @Test void een_tweede_doel_op_het_verre_veld_van_een_een_op_een_relatie_noemt_die_link() throws Exception {
        // K1 and K3 both name K2 as their zuidbuur: the forward fields are two, the far field, K2's noordbuur, is one.
        CaseInvoer.Rolfout f = rolfout(() -> CaseInvoer.van(bouw(KAVELGRENS), keten("Duinkavel", "kavelbreedte",
                List.of("K1", "K2", "K3"), Map.of("K1", Map.of("zuidbuur", List.of("K2")),
                        "K3", Map.of("zuidbuur", List.of("K2"))))));
        botsing(f, "K3", "zuidbuur", "K2");
    }

    @Test void een_tweede_doel_op_het_veld_van_een_naar_veel_rol_noemt_die_link() throws Exception {
        // W2 names W3 as its voorligger first; then W1 writes W2 among its opvolgers, which is stored in that same
        // field of W2. The forward write of W2 came first, so the link that collides is W1's.
        CaseInvoer.Rolfout f = rolfout(() -> CaseInvoer.van(bouw(KETENBAND), keten("Wachter", "rangnummer",
                List.of("W2", "W3", "W1"), Map.of("W2", Map.of("voorligger", List.of("W3")),
                        "W1", Map.of("opvolgers", List.of("W2"))))));
        botsing(f, "W1", "opvolgers", "W2");
    }

    @Test void een_botsende_omgekeerde_poot_noemt_de_link_waarvan_de_pas_de_poot_spiegelde() throws Exception {
        // J1's maat is J2 and J2's maat is J3: the reverse leg of J1's link would make J2's maat J1. In either order
        // of the instances the pass meets the collision at J1's leg, and names the link J1 wrote.
        for (List<String> volgorde : List.of(List.of("J1", "J2", "J3"), List.of("J2", "J3", "J1"))) {
            CaseInvoer.Rolfout f = rolfout(() -> CaseInvoer.van(bouw(MAATSCHAP), maatschap(volgorde,
                    Map.of("J1", List.of("J2"), "J2", List.of("J3")))));
            botsing(f, "J1", "maat", "J2");
            assertTrue(((RuntimeException) f).getMessage().contains("de omgekeerde poot botst"), volgorde.toString());
        }
    }

    // ---------------- a link the bean model cannot hold ----------------

    @Test void een_onbekend_doellabel_wordt_geweigerd_met_de_link() throws Exception {
        CaseInvoer.Rolfout f = rolfout(() -> CaseInvoer.van(bouw(KETENBAND), keten("Wachter", "rangnummer",
                List.of("W1", "W2"), Map.of("W1", Map.of("opvolgers", List.of("W2", "W9"))))));
        weigering(f, "W1", "opvolgers", "W9", "role 'opvolgers' names unknown label W9");
    }

    @Test void een_rol_zonder_naar_een_referentieveld_wordt_geweigerd_met_de_link() throws Exception {
        // a key that names no role of the self-typed pair: neither by name nor by the pair, which carries two roles
        CaseInvoer.Rolfout f = rolfout(() -> CaseInvoer.van(bouw(KETENBAND), keten("Wachter", "rangnummer",
                List.of("W1", "W2"), Map.of("W1", Map.of("buur", List.of("W2"))))));
        weigering(f, "W1", "buur", "W2", "role 'buur' (Wachter -> Wachter) reaches no to-one reference field: the"
                + " written role name resolves to no unique role, and neither direction of the type pair derives a"
                + " to-one role");
    }

    @Test void een_verdrongen_referentieveld_wordt_geweigerd_met_de_link() throws Exception {
        // ketenband without its rules and with an attribute voorligger on Wachter: the attribute takes the bean
        // property of the role (attribute > characteristic > reference), so the field to write is gone
        ObjectNode ir = (ObjectNode) JSON.readTree(Files.readString(ir(KETENBAND)));
        ((ArrayNode) ir.get("rules").get("rules")).removeAll();
        ((ArrayNode) IrPad.objectType(ir, "Wachter").get("attributes")).addObject().put("node", "Attribute")
                .put("name", "voorligger").set("datatype", JSON.readTree(
                        "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"nonNegative\"}"));
        DroolsBackend.Build b = DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL);
        CaseInvoer.Rolfout f = rolfout(() -> CaseInvoer.van(b, keten("Wachter", "rangnummer", List.of("W1", "W2"),
                Map.of("W2", Map.of("voorligger", List.of("W1"))))));
        weigering(f, "W2", "voorligger", "W1", "role 'voorligger' resolves to the to-one role 'voorligger' on Wachter,"
                + " which shares its name with a declared member — the bean cannot carry the reference field"
                + " (name-sharing bean limit: attribute > characteristic > reference)");
    }

    // ---------------- the labels of a request ----------------

    @Test void met_de_ids_van_een_verzoek_als_labels_geeft_de_ingang_wat_het_rapport_geeft() throws Exception {
        // A service writes its request as the instances of a case, with its ids as labels. ketenband case2 so
        // relabelled reads, fires and compares through the entries alone as the report reads the case itself, the
        // labels in the expectations aside.
        Map<String, String> ids = Map.of("W1", "wachter/1", "W2", "wachter/2", "W3", "wachter/3");
        Path pad = CORPUS.resolve(KETENBAND).resolve("case2.json");
        ScenarioResultaat rapport = CaseRunner.rapport(ir(KETENBAND), List.of(pad)).get(0);
        assertEquals(ScenarioResultaat.Status.PASS, rapport.status());
        ScenarioResultaat ingang = speel(bouw(KETENBAND), "case2.json", hernoem(JSON.readTree(Files.readString(pad)), ids));
        Map<String, String> terugLabel = new LinkedHashMap<>();
        ids.forEach((label, id) -> terugLabel.put(id, label));
        List<ScenarioResultaat.VerwachtingResultaat> verwachtingen = new ArrayList<>();
        for (ScenarioResultaat.VerwachtingResultaat v : ingang.verwachtingen()) {
            String bron = v.bron();
            for (Map.Entry<String, String> e : terugLabel.entrySet()) bron = bron.replace(e.getKey(), e.getValue());
            assertTrue(!bron.equals(v.bron()) || v.bron().equals("consistent"), "the expectation names an id: " + v);
            verwachtingen.add(new ScenarioResultaat.VerwachtingResultaat(bron, v.verwacht(), v.werkelijk(), v.ok(),
                    v.positie()));
        }
        List<ScenarioResultaat.Vuring> vuringen = new ArrayList<>();
        for (ScenarioResultaat.Vuring v : ingang.trace().vuringen())
            vuringen.add(new ScenarioResultaat.Vuring(v.regel(), v.labels().stream().map(terugLabel::get).toList(),
                    v.aantal()));
        assertEquals(new ScenarioResultaat(rapport.naam(), rapport.status(), verwachtingen,
                new ScenarioResultaat.Trace(vuringen), ingang.dekking(), ingang.diagnose()), rapport);
        // and a collision names the ids
        CaseInvoer.Rolfout f = rolfout(() -> CaseInvoer.van(bouw(KAVELGRENS), keten("Duinkavel", "kavelbreedte",
                List.of("kavel-1", "kavel-2", "kavel-3"), Map.of("kavel-1", Map.of("zuidbuur", List.of("kavel-2")),
                        "kavel-3", Map.of("zuidbuur", List.of("kavel-2"))))));
        botsing(f, "kavel-3", "zuidbuur", "kavel-2");
    }

    // ---------------- the Invoer ----------------

    @Test void een_lijst_geeft_de_geschreven_leden_in_schrijfvolgorde_en_dan_de_gespiegelde_in_bytevolgorde()
            throws Exception {
        // W1 writes opvolgers W9 then W3; W3, W10 and W2 each name W1 as their voorligger. The list under W1: the
        // written members in the order written, then the mirrored ones by label in byte order ("W10" < "W2"), and
        // W3, in both, once, where it was written.
        DroolsBackend.Build b = bouw(KETENBAND);
        CaseInvoer invoer = CaseInvoer.van(b, keten("Wachter", "rangnummer", List.of("W1", "W9", "W3", "W2", "W10"),
                Map.of("W1", Map.of("opvolgers", List.of("W9", "W3")), "W3", Map.of("voorligger", List.of("W1")),
                        "W10", Map.of("voorligger", List.of("W1")), "W2", Map.of("voorligger", List.of("W1")))));
        Map<String, Object> w = invoer.beans();
        List<Invoer.Lijst> lijsten = invoer.invoer().lijsten();
        assertEquals(1, lijsten.size(), () -> "one list: " + lijsten);
        assertSame(w.get("W1"), lijsten.get(0).houder());
        assertEquals("voorligger", lijsten.get(0).rol());
        assertEquals(List.of("W9", "W3", "W10", "W2"), labels(w, lijsten.get(0).leden()));
        // voegIn takes it, and the session counts the four
        Fixpunt fixpunt = vuur(b, invoer, Map.of());
        assertEquals(0, new BigDecimal("4").compareTo((BigDecimal) waarde(fixpunt, w.get("W1"), "opvolgeraantal")));
    }

    @Test void de_volgorde_van_de_invoer_beslist_welk_lid_een_vouwfout_noemt() throws Exception {
        // The fold of TmsFilterfoutWitnessTest stops at the first member in fold order whose filter divides by 0
        // (RUNTIME-SEMANTICS ch. 5.11). V9 divides 4, V10 divides 9. Written under J1 as V9, V10: 4. Mirrored from
        // their own links: V10 before V9 in byte order, so 9, where the order of the case would give 4.
        DroolsBackend.Build b = DroolsBackend.build(TmsFilterfoutWitnessTest.document(TmsFilterfoutWitnessTest.ZWARE),
                DroolsBackend.Branch.EXEC_MODEL);
        String v9 = TmsDocumenten.instantieMetRollen("V9", "Vondst", Map.of("gewicht", "4", "deelnemers", "0"),
                List.of(), Map.of());
        String v10 = TmsDocumenten.instantieMetRollen("V10", "Vondst", Map.of("gewicht", "9", "deelnemers", "0"),
                List.of(), Map.of());
        JsonNode geschreven = TmsDocumenten.zaak(List.of(TmsDocumenten.instantieMetRollen("J1", "Jutter", Map.of(),
                List.of("actief"), Map.of("vondsten", List.of("V9", "V10"))), v9, v10), List.of());
        assertEquals(List.of("4 gedeeld door 0"), deelfouten(b, geschreven));
        JsonNode gespiegeld = TmsDocumenten.zaak(List.of(TmsDocumenten.instantieMetRollen("J1", "Jutter", Map.of(),
                List.of("actief"), Map.of()),
                TmsDocumenten.instantieMetRollen("V9", "Vondst", Map.of("gewicht", "4", "deelnemers", "0"), List.of(),
                        Map.of("aanmelder", List.of("J1"))),
                TmsDocumenten.instantieMetRollen("V10", "Vondst", Map.of("gewicht", "9", "deelnemers", "0"), List.of(),
                        Map.of("aanmelder", List.of("J1")))), List.of());
        assertEquals(List.of("9 gedeeld door 0"), deelfouten(b, gespiegeld));
    }

    @Test void een_scalar_op_een_tijdlijn_is_een_emmerscalair_en_een_tijdlijn_niet() throws Exception {
        DroolsBackend.Build b = bouw("jutterswet-penningjaar");
        CaseInvoer scalar = CaseInvoer.van(b, penningjaar("{ \"number\": \"14\" }"));
        Object j1 = scalar.beans().get("J1");
        List<Invoer.Emmer> emmers = scalar.invoer().emmers();
        assertEquals(1, emmers.size(), () -> "one bucket scalar: " + emmers);
        assertSame(j1, emmers.get(0).bean());
        assertEquals("aantal aangemelde vondsten", emmers.get(0).attribuut());
        List<?> tl = (List<?>) RegelSpraakRuntime.get(j1, Namen.ident("aantal aangemelde vondsten", false));
        assertEquals(1, tl.size(), "the bean holds the one segment of the asOfDate bucket");
        assertEquals(LocalDate.of(2026, 1, 1), RegelSpraakRuntime.get(tl.get(0), "van"));
        assertEquals(LocalDate.of(2027, 1, 1), RegelSpraakRuntime.get(tl.get(0), "tot"));
        CaseInvoer tijdlijn = CaseInvoer.van(b, penningjaar(
                "{ \"timeline\": [ { \"from\": \"2026-01-01\", \"to\": \"2027-01-01\", \"value\": { \"number\": \"14\" } } ] }"));
        assertEquals(List.of(), tijdlijn.invoer().emmers(), "a full timeline is no bucket scalar");
    }

    @Test void invoer_is_bij_elke_aanroep_vers_en_beans_is_niet_te_wijzigen() throws Exception {
        CaseInvoer invoer = CaseInvoer.van(bouw(KETENBAND), caseFile(KETENBAND, "case2"));
        Invoer een = invoer.invoer(), twee = invoer.invoer();
        assertNotSame(een, twee);
        een.bean(new Object());
        assertEquals(List.copyOf(invoer.beans().values()), twee.beans(), "the second Invoer holds the beans alone");
        assertEquals(List.copyOf(invoer.beans().values()), invoer.invoer().beans());
        assertThrows(UnsupportedOperationException.class, () -> invoer.beans().put("W4", new Object()));
        assertThrows(UnsupportedOperationException.class, () -> invoer.typen().clear());
    }

    @Test void van_geeft_de_contextklassenlader_terug_ook_na_een_weigering() throws Exception {
        DroolsBackend.Build b = bouw(MAATSCHAP);
        ClassLoader vorige = Thread.currentThread().getContextClassLoader();
        try (URLClassLoader eigen = new URLClassLoader(new URL[0], vorige)) {
            Thread.currentThread().setContextClassLoader(eigen);
            CaseInvoer.van(b, caseFile(MAATSCHAP, "case1"));
            assertSame(eigen, Thread.currentThread().getContextClassLoader());
            assertThrows(CaseInvoer.Rolbotsing.class, () -> CaseInvoer.van(b, maatschap(List.of("J1", "J2", "J3"),
                    Map.of("J1", List.of("J2", "J3")))));
            assertSame(eigen, Thread.currentThread().getContextClassLoader());
        } finally {
            Thread.currentThread().setContextClassLoader(vorige);
        }
    }

    @Test void een_case_zonder_instances_wordt_geweigerd() throws Exception {
        DroolsBackend.Build b = bouw(MAATSCHAP);
        for (String c : List.of("{ \"asOfDate\": null }", "{ \"input\": { \"parameters\": {} } }",
                "{ \"input\": { \"instances\": {} } }")) {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> CaseInvoer.van(b, JSON.readTree(c)));
            assertTrue(e.getMessage().startsWith("CaseInvoer.van: de case draagt geen array input.instances"),
                    e.getMessage());
        }
    }

    // ---------------- the whole corpus, through the entries alone ----------------

    @Test void elke_case_van_het_corpus_speelt_via_de_ingangen_als_in_het_rapport() throws Exception {
        // Every golden with case files, read with CaseInvoer.van and played with voegIn, vuur, zetAfgeleideWaarden and
        // vergelijk as a consumer plays it, gives per case the ScenarioResultaat of the report: verdict, expectations,
        // firings, coverage, and for a form not carried the MARKER with the replay's reason.
        int goldens = 0, cases = 0, gespeeld = 0;
        List<Path> irs;
        try (Stream<Path> s = Files.walk(CORPUS)) {
            irs = s.filter(p -> p.getFileName().toString().equals("ir.json")).sorted().toList();
        }
        for (Path ir : irs) {
            List<Path> paden;
            try (Stream<Path> s = Files.list(ir.getParent())) {
                paden = s.filter(p -> p.getFileName().toString().matches("case.*\\.json")).sorted().toList();
            }
            if (paden.isEmpty()) continue;
            goldens++;
            List<ScenarioResultaat> rapport = CaseRunner.rapport(ir, paden);
            List<ScenarioResultaat> ingang = viaDeIngangen(ir, paden);
            assertEquals(rapport, ingang, ir.toString());
            cases += ingang.size();
            gespeeld += (int) ingang.stream().filter(r -> r.status() == ScenarioResultaat.Status.PASS).count();
        }
        // the counts of the report on the corpus of this card's start (300 goldens, 823 cases, 695 PASS), as floors
        assertTrue(goldens >= 300 && cases >= 823 && gespeeld >= 695,
                "goldens " + goldens + ", cases " + cases + ", gespeeld " + gespeeld);
    }

    // ---------------- fixtures ----------------

    private static Path ir(String golden) {
        Path p = CORPUS.resolve(golden).resolve("ir.json");
        assertTrue(Files.exists(p), "golden ir.json present: " + p.toAbsolutePath());
        return p;
    }

    private static DroolsBackend.Build bouw(String golden) throws Exception {
        return DroolsBackend.build(ir(golden), DroolsBackend.Branch.EXEC_MODEL);
    }

    private static JsonNode caseFile(String golden, String naam) throws Exception {
        return JSON.readTree(Files.readString(CORPUS.resolve(golden).resolve(naam + ".json")));
    }

    /** A reference field of a bean, read by its property name. */
    private static Object veld(Object bean, String rol) {
        return RegelSpraakRuntime.get(bean, rol);
    }

    /** A case of {@code type} instances in the order of {@code labels}, each with the whole number {@code attribuut} 1,
     *  and the roles {@code rollen} by label. */
    private static JsonNode keten(String type, String attribuut, List<String> labels,
                                  Map<String, Map<String, List<String>>> rollen) throws Exception {
        List<String> instanties = new ArrayList<>();
        for (String label : labels)
            instanties.add(TmsDocumenten.instantieMetRollen(label, type, Map.of(attribuut, "1"), List.of(),
                    new LinkedHashMap<>(rollen.getOrDefault(label, Map.of()))));
        return TmsDocumenten.zaak(instanties, List.of());
    }

    /** Jutters of the maatschap golden, in the order of {@code labels}, with the maat {@code maten} by label. */
    private static JsonNode maatschap(List<String> labels, Map<String, List<String>> maten) throws Exception {
        List<String> instanties = new ArrayList<>();
        for (String label : labels)
            instanties.add(TmsDocumenten.instantieMetRollen(label, "Jutter", Map.of("vondstwaarde", "1"), List.of(),
                    maten.containsKey(label) ? Map.of("maat", maten.get(label)) : Map.of()));
        return TmsDocumenten.zaak(instanties, List.of());
    }

    /** J1 of the penningjaar golden on asOfDate 2026-03-15, with {@code waarde} as its aantal aangemelde vondsten. */
    private static JsonNode penningjaar(String waarde) throws Exception {
        return JSON.readTree("""
                { "format": "uchar-case-v0", "asOfDate": "2026-03-15",
                  "input": { "instances": [
                      { "label": "J1", "objectType": "Jutter",
                        "attributes": { "aantal aangemelde vondsten": %s },
                        "roles": {}, "characteristics": [] } ],
                    "parameters": {} } }
                """.formatted(waarde));
    }

    /** A read of a case that may throw. */
    @FunctionalInterface
    private interface Lezing {
        CaseInvoer lees() throws Exception;
    }

    /** The refusal of a role link that {@code lezing} throws, caught as a consumer catches both kinds: in one clause,
     *  whose parameter is a {@link CaseInvoer.Rolfout}. */
    private static CaseInvoer.Rolfout rolfout(Lezing lezing) throws Exception {
        try {
            lezing.lees();
        } catch (CaseInvoer.Rolbotsing | CaseInvoer.Rolweigering e) {
            return e;
        }
        return fail("no refusal of a role link");
    }

    private static void botsing(CaseInvoer.Rolfout f, String label, String rol, String doel) {
        CaseInvoer.Rolbotsing b = assertInstanceOf(CaseInvoer.Rolbotsing.class, f);
        assertInstanceOf(IllegalArgumentException.class, b, "a defect of the case");
        assertEquals(List.of(label, rol, doel), List.of(f.label(), f.rol(), f.doel()), b.getMessage());
        assertTrue(b.getMessage().contains("WF-013"), b.getMessage());
    }

    private static void weigering(CaseInvoer.Rolfout f, String label, String rol, String doel, String zin) {
        CaseInvoer.Rolweigering w = assertInstanceOf(CaseInvoer.Rolweigering.class, f);
        assertInstanceOf(CaseRunner.Excluded.class, w, "a form this backend does not carry");
        assertEquals(List.of(label, rol, doel), List.of(f.label(), f.rol(), f.doel()), w.getMessage());
        assertEquals(zin, w.getMessage());
    }

    private static List<String> labels(Map<String, Object> beans, List<Object> leden) {
        Map<Object, String> label = new IdentityHashMap<>();
        beans.forEach((l, b) -> label.put(b, l));
        return leden.stream().map(label::get).toList();
    }

    private static Object waarde(Fixpunt fixpunt, Object object, String attribuut) {
        return fixpunt.waarden().stream().filter(w -> w.object() == object && w.attribuut().equals(attribuut))
                .map(Fixpunt.AfgeleideWaarde::waarde).findFirst().orElse(null);
    }

    /** The fixpoint of {@code invoer} on {@code b}, read without a write-back, with {@code globals} set. */
    private static Fixpunt vuur(DroolsBackend.Build b, CaseInvoer invoer, Map<String, Object> globals) {
        Map<Object, String> label = new IdentityHashMap<>();
        invoer.beans().forEach((l, bean) -> label.put(bean, l));
        ClassLoader vorige = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(b.beanLoader());
        KieSession sessie = b.kieBase().newKieSession();
        try {
            globals.forEach((naam, w) -> zetGlobal(sessie, naam, w));
            DroolsBackend.voegIn(b, sessie, invoer.invoer());
            DroolsBackend.Vuring v = DroolsBackend.vuur(b, sessie);
            assertNull(v.geenFixpunt());
            return DroolsBackend.lees(b, sessie, label::get);
        } finally {
            sessie.dispose();
            Thread.currentThread().setContextClassLoader(vorige);
        }
    }

    /** The dividends of the divisions by zero that stand at the fixpoint of case {@code c}, as "n gedeeld door 0". */
    private static List<String> deelfouten(DroolsBackend.Build b, JsonNode c) {
        Fixpunt f = vuur(b, CaseInvoer.van(b, c), Map.of());
        List<String> uit = new ArrayList<>();
        for (var fout : f.fouten()) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+) gedeeld door 0")
                    .matcher(fout.fout().getMessage());
            assertTrue(m.find(), fout.fout().getMessage());
            uit.add(m.group());
        }
        return uit;
    }

    private static void zetGlobal(KieSession sessie, String naam, Object waarde) {
        try {
            sessie.setGlobal(naam, waarde);
        } catch (RuntimeException nietGedeclareerd) {
            // a global the KieBase does not declare is one no rule reads
        }
    }

    /** Case {@code c} with every label renamed after {@code ids}: the instances, their role targets, the expectations. */
    private static JsonNode hernoem(JsonNode c, Map<String, String> ids) {
        ObjectNode uit = c.deepCopy();
        for (JsonNode i : uit.get("input").get("instances")) {
            ((ObjectNode) i).put("label", ids.get(i.get("label").asText()));
            for (JsonNode doelen : i.get("roles"))
                for (int k = 0; k < doelen.size(); k++)
                    ((ArrayNode) doelen).set(k, TextNode.valueOf(ids.get(doelen.get(k).asText())));
        }
        for (JsonNode v : uit.get("expected"))
            if (v.hasNonNull("label")) ((ObjectNode) v).put("label", ids.get(v.get("label").asText()));
        return uit;
    }

    /** Every case of {@code paden} that is no red-path case, as {@link CaseRunner#rapport} reports it, played through
     *  the public entries alone. */
    private static List<ScenarioResultaat> viaDeIngangen(Path ir, List<Path> paden) throws Exception {
        DroolsBackend.Build b = null;
        String weigering = null;
        try {
            b = DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL);
        } catch (CaseRunner.Excluded e) {
            weigering = e.getMessage();
        } catch (IrLoader.Refusal e) {
            weigering = "loader: " + e.getMessage();
        }
        List<ScenarioResultaat> uit = new ArrayList<>();
        for (Path p : paden) {
            JsonNode c = JSON.readTree(Files.readString(p));
            if (c.has("expectedFault")) continue;
            String naam = p.getFileName().toString();
            if (b == null) {
                uit.add(marker(naam, weigering));
                continue;
            }
            try {
                uit.add(speel(b, naam, c));
            } catch (CaseRunner.Excluded e) {
                uit.add(marker(naam, e.getMessage()));
            }
        }
        return uit;
    }

    private static ScenarioResultaat marker(String naam, String reden) {
        return new ScenarioResultaat(naam, ScenarioResultaat.Status.MARKER, List.of(),
                new ScenarioResultaat.Trace(List.of()), List.of(), reden);
    }

    /** Case {@code c} as a consumer plays it: read through {@link CaseInvoer}, its parameters and asOfDate as globals,
     *  put in, fired with a waarnemer, written back and compared. */
    private static ScenarioResultaat speel(DroolsBackend.Build b, String naam, JsonNode c) {
        CaseInvoer invoer = CaseInvoer.van(b, c);
        Map<Object, String> label = new IdentityHashMap<>();
        invoer.beans().forEach((l, bean) -> label.put(bean, l));
        ClassLoader vorige = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(b.beanLoader());
        KieSession sessie = b.kieBase().newKieSession();
        try {
            JsonNode params = c.get("input").get("parameters");
            if (params != null) for (Map.Entry<String, JsonNode> e : params.properties())
                zetGlobal(sessie, Namen.ident(e.getKey(), false), parameterwaarde(e.getKey(), e.getValue()));
            if (c.hasNonNull("asOfDate")) zetGlobal(sessie, "rekendatum", LocalDate.parse(c.get("asOfDate").asText()));
            DroolsBackend.voegIn(b, sessie, invoer.invoer());
            DroolsBackend.Waarnemer waarnemer = new DroolsBackend.Waarnemer(label::get);
            DroolsBackend.Vuring vuring = DroolsBackend.vuur(b, sessie, waarnemer);
            ScenarioResultaat.Trace trace = new ScenarioResultaat.Trace(waarnemer.vuringen());
            List<ScenarioResultaat.DekkingRegel> dekking = DroolsBackend.dekking(b, waarnemer.vormen());
            if (vuring.geenFixpunt() != null)
                return new ScenarioResultaat(naam, ScenarioResultaat.Status.FAIL, List.of(), trace, dekking,
                        naam + ": " + vuring.geenFixpunt());
            DroolsBackend.zetAfgeleideWaarden(b, sessie, label::get);
            DroolsBackend.Vergelijking v = DroolsBackend.vergelijk(b, sessie, label::get, c.get("expected"));
            return new ScenarioResultaat(naam, v.pass() ? ScenarioResultaat.Status.PASS : ScenarioResultaat.Status.FAIL,
                    v.verwachtingen(), trace, dekking, v.pass() ? "" : naam + ": " + v.diagnose());
        } finally {
            sessie.dispose();
            Thread.currentThread().setContextClassLoader(vorige);
        }
    }

    /** A parameter value as a consumer gives it: the forms the corpus carries, and the replay's refusal of a timeline
     *  and of a rational number. */
    private static Object parameterwaarde(String naam, JsonNode w) {
        if (w.has("timeline"))
            throw new CaseRunner.Excluded("granular (timeline) parameter '" + naam + "' is not carried by the v0 backend");
        if (w.has("number")) {
            if (w.get("number").asText().contains("/"))
                throw new CaseRunner.Excluded("rational value on '" + naam + "' is not carried by the v0 backend");
            return new BigDecimal(w.get("number").asText());
        }
        if (w.has("date")) return LocalDate.parse(w.get("date").asText());
        if (w.has("boolean")) return w.get("boolean").asBoolean();
        if (w.has("text")) return w.get("text").asText();
        if (w.has("enumeration")) return w.get("enumeration").asText();
        return fail("a parameter form this witness does not set: " + naam + " " + w);
    }
}
