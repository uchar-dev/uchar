package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static dev.uchar.regelspraak.cases.TmsDocuments.WHOLE;
import static dev.uchar.regelspraak.cases.TmsDocuments.attribute;
import static dev.uchar.regelspraak.cases.TmsDocuments.computingRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.consistent;
import static dev.uchar.regelspraak.cases.TmsDocuments.instance;
import static dev.uchar.regelspraak.cases.TmsDocuments.number;
import static dev.uchar.regelspraak.cases.TmsDocuments.ownRead;
import static dev.uchar.regelspraak.cases.TmsDocuments.value;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The write boundary of RUNTIME-SEMANTICS ch. 2.4 point 1 on both branches: a value written into a target whose
 * declared decimals do not hold it is the evaluation fault EF-006, a logical {@code Fault} in place of the write, with
 * the parameters of ch. 5.7b, the place, the exact value and the decimals; the same form with a value that fits plays.
 * The value counts and not the scale it is written at. Per form a document: a division ("zijn opbrengst gedeeld door
 * 2"), a product ({@code corpus/negative/wf023-halvering-past-niet}), a copy ({@code wf023-kopie-past-niet}), a
 * literal ({@code wf023-vast-bergloon-past-niet}), an allocation ({@code corpus/red/jutterswet-verdeling-past-niet}), an
 * initialisation ({@code wf023-initialisatie-past-niet}), a default ({@code wf023-afwijking-past-niet}), each of
 * the last two with its rules in both orders ({@code -omgekeerd}), and the member assignment of an object creation, the
 * golden {@code jutterswet-strandmelding} with an omvang of three decimals. The precision gate of WF-023 refuses every
 * one of these documents but the allocation before evaluation; the fault of the write boundary stands behind it, and the
 * negative corpus keeps the case beside the document. Beside them: an intermediate state that does
 * not fit with a fixpoint that does plays in every order of its rules (the families of the measurement of the card, an
 * initialisation overwritten ({@code src/test/resources/write-boundary/fout-op-tussenstand}, in both orders), a reader
 * withdrawn, an initialisation displaced, a chain); the input precision fence EF-004
 * on an attribute, a parameter and a timeline; the exact distribution of an allocation with one fault per allocation and
 * source; and a default whose decision does not fit decides, so the lower default of its chain is displaced, as the
 * reference interpreter lets it decide (RUNTIME-SEMANTICS ch. 7.1), in the general form of a stack and in its simple form
 * ({@code src/test/resources/write-boundary/simple-default}, the toeslag of {@code wf023-afwijking-past-niet}
 * without its exception).
 *
 * <p><b>Red</b> on {@code a0e0fa0ee}, before the card, with the shaded jar of that commit (the outcomes are in the card's
 * Result): every fault case ran to a model without faulting ({@code NOFAULT}), the exact allocation wrote
 * 1000000000000000.00 three times and the rest -0.03, and the input cases played.
 */
class WriteBoundaryWitnessTest {

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up
    private static final Path RED = Path.of("..", "..", "corpus", "red");
    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");
    // the documents the precision gate of WF-023 refuses before evaluation, with the case on which their fault stands
    private static final Path NEGATIVE = Path.of("..", "..", "corpus", "negative");
    // the documents of an intermediate state that does not fit, which the gate refuses as goldens as well
    private static final Path RESOURCES = Path.of("src", "test", "resources", "write-boundary");

    private static final String UNBOUNDED = "{\"node\":\"Number\",\"whole\":false,\"decimals\":0,\"sign\":\"unspecified\"}";

    // ---------------- (a) every writing form ----------------

    @Test void a_division_that_does_not_fit_faults_and_one_that_fits_plays() throws Exception {
        Path ir = TmsDocuments.document(TmsDocuments.model(List.of(TmsDocuments.objectType("Vondst",
                List.of(attribute("opbrengst", WHOLE), attribute("helft", WHOLE)), List.of())), List.of()),
                List.of(computingRule("helft van een vondst", "BEREKENING", "Vondst", "helft",
                        binary("dividedBy", ownRead("opbrengst", "Vondst"), number("2"), UNBOUNDED), "null")));
        fault(red(ir, vondst("V1", Map.of("opbrengst", "1001"))), "V1.helft", "1001/2", "0");
        plays(ir, vondst("V1", Map.of("opbrengst", "1000")), value("V1", "helft", "500"));
    }

    @Test void a_product_that_does_not_fit_faults_and_one_that_fits_plays() throws Exception {
        Path ir = NEGATIVE.resolve("wf023-halvering-past-niet").resolve("ir.json");
        fault(red(ir, negativeCase("wf023-halvering-past-niet")), "J1.veilingdeel", "1001/2", "0");
        plays(ir, instances(List.of(instance("J1", "Jutter", Map.of("opbrengst", "1000"), List.of(), Map.of()))),
                value("J1", "veilingdeel", "500"));
    }

    @Test void a_copy_faults_on_its_value_and_not_on_its_scale() throws Exception {
        Path ir = NEGATIVE.resolve("wf023-kopie-past-niet").resolve("ir.json");
        fault(red(ir, negativeCase("wf023-kopie-past-niet")), "V1.bergloon", "3/2", "0");
        plays(ir, vondst("V1", Map.of("taxatie", "1.00")), value("V1", "bergloon", "1"));
    }

    @Test void a_literal_that_does_not_fit_faults_and_one_that_fits_plays() throws Exception {
        Path ir = NEGATIVE.resolve("wf023-vast-bergloon-past-niet").resolve("ir.json");
        fault(red(ir, negativeCase("wf023-vast-bergloon-past-niet")), "V1.bergloon", "3/2", "0");
        // the same literal of one decimal with a whole value: 1,0 writes 1
        ObjectNode doc = (ObjectNode) TmsDocuments.JSON.readTree(ir.toFile());
        ObjectNode literal = (ObjectNode) IrPath.version(doc, "rules", 0, 0).get("value");
        literal.putObject("value").put("number", "1.0");
        plays(written(doc), vondst("V1", Map.of()), value("V1", "bergloon", "1"));
    }

    @Test void an_allocation_whose_share_does_not_fit_faults_once_and_writes_nothing() throws Exception {
        Path ir = RED.resolve("jutterswet-verdeling-past-niet").resolve("ir.json");
        JsonNode c = redCase("jutterswet-verdeling-past-niet");
        fault(red(ir, c), "J1.veilingdeel", "1001/2", "0");
        List<String> facts = faultFacts(ir, c);
        assertEquals(1, facts.size(), () -> "één fout voor de verdeling op V1: " + facts);
        plays(ir, veiling("1000", List.of("J1", "J2")), value("J1", "veilingdeel", "500"),
                value("J2", "veilingdeel", "500"), value("V1", "veilingrestant", "0"));
    }

    @Test void an_initialisation_that_does_not_fit_faults_in_both_orders_and_one_that_fits_plays() throws Exception {
        for (String name : List.of("wf023-initialisatie-past-niet", "wf023-initialisatie-past-niet-omgekeerd")) {
            Path ir = NEGATIVE.resolve(name).resolve("ir.json");
            fault(red(ir, negativeCase(name)), "V6.bergloon", "100/3", "0");
            plays(ir, vondst("V6", Map.of("opbrengst", "100", "aantal deelnemers", "4")), value("V6", "bergloon", "25"));
        }
    }

    @Test void a_default_that_does_not_fit_faults_in_both_orders_and_one_that_fits_plays() throws Exception {
        for (String name : List.of("wf023-afwijking-past-niet", "wf023-afwijking-past-niet-omgekeerd")) {
            Path ir = NEGATIVE.resolve(name).resolve("ir.json");
            fault(red(ir, negativeCase(name)), "V6.toeslag", "25/3", "2");
            plays(ir, vondst("V6", Map.of("geschatte gewicht", "30.00", "toeslaggrond", "90.00")),
                    value("V6", "toeslag", "8.00"));
        }
    }

    @Test void a_member_assignment_of_an_object_creation_that_does_not_fit_faults_and_one_that_fits_plays()
            throws Exception {
        ObjectNode doc = (ObjectNode) TmsDocuments.JSON.readTree(GOLDEN.resolve("jutterswet-strandmelding")
                .resolve("ir.json").toFile());
        ((ObjectNode) IrPath.attribute(IrPath.objectType(doc, "Melding"), "omvang")).set("datatype",
                TmsDocuments.JSON.readTree("{\"node\":\"Number\",\"whole\":false,\"decimals\":3,\"sign\":\"nonNegative\"}"));
        Path ir = written(doc);
        String m1 = instance("M1", "Melding", Map.of("omvang", "50.005"), List.of(), Map.of());
        fault(red(ir, instances(List.of(m1))), "«maak een strandrecht bij elke melding»(M1).uitkering", "10001/200", "2");
        plays(ir, instances(List.of(instance("M1", "Melding", Map.of("omvang", "50.010"), List.of(), Map.of()))),
                value("M1", "gemelde uitkering", "50.01"));
    }

    // ---------------- (b) an intermediate state that does not fit, a fixpoint that does ----------------

    @Test void an_initialisation_overwritten_plays_in_both_orders() throws Exception {
        for (String name : List.of("fout-op-tussenstand", "fout-op-tussenstand-omgekeerd")) {
            Path map = RESOURCES.resolve(name);
            List<Path> cases;
            try (Stream<Path> s = Files.list(map)) {
                cases = s.filter(f -> f.getFileName().toString().matches("case.*\\.json")).sorted().toList();
            }
            assertEquals("PLAYED", CaseRunner.viaExecModel(map.resolve("ir.json"), cases).status(), name);
            assertEquals("PLAYED", CaseRunner.viaDrlText(map.resolve("ir.json"), cases).status(), name);
        }
    }

    @Test void a_reader_withdrawn_at_the_fixpoint_plays_in_both_orders() throws Exception {
        // teller 3 in the input, 10 at the fixpoint: 10/3 on the state between, and the reader's condition gone after
        String model = vondstModel(List.of("opbrengst", "score", "teller", "aandeel"));
        String teller = computingRule("teller van een vondst", "GELIJKSTELLING", "Vondst", "teller",
                ownRead("score", "Vondst"), "null");
        String aandeel = computingRule("aandeel van een vondst", "BEREKENING", "Vondst", "aandeel",
                binary("dividedBy", ownRead("opbrengst", "Vondst"), ownRead("teller", "Vondst"), UNBOUNDED),
                TmsDocuments.comparison("less", ownRead("teller", "Vondst"), number("5")));
        for (List<String> order : List.of(List.of(aandeel, teller), List.of(teller, aandeel)))
            plays(TmsDocuments.document(model, order), vondst("V1", Map.of("opbrengst", "10", "score", "10", "teller", "3")),
                    value("V1", "teller", "10"), value("V1", "aandeel", null));
    }

    @Test void an_initialisation_that_does_not_fit_and_is_displaced_plays_in_both_orders() throws Exception {
        String model = vondstModel(List.of("x", "t", "y"));
        String init = computingRule("standaard y", "INITIALISATIE", "Vondst", "y",
                binary("dividedBy", ownRead("x", "Vondst"), number("2"), UNBOUNDED), "null");
        String writer = computingRule("schrijver van y", "GELIJKSTELLING", "Vondst", "y", ownRead("t", "Vondst"), "null");
        for (List<String> order : List.of(List.of(init, writer), List.of(writer, init)))
            plays(TmsDocuments.document(model, order), vondst("V1", Map.of("x", "1001", "t", "5")), value("V1", "y", "5"));
    }

    @Test void a_chain_over_an_initialisation_that_a_writer_replaces_plays_in_both_orders() throws Exception {
        String model = vondstModel(List.of("s", "x", "y"));
        String writer = computingRule("schrijver van x", "GELIJKSTELLING", "Vondst", "x", ownRead("s", "Vondst"), "null");
        String init = computingRule("standaard x", "INITIALISATIE", "Vondst", "x", number("1001"), "null");
        String half = computingRule("helft van x", "BEREKENING", "Vondst", "y",
                binary("dividedBy", ownRead("x", "Vondst"), number("2"), UNBOUNDED), "null");
        for (List<String> order : List.of(List.of(writer, init, half), List.of(half, init, writer)))
            plays(TmsDocuments.document(model, order), vondst("V1", Map.of("s", "1000")), value("V1", "x", "1000"),
                    value("V1", "y", "500"));
    }

    // ---------------- (d) the input precision fence ----------------

    @Test void an_input_that_does_not_fit_its_attribute_or_parameter_is_refused_as_ef004() throws Exception {
        CaseRunner.RedResult attribute = redCorpus("input-overprecision");
        assertEquals("FAULT", attribute.status(), attribute::detail);
        assertEquals("EF-004", attribute.code(), attribute::detail);
        assertEquals(List.of(Map.of("place", "v1.geschatte waarde", "value", "40.005", "decimals", "2")),
                attribute.params(), attribute::detail);
        CaseRunner.RedResult parameter = redCorpus("input-overprecision-param");
        assertEquals("EF-004", parameter.code(), parameter::detail);
        assertEquals(List.of(Map.of("place", "parameter vondstdrempel", "value", "7.5", "decimals", "0")),
                parameter.params(), parameter::detail);
    }

    @Test void a_timeline_input_is_held_to_its_declaration_segment_by_segment() throws Exception {
        Path ir = GOLDEN.resolve("jutterswet-getijdenwacht").resolve("ir.json");
        JsonNode c = TmsDocuments.JSON.readTree("""
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "J1", "objectType": "Jutter",
                        "attributes": { "wachtvergoeding per maand": { "timeline": [
                            { "from": "2023-01-01", "to": "2024-01-01", "value": { "number": "10" } },
                            { "from": "2024-01-01", "to": "2024-07-01", "value": { "number": "11.50" } } ] } },
                        "roles": {}, "characteristics": [] } ],
                    "parameters": {} } }
                """);
        CaseRunner.RedResult r = red(ir, c);
        assertEquals("EF-004", r.code(), r::detail);
        assertEquals(List.of(Map.of("place", "J1.wachtvergoeding per maand", "value", "11.5", "decimals", "0")),
                r.params(), r::detail);
    }

    @Test void a_rational_input_is_held_to_its_declaration_before_it_is_refused_as_a_form() throws Exception {
        Path ir = NEGATIVE.resolve("wf023-kopie-past-niet").resolve("ir.json");
        CaseRunner.RedResult misfit = red(ir, vondst("V1", Map.of("taxatie", "1/3")));
        assertEquals("EF-004", misfit.code(), misfit::detail);
        assertEquals(List.of(Map.of("place", "V1.taxatie", "value", "1/3", "decimals", "2")), misfit.params(),
                misfit::detail);
        // a rational that fits passes the fence, and the case reader refuses the form by name
        CaseRunner.RedResult fits = red(ir, vondst("V1", Map.of("taxatie", "3/2")));
        assertEquals("EXCLUDE", fits.status(), fits::detail);
        assertEquals("DROOLS-042", fits.droolsCode().id(), fits::detail);
    }

    // ---------------- (e) the exact distribution ----------------

    @Test void an_allocation_divides_the_exact_pot_and_floors_once() throws Exception {
        ObjectNode doc = (ObjectNode) TmsDocuments.JSON.readTree(GOLDEN.resolve("jutterswet-gelijkdeling")
                .resolve("ir.json").toFile());
        JsonNode twoDecimals = TmsDocuments.JSON.readTree(
                "{\"node\":\"Number\",\"whole\":false,\"decimals\":2,\"sign\":\"unspecified\"}");
        for (String[] member : List.of(new String[]{"Strandveiling", "veilingopbrengst"},
                new String[]{"Strandveiling", "veilingrestant"}, new String[]{"Jutter", "veilingdeel"}))
            ((ObjectNode) IrPath.attribute(IrPath.objectType(doc, member[0]), member[1])).set("datatype", twoDecimals);
        IrPath.version(doc, "allocations", 0, 0).put("roundingDecimals", 2);
        plays(written(doc), veiling("2999999999999999.97", List.of("J1", "J2", "J3")),
                value("J1", "veilingdeel", "999999999999999.99"), value("J2", "veilingdeel", "999999999999999.99"),
                value("J3", "veilingdeel", "999999999999999.99"), value("V1", "veilingrestant", "0"));
    }

    @Test void three_shares_that_do_not_fit_are_one_fault_of_the_allocation_on_its_source() throws Exception {
        Path ir = RED.resolve("jutterswet-verdeling-past-niet").resolve("ir.json");
        JsonNode c = veiling("1001", List.of("J1", "J2", "J3"));
        fault(red(ir, c), "J1.veilingdeel", "1001/3", "0");
        List<String> facts = faultFacts(ir, c);
        assertEquals(1, facts.size(), () -> "één fout voor de verdeling op V1: " + facts);
        assertTrue(facts.get(0).startsWith("fact Fault(subject=\"V1\""), facts.get(0));
    }

    // ---------------- (g) a default whose decision does not fit decides ----------------

    @Test void a_default_whose_decision_does_not_fit_displaces_the_lower_defaults() throws Exception {
        for (String name : List.of("wf023-afwijking-past-niet", "wf023-afwijking-past-niet-omgekeerd")) {
            Path ir = NEGATIVE.resolve(name).resolve("ir.json");
            JsonNode c = negativeCase(name);
            String dump = dumpOnBothBranches(ir, c);
            List<String> toeslag = dump.lines().filter(r -> r.startsWith("fact ") && r.contains("toeslag")).toList();
            // the deviation decided, the outcome of its chain stands with its exact decision, its write is the fault,
            // and the rule beneath it, "op 1,00", wrote nothing
            assertTrue(toeslag.stream().noneMatch(r -> r.startsWith("fact Value(")), () -> name + ":\n" + dump);
            assertTrue(toeslag.stream().anyMatch(r -> r.startsWith("fact Outcome(") && r.contains("position=Integer(0)")
                    && r.contains("kind=Kind.DECIDED, value=Breuk(\"25/3\")")), () -> name + ":\n" + dump);
            assertEquals(1, faultFacts(ir, c).size(), () -> name + ":\n" + dump);
        }
        // the simple form of the same stack, without the exception: the control fact of the deviation, its Decides,
        // stands beside the fault and displaces the rule beneath it
        Path ir = Path.of("src", "test", "resources", "write-boundary", "simple-default", "ir.json");
        JsonNode c = negativeCase("wf023-afwijking-past-niet");
        ((ObjectNode) c.get("input").get("instances").get(0)).putObject("attributes").set("geschatte gewicht",
                TmsDocuments.JSON.readTree("{\"number\":\"30.00\"}"));
        ((ObjectNode) c.get("input").get("instances").get(0).get("attributes")).set("toeslaggrond",
                TmsDocuments.JSON.readTree("{\"number\":\"100.00\"}"));
        fault(red(ir, c), "V6.toeslag", "25/3", "2");
        String dump = dumpOnBothBranches(ir, c);
        List<String> facts = dump.lines().filter(r -> r.startsWith("fact ") && !r.startsWith("fact bean")
                && !r.startsWith("fact Fired")).toList();
        assertEquals(2, facts.size(), () -> "het besluit en de fout:\n" + dump);
        assertTrue(facts.stream().anyMatch(r -> r.startsWith("fact Decides(subject=\"V6\"") && r.contains("toeslag")
                && r.contains("index=Integer(0)")), () -> "de afwijking besliste:\n" + dump);
        assertTrue(facts.stream().anyMatch(r -> r.startsWith("fact Fault(subject=\"V6\"")), () -> dump);
    }

    // ---------------- helpers ----------------

    /** A binary operation in its wire spelling, of the type {@code type}. */
    private static String binary(String op, String left, String right, String type) {
        return "{\"node\":\"Binary\",\"op\":\"" + op + "\",\"left\":" + left + ",\"right\":" + right + ",\"type\":" + type
                + ",\"source\":{\"snippet\":\"" + op + "\"}}";
    }

    /** A model with one object type, Vondst, whose whole-number attributes are {@code attributes}. */
    private static String vondstModel(List<String> attributes) {
        List<String> a = new ArrayList<>();
        for (String name : attributes) a.add(attribute(name, WHOLE));
        return TmsDocuments.model(List.of(TmsDocuments.objectType("Vondst", a, List.of())), List.of());
    }

    /** A case of one Vondst {@code label} with {@code numbers}. */
    private static JsonNode vondst(String label, Map<String, String> numbers) throws Exception {
        return instances(List.of(instance(label, "Vondst", numbers, List.of(), Map.of())));
    }

    /** A case of the Strandveiling V1 with veilingopbrengst {@code pot} over the jutters {@code jutters}. */
    private static JsonNode veiling(String pot, List<String> jutters) throws Exception {
        List<String> i = new ArrayList<>();
        i.add(TmsDocuments.instanceWithRoles("V1", "Strandveiling", Map.of("veilingopbrengst", pot), List.of(),
                Map.of("veilingjutters", jutters)));
        for (String j : jutters) i.add(instance(j, "Jutter", Map.of(), List.of(), Map.of()));
        return instances(i);
    }

    /** A case of {@code instances} without expectations. */
    private static JsonNode instances(List<String> instances) throws Exception {
        return TmsDocuments.caseDocument(instances, List.of());
    }

    /** The case of the red corpus case {@code name}. */
    private static JsonNode redCase(String name) throws Exception {
        return TmsDocuments.JSON.readTree(RED.resolve(name).resolve("case1.json").toFile());
    }

    /** The case beside the negative corpus document {@code name}. */
    private static JsonNode negativeCase(String name) throws Exception {
        return TmsDocuments.JSON.readTree(NEGATIVE.resolve(name).resolve("case1.json").toFile());
    }

    /** The red path of the red corpus case {@code name} on both branches. */
    private static CaseRunner.RedResult redCorpus(String name) throws Exception {
        return red(RED.resolve(name).resolve("ir.json"), redCase(name));
    }

    /** {@code doc} written to a temporary file. */
    private static Path written(JsonNode doc) throws Exception {
        Path f = Files.createTempFile("schrijfgrens", ".json");
        f.toFile().deleteOnExit();
        return Files.writeString(f, doc.toString());
    }

    /** The red path of {@code c} on {@code ir}, on both branches, which give the same result. */
    private static CaseRunner.RedResult red(Path ir, JsonNode c) throws Exception {
        Path f = written(c);
        CaseRunner.RedResult exec = CaseRunner.runRed(ir, f, DroolsBackend.Branch.EXEC_MODEL);
        assertEquals(exec, CaseRunner.runRed(ir, f, DroolsBackend.Branch.DRL_TEXT), "de twee takken, " + ir);
        return exec;
    }

    /** {@code r} is the one fault of the write boundary at {@code place}, of the exact {@code value} and the declared
     *  {@code decimals}. */
    private static void fault(CaseRunner.RedResult r, String place, String value, String decimals) {
        assertEquals("FAULT", r.status(), r::detail);
        assertEquals("EF-006", r.code(), r::detail);
        assertEquals(List.of(Map.of("place", place, "value", value, "decimals", decimals)), r.params(), r::detail);
        assertTrue(r.detail().startsWith("WriteBoundaryFault: "), r::detail);
    }

    /** {@code c} with {@code expectations} plays on both branches. */
    private static void plays(Path ir, JsonNode c, String... expectations) throws Exception {
        ObjectNode withExpectations = c.deepCopy();
        ArrayNode e = withExpectations.putArray("expected");
        for (String x : expectations) e.add(TmsDocuments.JSON.readTree(x));
        e.add(TmsDocuments.JSON.readTree(consistent()));
        assertEquals("gespeeld", TmsFaultWitnessTest.playBoth(ir, withExpectations), ir.toString());
    }

    /** The dump of the session of {@code c} on {@code ir}, the same on both branches but for the firings. */
    private static String dumpOnBothBranches(Path ir, JsonNode c) throws Exception {
        String exec = BranchDumps.withoutFirings(BranchDumps.dump(ir, c, DroolsBackend.Branch.EXEC_MODEL));
        assertEquals(exec, BranchDumps.withoutFirings(BranchDumps.dump(ir, c, DroolsBackend.Branch.DRL_TEXT)),
                ir.toString());
        return exec;
    }

    /** The {@code Fault} facts of the session of {@code c} on {@code ir} at its fixpoint, on both branches alike. */
    private static List<String> faultFacts(Path ir, JsonNode c) throws Exception {
        return dumpOnBothBranches(ir, c).lines().filter(r -> r.startsWith("fact Fault(")).toList();
    }
}
