package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * WF-112 positions NESTED inside a position WF-112 admits — a GEEN fold or a GEEN composite as a member of an
 * ALLE or DE composite, a GEEN composite in the filter of an aggregation that sits in an operand, a GEEN fold
 * in the toets of an ALLE fold — and the bound shape WF-111 refuses, in the toets and the filter of a fold.
 * Both branches refuse every one of them by name, and name the position that is ill-formed rather than the
 * node around it.
 *
 * <p><b>Why.</b> Measured 2026-09-23 on documents built exactly as below (the lens round of card
 * drools-venster-vouwtoets). The exec-model branch named WF-112 at all five composite nestings; the DRL-text
 * branch refused them as "voorwaarde" or "aggregatie-bullet onder tellende kwantor", because its composite arm
 * put its own generic marker over a member's reason. On the card's base the same branch PLAYED the two nestings
 * of a GEEN fold in an ALLE and in a DE composite, with the complement in a time-free session: the flank was
 * closed by the card's fold refusal, but under a name that said nothing. The fold's frame also named the OUTER
 * fold's element type for a GEEN fold nested in its toets, and a WF-111 bound in a fold was reported as "een
 * matrixvorm die deze tak niet IN het elementpatroon kan schrijven (Periode)" although this branch writes
 * windows there; the bound is what it cannot write.
 *
 * <p>The documents are {@code corpus/golden/jutterswet-schouwploeg} with the condition of "bevoegd bij diploma"
 * replaced; the nested fold adds a {@code Brevet} the Strandwacht holds. The case is the golden's own first
 * case (time-free): the refusals happen when the rule is built, before any case is read.
 */
class WindowNestingWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** cwd under surefire is the module dir (backends/drools); the corpus is two levels up. */
    private static final Path SCHOUWPLOEG = Path.of("..", "..", "corpus", "golden", "jutterswet-schouwploeg");

    private static final String IN_NONE_FOLD = "periodevenster in de toets van een kwantorvouw onder GEEN over Strandwacht (WF-112)";
    private static final String IN_NONE_COMPOUND = "periodevenster in een GEEN-samenstelling (WF-112)";
    private static final String BOUND_FORM = "een periodevenster met een grens die geen datumliteral en geen eigen"
            + " scalair datumattribuut van het element is (WF-111)";

    @TempDir Path tmp;

    private int n;

    @Test void both_branches_refuse_a_none_fold_in_an_all_compound_by_name() throws Exception {
        BothBranches.refused(document(combined("all", ERVAREN, noneFold(WINDOW))), goldenCase(), IN_NONE_FOLD,
                "ALLE[ervaren, GEEN-vouw(venster)]");
    }

    @Test void a_none_fold_in_a_the_composition_both_branches_refuse_by_name() throws Exception {
        BothBranches.refused(document(combined("the", ERVAREN, noneFold(WINDOW))), goldenCase(), IN_NONE_FOLD,
                "DE[ervaren, GEEN-vouw(venster)]");
    }

    @Test void both_branches_refuse_a_none_compound_in_an_all_compound_by_name() throws Exception {
        BothBranches.refused(document(combined("all", ERVAREN, combined("none", WINDOW))), goldenCase(),
                IN_NONE_COMPOUND, "ALLE[ervaren, GEEN[venster]]");
    }

    /** The GEEN composite sits in the FILTER of a sum that is an operand: the position is inside an expression. */
    @Test void both_branches_refuse_a_none_compound_in_the_filter_of_an_operand_by_name() throws Exception {
        BothBranches.refused(document(combined("all", NOT_ERVAREN, greater(sum(combined("none", WINDOW)), 3))), goldenCase(),
                IN_NONE_COMPOUND, "ALLE[niet ervaren, SOM(… die GEEN[venster]) > 3]");
        BothBranches.refused(document(combined("none", greater(sum(combined("none", WINDOW)), 3))), goldenCase(),
                IN_NONE_COMPOUND, "GEEN[SOM(… die GEEN[venster]) > 3]");
    }

    /** A GEEN fold in the toets of an ALLE fold: the refusal names the INNER fold's element type, and is not
     *  framed as the outer fold's (the DRL-text branch's frame said "kwantorvouw (ALLE over Strandwacht)" around
     *  it until 2026-09-23). */
    @Test void a_none_fold_in_the_test_of_an_all_fold_names_the_innermost_fold() throws Exception {
        Path ir = withBrevet(document(fold("all", "Strandwacht", GUARD, null,
                fold("none", "Brevet", BREVET, null, BREVET_WINDOW))));
        BothBranches.refused(ir, goldenCase(),
                "periodevenster in de toets van een kwantorvouw onder GEEN over Brevet (WF-112)",
                "ALLE-vouw over Strandwacht(GEEN-vouw over Brevet(venster))");
        for (boolean viaDrl : new boolean[] { false, true })
            assertFalse(refusal(ir, viaDrl).contains("ALLE over Strandwacht"),
                    (viaDrl ? "drl-tekst" : "exec-model") + " kadert de weigering niet als de buitenste vouw");
    }

    /** WF-111 in a fold: a date PARAMETER as the bound, in the toets and in the filter. The refusal names the
     *  bound, and the DRL-text frame no longer calls a fold over a non-§8.2 matrix a §8.2 fold. */
    @Test void a_parameter_bound_in_test_and_filter_of_a_fold_names_the_bound() throws Exception {
        Path test = withAsOfDate(document(fold("all", "Strandwacht", GUARD, null, PARAMETER_WINDOW)));
        BothBranches.refused(test, goldenCase(), BOUND_FORM, "de parametergrens in de toets");
        BothBranches.refused(withAsOfDate(document(fold("all", "Strandwacht", GUARD, PARAMETER_WINDOW, GEDIPLOMEERD))),
                goldenCase(), BOUND_FORM, "de parametergrens in het filter");
        assertFalse(refusal(test, true).contains("§8.2"), "geen §8.2-kader om een vouw zonder heelperiodematrix");
    }

    private static String refusal(Path ir, boolean viaDrl) throws Exception {
        JsonNode c = goldenCase();
        return assertThrows(CaseRunner.Excluded.class, () -> CaseRunner.playCase(ir, c, viaDrl)).getMessage();
    }

    // ================= the documents =================

    private static final String DATE = "{ \"node\": \"Date\", \"precision\": \"day\" }";
    private static final String NUM = "{ \"node\": \"Number\", \"whole\": true, \"decimals\": 0,"
            + " \"sign\": \"unspecified\" }";
    private static final String GUARD = "{ \"node\": \"RoleBinding\", \"factType\": \"schouwindeling\", \"role\": \"wacht\" }";
    private static final String BREVET = "{ \"node\": \"RoleBinding\", \"factType\": \"brevethouding\", \"role\": \"brevet\" }";

    private static final String WINDOW = """
            { "node": "TimeWindow",
              "from": { "node": "Literal", "value": { "date": "2024-01-01" }, "type": { "node": "Date", "precision": "day" }, "source": { "snippet": "2024-01-01" } },
              "to": { "node": "Literal", "value": { "date": "2025-01-01" }, "type": { "node": "Date", "precision": "day" }, "source": { "snippet": "2025-01-01" } },
              "toInclusive": false, "source": { "snippet": "van 1 januari 2024 tot 1 januari 2025" } }""";

    private static final String BREVET_WINDOW = """
            { "node": "TimeWindow",
              "from": { "node": "AttributeReference", "attribute": "afgifte", "subjectType": "Brevet",
                         "source": { "snippet": "de afgifte" } },
              "toInclusive": false, "source": { "snippet": "vanaf de afgifte" } }""";

    private static final String PARAMETER_WINDOW = """
            { "node": "TimeWindow",
              "from": { "node": "ParameterReference", "parameter": "peildatum", "source": { "snippet": "de peildatum" } },
              "toInclusive": false, "source": { "snippet": "vanaf de peildatum" } }""";

    private static final String ERVAREN = """
            { "node": "CharacteristicCheck", "characteristic": "ervaren", "negated": false,
              "source": { "snippet": "hij ervaren is" } }""";
    private static final String NOT_ERVAREN = """
            { "node": "CharacteristicCheck", "characteristic": "ervaren", "negated": true,
              "source": { "snippet": "hij niet ervaren is" } }""";
    private static final String GEDIPLOMEERD = """
            { "node": "CharacteristicCheck", "characteristic": "gediplomeerd", "negated": false,
              "source": { "snippet": "hij gediplomeerd is" } }""";

    private static String combined(String quantifier, String... parts) {
        return "{ \"node\": \"Compound\", \"quantifier\": \"" + quantifier + "\", \"threshold\": 0, \"parts\": [ "
                + String.join(", ", parts) + " ], \"source\": {\"snippet\": \"de volgende voorwaarden\"} }";
    }

    private static String fold(String quantifier, String elem, String rol, String filter, String test) {
        return "{ \"node\": \"QuantifierFold\", \"quantifier\": \"" + quantifier + "\", \"elementType\": \"" + elem + "\","
                + (filter == null ? "" : " \"filter\": " + filter + ",") + " \"test\": " + test + ","
                + " \"source\": {\"snippet\": \"de vouw over " + elem + "\"}, \"sourceRole\": " + rol + " }";
    }

    private static String noneFold(String test) {
        return fold("none", "Strandwacht", GUARD, null, test);
    }

    private static String sum(String filter) {
        return "{ \"node\": \"Aggregation\", \"kind\": \"sum\", \"arguments\": [ { \"node\": \"AttributeReference\","
                + " \"attribute\": \"dienstjaren\", \"subjectType\": \"Strandwacht\","
                + " \"source\": {\"snippet\": \"de dienstjaren van de wachten\"} } ],"
                + " \"elementType\": \"Strandwacht\", \"filter\": " + filter + ", \"sourceRole\": " + GUARD + ","
                + " \"type\": " + NUM + ", \"source\": {\"snippet\": \"de som van de dienstjaren\"} }";
    }

    private static String greater(String left, int n) {
        return "{ \"node\": \"Comparison\", \"op\": \"greater\", \"left\": " + left + ", \"right\": { \"node\":"
                + " \"Literal\", \"value\": { \"number\": \"" + n + "\" }, \"type\": " + NUM + ", \"source\": {\"snippet\": \""
                + n + "\"} }, \"source\": {\"snippet\": \"de som groter is dan " + n + "\"} }";
    }

    /** The golden's document with the condition of "bevoegd bij diploma" replaced by {@code voorwaarde}. */
    private Path document(String voorwaarde) throws Exception {
        ObjectNode doc = (ObjectNode) JSON.readTree(Files.readString(SCHOUWPLOEG.resolve("ir.json")));
        IrPath.version(doc, "rules", "bevoegd bij diploma").set("condition", JSON.readTree(voorwaarde));
        return write(doc);
    }

    /** {@code ir} with a Brevet that the Strandwacht holds, to-many, carrying an afgifte date. */
    private Path withBrevet(Path ir) throws Exception {
        ObjectNode doc = (ObjectNode) JSON.readTree(Files.readString(ir));
        ObjectNode brevet = IrPath.objectType(doc, "Strandwacht").deepCopy();
        brevet.put("name", "Brevet").put("animate", false).put("plural", "Brevetten");
        brevet.set("attributes", JSON.readTree(
                "[ { \"node\": \"Attribute\", \"name\": \"afgifte\", \"datatype\": " + DATE + " } ]"));
        brevet.set("characteristics", JSON.createArrayNode());
        IrPath.modelList(doc, "objectTypes").add(brevet);
        IrPath.modelList(doc, "factTypes").add(JSON.readTree("""
                { "node": "FactType", "name": "brevethouding", "reciprocal": false, "roles": [
                    { "node": "Role", "name": "houder", "objectType": "Strandwacht", "many": false },
                    { "node": "Role", "name": "brevet", "objectType": "Brevet", "plural": "brevetten", "many": true } ] }"""));
        return write(doc);
    }

    /** {@code ir} with a date parameter {@code peildatum}. */
    private Path withAsOfDate(Path ir) throws Exception {
        ObjectNode doc = (ObjectNode) JSON.readTree(Files.readString(ir));
        IrPath.modelList(doc, "parameters").add(JSON.readTree(
                "{ \"node\": \"Parameter\", \"name\": \"peildatum\", \"datatype\": " + DATE + " }"));
        return write(doc);
    }

    private Path write(ObjectNode doc) throws Exception {
        Path p = tmp.resolve("vensternesting-" + (n++) + ".json");
        Files.writeString(p, doc.toPrettyString());
        return p;
    }

    private static JsonNode goldenCase() throws Exception {
        return JSON.readTree(Files.readString(SCHOUWPLOEG.resolve("case1-allen-ervaren.json")));
    }
}
