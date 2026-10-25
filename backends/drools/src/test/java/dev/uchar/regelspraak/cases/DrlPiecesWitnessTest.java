package dev.uchar.regelspraak.cases;

import dev.uchar.regelspraak.transpiler.tms.TmsTranspiler.DrlPiece;
import dev.uchar.regelspraak.transpiler.tms.TmsTranspiler.RuleOrigin;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static dev.uchar.regelspraak.cases.TmsDocuments.characteristicRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.check;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code TmsTranspiler.drlPieces} on the shapes of card drl-rule-functions, acceptance (b), all three in the golden
 * {@code jutterswet-uitsluiting-rgs}: a rule without functions, a rule with a composite condition, and the fall-back of
 * a default with an exception group, of the same shape as the fall-back of the card's v2 example (a test over a
 * composite, and a value; here a constant, which the consequence carries without a function since card
 * drl-no-trivial-value-functions). Until card drl-no-passthrough-functions the test of a composite
 * condition called the composite's function, the function of a rule that calls another function of the same rule
 * which that acceptance asked for; since that card the test carries the composite's statements, and
 * {@code DrlConditionFunctionsWitnessTest} shows such a call (a test that calls the function of a fold, and one of a
 * fault that calls the function of its composite). Per rule the table of its piece: per function its name and its
 * comment as the text has them at the places of the record, and which functions the rule and each function call, read
 * off the text of the piece by this witness (a name followed by an opening parenthesis), an oracle the rendering does
 * not consult. Beside it, on a document written here, that a place counts UTF-16 code units, and that a piece whose
 * places do not add up is a bookkeeping fault of the rendering. {@link DrlPiecesCorpusWitnessTest} holds the same over
 * the corpus.
 */
class DrlPiecesWitnessTest {

    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");
    /** A call of a function: its kind with its number, {@code _} and its rule part, with the rule's number. */
    private static final Pattern CALL = Pattern.compile(
            "(?<![\\p{L}\\p{N}_$.])([a-z]+[0-9]*_[\\p{L}\\p{N}]+(?:_[0-9]+)?)\\(");

    private static DroolsBackend.Build build(String golden) throws Exception {
        return DroolsBackend.build(GOLDEN.resolve(golden).resolve("ir.json"), DroolsBackend.Branch.DRL_TEXT);
    }

    static DrlPiece piece(DroolsBackend.Build b, String rule) {
        for (DrlPiece p : b.execModelTranspiler().drlPieces()) if (p.rule().equals(rule)) return p;
        throw new AssertionError("geen stuk van de regel '" + rule + "'");
    }

    /** The table of piece {@code p} of text {@code drl}: per function {@code <name> | <comment line or -> | calls
     *  [...]}, then {@code regel | calls [...]}. */
    static List<String> table(String drl, DrlPiece p) {
        List<String> out = new ArrayList<>();
        for (DrlPiece.DrlFunction f : p.functions()) {
            String comment = drl.substring(f.start(), f.declaration());
            out.add(f.name() + " | " + (comment.isEmpty() ? "-" : comment.strip()) + " | roept "
                    + calls(drl.substring(f.declaration(), f.end()), f.name()));
        }
        out.add("regel | roept " + calls(drl.substring(p.ruleStart(), p.end()), null));
        return out;
    }

    private static List<String> calls(String code, String self) {
        List<String> out = new ArrayList<>();
        for (Matcher m = CALL.matcher(code); m.find(); )
            if (!m.group(1).equals(self) && !out.contains(m.group(1))) out.add(m.group(1));
        return out;
    }

    @Test void a_rule_without_functions_is_its_rule_alone() throws Exception {
        DroolsBackend.Build b = build("jutterswet-uitsluiting-rgs");
        DrlPiece p = piece(b, "kenmerkstand Jutter.uitgesloten: waar uit de invoer");
        assertEquals(List.of(), p.functions());
        assertEquals(p.start(), p.ruleStart());
        assertEquals("""
                rule "kenmerkstand Jutter.uitgesloten: waar uit de invoer"
                    dialect "mvel"
                when
                    $onderwerp : dev.uchar.regelspraak.facts.Jutter( ComputeHelpers.holds(uitgesloten) == true )
                then
                    insertLogical(new CharacteristicState($onderwerp, ElementKey.of("jutter", "uitgesloten", true), true));
                end

                """, b.drl().substring(p.start(), p.end()));
    }

