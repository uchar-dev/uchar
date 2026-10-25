package dev.uchar.regelspraak.cases;

import dev.uchar.regelspraak.transpiler.tms.TmsTranspiler.DrlPiece;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static dev.uchar.regelspraak.cases.DrlPiecesWitnessTest.piece;
import static dev.uchar.regelspraak.cases.DrlPiecesWitnessTest.table;
import static dev.uchar.regelspraak.cases.TmsDocuments.characteristicRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.check;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The functions of a condition in the DRL text (card drl-no-passthrough-functions, acceptance (b)): a function stands
 * only where it tests or computes something. A test over a composite is one function in the roles that ask the
 * composite's verdict ({@code CONDITION}, {@code VORM}, {@code WITHOUT_FAULT}); a test that asks whether the composite
 * faults or deposits ({@code FAULT}, {@code DEPOSIT}) keeps the composite's function, which its {@code try} calls. A
 * default without a condition, whose composite has no member and the verdict true, has no test. A composite over one
 * member is that member where its quantifier gives the member's verdict, also inside another composite, and its sentence
 * stays as a comment line where it differs from that of the function it goes into. On goldens where the corpus shows
 * the form, else on a document written here, whose cases the exec-model branch and the DRL-text branch both play: the
 * text means what the plan means. Per rule the table of its piece ({@link DrlPiecesWitnessTest#table}); where the shape
 * of a body is the point, the function itself.
 *
 * <p><b>Red</b> on {@code ce2566f3}, before the card: every table here named a function {@code samengesteld} and a test
 * that only called it, and the default without a condition had a test of its own and an {@code eval}; the tests of a
 * fault or a deposit over a composite of more than one member held there already, since the card keeps their form.
 */
class DrlConditionFunctionsWitnessTest {

    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");

    private static DroolsBackend.Build build(String golden) throws Exception {
        return DroolsBackend.build(GOLDEN.resolve(golden).resolve("ir.json"), DroolsBackend.Branch.DRL_TEXT);
    }

    /** Function {@code name} of piece {@code p}, from its comment to the empty line after it. */
    private static String function(DroolsBackend.Build b, DrlPiece p, String name) {
        for (DrlPiece.DrlFunction f : p.functions()) if (f.name().equals(name)) return b.drl().substring(f.start(), f.end());
        throw new AssertionError("geen functie " + name + " in het stuk van '" + p.rule() + "': " + table(b.drl(), p));
    }

    private static String rule(DroolsBackend.Build b, DrlPiece p) {
        return b.drl().substring(p.ruleStart(), p.end());
    }

    @Test void a_test_over_a_composite_is_one_function_where_its_role_asks_the_verdict() throws Exception {
        // CONDITION: the composite of a default of the dialect
        DroolsBackend.Build b = build("jutterswet-uitsluiting-rgs");
        DrlPiece p = piece(b, "defaults K:Jutter.uitgesloten: default 0.0.0");
        String sentence = "// hij vergunning geweigerd is en hij weigering herroepen is en hij herroeping vervallen is";
        assertEquals(List.of(
                "r1_toets0 | " + sentence + " | roept []",
                "r1_afloop1 | - | roept []",
                "regel | roept [r1_toets0, r1_afloop1]"), table(b.drl(), p));
        assertEquals(sentence + "\n"
                + "function boolean r1_toets0(Object $vergunningGeweigerd, Object $weigeringHerroepen, Object $herroepingVervallen) {\n"
                + "    int raak = 0;\n"
                + "    Boolean vast;\n"
                + "    vast = ComputeHelpers.established(dev.uchar.regelspraak.ir.Condition.Quantifier.ALLE, 0, raak, 0, 3);\n"
                + "    if (vast != null) {\n"
                + "        return vast;\n"
                + "    }\n"
                + "    if (ComputeHelpers.characteristic($vergunningGeweigerd, Boolean.TRUE)) {\n"
                + "        raak++;\n"
                + "    }\n"
                + "    vast = ComputeHelpers.established(dev.uchar.regelspraak.ir.Condition.Quantifier.ALLE, 0, raak, 1, 2);\n"
                + "    if (vast != null) {\n"
                + "        return vast;\n"
                + "    }\n"
                + "    if (ComputeHelpers.characteristic($weigeringHerroepen, Boolean.TRUE)) {\n"
                + "        raak++;\n"
                + "    }\n"
                + "    vast = ComputeHelpers.established(dev.uchar.regelspraak.ir.Condition.Quantifier.ALLE, 0, raak, 2, 1);\n"
                + "    if (vast != null) {\n"
                + "        return vast;\n"
                + "    }\n"
                + "    if (ComputeHelpers.characteristic($herroepingVervallen, Boolean.TRUE)) {\n"
                + "        raak++;\n"
                + "    }\n"
                + "    return ComputeHelpers.endVerdict(dev.uchar.regelspraak.ir.Condition.Quantifier.ALLE, 0, raak, 3);\n"
                + "}\n\n", function(b, p, "r1_toets0"));

        // VORM: the participation of a key of a uniqueness, a composite of the form without a sentence
        b = build("anchor/jutterswet-kustregister");
        p = piece(b, "eenmalig kaveletiket: sleutel van Kavel 1");
        assertEquals(List.of(
                "r1_toets0 | - | roept []",
                "r1_componenten1 | - | roept []",
                "regel | roept [r1_toets0, r1_componenten1]"), table(b.drl(), p));
        assertTrue(function(b, p, "r1_toets0").contains("ComputeHelpers.established("), function(b, p, "r1_toets0"));

        // WITHOUT_FAULT: the body of the composite within the try
        b = build("fold-filter-divides-by-zero");
        p = piece(b, "drukte");
        String drukte = "// hij aan alle volgende voorwaarden voldoet: - hij is actief - het aantal vondsten van de Jutter die"
                + " aan de volgende voorwaarde voldoen: zijn gewicht gedeeld door zijn deelnemers is groter dan 2 is groter"
                + " dan 0";
        String fold = "// het aantal vondsten van de Jutter die aan de volgende voorwaarde voldoen: zijn gewicht gedeeld door"
                + " zijn deelnemers is groter dan 2";
        assertEquals(List.of(
                "r2_toets0 | " + drukte + " | roept [r2_vouwlezing1]",
                "r2_vouwlezing1 | " + fold + " | roept []",
                "regel | roept [r2_toets0]"), table(b.drl(), p));
        String parameters = "$actief, $aantal_vouw_46, $eerste_filterfout_vouw_51, $filterdeponeringen_vouw_55";
        assertEquals(drukte + "\n"
                + "function boolean r2_toets0(Object $actief, Object $aantal_vouw_46, Object $eerste_filterfout_vouw_51,"
                + " Object $filterdeponeringen_vouw_55) {\n"
                + "    try {\n"
                + "        int raak = 0;\n"
                + "        Boolean vast;\n"
                + "        vast = ComputeHelpers.established(dev.uchar.regelspraak.ir.Condition.Quantifier.ALLE, 0, raak, 0, 2);\n"
                + "        if (vast != null) {\n"
                + "            return vast;\n"
                + "        }\n"
                + "        if (ComputeHelpers.characteristic($actief, Boolean.TRUE)) {\n"
                + "            raak++;\n"
                + "        }\n"
                + "        vast = ComputeHelpers.established(dev.uchar.regelspraak.ir.Condition.Quantifier.ALLE, 0, raak, 1, 1);\n"
                + "        if (vast != null) {\n"
                + "            return vast;\n"
                + "        }\n"
                + "        if (RegelSpraakRuntime.rsAggVergelijk(r2_vouwlezing1(" + parameters + "), new java.math.BigDecimal(\"0\"),"
                + " \">\")) {\n"
                + "            raak++;\n"
                + "        }\n"
                + "        return ComputeHelpers.endVerdict(dev.uchar.regelspraak.ir.Condition.Quantifier.ALLE, 0, raak, 2);\n"
                + "    } catch (RuntimeException e) {\n"
                + "        ComputeHelpers.caught(e);\n"
                + "        return false;\n"
                + "    }\n"
                + "}\n\n", function(b, p, "r2_toets0"));
    }

    @Test void a_test_that_asks_for_a_fault_or_a_deposit_keeps_the_function_of_its_composite() throws Exception {
        // the test holds where the composite faults or deposits, whatever its verdict: the try calls the composite's
        // function, as the function of the fault and of the deposit of the consequence do
        DroolsBackend.Build b = build("fold-filter-divides-by-zero");
        String drukte = "// hij aan alle volgende voorwaarden voldoet: - hij is actief - het aantal vondsten van de Jutter die"
                + " aan de volgende voorwaarde voldoen: zijn gewicht gedeeld door zijn deelnemers is groter dan 2 is groter"
                + " dan 0";
        String fold = "// het aantal vondsten van de Jutter die aan de volgende voorwaarde voldoen: zijn gewicht gedeeld door"
                + " zijn deelnemers is groter dan 2";
        assertEquals(List.of(
                "r3_toets0 | " + drukte + " | roept [r3_samengesteld1]",
                "r3_samengesteld1 | " + drukte + " | roept [r3_vouwlezing2]",
                "r3_vouwlezing2 | " + fold + " | roept []",
                "r3_fout3 | " + drukte + " | roept [r3_samengesteld4]",
                "r3_samengesteld4 | " + drukte + " | roept [r3_vouwlezing5]",
                "r3_vouwlezing5 | " + fold + " | roept []",
                "regel | roept [r3_toets0, r3_fout3]"), table(b.drl(), piece(b, "drukte: fout in de voorwaarde")));
        assertEquals(List.of(
                "r4_toets0 | " + drukte + " | roept [r4_samengesteld1]",
                "r4_samengesteld1 | " + drukte + " | roept [r4_vouwlezing2]",
                "r4_vouwlezing2 | " + fold + " | roept []",
                "r4_deponering3 | " + drukte + " | roept [r4_samengesteld4]",
                "r4_samengesteld4 | " + drukte + " | roept [r4_vouwlezing5]",
                "r4_vouwlezing5 | " + fold + " | roept []",
                "regel | roept [r4_toets0, r4_deponering3]"), table(b.drl(), piece(b, "drukte: deponering in de voorwaarde")));
    }

    @Test void a_default_without_a_condition_has_no_test() throws Exception {
        // the composite over no member under ALLE, whose verdict is true (ComputeHelpers.endVerdict(ALLE, 0, 0, 0)): no
        // function and no eval; the rule fires on its patterns alone
        DroolsBackend.Build b = build("jutterswet-bergingstoeslag");
        DrlPiece p = piece(b, "defaults A:Jutter.bergingstoeslag: default 1");
        assertEquals(List.of(
                "r1_afloop0 | // 0.00 | roept []",
                "regel | roept [r1_afloop0]"), table(b.drl(), p));
        assertFalse(rule(b, p).contains("eval("), rule(b, p));
        // the same in the default whose binder its rule does not reach: its functions are those of its value
        assertEquals(List.of(
                "r2_afloop0 | // de bergingsgrond is zijn bergingsgrondslag | roept [r2_let1]",
                "r2_let1 | // de bergingsgrond is zijn bergingsgrondslag | roept []",
                "r2_binder2 | // zijn bergingsgrondslag | roept []",
                "regel | roept [r2_afloop0]"), table(b.drl(), piece(b, "defaults A:Jutter.bergingsforfait: default 0")));
    }

    @Test void a_composite_of_one_member_is_that_member_in_the_goldens() throws Exception {
        // ALLE, a test of the form: the participation of a key of one component
        DroolsBackend.Build b = build("anchor/jutterswet-kustregister");
        DrlPiece p = piece(b, "eenmalig vondstnummer: sleutel van Strandvondst 1");
        assertEquals(List.of(
                "r7_toets0 | - | roept []",
                "r7_componenten1 | - | roept []",
                "regel | roept [r7_toets0, r7_componenten1]"), table(b.drl(), p));
        assertEquals("function boolean r7_toets0(Object $vondstnummer) {\n"
                + "    return ComputeHelpers.emptyTest($vondstnummer, Boolean.FALSE);\n"
                + "}\n\n", function(b, p, "r7_toets0"));

        // DE, the filter of a fold: in the test without its fault, in the test of the fault and its function, and in the
        // test of the deposit and its function
        b = build("fold-filter-divides-by-zero");
        String filter = "// aan de volgende voorwaarde voldoen: zijn gewicht gedeeld door zijn deelnemers is groter dan 2";
        String member = "RegelSpraakRuntime.rsAggVergelijk(ComputeHelpers.divided($gewicht, $deelnemers, null),"
                + " new java.math.BigDecimal(\"2\"), \">\")";
        p = piece(b, "zware vondsten: bijdrage aan vouw 1");
        assertEquals(List.of(
                "r6_toets0 | " + filter + " | roept []",
                "regel | roept [r6_toets0]"), table(b.drl(), p));
        assertEquals(filter + "\nfunction boolean r6_toets0(Object $gewicht, Object $deelnemers) {\n"
                + "    try {\n"
                + "        return " + member + ";\n"
                + "    } catch (RuntimeException e) {\n"
                + "        ComputeHelpers.caught(e);\n"
                + "        return false;\n"
                + "    }\n"
                + "}\n\n", function(b, p, "r6_toets0"));
        p = piece(b, "zware vondsten: filterfout in vouw 1");
        assertEquals(List.of(
                "r7_beta0 | - | roept []",
                "r7_toets1 | " + filter + " | roept []",
                "r7_fout2 | " + filter + " | roept []",
                "r7_plaats3 | - | roept []",
                "regel | roept [r7_beta0, r7_toets1, r7_fout2, r7_plaats3]"), table(b.drl(), p));
        assertTrue(function(b, p, "r7_toets1").contains("        boolean uitkomst = " + member + ";\n"),
                function(b, p, "r7_toets1"));
        p = piece(b, "zware vondsten: filterdeponering in vouw 1");
        List<String> names = new ArrayList<>();
        for (DrlPiece.DrlFunction f : p.functions()) names.add(f.name());
        assertTrue(names.stream().noneMatch(n -> n.contains("samengesteld")), names::toString);

        // GEEN over one member is its negation, no member's verdict: the test carries the composite
        b = build("jutterswet-uniekwantor");
        p = piece(b, "ongemerkt aantal: bijdrage aan vouw 0, selectie 0");
        assertEquals(List.of(
                "r3_toets0 | // aan geen van de volgende voorwaarden voldoen: gemerkt zijn | roept []",
                "regel | roept [r3_toets0]"), table(b.drl(), p));
        assertTrue(function(b, p, "r3_toets0").contains(
                "return ComputeHelpers.endVerdict(dev.uchar.regelspraak.ir.Condition.Quantifier.GEEN, 0, raak, 1);"),
                function(b, p, "r3_toets0"));
    }

    /** The quantifiers over one member, each with the threshold the case needs, and whether the composite's verdict is
     *  the member's: then the test returns the member, else it carries the composite. */
    private static final List<Object[]> ONE_MEMBER = List.of(
            new Object[] {"the", 0, true}, new Object[] {"all", 0, true}, new Object[] {"none", 0, false},
            new Object[] {"atLeast", 1, true}, new Object[] {"atLeast", 2, false}, new Object[] {"atMost", 0, false},
            new Object[] {"atMost", 1, false}, new Object[] {"exactly", 0, false}, new Object[] {"exactly", 1, true});

    /** Whether the composite of {@code quantifier} with {@code threshold} over one member holds, for a member that does
     *  or does not hold: RUNTIME-SEMANTICS ch. 5.10, written out by hand. */
    private static boolean holds(String quantifier, int threshold, boolean member) {
        int hits = member ? 1 : 0;
        return switch (quantifier) {
            case "the", "all" -> member;
            case "none" -> !member;
            case "atLeast" -> hits >= threshold;
            case "atMost" -> hits <= threshold;
            case "exactly" -> hits == threshold;
            default -> throw new IllegalArgumentException(quantifier);
        };
    }

    @Test void a_composite_of_one_member_is_that_member_where_its_quantifier_gives_the_members_verdict() throws Exception {
        List<String> characteristics = new ArrayList<>(List.of("lid"));
        List<String> rules = new ArrayList<>();
        for (int i = 0; i < ONE_MEMBER.size(); i++) {
            characteristics.add("doel" + i);
            rules.add(characteristicRule("één lid " + i, "doel" + i, TmsDocuments.compound((String) ONE_MEMBER.get(i)[0],
                    (Integer) ONE_MEMBER.get(i)[1], List.of(check("lid", false)))));
        }
        Path document = TmsDocuments.document(TmsDocuments.model(characteristics), rules);
        DroolsBackend.Build b = DroolsBackend.build(document, DroolsBackend.Branch.DRL_TEXT);
        for (int i = 0; i < ONE_MEMBER.size(); i++) {
            Object[] q = ONE_MEMBER.get(i);
            DrlPiece p = piece(b, "één lid " + i);
            String sentence = "// " + q[0] + " " + q[1];
            assertEquals(List.of("r" + i + "_toets0 | " + sentence + " | roept []", "regel | roept [r" + i + "_toets0]"),
                    table(b.drl(), p), q[0] + " " + q[1]);
            String test = function(b, p, "r" + i + "_toets0");
            if ((Boolean) q[2])
                assertEquals(sentence + "\nfunction boolean r" + i + "_toets0(Object $lid) {\n"
                        + "    return ComputeHelpers.characteristic($lid, Boolean.TRUE);\n}\n\n", test, q[0] + " " + q[1]);
            else
                assertTrue(test.contains("ComputeHelpers.established("), q[0] + " " + q[1] + ":\n" + test);
        }
        // the text means what the plan means: a Jutter with the member and one without, on both branches
        List<String> expectations = new ArrayList<>();
        for (int i = 0; i < ONE_MEMBER.size(); i++) {
            Object[] q = ONE_MEMBER.get(i);
            expectations.add(TmsDocuments.expected("met", "doel" + i, holds((String) q[0], (Integer) q[1], true)));
            expectations.add(TmsDocuments.expected("zonder", "doel" + i, holds((String) q[0], (Integer) q[1], false)));
        }
        play(document, List.of(TmsDocuments.jutter("met", List.of("lid")), TmsDocuments.jutter("zonder", List.of())),
                expectations);
    }

    /** A composite of {@code parts} under {@code quantifier} with the sentence {@code sentence}. */
    private static String compound(String quantifier, int threshold, List<String> parts, String sentence) {
        return "{\"node\":\"Compound\",\"quantifier\":\"" + quantifier + "\",\"threshold\":" + threshold + ",\"parts\":["
                + String.join(",", parts) + "],\"source\":{\"snippet\":\"" + sentence + "\"}}";
    }

    @Test void a_composite_of_one_member_in_another_is_its_member_with_its_sentence_as_a_comment_line() throws Exception {
        Path document = TmsDocuments.document(TmsDocuments.model(List.of("a", "b", "genest", "omhuld", "leeg", "gelijk")),
                List.of(
                        // a member of one member with a sentence of its own: the member in place of the call, the
                        // sentence as a comment line above it
                        characteristicRule("genest", "genest", compound("all", 0, List.of(check("a", false),
                                compound("the", 0, List.of(check("b", false)), "aan de voorwaarde b voldoet")),
                                "aan alle voorwaarden voldoet")),
                        // a test over a composite of one member that is a composite of two: the test carries the body
                        // of the inner one, whose sentence stands above it
                        characteristicRule("omhuld", "omhuld", compound("the", 0, List.of(compound("all", 0,
                                List.of(check("a", false), check("b", false)), "aan a en b voldoet")),
                                "aan de volgende voorwaarde voldoet")),
                        // a member over no member: its verdict in place of the call, its sentence above it
                        characteristicRule("leeg", "leeg", compound("all", 0, List.of(check("a", false),
                                compound("all", 0, List.of(), "altijd")), "aan a en altijd voldoet")),
                        // a member of one member with the sentence of the composite it stands in: no comment line
                        characteristicRule("gelijk", "gelijk", compound("all", 0, List.of(check("a", false),
                                compound("the", 0, List.of(check("b", false)), "aan beide voldoet")),
                                "aan beide voldoet"))));
        DroolsBackend.Build b = DroolsBackend.build(document, DroolsBackend.Branch.DRL_TEXT);
        String established = "ComputeHelpers.established(dev.uchar.regelspraak.ir.Condition.Quantifier.ALLE, 0, raak, ";
        String end = "ComputeHelpers.endVerdict(dev.uchar.regelspraak.ir.Condition.Quantifier.ALLE, 0, raak, 2)";
        assertEquals("// aan alle voorwaarden voldoet\n"
                + "function boolean r0_toets0(Object $a, Object $b) {\n"
                + "    int raak = 0;\n"
                + "    Boolean vast;\n"
                + "    vast = " + established + "0, 2);\n"
                + "    if (vast != null) {\n        return vast;\n    }\n"
                + "    if (ComputeHelpers.characteristic($a, Boolean.TRUE)) {\n        raak++;\n    }\n"
                + "    vast = " + established + "1, 1);\n"
                + "    if (vast != null) {\n        return vast;\n    }\n"
                + "    // aan de voorwaarde b voldoet\n"
                + "    if (ComputeHelpers.characteristic($b, Boolean.TRUE)) {\n        raak++;\n    }\n"
                + "    return " + end + ";\n"
                + "}\n\n", function(b, piece(b, "genest"), "r0_toets0"));
        assertEquals("// aan de volgende voorwaarde voldoet\n"
                + "function boolean r1_toets0(Object $a, Object $b) {\n"
                + "    // aan a en b voldoet\n"
                + "    int raak = 0;\n"
                + "    Boolean vast;\n"
                + "    vast = " + established + "0, 2);\n"
                + "    if (vast != null) {\n        return vast;\n    }\n"
                + "    if (ComputeHelpers.characteristic($a, Boolean.TRUE)) {\n        raak++;\n    }\n"
                + "    vast = " + established + "1, 1);\n"
                + "    if (vast != null) {\n        return vast;\n    }\n"
                + "    if (ComputeHelpers.characteristic($b, Boolean.TRUE)) {\n        raak++;\n    }\n"
                + "    return " + end + ";\n"
                + "}\n\n", function(b, piece(b, "omhuld"), "r1_toets0"));
        assertEquals("// aan a en altijd voldoet\n"
                + "function boolean r2_toets0(Object $a) {\n"
                + "    int raak = 0;\n"
                + "    Boolean vast;\n"
                + "    vast = " + established + "0, 2);\n"
                + "    if (vast != null) {\n        return vast;\n    }\n"
                + "    if (ComputeHelpers.characteristic($a, Boolean.TRUE)) {\n        raak++;\n    }\n"
                + "    vast = " + established + "1, 1);\n"
                + "    if (vast != null) {\n        return vast;\n    }\n"
                + "    // altijd\n"
                + "    if (ComputeHelpers.endVerdict(dev.uchar.regelspraak.ir.Condition.Quantifier.ALLE, 0, 0, 0)) {\n"
                + "        raak++;\n    }\n"
                + "    return " + end + ";\n"
                + "}\n\n", function(b, piece(b, "leeg"), "r2_toets0"));
        String gelijk = function(b, piece(b, "gelijk"), "r3_toets0");
        assertEquals(1, gelijk.split("aan beide voldoet", -1).length - 1, gelijk);
        for (String rule : List.of("genest", "omhuld", "leeg", "gelijk"))
            assertEquals(1, piece(b, rule).functions().size(), () -> rule + ": " + table(b.drl(), piece(b, rule)));

        // the text means what the plan means, for every combination of a and b, on both branches
        List<String> instances = new ArrayList<>(), expectations = new ArrayList<>();
        for (boolean a : new boolean[] {false, true})
            for (boolean c : new boolean[] {false, true}) {
                String label = (a ? "a" : "") + (c ? "b" : "") + "-";
                List<String> own = new ArrayList<>();
                if (a) own.add("a");
                if (c) own.add("b");
                instances.add(TmsDocuments.jutter(label, own));
                expectations.add(TmsDocuments.expected(label, "genest", a && c));
                expectations.add(TmsDocuments.expected(label, "omhuld", a && c));
                expectations.add(TmsDocuments.expected(label, "leeg", a));
                expectations.add(TmsDocuments.expected(label, "gelijk", a && c));
            }
        play(document, instances, expectations);
    }

    /** The case of {@code instances} with {@code expectations} plays on the exec-model branch and on the DRL-text
     *  branch. */
    private static void play(Path document, List<String> instances, List<String> expectations) throws Exception {
        var c = TmsDocuments.caseDocument(instances, expectations);
        assertNull(CaseRunner.playCase(document, c, DroolsBackend.Branch.EXEC_MODEL), "exec-model");
        assertNull(CaseRunner.playCase(document, c, DroolsBackend.Branch.DRL_TEXT), "drl-tekst");
    }
}
