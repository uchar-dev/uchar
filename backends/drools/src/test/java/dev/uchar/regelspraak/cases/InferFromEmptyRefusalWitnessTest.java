package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.uchar.regelspraak.refusal.DroolsCode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * A document that carries {@code inferFromEmpty: false} and holds a form whose reading of RUNTIME-SEMANTICS ch. 1.5 this
 * backend does not carry is refused by the build, {@link DroolsBackend#build}, with DROOLS-370 and the slug of that
 * form ({@code UncarriedForms}): on both branches, with and without readings, before anything is built, and the CLI
 * reports it in the status line {@code EXCLUDE DROOLS-370: <sentence>}. The fixture is the golden
 * {@code jutterswet-afleiden-uit-leeg}, whose defaults group comes first of its forms in that list; the same document
 * without the property builds and plays. The list opens with the table "Forms without the reading" of ch. 1.5, in its
 * order, and the build with readings refuses a document whose forms this backend carries with the slug
 * {@code reading}.
 */
class InferFromEmptyRefusalWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** cwd under surefire is the module dir (backends/drools); the corpus is two levels up. */
    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden", "jutterswet-afleiden-uit-leeg");

    /** A Vondst whose every value is filled, so the reading of an empty value decides nothing in it. */
    private static final String CASE = """
            {
              "format": "uchar-case-v0",
              "asOfDate": null,
              "input": {
                "instances": [
                  {
                    "label": "V1",
                    "objectType": "Vondst",
                    "attributes": {
                      "taxatie": {"number": "1500"},
                      "gewicht": {"number": "3"}
                    },
                    "roles": {},
                    "characteristics": []
                  }
                ],
                "parameters": {}
              },
              "expected": [
                {"kind": "characteristicPresent", "label": "V1", "characteristic": "kostbaar"},
                {"kind": "attributeValue", "label": "V1", "attribute": "vindloon", "value": {"number": "13"}},
                {"kind": "consistent"}
              ]
            }
            """;

    private static ObjectNode golden() throws Exception {
        return (ObjectNode) JSON.readTree(Files.readString(GOLDEN.resolve("ir.json")));
    }

    private static String sentence() {
        return DroolsCode.INFER_FROM_EMPTY_FALSE.reason("form", "defaults-group").text();
    }

    @Test void the_build_refuses_the_document_on_both_branches_with_and_without_readings() throws Exception {
        for (DroolsBackend.Branch branch : DroolsBackend.Branch.values())
            for (boolean readings : List.of(false, true)) {
                ObjectNode ir = golden();
                CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                        () -> DroolsBackend.build(ir, branch, readings), () -> branch + " readings=" + readings);
                assertEquals(DroolsCode.INFER_FROM_EMPTY_FALSE, e.code());
                assertEquals(sentence(), e.getMessage());
            }
    }

    @Test void the_status_line_names_the_code_and_its_sentence_on_both_branches(@TempDir Path dir) throws Exception {
        Path casePath = dir.resolve("case1-kostbaar.json");
        Files.writeString(casePath, CASE);
        String ir = GOLDEN.resolve("ir.json").toString();
        for (List<String> flags : List.of(List.<String>of(), List.of("--drl"), List.of("--readings"),
                List.of("--readings", "--drl")))
            assertEquals("EXCLUDE DROOLS-370: " + sentence(), statusLine(flags, ir, casePath, 2), flags::toString);
    }

    @Test void the_sentence_in_both_languages() {
        var reason = DroolsCode.INFER_FROM_EMPTY_FALSE.reason("form", "defaults-group");
        assertEquals("the document carries inferFromEmpty false, a reading in which nothing follows from an empty "
                + "value, and this backend does not carry that reading (form: defaults-group)", reason.text(Locale.ENGLISH));
        assertEquals("het document draagt inferFromEmpty false, een lezing waarin uit een lege waarde niets volgt, en "
                + "deze backend draagt die lezing niet (vorm: defaults-group)", reason.text(Locale.forLanguageTag("nl")));
    }

    /** The build with readings refuses a document with the property whose forms this backend carries, with the slug
     *  {@code reading}, on both branches; the build without readings builds it. */
    @Test void the_build_with_readings_refuses_a_carried_document() throws Exception {
        Path ir = Path.of("src", "test", "resources", "open-values", "derived", "ir.json");
        for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) {
            CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class, () -> DroolsBackend.build(ir, branch, true),
                    branch::name);
            assertEquals(DroolsCode.INFER_FROM_EMPTY_FALSE.reason("form", "reading").text(), e.getMessage());
            DroolsBackend.build(ir, branch, false);
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

    @Test void without_the_property_the_document_builds_and_plays_on_both_branches(@TempDir Path dir)
            throws Exception {
        ObjectNode ir = golden();
        ir.remove("inferFromEmpty");
        Path irPath = dir.resolve("ir.json"), casePath = dir.resolve("case1-kostbaar.json");
        Files.writeString(irPath, JSON.writerWithDefaultPrettyPrinter().writeValueAsString(ir));
        Files.writeString(casePath, CASE);
        for (List<String> flags : List.of(List.<String>of(), List.of("--drl")))
            assertEquals("PLAYED 1", statusLine(flags, irPath.toString(), casePath, 0), flags::toString);
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
