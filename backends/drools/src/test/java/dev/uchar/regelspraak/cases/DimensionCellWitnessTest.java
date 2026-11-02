package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.uchar.regelspraak.graph.ProducerConsumer;
import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.refusal.DroolsCode;
import dev.uchar.regelspraak.refusal.Reason;
import dev.uchar.regelspraak.transpiler.tms.Cycles;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The cells of a dimensioned attribute (RUNTIME-SEMANTICS ch. 5.8c) on both branches, over the goldens
 * {@code jutterswet-seizoenvondst} and {@code jutterswet-seizoenvouw} and over the two-dimensional document of the worked
 * example of ch. 5.8c, which this class derives from the second: the case reader lays a {@code cells} input on the bean
 * of its instance, cell by cell under the labels of the cell, a cell read over one or two dimensions reads one cell, a
 * sum, a minimum and a maximum fold the cells of the product of their selections, an empty cell skipped and an all-empty
 * selection leeg, and the expectations {@code cellValue} and {@code cellEmpty} read a cell back. The case reader refuses
 * labels that name no cell (EF-034), a cell named twice (EF-035) and a cell value its decimals do not hold (EF-004, its
 * place the cell), and refuses as a defect of the case without a code a scalar on a dimensioned attribute, a
 * {@code cells} input on an attribute without dimensions, an expectation on a dimensioned attribute as a whole and a
 * cell expectation whose labels name no cell. A reading (ch. 5.12) has the cell read and the dimension fold as its
 * nodes and the reference under them without a line; the rule graph (IR-SPEC ch. 5.1) reads a cell as a member of its
 * own, spelled as the verifier spells the node. A cell read or a fold whose labels name no cell of the dimensions of
 * its attribute breaks the contract of WF-040 and is refused by name. A cell written by a rule, an initialisation or an
 * object creation ({@code jutterswet-seizoenboeking}, {@code jutterswet-seizoenkopie}) is a member of its own: the
 * loader carries the forms, the rule graph and the cycle check draw the cell as a node, the rules write it, a fold reads
 * it beside the input cells, the write-back puts it into the map, its write boundary names it, and a reading reads a rule
 * on a cell as a rule on an attribute.
 *
 * <p><b>Red before the writing of cells</b> (measured 2026-11-02 on {@code 2e07be6b2}, the four rows of "writing
 * cells" after the loader row): the build refused both goldens and the red case, {@code EXCLUDE DROOLS-298} ("het
 * schrijven van een cel van het gedimensioneerde attribuut 'vondstgewicht' door 'najaarsgewicht van een jutter': deze
 * tak schrijft geen cel"), where a play, {@code FAULT EF-006} and readings were expected; and the cycle check admitted a
 * rule that computes the cell najaar from itself, since it left a rule with a cell target out of its graph.
 *
 * <p><b>Red before the change</b> (measured 2026-11-02 on {@code 574faa1a7}, all nine rows): every case with a cells
 * input {@code EXCLUDE DROOLS-047} ("invoer van een attribuut met dimensie 'vondstgewicht' (§3.6, een Map-beanlid)
 * wordt niet gedragen door de v0-backend"), also where a reading or a defect of the case was expected; the
 * two-dimensional document refused by the loader, {@code DROOLS-297} ("een dimensievouw over 2 dimensies …"), where a
 * play, a fault of the case input or {@code DROOLS-299} was expected; and the rule graph of
 * {@code jutterswet-seizoenvouw} reading the attribute {@code vondstgewicht} where its cells stand.
 */
class DimensionCellWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");
    private static final List<DroolsBackend.Branch> BRANCHES =
            List.of(DroolsBackend.Branch.EXEC_MODEL, DroolsBackend.Branch.DRL_TEXT);

    @TempDir Path tmp;
    private int files;

    // ---------------- reading and folding cells ----------------

    /** "Het voorjaarsgewicht van een Jutter moet berekend worden als zijn vondstgewicht in voorjaar": J1 with the cells
     *  voorjaar 12.50 and najaar 4.00 gives 12.50, J2 with the cell najaar alone leeg; the cells read back. */
    @Test void a_cell_of_one_dimension_reads_its_input() throws Exception {
        Path ir = document(golden("jutterswet-seizoenvondst"));
        Path c1 = caseFile(List.of(jutter("J1", cell("12.50", "voorjaar"), cell("4.00", "najaar"))),
                List.of(value("J1", "voorjaarsgewicht", "12.50"), cellValue("J1", "4.00", "najaar")));
        Path c2 = caseFile(List.of(jutter("J2", cell("4.00", "najaar"))),
                List.of(empty("J2", "voorjaarsgewicht"), cellEmpty("J2", "voorjaar"), cellValue("J2", "4.00", "najaar")));
        assertPlays(ir, List.of(c1, c2));
        // the comparison reads: a wrong value and a wrong cell fail
        assertFails(ir, caseFile(List.of(jutter("J1", cell("12.50", "voorjaar"))),
                List.of(value("J1", "voorjaarsgewicht", "12.49"))), "12.49");
        assertFails(ir, caseFile(List.of(jutter("J1", cell("12.50", "voorjaar"))),
                List.of(cellEmpty("J1", "voorjaar"))), "12.50");
        assertFails(ir, caseFile(List.of(jutter("J1", cell("12.50", "voorjaar"))),
                List.of(cellValue("J1", "12.50", "najaar"))), "leeg");
    }

    /** jutterswet-seizoenvouw, the worked example of ch. 5.8c: the sum over all seasons, the sum from voorjaar up to and
     *  including zomer, the maximum over voorjaar, zomer and najaar, the minimum from zomer up to and including najaar.
     *  J1 voorjaar 3.00 and najaar 2.00: 5.00, 3.00, 3.00, 2.00; J2 without a cell: leeg four times; J3 zomer 1.25:
     *  1.25 four times. */
    @Test void a_fold_over_one_dimension_skips_an_empty_cell() throws Exception {
        Path ir = document(golden("jutterswet-seizoenvouw"));
        Path c1 = caseFile(List.of(jutter("J1", cell("3.00", "voorjaar"), cell("2.00", "najaar"))),
                List.of(value("J1", "jaargewicht", "5.00"), value("J1", "groeigewicht", "3.00"),
                        value("J1", "piekgewicht", "3.00"), value("J1", "dalgewicht", "2.00"), cellEmpty("J1", "zomer")));
        Path c2 = caseFile(List.of(jutter("J2")), List.of(empty("J2", "jaargewicht"), empty("J2", "groeigewicht"),
                empty("J2", "piekgewicht"), empty("J2", "dalgewicht")));
        Path c3 = caseFile(List.of(jutter("J3", cell("1.25", "zomer"))),
                List.of(value("J3", "jaargewicht", "1.25"), value("J3", "groeigewicht", "1.25"),
                        value("J3", "piekgewicht", "1.25"), value("J3", "dalgewicht", "1.25")));
        assertPlays(ir, List.of(c1, c2, c3));
    }

    /** The two-dimensional example of ch. 5.8c: the cells (voorjaar, noord) 2.00, (najaar, noord) 1.00 and (voorjaar,
     *  zuid) 5.00; the cell voorjaar, zuid gives 5.00, the sum over all seasons at noord 3.00, at zuid 5.00, at both 8.00,
     *  and the minimum over the same selections 1.00. */
    @Test void a_cell_and_a_fold_over_two_dimensions() throws Exception {
        Path ir = document(twoDimensions());
        Path c1 = caseFile(List.of(jutter("J1", cell("2.00", "voorjaar", "noord"), cell("1.00", "najaar", "noord"),
                        cell("5.00", "voorjaar", "zuid"))),
                List.of(value("J1", "voorjaarzuid", "5.00"), value("J1", "noordgewicht", "3.00"),
                        value("J1", "zuidgewicht", "5.00"), value("J1", "totaalgewicht", "8.00"),
                        value("J1", "minimumgewicht", "1.00"), cellValue("J1", "1.00", "najaar", "noord"),
                        cellEmpty("J1", "zomer", "zuid")));
        Path c2 = caseFile(List.of(jutter("J2", cell("4.00", "zomer", "zuid"))),
                List.of(empty("J2", "voorjaarzuid"), empty("J2", "noordgewicht"), value("J2", "zuidgewicht", "4.00"),
                        value("J2", "totaalgewicht", "4.00"), value("J2", "minimumgewicht", "4.00")));
        assertPlays(ir, List.of(c1, c2));
    }

    /** A label is a name (IR-SPEC ch. 2.1): the input and an expectation may write it in another capitalisation, and
     *  name the declared cell. */
    @Test void a_label_compares_as_a_name() throws Exception {
        Path ir = document(golden("jutterswet-seizoenvondst"));
        assertPlays(ir, List.of(caseFile(List.of(jutter("J1", cell("12.50", "Voorjaar"))),
                List.of(value("J1", "voorjaarsgewicht", "12.50"), cellValue("J1", "12.50", "VOORJAAR")))));
    }

    // ---------------- the case reader ----------------

    /** EF-034: the cell zuid, voorjaar names its two declared labels in the reversed order, and a cell of one label
     *  names one dimension of two; EF-035: one cell twice, also in two spellings of a label; EF-004: 1.005 in a cell of
     *  2 decimals, its place the cell. */
    @Test void the_case_reader_refuses_a_cell_by_its_fault_kind() throws Exception {
        Path two = document(twoDimensions());
        assertFault(two, List.of(jutter("J1", cell("5.00", "zuid", "voorjaar"))), "EF-034",
                Map.of("label", "J1", "attribute", "vondstgewicht", "labels", "zuid, voorjaar"));
        assertFault(two, List.of(jutter("J1", cell("5.00", "voorjaar"))), "EF-034",
                Map.of("label", "J1", "attribute", "vondstgewicht", "labels", "voorjaar"));
        assertFault(two, List.of(jutter("J1", cell("5.00", "voorjaar", "zuid"), cell("3.00", "voorjaar", "zuid"))),
                "EF-035", Map.of("label", "J1", "attribute", "vondstgewicht", "labels", "voorjaar, zuid"));
        assertFault(two, List.of(jutter("J1", cell("5.00", "voorjaar", "zuid"), cell("3.00", "Voorjaar", "ZUID"))),
                "EF-035", Map.of("label", "J1", "attribute", "vondstgewicht", "labels", "Voorjaar, ZUID"));
        assertFault(document(golden("jutterswet-seizoenvondst")), List.of(jutter("J1", cell("1.005", "voorjaar"))),
                "EF-004", Map.of("place", "J1.vondstgewicht[voorjaar]", "value", "1.005", "decimals", "2"));
        assertFault(two, List.of(jutter("J1", cell("2.00", "najaar", "noord"), cell("1.005", "voorjaar", "zuid"))),
                "EF-004", Map.of("place", "J1.vondstgewicht[voorjaar, zuid]", "value", "1.005", "decimals", "2"));
    }

    /** A case that does not fit the declarations is a defect without a code (ch. 5.8c): a scalar on a dimensioned
     *  attribute, a cells input on an attribute without dimensions, a value of another variant in a cell, an
     *  expectation on a dimensioned attribute as a whole, a cell expectation on an attribute without dimensions, and a
     *  cell expectation whose labels name no cell. */
    @Test void a_case_that_does_not_fit_the_dimensions_is_a_defect() throws Exception {
        Path ir = document(golden("jutterswet-seizoenvouw"));
        assertDefect(ir, caseFile(List.of(instance("J1", "{\"vondstgewicht\":{\"number\":\"3.00\"}}")),
                List.of(empty("J1", "jaargewicht"))), "J1.vondstgewicht: a dimensioned attribute takes its input per cell");
        assertDefect(ir, caseFile(List.of(instance("J1", "{\"jaargewicht\":{\"cells\":[" + cell("3.00", "voorjaar") + "]}}")),
                List.of(empty("J1", "groeigewicht"))), "J1.jaargewicht: a cells input on an attribute without dimensions");
        assertDefect(ir, caseFile(List.of(instance("J1", "{\"vondstgewicht\":{\"cells\":[{\"labels\":[\"voorjaar\"],"
                        + "\"value\":{\"text\":\"drie\"}}]}}")), List.of(empty("J1", "groeigewicht"))),
                "J1.vondstgewicht[voorjaar]: the value variant \"text\" does not match the declared datatype");
        assertDefect(ir, caseFile(List.of(jutter("J1", cell("3.00", "voorjaar"))), List.of(empty("J1", "vondstgewicht"))),
                "attributeEmpty on the dimensioned attribute 'vondstgewicht' of J1");
        assertDefect(ir, caseFile(List.of(jutter("J1", cell("3.00", "voorjaar"))),
                List.of(value("J1", "vondstgewicht", "3.00"))), "attributeValue on the dimensioned attribute 'vondstgewicht' of J1");
        assertDefect(ir, caseFile(List.of(jutter("J1", cell("3.00", "voorjaar"))),
                List.of("{\"kind\":\"cellEmpty\",\"label\":\"J1\",\"attribute\":\"jaargewicht\",\"labels\":[\"voorjaar\"]}")),
                "cellEmpty on the attribute 'jaargewicht' of J1, which has no dimensions");
        assertDefect(ir, caseFile(List.of(jutter("J1", cell("3.00", "voorjaar"))), List.of(cellEmpty("J1", "herfst"))),
                "cellEmpty on J1.vondstgewicht: the labels [herfst] name no cell");
        assertDefect(ir, caseFile(List.of(jutter("J1", cell("3.00", "voorjaar"))), List.of(cellEmpty("J1", "voorjaar",
                "noord"))), "cellEmpty on J1.vondstgewicht: the labels [voorjaar, noord] name no cell");
    }

    // ---------------- the contract of WF-040 ----------------

    /** A fold over one dimension of an attribute of two, a fold whose selections name the two dimensions in the reversed
     *  order while their labels stand in the declared one, and a cell whose labels stand in the reversed order, name no
     *  cell of the attribute: WF-040 refuses the three documents, and the build refuses them by name, never reading an
     *  empty cell where the document names none, nor a cell its selections do not name. */
    @Test void labels_that_name_no_cell_break_the_contract() throws Exception {
        JsonNode fold = twoDimensions();
        ((ArrayNode) IrPath.version(fold, "rules", "noordgewicht van een jutter").get("value")
                .get("dimensionSelections")).remove(1);
        JsonNode swapped = twoDimensions();
        JsonNode selections = IrPath.version(swapped, "rules", "noordgewicht van een jutter").get("value")
                .get("dimensionSelections");
        ((ObjectNode) selections.get(0)).put("dimension", "strand");
        ((ObjectNode) selections.get(1)).put("dimension", "seizoen");
        JsonNode cell = twoDimensions();
        ArrayNode labels = (ArrayNode) IrPath.version(cell, "rules", "voorjaarzuid van een jutter").get("value")
                .get("labels");
        labels.removeAll();
        labels.add("zuid").add("voorjaar");
        for (JsonNode ir : List.of(fold, swapped, cell)) {
            Path p = document(ir);
            Path c = caseFile(List.of(jutter("J1")), List.of(empty("J1", "jaargewicht")));
            for (DroolsBackend.Branch branch : BRANCHES) {
                CaseRunner.Result r = replay(p, List.of(c), branch);
                assertEquals("EXCLUDE", r.status(), () -> branch + ": " + r);
                assertEquals("DROOLS-299", r.code() == null ? null : r.code().id(), () -> branch + ": " + r);
            }
        }
    }

    // ---------------- readings and the rule graph ----------------

    /** ch. 5.12: the cell read is the node of its cell, and the reference under it has no line; the worked example of
     *  ch. 5.12, "indien zijn vondstgewicht in voorjaar groter is dan 10 kg", on the cases J1 and J2 of the reading
     *  example of ch. 5.8c; and the node of a dimension fold has the value of the fold, its argument no line and the fold
     *  no contributors. */
    @Test void the_cell_and_the_fold_are_the_nodes_of_a_reading() throws Exception {
        JsonNode vondst = golden("jutterswet-seizoenvondst");
        ObjectNode version = IrPath.version(vondst, "rules", "voorjaarsgewicht van een jutter");
        JsonNode unit = IrPath.attribute(IrPath.objectType(vondst, "Jutter"), "vondstgewicht").get("datatype").get("unit");
        String ten = "{\"node\":\"Literal\",\"value\":{\"number\":\"10\"},\"type\":{\"node\":\"Number\",\"whole\":true,"
                + "\"decimals\":0,\"sign\":\"nonNegative\",\"unit\":" + JSON.writeValueAsString(unit) + "},"
                + "\"source\":{\"snippet\":\"10 kg\"}}";
        version.set("condition", JSON.readTree("{\"node\":\"Comparison\",\"op\":\"greater\",\"left\":"
                + JSON.writeValueAsString(version.get("value")) + ",\"right\":" + ten
                + ",\"source\":{\"snippet\":\"zijn vondstgewicht in voorjaar groter is dan 10 kg\"}}"));
        Path ir = document(vondst);
        Path cases = caseFile(List.of(jutter("J1", cell("12.50", "voorjaar"), cell("4.00", "najaar")),
                jutter("J2", cell("4.00", "najaar"))), List.of(value("J1", "voorjaarsgewicht", "12.50")));
        String r = "/rules/rules/0/versions/0 ";
        assertReadings(ir, cases, List.of(
                r + "\"J1\" 0 Comparison true",
                r + "\"J1\" 1 DimensionCellRead 12.5 \"kg\"",
                r + "\"J1\" 3 Literal 10 \"kg\"",
                r + "\"J1\" 4 DimensionCellRead 12.5 \"kg\"",
                r + "\"J1\" HOLDS",
                r + "\"J2\" 0 Comparison false",
                r + "\"J2\" 1 DimensionCellRead empty \"kg\"",
                r + "\"J2\" 3 Literal 10 \"kg\"",
                r + "\"J2\" 4 DimensionCellRead unreached",
                r + "\"J2\" FAILS"));
        List<String> folds = new ArrayList<>();
        String[] values = { "5", "3", "3", "2" };
        for (int k = 0; k < 4; k++) {
            folds.add("/rules/rules/" + k + "/versions/0 \"J1\" 0 Aggregation " + values[k] + " \"kg\"");
            folds.add("/rules/rules/" + k + "/versions/0 \"J1\" HOLDS");
        }
        assertReadings(document(golden("jutterswet-seizoenvouw")), caseFile(List.of(jutter("J1", cell("3.00", "voorjaar"),
                cell("2.00", "najaar"))), List.of(value("J1", "jaargewicht", "5.00"))), folds);
    }

    /** IR-SPEC ch. 5.1: a cell is a node of its own, spelled as the verifier spells it, the member with its labels in
     *  declared order; a fold reads every cell of the product of its selections, the first selection outermost. */
    @Test void the_rule_graph_reads_a_cell_as_a_member_of_its_own() throws Exception {
        IrLoader.Loaded vouw = IrLoader.loadAll(golden("jutterswet-seizoenvouw"));
        assertEquals(List.of("vondstgewicht[voorjaar]", "vondstgewicht[zomer]"), members(ProducerConsumer.forms(
                vouw.model(), vouw.prioritizedDefaults(), ProducerConsumer.wireNavigation(vouw)).get(1)));
        IrLoader.Loaded loaded = IrLoader.loadAll(twoDimensions());
        List<ProducerConsumer.Form> forms = ProducerConsumer.forms(loaded.model(), loaded.prioritizedDefaults(),
                ProducerConsumer.wireNavigation(loaded));
        assertEquals(List.of("vondstgewicht[voorjaar, zuid]"), members(forms.get(0)));
        assertEquals(List.of("vondstgewicht[voorjaar, noord]", "vondstgewicht[zomer, noord]",
                "vondstgewicht[najaar, noord]"), members(forms.get(1)));
        assertEquals(List.of("vondstgewicht[voorjaar, noord]", "vondstgewicht[voorjaar, zuid]",
                "vondstgewicht[zomer, noord]", "vondstgewicht[zomer, zuid]", "vondstgewicht[najaar, noord]",
                "vondstgewicht[najaar, zuid]"), members(forms.get(3)));
    }

    // ---------------- writing cells ----------------

    /** IR-SPEC ch. 3.1, ch. 3.3: the loader carries a cell target and a creation member with labels into the model, and
     *  the rule graph writes each cell as a member of its own. In {@code jutterswet-seizoenboeking} the rule on najaar
     *  writes vondstgewicht[najaar] from vondstgewicht[voorjaar], the initialisation vondstgewicht[zomer], and the object
     *  creation vangstgewicht[najaar] of the Vangst it makes.
     *
     *  <p><b>Red before the change</b> (measured 2026-11-02 on {@code 6c397de93}): the loader refused the document,
     *  {@code IrLoader.Refusal} DROOLS-298, so neither the model nor the graph existed. */
    @Test void the_loader_carries_a_cell_write_and_the_graph_writes_the_cell() throws Exception {
        IrLoader.Loaded boeking = IrLoader.loadAll(golden("jutterswet-seizoenboeking"));
        Expr.DimensionCellRead target = assertInstanceOf(Expr.DimensionCellRead.class,
                boeking.model().resolvedRules.get(0).target());
        assertEquals("vondstgewicht", target.attribute().attribute());
        assertEquals(List.of("najaar"), target.labels());
        assertEquals(List.of("najaar"), boeking.model().objectCreations.get(0).initialisations().get(0).labels());
        List<ProducerConsumer.Form> forms = ProducerConsumer.forms(boeking.model(), boeking.prioritizedDefaults(),
                ProducerConsumer.wireNavigation(boeking));
        assertEquals(List.of("vondstgewicht[najaar]"), writes(forms.get(0)));
        assertEquals(List.of("vondstgewicht[voorjaar]"), members(forms.get(0)));
        assertEquals(List.of("vondstgewicht[zomer]"), writes(forms.get(1)));
        ProducerConsumer.Form creation = forms.stream()
                .filter(f -> f.kind() == ProducerConsumer.FormKind.OBJECT_CREATION).findFirst().orElseThrow();
        assertEquals(List.of("vangstgewicht[najaar]"), writes(creation));
        assertEquals(List.of("vondstgewicht[najaar]"), members(creation));
    }

    /** jutterswet-seizoenboeking, the worked example "Writing" of ch. 5.8c: a rule writes the cell najaar from the cell
     *  voorjaar of the same attribute, an initialisation the cell zomer, a sum folds the input cell beside the two written
     *  ones, and an object creation writes the cell najaar of the Vangst it makes, which two rules carry back to the jutter.
     *  J1 voorjaar 1.50: najaar 3.00, zomer 0.00, jaargewicht 4.50, geboekte gewicht 3.00; J2 with zomer 2.00 as input as
     *  well: the input fills the cell, so the initialisation is not at its turn, jaargewicht 6.50. The computed najaar
     *  displaces an input of the cell (J4, ch. 7.8). jutterswet-seizoenkopie: a cell is a standing target of its own, so a
     *  copy of an empty cell writes nothing and the input of the cell stands (J2 zomer 7.00). The comparison reads a written
     *  cell back from the bean. */
    @Test void a_cell_written_by_a_rule_an_initialisation_and_a_creation_plays() throws Exception {
        Path boeking = GOLDEN.resolve("jutterswet-seizoenboeking");
        assertPlays(boeking.resolve("ir.json"), List.of(boeking.resolve("case1-voorjaar-en-zomer.json"),
                boeking.resolve("case2-zonder-invoer.json"), boeking.resolve("case3-invoer-in-een-geschreven-cel.json")));
        Path kopie = GOLDEN.resolve("jutterswet-seizoenkopie");
        assertPlays(kopie.resolve("ir.json"), List.of(kopie.resolve("case1-kopie-en-staande-invoer.json")));
        Path ir = boeking.resolve("ir.json");
        assertFails(ir, caseFile(List.of(jutter("J1", cell("1.50", "voorjaar"))),
                List.of(cellValue("J1", "3.01", "najaar"))), "3.01");
        assertFails(ir, caseFile(List.of(jutter("J1", cell("1.50", "voorjaar"))), List.of(cellEmpty("J1", "zomer"))),
                "0");
    }

    /** jutterswet-cel-schrijfgrens: 1.25 maal 1.5 is 1.875, which the 2 decimals of the cell najaar do not hold (EF-006,
     *  RUNTIME-SEMANTICS ch. 2.4 point 1), its place the cell, as the reference interpreter names it. */
    @Test void a_value_a_cell_does_not_hold_is_the_fault_of_the_write_boundary() throws Exception {
        Path red = Path.of("..", "..", "corpus", "red", "jutterswet-cel-schrijfgrens");
        for (DroolsBackend.Branch branch : BRANCHES) {
            CaseRunner.RedResult r = CaseRunner.runRed(red.resolve("ir.json"), red.resolve("case1.json"), branch);
            assertEquals("FAULT", r.status(), () -> branch + ": " + r);
            assertEquals("EF-006", r.code(), () -> branch + ": " + r);
            assertEquals(List.of(Map.of("place", "j1.vondstgewicht[najaar]", "value", "15/8", "decimals", "2")),
                    r.params(), () -> branch + ": " + r);
        }
    }

    /** ch. 5.12: a rule whose target is a cell reads as a rule on an attribute, and an initialisation of a cell is
     *  displaced where its cell is filled (J2, zomer 2.00), as in the reference interpreter's {@code WV-5.12-celdoel}. */
    @Test void a_rule_on_a_cell_reads_as_a_rule_on_an_attribute() throws Exception {
        Path boeking = GOLDEN.resolve("jutterswet-seizoenboeking");
        String r0 = "/rules/rules/0/versions/0 ", r1 = "/rules/rules/1/versions/0 ";
        List<String> expected = List.of(
                r0 + "\"J1\" 0 Binary 3 \"kg\"", r0 + "\"J1\" 1 DimensionCellRead 1.5 \"kg\"", r0 + "\"J1\" 3 Literal 2",
                r0 + "\"J1\" HOLDS",
                r0 + "\"J2\" 0 Binary 3 \"kg\"", r0 + "\"J2\" 1 DimensionCellRead 1.5 \"kg\"", r0 + "\"J2\" 3 Literal 2",
                r0 + "\"J2\" HOLDS",
                r1 + "\"J1\" 0 Literal 0 \"kg\"", r1 + "\"J1\" HOLDS", r1 + "\"J2\" DISPLACED");
        for (DroolsBackend.Branch branch : BRANCHES) {
            List<String> lines = new ArrayList<>();
            for (String l : CaseRunner.readings(boeking.resolve("ir.json"), List.of(boeking.resolve(
                    "case1-voorjaar-en-zomer.json")), branch))
                if (l.startsWith("reading " + r0) || l.startsWith("reading " + r1)) lines.add(l.substring("reading ".length()));
            assertEquals(expected, lines, branch::toString);
        }
    }

    /** RUNTIME-SEMANTICS ch. 5.7 per cell: jutterswet-seizoenkopie with a second rule on each of its two cells, which
     *  computes voorjaar maal 2 where the first copies voorjaar. Where the two agree, voorjaar 0.00, the document plays;
     *  where they differ, voorjaar 1.00, each cell has two writers that disagree, and the fault names the cell. The WF-079
     *  refusal of two unconditional writers of one cell is the verifier's, which this backend does not run. */
    @Test void two_writers_of_one_cell_that_disagree_are_the_fault_of_the_cell() throws Exception {
        JsonNode ir = golden("jutterswet-seizoenkopie");
        JsonNode doubled = IrPath.version(golden("jutterswet-seizoenboeking"), "rules", "najaarsgewicht van een jutter")
                .get("value");
        ArrayNode rules = IrPath.rulesList(ir, "rules");
        for (int k = 0; k < 2; k++) {
            ObjectNode second = rules.get(k).deepCopy();
            second.put("name", second.get("name").asText() + " verdubbeld");
            ((ObjectNode) second.get("versions").get(0)).set("value", doubled.deepCopy());
            rules.add(second);
        }
        Path p = document(ir);
        assertPlays(p, List.of(caseFile(List.of(jutter("J1", cell("0.00", "voorjaar"))),
                List.of(cellValue("J1", "0.00", "najaar"), cellValue("J1", "0.00", "zomer")))));
        Path c = caseFile(List.of(jutter("J1", cell("1.00", "voorjaar"))), List.of(cellValue("J1", "1.00", "najaar")));
        for (DroolsBackend.Branch branch : BRANCHES) {
            CaseRunner.RedResult r = CaseRunner.runRed(p, c, branch);
            assertEquals("FAULT", r.status(), () -> branch + ": " + r);
            // the report carries the parameters of the first fault, the one of the rule first in the document
            assertEquals("EF-016", r.code(), () -> branch + ": " + r);
            assertEquals(List.of("vondstgewicht[najaar]"), r.params().stream().map(m -> m.get("attribute")).toList(),
                    () -> branch + ": " + r);
            assertTrue(r.detail().contains("in het attribuut vondstgewicht[najaar] van J1")
                    && r.detail().contains("in het attribuut vondstgewicht[zomer] van J1"), () -> branch + ": " + r);
        }
    }

    /** IR-SPEC ch. 5.1: a cell is a node of the graph whose cycles the exec-model branch refuses (WF-045), as the
     *  verifier draws it. A rule that computes the cell najaar from the cell voorjaar of the same attribute closes no
     *  cycle; one that computes najaar from najaar does, and so does one that computes najaar from the sum over all
     *  seasons, which reads the cell najaar among them. */
    @Test void a_cell_is_a_node_of_the_cycles_the_branch_refuses() throws Exception {
        assertNull(Cycles.refusal(IrLoader.loadAll(golden("jutterswet-seizoenboeking"))));
        assertNull(Cycles.refusal(IrLoader.loadAll(golden("jutterswet-seizoenkopie"))));
        JsonNode itself = golden("jutterswet-seizoenkopie");
        ((ArrayNode) IrPath.version(itself, "rules", "najaarsgewicht van een jutter").get("value").get("labels"))
                .set(0, "najaar");
        JsonNode sum = golden("jutterswet-seizoenboeking");
        IrPath.version(sum, "rules", "najaarsgewicht van een jutter").set("value",
                IrPath.version(sum, "rules", "jaargewicht van een jutter").get("value").deepCopy());
        for (JsonNode ir : List.of(itself, sum)) {
            Reason r = Cycles.refusal(IrLoader.loadAll(ir));
            assertNotNull(r);
            assertEquals(DroolsCode.CYCLE_OUTSIDE_RECURSIVE_CREATION, r.code(), r::text);
            assertTrue(r.text().contains("Jutter.vondstgewicht[najaar]") && r.text().contains("najaarsgewicht van een jutter"),
                    r::text);
        }
    }

    // ---------------- the documents ----------------

    /** The two-dimensional document of the worked example of ch. 5.8c: jutterswet-seizoenvouw with a second dimension
     *  strand (noord, zuid) on its vondstgewicht, and in place of its rules five rules on five new attributes: the cell
     *  voorjaar, zuid; the sum over all seasons at noord, at zuid, and at both; the minimum over all seasons at both. */
    private static JsonNode twoDimensions() throws Exception {
        JsonNode ir = golden("jutterswet-seizoenvouw");
        ObjectNode strand = IrPath.modelList(ir, "dimensions").addObject();
        strand.put("node", "Dimension").put("name", "strand").put("plural", "stranden").put("preposition", "op");
        strand.putArray("labels").add("noord").add("zuid");
        ObjectNode jutter = IrPath.objectType(ir, "Jutter");
        ObjectNode vondstgewicht = IrPath.attribute(jutter, "vondstgewicht");
        ((ArrayNode) vondstgewicht.get("dimensions")).add("strand");
        for (String a : List.of("voorjaarzuid", "noordgewicht", "zuidgewicht", "totaalgewicht", "minimumgewicht"))
            IrPath.addAttribute(jutter, a, vondstgewicht.get("datatype").deepCopy());
        ArrayNode rules = IrPath.rulesList(ir, "rules");
        ObjectNode template = (ObjectNode) rules.get(0).deepCopy();
        rules.removeAll();
        ObjectNode cellRule = rule(template, "voorjaarzuid");
        ObjectNode value = cellRule.get("versions").get(0).get("value").deepCopy();
        ObjectNode read = JSON.createObjectNode();
        read.put("node", "DimensionCellRead").set("attribute", value.get("arguments").get(0));
        read.putArray("labels").add("voorjaar").add("zuid");
        read.putObject("source").put("snippet", "zijn vondstgewicht in voorjaar op zuid");
        ((ObjectNode) cellRule.get("versions").get(0)).set("value", read);
        rules.add(cellRule);
        rules.add(fold(template, "noordgewicht", "sum", "noord"));
        rules.add(fold(template, "zuidgewicht", "sum", "zuid"));
        rules.add(fold(template, "totaalgewicht", "sum", "noord", "zuid"));
        rules.add(fold(template, "minimumgewicht", "minimum", "noord", "zuid"));
        return ir;
    }

    /** A copy of the rule {@code template} named after and writing {@code target}. */
    private static ObjectNode rule(ObjectNode template, String target) {
        ObjectNode r = template.deepCopy();
        r.put("name", target + " van een jutter");
        ObjectNode t = (ObjectNode) r.get("versions").get(0).get("target");
        t.put("attribute", target);
        t.putObject("source").put("snippet", "Het " + target + " van een Jutter");
        return r;
    }

    /** {@code target} as the fold {@code kind} of the vondstgewicht over all seasons and the stranden {@code stranden}. */
    private static ObjectNode fold(ObjectNode template, String target, String kind, String... stranden) {
        ObjectNode r = rule(template, target);
        ObjectNode value = (ObjectNode) r.get("versions").get(0).get("value");
        value.put("kind", kind);
        ObjectNode strand = ((ArrayNode) value.get("dimensionSelections")).addObject();
        strand.put("node", "DimensionSelection").put("dimension", "strand");
        ArrayNode labels = strand.putArray("labels");
        for (String s : stranden) labels.add(s);
        strand.putObject("source").put("snippet", "op " + String.join(" en ", stranden));
        return r;
    }

    private static JsonNode golden(String name) throws Exception {
        return JSON.readTree(Files.readString(GOLDEN.resolve(name).resolve("ir.json")));
    }

    private Path document(JsonNode ir) throws Exception {
        return Files.writeString(tmp.resolve("ir-" + files++ + ".json"), JSON.writeValueAsString(ir));
    }

    private Path caseFile(List<String> instances, List<String> expectations) throws Exception {
        return Files.writeString(tmp.resolve("case-" + files++ + ".json"), "{\"format\":\"uchar-case-v0\","
                + "\"asOfDate\":null,\"input\":{\"instances\":[" + String.join(",", instances) + "],\"parameters\":{}},"
                + "\"expected\":[" + String.join(",", expectations) + "]}");
    }

    /** A Jutter of the case whose vondstgewicht has the cells {@code cells}, and no input on it without one. */
    private static String jutter(String label, String... cells) {
        return instance(label, cells.length == 0 ? "{}" : "{\"vondstgewicht\":{\"cells\":[" + String.join(",", cells) + "]}}");
    }

    private static String instance(String label, String attributes) {
        return "{\"label\":\"" + label + "\",\"objectType\":\"Jutter\",\"attributes\":" + attributes
                + ",\"roles\":{},\"characteristics\":[]}";
    }

    /** One cell of a cells input: its labels, in the order of the attribute's dimensions, and its number. */
    private static String cell(String number, String... labels) {
        return "{\"labels\":" + labels(labels) + ",\"value\":{\"number\":\"" + number + "\"}}";
    }

    private static String labels(String... labels) {
        List<String> quoted = new ArrayList<>();
        for (String l : labels) quoted.add("\"" + l + "\"");
        return "[" + String.join(",", quoted) + "]";
    }

    private static String value(String label, String attribute, String number) {
        return "{\"kind\":\"attributeValue\",\"label\":\"" + label + "\",\"attribute\":\"" + attribute
                + "\",\"value\":{\"number\":\"" + number + "\"}}";
    }

    private static String empty(String label, String attribute) {
        return "{\"kind\":\"attributeEmpty\",\"label\":\"" + label + "\",\"attribute\":\"" + attribute + "\"}";
    }

    private static String cellValue(String label, String number, String... labels) {
        return "{\"kind\":\"cellValue\",\"label\":\"" + label + "\",\"attribute\":\"vondstgewicht\",\"labels\":"
                + labels(labels) + ",\"value\":{\"number\":\"" + number + "\"}}";
    }

    private static String cellEmpty(String label, String... labels) {
        return "{\"kind\":\"cellEmpty\",\"label\":\"" + label + "\",\"attribute\":\"vondstgewicht\",\"labels\":"
                + labels(labels) + "}";
    }

    // ---------------- the assertions ----------------

    private static CaseRunner.Result replay(Path ir, List<Path> cases, DroolsBackend.Branch branch) {
        return branch == DroolsBackend.Branch.EXEC_MODEL ? CaseRunner.viaExecModel(ir, cases)
                : CaseRunner.viaDrlText(ir, cases);
    }

    private static void assertPlays(Path ir, List<Path> cases) {
        for (DroolsBackend.Branch branch : BRANCHES) {
            CaseRunner.Result r = replay(ir, cases, branch);
            assertEquals("PLAYED", r.status(), () -> branch + ": " + r);
            assertEquals(cases.size(), r.casesPlayed(), () -> branch + ": " + r);
        }
    }

    private static void assertFails(Path ir, Path c, String fragment) {
        for (DroolsBackend.Branch branch : BRANCHES) {
            CaseRunner.Result r = replay(ir, List.of(c), branch);
            assertEquals("FAIL", r.status(), () -> branch + ": " + r);
            assertTrue(r.caseFailures().get(0).contains(fragment), () -> branch + ": " + r);
        }
    }

    private static void assertDefect(Path ir, Path c, String fragment) {
        for (DroolsBackend.Branch branch : BRANCHES) {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> replay(ir, List.of(c), branch), branch::toString);
            assertTrue(e.getMessage().contains(fragment), () -> branch + ": " + e.getMessage());
        }
    }

    private void assertFault(Path ir, List<String> instances, String code, Map<String, String> params) throws Exception {
        Path c = caseFile(instances, List.of(empty("J1", "jaargewicht")));
        for (DroolsBackend.Branch branch : BRANCHES) {
            CaseRunner.RedResult r = CaseRunner.runRed(ir, c, branch);
            assertEquals("FAULT", r.status(), () -> branch + ": " + r);
            assertEquals(code, r.code(), () -> branch + ": " + r);
            assertEquals(List.of(params), r.params(), () -> branch + ": " + r);
        }
    }

    private static void assertReadings(Path ir, Path c, List<String> expected) throws Exception {
        for (DroolsBackend.Branch branch : BRANCHES) {
            List<String> lines = new ArrayList<>();
            for (String l : CaseRunner.readings(ir, List.of(c), branch))
                if (l.startsWith("reading ")) lines.add(l.substring("reading ".length()));
            assertEquals(expected, lines, branch::toString);
        }
    }

    private static List<String> members(ProducerConsumer.Form form) {
        return form.reads().stream().map(ProducerConsumer.ElementKey::name).toList();
    }

    private static List<String> writes(ProducerConsumer.Form form) {
        return form.writes().stream().map(ProducerConsumer.ElementKey::name).toList();
    }
}