    @Test void a_composite_condition_is_the_body_of_its_test() throws Exception {
        DroolsBackend.Build b = build("jutterswet-uitsluiting-rgs");
        String rule = "defaults K:Jutter.uitgesloten: default 0.0.0";
        DrlPiece p = piece(b, rule);
        String sentence = "// hij vergunning geweigerd is en hij weigering herroepen is en hij herroeping vervallen is";
        String test = "toets_defaultsKJutterUitgeslotenDefault000";
        assertEquals(List.of(
                test + " | " + sentence + " | roept []",
                "regel | roept [" + test + "]"), table(b.drl(), p));
        // the piece begins with the comment of its first function and holds its rule up to its end
        String text = b.drl().substring(p.start(), p.end());
        assertTrue(text.startsWith(sentence + "\nfunction boolean " + test + "("), text);
        assertTrue(b.drl().startsWith("rule \"" + rule + "\"\n", p.ruleStart()));
        assertTrue(text.endsWith("\nend\n\n"), text);
        assertEquals(RuleOrigin.Role.OWN, b.execModelTranspiler().originOf(rule).role());
    }

    @Test void the_fall_back_of_a_default_carries_the_function_of_its_test_and_its_value_in_its_consequence()
            throws Exception {
        DroolsBackend.Build b = build("jutterswet-uitsluiting-rgs");
        String rule = "defaults K:Jutter.uitgesloten: terugval 0";
        DrlPiece p = piece(b, rule);
        String sentence = "// hij vergunning geweigerd is en hij weigering herroepen is";
        String test = "toets_defaultsKJutterUitgeslotenTerugval0";
        assertEquals(List.of(
                test + " | " + sentence + " | roept []",
                "regel | roept [" + test + "]"), table(b.drl(), p));
        // the value, a constant, computes nothing: the consequence carries it
        assertTrue(b.drl().substring(p.ruleStart(), p.end()).endsWith("then\n"
                + "    Object v = Boolean.FALSE;\n"
                + "    if (v != null) {\n"
                + "        insertLogical(new Outcome($onderwerp, \"defaults K:Jutter.uitgesloten\", 0, 0,"
                + " dev.uchar.regelspraak.transpiler.tms.Outcome.Kind.DECIDED, v, null));\n"
                + "    }\n"
                + "end\n\n"), b.drl().substring(p.ruleStart(), p.end()));
        RuleOrigin o = b.execModelTranspiler().originOf(rule);
        assertEquals(RuleOrigin.Kind.DEFAULT, o.kind());
        assertEquals(RuleOrigin.Role.FALLBACK, o.role());
        // the pieces of the stack stand one after the other: the agreement of the exception group before, the next
        // default after
        List<DrlPiece> all = b.execModelTranspiler().drlPieces();
        int i = all.indexOf(p);
        assertEquals("defaults K:Jutter.uitgesloten: eens 0", all.get(i - 1).rule());
        assertEquals(all.get(i - 1).end(), p.start());
        assertEquals("defaults K:Jutter.uitgesloten: default 1", all.get(i + 1).rule());
        assertEquals(p.end(), all.get(i + 1).start());
    }

