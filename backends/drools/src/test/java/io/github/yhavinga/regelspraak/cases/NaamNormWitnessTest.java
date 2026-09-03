package io.github.yhavinga.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.github.yhavinga.regelspraak.transpiler.kern.Namen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The BOUNDARIES of the shared name contract ({@link Namen#norm} / {@link Namen#gedeclareerdeNaam}), each
 * measured against ref first (2026-09-03).
 *
 * <p>ref is the arbiter, so a name equality WIDER than ref's is not a fix but a new divergence. ref's
 * {@code Checks.norm} is {@code lowercase_ascii (collapse_ws s)}: whitespace collapse over SPACE and TAB
 * only, case folding over A-Z only. The Java one-liner it was first written as ({@code trim() +
 * replaceAll("\\s+", " ") + toLowerCase(Locale.ROOT)}) folds two further axes — a NEWLINE inside a name and
 * a NON-ASCII capital — and on both of them a verifier-clean document that ref and clingo play read
 * "temporal" in the back-end where ref reads "scalar", so the key join met a {@code BigDecimal} bean member
 * and the replay died with a bare {@code ConstraintEvaluationException}. The arity guard, counting the same
 * notion, fired a ch. 5.9 form fault on a shape ref calls valid.
 *
 * <p>The second boundary is the other direction: a key that ref, clingo and the verifier DO resolve
 * ("vondstNummer" for a declared "vondstnummer") still has to be handed to the runtime's property resolver
 * in its DECLARED spelling, because the bean member was generated from the declaration. Of five
 * capitalisations of one key only two used to survive; the other three died with "geen getter voor …".
 *
 * <p>Every model is authored in memory on top of a corpus {@code ir.json} — no corpus file is written, and
 * no expectation is taken from one.
 */
class NaamNormWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");
    private static final Path ROOD = Path.of("..", "..", "corpus", "rood");

    private static final Path STRANDREGISTER = GOLDEN.resolve("anchor/jutterswet-strandregister/ir.json");
    private static final Path NUMMERREGISTER = GOLDEN.resolve("anchor/jutterswet-nummerregister/ir.json");
    private static final Path TWEE_TEMPOREEL = ROOD.resolve("uni-two-temporal-keys/ir.json");

    /** The DRL twin's ch. 5.9 refusal sentinel. */
    private static final String ONOPGELOSTE_SLEUTELVORM =
            "// onopgelost uniciteitscontrole (ch. 5.9: at most one temporal key component)";

    @TempDir static Path tmp;

    // ---------------- 1. the norm contract itself, axis by axis ----------------

    @Test void norm_vouwt_alleen_ascii_kast() {
        // ref: String.lowercase_ascii folds A-Z and nothing else, so "Ö" and "ö" stay DIFFERENT characters
        // and "COÖRDINAAT" is not the name "coördinaat". Measured on the OCaml side, 2026-09-03.
        assertEquals("coÖrdinaat", Namen.norm("COÖRDINAAT"));
        assertFalse(Namen.norm("COÖRDINAAT").equals(Namen.norm("coördinaat")),
                "a non-ASCII capital is NOT case-folded — ref reads two different names");
        assertEquals(Namen.norm("VONDSTNUMMER"), Namen.norm("vondstnummer"), "ASCII case IS folded");
    }

    @Test void norm_splitst_alleen_op_spatie_en_tab() {
        // ref: collapse_ws splits on ' ' and '\t' only. A newline (or \r, \f, \x0B) is a CHARACTER inside
        // the name, and there is no trim beyond dropping empty runs of those two separators.
        assertEquals("vondst\nnummer", Namen.norm("vondst\nnummer"));
        assertFalse(Namen.norm("vondst\nnummer").equals(Namen.norm("vondst nummer")),
                "a newline is not whitespace to ref — the two names differ");
        assertEquals("vondst nummer", Namen.norm("  vondst\t\t nummer  "), "space and tab DO collapse");
        assertEquals("vondstnummer\n", Namen.norm("vondstnummer\n"), "and a trailing newline is not trimmed");
    }

    // ---------------- 2. the norm boundary, played end to end ----------------

    @Test void een_diakritische_kastvariant_is_geen_temporele_sleutel() throws Exception {
        // "coördinaat" is declared as an attribute, "COÖRDINAAT" carries the granulariteit: ref folds neither
        // into the other, so the key is SCALAR and the two scalar 7s collide. ref, measured on this exact
        // model: PASS (2 expectations) — S1 and S2 flagged; clingo agrees. With a Locale-folding norm the
        // key read temporal, the join called rsOoitBotst on a BigDecimal bean member and the replay died:
        //   ERROR ConstraintEvaluationException — cause: "rsOoitWaarden: een temporele sleutel moet een
        //   List<Segment> zijn, kreeg java.math.BigDecimal"
        assertEquals(Set.of("eenmalig nummer @ S1", "eenmalig nummer @ S2"),
                vlaggen(diakriet(), scalairGeval("coördinaat", "7", "7")),
                "ref folds A-Z only, so the granulariteit under another diacritic case is another name");
    }

    @Test void een_newline_in_een_naam_is_geen_scheidingsteken() throws Exception {
        // the same boundary on the whitespace axis: the attribute is declared "vondst nummer", the
        // granulariteit is keyed "vondst\nnummer". ref: PASS (2 expectations); clingo agrees.
        assertEquals(Set.of("eenmalig nummer @ S1", "eenmalig nummer @ S2"),
                vlaggen(newline(), scalairGeval("vondst nummer", "7", "7")),
                "a newline inside a name does not collapse, so the two names are not the same name");
    }

    @Test void de_ariteitsguard_telt_niet_wat_ref_niet_telt() throws Exception {
        // key list ["coördinaat", "kavelnummer"] where only kavelnummer is temporal to ref: ONE temporal
        // component, so the shape is valid. ref, measured: "oracle: PASS (1 expectations)" — consistent
        // (the tuples (7,3) and (9,3) differ); clingo agrees. A wider norm counted two and raised the ch. 5.9
        // form fault on a shape all three executors accept.
        assertEquals(Set.of(), vlaggen(ariteit(), gemengdeSleutelGeval()),
                "the guard must count the temporal components ref counts, not one more");
    }

    // ---------------- 3. the declared spelling reaches the bean ----------------

    @Test void elke_kastvariant_van_een_sleutel_vindt_dezelfde_botsing() throws Exception {
        // the class the card names — "a differently capitalised key" — measured across five spellings on the
        // corpus case (two timelines that both ever carry 7). verifier PASS and ref "oracle: PASS
        // (2 expectations)" on all five; clingo agrees. Only the first two used to play: the temporality test
        // normalised the key but the JOIN handed it to the runtime RAW, so "vondstNummer" pascal-cased into
        // getVondstNummer — a getter the fact class (generated from the DECLARATION) does not have:
        //   ERROR ConstraintEvaluationException — cause: "geen getter voor 'vondstNummer' op …Strandvondst"
        for (String spelling : java.util.List.of(
                "VONDSTNUMMER", "Vondstnummer", "vondstNummer", "VOndstnummer", "vondstnummeR")) {
            Path ir = bewerk(STRANDREGISTER, o -> {
                ((ObjectNode) o.get("uniciteiten").get(0)).set("sleutels", JSON.createArrayNode().add(spelling));
                ((com.fasterxml.jackson.databind.node.ArrayNode) o.get("uniciteiten")).remove(1);
            });
            assertEquals(Set.of("eenmalig nummer @ S1", "eenmalig nummer @ S2"),
                    vlaggen(ir, tweeTijdlijnen()),
                    "spelling '" + spelling + "' names the declared attribute and must flag both instances");
        }
    }

    @Test void een_kastvariant_van_een_uniesleutel_vindt_dezelfde_botsing() throws Exception {
        // the union's own key site resolves through the same contract. ref on this model: PASS (2
        // expectations) — both Strandvondsten flagged within the one source; clingo agrees.
        Path ir = bewerk(NUMMERREGISTER, o -> ((ObjectNode) o.get("uniciteitUnies").get(0)
                .get("bronnen").get(0)).set("sleutels", JSON.createArrayNode().add("vondstNummer")));
        assertEquals(Set.of("eenmalig vondstnummer @ S1", "eenmalig vondstnummer @ S2"),
                vlaggen(ir, tweeTijdlijnen()),
                "a union source's key resolves to its declared spelling before the bean is read");
    }

    // ---------------- 4. the dormant DRL twin holds both conventions ----------------

    @Test void de_drl_tweeling_noemt_het_gedeclareerde_veld() throws Exception {
        // the text route emits a PROPERTY NAME, so the same resolution has to happen there: ident() of the
        // written key would render "vondstNummer", a field the fact class does not carry.
        Path ir = bewerk(STRANDREGISTER, o -> ((ObjectNode) o.get("uniciteiten").get(0))
                .set("sleutels", JSON.createArrayNode().add("vondstNummer")));
        String drl = drl(ir);
        assertTrue(drl.contains("rsOoitBotst(vondstnummer, $k0)"), drl);
        assertFalse(drl.contains("vondstNummer"), "the written spelling must not reach the DRL text: " + drl);
    }

    @Test void de_drl_tweeling_weigert_de_dubbel_temporele_sleutel() throws Exception {
        // ref FAULTS on this shape ("a uniqueness key list carries at most one temporal key component (…)"),
        // clingo raises the same wording at translation and the exec model faults on fire. The text route has
        // no fire moment, so it refuses in ITS loud idiom: the onopgelost sentinel, which books the construct
        // as a MARKER. It used to render two rsOoitBotst joins and say nothing.
        String drl = drl(TWEE_TEMPOREEL);
        assertTrue(drl.contains(ONOPGELOSTE_SLEUTELVORM), drl);
        assertFalse(drl.contains("rsOoitBotst"), "a rejected key shape must not render a join: " + drl);
    }

    @Test void de_drl_tweeling_weigert_de_dubbel_temporele_uniesleutel() throws Exception {
        // the same guard per SOURCE. ref on this model: "EVALUATION FAULT: a uniqueness key list carries at
        // most one temporal key component (…)"; the exec model faults on the "eenmalig vondstnummer
        // (Strandvondst 1)" rule. Both source rules of the twin must carry the sentinel.
        Path ir = bewerk(NUMMERREGISTER, o -> {
            for (JsonNode b : o.get("uniciteitUnies").get(0).get("bronnen"))
                ((ObjectNode) b).set("sleutels", JSON.createArrayNode().add("vondstnummer").add("volgnummer"));
            ((com.fasterxml.jackson.databind.node.ArrayNode) o.get("uniciteitUnies")).remove(1);
        });
        String drl = drl(ir);
        assertEquals(2, tel(drl, ONOPGELOSTE_SLEUTELVORM), "one refusal per source: " + drl);
        assertFalse(drl.contains("rsOoitBotst"), "a rejected key shape must not render a join: " + drl);
    }

    // ---------------- models ----------------

    /** {@code coördinaat} declared, {@code COÖRDINAAT} carrying the granulariteit — the diacritic axis. */
    private static Path diakriet() throws Exception {
        return bewerk(STRANDREGISTER, o -> {
            hernoemAttribuut(o, "coördinaat");
            ((ObjectNode) o.get("objecttypen").get("Strandvondst")).set("attribuutGranulariteit",
                    JSON.createObjectNode().put("COÖRDINAAT", "JAAR").put("kavelnummer", "JAAR"));
            enkeleUniciteit(o, JSON.createArrayNode().add("coördinaat"));
        });
    }

    /** {@code vondst nummer} declared, {@code vondst\nnummer} carrying the granulariteit — the whitespace axis. */
    private static Path newline() throws Exception {
        return bewerk(STRANDREGISTER, o -> {
            hernoemAttribuut(o, "vondst nummer");
            ((ObjectNode) o.get("objecttypen").get("Strandvondst")).set("attribuutGranulariteit",
                    JSON.createObjectNode().put("vondst\nnummer", "JAAR").put("kavelnummer", "JAAR"));
            enkeleUniciteit(o, JSON.createArrayNode().add("vondst nummer"));
        });
    }

    /** The diacritic model with a TWO-component key: scalar {@code coördinaat} + temporal {@code kavelnummer}. */
    private static Path ariteit() throws Exception {
        return bewerk(STRANDREGISTER, o -> {
            hernoemAttribuut(o, "coördinaat");
            ((ObjectNode) o.get("objecttypen").get("Strandvondst")).set("attribuutGranulariteit",
                    JSON.createObjectNode().put("COÖRDINAAT", "JAAR").put("kavelnummer", "JAAR"));
            enkeleUniciteit(o, JSON.createArrayNode().add("coördinaat").add("kavelnummer"));
        });
    }

    /** Rename Strandvondst's {@code vondstnummer} declaration, keeping the declared order. */
    private static void hernoemAttribuut(ObjectNode ir, String nieuw) {
        ObjectNode ot = (ObjectNode) ir.get("objecttypen").get("Strandvondst");
        ObjectNode attributen = (ObjectNode) ot.get("attributen");
        ObjectNode vervangen = JSON.createObjectNode();
        vervangen.set(nieuw, attributen.get("vondstnummer"));
        vervangen.set("kavelnummer", attributen.get("kavelnummer"));
        ot.set("attributen", vervangen);
    }

    /** Keep only the first uniciteit (the scoped second one plays no part here) and give it {@code sleutels}. */
    private static void enkeleUniciteit(ObjectNode ir, JsonNode sleutels) {
        var lijst = (com.fasterxml.jackson.databind.node.ArrayNode) ir.get("uniciteiten");
        while (lijst.size() > 1) lijst.remove(1);
        ((ObjectNode) lijst.get(0)).set("sleutels", sleutels);
    }

    /** A corpus {@code ir.json} with one authored edit, written to a temp file. */
    private static Path bewerk(Path bron, Consumer<ObjectNode> edit) throws Exception {
        ObjectNode ir = (ObjectNode) JSON.readTree(Files.readString(bron));
        edit.accept(ir);
        Path uit = Files.createTempFile(tmp, "naamnorm", ".json");
        Files.writeString(uit, ir.toString());
        return uit;
    }

    /** The dormant DRL back-end's rendering of one model. */
    private static String drl(Path ir) throws Exception {
        return io.github.yhavinga.regelspraak.transpiler.DrlTranspiler.transpile(
                io.github.yhavinga.regelspraak.loader.IrLoader.loadAll(
                        JSON.readTree(Files.readString(ir))).model());
    }

    // ---------------- cases ----------------

    /** Two Strandvondsten carrying {@code attr} as a PLAIN value. */
    private static JsonNode scalairGeval(String attr, String v1, String v2) throws Exception {
        return JSON.readTree("""
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "S1", "objectType": "Strandvondst",
                        "attributes": { "%s": { "number": "%s" } }, "roles": {}, "characteristics": [] },
                      { "label": "S2", "objectType": "Strandvondst",
                        "attributes": { "%s": { "number": "%s" } }, "roles": {}, "characteristics": [] } ],
                    "parameters": {} },
                  "expected": [ { "kind": "consistent" } ] }
                """.formatted(attr, v1, attr, v2));
    }

    /** Two Strandvondsten with a scalar coördinaat (7 / 9) and the SAME kavelnummer timeline. */
    private static JsonNode gemengdeSleutelGeval() throws Exception {
        return JSON.readTree("""
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "S1", "objectType": "Strandvondst",
                        "attributes": { "coördinaat": { "number": "7" },
                            "kavelnummer": { "timeline": [
                                { "from": "2024-01-01", "to": "2025-01-01", "value": { "number": "3" } } ] } },
                        "roles": {}, "characteristics": [] },
                      { "label": "S2", "objectType": "Strandvondst",
                        "attributes": { "coördinaat": { "number": "9" },
                            "kavelnummer": { "timeline": [
                                { "from": "2024-01-01", "to": "2025-01-01", "value": { "number": "3" } } ] } },
                        "roles": {}, "characteristics": [] } ],
                    "parameters": {} },
                  "expected": [ { "kind": "consistent" } ] }
                """);
    }

    /** The corpus shape: two vondstnummer timelines that never overlap in time yet both ever carry 7. */
    private static JsonNode tweeTijdlijnen() throws Exception {
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

    // ---------------- helpers ----------------

    /** The §9.5 channel after fire as {@code "<regel> @ <label>"} — the per-INSTANCE flagging ref pins. */
    private static Set<String> vlaggen(Path ir, JsonNode c) throws Exception {
        CaseRunner.NaFire na = CaseRunner.naFire(ir, c);
        Set<String> uit = new LinkedHashSet<>();
        for (Object f : na.feiten()) {
            if (!"Inconsistentie".equals(f.getClass().getSimpleName())) continue;
            Object subject = f.getClass().getMethod("getSubject").invoke(f);
            String label = "?";
            for (Map.Entry<String, Object> e : na.invoer().entrySet())
                if (e.getValue() == subject) label = e.getKey();
            uit.add(f.getClass().getMethod("getRegel").invoke(f) + " @ " + label);
        }
        return uit;
    }

    private static int tel(String tekst, String naald) {
        int n = 0;
        for (int i = tekst.indexOf(naald); i >= 0; i = tekst.indexOf(naald, i + naald.length())) n++;
        return n;
    }
}
