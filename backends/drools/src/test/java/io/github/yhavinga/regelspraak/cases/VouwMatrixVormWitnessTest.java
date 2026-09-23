package io.github.yhavinga.regelspraak.cases;

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
class VouwMatrixVormWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** cwd under surefire is the module dir (backends/drools); the corpus is two levels up. */
    private static final Path SAMENGESTELD =
            Path.of("..", "..", "corpus", "golden", "jutterswet-redderkeur");
    private static final Path PARAMETER =
            Path.of("..", "..", "corpus", "golden", "jutterswet-loodsgrens");

    @TempDir Path tmp;

    private int n;

    private static List<Path> casesVan(Path golden) throws IOException {
        try (Stream<Path> s = Files.list(golden)) {
            return s.filter(p -> p.getFileName().toString().startsWith("case")).sorted().toList();
        }
    }

    /** Both branches play every case of {@code golden} with the pinned expectations. */
    private void beideTakkenSpelen(Path golden) throws Exception {
        List<Path> cases = casesVan(golden);
        assertFalse(cases.isEmpty(), "de golden hoort casebestanden te dragen");
        Path ir = golden.resolve("ir.json");

        CaseRunner.Result exec = CaseRunner.viaExecModel(ir, cases);
        assertEquals("PLAYED", exec.status(), "de exec-model-tak hoort deze matrixvorm te loweren: " + exec.reason());
        assertEquals(cases.size(), exec.casesPlayed(), "elke cel hoort gespeeld te zijn, niet overgeslagen");

        CaseRunner.Result drl = CaseRunner.viaDrlTekst(ir, cases);
        assertEquals("PLAYED", drl.status(), "de DRL-tekst-tak hoort haar even goed te loweren: " + drl.reason());
        assertEquals(cases.size(), drl.casesPlayed(), "elke cel hoort gespeeld te zijn, niet overgeslagen");
    }

    /** A copy of {@code caseBestand} with the named characteristic's PRESENT expectation flipped to absent. */
    private Path omgeklapt(Path caseBestand, String label, String kenmerk) throws Exception {
        ObjectNode doc = (ObjectNode) JSON.readTree(Files.readString(caseBestand));
        boolean geraakt = false;
        for (JsonNode e : doc.withArray("expected"))
            if ("characteristicPresent".equals(e.path("kind").asText())
                    && kenmerk.equals(e.path("characteristic").asText())
                    && label.equals(e.path("label").asText())) {
                ((ObjectNode) e).put("kind", "characteristicAbsent");
                geraakt = true;
            }
        assertTrue(geraakt, "de cel hoort '" + kenmerk + "' AANWEZIG te verwachten op " + label
                + " — anders muteert deze rij niets");
        Path mutant = tmp.resolve("mutant-" + (n++) + ".json");
        Files.writeString(mutant, doc.toPrettyString());
        return mutant;
    }

    private void beideTakkenVerwerpen(Path ir, Path mutant) throws Exception {
        assertEquals("FAIL", CaseRunner.viaExecModel(ir, List.of(mutant)).status(),
                "de exec-model-tak hoort de omgeklapte verwachting te verwerpen");
        assertEquals("FAIL", CaseRunner.viaDrlTekst(ir, List.of(mutant)).status(),
                "de DRL-tekst-tak hoort hem even goed te verwerpen");
    }

    // ---------------- form 1: a COMPOSITE matrix ----------------

    /**
     * {@code corpus/golden/jutterswet-redderkeur} puts §13.4.14's composite at ch. 3.9a's {@code test}
     * position: an ALLE composite under an ALLE fold and under a GEEN fold, a TEN HOOGSTE composite, and
     * a TEN MINSTE composite whose second bullet is itself a composite. The composite's quantifier and the
     * FOLD's quantifier are different at every rule, which is what keeps them from collapsing into one.
     */
    @Test void beide_takken_spelen_de_samengestelde_matrix() throws Exception {
        beideTakkenSpelen(SAMENGESTELD);
    }

    /**
     * The nested bullet is what decides case 2: neither redder satisfies the outer TEN MINSTE on the
     * first bullet alone — one is carried by the nested ALLE composite, the other by the bullet beside
     * it. A branch that flattened the nesting, or read the inner quantifier as the outer, fails here.
     */
    @Test void een_mutant_op_de_geneste_bullet_wordt_door_beide_takken_verworpen() throws Exception {
        Path ir = SAMENGESTELD.resolve("ir.json");
        beideTakkenVerwerpen(ir, omgeklapt(
                SAMENGESTELD.resolve("case2-de-geneste-bullet-beslist.json"), "P1", "geoefend"));
    }

    /** The composite's own THRESHOLD, from the other side: case 1's TEN HOOGSTE 1 composite is what makes
     *  `wisselend` false there while the two ALLE folds beside it hold. Flipping the cell that DOES hold
     *  ({@code paraat}) pins that the composite is evaluated at all. */
    @Test void een_mutant_op_de_samengestelde_drempel_wordt_door_beide_takken_verworpen() throws Exception {
        Path ir = SAMENGESTELD.resolve("ir.json");
        beideTakkenVerwerpen(ir, omgeklapt(
                SAMENGESTELD.resolve("case1-alle-bullets-halen.json"), "P1", "paraat"));
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
    @Test void vier_lagen_samenstelling_spelen_in_beide_takken() throws Exception {
        Path ir = diepeSamenstelling();
        Path c = tmp.resolve("diep-case.json");
        Files.writeString(c, DIEP_CASE);
        BeideTakken.groen(ir, JSON.readTree(Files.readString(c)),
                "vier lagen samenstelling in één matrix");
    }

    // ---------------- form 2: a PARAMETER as the bound ----------------

    /**
     * {@code corpus/golden/jutterswet-loodsgrens} compares the contributor's own attribute with a
     * PARAMETER under all three fold quantifiers, and writes the bound on both sides of the comparison.
     * Its first three cases share one instance set and differ only in the parameter's value.
     */
    @Test void beide_takken_spelen_de_parametergrens() throws Exception {
        beideTakkenSpelen(PARAMETER);
    }

    /**
     * The bound is READ, not merely present. Cases 1–3 of that golden carry the same three loodsen under
     * keurgrens 4, 6 and 9; a branch that ignored the bound answers all three alike, and they agree on no
     * column. Flipping case 1's {@code bekwaam} is the direct form of that claim.
     */
    @Test void een_mutant_op_de_parametergrens_wordt_door_beide_takken_verworpen() throws Exception {
        Path ir = PARAMETER.resolve("ir.json");
        beideTakkenVerwerpen(ir, omgeklapt(PARAMETER.resolve("case1-grens-vier.json"), "L1", "bekwaam"));
    }

    // ---------------- what the exec branch still refuses, and names ----------------

    /**
     * The eval that asks a non-temporal matrix rides AFTER the {@code collectList} accumulate and binds
     * the collected list plus at most ONE further pattern variable, so a matrix reading TWO different
     * Parameters has nowhere to put the second. That is a structural limit of this branch, not of the
     * IR: the document is verifier-clean and BOTH OCaml executors answer it (measured: verifier PASS,
     * {@code ref} PASS, clingo PASS), and the DRL-text branch plays it, because there a global is a free
     * name in the constraint text and needs no variable at all.
     *
     * <p>The assertion is that the refusal is BY NAME and on the right axis — the two parameters — never
     * the node class and never silence.
     */
    @Test void twee_parameters_in_een_matrix_weigert_de_exec_tak_bij_naam() throws Exception {
        Path ir = tweeParameters();
        Path c = tmp.resolve("twee-param-case.json");
        Files.writeString(c, TWEE_PARAM_CASE);

        CaseRunner.Result exec = CaseRunner.viaExecModel(ir, List.of(c));
        assertEquals("EXCLUDE", exec.status(), "een niet-gelowerde vouw mag nooit meespelen");
        assertTrue(exec.reason().contains("twee verschillende parameters"),
                "de exec-reden noemt de as waarop zij afketst, niet de knoopklasse: " + exec.reason());
        assertFalse(exec.reason().contains("soort Samengesteld"),
                "de samenstelling zelf wordt wél gedragen — dat mag de reden niet suggereren: " + exec.reason());

        CaseRunner.Result drl = CaseRunner.viaDrlTekst(ir, List.of(c));
        assertEquals("PLAYED", drl.status(),
                "de DRL-tekst-tak draagt deze vorm wel — de resterende takongelijkheid: " + drl.reason());
    }

    /**
     * The bound this branch carries is the §5.6 GETALACHTIG one — the value path binds a numeric global as
     * a {@code BigDecimal} on the pattern. A TEKST parameter is a differently-typed global and is refused
     * here, naming the operand rather than the node: measured, the DRL-text branch plays it (a global is a
     * free name there) and so does {@code ref}, while the clingo backend refuses it on its own axis, so
     * this form has no corpus witness to be had and stays a named refusal on both sides of the register.
     */
    @Test void een_tekstparameter_als_grens_weigert_de_exec_tak_bij_naam() throws Exception {
        Path ir = tekstParameter();
        Path c = tmp.resolve("tekst-param-case.json");
        Files.writeString(c, TEKST_PARAM_CASE);

        CaseRunner.Result exec = CaseRunner.viaExecModel(ir, List.of(c));
        assertEquals("EXCLUDE", exec.status(), "een niet-gelowerde vouw mag nooit meespelen");
        assertTrue(exec.reason().contains("de parameter 'keurrang'") && exec.reason().contains("'rang'"),
                "de exec-reden noemt beide operanden van de vergelijking: " + exec.reason());

        CaseRunner.Result drl = CaseRunner.viaDrlTekst(ir, List.of(c));
        assertEquals("PLAYED", drl.status(),
                "de DRL-tekst-tak draagt deze vorm wel — de resterende takongelijkheid: " + drl.reason());
    }

    // ================= the spliced documents =================

    /** The composite golden's document with ONE rule whose matrix is four composites deep. */
    private Path diepeSamenstelling() throws Exception {
        ObjectNode doc = (ObjectNode) JSON.readTree(Files.readString(SAMENGESTELD.resolve("ir.json")));
        houdRegels(doc, "paraat bij ploegkeur", "redderstal van een reddingspost");
        for (JsonNode r : IrPad.rulesLijst(doc, "resolvedRegels"))
            if ("paraat bij ploegkeur".equals(r.path("naam").asText()))
                ((ObjectNode) r.get("voorwaarde")).set("toets", JSON.readTree(DIEPE_MATRIX));
        return schrijf(doc, "diep-ir");
    }

    /** The parameter golden's document with a SECOND parameter and one rule reading both in one matrix. */
    private Path tweeParameters() throws Exception {
        ObjectNode doc = (ObjectNode) JSON.readTree(Files.readString(PARAMETER.resolve("ir.json")));
        houdRegels(doc, "bekwaam bij keurgrens", "loodstal van een loodspost");
        IrPad.modelLijst(doc, "parameters").add(JSON.readTree("""
                { "node": "Parameter", "naam": "bovengrens",
                  "datatype": { "node": "Numeriek", "geheel": true, "decimalen": 0,
                                "teken": "NIET_NEGATIEF", "eenheid": null },
                  "granulariteit": null }"""));
        for (JsonNode r : IrPad.rulesLijst(doc, "resolvedRegels"))
            if ("bekwaam bij keurgrens".equals(r.path("naam").asText()))
                ((ObjectNode) r.get("voorwaarde")).set("toets", JSON.readTree(TWEE_PARAM_MATRIX));
        return schrijf(doc, "twee-param-ir");
    }

    /** The parameter golden's document with a TEKST attribute, a TEKST parameter, and one rule comparing
     *  the two as the fold's matrix. */
    private Path tekstParameter() throws Exception {
        ObjectNode doc = (ObjectNode) JSON.readTree(Files.readString(PARAMETER.resolve("ir.json")));
        houdRegels(doc, "bekwaam bij keurgrens", "loodstal van een loodspost");
        IrPad.addAttribute(IrPad.objectType(doc, "Loods"), "rang", JSON.readTree("{ \"node\": \"Tekst\" }"));
        IrPad.modelLijst(doc, "parameters").add(JSON.readTree("""
                { "node": "Parameter", "naam": "keurrang", "datatype": { "node": "Tekst" },
                  "granulariteit": null }"""));
        for (JsonNode r : IrPad.rulesLijst(doc, "resolvedRegels"))
            if ("bekwaam bij keurgrens".equals(r.path("naam").asText()))
                ((ObjectNode) r.get("voorwaarde")).set("toets", JSON.readTree(TEKST_PARAM_MATRIX));
        return schrijf(doc, "tekst-param-ir");
    }

    private static void houdRegels(ObjectNode doc, String... namen) {
        List<String> keep = List.of(namen);
        com.fasterxml.jackson.databind.node.ArrayNode uit = JSON.createArrayNode();
        for (JsonNode r : IrPad.rulesLijst(doc, "resolvedRegels")) if (keep.contains(r.path("naam").asText())) uit.add(r);
        IrPad.rules(doc).set("resolvedRegels", uit);
    }

    private Path schrijf(ObjectNode doc, String naam) throws Exception {
        Path p = tmp.resolve(naam + "-" + (n++) + ".json");
        Files.writeString(p, doc.toPrettyString());
        return p;
    }

    /** ALLE[ TEN MINSTE 1[ ALLE[ GEEN[ dj &gt; 9, c &gt; 20 ], c &gt; 7 ], gediplomeerd ], dj &gt; 2 ]. */
    private static final String DIEPE_MATRIX = """
            { "node": "Samengesteld", "kwantor": "ALLE", "drempel": 0, "onderdelen": [
                { "node": "Samengesteld", "kwantor": "TEN_MINSTE", "drempel": 1, "onderdelen": [
                    { "node": "Samengesteld", "kwantor": "ALLE", "drempel": 0, "onderdelen": [
                        { "node": "Samengesteld", "kwantor": "GEEN", "drempel": 0, "onderdelen": [
                            { "node": "Vergelijking", "op": "GROTER",
                              "links": { "node": "AttribuutRef", "attribuut": "dienstjaren",
                                         "objecttype": "Redder", "bron": "zijn dienstjaren" },
                              "rechts": { "node": "Literal", "waarde": "9",
                                          "type": { "node": "Numeriek", "geheel": true, "decimalen": 0,
                                                    "teken": "ONBEPAALD", "eenheid": null }, "bron": "9" },
                              "periode": null, "bron": "zijn dienstjaren groter zijn dan 9" },
                            { "node": "Vergelijking", "op": "GROTER",
                              "links": { "node": "AttribuutRef", "attribuut": "conditie",
                                         "objecttype": "Redder", "bron": "zijn conditie" },
                              "rechts": { "node": "Literal", "waarde": "20",
                                          "type": { "node": "Numeriek", "geheel": true, "decimalen": 0,
                                                    "teken": "ONBEPAALD", "eenheid": null }, "bron": "20" },
                              "periode": null, "bron": "zijn conditie groter is dan 20" } ],
                          "bron": "hij aan geen van de volgende voorwaarden voldoet" },
                        { "node": "Vergelijking", "op": "GROTER",
                          "links": { "node": "AttribuutRef", "attribuut": "conditie",
                                     "objecttype": "Redder", "bron": "zijn conditie" },
                          "rechts": { "node": "Literal", "waarde": "7",
                                      "type": { "node": "Numeriek", "geheel": true, "decimalen": 0,
                                                "teken": "ONBEPAALD", "eenheid": null }, "bron": "7" },
                          "periode": null, "bron": "zijn conditie groter is dan 7" } ],
                      "bron": "hij aan alle volgende voorwaarden voldoet" },
                    { "node": "Kenmerk", "kenmerk": "gediplomeerd", "negatie": false,
                      "eigenaar": null, "periode": null, "bron": "hij gediplomeerd is" } ],
                  "bron": "hij aan ten minste 1 van de volgende voorwaarden voldoet" },
                { "node": "Vergelijking", "op": "GROTER",
                  "links": { "node": "AttribuutRef", "attribuut": "dienstjaren", "objecttype": "Redder",
                             "bron": "zijn dienstjaren" },
                  "rechts": { "node": "Literal", "waarde": "2",
                              "type": { "node": "Numeriek", "geheel": true, "decimalen": 0,
                                        "teken": "ONBEPAALD", "eenheid": null }, "bron": "2" },
                  "periode": null, "bron": "zijn dienstjaren groter zijn dan 2" } ],
              "bron": "hij aan alle volgende voorwaarden voldoet" }
            """;

    /**
     * P1's two redders reach WAAR by different roads (R1 through the nested ALLE, R2 through the
     * characteristic beside it), P2's fails the innermost GEEN (12 &gt; 9), P3's fails the outermost
     * bullet (1 &gt; 2 is onwaar) while every inner level holds.
     */
    private static final String DIEP_CASE = """
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
    private static final String TWEE_PARAM_MATRIX = """
            { "node": "Samengesteld", "kwantor": "ALLE", "drempel": 0, "onderdelen": [
                { "node": "Vergelijking", "op": "GROTER",
                  "links": { "node": "AttribuutRef", "attribuut": "ervaring", "objecttype": "Loods",
                             "bron": "zijn ervaring" },
                  "rechts": { "node": "ParameterRef", "parameter": "keurgrens",
                              "bron": "de keurgrens" },
                  "periode": null, "bron": "zijn ervaring groter is dan de keurgrens" },
                { "node": "Vergelijking", "op": "KLEINER",
                  "links": { "node": "AttribuutRef", "attribuut": "ervaring", "objecttype": "Loods",
                             "bron": "zijn ervaring" },
                  "rechts": { "node": "ParameterRef", "parameter": "bovengrens",
                              "bron": "de bovengrens" },
                  "periode": null, "bron": "zijn ervaring kleiner is dan de bovengrens" } ],
              "bron": "hij aan alle volgende voorwaarden voldoet" }
            """;

    /** rang == keurrang — a NON-numeric parameter as the bound. */
    private static final String TEKST_PARAM_MATRIX = """
            { "node": "Vergelijking", "op": "GELIJK",
              "links": { "node": "AttribuutRef", "attribuut": "rang", "objecttype": "Loods",
                         "bron": "zijn rang" },
              "rechts": { "node": "ParameterRef", "parameter": "keurrang",
                          "bron": "de keurrang" },
              "periode": null, "bron": "zijn rang gelijk is aan de keurrang" }
            """;

    private static final String TEKST_PARAM_CASE = """
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

    private static final String TWEE_PARAM_CASE = """
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
