package dev.uchar.regelspraak.cases;

import dev.uchar.regelspraak.transpiler.core.Namen;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A member whose declared name gives no Java identifier by itself or no property a DRL pattern reads, on both
 * branches. The IR admits every name WF-057 leaves (IR-SPEC ch. 2.1): a digit in front ("3e lid"), a number sign that
 * no Java identifier holds ("m²", "½ deel"), a word of Java ("final") and a word of DRL ("in", "nil"). The reference
 * interpreter and the ASP backend play each of them. Here each name stands in turn for the attribute the rule reads
 * and for the one it writes, in {@code anchor/jutterswet-waardestelling} with its own cases: both branches play the
 * document as the reference interpreter does, where the bean of the member compiled for no name with a digit in
 * front, a number sign or a word of Java, and the DRL-text branch refused the words of DRL that a pattern does not
 * read as the member. A member whose getter is a method of {@code java.lang.Object} cannot be carried by the bean
 * model, and both branches refuse it by name.
 */
class MemberNameWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path WAARDESTELLING = Path.of("..", "..", "corpus", "golden", "anchor",
            "jutterswet-waardestelling");

    private static final List<String> CASES = List.of("case1-berekend-over-input.json", "case2-flank-input-blijft.json",
            "case3-leeg-valt-door.json", "case4-berekend.json");

    @Test void every_name_the_ir_admits_plays_on_both_branches(@TempDir Path dir) throws Exception {
        List<String> faults = new ArrayList<>();
        for (String name : List.of("3e lid", "3 M", "m²", "½ deel", "final", "true", "in", "is", "with", "drools", "nil"))
            for (String member : List.of("taxatiewaarde", "geschatte waarde")) {
                Path ir = document(dir, member, name);
                for (String c : CASES)
                    try {
                        BothBranches.green(ir, caseWith(c, member, name), name + " for " + member + ", " + c);
                    } catch (Throwable e) {
                        faults.add(name + " for " + member + ", " + c + ": " + e.getClass().getSimpleName() + ": "
                                + firstLine(e.getMessage()));
                        break;
                    }
            }
        assertEquals(List.of(), faults, "names the reference interpreter plays and a branch does not");
    }

    @Test void a_getter_of_object_is_refused_by_name(@TempDir Path dir) throws Exception {
        Path ir = document(dir, "geschatte waarde", "class");
        BothBranches.refused(ir, caseWith(CASES.get(0), "geschatte waarde", "class"), "getClass",
                "the attribute 'class' of Vondst, whose getter java.lang.Object declares");
    }

    @Test void every_property_is_a_java_identifier_and_its_pascal_form_names_the_same_getter() {
        for (String name : List.of("3e lid", "3 M", "m²", "m³", "½ deel", "final", "in", "İş", "日 X", "Ⅻ eeuw")) {
            String property = Namen.property(name);
            assertEquals(true, javax.lang.model.SourceVersion.isIdentifier(property), name + " -> " + property);
            // two known exceptions of RegelSpraakRuntime.pascal: "İş", whose first letter lowers to another letter, and
            // "日 X", whose property reads as a shouted word
            if (!name.equals("İş") && !name.equals("日 X"))
                assertEquals(Namen.ident(name, true), Namen.ident(property, true), name + ": the Pascal form of the"
                        + " property is that of the name, which the runtime's reflective bridge reads");
        }
        assertEquals(List.of("x3eLid", "m2", "m3", "x12Deel"), List.of(Namen.property("3e lid"), Namen.property("m²"),
                Namen.property("m³"), Namen.property("½ deel")));
    }

    // ---------------- helpers ----------------

    /** {@code anchor/jutterswet-waardestelling} with {@code member} renamed to {@code name}, written under {@code dir}. */
    private static Path document(Path dir, String member, String name) throws Exception {
        Path ir = Files.createTempFile(dir, "ir", ".json");
        Files.writeString(ir, Files.readString(WAARDESTELLING.resolve("ir.json"), StandardCharsets.UTF_8)
                .replace(member, name), StandardCharsets.UTF_8);
        return ir;
    }

    private static JsonNode caseWith(String file, String member, String name) throws Exception {
        return JSON.readTree(Files.readString(WAARDESTELLING.resolve(file), StandardCharsets.UTF_8).replace(member, name));
    }

    private static String firstLine(String s) {
        if (s == null) return "null";
        int nl = s.indexOf('\n');
        return nl < 0 ? s : s.substring(0, nl);
    }
}
