package io.github.yhavinga.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.github.yhavinga.regelspraak.engine.BeanCompiler;
import io.github.yhavinga.regelspraak.loader.IrLoader;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel;
import io.github.yhavinga.regelspraak.schema.FactModelGenerator;
import io.github.yhavinga.regelspraak.transpiler.DrlTranspiler;
import io.github.yhavinga.regelspraak.transpiler.kern.Dekking;
import io.github.yhavinga.regelspraak.transpiler.kern.Tijdlijnen;
import io.github.yhavinga.regelspraak.transpiler.model.ExecModelTranspiler;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The period COUNT of a timeline facet is a multiplicity, not a presence — pinned in both transpiler branches
 * (2026-09-11, card {@code 20260911-kenmerktijdlijnaantal-poort}).
 *
 * <p>Both branches asked "is this member a §13.3.6.7 multi-period raster?" as
 * {@code tijdlijnAantal.containsKey(lid)}, at 24 sites. The wire's count defaults to ONE (IR-SPEC ch. 2.8 states the
 * facet as {@code count >= 1} and its worked example adopts {@code count: 1}; ch. 2.10's v0 migration note reads a bare
 * granularity as {@code count: 1}; the verifier's WF-005 substitutes 1 for an absent entry), so an EXPLICIT 1 is the
 * same facet as no entry at all and a raster starts at 2. The presence test therefore refused a document that says out
 * loud what every other document says by default.
 *
 * <p>It stayed invisible because the RegelSpraak frontend writes the entry only for a count other than 1
 * ({@code DefinitionResolver}), so no frontend-produced document carries it at 1. Measured over the corpus on the
 * PRE-FIX jar — an explicit 1 injected on one temporal member at a time, both branches, against each branch's own
 * untouched status line: 32 of 46 member positions flipped out of the played channel, over 12 of the 16 goldens with
 * temporal members and case files, while the verifier, {@code ref} and clingo stayed unchanged on the very same
 * documents (verifier PASS; {@code oracle: PASS}; {@code clingo: PASS}, same pinned expectations).
 *
 * <p>Every model below is SYNTHETIC, built in memory from a golden's {@code ir.json} off the wire; the corpus files are
 * read and never written. Each test names the position it gates and the PRE-FIX measurement it reverses.
 */
class MeerPeriodiekePoortWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** cwd under surefire is the module dir (backends/drools); the corpus is two levels up. */
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    /** The §8.2 qualifier over a NAVIGATED temporal characteristic ("de werf van de Schuit gedurende het gehele jaar
     *  bedrijvig is"), four cases, both branches PLAYED. */
    private static final String GEHELEJAARKENMERK = "synth-gehelejaarkenmerk";
    /** A §7.3.2 tijdsevenredig deel into a temporal doel, clipped by the subject's OWN temporal kenmerk, totalled by
     *  a §7.1 "het totaal van". One case, both branches PLAYED. */
    private static final String CLIPPRORATA = "synth2-clip-over-prorata";
    /** A §7.1 "het totaal van" over a temporal attribute, no clip. One case, both branches PLAYED. */
    private static final String TEMPOTOTAAL = "jutterswet-tempototaal";
    /** The verifier-only witness cabinet — the corpus's living carrier of {@code attribuutTijdlijnAantal: 1}, on the
     *  very attribute over which it states a §8.2 whole-period comparison. Deliberately case-less. */
    private static final String VORMENKABINET = "jutterswet-vormenkabinet";

    // ---------------- the predicate itself ----------------

    @Test void een_aantal_van_een_is_geen_raster() {
        assertFalse(Tijdlijnen.meerPeriodiek(java.util.Map.of(), "waterstand"), "afwezig = enkelvoudig");
        assertFalse(Tijdlijnen.meerPeriodiek(java.util.Map.of("waterstand", 1), "waterstand"),
                "een expliciete 1 is dezelfde facet als geen sleutel (IR-SPEC ch. 2.8/2.10, WF-005)");
        assertTrue(Tijdlijnen.meerPeriodiek(java.util.Map.of("waterstand", 2), "waterstand"),
                "een raster begint bij twee");
        assertFalse(Tijdlijnen.meerPeriodiek(java.util.Map.of("waterstand", 0), "waterstand"),
                "een count < 1 is WF-005's weigering, niet die van dit predicaat — zo'n document haalt geen enkele poort");
    }

    // ---------------- the kenmerk side ----------------

    @Test void de_genavigeerde_kenmerktijdreeks_speelt_bij_aantal_een() throws Exception {
        // navigatieKenmerkTijdreeks, both branches — the position the card was cut for. PRE-FIX measurement on the
        // untouched golden + Werf.kenmerkTijdlijnAantal {"bedrijvig": 1}:
        //     verifier: PASS
        //     ref     : oracle: PASS (3 expectations) on each of the four cases
        //     clingo  : clingo: PASS (3 expectations) on each of the four cases
        //     Drools exec-model : EXCLUDE transpiler SKIP: regel: toeslag van een schuit — regel (voorwaarde buiten
        //                         Phase-B scope)
        //     Drools DRL-tekst  : EXCLUDE drl-transpiler MARKER: regel: toeslag van een schuit — TODO §8.2
        //                         gehele-periode: de werf van de Schuit gedurende het gehele jaar bedrijvig is
        // One key, one value, no semantic difference, two executors fewer.
        beideTakkenSpelen(metAantal(GEHELEJAARKENMERK, "Werf", "kenmerkTijdlijnAantal", "bedrijvig", 1),
                GEHELEJAARKENMERK, 4);
    }

    @Test void de_eigen_kenmerktijdreeks_speelt_bij_aantal_een() throws Exception {
        // temporeelKenmerkTijdreeks — the subject's OWN kenmerk on the §7.1 clip of a temporal write. PRE-FIX, with
        // Jutter.kenmerkTijdlijnAantal {"borgplichtig": 1}:
        //     Drools exec-model : EXCLUDE transpiler SKIP: regel: borgdeel van een jutter — regel (temporele
        //                         toekenning buiten Phase-F scope)
        //     Drools DRL-tekst  : EXCLUDE drl-transpiler MARKER: regel: borgdeel van een jutter — TODO temporele
        //                         toekenning
        beideTakkenSpelen(metAantal(CLIPPRORATA, "Jutter", "kenmerkTijdlijnAantal", "borgplichtig", 1),
                CLIPPRORATA, 1);
    }

    // ---------------- the attribute side: the card's question 3, measured ----------------

    @Test void het_temporele_doel_speelt_bij_aantal_een() throws Exception {
        // The §5.1.4 temporal-doel write AND the §13.3.6.7 multi-period marker beside it — the branch pair that reads
        // the count TWICE, once negated and once plain. PRE-FIX, with Jutter.attribuutTijdlijnAantal {"borgdeel": 1}:
        //     Drools exec-model : EXCLUDE transpiler SKIP: regel: borgdeel van een jutter — regel (meer-periodieke
        //                         temporele doel — §13.3.6.7, geen producent)
        //     Drools DRL-tekst  : EXCLUDE drl-transpiler MARKER: regel: borgdeel van een jutter — TODO meer-periodieke
        //                         temporele toekenning (§13.3.6.7)
        // The same injection also crosses the §7.1 "het totaal van zijn borgdeel" fold in the second rule.
        beideTakkenSpelen(metAantal(CLIPPRORATA, "Jutter", "attribuutTijdlijnAantal", "borgdeel", 1),
                CLIPPRORATA, 1);
    }

    @Test void de_tijdsevenredige_basis_speelt_bij_aantal_een() throws Exception {
        // The §7.3.2 tijdsevenredig-deel BASE operand. PRE-FIX, with Jutter.attribuutTijdlijnAantal {"borgtarief": 1}:
        //     Drools exec-model : EXCLUDE transpiler SKIP: regel: borgdeel van een jutter — regel (temporele
        //                         toekenning buiten Phase-F scope)
        //     Drools DRL-tekst  : EXCLUDE drl-transpiler MARKER: regel: borgdeel van een jutter — TODO temporele
        //                         toekenning
        beideTakkenSpelen(metAantal(CLIPPRORATA, "Jutter", "attribuutTijdlijnAantal", "borgtarief", 1),
                CLIPPRORATA, 1);
    }

    @Test void het_totaal_van_een_tijdreeks_speelt_bij_aantal_een() throws Exception {
        // The §7.1 "het totaal van" fold, the one temporal expression that collapses to a scalar. PRE-FIX, with
        // Jutter.attribuutTijdlijnAantal {"bewaarvergoeding per maand": 1}:
        //     Drools exec-model : EXCLUDE transpiler SKIP: regel: bewaargeld van een jutter — regel (aggregatie
        //                         buiten Phase-C scope)
        //     Drools DRL-tekst  : EXCLUDE drl-transpiler MARKER: regel: bewaargeld van een jutter — TODO totaal van
        //                         een meer-periodieke tijdreeks (§13.3.6.7, voor elke <n> maanden)
        beideTakkenSpelen(metAantal(TEMPOTOTAAL, "Jutter", "attribuutTijdlijnAantal", "bewaarvergoeding per maand", 1),
                TEMPOTOTAAL, 1);
    }

    @Test void de_gehele_periode_vergelijking_leest_het_aantal_helemaal_niet() throws Exception {
        // The card's question 3 on the position its living corpus witness occupies: jutterswet-vormenkabinet carries
        // attribuutTijdlijnAantal {"waterstand": 1} on the very attribute over which it states a §8.2 whole-period
        // COMPARISON. Measured here rather than assumed — and the answer is NO for this one position: the scalar §8.2
        // arm gates on granularity alone (DrlTranspiler.isTemporeelAttribuut / ExecModelTranspiler.isTemporeel82), so
        // the count table is not consulted and the document transpiles identically with and without the key. The SET
        // arm of the same section (isTemporeelSetAttribuut / isTemporeelSet82) DOES read the count and is fixed with
        // the rest; no corpus document reaches it.
        // Verdict compared byte for byte on the DRL text and construct for construct on both coverage manifests.
        Path met = ir(VORMENKABINET), zonder = zonderAantal(VORMENKABINET, "Schouwveld", "attribuutTijdlijnAantal");
        assertEquals(drlTekst(zonder), drlTekst(met),
                "de §8.2-vergelijking leest het periodeaantal niet — de DRL is identiek met en zonder de sleutel");
        assertEquals(dekking(zonder), dekking(met),
                "en de exec-model-dekking eveneens");
    }

    // ---------------- the over-acceptance flank ----------------

    @Test void een_echt_raster_blijft_geweigerd() throws Exception {
        // Without this, deleting the gate outright would satisfy every test above. A count of TWO is a §13.3.6.7
        // multi-period raster whose producer semantics no branch carries, and BOTH must keep refusing it by name.
        Path kenmerk = metAantal(GEHELEJAARKENMERK, "Werf", "kenmerkTijdlijnAantal", "bedrijvig", 2);
        assertTrue(execReden(kenmerk, GEHELEJAARKENMERK).contains("voorwaarde buiten Phase-B scope"),
                execReden(kenmerk, GEHELEJAARKENMERK));
        assertTrue(drlReden(kenmerk, GEHELEJAARKENMERK).contains("§8.2 gehele-periode"),
                drlReden(kenmerk, GEHELEJAARKENMERK));

        Path attribuut = metAantal(CLIPPRORATA, "Jutter", "attribuutTijdlijnAantal", "borgdeel", 2);
        assertTrue(execReden(attribuut, CLIPPRORATA).contains("meer-periodieke temporele doel — §13.3.6.7"),
                execReden(attribuut, CLIPPRORATA));
        assertTrue(drlReden(attribuut, CLIPPRORATA).contains("meer-periodieke temporele toekenning (§13.3.6.7)"),
                drlReden(attribuut, CLIPPRORATA));
    }

    // ---------------- fixtures ----------------

    /** Both branches replay the golden's OWN case files against the golden's OWN pinned expectations — so a pass here
     *  is the differential square on a document only the count key distinguishes from the played original.
     *
     *  <p>The exec branch is driven case by case through {@link CaseRunner#speelCase} rather than through the
     *  multi-case entry beside it, and that is not a style choice: gate 1's rename tripwire greps for the pre-rename
     *  vocabulary of 2026-08-29, one token of which sits inside that other method's NAME, and the tripwire's
     *  reason-carrying allowance list (a protected script) names the two production files only. A test that called
     *  it would turn gate 1 red — measured, not guessed. */
    private static void beideTakkenSpelen(Path ir, String golden, int cases) throws Exception {
        List<Path> gevallen = gevallen(golden);
        assertEquals(cases, gevallen.size(), "het aantal cases van " + golden);
        for (Path c : gevallen)
            assertEquals(null, CaseRunner.speelCase(ir, JSON.readTree(Files.readString(c))),
                    "exec-model op " + c.getFileName());
        CaseRunner.Result drl = CaseRunner.viaDrlTekst(ir, gevallen);
        assertEquals("PLAYED", drl.status(), "DRL-tekst: " + drl.reason() + " " + drl.caseFailures());
        assertEquals(cases, drl.casesPlayed());
    }

    private static String execReden(Path ir, String golden) {
        JsonNode c;
        try {
            c = JSON.readTree(Files.readString(gevallen(golden).get(0)));
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
        return assertThrows(CaseRunner.Excluded.class, () -> CaseRunner.speelCase(ir, c),
                "verwacht een weigering van de exec-model-tak").getMessage();
    }

    private static String drlReden(Path ir, String golden) {
        CaseRunner.Result r = CaseRunner.viaDrlTekst(ir, gevallen(golden));
        assertEquals("EXCLUDE", r.status(), "verwacht een weigering, kreeg " + r.status());
        return r.reason();
    }

    private static Path ir(String golden) {
        Path p = CORPUS.resolve(golden).resolve("ir.json");
        assertTrue(Files.exists(p), "golden ir.json present: " + p.toAbsolutePath());
        return p;
    }

    private static List<Path> gevallen(String golden) {
        List<Path> uit = new ArrayList<>();
        try (var s = Files.list(CORPUS.resolve(golden))) {
            s.filter(p -> p.getFileName().toString().startsWith("case")).sorted().forEach(uit::add);
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
        assertFalse(uit.isEmpty(), "golden carries case files: " + golden);
        return uit;
    }

    /** The golden's ir.json with ONE per-member count entry written explicitly. */
    private static Path metAantal(String golden, String objecttype, String tabel, String lid, int aantal) throws Exception {
        ObjectNode d = (ObjectNode) JSON.readTree(Files.readString(ir(golden)));
        ((ObjectNode) d.get("objecttypen").get(objecttype).get(tabel)).put(lid, aantal);
        return tijdelijk(golden + "-" + lid + "-" + aantal, d);
    }

    /** The golden's ir.json with a whole count table emptied — the "as if no producer had written it" control. */
    private static Path zonderAantal(String golden, String objecttype, String tabel) throws Exception {
        ObjectNode d = (ObjectNode) JSON.readTree(Files.readString(ir(golden)));
        ((ObjectNode) d.get("objecttypen").get(objecttype)).set(tabel, JSON.createObjectNode());
        return tijdelijk(golden + "-zonder-" + tabel, d);
    }

    private static Path tijdelijk(String naam, JsonNode d) throws Exception {
        Path tmp = Files.createTempFile(naam, ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, d.toString());
        return tmp;
    }

    /** The DRL TEXT the classic branch emits for a document — no case, no fire. */
    private static String drlTekst(Path irPath) throws Exception {
        return DrlTranspiler.build(IrLoader.load(JSON.readTree(Files.readString(irPath)))).drl();
    }

    /** The exec-model branch's coverage manifest for a document, as comparable text. */
    private static List<String> dekking(Path irPath) throws Exception {
        IrLoader.Loaded loaded = IrLoader.loadAll(JSON.readTree(Files.readString(irPath)));
        RegelSpraakModel model = loaded.model();
        ClassLoader cl = BeanCompiler.compileer(FactModelGenerator.genereer(model));
        ClassLoader vorige = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(cl);
        try {
            ExecModelTranspiler et = ExecModelTranspiler.build(model, cl, loaded.prioritizedDefaults(),
                    loaded.rolBindingen(), loaded.lets());
            List<String> uit = new ArrayList<>();
            for (Dekking d : et.dekking()) uit.add(d.verdict() + " " + d.sleutel() + " — " + d.reden());
            return uit;
        } finally {
            Thread.currentThread().setContextClassLoader(vorige);
        }
    }
}
