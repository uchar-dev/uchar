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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code TmsTranspiler.drlPieces} on the shapes of card drl-rule-functions, acceptance (b), all three in the golden
 * {@code jutterswet-uitsluiting-rgs}: a rule without functions, a rule with a composite condition (a function that calls
 * another function of the same rule), and the fall-back of a default with an exception group, of the same shape as the
 * fall-back of the card's v2 example (a test whose function calls the function of its composite, and the function of
 * the value). Per rule the table of its piece: per function its name and its comment as the text has them at the places
 * of the record, and which functions the rule and each function call, read off the text of the piece by this witness
 * (a name followed by an opening parenthesis), an oracle the rendering does not consult. Beside it, on a document
 * written here, that a place counts UTF-16 code units. {@link DrlPiecesCorpusWitnessTest} holds the same over the corpus.
 */
class DrlPiecesWitnessTest {

    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");
    private static final Pattern CALL = Pattern.compile("(?<![A-Za-z0-9_$.])(r[0-9]+_[a-z]+[0-9]+)\\(");

    private static DroolsBackend.Build build(String golden) throws Exception {
        return DroolsBackend.build(GOLDEN.resolve(golden).resolve("ir.json"), DroolsBackend.Branch.DRL_TEXT);
    }

    private static DrlPiece piece(DroolsBackend.Build b, String rule) {
        for (DrlPiece p : b.execModelTranspiler().drlPieces()) if (p.rule().equals(rule)) return p;
        throw new AssertionError("geen stuk van de regel '" + rule + "'");
    }

    /** The table of piece {@code p} of text {@code drl}: per function {@code <name> | <comment line or -> | calls
     *  [...]}, then {@code regel | calls [...]}. */
    private static List<String> table(String drl, DrlPiece p) {
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

    @Test void a_composite_condition_is_a_function_its_test_calls() throws Exception {
        DroolsBackend.Build b = build("jutterswet-uitsluiting-rgs");
        String rule = "defaults K:Jutter.uitgesloten: default 0.0.0";
        DrlPiece p = piece(b, rule);
        String sentence = "// hij vergunning geweigerd is en hij weigering herroepen is en hij herroeping vervallen is";
        assertEquals(List.of(
                "r1_toets0 | " + sentence + " | roept [r1_samengesteld1]",
                "r1_samengesteld1 | " + sentence + " | roept []",
                "r1_afloop2 | - | roept []",
                "regel | roept [r1_toets0, r1_afloop2]"), table(b.drl(), p));
        // the piece begins with the comment of its first function and holds its rule up to its end
        String text = b.drl().substring(p.start(), p.end());
        assertTrue(text.startsWith(sentence + "\nfunction boolean r1_toets0("), text);
        assertTrue(b.drl().startsWith("rule \"" + rule + "\"\n", p.ruleStart()));
        assertTrue(text.endsWith("\nend\n\n"), text);
        assertEquals(RuleOrigin.Role.OWN, b.execModelTranspiler().originOf(rule).role());
    }

    @Test void the_fall_back_of_a_default_carries_the_functions_of_its_test_and_its_value() throws Exception {
        DroolsBackend.Build b = build("jutterswet-uitsluiting-rgs");
        String rule = "defaults K:Jutter.uitgesloten: terugval 0";
        DrlPiece p = piece(b, rule);
        String sentence = "// hij vergunning geweigerd is en hij weigering herroepen is";
        assertEquals(List.of(
                "r3_toets0 | " + sentence + " | roept [r3_samengesteld1]",
                "r3_samengesteld1 | " + sentence + " | roept []",
                "r3_afloop2 | - | roept []",
                "regel | roept [r3_toets0, r3_afloop2]"), table(b.drl(), p));
        assertEquals("function Object r3_afloop2() {\n    return Boolean.FALSE;\n}\n\n",
                b.drl().substring(p.functions().get(2).start(), p.functions().get(2).end()));
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
}
