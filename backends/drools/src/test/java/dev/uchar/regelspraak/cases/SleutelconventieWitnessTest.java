package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The two KEY CONVENTIONS of the ch. 5.9 uniqueness lowering, each measured against ref first
 * (2026-09-03) and each pre-existing:
 *
 * <ol>
 *   <li><b>Name normalisation.</b> A uniqueness key names an attribute in another CAPITALISATION than the
 *       declaration. The IR spells a name as the author wrote it, ref and clingo compare declared names
 *       normalised ({@code Checks.norm} / {@code Checks.attr_temporal}) and the verifier passes the document,
 *       so this is a VALID one-component model — but the Drools key sites tested temporality with an exact
 *       {@code Map.containsKey}, read the key as scalar, and then joined two {@code List<Segment>} timelines
 *       with {@code equals} (Segment has no {@code equals}): every collision ref and clingo find went
 *       silently missing, and the ch. 5.9 arity guard stayed silent on a shape ref FAULTS on.</li>
 *   <li><b>The cross-source mixed union.</b> Each source of a "verenigd met" union declares its OWN key list,
 *       so one component can be temporal on source A and scalar on source B. ref plays it (per-source
 *       {@code ever_tuples}: the temporal source contributes one tuple per ever-value, the scalar source one
 *       tuple with its value) and clingo agrees; Drools crashed with a {@code ConstraintEvaluationException}
 *       — an ERROR, which under the gate-12 regime is never a valid not-carried outcome.</li>
 * </ol>
 *
 * <p>Every model here is authored in memory on top of a corpus {@code ir.json} (no corpus file is written or
 * read for its expectations), and every expectation was measured through ref and clingo BEFORE it was pinned.
 *
 * <p>The witnesses read the §9.5 channel through {@link CaseRunner#naFire}, not through
 * {@link CaseRunner#speelCase}: for a rule-name key the case comparator matches on the RULE NAME ALONE (the
 * subject the exec model attaches may legitimately differ from ref's), so an {@code inconsistent} expectation
 * naming S2 is already satisfied by a flag on S1 — a comparator seam too blunt to see a half-found collision.
 * {@link #vlaggen} pins the exact {@code regel @ label} SET, which is what ref pins per instance.
 */
class SleutelconventieWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");
    private static final Path ROOD = Path.of("..", "..", "corpus", "rood");

    private static final Path STRANDREGISTER = GOLDEN.resolve("anchor/jutterswet-strandregister/ir.json");
    private static final Path NUMMERREGISTER = GOLDEN.resolve("anchor/jutterswet-nummerregister/ir.json");
    private static final Path TWEE_TEMPOREEL = ROOD.resolve("uni-two-temporal-keys/ir.json");

    /** The shared ch. 5.9 key-shape wording, byte-equal to ref's ({@code eval.ml, ever_tuples}). */
    private static final String TE_VEEL = "a uniqueness key list carries at most one temporal key component";

    @TempDir static Path tmp;

    // ---------------- 1. name normalisation: the single form ----------------

    @Test void een_anders_gekapitaliseerde_sleutel_vindt_dezelfde_botsing() throws Exception {
        // ref, measured 2026-09-03 on this exact model: PASS (2 expectations) — S1 and S2 both flagged;
        // clingo agrees. Before the convention was pulled straight the backend answered "consistent":
        // "Vondstnummer" missed the granulariteit map, the scalar branch compared two List<Segment> with
        // equals, and a Segment carries no equals — so not even the instance's own key matched itself.
        Path ir = metSleutels(STRANDREGISTER, o -> IrPad.bron(o, 0, 0).set("keys", JSON.createArrayNode().add("Vondstnummer")));
        assertEquals(Set.of("eenmalig nummer @ S1", "eenmalig nummer @ S2"),
                vlaggen(ir, tweeStrandvondsten()),
                "a differently capitalised key names the SAME declared attribute (ref's Checks.norm)");
    }

    @Test void de_kale_tweeling_van_de_enkelvoudige_sleutel() throws Exception {
        // the bare twin: the corpus model untouched, so the only difference with the arm above is the
        // capitalisation of the key. Both must flag the same two instances.
        assertEquals(Set.of("eenmalig nummer @ S1", "eenmalig nummer @ S2"),
                vlaggen(STRANDREGISTER, tweeStrandvondsten()),
                "the declared spelling is the same model and must flag the same instances");
    }

    // ---------------- 2. name normalisation: the union form ----------------

    @Test void een_anders_gekapitaliseerde_uniesleutel_vindt_dezelfde_botsing() throws Exception {
        // the union's own key site. TWO Strandvondsten and NO Wrakvondst, deliberately: with a Wrakvondst in
        // play the mis-read source still counts ITSELF (equals on the identical list object) and the sum
        // crosses 1 anyway, so the gap hides. ref, measured on this model: PASS (2 expectations); clingo
        // agrees. Before the fix the backend answered "consistent".
        Path ir = metSleutels(NUMMERREGISTER, o -> IrPad.bron(o, 0, 0).set("keys", JSON.createArrayNode().add("Vondstnummer")));
        assertEquals(Set.of("eenmalig vondstnummer @ S1", "eenmalig vondstnummer @ S2"),
                vlaggen(ir, tweeStrandvondsten()),
                "a union source's key is the same declared attribute in another capitalisation");
    }

    @Test void de_kale_tweeling_van_de_uniesleutel() throws Exception {
        assertEquals(Set.of("eenmalig vondstnummer @ S1", "eenmalig vondstnummer @ S2"),
                vlaggen(NUMMERREGISTER, tweeStrandvondsten()),
                "the declared spelling flags the same two instances within one source");
    }

    // ---------------- 3. name normalisation: the ch. 5.9 arity guard ----------------

    @Test void een_anders_gekapitaliseerde_sleutel_telt_mee_voor_de_ariteit() throws Exception {
        // the guard counts the TEMPORAL components of a key list; counting them with an exact match made a
        // mis-capitalised component invisible, so a two-temporal key list read as one and ran on. ref on this
        // model: "EVALUATION FAULT: a uniqueness key list carries at most one temporal key component (…)";
        // clingo raises the same wording at translation.
        Path ir = metSleutels(TWEE_TEMPOREEL, o -> IrPad.bron(o, 0, 0).set("keys", JSON.createArrayNode().add("Vondstnummer").add("kavelnummer")));
        Throwable t = assertThrows(RuntimeException.class, () -> vlaggen(ir, tweeTemporeleSleutels()));
        assertTrue(oorzaakKeten(t).contains(TE_VEEL),
                "the arity guard must count a mis-capitalised component too, got: " + oorzaakKeten(t));
    }

    @Test void de_kale_tweeling_van_de_ariteitsguard() throws Exception {
        Throwable t = assertThrows(RuntimeException.class, () -> vlaggen(TWEE_TEMPOREEL, tweeTemporeleSleutels()));
        assertTrue(oorzaakKeten(t).contains(TE_VEEL),
                "the declared spelling faults on the same shape, got: " + oorzaakKeten(t));
    }

    // ---------------- 4. the cross-source mixed union ----------------

    @Test void een_kruis_bron_gemengde_unie_vindt_de_botsing() throws Exception {
        // vondstnummer is temporal on Strandvondst and scalar on Wrakvondst — verifier-clean, and ref plays
        // it: PASS (2 expectations), S1 and W1 both flagged; clingo agrees. Before the fix BOTH mixed arms
        // were wrong, and the loud one crashed the whole replay:
        //   ERROR ConstraintEvaluationException — cause: "rsOoitWaarden: een temporele sleutel moet een
        //   List<Segment> zijn, kreeg java.math.BigDecimal"
        // (the Wrakvondst rule binds a scalar key and joins it against Strandvondst's timeline), while the
        // mirror arm (the Strandvondst rule joining its timeline against Wrakvondst's scalar) silently
        // missed. The flag set pins BOTH arms: S1 comes from the second, W1 from the first.
        assertEquals(Set.of("eenmalig vondstnummer @ S1", "eenmalig vondstnummer @ W1"),
                vlaggen(gemengdeUnie(), gemengdGeval("7", "7")),
                "the scalar source claims its value, the temporal source every value it EVER carries");
    }

    @Test void een_kruis_bron_gemengde_unie_zonder_botsing_zwijgt() throws Exception {
        // the sharp twin of the arm above: the same mixed shape with 7 against 8. ref: PASS (1 expectation,
        // consistent); clingo agrees. Before the fix this crashed too — the ERROR was unconditional, so a
        // "carry" that flagged everything would look just as green on the colliding case alone.
        assertEquals(Set.of(), vlaggen(gemengdeUnie(), gemengdGeval("7", "8")),
                "a scalar that is not among the timeline's ever-values claims nothing");
    }

    @Test void de_kale_tweeling_van_de_gemengde_unie() throws Exception {
        // the unmixed twin: the corpus model, where vondstnummer is temporal on BOTH sources, with the same
        // two labels colliding on 7. Same flags — the mixed lowering must not shift the pure form.
        assertEquals(Set.of("eenmalig vondstnummer @ S1", "eenmalig vondstnummer @ W1"),
                vlaggen(NUMMERREGISTER, JSON.readTree("""
                        { "format": "uchar-case-v0", "asOfDate": null,
                          "input": { "instances": [
                              { "label": "S1", "objectType": "Strandvondst",
                                "attributes": { "vondstnummer": { "timeline": [
                                    { "from": "2024-01-01", "to": "2025-01-01", "value": { "number": "7" } } ] } },
                                "roles": {}, "characteristics": [] },
                              { "label": "W1", "objectType": "Wrakvondst",
                                "attributes": { "vondstnummer": { "timeline": [
                                    { "from": "2026-01-01", "to": "2027-01-01", "value": { "number": "7" } } ] } },
                                "roles": {}, "characteristics": [] } ],
                            "parameters": {} },
                          "expected": [ { "kind": "consistent" } ] }
                        """)),
                "two timelines that ever carry 7 collide across the sources, as they always did");
    }

    // ---------------- 5. the dormant DRL twin carries the same two conventions ----------------

    @Test void de_drl_tweeling_leest_een_anders_gekapitaliseerde_sleutel_temporeel() throws Exception {
        // the DRL back-end is the golden-text twin; it is not fired by any gate, so its two key sites have no
        // behavioural witness — only the emitted text can say which join it chose. An exact-match temporality
        // test rendered "vondstnummer == $k0", the List-identity compare that never fires.
        Path ir = metSleutels(STRANDREGISTER, o -> IrPad.bron(o, 0, 0).set("keys", JSON.createArrayNode().add("Vondstnummer")));
        String drl = drl(ir);
        assertTrue(drl.contains("rsOoitBotst(vondstnummer, $k0)"), drl);
        assertTrue(!drl.contains("vondstnummer == $k0"), "the mis-capitalised key must not render a scalar join: " + drl);
    }

    @Test void de_drl_tweeling_normaliseert_ook_de_uniesleutel() throws Exception {
        // the union has its OWN key site in the twin, and it reads the pair: with an exact match the
        // mis-capitalised source turns the whole union MIXED (one arm rendering rsOoitBevat, the other a
        // scalar ==) where both sources are in truth temporal. Mutation-checked: without this arm a
        // containsKey restored at that one site survives every other witness here.
        Path ir = metSleutels(NUMMERREGISTER, o -> IrPad.bron(o, 0, 0).set("keys", JSON.createArrayNode().add("Vondstnummer")));
        String drl = drl(ir);
        assertTrue(!drl.contains("rsOoitBevat"), "both sources are temporal — no mixed arm may appear: " + drl);
        assertTrue(!drl.contains("vondstnummer == $k0"), "nor a scalar join: " + drl);
    }

    @Test void de_drl_tweeling_rendert_de_gemengde_unie_in_beide_richtingen() throws Exception {
        // both mixed arms, in one union: the Strandvondst rule counts Wrakvondsten with its own timeline as
        // the haystack, the Wrakvondst rule counts Strandvondsten with its own value as the needle.
        String drl = drl(gemengdeUnie());
        assertTrue(drl.contains("Wrakvondst( rsOoitBevat($k0, vondstnummer) )"), drl);
        assertTrue(drl.contains("Strandvondst( rsOoitBevat(vondstnummer, $k0) )"), drl);
    }

    // ---------------- models ----------------

    /** The dormant DRL back-end's rendering of one model. */
    private static String drl(Path ir) throws Exception {
        dev.uchar.regelspraak.loader.IrLoader.Loaded l =
                dev.uchar.regelspraak.loader.IrLoader.loadAll(JSON.readTree(Files.readString(ir)));
        return dev.uchar.regelspraak.transpiler.DrlTranspiler.build(l.model(), l.lets(), l.prioritizedDefaults(),
                l.bronPaden(), l.rolBindingen(), l.navigatieBindingen()).drl();
    }

    /** {@code vondstnummer} temporal on Strandvondst, SCALAR on Wrakvondst: the cross-source mixed key. The
     *  document stays verifier-clean (measured 2026-09-03) — each source declares its own key list. */
    private static Path gemengdeUnie() throws Exception {
        return metSleutels(NUMMERREGISTER, o -> IrPad.attribute(IrPad.objectType(o, "Wrakvondst"), "vondstnummer")
                .remove("timeline"));
    }

    /** A corpus {@code ir.json} with one authored edit, written to a temp file — the corpus file itself is
     *  never touched (the mutation lives in this test, not in the shared zone). */
    private static Path metSleutels(Path bron, Consumer<ObjectNode> edit) throws Exception {
        ObjectNode ir = (ObjectNode) JSON.readTree(Files.readString(bron));
        edit.accept(ir);
        Path uit = Files.createTempFile(tmp, "sleutelconventie", ".json");
        Files.writeString(uit, ir.toString());
        return uit;
    }

    // ---------------- cases ----------------

    /** Two Strandvondsten whose vondstnummer timelines never overlap in TIME yet both ever carry 7 — the
     *  ch. 5.9 ever-reading, within one type and (through the union) within one source. */
    private static JsonNode tweeStrandvondsten() throws Exception {
        return JSON.readTree("""
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "S1", "objectType": "Strandvondst",
                        "attributes": { "vondstnummer": { "timeline": [
                            { "from": "2024-01-01", "to": "2025-01-01", "value": { "number": "7" } } ] } },
                        "roles": {}, "characteristics": [] },
                      { "label": "S2", "objectType": "Strandvondst",
                        "attributes": { "vondstnummer": { "timeline": [
                            { "from": "2026-01-01", "to": "2027-01-01", "value": { "number": "7" } } ] } },
                        "roles": {}, "characteristics": [] } ],
                    "parameters": {} },
                  "expected": [ { "kind": "consistent" } ] }
                """);
    }

    /** One Strandvondst supplying BOTH temporal key components — enough to reach the arity guard, which
     *  judges the SHAPE and needs no collision. */
    private static JsonNode tweeTemporeleSleutels() throws Exception {
        return JSON.readTree("""
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "S1", "objectType": "Strandvondst",
                        "attributes": {
                            "vondstnummer": { "timeline": [
                                { "from": "2024-01-01", "to": "2025-01-01", "value": { "number": "1" } } ] },
                            "kavelnummer": { "timeline": [
                                { "from": "2024-01-01", "to": "2025-01-01", "value": { "number": "2" } } ] } },
                        "roles": {}, "characteristics": [] } ],
                    "parameters": {} },
                  "expected": [ { "kind": "consistent" } ] }
                """);
    }

    /** A Strandvondst carrying {@code s} over one period and a Wrakvondst carrying the plain value {@code w}. */
    private static JsonNode gemengdGeval(String s, String w) throws Exception {
        return JSON.readTree("""
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "S1", "objectType": "Strandvondst",
                        "attributes": { "vondstnummer": { "timeline": [
                            { "from": "2024-01-01", "to": "2025-01-01", "value": { "number": "%s" } } ] } },
                        "roles": {}, "characteristics": [] },
                      { "label": "W1", "objectType": "Wrakvondst",
                        "attributes": { "vondstnummer": { "number": "%s" } },
                        "roles": {}, "characteristics": [] } ],
                    "parameters": {} },
                  "expected": [ { "kind": "consistent" } ] }
                """.formatted(s, w));
    }

    // ---------------- helpers ----------------

    /** The §9.5 channel after fire as {@code "<regel> @ <label>"} — the per-INSTANCE flagging ref pins. */
    private static Set<String> vlaggen(Path ir, JsonNode c) throws Exception {
        CaseRunner.NaFire na = CaseRunner.naFire(ir, c);
        Set<String> uit = new LinkedHashSet<>();
        for (Object f : na.feiten()) {
            if (!"Inconsistentie".equals(f.getClass().getSimpleName())) continue;
            Object subject = lees(f, "getSubject");
            String label = "?";
            for (var e : na.invoer().entrySet()) if (e.getValue() == subject) label = e.getKey();
            uit.add(lees(f, "getRegel") + " @ " + label);
        }
        return uit;
    }

    /** A no-arg read on a CHILD-loaded bean (the fact classes are not on this test's classpath). */
    private static Object lees(Object bean, String getter) throws Exception {
        return bean.getClass().getMethod(getter).invoke(bean);
    }

    /** The whole cause chain as one string — Drools wraps a consequence throw. */
    private static String oorzaakKeten(Throwable t) {
        StringBuilder sb = new StringBuilder();
        for (Throwable c = t; c != null; c = c.getCause() == c ? null : c.getCause())
            sb.append(c).append(" | ");
        return sb.toString();
    }
}
