package io.github.yhavinga.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The VALUE fold over a set source whose members are NOT numbers — a crash in both branches until
 * 2026-09-20, and a named refusal since (card datum-setbron-breuk).
 *
 * <p>RED FIRST, measured on the jar built from the commit before this card, on the documents below:
 * <pre>
 *   SOM / TOTAAL over Vondst.vondstdatum (Datum in dagen)
 *       exec  ERROR ClassCastException: class java.time.LocalDate cannot be cast to
 *             class java.math.BigDecimal
 *       drl   EXCLUDE drl compile: … Unable to Analyse Expression if (($n1 == 0 ? null : $acc1) …
 *   MINIMUM / MAXIMUM over the same source
 *       exec  ERROR ConsequenceException … java.lang.IllegalStateException: kan
 *             accumulate-resultaat niet naar BigDecimal omzetten: class java.time.LocalDate
 *       drl   EXCLUDE drl compile: … Unable to Analyse Expression if (toBd($acc1) != null …
 *   SOM over a TEKST member: the same pair, with java.lang.String.
 * </pre>
 * Every one of those documents is verifier-CLEAN, and none of those outcomes is a refusal: a crash
 * names no form, and a whole-file DRL compile failure names the generated text rather than the
 * construct. Gate 12 sees the first as an ERROR — which it rightly refuses to excuse — and the
 * second as a channel with no diagnosis in it.
 *
 * <p>ONE row below rides a document the verifier REJECTS, and it says so in its own comment: an
 * ORDERING fold over text is WF-038 FAIL, so it can witness no carried form. It is here because the
 * reason is chosen on the MEMBER TYPE, and that choice needs a case where the two candidate
 * sentences differ. Every other row is verifier-clean.
 *
 * <p>The refusals carry TWO different reasons on purpose, and the rows below pin that difference,
 * because it is the whole finding of the card. An ADDITIVE fold over a date has no reading anywhere:
 * {@code ref} faults on the addition itself and the ASP backend declines in translation. An ORDERING
 * fold over a date is well defined and BOTH OCaml executors carry it
 * ({@code corpus/golden/jutterswet-vondstbereik}, hand-computed absolutes) — this backend is the one
 * that does not. A reader of a gate report has to be able to tell "nobody carries this" from "we do
 * not carry this".
 *
 * <p>The last row is the narrowness guard: a NUMERIC set source under the very same kinds still
 * plays in both branches, so the arm refuses a member type and not a fold.
 */
class DatumSetvouwWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    /** The MINIMUM/MAXIMUM-over-a-date-set golden: ref = clingo pin it, this backend refuses it. */
    private static final String BEREIK = "jutterswet-vondstbereik";

    /** Its numeric sibling — MINIMUM/MAXIMUM over a `kg` member — which both branches PLAY. */
    private static final String PIEK = "jutterswet-muntpiek";

    /** The bereik golden's J1 with text-valued members, for the Tekst row. */
    private static final String TEKSTZAAK = """
        { "format": "uchar-case-v0", "asOfDate": null,
          "input": { "instances": [
            { "label": "J1", "objectType": "Jutter", "attributes": {},
              "roles": { "vondsten": ["V1", "V2"] }, "characteristics": [] },
            { "label": "V1", "objectType": "Vondst",
              "attributes": { "vondstdatum": { "text": "aa" } }, "roles": {}, "characteristics": [] },
            { "label": "V2", "objectType": "Vondst",
              "attributes": { "vondstdatum": { "text": "bb" } }, "roles": {}, "characteristics": [] } ],
            "parameters": {} },
          "expected": [
            { "kind": "attributeValue", "label": "J1", "attribute": "vroegste vondstdag",
              "value": { "text": "aa" } } ] }
        """;

    @Test void de_ordenende_vouw_over_een_datumbron_weigert_bij_naam() throws Exception {
        // The golden as it stands. Both branches must name the fold, the member type AND the fact
        // that the reading exists — this is a gap in this backend, not a semantic difference.
        BeideTakken.geweigerd(ir(BEREIK), zaak(BEREIK, "case1-bereik.json"),
                "vouw MINIMUM over de setbron 'vondstdatum' van Vondst: die leden zijn van type "
                        + "Datum in dagen en niet getalachtig, terwijl beide takken elke waardevouw in "
                        + "BigDecimal accumuleren. De ordenende lezing KIEST één bijgedragen waarde",
                "an ordering fold over a date set source refuses by name in both branches");
    }

    @Test void de_optellende_vouw_over_een_datumbron_weigert_om_een_andere_reden() throws Exception {
        // Same document, only the kind twisted: now nothing carries the form, and the sentence says so.
        for (String kind : new String[] { "SOM", "TOTAAL" }) {
            BeideTakken.geweigerd(tijdelijk("datumsetbron-" + kind, metKind(BEREIK, kind)),
                    zaak(BEREIK, "case1-bereik.json"),
                    "vouw " + kind + " over de setbron 'vondstdatum' van Vondst: die leden zijn van type "
                            + "Datum in dagen en niet getalachtig, terwijl beide takken elke waardevouw in "
                            + "BigDecimal accumuleren. Optellen is rekenkunde en alleen bedragen tellen op",
                    "an additive fold over a date set source refuses by name in both branches");
        }
    }

    @Test void een_tekstbron_onder_de_optelvouw_valt_in_dezelfde_arm() throws Exception {
        // The second member type that reaches the BigDecimal accumulate on a VERIFIER-CLEAN document,
        // and the second crash the one arm closes (java.lang.String instead of java.time.LocalDate).
        // The kind has to be SOM: an ORDERING fold over text is rejected by the verifier itself
        // (WF-038 — "operand of the MINIMUM ordering fold is text, must be a number or a date",
        // measured 2026-09-20), so under min/max there is no clean document for this backend to be
        // wrong about. Under the additive kinds there is: retyped end to end, the document passes.
        // Only the case carries text instead of dates, because a date value against a Tekst
        // declaration is refused by the case reader and never reaches the transpiler at all.
        BeideTakken.geweigerd(tijdelijk("tekstsetbron", alsTekst(BEREIK, "SOM")), JSON.readTree(TEKSTZAAK),
                "vouw SOM over de setbron 'vondstdatum' van Vondst: die leden zijn van type Tekst en "
                        + "niet getalachtig, terwijl beide takken elke waardevouw in BigDecimal "
                        + "accumuleren. Optellen is rekenkunde en alleen bedragen tellen op",
                "a text set source under an additive fold refuses by name in both branches");
    }

    @Test void de_ordenende_tekstbron_krijgt_de_derde_zin() throws Exception {
        // DEFENCE IN DEPTH, and labelled as such. This document is NOT verifier-clean — WF-038
        // rejects it — so no corpus zone can hold it and the row exists only to pin that the reason
        // is chosen on the MEMBER TYPE and not on the kind. Choosing on the kind gave a text source
        // the date sentence ("is over een datumbron welgedefinieerd — ref en de ASP-backend dragen
        // haar"), which is false of text in every executor: measured 2026-09-20, ref faults
        // "unordered comparison: \"bb\" vs \"aa\"" and the ASP backend declines. A refusal that
        // misdiagnoses points its reader at the wrong card, which is the whole cost of getting a
        // reason wrong.
        BeideTakken.geweigerd(tijdelijk("tekstordening", alsTekst(BEREIK, null)), JSON.readTree(TEKSTZAAK),
                "vouw MINIMUM over de setbron 'vondstdatum' van Vondst: die leden zijn van type Tekst en "
                        + "niet getalachtig, terwijl beide takken elke waardevouw in BigDecimal "
                        + "accumuleren. De ordenende lezing vraagt een ORDENING op de leden en dit type "
                        + "heeft er geen",
                "a text set source under an ordering fold gets the nobody-carries-it sentence");
    }

    @Test void een_getalbron_onder_dezelfde_soorten_blijft_spelen() throws Exception {
        // NARROWNESS. Same two kinds, same set-source shape, numeric members: both branches play the
        // corpus golden untouched. Without this row the arm could refuse every value fold and every
        // row above would still be green.
        BeideTakken.groen(ir(PIEK), zaak(PIEK, "case.json"),
                "MINIMUM/MAXIMUM over a numeric set source is untouched by the member-type arm");
    }

    // ---------------- fixtures ----------------

    /** The bereik golden with both rules' fold kind replaced. Verifier-clean either way: the target,
     *  the member and the fold all stay `Datum in dagen`. */
    private static ObjectNode metKind(String golden, String kind) throws Exception {
        ObjectNode ir = (ObjectNode) JSON.readTree(Files.readString(ir(golden)));
        for (JsonNode r : ir.get("rules").get("resolvedRegels")) ((ObjectNode) r.get("versions").get(0).get("value")).put("kind", kind);
        return ir;
    }

    /** The bereik golden retyped to Tekst end to end — the member declaration, the fold's own type
     *  and both targets — which is what carries it past WF-091 (a fold's type must follow its
     *  operand). WF-038 is the one that then decides: with {@code kind} an additive one the document
     *  is verifier-CLEAN, with the golden's own MINIMUM/MAXIMUM left standing ({@code kind} null) it
     *  is verifier-REJECTED, and the two rows above say which they are. */
    private static ObjectNode alsTekst(String golden, String kind) throws Exception {
        ObjectNode ir = (ObjectNode) JSON.readTree(Files.readString(ir(golden)));
        if (kind != null)
            for (JsonNode r : ir.get("rules").get("resolvedRegels")) ((ObjectNode) r.get("versions").get(0).get("value")).put("kind", kind);
        ObjectNode tekst = JSON.createObjectNode().put("node", "Tekst");
        ObjectNode jutter = IrPad.objectType(ir, "Jutter");
        IrPad.attribute(jutter, "vroegste vondstdag").set("datatype", tekst);
        IrPad.attribute(jutter, "jongste vondstdag").set("datatype", tekst);
        IrPad.attribute(IrPad.objectType(ir, "Vondst"), "vondstdatum").set("datatype", tekst);
        // the reads take their type from the declarations above; only the fold carries an annotation of its own
        for (JsonNode r : ir.get("rules").get("resolvedRegels")) ((ObjectNode) r.get("versions").get(0).get("value")).set("type", tekst);
        return ir;
    }

    private static JsonNode zaak(String golden, String bestand) throws Exception {
        Path p = CORPUS.resolve(golden).resolve(bestand);
        assertTrue(Files.exists(p), "golden case present: " + p.toAbsolutePath());
        return JSON.readTree(Files.readString(p));
    }

    private static Path ir(String golden) {
        Path p = CORPUS.resolve(golden).resolve("ir.json");
        assertTrue(Files.exists(p), "golden ir.json present: " + p.toAbsolutePath());
        return p;
    }

    private static Path tijdelijk(String naam, ObjectNode ir) throws Exception {
        Path tmp = Files.createTempFile(naam, ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir.toString());
        return tmp;
    }
}
