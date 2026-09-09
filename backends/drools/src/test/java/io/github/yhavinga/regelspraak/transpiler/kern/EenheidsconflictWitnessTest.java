package io.github.yhavinga.regelspraak.transpiler.kern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.github.yhavinga.regelspraak.engine.BeanCompiler;
import io.github.yhavinga.regelspraak.loader.IrLoader;
import io.github.yhavinga.regelspraak.model.Eenheid;
import io.github.yhavinga.regelspraak.schema.FactModelGenerator;
import io.github.yhavinga.regelspraak.transpiler.DrlTranspiler;
import io.github.yhavinga.regelspraak.transpiler.model.ExecModelTranspiler;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The MIRROR of {@code scripts/check-reftype-derivation.sh}, one direction over.
 *
 * <p>That harness twists an ANNOTATION and holds the DECLARATIONS byte-identical: it asks whether a consumer
 * reproduces the outcome when the wire repetition lies. This witness twists a DECLARATION and holds the
 * annotations byte-identical: it asks whether a consumer NOTICES when the two contradict each other so badly
 * that no conversion factor exists. Measured before the fence below existed (2026-09-09, on a copy of
 * {@code corpus/golden/jutterswet-schouwrooster} whose declaration of one addend moved to {@code uur}, factor
 * 3600 to {@code seconde}): the verifier failed WF-091, {@code ref} faulted "incompatible units: schouwdagen
 * vs uur", clingo faulted ahead of translation, and this backend replayed both cases against the untouched
 * golden's pinned values. Not a wire read — a copy with a CONVERTIBLE sister declaration (factor 0,5) moved
 * the outcome from 15 to 13,5, so the declaration WAS consulted. What was missing is the consequence: every
 * restatement helper answered "no conversion is due" and "no conversion EXISTS" with the same identity.
 *
 * <p><b>Both directions are asserted here, and the second is what keeps the harness green.</b> The fence keys
 * on the two DERIVED units, never on annotation-against-declaration disagreement — so a lying annotation over
 * an honest declaration still lowers, byte-identically, exactly as the harness demands. A fence that fired on
 * disagreement would refuse every one of its twists.
 */
class EenheidsconflictWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** cwd under surefire is the module dir (backends/drools); the corpus is two levels up. */
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    /** A concrete unit from no system the corpus knows: a canonical of its own, so it inter-converts with
     *  nothing a golden declares, and a factor, so it is not the WF-092 factorless form. */
    private static final String VREEMD = """
            {"node":"Eenheid","afkorting":"uchar-vreemd","stelsel":null,
             "canoniek":"uchar-vreemd-canoniek","factorNaarCanoniek":3600,"noemer":null}""";

    // ---------------- the guard itself: what it refuses and what it lets through ----------------

    private static final Eenheid KG = new Eenheid("kg", "Gewicht", "kg", BigDecimal.ONE);
    private static final Eenheid TON = new Eenheid("t", "Gewicht", "kg", new BigDecimal("1000"));
    private static final Eenheid EUR = new Eenheid("EUR", "Geld", "EUR", BigDecimal.ONE);
    private static final Eenheid JR = new Eenheid("jr", "Tijd", "mnd", new BigDecimal("12"));

    @Test void twee_concrete_eenheden_zonder_gedeelde_canoniek_zijn_een_fout() {
        Eenheidsconflict f = assertThrows(Eenheidsconflict.class,
                () -> Eenheidsconflict.eisHerschaalbaar(JR, KG, "een §4-waardeherschaling"));
        assertSame(JR, f.van());
        assertSame(KG, f.naar());
        assertEquals("een §4-waardeherschaling", f.plek());
        // the message must name BOTH sides, the way ref's "incompatible units: schouwdagen vs uur" does:
        // a fault that names only the target leaves the reader without the half that moved.
        assertTrue(f.getMessage().contains("'jr'") && f.getMessage().contains("'kg'"), f.getMessage());
        assertTrue(f.getMessage().contains("een §4-waardeherschaling"), "de plek hoort in de melding");
    }

    @Test void een_omwisselbaar_paar_gaat_ongehinderd_door() {
        // the ordinary cross-unit restatement: kg <-> t share a canonical, so a factor exists and the site
        // must compute it. Both directions, and the identity pair.
        Eenheidsconflict.eisHerschaalbaar(KG, TON, "witness");
        Eenheidsconflict.eisHerschaalbaar(TON, KG, "witness");
        Eenheidsconflict.eisHerschaalbaar(KG, KG, "witness");
    }

    @Test void een_eenheidloze_zijde_is_geen_conflict() {
        // ch. 2.6: a unitless side adopts the other, value unchanged — ref does the same and no factor is due.
        Eenheidsconflict.eisHerschaalbaar(null, KG, "witness");
        Eenheidsconflict.eisHerschaalbaar(KG, null, "witness");
        Eenheidsconflict.eisHerschaalbaar(null, null, "witness");
    }

    @Test void een_opaak_product_blijft_de_behoudende_no_op() {
        // RUNTIME-SEMANTICS ch. 2.10 records this backend's stance on "€·kg": no canonical, so no factor is
        // invented and the value stands in the unit it was computed in. Faulting here would make that recorded
        // sentence false, so the exemption is deliberate and named.
        Eenheid opaak = Eenheid.product(EUR, KG);
        assertEquals(null, opaak.canoniek(), "een opaak product draagt per constructie geen canoniek");
        Eenheidsconflict.eisHerschaalbaar(opaak, EUR, "witness");
        Eenheidsconflict.eisHerschaalbaar(EUR, opaak, "witness");
    }

    @Test void een_tarief_tegen_zijn_eigen_teller_is_de_afgetopte_noemer() {
        // The §7.1 cancellation: a €/mnd quantity totalled over months lands in €, so the restatement that
        // follows is the identity by design. Measured on jutterswet-tempototaal and jutterswet-tempovouw,
        // which both replay against their pinned values only because of it. Symmetric in both directions.
        Eenheid tarief = Eenheid.samengesteld(EUR, JR);
        Eenheidsconflict.eisHerschaalbaar(tarief, EUR, "witness");
        Eenheidsconflict.eisHerschaalbaar(EUR, tarief, "witness");
    }

    @Test void een_tarief_tegen_een_vreemde_kale_eenheid_blijft_een_fout() {
        // The exemption above is narrow: it needs CONVERTIBLE numerators. €/jr against kg shares nothing, so
        // the fence must still fire — otherwise every composite would buy itself a free pass.
        Eenheid tarief = Eenheid.samengesteld(EUR, JR);
        assertThrows(Eenheidsconflict.class, () -> Eenheidsconflict.eisHerschaalbaar(tarief, KG, "witness"));
        assertThrows(Eenheidsconflict.class, () -> Eenheidsconflict.eisHerschaalbaar(KG, tarief, "witness"));
    }

    // ---------------- the mutant, rebuilt from whatever goldens carry its shape ----------------

    /**
     * For every golden whose rule adds two unit-bearing attribute reads: move ONE DECLARATION to a foreign unit,
     * leave every annotation untouched, and demand that BOTH transpiler branches fault instead of lowering.
     */
    @Test void een_tegensprekende_declaratie_wordt_door_geen_van_beide_takken_stil_doorgespeeld() throws Exception {
        int execGezien = 0, drlGezien = 0;
        List<String> stil = new ArrayList<>();
        for (Path ir : goldens()) {
            JsonNode eerlijk = JSON.readTree(Files.readString(ir));
            JsonNode liegend = metVerdraaideDeclaratie(eerlijk);
            if (liegend == null) continue;                       // this golden does not carry the shape

            if (lowertExec(eerlijk)) {
                execGezien++;
                if (!(vangExec(liegend) instanceof Eenheidsconflict))
                    stil.add(naam(ir) + " (exec-model): " + omschrijf(vangExec(liegend)));
            }
            if (lowertDrl(eerlijk)) {
                drlGezien++;
                if (!(vangDrl(liegend) instanceof Eenheidsconflict))
                    stil.add(naam(ir) + " (DRL-tekst): " + omschrijf(vangDrl(liegend)));
            }
        }
        // never silently green: a corpus without the shape would make every assertion above vacuous, so the
        // empty probe is itself the failure — and each branch must have been probed on its own.
        assertTrue(execGezien > 0, "geen enkele golden bood de exec-model-tak een additieve eenheidsjoin");
        assertTrue(drlGezien > 0, "geen enkele golden bood de DRL-tekst-tak een additieve eenheidsjoin");
        assertEquals(List.of(), stil, "een verdraaide declaratie werd stil gelowered in plaats van gemeld");
    }

    /**
     * The harness's own direction, at the same position: move only the ANNOTATION of that same reference to the
     * same foreign unit. The declaration is the source, so nothing may fault and the DRL text may not move by a
     * character — this is the property that keeps {@code check-reftype-derivation.sh} green under the fence.
     */
    @Test void een_liegende_annotatie_boven_een_kloppende_declaratie_verandert_niets() throws Exception {
        int gezien = 0;
        List<String> bewogen = new ArrayList<>();
        for (Path ir : goldens()) {
            JsonNode eerlijk = JSON.readTree(Files.readString(ir));
            JsonNode liegend = metVerdraaideAnnotatie(eerlijk);
            if (liegend == null || !lowertDrl(eerlijk)) continue;
            gezien++;
            Throwable t = vangDrl(liegend);
            if (t != null) { bewogen.add(naam(ir) + " faalde: " + omschrijf(t)); continue; }
            if (!drl(eerlijk).equals(drl(liegend))) bewogen.add(naam(ir) + ": de DRL-tekst bewoog");
            if (lowertExec(eerlijk) && vangExec(liegend) != null)
                bewogen.add(naam(ir) + " (exec-model) faalde: " + omschrijf(vangExec(liegend)));
        }
        assertTrue(gezien > 0, "geen enkele golden bood een verdraaibare annotatie op deze positie");
        assertEquals(List.of(), bewogen, "de annotatie is niet de bron: een leugen daar mag niets bewegen");
    }

    // ---------------- twisting ----------------

    /** A copy whose right addend's DECLARED unit is foreign, or null when this golden carries no such shape. */
    private static JsonNode metVerdraaideDeclaratie(JsonNode ir) throws Exception {
        JsonNode kopie = ir.deepCopy();
        ObjectNode ref = additieveRechterRef(kopie);
        if (ref == null) return null;
        ObjectNode decl = declaratieVan(kopie, ref);
        decl.set("eenheid", JSON.readTree(VREEMD));
        return kopie;
    }

    /** A copy whose right addend's ANNOTATED unit is foreign — the declaration untouched. */
    private static JsonNode metVerdraaideAnnotatie(JsonNode ir) throws Exception {
        JsonNode kopie = ir.deepCopy();
        ObjectNode ref = additieveRechterRef(kopie);
        if (ref == null) return null;
        ((ObjectNode) ref.get("type")).set("eenheid", JSON.readTree(VREEMD));
        return kopie;
    }

    /**
     * The right operand of the FIRST rule whose value is an additive join of two attribute reads that BOTH
     * resolve to a unit-bearing declaration — the shape an additive restatement always visits. Null when the
     * document carries none. Deterministic: rules are visited in document order and the first hit wins.
     */
    private static ObjectNode additieveRechterRef(JsonNode ir) {
        JsonNode regels = ir.get("resolvedRegels");
        if (regels == null) return null;
        for (JsonNode r : regels) {
            JsonNode w = r.get("waarde");
            if (w == null || !"Binair".equals(w.path("node").asText())
                    || !List.of("PLUS", "MIN", "VERMINDERD_MET").contains(w.path("op").asText())) continue;
            JsonNode links = w.get("links"), rechts = w.get("rechts");
            if (declaratieVan(ir, links) == null || declaratieVan(ir, rechts) == null) continue;
            return (ObjectNode) rechts;
        }
        return null;
    }

    /** The DECLARATION an {@code AttribuutRef} reads from, when it carries a unit; else null. */
    private static ObjectNode declaratieVan(JsonNode ir, JsonNode ref) {
        if (ref == null || !"AttribuutRef".equals(ref.path("node").asText())) return null;
        JsonNode ot = ir.path("objecttypen").get(ref.path("objecttype").asText(""));
        if (ot == null) return null;
        JsonNode attr = ot.path("attributen").get(ref.path("attribuut").asText(""));
        if (attr == null || attr.path("eenheid").isMissingNode() || attr.path("eenheid").isNull()) return null;
        return (ObjectNode) attr;
    }

    // ---------------- driving the two branches ----------------

    /** Lower through the DRL-TEXT branch, returning the throwable it raised (null on success). */
    private static Throwable vangDrl(JsonNode ir) {
        try { drl(ir); return null; } catch (Throwable t) { return t; }
    }

    /** Lower through the EXEC-MODEL branch — the branch gate 12 replays — returning the throwable (null on success). */
    private static Throwable vangExec(JsonNode ir) {
        try { exec(ir); return null; } catch (Throwable t) { return t; }
    }

    private static boolean lowertDrl(JsonNode ir) { return vangDrl(ir) == null; }

    private static boolean lowertExec(JsonNode ir) { return vangExec(ir) == null; }

    private static String drl(JsonNode ir) {
        return DrlTranspiler.transpile(IrLoader.loadAll(ir).model());
    }

    private static void exec(JsonNode ir) {
        IrLoader.Loaded loaded = IrLoader.loadAll(ir);
        ClassLoader cl = BeanCompiler.compileer(FactModelGenerator.genereer(loaded.model()));
        ClassLoader vorige = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(cl);
        try {
            assertNotNull(ExecModelTranspiler.build(loaded.model(), cl, loaded.prioritizedDefaults(),
                    loaded.rolBindingen(), loaded.letVormen()));
        } finally {
            Thread.currentThread().setContextClassLoader(vorige);
        }
    }

    // ---------------- helpers ----------------

    private static List<Path> goldens() throws Exception {
        try (Stream<Path> s = Files.walk(CORPUS)) {
            return s.filter(p -> p.getFileName().toString().equals("ir.json")).sorted().toList();
        }
    }

    private static String naam(Path ir) {
        return CORPUS.relativize(ir.getParent()).toString();
    }

    /** What came back instead of the expected fault — the diagnostic a bare boolean cannot give. */
    private static String omschrijf(Throwable t) {
        if (t == null) return "het document lowerde zonder melding";
        return t.getClass().getSimpleName() + ": " + t.getMessage();
    }

    @Test void de_corpuswandeling_vindt_daadwerkelijk_goldens() throws Exception {
        if (goldens().isEmpty()) fail("geen enkele corpus-golden gevonden onder " + CORPUS.toAbsolutePath());
    }
}
