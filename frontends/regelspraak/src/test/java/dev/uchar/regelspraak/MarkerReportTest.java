package dev.uchar.regelspraak;

import dev.uchar.regelspraak.emit.IrEmitter;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WF-113 (b) on the producer route (IR-SPEC ch. 2, 2026-09-23): every {@code Unresolved} in a document the frontend
 * WRITES is accounted for by a warning whose message contains the marker's source snippet verbatim. The verifier holds
 * the rule on every document that reaches it; this suite holds it where the documents are made, on the emitted JSON
 * ({@link Report}).
 *
 * <p><b>Every marker route, in three classes.</b> A marker whose {@code reason} is set is reported by the resolver's
 * survivor sweep, whose text quotes the bron; every route test in {@link SilentMarkerTest} passes through its
 * {@code markerWithMessage}, which runs {@link Report#assertAccountedFor} on the emission. A marker whose {@code reason} is
 * null is spoken for by its own route, and {@link MarkerReasonPinTest} lists each such family with the diagnostic that
 * speaks: where that is a WARNING, the route is pinned here with a sentence that reaches it; where it is an ERROR,
 * the frontend writes no document at all (irMode writes nothing, and {@code IrEmitter.emit} refuses the model under
 * WF-113 (a)), so there is no wire for (b) to hold on. {@link #every_sanctioned_family_is_classified()} keeps
 * that split complete: a new family on the pin list fails here until it is placed.
 */
class MarkerReportTest {

    /** A Jutter with a set of vondsten, a temporal attribute and a Jutter-to-Jutter relation: enough for every
     *  aggregation route below and for a rule that binds 'Jutter' twice. */
    static final String MODEL = """
        Objecttype de Jutter (mv: Jutters) (bezield)
            het totaalgewicht Numeriek (getal met 2 decimalen);
            de maandvangst Numeriek (getal met 2 decimalen) voor elke maand;
            het vangsttotaal Numeriek (getal met 2 decimalen);
            is bevoegd kenmerk (bijvoeglijk);

        Objecttype de Vondst (mv: Vondsten)
            het gewicht Numeriek (getal met 2 decimalen);
            is gekeurd kenmerk (bijvoeglijk);

        Feittype vondstaanmelding
            de aanmelder Jutter
            de aangemelde vondst (mv: aangemelde vondsten) Vondst
        Eén aanmelder meldt meerdere aangemelde vondsten

        Feittype vriendschap
            de jutter Jutter
            de strandmaat (mv: strandmaten) Jutter
        Eén jutter heeft meerdere strandmaten
        """;

    /** Each null-reden family of {@link MarkerReasonPinTest#SANCTIONED}, placed: the WARNING routes it has and
     *  the test here that holds (b) on each, or the ERROR that keeps its document from being written. */
    private static final Map<String, String> CLASSIFICATION = new LinkedHashMap<>();
    static {
        CLASSIFICATION.put("RuleChecker.java#checkWholePeriod", "ERROR (§8.2 over een statisch onderwerp): geen document");
        CLASSIFICATION.put("RuleChecker.java#quantifierFold",
                "ERROR (RGS-348, de toets van een kwantor over een pad leest het regelonderwerp; RGS-349, het "
                + "regelonderwerp heeft het type van het element): geen document");
        CLASSIFICATION.put("RuleChecker.java#compoundElementFilter",
                "ERROR (RGS-350, een deelselectie noemt een onderwerp van het type van het element): geen document");
        CLASSIFICATION.put("RuleChecker.java#dayKindOperandError", "ERROR (Tabel 57): geen document");
        CLASSIFICATION.put("ExpressionBuilder.java#refuseInFilter",
                "ERROR (RGS-352, een lezing van het regelonderwerp in een filter zonder spelling vanaf het element): "
                + "geen document");
        CLASSIFICATION.put("ExpressionBuilder.java#variableInFilter",
                "ERROR (RGS-350, een variabele van de regel in een deelselectie over elementen van het type van het "
                + "regelonderwerp): geen document");
        CLASSIFICATION.put("ExpressionBuilder.java#buildFunction", "WARNING: aggregatie_over_een_ongedeclareerd_lid, "
                + "dimensieselectie_of_gedurende_op_een_aggregatie; ERROR (WF-037, aantal gevulde): geen document");
        CLASSIFICATION.put("ExpressionBuilder.java#checkFoldSource", "WARNING: vouwbron_geneste_verzamelingsstap, "
                + "vouwbron_tussenstap, vouwbron_rolanker, vouwbron_bindt_niet_op_de_geschreven_naam");
        CLASSIFICATION.put("ExpressionBuilder.java#aggregationFilter", "WARNING (van buildFunction): deelselectie_op_een_aggregatie_operand");
        CLASSIFICATION.put("ExpressionBuilder.java#checkOrdinal",
                "WARNING: rangtelwoord_over_twee_bindingen; ERROR (positie voorbij de bindingen): geen document");
    }

    // ---------------- the predicate itself ----------------

    @Test void the_predicate_reads_the_json_and_not_the_model() {
        String marker = "{\"node\": \"Unresolved\", \"source\": {\"snippet\": \"de som van \\u00e9\\u00e9n \\\"lid\\\"\"}}";
        String goed = document(marker, "{\"node\": \"Diagnostic\", \"severity\": \"warning\", "
                + "\"message\": \"onopgelost: x \\u2014 'de som van \\u00e9\\u00e9n \\\"lid\\\"' blijft\", \"line\": 3}");
        Report.assertAccountedFor(goed);
        assertEquals(java.util.List.of("de som van één \"lid\""), Report.markerSources(goed));

        String withoutReport = document(marker, "");
        assertThrows(AssertionError.class, () -> Report.assertAccountedFor(withoutReport), "(b): een marker, lege lijst");
        String otherSource = document(marker, "{\"node\": \"Diagnostic\", \"severity\": \"warning\", "
                + "\"message\": \"onopgelost: x \\u2014 'de som van een lid' blijft\", \"line\": 3}");
        assertThrows(AssertionError.class, () -> Report.assertAccountedFor(otherSource), "(b): een melding over iets anders");
        String error = document("null", "{\"node\": \"Diagnostic\", \"severity\": \"error\", \"message\": \"x\", \"line\": 1}");
        assertThrows(AssertionError.class, () -> Report.assertAccountedFor(error), "(a): severity error op de wire");

        // (b) counts a blank snippet as absent: an empty one is contained in every message, so the warning beside it
        // would account for it under a literal reading, and a marker without a source record is contained in none
        String message = "{\"node\": \"Diagnostic\", \"severity\": \"warning\", \"message\": \"iets\", \"line\": 1}";
        for (String source : new String[] { ", \"source\": {\"snippet\": \"\"}", ", \"source\": {\"snippet\": \"   \"}", "" }) {
            String withoutSource = document("{\"node\": \"Unresolved\"" + source + "}", message);
            assertThrows(AssertionError.class, () -> Report.assertAccountedFor(withoutSource), "(b): een marker" + source);
        }
    }

    // ---------------- the null-reden WARNING routes ----------------

    @Test void aggregation_over_an_undeclared_member() {
        written(MODEL + """

            Regel totaalgewicht
                geldig altijd
                    Het totaalgewicht van een Jutter moet berekend worden als de som van de schelpen van alle aangemelde vondsten van de Jutter.
            """, "is geen (meervoud van een) gedeclareerd attribuut van Vondst");
    }

    @Test void subselection_on_an_aggregation_operand() {
        written(MODEL + """

            Regel totaalgewicht
                geldig altijd
                    Het totaalgewicht van een Jutter moet berekend worden als de som van het gewicht van alle aangemelde vondsten van de Jutter die een kind hebben.
            """, "'die/dat'-subselectie op een aggregatie-operand");
    }

    @Test void dimension_selection_or_during_on_an_aggregation() {
        written(MODEL + """

            Regel vangsttotaal
                geldig altijd
                    Het vangsttotaal van een Jutter moet berekend worden als het totaal van de maandvangst van de Jutter gedurende de tijd dat de strandmaat van de Jutter bevoegd is.
            """, "een dimensieselectie / 'gedurende de tijd dat' op een aggregatie");
    }

    @Test void fold_source_nested_set_step() {
        written(NestedFoldSourceTest.MODEL_A + NestedFoldSourceTest.MIXED_CHAIN_RULE,
                NestedFoldSourceTest.CHAIN_HEAD);
    }

    @Test void fold_source_path_step() {
        written(NestedFoldSourceTest.MODEL_B + NestedFoldSourceTest.FOREIGN_ANCHOR_RULE, "tussenstap in een vouwbron");
    }

    @Test void fold_source_role_anchor() {
        written(NestedFoldSourceTest.ROLE_ANCHOR_MODEL + NestedFoldSourceTest.FAR_ROLE_ANCHOR_RULE,
                "rolanker in een vouwbron");
    }

    @Test void fold_source_does_not_bind_on_the_written_name() {
        written(NestedFoldSourceTest.MODEL_B + NestedFoldSourceTest.COLLIDING_ONE_STEP_RULE,
                "vouwbron bindt niet op de geschreven naam");
    }

    @Test void ordinal_over_two_bindings() {
        written(MODEL + """

            Regel totaalgewicht
                geldig altijd
                    Het totaalgewicht van een Jutter moet berekend worden als het vangsttotaal van de laatstgenoemde Jutter
                    indien een Jutter bevoegd is.
            """, "rangtelwoord: de regel bindt 2 onderwerpen 'Jutter'");
    }

    // ---------------- a sweep route that does not pass through SilentMarkerTest's helper ----------------

    @Test void a_numeral_under_one_beside_the_sweep_message() {
        // the fold's own decline speaks AND the sweep reports the surviving marker (SilentMarkerTest's
        // a_loud_route_is_not_reported_twice asserts the count, not the wire)
        written(SilentMarkerTest.MODEL + """
            Regel bevoegde ploeg
                geldig altijd
                    Een Schouwploeg is bevoegd
                    indien ten minste 0 van de wachten van de Schouwploeg gediplomeerd zijn.
            """, "telwoord van de kwantor");
    }

    // ---------------- completeness ----------------

    @Test void every_sanctioned_family_is_classified() {
        Set<String> families = MarkerReasonPinTest.SANCTIONED.stream()
                .map(p -> p.file() + "#" + p.method()).collect(Collectors.toCollection(TreeSet::new));
        assertEquals(families, new TreeSet<>(CLASSIFICATION.keySet()),
                "elke null-reden-familie van MarkerRedenPinTest hoort hier als WARNING-route met een zin of als "
                        + "ERROR-route zonder document ingedeeld te staan");
    }

    // ---------------- helpers ----------------

    /** The route is reached (its own WARNING is there), the model carries no ERROR so the document is written, a
     *  marker is on the wire, and WF-113 holds on the JSON. */
    private static void written(String source, String route) {
        RegelSpraakModel m = Resolver.resolve(source);
        assertTrue(m.isOk(), () -> "een ERROR houdt het document tegen, dus de route wordt hier niet bereikt: "
                + m.diagnostics);
        assertTrue(m.diagnostics.stream().anyMatch(d -> d.message().contains(route)),
                () -> "de route '" + route + "' wordt niet bereikt: " + m.diagnostics);
        String json = IrEmitter.emit(m);
        assertFalse(Report.markerSources(json).isEmpty(), () -> "geen marker op de wire:\n" + json);
        Report.assertAccountedFor(json);
    }

    /** A document of one rule whose one version carries {@code ruleValue} as its value — only as much of the wire
     *  as the predicate reads, in the rule layer's own shape. */
    private static String document(String ruleValue, String diagnostic) {
        return "{\"format\": \"uchar-ir-v1\", \"model\": {}, \"rules\": {\"rules\": [{\"node\": "
                + "\"Rule\", \"name\": \"r\", \"versions\": [{\"value\": " + ruleValue + "}]}]}, "
                + "\"diagnostics\": [" + diagnostic + "]}";
    }
}
