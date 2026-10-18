package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The two MATRIX FORMS of the quantifier fold that the exec-model branch did not carry until
 * 2026-09-19 — a {@code Samengesteld} composite (at any depth) and a comparison whose bound is a
 * Parameter — replayed through BOTH transpiler branches, plus the refusals that remain.
 *
 * <p><b>Why beside gate 12.</b> Gate 12 replays every golden through the EXEC-MODEL branch only, and
 * gate 13 drives the DRL-text branch over the goldens it can twist — the unit-bearing ones, which
 * neither of these documents is. So the DRL-text lowering of these two forms has no gate that would
 * notice it breaking, and the two branches carrying the SAME set is exactly what this card was about.
 * This is that gate: both branches, the corpus's own hand-computed expectations on both sides.
 *
 * <p><b>Why the mutants.</b> PLAYED alone is not a witness — a branch that silently lowers nothing
 * writes nothing, and every cell expecting a characteristic ABSENT then passes by itself. Each mutant
 * below flips a PRESENT expectation, so only a branch that really evaluates the composite (its own
 * quantifier, its own threshold) or really reads the bound can reject it.
 *
 * <p>The corpus files are READ and never written; every mutation and every spliced document is
 * written to a {@link TempDir} first.
 */
class FoldMatrixFormWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** cwd under surefire is the module dir (backends/drools); the corpus is two levels up. */
    private static final Path COMPOUND =
            Path.of("..", "..", "corpus", "golden", "jutterswet-redderkeur");
    private static final Path PARAMETER =
            Path.of("..", "..", "corpus", "golden", "jutterswet-loodsgrens");

    @TempDir Path tmp;

    private int n;

    private static List<Path> casesOf(Path golden) throws IOException {
        try (Stream<Path> s = Files.list(golden)) {
            return s.filter(p -> p.getFileName().toString().startsWith("case")).sorted().toList();
        }
    }

    /** Both branches play every case of {@code golden} with the pinned expectations. */
    private void playBothBranches(Path golden) throws Exception {
        List<Path> cases = casesOf(golden);
        assertFalse(cases.isEmpty(), "de golden hoort casebestanden te dragen");
        Path ir = golden.resolve("ir.json");

        CaseRunner.Result exec = CaseRunner.viaExecModel(ir, cases);
        assertEquals("PLAYED", exec.status(), "de exec-model-tak hoort deze matrixvorm te loweren: " + exec.reason());
        assertEquals(cases.size(), exec.casesPlayed(), "elke cel hoort gespeeld te zijn, niet overgeslagen");

        CaseRunner.Result drl = CaseRunner.viaDrlText(ir, cases);
        assertEquals("PLAYED", drl.status(), "de DRL-tekst-tak hoort haar even goed te loweren: " + drl.reason());
        assertEquals(cases.size(), drl.casesPlayed(), "elke cel hoort gespeeld te zijn, niet overgeslagen");
    }

    /** A copy of {@code caseFile} with the named characteristic's PRESENT expectation flipped to absent. */
    private Path flipped(Path caseFile, String label, String characteristic) throws Exception {
        ObjectNode doc = (ObjectNode) JSON.readTree(Files.readString(caseFile));
        boolean touched = false;
        for (JsonNode e : doc.withArray("expected"))
            if ("characteristicPresent".equals(e.path("kind").asText())
                    && characteristic.equals(e.path("characteristic").asText())
                    && label.equals(e.path("label").asText())) {
                ((ObjectNode) e).put("kind", "characteristicAbsent");
                touched = true;
            }
        assertTrue(touched, "de cel hoort '" + characteristic + "' AANWEZIG te verwachten op " + label
                + " — anders muteert deze rij niets");
        Path mutant = tmp.resolve("mutant-" + (n++) + ".json");
        Files.writeString(mutant, doc.toPrettyString());
        return mutant;
    }

    private void rejectBothBranches(Path ir, Path mutant) throws Exception {
        assertEquals("FAIL", CaseRunner.viaExecModel(ir, List.of(mutant)).status(),
                "de exec-model-tak hoort de omgeklapte verwachting te verwerpen");
        assertEquals("FAIL", CaseRunner.viaDrlText(ir, List.of(mutant)).status(),
                "de DRL-tekst-tak hoort hem even goed te verwerpen");
    }

    // ---------------- form 1: a COMPOSITE matrix ----------------

    /**
     * {@code corpus/golden/jutterswet-redderkeur} puts §13.4.14's composite at ch. 3.9a's {@code test}
     * position: an ALLE composite under an ALLE fold and under a GEEN fold, a TEN HOOGSTE composite, and
     * a TEN MINSTE composite whose second bullet is itself a composite. The composite's quantifier and the
     * FOLD's quantifier are different at every rule, which is what keeps them from collapsing into one.
     */
    @Test void both_branches_play_the_compound_matrix() throws Exception {
        playBothBranches(COMPOUND);
    }

    /**
     * The nested bullet is what decides case 2: neither redder satisfies the outer TEN MINSTE on the
     * first bullet alone — one is carried by the nested ALLE composite, the other by the bullet beside
     * it. A branch that flattened the nesting, or read the inner quantifier as the outer, fails here.
     */
    @Test void a_mutant_on_the_nested_bullet_is_rejected_by_both_branches() throws Exception {
        Path ir = COMPOUND.resolve("ir.json");
        rejectBothBranches(ir, flipped(
                COMPOUND.resolve("case2-de-geneste-bullet-beslist.json"), "P1", "geoefend"));
    }

    /** The composite's own THRESHOLD, from the other side: case 1's TEN HOOGSTE 1 composite is what makes
     *  `wisselend` false there while the two ALLE folds beside it hold. Flipping the cell that DOES hold
     *  ({@code paraat}) pins that the composite is evaluated at all. */
    @Test void a_mutant_on_the_compound_threshold_is_rejected_by_both_branches() throws Exception {
        Path ir = COMPOUND.resolve("ir.json");
        rejectBothBranches(ir, flipped(
                COMPOUND.resolve("case1-alle-bullets-halen.json"), "P1", "paraat"));
    }

    /**
     * FOUR levels of composite inside one matrix — ALLE ⊃ TEN MINSTE ⊃ ALLE ⊃ GEEN — spliced into the
     * corpus model. The corpus golden carries three; this row exists because the lowering is recursive
     * and a depth limit would not show up in it. The three posts are chosen so that a DIFFERENT level
     * decides each: P2's redder fails the innermost GEEN, P3's fails the outermost bullet, and P1's two
     * redders reach the same verdict by different roads through the TEN MINSTE.
     *
     * <p>Measured on this document before it was pinned here: verifier PASS, {@code ref} PASS (4
     * expectations), clingo PASS (4 expectations) — so the two Drools branches are being held to the
     * same answer the two OCaml executors give, not merely to each other.
     */
    @Test void four_compound_layers_play_in_both_branches() throws Exception {
        Path ir = deepCompound();
        Path c = tmp.resolve("diep-case.json");
        Files.writeString(c, DEEP_CASE);
        BothBranches.green(ir, JSON.readTree(Files.readString(c)),
                "vier lagen samenstelling in één matrix");
    }

    // ---------------- form 2: a PARAMETER as the bound ----------------

    /**
     * {@code corpus/golden/jutterswet-loodsgrens} compares the contributor's own attribute with a
     * PARAMETER under all three fold quantifiers, and writes the bound on both sides of the comparison.
     * Its first three cases share one instance set and differ only in the parameter's value.
     */
    @Test void both_branches_play_the_parameter_bound() throws Exception {
        playBothBranches(PARAMETER);
    }

    /**
     * The bound is READ, not merely present. Cases 1–3 of that golden carry the same three loodsen under
     * keurgrens 4, 6 and 9; a branch that ignored the bound answers all three alike, and they agree on no
     * column. Flipping case 1's {@code bekwaam} is the direct form of that claim.
     */
    @Test void a_mutant_on_the_parameter_bound_is_rejected_by_both_branches() throws Exception {
        Path ir = PARAMETER.resolve("ir.json");
        rejectBothBranches(ir, flipped(PARAMETER.resolve("case1-grens-vier.json"), "L1", "bekwaam"));
    }

    // ---------------- what the destructive exec-model branch refused, and the branch with truth maintenance plays ----

    /**
     * The eval that asks a non-temporal matrix rides AFTER the {@code collectList} accumulate and binds
     * the collected list plus at most ONE further pattern variable, so a matrix reading TWO different
     * Parameters has nowhere to put the second. That is a structural limit of this branch, not of the
     * IR: the document is verifier-clean and BOTH OCaml executors answer it (measured: verifier PASS,
     * {@code ref} PASS, clingo PASS), and the DRL-text branch plays it, because there a global is a free
     * name in the constraint text and needs no variable at all.
     *
     * <p>That refusal was the destructive exec-model branch's. The exec-model branch with truth maintenance plays the
     * form as {@code ref} answers, on this row's case ({@code TmsLikeRefWitnessTest}, {@code matrix-twee-parameters}).
     */
    @Test void two_parameters_in_a_matrix_plays_with_truth_maintenance() throws Exception {
        Path ir = twoParameters();
        Path c = tmp.resolve("twee-param-case.json");
        Files.writeString(c, TWO_PARAM_CASE);

        CaseRunner.Result tms = CaseRunner.viaExecModel(ir, List.of(c));
        assertEquals("PLAYED", tms.status(), "de exec-model-tak antwoordt zoals ref: " + tms);

        CaseRunner.Result drl = CaseRunner.viaDrlText(ir, List.of(c));
        assertEquals("PLAYED", drl.status(),
                "de DRL-tekst-tak draagt deze vorm wel — de resterende takongelijkheid: " + drl.reason());
    }

    /**
     * The bound this branch carries is the §5.6 GETALACHTIG one — the value path binds a numeric global as
     * a {@code BigDecimal} on the pattern. A TEKST parameter is a differently-typed global and is refused
     * here, naming the operand rather than the node: measured, the DRL-text branch plays it (a global is a
     * free name there) and so does {@code ref}, while the clingo backend refuses it on its own axis, so
     * this form has no corpus witness to be had and stays a named refusal on both sides of the register.
     * That refusal was the destructive exec-model branch's; the exec-model branch with truth maintenance plays the form
     * as {@code ref} answers, on this row's case ({@code TmsLikeRefWitnessTest}, {@code matrix-tekstparameter}).
     */
    @Test void a_text_parameter_as_bound_plays_with_truth_maintenance() throws Exception {
        Path ir = textParameter();
        Path c = tmp.resolve("tekst-param-case.json");
        Files.writeString(c, TEXT_PARAM_CASE);

        CaseRunner.Result tms = CaseRunner.viaExecModel(ir, List.of(c));
        assertEquals("PLAYED", tms.status(), "de exec-model-tak antwoordt zoals ref: " + tms);

        CaseRunner.Result drl = CaseRunner.viaDrlText(ir, List.of(c));
        assertEquals("PLAYED", drl.status(),
                "de DRL-tekst-tak draagt deze vorm wel — de resterende takongelijkheid: " + drl.reason());
    }

    // ================= the spliced documents =================

    /** The composite golden's document with ONE rule whose matrix is four composites deep. */
    private Path deepCompound() throws Exception {
        ObjectNode doc = (ObjectNode) JSON.readTree(Files.readString(COMPOUND.resolve("ir.json")));
        keepRules(doc, "paraat bij ploegkeur", "redderstal van een reddingspost");
        ((ObjectNode) IrPath.version(doc, "rules", "paraat bij ploegkeur").get("condition")).set("test", JSON.readTree(DEEP_MATRIX));
        return write(doc, "diep-ir");
    }

    /** The parameter golden's document with a SECOND parameter and one rule reading both in one matrix. */
    private Path twoParameters() throws Exception {
        ObjectNode doc = (ObjectNode) JSON.readTree(Files.readString(PARAMETER.resolve("ir.json")));
        keepRules(doc, "bekwaam bij keurgrens", "loodstal van een loodspost");
        IrPath.modelList(doc, "parameters").add(JSON.readTree("""
                { "node": "Parameter", "name": "bovengrens",
                  "datatype": { "node": "Number", "whole": true, "decimals": 0,
                                "sign": "nonNegative" } }"""));
        ((ObjectNode) IrPath.version(doc, "rules", "bekwaam bij keurgrens").get("condition")).set("test", JSON.readTree(TWO_PARAM_MATRIX));
        return write(doc, "twee-param-ir");
    }

    /** The parameter golden's document with a TEKST attribute, a TEKST parameter, and one rule comparing
     *  the two as the fold's matrix. */
    private Path textParameter() throws Exception {
        ObjectNode doc = (ObjectNode) JSON.readTree(Files.readString(PARAMETER.resolve("ir.json")));
        keepRules(doc, "bekwaam bij keurgrens", "loodstal van een loodspost");
        IrPath.addAttribute(IrPath.objectType(doc, "Loods"), "rang", JSON.readTree("{ \"node\": \"Text\" }"));
        IrPath.modelList(doc, "parameters").add(JSON.readTree("""
                { "node": "Parameter", "name": "keurrang", "datatype": { "node": "Text" } }"""));
        ((ObjectNode) IrPath.version(doc, "rules", "bekwaam bij keurgrens").get("condition")).set("test", JSON.readTree(TEXT_PARAM_MATRIX));
        return write(doc, "tekst-param-ir");
    }

    private static void keepRules(ObjectNode doc, String... names) {
        List<String> keep = List.of(names);
        com.fasterxml.jackson.databind.node.ArrayNode out = JSON.createArrayNode();
        for (JsonNode r : IrPath.rulesList(doc, "rules")) if (keep.contains(r.path("name").asText())) out.add(r);
        if (out.size() != keep.size()) throw new AssertionError("not every rule of " + keep + " is in the document");
        IrPath.rules(doc).set("rules", out);
    }

    private Path write(ObjectNode doc, String naam) throws Exception {
        Path p = tmp.resolve(naam + "-" + (n++) + ".json");
        Files.writeString(p, doc.toPrettyString());
        return p;
    }

    /** ALLE[ TEN MINSTE 1[ ALLE[ GEEN[ dj &gt; 9, c &gt; 20 ], c &gt; 7 ], gediplomeerd ], dj &gt; 2 ]. */
    private static final String DEEP_MATRIX = """
            { "node": "Compound", "quantifier": "all", "threshold": 0, "parts": [
                { "node": "Compound", "quantifier": "atLeast", "threshold": 1, "parts": [
                    { "node": "Compound", "quantifier": "all", "threshold": 0, "parts": [
                        { "node": "Compound", "quantifier": "none", "threshold": 0, "parts": [
                            { "node": "Comparison", "op": "greater",
                              "left": { "node": "AttributeReference", "attribute": "dienstjaren",
                                         "subjectType": "Redder", "source": { "snippet": "zijn dienstjaren" } },
                              "right": { "node": "Literal", "value": { "number": "9" }, "type": { "node": "Number", "whole": true, "decimals": 0,
                                                    "sign": "unspecified" }, "source": { "snippet": "9" } },
                              "source": { "snippet": "zijn dienstjaren groter zijn dan 9" } },
                            { "node": "Comparison", "op": "greater",
                              "left": { "node": "AttributeReference", "attribute": "conditie",
                                         "subjectType": "Redder", "source": { "snippet": "zijn conditie" } },
                              "right": { "node": "Literal", "value": { "number": "20" }, "type": { "node": "Number", "whole": true, "decimals": 0,
                                                    "sign": "unspecified" }, "source": { "snippet": "20" } },
                              "source": { "snippet": "zijn conditie groter is dan 20" } } ],
                          "source": { "snippet": "hij aan geen van de volgende voorwaarden voldoet" } },
                        { "node": "Comparison", "op": "greater",
                          "left": { "node": "AttributeReference", "attribute": "conditie",
                                     "subjectType": "Redder", "source": { "snippet": "zijn conditie" } },
                          "right": { "node": "Literal", "value": { "number": "7" }, "type": { "node": "Number", "whole": true, "decimals": 0,
                                                "sign": "unspecified" }, "source": { "snippet": "7" } },
                          "source": { "snippet": "zijn conditie groter is dan 7" } } ],
                      "source": { "snippet": "hij aan alle volgende voorwaarden voldoet" } },
                    { "node": "CharacteristicCheck", "characteristic": "gediplomeerd", "negated": false,
                      "source": { "snippet": "hij gediplomeerd is" } } ],
                  "source": { "snippet": "hij aan ten minste 1 van de volgende voorwaarden voldoet" } },
                { "node": "Comparison", "op": "greater",
                  "left": { "node": "AttributeReference", "attribute": "dienstjaren", "subjectType": "Redder",
                             "source": { "snippet": "zijn dienstjaren" } },
                  "right": { "node": "Literal", "value": { "number": "2" }, "type": { "node": "Number", "whole": true, "decimals": 0,
                                        "sign": "unspecified" }, "source": { "snippet": "2" } },
                  "source": { "snippet": "zijn dienstjaren groter zijn dan 2" } } ],
              "source": { "snippet": "hij aan alle volgende voorwaarden voldoet" } }
            """;

    /**
     * P1's two redders reach WAAR by different roads (R1 through the nested ALLE, R2 through the
     * characteristic beside it), P2's fails the innermost GEEN (12 &gt; 9), P3's fails the outermost
     * bullet (1 &gt; 2 is onwaar) while every inner level holds.
     */
    private static final String DEEP_CASE = """
            { "format": "uchar-case-v0", "asOfDate": null,
              "input": { "instances": [
                  { "label": "P1", "objectType": "Reddingspost", "attributes": {},
                    "roles": { "redders": [ "R1", "R2" ] }, "characteristics": [] },
                  { "label": "P2", "objectType": "Reddingspost", "attributes": {},
                    "roles": { "redders": [ "R3" ] }, "characteristics": [] },
                  { "label": "P3", "objectType": "Reddingspost", "attributes": {},
                    "roles": { "redders": [ "R4" ] }, "characteristics": [] },
                  { "label": "R1", "objectType": "Redder",
                    "attributes": { "dienstjaren": { "number": "8" }, "conditie": { "number": "9" } },
                    "roles": {}, "characteristics": [] },
                  { "label": "R2", "objectType": "Redder",
                    "attributes": { "dienstjaren": { "number": "4" }, "conditie": { "number": "3" } },
                    "roles": {}, "characteristics": [ "gediplomeerd" ] },
                  { "label": "R3", "objectType": "Redder",
                    "attributes": { "dienstjaren": { "number": "12" }, "conditie": { "number": "9" } },
                    "roles": {}, "characteristics": [] },
                  { "label": "R4", "objectType": "Redder",
                    "attributes": { "dienstjaren": { "number": "1" }, "conditie": { "number": "9" } },
                    "roles": {}, "characteristics": [ "gediplomeerd" ] } ], "parameters": {} },
              "expected": [ { "kind": "characteristicPresent", "label": "P1", "characteristic": "paraat" },
                            { "kind": "characteristicAbsent", "label": "P2", "characteristic": "paraat" },
                            { "kind": "characteristicAbsent", "label": "P3", "characteristic": "paraat" },
                            { "kind": "consistent" } ] }
            """;

    /** ALLE[ ervaring &gt; keurgrens, ervaring &lt; bovengrens ] — one composite, TWO parameters. */
    private static final String TWO_PARAM_MATRIX = """
            { "node": "Compound", "quantifier": "all", "threshold": 0, "parts": [
                { "node": "Comparison", "op": "greater",
                  "left": { "node": "AttributeReference", "attribute": "ervaring", "subjectType": "Loods",
                             "source": { "snippet": "zijn ervaring" } },
                  "right": { "node": "ParameterReference", "parameter": "keurgrens",
                              "source": { "snippet": "de keurgrens" } },
                  "source": { "snippet": "zijn ervaring groter is dan de keurgrens" } },
                { "node": "Comparison", "op": "less",
                  "left": { "node": "AttributeReference", "attribute": "ervaring", "subjectType": "Loods",
                             "source": { "snippet": "zijn ervaring" } },
                  "right": { "node": "ParameterReference", "parameter": "bovengrens",
                              "source": { "snippet": "de bovengrens" } },
                  "source": { "snippet": "zijn ervaring kleiner is dan de bovengrens" } } ],
              "source": { "snippet": "hij aan alle volgende voorwaarden voldoet" } }
            """;

    /** rang == keurrang — a NON-numeric parameter as the bound. */
    private static final String TEXT_PARAM_MATRIX = """
            { "node": "Comparison", "op": "equal",
              "left": { "node": "AttributeReference", "attribute": "rang", "subjectType": "Loods",
                         "source": { "snippet": "zijn rang" } },
              "right": { "node": "ParameterReference", "parameter": "keurrang",
                          "source": { "snippet": "de keurrang" } },
              "source": { "snippet": "zijn rang gelijk is aan de keurrang" } }
            """;

    private static final String TEXT_PARAM_CASE = """
            { "format": "uchar-case-v0", "asOfDate": null,
              "input": { "instances": [
                  { "label": "L1", "objectType": "Loodspost", "attributes": {},
                    "roles": { "loodsen": [ "A", "B" ] }, "characteristics": [] },
                  { "label": "A", "objectType": "Loods",
                    "attributes": { "ervaring": { "number": "8" }, "rang": { "text": "schipper" } },
                    "roles": {}, "characteristics": [] },
                  { "label": "B", "objectType": "Loods",
                    "attributes": { "ervaring": { "number": "6" }, "rang": { "text": "schipper" } },
                    "roles": {}, "characteristics": [] } ],
                "parameters": { "keurgrens": { "number": "4" }, "keurrang": { "text": "schipper" } } },
              "expected": [ { "kind": "attributeValue", "label": "L1", "attribute": "loodstal",
                              "value": { "number": "2" } },
                            { "kind": "characteristicPresent", "label": "L1", "characteristic": "bekwaam" },
                            { "kind": "consistent" } ] }
            """;

    private static final String TWO_PARAM_CASE = """
            { "format": "uchar-case-v0", "asOfDate": null,
              "input": { "instances": [
                  { "label": "L1", "objectType": "Loodspost", "attributes": {},
                    "roles": { "loodsen": [ "A", "B" ] }, "characteristics": [] },
                  { "label": "A", "objectType": "Loods", "attributes": { "ervaring": { "number": "8" } },
                    "roles": {}, "characteristics": [] },
                  { "label": "B", "objectType": "Loods", "attributes": { "ervaring": { "number": "6" } },
                    "roles": {}, "characteristics": [] } ],
                "parameters": { "keurgrens": { "number": "4" }, "bovengrens": { "number": "9" } } },
              "expected": [ { "kind": "attributeValue", "label": "L1", "attribute": "loodstal",
                              "value": { "number": "2" } },
                            { "kind": "characteristicPresent", "label": "L1", "characteristic": "bekwaam" },
                            { "kind": "consistent" } ] }
            """;
}
