package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import dev.uchar.regelspraak.transpiler.tms.TmsTranspiler.DrlPiece;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static dev.uchar.regelspraak.cases.DrlPiecesWitnessTest.piece;
import static dev.uchar.regelspraak.cases.DrlPiecesWitnessTest.table;
import static dev.uchar.regelspraak.cases.TmsDocuments.characteristicRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.check;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The names of the functions of the DRL text (card drl-function-names-from-rule, acceptance (b) and (c)): a function is
 * named {@code <kind>_<rule>}, the kind the word of what it computes and the rule part {@code Namen.ident} of its
 * rule's name; a kind of which the rule has more than one function is numbered right after the kind, from 1 in the
 * order of the plan, and a function that is the only one of its kind in its rule has no number. The cases of the card
 * that no golden shows stand in a document written here, whose cases the exec-model branch and the DRL-text branch
 * both play, so that Drools 8.44 takes every name: a rule name that begins with a digit, one whose ident is empty, two
 * whose idents are the same, a name with {@code ë}, the documents of the members {@code İş} and {@code 日 X} of card
 * drl-teksttak-op-truth-maintenance (step 1e and 2a) under a rule name with those letters, and code points Drools does
 * not read in a name. Per rule the table of its piece ({@link DrlPiecesWitnessTest#table}).
 *
 * <p><b>Red</b> on {@code f867513d}, before the card: every function was named {@code r<n>_<kind><place>}, after the
 * rule's place in the plan, and every table here names it so.
 */
class DrlFunctionNamesWitnessTest {

    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");
    private static final Path PROPERTY = Path.of("src", "test", "resources", "property");

    @Test void a_kind_of_which_a_rule_has_more_than_one_function_is_numbered() throws Exception {
        // the golden reads the taxatiebedrag along a path in havengeld, and it and the herstelbedrag in kadegeld
        DroolsBackend.Build b = DroolsBackend.build(GOLDEN.resolve("jutterswet-havengeld").resolve("ir.json"),
                DroolsBackend.Branch.DRL_TEXT);
        String havengeld = "// (het percentage havengeld van het taxatiebedrag van de taxatie van de Vondst) naar beneden"
                + " afgerond op 2 decimalen";
        assertEquals(List.of(
                "pad_havengeld | - | roept []",
                "waarde_havengeld | " + havengeld + " | roept []",
                "regel | roept [pad_havengeld, waarde_havengeld]"), table(b.drl(), piece(b, "havengeld")));
        String kadegeld = "// (het percentage kadegeld van (het taxatiebedrag van de taxatie van de Vondst plus het"
                + " herstelbedrag van de taxatie van de Vondst)) naar beneden afgerond op 2 decimalen";
        assertEquals(List.of(
                "pad1_kadegeld | - | roept []",
                "pad2_kadegeld | - | roept []",
                "waarde_kadegeld | " + kadegeld + " | roept []",
                "regel | roept [pad1_kadegeld, pad2_kadegeld, waarde_kadegeld]"), table(b.drl(), piece(b, "kadegeld")));
    }

    /** The rules of {@link #ruleParts}: per rule its name and the names of its functions. */
    private static final List<String[]> RULES = List.of(
            // an ident that begins with a digit: the kind comes first, so the name begins with a letter
            new String[] {"2 jarige termijn", "toets_2JarigeTermijn"},
            // no letter and no digit: Namen.ident gives x
            new String[] {"— § —", "toets_x"},
            // two rules with functions of one ident, numbered after it in the order of the plan; a rule without
            // functions of that ident between them has no name and no number
            new String[] {"a-b", "toets_aB_1"},
            new String[] {"a/b", null},
            new String[] {"a b", "toets_aB_2"},
            new String[] {"beëindiging", "toets_beëindiging"},
            // a letter outside the Basic Multilingual Plane, which the DRL lexer does not read in a name
            new String[] {"penninggerechtigd 𝔘", "toets_penninggerechtigd"},
            // a digit that is no Java identifier part (U+00B2) and a letter of Unicode 4.1 (U+0237), which the DRL
            // lexer, whose table of names is that of Java 5, does not read
            new String[] {"artikel 2² en ȷ", "toets_artikel2En"});

    /** The document of {@link #RULES}: per rule "Een Jutter is doelN indien hij lid is", the rule without functions
     *  without a condition. */
    private static Path ruleParts() throws Exception {
        List<String> characteristics = new ArrayList<>(List.of("lid")), rules = new ArrayList<>();
        for (int i = 0; i < RULES.size(); i++) {
            characteristics.add("doel" + i);
            rules.add(characteristicRule(RULES.get(i)[0], "doel" + i, RULES.get(i)[1] == null ? "null"
                    : check("lid", false)));
        }
        return TmsDocuments.document(TmsDocuments.model(characteristics), rules);
    }

    @Test void the_rule_part_is_the_ident_of_the_rule_with_what_drools_reads_in_a_name() throws Exception {
        Path document = ruleParts();
        DroolsBackend.Build b = DroolsBackend.build(document, DroolsBackend.Branch.DRL_TEXT);
        for (String[] r : RULES) {
            DrlPiece p = piece(b, r[0]);
            if (r[1] == null) {
                assertEquals(List.of("regel | roept []"), table(b.drl(), p), r[0]);
                continue;
            }
            assertEquals(List.of(r[1] + " | // hij lid is | roept []", "regel | roept [" + r[1] + "]"),
                    table(b.drl(), p), r[0]);
        }
        // Drools takes every name: the text compiles, and means what the plan means on both branches
        List<String> expectations = new ArrayList<>();
        for (int i = 0; i < RULES.size(); i++) {
            expectations.add(TmsDocuments.expected("met", "doel" + i, true));
            expectations.add(TmsDocuments.expected("zonder", "doel" + i, RULES.get(i)[1] == null));
        }
        play(document, TmsDocuments.caseDocument(List.of(TmsDocuments.jutter("met", List.of("lid")),
                TmsDocuments.jutter("zonder", List.of())), expectations));
    }

    @Test void a_rule_name_with_letters_outside_ascii_names_its_functions_with_them() throws Exception {
        // the document of the member İş under a rule name that begins with it: Namen.ident writes İ in lower case as i
        Path turkish = renamed(PROPERTY.resolve("turkish-member"), "uit van een jutter", "İş van een jutter");
        DroolsBackend.Build b = DroolsBackend.build(turkish, DroolsBackend.Branch.DRL_TEXT);
        assertEquals(List.of(
                "waarde_işVanEenJutter | // het İş van de Jutter plus 1,00 | roept []",
                "regel | roept [waarde_işVanEenJutter]"), table(b.drl(), piece(b, "İş van een jutter")));
        play(turkish, TmsDocuments.JSON.readTree(PROPERTY.resolve("turkish-member").resolve("case.json").toFile()));
        play(turkish, TmsDocuments.JSON.readTree(PROPERTY.resolve("turkish-member").resolve("case-input.json")
                .toFile()));
        // the document of the member 日 X under a rule name of it, and İç in the middle of a name, where its case stays
        Path awkward = renamed(PROPERTY.resolve("awkward-members"), "İç van een jutter", "日 X naar İç");
        b = DroolsBackend.build(awkward, DroolsBackend.Branch.DRL_TEXT);
        assertEquals(List.of(
                "waarde_日XNaarİç | // het 日 X van de Jutter plus 1,00 | roept []",
                "regel | roept [waarde_日XNaarİç]"), table(b.drl(), piece(b, "日 X naar İç")));
        play(awkward, TmsDocuments.JSON.readTree(PROPERTY.resolve("awkward-members").resolve("case.json").toFile()));
    }

    @Test void one_plan_gives_one_text() throws Exception {
        // two builds of one document, two analyses and two renderings of their plans, on both branches: the same
        // text with the same pieces, numbers and rule parts included
        Path document = ruleParts();
        DroolsBackend.Build first = DroolsBackend.build(document, DroolsBackend.Branch.DRL_TEXT);
        DroolsBackend.Build second = DroolsBackend.build(document, DroolsBackend.Branch.DRL_TEXT);
        DroolsBackend.Build model = DroolsBackend.build(document, DroolsBackend.Branch.EXEC_MODEL);
        assertEquals(first.drl(), second.drl());
        assertEquals(first.drl(), model.execModelTranspiler().drl());
        assertEquals(first.execModelTranspiler().drlPieces(), second.execModelTranspiler().drlPieces());
        assertEquals(first.execModelTranspiler().drlPieces(), model.execModelTranspiler().drlPieces());
    }

    /** The document {@code ir.json} of {@code map} with its one rule {@code from} named {@code to}, written to a
     *  temporary file. */
    private static Path renamed(Path map, String from, String to) throws Exception {
        String ir = Files.readString(map.resolve("ir.json"));
        String name = "\"name\": \"" + from + "\"";
        assertEquals(1, ir.split(name, -1).length - 1, () -> "de regel '" + from + "' in " + map);
        Path tmp = Files.createTempFile("functienamen", ".json");
        tmp.toFile().deleteOnExit();
        return Files.writeString(tmp, ir.replace(name, "\"name\": \"" + to + "\""));
    }

    private static void play(Path document, JsonNode c) throws Exception {
        assertNull(CaseRunner.playCase(document, c, DroolsBackend.Branch.EXEC_MODEL), "exec-model");
        assertNull(CaseRunner.playCase(document, c, DroolsBackend.Branch.DRL_TEXT), "drl-tekst");
    }
}
