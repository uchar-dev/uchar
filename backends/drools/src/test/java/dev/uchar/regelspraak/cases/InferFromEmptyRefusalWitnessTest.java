package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.refusal.DroolsCode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * A document that carries {@code inferFromEmpty: false} and holds a form whose reading of RUNTIME-SEMANTICS ch. 1.5 this
 * backend does not carry is refused by the build, {@link DroolsBackend#build}, with DROOLS-370 and the slug of that
 * form ({@code UncarriedForms}): on both branches, with and without readings, before anything is built, and the CLI
 * reports it in the status line {@code EXCLUDE DROOLS-370: <sentence>}. The fixture is the witness {@code object-creation}
 * of the reference interpreter; the same document without
 * the property builds and plays. The forms the analysis refuses where it lowers them each have a document here, beside its
 * neighbour that is carried. The list opens with the table "Forms without the reading" of ch. 1.5, in its
 * order, and the build with readings refuses a document whose forms this backend carries with the slug
 * {@code reading}.
 */
class InferFromEmptyRefusalWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** cwd under surefire is the module dir (backends/drools); ref stands two levels up. */
    private static final Path WITNESS = Path.of("..", "..", "ref", "test", "fixtures", "strict-empty", "object-creation");

    /** A case in which every value is filled, so the reading of an empty value decides nothing in it. */
    private static final Path CASE = WITNESS.resolve("case2-onervaren.json");

    private static ObjectNode witness() throws Exception {
        return (ObjectNode) JSON.readTree(Files.readString(WITNESS.resolve("ir.json")));
    }

    private static String sentence() {
        return DroolsCode.INFER_FROM_EMPTY_FALSE.reason("form", "object-creation").text();
    }

    @Test void the_build_refuses_the_document_on_both_branches_with_and_without_readings() throws Exception {
        for (DroolsBackend.Branch branch : DroolsBackend.Branch.values())
            for (boolean readings : List.of(false, true)) {
                ObjectNode ir = witness();
                CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                        () -> DroolsBackend.build(ir, branch, readings), () -> branch + " readings=" + readings);
                assertEquals(DroolsCode.INFER_FROM_EMPTY_FALSE, e.code());
                assertEquals(sentence(), e.getMessage());
            }
    }

    @Test void the_status_line_names_the_code_and_its_sentence_on_both_branches() throws Exception {
        String ir = WITNESS.resolve("ir.json").toString();
        for (List<String> flags : List.of(List.<String>of(), List.of("--drl"), List.of("--readings"),
                List.of("--readings", "--drl")))
            assertEquals("EXCLUDE DROOLS-370: " + sentence(), statusLine(flags, ir, CASE, 2), flags::toString);
    }

    @Test void the_sentence_in_both_languages() {
        var reason = DroolsCode.INFER_FROM_EMPTY_FALSE.reason("form", "set-fold");
        assertEquals("the document carries inferFromEmpty false, a reading in which nothing follows from an empty "
                + "value, and this backend does not carry that reading for all that is asked (form: set-fold)",
                reason.text(Locale.ENGLISH));
        assertEquals("het document draagt inferFromEmpty false, een lezing waarin uit een lege waarde niets volgt, en "
                + "deze backend draagt die lezing niet voor alles wat gevraagd is (vorm: set-fold)",
                reason.text(Locale.forLanguageTag("nl")));
    }

    /** The build with readings refuses a document with the property whose forms this backend carries, with the slug
     *  {@code reading}, on both branches; the build without readings builds it. */
    @Test void the_build_with_readings_refuses_a_carried_document() throws Exception {
        for (String fixture : List.of("derived", "stack-exception-groups")) {
            Path ir = Path.of("src", "test", "resources", "open-values", fixture, "ir.json");
            for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) {
                CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class, () -> DroolsBackend.build(ir, branch, true),
                        branch::name);
                assertEquals(DroolsCode.INFER_FROM_EMPTY_FALSE.reason("form", "reading").text(), e.getMessage());
                DroolsBackend.build(ir, branch, false);
            }
        }
    }

    /** A declared-unordered group of two defaults groups of which one has an initialisation is the one defaults group the
     *  backend does not carry: whether the initialisation closes over the open default of its own group is asked per
     *  group. Without it the same group is carried. */
    @Test void groups_without_an_order_of_which_one_has_an_initialisation_are_refused() throws Exception {
        Path path = Path.of("src", "test", "resources", "open-values", "stack-unordered", "ir.json");
        ObjectNode ir = (ObjectNode) JSON.readTree(Files.readString(path));
        for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) DroolsBackend.build(ir.deepCopy(), branch, false);
        ObjectNode initialisation = ((ArrayNode) ir.at("/rules/prioritizedDefaults/1/defaults")).addObject();
        initialisation.put("node", "Default");
        initialisation.set("consequence", ir.at("/rules/prioritizedDefaults/1/defaults/0/consequence").deepCopy());
        initialisation.put("initialisation", true);
        for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) {
            CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                    () -> DroolsBackend.build(ir.deepCopy(), branch, false), branch::name);
            assertEquals(DroolsCode.INFER_FROM_EMPTY_FALSE.reason("form", "defaults-group").text(), e.getMessage());
        }
    }

    /** WV-1.5-uncarried-forms: the list opens with the table "Forms without the reading" of RUNTIME-SEMANTICS ch. 1.5,
     *  read from the spec, in its order; the slugs after it are this backend's own and none is in the table. */
    @Test void the_list_opens_with_the_table_of_the_spec() throws Exception {
        String spec = Files.readString(Path.of("..", "..", "spec", "RUNTIME-SEMANTICS.md"));
        int start = spec.indexOf("**Forms without the reading.**"), end = spec.indexOf("The suite holds this table", start);
        List<String> table = new ArrayList<>();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?m)^\\| `([a-z-]+)` \\|").matcher(spec.substring(start, end));
        while (m.find()) table.add(m.group(1));
        assertEquals(UncarriedForms.TABLE, table.size());
        assertEquals(table, UncarriedForms.SLUGS.subList(0, UncarriedForms.TABLE));
        for (String own : UncarriedForms.SLUGS.subList(UncarriedForms.TABLE, UncarriedForms.SLUGS.size()))
            assertEquals(false, table.contains(own), own);
    }

    /** WV-1.5-let-in-composite-*: a Let whose binder a member of a composite other than the first reads, at any depth,
     *  is named {@code let-in-composite}, on the documents of the reference interpreter's suite ({@code ref/test}): "de
     *  uitkering van een Vondst is, daarbij geldt: de toeslag is zijn vindloon, 10 als [condition]; anders 0". The slug
     *  stands before the slugs of this backend's own, so it is named although the document holds an IfThenElse. */
    @Test void a_let_read_in_a_later_member_of_a_composite_is_named() throws Exception {
        String toeslag = greater(binder("toeslag"), "5"), gewicht = greater(attribute("gewicht"), "10"),
                schade = greater(attribute("schade"), "1");
        Map<String, String> bodies = new LinkedHashMap<>();
        bodies.put("later-member", choice(any(gewicht, toeslag), literal("10"), literal("0")));
        bodies.put("later-member-of-a-first-member", choice(all(any(gewicht, toeslag), schade), literal("10"), literal("0")));
        bodies.put("first-member-of-a-later-member", choice(all(gewicht, any(toeslag, schade)), literal("10"), literal("0")));
        bodies.put("choice-in-a-later-member",
                choice(all(gewicht, greater(choice(toeslag, literal("1"), literal("0")), "0")), literal("10"), literal("0")));
        bodies.put("let-value-in-a-later-member", choice(all(gewicht,
                greater(let("verhoogd", plus(binder("toeslag"), literal("1")), binder("verhoogd")), "5")), literal("10"), literal("0")));
        bodies.put("under-a-let", let("zwaarte", attribute("gewicht"),
                choice(any(greater(binder("zwaarte"), "10"), toeslag), literal("10"), literal("0"))));
        for (Map.Entry<String, String> e : bodies.entrySet())
            assertEquals("let-in-composite", first(uitkering(e.getValue())), e.getKey());
        for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) {
            CaseRunner.Excluded refused = assertThrows(CaseRunner.Excluded.class,
                    () -> DroolsBackend.build(uitkering(bodies.get("later-member")), branch, false), branch::name);
            assertEquals(DroolsCode.INFER_FROM_EMPTY_FALSE.reason("form", "let-in-composite").text(), refused.getMessage());
        }
    }

    /** WV-1.5-let-in-composite-quantifier-fold-*: the same in the filter and in the matrix of a quantifier fold, the
     *  rules 0, 1 and 2 of the golden {@code jutterswet-grens-per-jutter} with the property; its rule 3 reads the binder
     *  in the filter of a count, no quantifier fold. */
    @Test void a_let_read_in_a_quantifier_fold_is_named() throws Exception {
        for (int rule : List.of(0, 1, 2)) assertEquals("let-in-composite", first(grens(rule)), "rule " + rule);
        assertEquals(null, first(grens(3)));
    }

    /** {@code set-fold}: a fold over a set whose filter holds a fold that can be open, and a sum whose fallback reads a
     *  member that can be open. Where every input is complete, nothing can be open, and the document is carried. */
    @Test void a_fold_over_a_set_outside_the_bound_is_refused() throws Exception {
        refused("set-fold", "eerste schone maand vanaf de eindmaand", golden("fold-filter-inner-fold-via-element-role"));
        ObjectNode fallback = fixture("set-fold");
        ObjectNode sum = (ObjectNode) fallback.at("/rules/rules/7/versions/0/value");
        assertEquals("sum", sum.get("kind").asText(), "the aangemeld gewicht, which has a fallback");
        sum.set("emptyFallback", fallback.at("/rules/rules/1/versions/0/target").deepCopy());
        refused("set-fold", "aangemeld gewicht", fallback);
    }

    /** {@code quantifier-fold}: one whose filter reads a member that can be open. */
    @Test void a_quantifier_fold_under_a_filter_that_can_be_open_is_refused() throws Exception {
        refused("quantifier-fold", "keurig bij sterkte", golden("jutterswet-vuurtoren"));
    }

    /** A filter and a matrix that can fault or deposit over a member that can be open are carried: a division, a power, a
     *  parameter, in a fold over a set, a union and a quantifier fold. */
    @Test void a_fold_whose_order_shows_is_carried() throws Exception {
        for (String name : List.of("fold-filter-divides-by-zero", "jutterswet-filterwacht", "jutterswet-macht-van-nul",
                "jutterswet-kwantorvouw-en-unie", "jutterswet-vouwfilter-buiten-de-beurt", "jutterswet-keurgemiddelde",
                "jutterswet-loodsgrens"))
            for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) DroolsBackend.build(golden(name), branch, false);
    }

    /** {@code anchored-read}: an anchored read of an input member that can be open, in the filter of a fold. */
    @Test void an_anchored_read_of_an_open_input_is_refused() throws Exception {
        refused("anchored-read", "hoogste latere tarief", golden("jutterswet-registervoorrang"));
    }

    /** {@code ir} is refused on both branches with {@code slug}, in the sentence of its rule {@code rule}, and is built once
     *  every input is complete. */
    private static void refused(String slug, String rule, ObjectNode ir) {
        String sentence = DroolsCode.INFER_FROM_EMPTY_FALSE.reason("form", slug).text();
        ObjectNode complete = ir.deepCopy();
        for (JsonNode list : List.of("attributes", "characteristics").stream()
                .flatMap(k -> complete.at("/model/objectTypes").findValues(k).stream()).toList())
            for (JsonNode member : list) ((ObjectNode) member).put("complete", true);
        for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) {
            CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                    () -> DroolsBackend.build(ir.deepCopy(), branch, false), () -> branch + " " + rule);
            assertEquals(DroolsCode.INFER_FROM_EMPTY_FALSE, e.code(), rule);
            assertEquals("transpiler SKIP: regel: " + rule + " — " + sentence, e.getMessage());
            DroolsBackend.build(complete.deepCopy(), branch, false);
        }
    }

    /** The golden {@code name} with the property. */
    private static ObjectNode golden(String name) throws Exception {
        ObjectNode ir = (ObjectNode) JSON.readTree(Files.readString(
                Path.of("..", "..", "corpus", "golden", name, "ir.json")));
        ir.put("inferFromEmpty", false);
        return ir;
    }

    /** The document of the fixture {@code name} of {@code OpenValuesWitnessTest}. */
    private static ObjectNode fixture(String name) throws Exception {
        return (ObjectNode) JSON.readTree(Files.readString(
                Path.of("src", "test", "resources", "open-values", name, "ir.json")));
    }

    /** WV-1.5-let-in-composite-not-*: a Let whose binder is read in a first member only, outside every condition, under
     *  an inner Let of the same name, or inside the later member that holds the Let, is not named: the document holds
     *  the IfThenElse this backend does not carry, or no form it does not carry. */
    @Test void a_let_read_elsewhere_is_not_named() throws Exception {
        String toeslag = greater(binder("toeslag"), "5"), gewicht = greater(attribute("gewicht"), "10"),
                schade = greater(attribute("schade"), "1");
        Map<String, String> bodies = new LinkedHashMap<>();
        bodies.put("first-member", choice(any(toeslag, gewicht), literal("10"), literal("0")));
        bodies.put("first-of-a-first", choice(all(any(toeslag, gewicht), schade), literal("10"), literal("0")));
        bodies.put("outside-a-condition", choice(all(gewicht, schade), literal("7"), binder("toeslag")));
        bodies.put("shadowed", choice(all(gewicht, greater(let("toeslag", attribute("schade"), binder("toeslag")), "1")),
                literal("7"), binder("toeslag")));
        for (Map.Entry<String, String> e : bodies.entrySet())
            assertEquals("if-then-else", first(uitkering(e.getValue())), e.getKey());
        assertEquals(null, first(beloond(all(gewicht,
                greater(let("toeslag", attribute("vindloon"), plus(binder("toeslag"), literal("1"))), "5")))));
    }

    /** The slug {@link UncarriedForms} names for {@code ir}, null where it names none. */
    private static String first(ObjectNode ir) {
        IrLoader.Loaded loaded = IrLoader.loadWithPositions(ir).loaded();
        return UncarriedForms.first(loaded.model(), loaded.prioritizedDefaults(), false);
    }

    /** The golden jutterswet-grens-per-jutter with the property and only its rule {@code n}. */
    private static ObjectNode grens(int n) throws Exception {
        ObjectNode ir = (ObjectNode) JSON.readTree(Files.readString(
                Path.of("..", "..", "corpus", "golden", "jutterswet-grens-per-jutter", "ir.json")));
        ir.put("inferFromEmpty", false);
        ObjectNode rules = (ObjectNode) ir.get("rules");
        rules.set("rules", JSON.createArrayNode().add(rules.get("rules").get(n)));
        return ir;
    }

    /** "De uitkering van een Vondst is, daarbij geldt: de toeslag is zijn vindloon, {@code body}". */
    private static ObjectNode uitkering(String body) throws Exception {
        return document(rule("uitkering", let("toeslag", attribute("vindloon"), body), null, ",\"presentation\":\"calculation\""));
    }

    /** "Een Vondst is beloond indien {@code condition}". */
    private static ObjectNode beloond(String condition) throws Exception {
        return document(rule("beloond", "{\"node\":\"Literal\",\"value\":{\"boolean\":true},\"type\":{\"node\":\"Boolean\"}}",
                condition, ""));
    }

    private static final String NUMBER = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\"}";

    /** A document with the property, a Vondst and {@code rule} beside the vindloon of ch. 1.5. */
    private static ObjectNode document(String rule) throws Exception {
        StringBuilder attributes = new StringBuilder();
        for (String a : List.of("taxatie", "vindloon", "gewicht", "schade", "uitkering"))
            attributes.append(attributes.isEmpty() ? "" : ",")
                    .append("{\"node\":\"Attribute\",\"name\":\"" + a + "\",\"datatype\":" + NUMBER + "}");
        return (ObjectNode) JSON.readTree("{\"format\":\"uchar-ir-v1\",\"inferFromEmpty\":false,\"model\":{\"objectTypes\":["
                + "{\"node\":\"ObjectType\",\"name\":\"Vondst\",\"animate\":false,\"plural\":\"Vondsten\",\"attributes\":[" + attributes
                + "],\"characteristics\":[{\"node\":\"Characteristic\",\"name\":\"beloond\",\"kind\":\"adjectival\"}]}],"
                + "\"factTypes\":[],\"parameters\":[],\"domains\":[],\"unitSystems\":[],\"dimensions\":[],\"timelines\":[],"
                + "\"dayKinds\":[]},\"rules\":{\"rules\":["
                + rule("vindloon", literal("10"), greater(attribute("taxatie"), "1000"), ",\"presentation\":\"calculation\"") + "," + rule
                + "],\"dayKindDefinitions\":[],\"objectCreations\":[],\"allocations\":[],\"consistencies\":[],\"uniquenesses\":[],"
                + "\"factCreations\":[],\"startPointDeterminations\":[],\"ruleGroups\":[]},\"diagnostics\":[]}");
    }

    private static String rule(String member, String value, String condition, String presentation) {
        return "{\"node\":\"Rule\",\"name\":\"" + member + "\",\"versions\":[{\"validity\":{\"node\":\"Validity\"},\"target\":"
                + attribute(member) + ",\"value\":" + value + (condition == null ? "" : ",\"condition\":" + condition)
                + ",\"initialisation\":false" + presentation + "}]}";
    }

    private static String attribute(String name) {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"" + name + "\",\"subjectType\":\"Vondst\"}";
    }

    private static String literal(String number) {
        return "{\"node\":\"Literal\",\"value\":{\"number\":\"" + number + "\"},\"type\":" + NUMBER + "}";
    }

    private static String binder(String name) {
        return "{\"node\":\"BinderRef\",\"name\":\"" + name + "\"}";
    }

    private static String plus(String left, String right) {
        return "{\"node\":\"Binary\",\"op\":\"plus\",\"left\":" + left + ",\"right\":" + right + ",\"type\":" + NUMBER + "}";
    }

    private static String let(String binder, String value, String body) {
        return "{\"node\":\"Let\",\"type\":" + NUMBER + ",\"binder\":\"" + binder + "\",\"value\":" + value + ",\"body\":" + body + "}";
    }

    private static String choice(String condition, String then, String otherwise) {
        return "{\"node\":\"IfThenElse\",\"type\":" + NUMBER + ",\"condition\":" + condition + ",\"then\":" + then + ",\"else\":"
                + otherwise + "}";
    }

    private static String greater(String left, String number) {
        return "{\"node\":\"Comparison\",\"op\":\"greater\",\"left\":" + left + ",\"right\":" + literal(number) + "}";
    }

    private static String all(String... parts) {
        return "{\"node\":\"Compound\",\"quantifier\":\"all\",\"threshold\":0,\"parts\":[" + String.join(",", parts) + "]}";
    }

    private static String any(String... parts) {
        return "{\"node\":\"Compound\",\"quantifier\":\"atLeast\",\"threshold\":1,\"parts\":[" + String.join(",", parts) + "]}";
    }

    @Test void without_the_property_the_document_builds_and_plays_on_both_branches(@TempDir Path dir)
            throws Exception {
        ObjectNode ir = witness();
        ir.remove("inferFromEmpty");
        Path irPath = dir.resolve("ir.json");
        Files.writeString(irPath, JSON.writerWithDefaultPrettyPrinter().writeValueAsString(ir));
        for (List<String> flags : List.of(List.<String>of(), List.of("--drl")))
            assertEquals("PLAYED 1", statusLine(flags, irPath.toString(), CASE, 0), flags::toString);
    }

    /** The last line {@code DroolsBackendMain} prints for {@code flags}, {@code ir} and {@code casePath}, whose exit
     *  code must be {@code exit}. */
    private static String statusLine(List<String> flags, String ir, Path casePath, int exit) {
        List<String> arguments = new ArrayList<>(flags);
        arguments.add(ir);
        arguments.add(casePath.toString());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int code = DroolsBackendMain.run(arguments.toArray(String[]::new), new PrintStream(out, true, StandardCharsets.UTF_8));
        List<String> lines = out.toString(StandardCharsets.UTF_8).lines().toList();
        String last = lines.isEmpty() ? "<no output>" : lines.get(lines.size() - 1);
        assertEquals(exit, code, () -> flags + ": " + last);
        return last;
    }
}
