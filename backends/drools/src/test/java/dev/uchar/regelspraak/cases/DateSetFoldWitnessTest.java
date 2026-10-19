package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.uchar.regelspraak.ir.Spelling;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The VALUE fold over a set source whose members are NOT numbers — a crash in both branches until
 * 2026-09-20, and a named refusal since (card datum-setbron-breuk). The ORDERING fold over a date
 * plays in the exec-model branch since 2026-09-30 (card drools-exec-datumvouw-over-rol), and in the
 * DRL-text branch since card drl-teksttak-op-truth-maintenance, which made it the second rendering of
 * the same analysis; until then that branch refused it by name.
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
 * None of those outcomes is a refusal: a crash names no form, and a whole-file DRL compile failure
 * names the generated text rather than the construct. Gate 12 sees the first as an ERROR — which it
 * rightly refuses to excuse — and the second as a channel with no diagnosis in it.
 *
 * <p>The documents of the three refusal rows below are NOT verifier-clean: the verifier rejects an
 * additive fold over a date or a text (WF-091, "a total, sum or pro-rata part is arithmetic") and an
 * ordering fold over a text (WF-038), measured 2026-10-10 on the documents as these rows build them.
 * They pin the refusal of a form no executor carries; they witness no carried form. The truth-maintenance
 * branch refuses them in a sentence of its own, which names the rule of the verifier
 * ({@code TmsTranspiler}), so each row pins that branch's sentence beside the DRL-text branch's.
 *
 * <p>The refusals carry TWO different reasons on purpose, and the rows below pin that difference,
 * because it is the whole finding of the card. An ADDITIVE fold over a date has no reading anywhere:
 * {@code ref} faults on the addition itself and the ASP backend declines in translation. An ORDERING
 * fold over a date is well defined and BOTH OCaml executors carry it
 * ({@code corpus/golden/jutterswet-vondstbereik}, hand-computed absolutes); the exec-model branch
 * folds it in a rule's value since 2026-09-30, as it folds eerste/laatste, so the DRL-text branch is
 * the one that does not, and its refusal says so. A reader of a gate report has to be able to tell
 * "nobody carries this" from "this branch does not carry this".
 *
 * <p>The last row is the narrowness guard: a NUMERIC set source under the very same kinds still
 * plays in both branches, so the arm refuses a member type and not a fold.
 */
class DateSetFoldWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    /** The MINIMUM/MAXIMUM-over-a-date-set golden: ref, clingo and the exec-model branch play it, the DRL-text
     *  branch refuses it. */
    private static final String RANGE = "jutterswet-vondstbereik";

    /** Its numeric sibling — MINIMUM/MAXIMUM over a `kg` member — which both branches PLAY. */
    private static final String PEAK = "jutterswet-muntpiek";

    /** The bereik golden's J1 with text-valued members, for the Tekst row. */
    private static final String TEXT_CASE = """
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

    @Test void the_ordering_fold_over_a_date_source_plays_in_both_branches() throws Exception {
        // The golden as it stands, both cases, with the values ref and clingo answer: the analysis folds the dates
        // by their order, through the accumulate it folds eerste/laatste with, and both branches render it. Until
        // card drl-teksttak-op-truth-maintenance the DRL-text branch refused it by name ("… terwijl de DRL-teksttak
        // elke waardevouw in BigDecimal accumuleert …").
        for (String c : new String[] { "case1-bereik.json", "case2-volgorde.json" })
            BothBranches.green(ir(RANGE), caseDocument(RANGE, c), "the ordering fold over a date set source, " + c);
    }

    @Test void the_summing_fold_over_a_date_source_refuses_for_another_reason() throws Exception {
        // Same document, only the kind twisted: now nothing carries the form, and the sentence says so.
        for (String kind : new String[] { "sum", "total" }) {
            String fold = "vouw " + Spelling.AGGREGATION_KIND.value("Aggregation.kind", kind).name()
                    + " over de setbron 'vondstdatum' van Vondst: die leden zijn van type Datum in dagen en niet "
                    + "getalachtig";
            BothBranches.refused(temporary("datumsetbron-" + kind, withKind(RANGE, kind)),
                    caseDocument(RANGE, "case1-bereik.json"),
                    fold + "; optellen is rekenkunde over getallen (WF-091), en de referentie-interpreter faalt op de "
                            + "optelling zelf: geen enkele uitvoerder draagt deze vorm",
                    "an additive fold over a date set source refuses by name in both branches");
        }
    }

    @Test void a_text_source_under_the_summing_fold_falls_in_the_same_arm() throws Exception {
        // The second member type that reaches the BigDecimal accumulate, and the second crash the one arm
        // closes (java.lang.String instead of java.time.LocalDate). The document passed the verifier on
        // 2026-09-20; today the verifier rejects an additive fold over text as well (WF-091, measured
        // 2026-10-10), so the row pins a refusal and witnesses no carried form. Only the case carries text
        // instead of dates, because a date value against a Tekst declaration is refused by the case reader
        // and never reaches the transpiler at all.
        String fold = "vouw SOM over de setbron 'vondstdatum' van Vondst: die leden zijn van type Tekst en niet "
                + "getalachtig";
        BothBranches.refused(temporary("tekstsetbron", asText(RANGE, "sum")), JSON.readTree(TEXT_CASE),
                fold + "; optellen is rekenkunde over getallen (WF-091), en de referentie-interpreter faalt op de "
                        + "optelling zelf: geen enkele uitvoerder draagt deze vorm",
                "a text set source under an additive fold refuses by name in both branches");
    }

    @Test void the_ordering_text_source_gets_the_third_sentence() throws Exception {
        // DEFENCE IN DEPTH, and labelled as such. This document is NOT verifier-clean — WF-038
        // rejects it — so no corpus zone can hold it and the row exists only to pin that the reason
        // is chosen on the MEMBER TYPE and not on the kind. Choosing on the kind gave a text source
        // the date sentence ("is over een datumbron welgedefinieerd — ref en de ASP-backend dragen
        // haar"), which is false of text in every executor: measured 2026-09-20, ref faults
        // "unordered comparison: \"bb\" vs \"aa\"" and the ASP backend declines. A refusal that
        // misdiagnoses points its reader at the wrong card, which is the whole cost of getting a
        // reason wrong.
        BothBranches.refused(temporary("tekstordening", asText(RANGE, null)), JSON.readTree(TEXT_CASE),
                "vouw MINIMUM over de setbron 'vondstdatum' van Vondst: die leden zijn van type Tekst en kennen geen "
                        + "ordening; een ordenende vouw vraagt een getal of een datum (WF-038), en de "
                        + "referentie-interpreter faalt op de vergelijking zelf: geen enkele uitvoerder draagt deze vorm",
                "a text set source under an ordering fold gets the nobody-carries-it sentence");
    }

    @Test void a_number_source_under_the_same_kinds_stays_playing() throws Exception {
        // NARROWNESS. Same two kinds, same set-source shape, numeric members: both branches play the
        // corpus golden untouched. Without this row the arm could refuse every value fold and every
        // row above would still be green.
        BothBranches.green(ir(PEAK), caseDocument(PEAK, "case.json"),
                "MINIMUM/MAXIMUM over a numeric set source is untouched by the member-type arm");
    }

    // ---------------- fixtures ----------------

    /** The bereik golden with both rules' fold kind replaced. The rows replace it with an additive kind,
     *  and then the document is not verifier-clean: WF-091 rejects a sum or a total over Date values
     *  ("a total, sum or pro-rata part is arithmetic over a Number, and Date values are calendar
     *  positions"), measured 2026-10-10 on both kinds. */
    private static ObjectNode withKind(String golden, String kind) throws Exception {
        ObjectNode ir = (ObjectNode) JSON.readTree(Files.readString(ir(golden)));
        for (JsonNode r : ir.get("rules").get("rules")) ((ObjectNode) r.get("versions").get(0).get("value")).put("kind", kind);
        return ir;
    }

    /** The bereik golden retyped to Tekst end to end — the member declaration, the fold's own type
     *  and both targets. The verifier rejects it either way (measured 2026-10-10): with {@code kind}
     *  an additive one on WF-091 ("Text values are not amounts"), with the golden's own
     *  MINIMUM/MAXIMUM left standing ({@code kind} null) on WF-038 (an ordering fold over text). */
    private static ObjectNode asText(String golden, String kind) throws Exception {
        ObjectNode ir = (ObjectNode) JSON.readTree(Files.readString(ir(golden)));
        if (kind != null)
            for (JsonNode r : ir.get("rules").get("rules")) ((ObjectNode) r.get("versions").get(0).get("value")).put("kind", kind);
        ObjectNode text = JSON.createObjectNode().put("node", "Text");
        ObjectNode jutter = IrPath.objectType(ir, "Jutter");
        IrPath.attribute(jutter, "vroegste vondstdag").set("datatype", text);
        IrPath.attribute(jutter, "jongste vondstdag").set("datatype", text);
        IrPath.attribute(IrPath.objectType(ir, "Vondst"), "vondstdatum").set("datatype", text);
        // the reads take their type from the declarations above; only the fold carries an annotation of its own
        for (JsonNode r : ir.get("rules").get("rules")) ((ObjectNode) r.get("versions").get(0).get("value")).set("type", text);
        return ir;
    }

    private static JsonNode caseDocument(String golden, String file) throws Exception {
        Path p = CORPUS.resolve(golden).resolve(file);
        assertTrue(Files.exists(p), "golden case present: " + p.toAbsolutePath());
        return JSON.readTree(Files.readString(p));
    }

    private static Path ir(String golden) {
        Path p = CORPUS.resolve(golden).resolve("ir.json");
        assertTrue(Files.exists(p), "golden ir.json present: " + p.toAbsolutePath());
        return p;
    }

    private static Path temporary(String name, ObjectNode ir) throws Exception {
        Path tmp = Files.createTempFile(name, ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir.toString());
        return tmp;
    }
}
