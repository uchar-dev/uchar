package io.github.yhavinga.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.github.yhavinga.regelspraak.loader.IrLoader;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The NEGATED leeg/gevuld test (IR-SPEC ch. 4 Predicate row, field {@code negated}; RUNTIME-SEMANTICS ch. 1.2),
 * pinned on BOTH transpiler branches. {@code Predicaat{soort: LEEG, negatie: true}} is "is niet leeg" and
 * {@code GEVULD, negatie: true} is "is niet gevuld"; ref reads both as the plain test with the verdict flipped.
 *
 * <p><b>Red before the fix</b> (measured 2026-09-22 on main db224b04 through the backend CLI, on the fixture below
 * cut to one rule per run): exec-model AND DRL-text both answered the UNNEGATED test — every row of every form
 * inverted, for example {@code attribuut meldpremie: verwacht 40, werkelijk leeg; verwacht leeg, werkelijk 40} and
 * {@code kenmerk ongemeten: verwacht afwezig, werkelijk aanwezig}; the navigated form identically, with and
 * without its wire binding. The verifier passes the document and ref PASSes all 13 expectations, so this was a
 * silent wrong answer on a verifier-clean document. The frontend never writes {@code negatie: true} on these two
 * soorten (RegelSpraak has no "is niet leeg"), which is why the loader passed the field through and no lowering
 * ever read it.
 *
 * <p><b>The fixture is a corpus golden</b>, {@code corpus/golden/jutterswet-waardemelding}, read from there. It
 * lived in this suite's resources until the ASP backend carried the three forms it faulted on ({@code condition
 * kind not translated}; card clingo-leeg-negatie, 2026-09-23). Gates 5, 6 and 12 now replay the document itself;
 * this class keeps what the golden cannot say: the unbound step, the mirrored unnegated twin, and the two
 * whole-period refusals (negated and unnegated).
 *
 * <p><b>Hand computation</b> ({@code jutterswet-waardemelding/case.json}): W1 has tarief 312, W2 none. S1 (lengte 8, werf W1),
 * S2 (no lengte, werf W1), S3 (lengte 5, no werf), S4 (no lengte, werf W2). meldpremie = 40 iff lengte is niet
 * leeg: S1, S3 get 40, S2, S4 leeg. ongemeten iff lengte is niet gevuld: S2, S4. werfpremie = 25 iff the
 * tarief of the werf is niet leeg, an absent werf reading as leeg (ch. 1.2): S1, S2 get 25, S3, S4 leeg.
 */
class LeegNegatieWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path FIXTURE = Path.of("..", "..", "corpus", "golden", "jutterswet-waardemelding");

    @Test void de_negatie_speelt_in_beide_takken() throws Exception {
        BeideTakken.groen(FIXTURE.resolve("ir.json"), geval(),
                "niet leeg / niet gevuld on an own attribute and over one bound navigation step");
    }

    /** The same document with the navigation's wire binding removed. Since L2 of the v1 wire migration a read
     *  without bindings IS the subject's own (IR-SPEC ch. 4.1a), and this one names Werf under a Schuit subject —
     *  ill-formed (WF-053) — so no branch walks a relation of its own choosing: the loader refuses the document in
     *  the sentence ref and the verifier share (until L2's advisory round both branches refused the rule with a
     *  generic reason). */
    @Test void de_ongebonden_lezing_van_een_ander_type_weigert_bij_het_laden() throws Exception {
        ObjectNode ir = ir();
        for (JsonNode r : IrPad.rulesLijst(ir, "rules"))
            ((ObjectNode) r.get("versions").get(0).get("condition").get("operand")).remove("path");
        IrLoader.Documentfout e = assertThrows(IrLoader.Documentfout.class, () -> IrLoader.loadAll(ir),
                "no branch walks a relation the wire does not name");
        assertEquals("/rules/rules/2/versions/0/condition/operand", e.plek(), e.getMessage());
        assertTrue(e.getMessage().endsWith("its subjectType is the subject type 'Schuit' — 'Werf' names another type,"
                + " which only a navigation reaches"), e.getMessage());
    }

    /** CONTROL: the unnegated twin must still answer the plain test — every expectation mirrored. A lowering that
     *  flipped the soort without looking at the field would pass the first row and fail this one. */
    @Test void de_ongenegeerde_tweeling_speelt_gespiegeld() throws Exception {
        ObjectNode ir = ir();
        for (JsonNode r : IrPad.rulesLijst(ir, "rules"))
            ((ObjectNode) r.get("versions").get(0).get("condition")).put("negated", false);
        BeideTakken.groen(tijdelijk(ir), gespiegeld(geval()), "leeg / gevuld unnegated");
    }

    /** Under the ch. 3.9 whole-period qualifier the norm is OPEN: {@code negatie} is the inner, per-day negation
     *  (the qualifier's own is {@code GeheelPeriode.niet}), and ch. 3.9 gives the leeg test the touched-days
     *  threshold and every other test days(B) without saying which a NEGATED leeg test takes — under days(B) the
     *  sister swap would be exact, under touched days not. ref faults on every whole-period leeg/gevuld test, so
     *  both branches refuse the negated form by name rather than choose a threshold. */
    @Test void de_negatie_onder_geheel_periode_weigert_in_beide_takken() throws Exception {
        ObjectNode ir = ir();
        IrPad.timeline(IrPad.attribute(IrPad.objectType(ir, "Schuit"), "lengte"), "month");
        // meldpremie's rule alone: the ongemeten rule reads the now-temporal lengte unqualified and would be
        // excluded for THAT, hiding the refusal under test.
        ArrayNode een = JSON.createArrayNode().add(IrPad.rulesLijst(ir, "rules").get(0));
        IrPad.rules(ir).set("rules", een);
        ObjectNode vw = (ObjectNode) IrPad.versie(ir, "rules", 0, 0).get("condition");
        vw.putObject("wholePeriod").put("node", "WholePeriod").put("granularity", "year").put("negated", false);
        Path p = tijdelijk(ir);
        // lengte is now month-granular, so the case supplies it as a timeline (a scalar input would be refused
        // for that reason first and hide the one under test). Before the fix both branches PLAYED this document
        // and answered the unnegated whole-period test: `verwacht 40, werkelijk leeg; verwacht leeg, werkelijk 40`.
        ObjectNode c = (ObjectNode) geval();
        for (JsonNode inst : c.get("input").get("instances"))
            if (inst.get("attributes").has("lengte"))
                ((ObjectNode) inst.get("attributes")).set("lengte", JSON.readTree(
                        "{\"timeline\":[{\"from\":\"2025-01-01\",\"to\":\"2026-01-01\",\"value\":{\"number\":\"8\"}}]}"));
        for (boolean viaDrl : new boolean[] { false, true }) {
            IrLoader.Refusal e = assertThrows(IrLoader.Refusal.class, () -> CaseRunner.speelCase(p, c, viaDrl),
                    (viaDrl ? "drl-tekst" : "exec-model") + " refuses the negated whole-period leeg test");
            assertTrue(e.getMessage().contains("Predicaat.negatie"), e.getMessage());
            assertTrue(e.getMessage().contains("threshold of the negated test is not normed"), e.getMessage());
        }
    }

    /** The UNNEGATED leeg and gevuld tests under the same qualifier, with and without the qualifier's own
     *  {@code niet}. ch. 3.9 norms both (touched days for leeg, days(B) for gevuld), but ref faults on every
     *  whole-period leeg/gevuld test and the ASP backend refuses it in ref's words. Before this refusal both
     *  branches PLAYED all four variants (measured 2026-09-23, card drools-heeltijdvak-leegtest): an answer where
     *  the other two executors refuse by name, with nothing to check it against. */
    @Test void de_ongenegeerde_test_onder_geheel_periode_weigert_in_beide_takken() throws Exception {
        for (String soort : new String[] { "empty", "filled" })
            for (boolean niet : new boolean[] { false, true }) {
                ObjectNode ir = ir();
                IrPad.timeline(IrPad.attribute(IrPad.objectType(ir, "Schuit"), "lengte"), "month");
                IrPad.rules(ir).set("rules",
                        JSON.createArrayNode().add(IrPad.rulesLijst(ir, "rules").get(0)));
                ObjectNode vw = (ObjectNode) IrPad.versie(ir, "rules", 0, 0).get("condition");
                vw.put("kind", soort).put("negated", false);
                vw.putObject("wholePeriod").put("node", "WholePeriod").put("granularity", "year").put("negated", niet);
                Path p = tijdelijk(ir);
                ObjectNode c = (ObjectNode) geval();
                for (JsonNode inst : c.get("input").get("instances"))
                    if (inst.get("attributes").has("lengte"))
                        ((ObjectNode) inst.get("attributes")).set("lengte", JSON.readTree(
                                "{\"timeline\":[{\"from\":\"2025-01-01\",\"to\":\"2026-01-01\",\"value\":{\"number\":\"8\"}}]}"));
                for (boolean viaDrl : new boolean[] { false, true }) {
                    String wat = (viaDrl ? "drl-tekst" : "exec-model") + " refuses whole-period " + soort
                            + (niet ? " (niet gedurende)" : "");
                    IrLoader.Refusal e = assertThrows(IrLoader.Refusal.class, () -> CaseRunner.speelCase(p, c, viaDrl), wat);
                    assertTrue(e.getMessage().contains("Predicaat.periode"), wat + ": " + e.getMessage());
                    assertTrue(e.getMessage().contains("ref executes neither the leeg fold"), wat + ": " + e.getMessage());
                }
            }
    }

    // ---------------- fixtures ----------------

    private static ObjectNode ir() throws Exception {
        return (ObjectNode) JSON.readTree(Files.readString(FIXTURE.resolve("ir.json")));
    }

    private static JsonNode geval() throws Exception {
        return JSON.readTree(Files.readString(FIXTURE.resolve("case.json")));
    }

    private static Path tijdelijk(ObjectNode ir) throws Exception {
        Path tmp = Files.createTempFile("leeg-negatie", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir.toString());
        return tmp;
    }

    /** Every rule in the fixture writes a constant under a one-predicate condition, so the unnegated outcome is
     *  the negated one with each row flipped: a value becomes leeg and back, present becomes absent and back. */
    private static JsonNode gespiegeld(JsonNode c) {
        ObjectNode uit = c.deepCopy();
        ArrayNode rijen = (ArrayNode) uit.get("expected");
        JsonNode waarden = JSON.createObjectNode()
                .set("meldpremie", JSON.createObjectNode().put("number", "40"));
        ((ObjectNode) waarden).set("werfpremie", JSON.createObjectNode().put("number", "25"));
        for (JsonNode rij : rijen) {
            ObjectNode r = (ObjectNode) rij;
            switch (r.get("kind").asText()) {
                case "attributeValue" -> { r.put("kind", "attributeEmpty"); r.remove("value"); }
                case "attributeEmpty" -> { r.put("kind", "attributeValue"); r.set("value", waarden.get(r.get("attribute").asText())); }
                case "characteristicPresent" -> r.put("kind", "characteristicAbsent");
                case "characteristicAbsent" -> r.put("kind", "characteristicPresent");
                case "consistent" -> { }
                default -> throw new IllegalStateException("unmirrored row kind " + r.get("kind"));
            }
        }
        return uit;
    }
}
