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

    @Test void een_tekstbron_valt_in_dezelfde_arm() throws Exception {
        // The second member type that reaches the BigDecimal accumulate on a verifier-clean document,
        // and the second crash the one arm closes (java.lang.String instead of java.time.LocalDate).
        // It rides the same fixture so the row cannot pass by testing a different mechanism; only the
        // case carries text instead of dates, because a date value against a Tekst declaration would
        // be refused by the case reader and never reach the transpiler at all.
        BeideTakken.geweigerd(tijdelijk("tekstsetbron", alsTekst(BEREIK)), JSON.readTree(TEKSTZAAK),
                "vouw MINIMUM over de setbron 'vondstdatum' van Vondst: die leden zijn van type Tekst",
                "a text set source refuses by name in both branches");
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
        for (JsonNode r : ir.get("resolvedRegels")) ((ObjectNode) r.get("waarde")).put("kind", kind);
        return ir;
    }

    /** The bereik golden retyped to Tekst end to end — the member declaration, the fold's own type
     *  and both targets — which is what keeps it past WF-091 (a fold's type must follow its operand). */
    private static ObjectNode alsTekst(String golden) throws Exception {
        ObjectNode ir = (ObjectNode) JSON.readTree(Files.readString(ir(golden)));
        ObjectNode tekst = JSON.createObjectNode().put("node", "Tekst");
        ObjectNode jutter = (ObjectNode) ir.get("objecttypen").get("Jutter").get("attributen");
        jutter.set("vroegste vondstdag", tekst);
        jutter.set("jongste vondstdag", tekst);
        ((ObjectNode) ir.get("objecttypen").get("Vondst").get("attributen")).set("vondstdatum", tekst);
        for (JsonNode r : ir.get("resolvedRegels")) {
            ((ObjectNode) r.get("doel")).set("type", tekst);
            ObjectNode w = (ObjectNode) r.get("waarde");
            w.set("type", tekst);
            ((ObjectNode) w.get("argumenten").get(0)).set("type", tekst);
        }
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
