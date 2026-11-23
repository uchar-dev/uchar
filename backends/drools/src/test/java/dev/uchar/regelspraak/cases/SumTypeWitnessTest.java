package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.refusal.DroolsCode;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The sum types of RUNTIME-SEMANTICS ch. 9 on the documents of the reference interpreter's suite, read where they stand,
 * {@code ref/test/fixtures/sum-types/<name>/}, with their own case files and expectations: a document whose forms this
 * backend carries plays every case on both branches (a constructor value written by a rule, its payload computed, and
 * a constructor name of several words); a document with a form of the table of ch. 9.4 is refused on both branches with
 * DROOLS-373 and the slug of the first such form ({@code SumTypeForms}), a document with two forms for the first of the
 * table; a {@code Construct} in a document with {@code inferFromEmpty: false} with DROOLS-370 and the slug
 * {@code construct} ({@code UncarriedForms}); and a {@code Match} by the loader with DROOLS-021. An object creation that
 * gives a value to a characteristic which shares its name with a sum-typed attribute (IR-SPEC WF-002) is no form of
 * ch. 9.4: the build passes {@code SumTypeForms} and refuses the document later, as a limit of the bean model, which
 * holds one property per name (DROOLS-054). The list names every document once.
 */
class SumTypeWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path DOCUMENTS = Path.of("..", "..", "ref", "test", "fixtures", "sum-types");

    /** Per document, null where it plays, else its refusal: the code and, for a form, its slug. */
    private static final Map<String, List<Object>> DOCUMENT = new LinkedHashMap<>();
    static {
        for (String plays : List.of("computed-payload", "constructor-of-several-words", "payload-at-another-scale"))
            DOCUMENT.put(plays, null);
        DOCUMENT.put("characteristic-in-a-creation", List.of(DroolsCode.BEAN_NAME_COLLISION_USE));
        DOCUMENT.put("construct-in-a-strict-document", List.of(DroolsCode.INFER_FROM_EMPTY_FALSE, "construct"));
        DOCUMENT.put("match-as-payload", List.of(DroolsCode.EXPR_MATCH));
        for (String form : List.of("timeline", "dimension", "creation", "payload-unit", "defaults-group"))
            DOCUMENT.put("forms-not-carried/" + form, List.of(DroolsCode.SUM_TYPE_FORM_NOT_CARRIED, form));
        DOCUMENT.put("forms-not-carried/initialisation", List.of(DroolsCode.SUM_TYPE_FORM_NOT_CARRIED, "defaults-group"));
        DOCUMENT.put("forms-not-carried/creation-and-initialisation",
                List.of(DroolsCode.SUM_TYPE_FORM_NOT_CARRIED, "creation"));
    }

    @Test void every_document_plays_or_is_refused_by_name() throws Exception {
        List<String> found = new ArrayList<>();
        try (Stream<Path> s = Files.walk(DOCUMENTS)) {
            s.filter(p -> p.getFileName().toString().equals("ir.json"))
                    .forEach(p -> found.add(DOCUMENTS.relativize(p.getParent()).toString()));
        }
        found.sort(null);
        assertEquals(DOCUMENT.keySet().stream().sorted().toList(), found, "every document of ref, each once");
        int played = 0;
        for (Map.Entry<String, List<Object>> d : DOCUMENT.entrySet()) {
            Path dir = DOCUMENTS.resolve(d.getKey()), ir = dir.resolve("ir.json");
            List<Path> cases;
            try (Stream<Path> s = Files.list(dir)) {
                cases = s.filter(p -> p.getFileName().toString().startsWith("case")).sorted().toList();
            }
            assertFalse(cases.isEmpty(), d::getKey);
            List<JsonNode> contents = new ArrayList<>();
            for (Path c : cases) contents.add(JSON.readTree(Files.readString(c)));
            for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) {
                String what = branch + " " + d.getKey();
                List<Object> refusal = d.getValue();
                if (refusal == null) {
                    List<String> verdicts = CaseRunner.playCases(ir, contents, branch, false);
                    for (int i = 0; i < cases.size(); i++) {
                        assertNull(verdicts.get(i), what + "/" + cases.get(i).getFileName());
                        played++;
                    }
                } else if (refusal.get(0) == DroolsCode.EXPR_MATCH) {
                    IrLoader.Refusal e = assertThrows(IrLoader.Refusal.class,
                            () -> CaseRunner.playCases(ir, contents, branch, false), what);
                    assertEquals(DroolsCode.EXPR_MATCH, e.code(), what);
                } else {
                    DroolsCode code = (DroolsCode) refusal.get(0);
                    CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                            () -> CaseRunner.playCases(ir, contents, branch, false), what);
                    assertEquals(code, e.code(), what);
                    if (refusal.size() > 1)
                        assertEquals(code.reason("form", refusal.get(1)).text(), e.getMessage(), what);
                }
            }
        }
        // the cases of the three documents that play, on two branches, counted 2026-11-23
        assertEquals(2 * 6, played);
    }

    /** The list of {@code SumTypeForms} is the table "Forms not carried" of RUNTIME-SEMANTICS ch. 9.4, read from the
     *  spec, in its order. */
    @Test void the_list_is_the_table_of_the_spec() throws Exception {
        String spec = Files.readString(Path.of("..", "..", "spec", "RUNTIME-SEMANTICS.md"));
        int start = spec.indexOf("**Forms not carried.**"), end = spec.indexOf("The suite holds this table", start);
        List<String> table = new ArrayList<>();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?m)^\\| `([a-z-]+)` \\|").matcher(spec.substring(start, end));
        while (m.find()) table.add(m.group(1));
        assertEquals(table, SumTypeForms.SLUGS);
    }
}
