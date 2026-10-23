package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.uchar.regelspraak.engine.BeanCompiler;
import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.schema.FactModelGenerator;
import dev.uchar.regelspraak.transpiler.core.Coverage;
import dev.uchar.regelspraak.transpiler.core.Timelines;
import dev.uchar.regelspraak.transpiler.tms.TmsTranspiler;

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
 * {@code timelineCount.containsKey(member)}, at 24 sites. The wire's count defaults to ONE (IR-SPEC §2.2's TimelineFacet
 * table states it as {@code count >= 1}, "1 = single-period", and ch. 2.8's worked example adopts {@code count: 1};
 * ch. 2.10's v0 migration note reads a bare
 * granularity as {@code count: 1}; the verifier's WF-005 substitutes 1 for an absent entry), so an EXPLICIT 1 is the
 * same facet as no entry at all and a raster starts at 2. The presence test therefore refused a document that says out
 * loud what every other document says by default.
 *
 * <p>It stayed invisible because under v0 the RegelSpraak frontend wrote the entry only for a count other than 1
 * ({@code DefinitionResolver}), so no frontend-produced document carried it at 1. Measured over the corpus on the
 * PRE-FIX jar — an explicit 1 injected on one temporal member at a time, both branches, against each branch's own
 * untouched status line: 32 of 46 member positions flipped out of the played channel, over 12 of the 17 goldens with
 * temporal members and case files, while the verifier, {@code ref} and clingo stayed unchanged on the very same
 * documents (verifier PASS; {@code oracle: PASS}; {@code clingo: PASS}, same pinned expectations).
 *
 * <p>Every model below is SYNTHETIC, built in memory from a golden's {@code ir.json} off the wire; the corpus files are
 * read and never written. Each test names the position it gates and the PRE-FIX measurement it reverses.
 *
 * <p><b>Since M1 (2026-09-23) the explicit 1 is the corpus's own.</b> Every {@code TimelineFacet} writes its count and
 * the IR loader keeps it, 1 included, as the OCaml loader does, so each temporal member of a loaded golden reaches the
 * predicate with an explicit entry. The five replays below therefore replay the golden as written ({@link #withCount}
 * with 1 restates its count and asserts the facet is there), and a regression to the presence test fails them, which
 * it did not while the loader dropped the 1: then the tables held no entry and the replays were hollow. Measured on
 * this change with the presence test put back: 7 of the 8 tests fail; the count-2 flank passes under both.
 *
 * <p>The tests that replay ({@link #playBothBranches}) cannot go quietly true — a probe document that stopped
 * carrying its construct would change the played status or the case count. The §8.2 test compares OUTPUTS instead,
 * and that shape can: it stood on {@code jutterswet-vormenkabinet} until that cabinet shrank (2026-09-19) and
 * stopped reading its temporal attribute, after which it compared two identical documents that exercised nothing.
 * It now stands on a replaying golden and asserts up front that the construct is still emitted.
 */
class MorePeriodicGateWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** cwd under surefire is the module dir (backends/drools); the corpus is two levels up. */
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    /** The §8.2 qualifier over a NAVIGATED temporal characteristic ("de werf van de Schuit gedurende het gehele jaar
     *  bedrijvig is"), four cases, both branches PLAYED. */
    private static final String WHOLE_YEAR_CHARACTERISTIC = "synth-gehelejaarkenmerk";
    /** A §7.3.2 tijdsevenredig deel into a temporal doel, clipped by the subject's OWN temporal kenmerk, totalled by
     *  a §7.1 "het totaal van". One case, both branches PLAYED. */
    private static final String CLIPPRORATA = "synth2-clip-over-prorata";
    /** A §7.1 "het totaal van" over a temporal attribute, no clip. One case, both branches PLAYED. */
    private static final String TEMPOTOTAAL = "jutterswet-tempototaal";
    /** A §8.2 whole-period comparison over the MAAND-granular {@code keurvergoeding}, quantified over a navigated
     *  set. Its facet writes count 1 and the loader keeps it, so the §8.2 test below compares the loaded table WITH
     *  that entry against the same table with the entry removed — the removal is the probe's; an absent entry is
     *  what the v0 wire wrote. */
    private static final String KEURPLOEG = "jutterswet-keurploeg";

    // ---------------- the predicate itself ----------------

    @Test void a_count_of_one_is_no_grid() {
        assertFalse(Timelines.morePeriodic(java.util.Map.of(), "waterstand"), "afwezig = enkelvoudig");
        assertFalse(Timelines.morePeriodic(java.util.Map.of("waterstand", 1), "waterstand"),
                "een expliciete 1 is dezelfde facet als geen sleutel (IR-SPEC §2.2/2.8/2.10, WF-005)");
        assertTrue(Timelines.morePeriodic(java.util.Map.of("waterstand", 2), "waterstand"),
                "een raster begint bij twee");
        assertFalse(Timelines.morePeriodic(java.util.Map.of("waterstand", 0), "waterstand"),
                "een count < 1 is WF-005's weigering, niet die van dit predicaat — zo'n document haalt geen enkele poort");
    }

    // ---------------- the kenmerk side ----------------

    @Test void the_navigated_characteristic_time_sequence_plays_at_count_one() throws Exception {
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
        playBothBranches(withCount(WHOLE_YEAR_CHARACTERISTIC, "Werf", "characteristics", "bedrijvig", 1),
                WHOLE_YEAR_CHARACTERISTIC, 4);
    }

    @Test void the_own_characteristic_time_sequence_plays_at_count_one() throws Exception {
        // temporeelKenmerkTijdreeks — the subject's OWN kenmerk on the §7.1 clip of a temporal write. PRE-FIX, with
        // Jutter.kenmerkTijdlijnAantal {"borgplichtig": 1}:
        //     Drools exec-model : EXCLUDE transpiler SKIP: regel: borgdeel van een jutter — regel (temporele
        //                         toekenning buiten Phase-F scope)
        //     Drools DRL-tekst  : EXCLUDE drl-transpiler MARKER: regel: borgdeel van een jutter — TODO temporele
        //                         toekenning
        playBothBranches(withCount(CLIPPRORATA, "Jutter", "characteristics", "borgplichtig", 1),
                CLIPPRORATA, 1);
    }

    // ---------------- the attribute side: the card's question 3, measured ----------------

    @Test void the_temporal_target_plays_at_count_one() throws Exception {
        // The §5.1.4 temporal-doel write AND the §13.3.6.7 multi-period marker beside it — the branch pair that reads
        // the count TWICE, once negated and once plain. PRE-FIX, with Jutter.attribuutTijdlijnAantal {"borgdeel": 1}:
        //     Drools exec-model : EXCLUDE transpiler SKIP: regel: borgdeel van een jutter — regel (meer-periodieke
        //                         temporele doel — §13.3.6.7, geen producent)
        //     Drools DRL-tekst  : EXCLUDE drl-transpiler MARKER: regel: borgdeel van een jutter — TODO meer-periodieke
        //                         temporele toekenning (§13.3.6.7)
        // The same injection also crosses the §7.1 "het totaal van zijn borgdeel" fold in the second rule.
        playBothBranches(withCount(CLIPPRORATA, "Jutter", "attributes", "borgdeel", 1),
                CLIPPRORATA, 1);
    }

    @Test void the_time_proportional_base_plays_at_count_one() throws Exception {
        // The §7.3.2 tijdsevenredig-deel BASE operand. PRE-FIX, with Jutter.attribuutTijdlijnAantal {"borgtarief": 1}:
        //     Drools exec-model : EXCLUDE transpiler SKIP: regel: borgdeel van een jutter — regel (temporele
        //                         toekenning buiten Phase-F scope)
        //     Drools DRL-tekst  : EXCLUDE drl-transpiler MARKER: regel: borgdeel van een jutter — TODO temporele
        //                         toekenning
        playBothBranches(withCount(CLIPPRORATA, "Jutter", "attributes", "borgtarief", 1),
                CLIPPRORATA, 1);
    }

    @Test void the_total_of_a_time_sequence_plays_at_count_one() throws Exception {
        // The §7.1 "het totaal van" fold, the one temporal expression that collapses to a scalar. PRE-FIX, with
        // Jutter.attribuutTijdlijnAantal {"bewaarvergoeding per maand": 1}:
        //     Drools exec-model : EXCLUDE transpiler SKIP: regel: bewaargeld van een jutter — regel (aggregatie
        //                         buiten Phase-C scope)
        //     Drools DRL-tekst  : EXCLUDE drl-transpiler MARKER: regel: bewaargeld van een jutter — TODO totaal van
        //                         een meer-periodieke tijdreeks (§13.3.6.7, voor elke <n> maanden)
        playBothBranches(withCount(TEMPOTOTAAL, "Jutter", "attributes", "bewaarvergoeding per maand", 1),
                TEMPOTOTAAL, 1);
    }

    @Test void the_whole_period_comparison_does_not_read_the_count_at_all() throws Exception {
        // The card's question 3 on the position a REPLAYING corpus document occupies: jutterswet-keurploeg states a
        // §8.2 whole-period matrix (a QuantifierFold) over the MAAND-granular keurvergoeding. Measured rather than
        // assumed: THIS position routes through the fold's whole-period arm, whose gate elementTijdlijnLid
        // (TmsTranspiler's tijdlid, and DrlTranspiler's until card drl-teksttak-op-truth-maintenance) DOES consult the
        // count table — and must classify an explicit 1 as single-periodic, where the PRE-FIX containsKey test refused
        // it. That is exactly why the mutant kills this test, and it makes this a stronger witness than the cabinet
        // position ever was (that one never consulted the table at all). The SET gates
        // (isTemporeelSetAttribuut/isTemporeelSet82) are reached by no corpus document: every §8.2 operand in golden
        // carries set:false (re-measured 2026-09-19). Verdict compared byte for byte on the DRL text and construct for
        // construct on both coverage manifests. Since M1 every facet writes its count and the loader keeps it, 1
        // included: the loaded table carries the explicit 1 of the wire, and the probe REMOVES it for the other side —
        // the absent entry the v0 wire wrote. It asserts the entry was the wire's 1; otherwise both sides would be the
        // same document.
        Path keurploeg = ir(KEURPLOEG);
        java.util.function.Consumer<RegelSpraakModel> without = m -> assertEquals(1,
                m.objectTypes.get("Keurmeester").attributeTimelineCount.remove("keurvergoeding"),
                "de geladen teltabel draagt de 1 van de wire voor keurvergoeding");
        java.util.function.Consumer<RegelSpraakModel> with = m -> { };
        String drl = drlText(keurploeg, without);
        // The guard that keeps this witness from emptying silently: it pins that the document under probe still
        // EMITS the construct being gated. This test once stood on jutterswet-vormenkabinet and went trivially true
        // when that cabinet shrank (2026-09-19) and stopped reading its temporal attribute at all — no gate saw it.
        assertTrue(drl.contains("ComputeHelpers.wholePeriod("),
                "het probe-document moet de §8.2-vergelijking nog emitteren, anders is de gelijkheid hieronder leeg");
        assertEquals(drl, drlText(keurploeg, with),
                "de §8.2-vergelijking leest het periodeaantal niet — de DRL is identiek met en zonder de sleutel");
        assertEquals(coverage(keurploeg, without), coverage(keurploeg, with),
                "en de dekking van de exec-model-tak eveneens");
    }

    // ---------------- the over-acceptance flank ----------------

    @Test void a_real_grid_stays_refused() throws Exception {
        // Without this, deleting the gate outright would satisfy every test above. A count of TWO is a §13.3.6.7
        // multi-period raster whose producer semantics no branch carries, and BOTH must keep refusing it by name, in
        // the one sentence of the analysis they render (until card drl-teksttak-op-truth-maintenance the DRL-text
        // branch refused it in sentences of its own lowering: "§8.2 gehele-periode" and "meer-periodieke temporele
        // toekenning (§13.3.6.7)").
        Path characteristic = withCount(WHOLE_YEAR_CHARACTERISTIC, "Werf", "characteristics", "bedrijvig", 2);
        assertTrue(tmsReason(characteristic, WHOLE_YEAR_CHARACTERISTIC).contains("regel: toeslag van een schuit — een CharacteristicCheck"
                        + " met wholePeriod ('de werf van de Schuit gedurende het gehele jaar bedrijvig is') over het kenmerk"
                        + " 'bedrijvig' van Werf op een raster van meer perioden, dat geen bewerking draagt"),
                tmsReason(characteristic, WHOLE_YEAR_CHARACTERISTIC));
        assertEquals(tmsReason(characteristic, WHOLE_YEAR_CHARACTERISTIC), drlReason(characteristic, WHOLE_YEAR_CHARACTERISTIC));

        Path attribute = withCount(CLIPPRORATA, "Jutter", "attributes", "borgdeel", 2);
        assertTrue(tmsReason(attribute, CLIPPRORATA).contains("op een raster van meer perioden, dat geen bewerking draagt"),
                tmsReason(attribute, CLIPPRORATA));
        assertEquals(tmsReason(attribute, CLIPPRORATA), drlReason(attribute, CLIPPRORATA));
    }

    // ---------------- fixtures ----------------

    /** Both branches replay the golden's OWN case files against the golden's OWN pinned expectations — so a pass here
     *  is the differential square on a document only the count key distinguishes from the played original.
     *
     *  <p>The exec branch is driven case by case through {@link CaseRunner#playCase} rather than through the
     *  multi-case entry beside it, and that is not a style choice: gate 1's rename tripwire greps for the pre-rename
     *  vocabulary of 2026-08-29, one token of which sits inside that other method's NAME, and the tripwire's
     *  reason-carrying allowance list (a protected script) names the two production files only. A test that called
     *  it would turn gate 1 red — measured, not guessed. */
    private static void playBothBranches(Path ir, String golden, int cases) throws Exception {
        List<Path> casePaths = casePaths(golden);
        assertEquals(cases, casePaths.size(), "het aantal cases van " + golden);
        for (Path c : casePaths)
            assertEquals(null, CaseRunner.playCase(ir, JSON.readTree(Files.readString(c))),
                    "exec-model op " + c.getFileName());
        CaseRunner.Result drl = CaseRunner.viaDrlText(ir, casePaths);
        assertEquals("PLAYED", drl.status(), "DRL-tekst: " + drl.reason() + " " + drl.caseFailures());
        assertEquals(cases, drl.casesPlayed());
    }

    private static String tmsReason(Path ir, String golden) {
        JsonNode c;
        try {
            c = JSON.readTree(Files.readString(casePaths(golden).get(0)));
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
        return assertThrows(CaseRunner.Excluded.class,
                () -> CaseRunner.playCase(ir, c, DroolsBackend.Branch.EXEC_MODEL),
                "verwacht een weigering van de exec-model-tak").getMessage();
    }

    private static String drlReason(Path ir, String golden) {
        CaseRunner.Result r = CaseRunner.viaDrlText(ir, casePaths(golden));
        assertEquals("EXCLUDE", r.status(), "verwacht een weigering, kreeg " + r.status());
        return r.reason();
    }

    private static Path ir(String golden) {
        Path p = CORPUS.resolve(golden).resolve("ir.json");
        assertTrue(Files.exists(p), "golden ir.json present: " + p.toAbsolutePath());
        return p;
    }

    private static List<Path> casePaths(String golden) {
        List<Path> out = new ArrayList<>();
        try (var s = Files.list(CORPUS.resolve(golden))) {
            s.filter(p -> p.getFileName().toString().startsWith("case")).sorted().forEach(out::add);
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
        assertFalse(out.isEmpty(), "golden carries case files: " + golden);
        return out;
    }

    /** The golden's ir.json with ONE member's period count written explicitly on its timeline facet; {@code members}
     *  names the member list, {@code attributes} or {@code characteristics}. Since M1 a facet always carries its
     *  count, so for 1 this restates what the golden writes; the guard that the facet is there stays. */
    private static Path withCount(String golden, String objectType, String members, String memberName, int count) throws Exception {
        ObjectNode d = (ObjectNode) JSON.readTree(Files.readString(ir(golden)));
        ObjectNode ot = IrPath.objectType(d, objectType);
        ObjectNode member = switch (members) {
            case "attributes" -> IrPath.attribute(ot, memberName);
            case "characteristics" -> IrPath.characteristic(ot, memberName);
            default -> throw new IllegalArgumentException("no member list '" + members + "'");
        };
        assertTrue(member.path("timeline").isObject(), golden + ": " + objectType + "." + memberName + " carries a timeline facet");
        ((ObjectNode) member.get("timeline")).put("count", count);
        return temporary(golden + "-" + memberName + "-" + count, d);
    }

    private static Path temporary(String name, JsonNode d) throws Exception {
        Path tmp = Files.createTempFile(name, ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, d.toString());
        return tmp;
    }

    /** The DRL TEXT of the DRL-text branch for a document, the analysis rendered as text — no case, no fire;
     *  {@code intervention} edits the loaded model first. */
    private static String drlText(Path irPath, java.util.function.Consumer<RegelSpraakModel> intervention) throws Exception {
        IrLoader.Loaded loaded = IrLoader.loadAll(JSON.readTree(Files.readString(irPath)));
        RegelSpraakModel model = loaded.model();
        intervention.accept(model);
        ClassLoader cl = BeanCompiler.compile(FactModelGenerator.generate(model));
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(cl);
        try {
            return TmsTranspiler.build(model, cl, loaded.prioritizedDefaults(), loaded.roleBindings(), loaded.lets(),
                    loaded.sourcePaths(), loaded.navigationBindings(), loaded.anchored()).drl();
        } finally {
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    /** The exec-model branch's coverage manifest for a document, as comparable text; {@code intervention} edits
     *  the loaded model first. */
    private static List<String> coverage(Path irPath, java.util.function.Consumer<RegelSpraakModel> intervention) throws Exception {
        IrLoader.Loaded loaded = IrLoader.loadAll(JSON.readTree(Files.readString(irPath)));
        RegelSpraakModel model = loaded.model();
        intervention.accept(model);
        ClassLoader cl = BeanCompiler.compile(FactModelGenerator.generate(model));
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(cl);
        try {
            TmsTranspiler et = TmsTranspiler.build(model, cl, loaded.prioritizedDefaults(),
                    loaded.roleBindings(), loaded.lets(), loaded.sourcePaths(), loaded.navigationBindings(),
                    loaded.anchored());
            List<String> out = new ArrayList<>();
            for (Coverage d : et.coverage()) out.add(d.verdict() + " " + d.key() + " — " + d.reason());
            return out;
        } finally {
            Thread.currentThread().setContextClassLoader(previous);
        }
    }
}
