package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.runtime.RegelSpraakRuntime;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
 * this class keeps what the golden cannot say: the unbound step and the mirrored unnegated twin. It also plays the
 * whole-period forms on this document (RUNTIME-SEMANTICS ch. 3.9, decided 2026-09-26; their own golden is
 * {@code corpus/golden/jutterswet-gehele-periode-leeg}), where both branches refused them by name until that date,
 * and pins the one clause of that decision no case input can reach: a leeg segment touches no bucket.
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

    /** Under the ch. 3.9 whole-period qualifier the inner negation is still per day (decided 2026-09-26): the leeg
     *  and the gevuld test both take the threshold days(B), so the negated leeg test IS the gevuld test and the
     *  negated gevuld test the leeg test, which is exactly the loader's lowering. S3 carries its lengte over the
     *  first half of 2025 only, and that row tells this reading from one that negates the folded verdict:
     *  "gedurende het gehele jaar niet leeg" fails on S3, where "niet gedurende het gehele jaar leeg" holds. Until
     *  that date both branches refused the negated form by name, on the then open threshold. */
    @Test void de_negatie_onder_geheel_periode_speelt_als_de_zustertest() throws Exception {
        speelGeheelJaar("empty", true, false, "S1", "S2", "S4");
        speelGeheelJaar("filled", true, false, "S2", "S4");
    }

    /** The UNNEGATED leeg and gevuld tests under the same qualifier, with and without the qualifier's own
     *  {@code niet}, which complements the folded verdict. S2 and S4 carry no lengte and pass both tests. Until
     *  2026-09-26 both branches refused all four by name, because ref did not execute the fold. */
    @Test void de_ongenegeerde_test_onder_geheel_periode_speelt() throws Exception {
        speelGeheelJaar("empty", false, false, "S2", "S4");
        speelGeheelJaar("empty", false, true, "S1", "S3");
        speelGeheelJaar("filled", false, false, "S1", "S2", "S4");
        speelGeheelJaar("filled", false, true, "S3");
    }

    /** A time-dependent PARAMETER as the operand passes WF-043 and is folded by no executor: the loader refuses it
     *  in the sentence ref and the ASP backend give, so the three executors refuse it with one voice. Before
     *  2026-09-26 the exec-model branch skipped the rule and the DRL-text branch markered it, each in its own
     *  words. */
    @Test void een_parameter_onder_geheel_periode_weigert_in_de_zin_van_ref() throws Exception {
        ObjectNode ir = ir();
        ObjectNode par = ((ArrayNode) ir.get("model").get("parameters")).addObject();
        par.put("node", "Parameter").put("name", "havenpeil");
        par.set("datatype", IrPad.attribute(IrPad.objectType(ir, "Schuit"), "lengte").get("datatype").deepCopy());
        IrPad.timeline(par, "month");
        ObjectNode vw = (ObjectNode) IrPad.versie(ir, "rules", 0, 0).get("condition");
        vw.put("kind", "filled").put("negated", false);
        vw.putObject("operand").put("node", "ParameterReference").put("parameter", "havenpeil")
                .putObject("source").put("snippet", "het havenpeil");
        vw.putObject("wholePeriod").put("node", "WholePeriod").put("granularity", "year").put("negated", false);
        IrLoader.Refusal e = assertThrows(IrLoader.Refusal.class, () -> IrLoader.loadAll(ir));
        assertTrue(e.getMessage().endsWith("Predicate.operand (a whole-period leeg/gevuld test over the parameter"
                + " 'havenpeil' is not executed: the fold reads the timeline of an attribute)"), e.getMessage());
    }

    /** ch. 3.9 reads the touched buckets of the leeg and gevuld tests off {@code read} (ch. 3.1): a leeg segment
     *  reads as a gap and touches no bucket by itself. No case input can carry one — the case format has no leeg
     *  value — and ch. 3.5's temporal sum is what writes them, so the runtime fold is pinned directly. Before
     *  2026-09-26 the gevuld check counted the leeg segment's bucket as touched and failed it. */
    @Test void een_leeg_segment_raakt_geen_emmer() {
        List<Object> alleenLeeg = List.of(new Seg("2025-01-01", "2026-01-01", null));
        assertTrue(RegelSpraakRuntime.rsGeheelPeriode(alleenLeeg, "JAAR", 6, null), "gevuld: no bucket is touched");
        assertTrue(RegelSpraakRuntime.rsGeheelPeriode(alleenLeeg, "JAAR", 8, null), "leeg: every day reads leeg");
        List<Object> halfEnLeeg = List.of(new Seg("2025-01-01", "2025-07-01", "4"),
                new Seg("2025-07-01", "2026-01-01", null));
        assertFalse(RegelSpraakRuntime.rsGeheelPeriode(halfEnLeeg, "JAAR", 6, null), "gevuld: 181 of 365 days");
        assertFalse(RegelSpraakRuntime.rsGeheelPeriode(halfEnLeeg, "JAAR", 8, null), "leeg: a half year of 4");
    }

    /** meldpremie's rule alone, its test set to {@code soort}/{@code negatie} under "gedurende het gehele jaar"
     *  (with the qualifier's own {@code niet}), over a month-granular lengte: S1 carries 8 over the whole of 2025,
     *  S3 carries 5 over its first half, S2 and S4 carry none. meldpremie is 40 on {@code treffers}, leeg on the
     *  others. The ongemeten rule is dropped because it reads the now-temporal lengte without a qualifier and
     *  would be refused for THAT; the werfpremie rule because it reads the werf, not the lengte. */
    private void speelGeheelJaar(String soort, boolean negatie, boolean niet, String... treffers) throws Exception {
        ObjectNode ir = ir();
        IrPad.timeline(IrPad.attribute(IrPad.objectType(ir, "Schuit"), "lengte"), "month");
        IrPad.rules(ir).set("rules", JSON.createArrayNode().add(IrPad.rulesLijst(ir, "rules").get(0)));
        ObjectNode vw = (ObjectNode) IrPad.versie(ir, "rules", 0, 0).get("condition");
        vw.put("kind", soort).put("negated", negatie);
        vw.putObject("wholePeriod").put("node", "WholePeriod").put("granularity", "year").put("negated", niet);
        ObjectNode c = (ObjectNode) geval();
        for (JsonNode inst : c.get("input").get("instances")) {
            String label = inst.get("label").asText();
            String tot = label.equals("S1") ? "2026-01-01" : "2025-07-01";
            if (inst.get("attributes").has("lengte"))
                ((ObjectNode) inst.get("attributes")).set("lengte", JSON.readTree(
                        "{\"timeline\":[{\"from\":\"2025-01-01\",\"to\":\"" + tot + "\",\"value\":"
                                + inst.get("attributes").get("lengte") + "}]}"));
        }
        ArrayNode rijen = JSON.createArrayNode();
        for (String label : new String[] { "S1", "S2", "S3", "S4" }) {
            ObjectNode r = rijen.addObject().put("label", label).put("attribute", "meldpremie");
            if (Arrays.asList(treffers).contains(label)) {
                r.put("kind", "attributeValue");
                r.putObject("value").put("number", "40");
            } else r.put("kind", "attributeEmpty");
        }
        rijen.addObject().put("kind", "consistent");
        c.set("expected", rijen);
        BeideTakken.groen(tijdelijk(ir), c, "whole-period " + soort + (negatie ? " negated" : "")
                + (niet ? ", niet gedurende" : ""));
    }

    /** Shape-faithful segment stand-in for the runtime fold: the {@code van}/{@code tot}/{@code waarde} properties
     *  it reads reflectively, a null waarde being a leeg segment. */
    public static final class Seg {
        private final LocalDate van, tot; private final BigDecimal waarde;
        Seg(String van, String tot, String waarde) {
            this.van = LocalDate.parse(van); this.tot = LocalDate.parse(tot);
            this.waarde = waarde == null ? null : new BigDecimal(waarde);
        }
        public LocalDate getVan() { return van; }
        public LocalDate getTot() { return tot; }
        public BigDecimal getWaarde() { return waarde; }
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
