package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.uchar.regelspraak.loader.IrLoader;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The ch. 4.3 {@code sourceRole} witness for the TEMPORAL set fold — §5.8.2/§5.1.4 "de som van &lt;temporeel
 * attribuut&gt; van alle &lt;rol&gt;" written into a TEMPORAL target, which keeps the timeline instead of
 * collapsing it (only the §7.1 "totaal van" collapses to a scalar).
 *
 * <p>This fold resolves its OWN range, apart from the instance fold's, and until 2026-09-19 it resolved it with
 * the by-pair search alone in BOTH branches. On a REPEATED type pair that search is ambiguous by construction,
 * so the wire's binding was the only thing that could name the relation — and neither branch read it.
 *
 * <p>RED FIRST, measured on the jar from the commit before the fix, on the three documents below:
 * <pre>
 *   uniek      exec PLAYED 1        drl PLAYED 1
 *   herhaald   exec EXCLUDE transpiler SKIP: regel (temporele toekenning buiten Phase-F scope)
 *              drl  EXCLUDE drl-transpiler MARKER: TODO temporele toekenning: de som van het bewaartarief …
 *   herhaald,  the same two refusals, WORD FOR WORD — so the two branches could not tell a document that
 *   binding    NAMES its range from one that does not, and the one that does not is not well-formed at all
 *   gestript   (verifier: WF-048 FAIL … 2 fact types relate 'Jutter' to 'Vondst').
 * </pre>
 * The rest of the square refuses ALL THREE, which is why no corpus golden can pin this form: ref answers
 * "EVALUATION FAULT: temporal attribute \"bewaarlast\" as a target: temporal writes are not implemented" and
 * clingo "TRANSLATION FAULT: a temporal target takes only the ch. 3.7 pro-rata or ch. 3.8 day-count shape". The
 * timeline expectation below is therefore hand-computed and pinned HERE, in the only two executors that carry
 * the form.
 *
 * <p>Every model is an in-memory variant of ONE corpus golden ({@code jutterswet-tempovouw}); the golden's own
 * files are never touched, and it deliberately folds the §7.1 scalar TOTAAL, not this.
 */
class TemporeleSetvouwBereikWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    /** Jutter -- bewaring -- Vondst, with {@code Vondst.bewaartarief} a MAAND timeline. */
    private static final String TEMPOVOUW = "jutterswet-tempovouw";

    /** J1 keeps V1 (12 EUR, jan–mrt) and V2 (7 EUR jan, 5 EUR feb): per period 19 / 17 / 12. */
    private static final String ZAAK = """
        { "format": "uchar-case-v0", "asOfDate": "2026-03-15",
          "input": { "instances": [
            { "label": "J1", "objectType": "Jutter", "attributes": {},
              "roles": { "vondsten": ["V1", "V2"] }, "characteristics": [] },
            { "label": "V1", "objectType": "Vondst", "attributes": { "bewaartarief": { "timeline": [
                { "from": "2026-01-01", "to": "2026-04-01", "value": { "number": "12" } } ] } },
              "roles": {}, "characteristics": [] },
            { "label": "V2", "objectType": "Vondst", "attributes": { "bewaartarief": { "timeline": [
                { "from": "2026-01-01", "to": "2026-02-01", "value": { "number": "7" } },
                { "from": "2026-02-01", "to": "2026-03-01", "value": { "number": "5" } } ] } },
              "roles": {}, "characteristics": [] } ], "parameters": {} },
          "expected": [
            { "kind": "attributeTimeline", "label": "J1", "attribute": "bewaarlast", "timeline": [
              { "from": "2026-01-01", "to": "2026-02-01", "value": { "number": "19" } },
              { "from": "2026-02-01", "to": "2026-03-01", "value": { "number": "17" } },
              { "from": "2026-03-01", "to": "2026-04-01", "value": { "number": "12" } } ] } ] }
        """;

    @Test void de_temporele_setvouw_over_een_uniek_paar_blijft_spelen() throws Exception {
        // The regression mirror: ONE feittype over the pair. Green before this card as well — the row that says
        // the new arm reached only the case the old one could not answer; since L2 of the v1 wire migration the
        // binding decides here too.
        drlSpeeltTmsWeigert(tijdelijk("setvouw-uniek", setvouw()), JSON.readTree(ZAAK),
                "per-period sum over a uniquely connected pair: 19 / 17 / 12");
    }

    @Test void de_temporele_setvouw_leest_de_wire_binding_op_een_herhaald_paar() throws Exception {
        // THE ROW. A SECOND feittype relates Jutter to Vondst, so the by-pair search is ambiguous and only the
        // ch. 4.3 binding names the relation. Same timeline, both branches — this fold now takes the same LAST
        // arm the instance fold takes, out of the same shared derivation.
        drlSpeeltTmsWeigert(tijdelijk("setvouw-herhaald", herhaaldPaar()), JSON.readTree(ZAAK),
                "the wire's range carries the fold over a repeated type pair");
    }

    /** The DRL-text branch plays {@code c}; the truth-maintenance branch refuses the rule by name, since it writes a
     *  timeline target only with a time-proportional part or a day count (RUNTIME-SEMANTICS ch. 3.4, 3.5, 3.11), a
     *  form the destructive exec-model branch carried; the loss is accepted with card tms-omschakeling. */
    private static void drlSpeeltTmsWeigert(Path ir, JsonNode c, String boodschap) throws Exception {
        org.junit.jupiter.api.Assertions.assertNull(CaseRunner.speelCase(ir, c, true), "drl-tekst — " + boodschap);
        CaseRunner.Excluded e = org.junit.jupiter.api.Assertions.assertThrows(CaseRunner.Excluded.class,
                () -> CaseRunner.speelCase(ir, c, DroolsBackend.Branch.TRUTH_MAINTENANCE), "tms — " + boodschap);
        org.junit.jupiter.api.Assertions.assertTrue(e.getMessage().contains("regel: bewaarlast van een jutter — een"
                + " waarde van het tijdsafhankelijke doel 'bewaarlast' van Jutter die geen tijdsevenredig deel en geen"
                + " dagentelling is ('de som van het bewaartarief van alle vondsten van de Jutter')"), e.getMessage());
    }

    @Test void de_setvouw_zonder_binding_is_een_documentfout() throws Exception {
        // THE GUARD AGAINST WIDENING THE FIX INTO A GUESS. Strip the range off the wire and two feittypen are
        // left with nothing to choose between; neither branch may pick a winner. Since L2 of the v1 wire
        // migration a SOM over one source with an element type carries its range by schema (IR-SPEC ch. 4.3),
        // so the stripped document is ill-formed everywhere and this loader refuses it at the fold, before
        // either branch lowers a thing.
        ObjectNode ir = herhaaldPaar();
        ((ObjectNode) IrPad.versie(ir, "rules", 0, 0).get("value")).remove("sourceRole");
        Path p = tijdelijk("setvouw-geen-binding", ir);
        JsonNode c = JSON.readTree(ZAAK);
        for (boolean viaDrl : new boolean[] { false, true }) {
            IrLoader.Documentfout e = assertThrows(IrLoader.Documentfout.class,
                    () -> CaseRunner.speelCase(p, c, viaDrl),
                    (viaDrl ? "drl-tekst" : "exec-model") + " never lowers a bindingless temporal set fold");
            assertTrue(e.plek().equals("/rules/rules/0/versions/0/value")
                    && e.getMessage().contains("carries its range (sourceRole or sourcePath)"), e.getMessage());
        }
    }

    // ---------------- fixtures ----------------

    /** The golden turned into the §5.8.2 SET fold: the TARGET becomes a MAAND timeline, the fold kind becomes
     *  SOM (TOTAAL would collapse to a scalar), and element and target share one unit (the golden's element
     *  carries EUR/mnd, which does not restate into the target's EUR — §4.1 would marker before the range is
     *  ever asked). */
    private static ObjectNode setvouw() throws Exception {
        ObjectNode ir = (ObjectNode) JSON.readTree(Files.readString(ir(TEMPOVOUW)));
        ObjectNode bewaarlast = IrPad.attribute(IrPad.objectType(ir, "Jutter"), "bewaarlast");
        IrPad.timeline(bewaarlast, "month");
        bewaarlast.put("plural", "bewaarlasten");
        bewaarlast.set("datatype", eur());
        IrPad.attribute(IrPad.objectType(ir, "Vondst"), "bewaartarief").set("datatype", eur());
        ObjectNode r = IrPad.versie(ir, "rules", 0, 0);
        // the reads take their type from the declarations above; only the fold carries an annotation of its own
        ObjectNode w = (ObjectNode) r.get("value");
        w.put("kind", "sum").set("source", IrPad.bronrecord("de som van het bewaartarief van alle vondsten van de Jutter"));
        w.set("type", eur());
        return ir;
    }

    /** ...plus a SECOND feittype over the very same (Jutter, Vondst) pair, which is all it takes to make the
     *  by-pair search ambiguous while the wire binding stays exact. */
    private static ObjectNode herhaaldPaar() throws Exception {
        ObjectNode ir = setvouw();
        ObjectNode ft = IrPad.modelLijst(ir, "factTypes").addObject();
        ft.put("node", "FactType").put("name", "keurbewaring").put("reciprocal", false);
        ft.putArray("roles")
          .add(rol("keurende jutter", "Jutter", null, false))
          .add(rol("keurvondst", "Vondst", "keurvondsten", true));
        return ir;
    }

    private static ObjectNode rol(String naam, String objecttype, String meervoud, boolean meervoudig) {
        ObjectNode r = JSON.createObjectNode().put("node", "Role").put("name", naam)
                .put("objectType", objecttype);
        if (meervoud != null) r.put("plural", meervoud);
        return r.put("many", meervoudig);
    }

    private static ObjectNode eur() {
        ObjectNode e = JSON.createObjectNode().put("node", "Unit").put("abbreviation", "EUR")
                .put("system", "Munt").put("canonical", "EUR");
        e.putObject("factorToCanonical").put("num", 1).put("den", 1);
        ObjectNode n = JSON.createObjectNode().put("node", "Number").put("whole", true)
                .put("decimals", 0).put("sign", "unspecified");
        n.set("unit", e);
        return n;
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
