package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A value fold over a set source whose members are no number. An additive fold, "de som van de vondstdatum van alle
 * vondsten van de Jutter", adds them, which is arithmetic over numbers (WF-091); an ordering fold, "de minimale
 * vondstdatum …" or "de eerste …" over members of type Tekst, orders them, which asks for a number or a date (WF-038).
 * Both have no reading: the reference interpreter faults on the addition or the comparison itself and the ASP runner
 * declines them. The branch refuses them by name; before, it handed the members of an additive fold to a BigDecimal
 * sum, which failed at fire, and those of an ordering fold to the Java order of their values, which answered. An
 * ordering fold over dates is well defined (RUNTIME-SEMANTICS ch. 1.2e) and plays, as does every fold over numbers.
 *
 * <p>The documents are the golden {@code jutterswet-vondstbereik}, whose two rules take the minimum and the maximum of
 * the vondstdatum, with the fold kind replaced, and with the member, the folds and the targets retyped to Tekst; the
 * case of the text rows is J1 with two vondsten "aa" and "bb".
 *
 * <p><b>Measured</b> 2026-10-10: the verifier refuses the additive documents, "WF-091 FAIL …: the sum of 'vondstdatum'
 * on 'Vondst' folds Date values: a total, sum or pro-rata part is arithmetic over a Number, and Date values are
 * calendar positions" (and "… folds Text values … are not amounts"), and the ordering documents over text, "WF-038 FAIL
 * …: operand of the minimum ordering fold is text, must be a number or a date"; the reference interpreter faults,
 * "plus: expected a number, got 2024-01-15", "plus: expected a number, got "aa"" and "unordered comparison: "bb" vs
 * "aa""; the ASP runner declines each at translation; the exec-model and the DRL-text branch refuse each by name.
 *
 * <p><b>Red</b> on the claim of the card, 331b1179: no manifest carried a refusal, and the replay of the additive
 * documents stopped with "ClassCastException: class java.time.LocalDate cannot be cast to class java.math.BigDecimal"
 * and "… java.lang.String cannot be cast …", while the ordering documents over text played.
 */
class TmsSetbronWitnessTest {

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    private static final String BEREIK = "jutterswet-vondstbereik";

    /** The golden's J1 with text-valued vondsten. */
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

    private static ObjectNode golden(String naam) throws Exception {
        return (ObjectNode) TmsDocumenten.JSON.readTree(Files.readString(CORPUS.resolve(naam).resolve("ir.json")));
    }

    /** The bereik golden with the fold kind of both rules replaced by {@code kind}. */
    private static ObjectNode metKind(String kind) throws Exception {
        ObjectNode ir = golden(BEREIK);
        for (JsonNode r : ir.get("rules").get("rules")) ((ObjectNode) r.get("versions").get(0).get("value")).put("kind", kind);
        return ir;
    }

    /** {@link #metKind} retyped to Tekst end to end: the member, both targets and both folds. */
    private static ObjectNode alsTekst(String kind) throws Exception {
        ObjectNode ir = metKind(kind);
        ObjectNode tekst = TmsDocumenten.JSON.createObjectNode().put("node", "Text");
        ObjectNode jutter = IrPad.objectType(ir, "Jutter");
        IrPad.attribute(jutter, "vroegste vondstdag").set("datatype", tekst);
        IrPad.attribute(jutter, "jongste vondstdag").set("datatype", tekst);
        IrPad.attribute(IrPad.objectType(ir, "Vondst"), "vondstdatum").set("datatype", tekst);
        for (JsonNode r : ir.get("rules").get("rules")) ((ObjectNode) r.get("versions").get(0).get("value")).set("type", tekst);
        return ir;
    }

    private static Path tijdelijk(ObjectNode ir) throws Exception {
        Path p = Files.writeString(Files.createTempFile("tms-setbron", ".json"), TmsDocumenten.JSON.writeValueAsString(ir));
        p.toFile().deleteOnExit();
        return p;
    }