    @Test void a_place_counts_utf16_code_units() throws Exception {
        // the first rule's name carries a character outside the Basic Multilingual Plane, two code units and one code
        // point, so every place after it is one more in code units than in code points: the count of a Java string and
        // of a JavaScript one
        String astral = "𝔘";
        String model = TmsDocuments.model(List.of("uitgesloten", "penninggerechtigd", "gemeld"));
        Path document = TmsDocuments.document(model, List.of(
                characteristicRule("penninggerechtigd " + astral, "penninggerechtigd", check("uitgesloten", true)),
                characteristicRule("gemeld", "gemeld", check("uitgesloten", false))));
        DroolsBackend.Build b = DroolsBackend.build(document, DroolsBackend.Branch.DRL_TEXT);
        DrlPiece first = piece(b, "penninggerechtigd " + astral), second = piece(b, "gemeld");
        assertTrue(first.end() <= second.start());
        assertTrue(b.drl().startsWith("rule \"gemeld\"\n", second.ruleStart()));
        assertEquals(second.ruleStart() - 1, b.drl().codePointCount(0, second.ruleStart()));
        assertEquals("// hij uitgesloten is",
                b.drl().substring(second.functions().get(0).start(), second.functions().get(0).declaration()).strip());
    }

    @Test void the_pieces_of_an_exec_model_build_are_those_of_the_text_branch_of_the_same_document() throws Exception {
        // drlPieces() on a build that carries no text renders it on request: the text, and so the places, are those a
        // build of the branch DRL_TEXT carries, with and without readings
        Path ir = GOLDEN.resolve("jutterswet-uitsluiting-rgs").resolve("ir.json");
        for (boolean readings : new boolean[] {false, true}) {
            DroolsBackend.Build model = DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL, readings);
            DroolsBackend.Build text = DroolsBackend.build(ir, DroolsBackend.Branch.DRL_TEXT, readings);
            assertNull(model.drl());
            assertEquals(text.drl(), model.execModelTranspiler().drl(), "readings " + readings);
            assertEquals(text.execModelTranspiler().drlPieces(), model.execModelTranspiler().drlPieces(),
                    "readings " + readings);
        }
    }

    @Test void places_that_do_not_add_up_are_a_bookkeeping_fault_of_the_rendering() {
        // an IllegalStateException, as the other bookkeeping faults of DrlRendering: the toolchain turns that one into
        // its drlError, where any other exception would fail the analysis as a whole. The message holds the rule and
        // the places, so that the fault is found without a debugger.
        DrlPiece.DrlFunction f = new DrlPiece.DrlFunction("r1_toets0", 10, 20, 40);
        // the first function does not begin at the start of the piece
        IllegalStateException gap = assertThrows(IllegalStateException.class,
                () -> new DrlPiece("regel gat", 12, 40, 60, List.of(f)));
        assertTrue(gap.getMessage().contains("'regel gat'") && gap.getMessage().contains("r1_toets0")
                && gap.getMessage().contains("10") && gap.getMessage().contains("12"), gap.getMessage());
        // the functions do not end where the rule begins
        IllegalStateException rule = assertThrows(IllegalStateException.class,
                () -> new DrlPiece("regel naad", 10, 45, 60, List.of(f)));
        assertTrue(rule.getMessage().contains("'regel naad'") && rule.getMessage().contains("45")
                && rule.getMessage().contains("40"), rule.getMessage());
        // the rule does not end after it begins
        IllegalStateException end = assertThrows(IllegalStateException.class,
                () -> new DrlPiece("regel einde", 60, 60, 60, List.of()));
        assertTrue(end.getMessage().contains("'regel einde'") && end.getMessage().contains("60"), end.getMessage());
        // a function whose declaration lies before its start, and one that ends before its declaration
        IllegalStateException declaration = assertThrows(IllegalStateException.class,
                () -> new DrlPiece.DrlFunction("r2_afloop1", 20, 10, 40));
        assertTrue(declaration.getMessage().contains("r2_afloop1") && declaration.getMessage().contains("20")
                && declaration.getMessage().contains("10") && declaration.getMessage().contains("40"),
                declaration.getMessage());
        IllegalStateException functionEnd = assertThrows(IllegalStateException.class,
                () -> new DrlPiece.DrlFunction("r2_afloop1", 10, 20, 20));
        assertTrue(functionEnd.getMessage().contains("r2_afloop1"), functionEnd.getMessage());
    }
}