    /** The two refusals of a document whose both rules fold {@code kind} over members of type {@code type}. */
    private static List<String> weigeringen(String kind, String type, boolean optellend) {
        List<String> uit = new ArrayList<>();
        for (String regel : List.of("vroegste vondstdag van een jutter", "jongste vondstdag van een jutter"))
            uit.add("transpiler SKIP: regel: " + regel + " — vouw " + kind + " over de setbron 'vondstdatum' van"
                    + " Vondst: die leden zijn van type " + type + (optellend
                    ? " en niet getalachtig; optellen is rekenkunde over getallen (WF-091), en de referentie-interpreter"
                      + " faalt op de optelling zelf: geen enkele uitvoerder draagt deze vorm"
                    : " en kennen geen ordening; een ordenende vouw vraagt een getal of een datum (WF-038), en de"
                      + " referentie-interpreter faalt op de vergelijking zelf: geen enkele uitvoerder draagt deze vorm"));
        return uit;
    }

    @Test void een_optellende_vouw_over_datums_of_tekst_wordt_met_naam_geweigerd() throws Exception {
        assertEquals(weigeringen("SOM", "Datum in dagen", true),
                TmsTijdlijnWeigeringWitnessTest.weigeringen(tijdelijk(metKind("sum"))));
        assertEquals(weigeringen("TOTAAL", "Datum in dagen", true),
                TmsTijdlijnWeigeringWitnessTest.weigeringen(tijdelijk(metKind("total"))));
        assertEquals(weigeringen("SOM", "Tekst", true),
                TmsTijdlijnWeigeringWitnessTest.weigeringen(tijdelijk(alsTekst("sum"))));
    }

    @Test void een_ordenende_vouw_over_tekst_wordt_met_naam_geweigerd() throws Exception {
        String[][] soorten = {{"minimum", "MINIMUM"}, {"maximum", "MAXIMUM"}, {"first", "EERSTE"}, {"last", "LAATSTE"}};
        for (String[] s : soorten)
            assertEquals(weigeringen(s[1], "Tekst", false),
                    TmsTijdlijnWeigeringWitnessTest.weigeringen(tijdelijk(alsTekst(s[0]))), s[0]);
    }

    @Test void een_ordenende_vouw_over_datums_en_een_vouw_over_getallen_spelen() throws Exception {
        // the narrowness of the refusal: the golden itself, minimum and maximum over dates, and its numeric sibling
        for (String c : new String[] { "case1-bereik.json", "case2-volgorde.json" })
            assertNull(CaseRunner.speelCase(CORPUS.resolve(BEREIK).resolve("ir.json"),
                    TmsDocumenten.JSON.readTree(Files.readString(CORPUS.resolve(BEREIK).resolve(c))),
                    DroolsBackend.Branch.EXEC_MODEL), c);
        assertNull(CaseRunner.speelCase(CORPUS.resolve("jutterswet-muntpiek").resolve("ir.json"),
                TmsDocumenten.JSON.readTree(Files.readString(CORPUS.resolve("jutterswet-muntpiek").resolve("case.json"))),
                DroolsBackend.Branch.EXEC_MODEL));
    }

    /** Writes the refused documents, each with the case its row replays, under {@code args[0]}. */
    public static void main(String[] args) throws Exception {
        Path uit = Path.of(args[0]);
        JsonNode datumzaak = TmsDocumenten.JSON.readTree(Files.readString(CORPUS.resolve(BEREIK).resolve("case1-bereik.json")));
        JsonNode tekstzaak = TmsDocumenten.JSON.readTree(TEKSTZAAK);
        TmsTijdlijnWitnessTest.schrijf(uit.resolve("som-datum"), TmsDocumenten.JSON.writeValueAsString(metKind("sum")),
                datumzaak);
        TmsTijdlijnWitnessTest.schrijf(uit.resolve("totaal-datum"), TmsDocumenten.JSON.writeValueAsString(metKind("total")),
                datumzaak);
        for (String k : List.of("sum", "minimum", "maximum", "first", "last"))
            TmsTijdlijnWitnessTest.schrijf(uit.resolve(k + "-tekst"), TmsDocumenten.JSON.writeValueAsString(alsTekst(k)),
                    tekstzaak);
    }
}
