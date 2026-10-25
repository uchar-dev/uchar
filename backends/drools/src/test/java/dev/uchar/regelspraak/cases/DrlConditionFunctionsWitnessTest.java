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
                "toets_defaultsKJutterUitgeslotenDefault000 | " + sentence + " | roept []",
                "regel | roept [toets_defaultsKJutterUitgeslotenDefault000]"), table(b.drl(), p));
        assertEquals(sentence + "\n"
                + "function boolean toets_defaultsKJutterUitgeslotenDefault000(Object $vergunningGeweigerd, Object $weigeringHerroepen, Object $herroepingVervallen) {\n"
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
                + "}\n\n", function(b, p, "toets_defaultsKJutterUitgeslotenDefault000"));

        // VORM: the participation of a key of a uniqueness, a composite of the form without a sentence
        b = build("anchor/jutterswet-kustregister");
        p = piece(b, "eenmalig kaveletiket: sleutel van Kavel 1");
        assertEquals(List.of(
                "toets_eenmaligKaveletiketSleutelVanKavel1 | - | roept []",
                "componenten_eenmaligKaveletiketSleutelVanKavel1 | - | roept []",
                "regel | roept [toets_eenmaligKaveletiketSleutelVanKavel1, componenten_eenmaligKaveletiketSleutelVanKavel1]"), table(b.drl(), p));
        assertTrue(function(b, p, "toets_eenmaligKaveletiketSleutelVanKavel1").contains("ComputeHelpers.established("), function(b, p, "toets_eenmaligKaveletiketSleutelVanKavel1"));

        // WITHOUT_FAULT: the body of the composite within the try
        b = build("fold-filter-divides-by-zero");
        p = piece(b, "drukte");
        String drukte = "// hij aan alle volgende voorwaarden voldoet: - hij is actief - het aantal vondsten van de Jutter die"
                + " aan de volgende voorwaarde voldoen: zijn gewicht gedeeld door zijn deelnemers is groter dan 2 is groter"
                + " dan 0";
        String fold = "// het aantal vondsten van de Jutter die aan de volgende voorwaarde voldoen: zijn gewicht gedeeld door"
                + " zijn deelnemers is groter dan 2";
        assertEquals(List.of(
                "toets_drukte | " + drukte + " | roept [vouwlezing_drukte]",
                "vouwlezing_drukte | " + fold + " | roept []",
                "regel | roept [toets_drukte]"), table(b.drl(), p));
        String parameters = "$actief, $aantal_vouw_46, $eerste_filterfout_vouw_51, $filterdeponeringen_vouw_55";
        assertEquals(drukte + "\n"
                + "function boolean toets_drukte(Object $actief, Object $aantal_vouw_46, Object $eerste_filterfout_vouw_51,"
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
                + "        if (RegelSpraakRuntime.rsAggVergelijk(vouwlezing_drukte(" + parameters + "), new java.math.BigDecimal(\"0\"),"
                + " \">\")) {\n"
                + "            raak++;\n"
                + "        }\n"
                + "        return ComputeHelpers.endVerdict(dev.uchar.regelspraak.ir.Condition.Quantifier.ALLE, 0, raak, 2);\n"
                + "    } catch (RuntimeException e) {\n"
                + "        ComputeHelpers.caught(e);\n"
                + "        return false;\n"
                + "    }\n"
                + "}\n\n", function(b, p, "toets_drukte"));
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
                "toets_drukteFoutInDeVoorwaarde | " + drukte + " | roept [samengesteld1_drukteFoutInDeVoorwaarde]",
                "samengesteld1_drukteFoutInDeVoorwaarde | " + drukte + " | roept [vouwlezing1_drukteFoutInDeVoorwaarde]",
                "vouwlezing1_drukteFoutInDeVoorwaarde | " + fold + " | roept []",
                "fout_drukteFoutInDeVoorwaarde | " + drukte + " | roept [samengesteld2_drukteFoutInDeVoorwaarde]",
                "samengesteld2_drukteFoutInDeVoorwaarde | " + drukte + " | roept [vouwlezing2_drukteFoutInDeVoorwaarde]",
                "vouwlezing2_drukteFoutInDeVoorwaarde | " + fold + " | roept []",
                "regel | roept [toets_drukteFoutInDeVoorwaarde, fout_drukteFoutInDeVoorwaarde]"), table(b.drl(), piece(b, "drukte: fout in de voorwaarde")));
        assertEquals(List.of(
                "toets_drukteDeponeringInDeVoorwaarde | " + drukte + " | roept [samengesteld1_drukteDeponeringInDeVoorwaarde]",
                "samengesteld1_drukteDeponeringInDeVoorwaarde | " + drukte + " | roept [vouwlezing1_drukteDeponeringInDeVoorwaarde]",
                "vouwlezing1_drukteDeponeringInDeVoorwaarde | " + fold + " | roept []",
                "deponering_drukteDeponeringInDeVoorwaarde | " + drukte + " | roept [samengesteld2_drukteDeponeringInDeVoorwaarde]",
                "samengesteld2_drukteDeponeringInDeVoorwaarde | " + drukte + " | roept [vouwlezing2_drukteDeponeringInDeVoorwaarde]",
                "vouwlezing2_drukteDeponeringInDeVoorwaarde | " + fold + " | roept []",
                "regel | roept [toets_drukteDeponeringInDeVoorwaarde, deponering_drukteDeponeringInDeVoorwaarde]"), table(b.drl(), piece(b, "drukte: deponering in de voorwaarde")));
    }

    @Test void a_default_without_a_condition_has_no_test() throws Exception {
        // the composite over no member under ALLE, whose verdict is true (ComputeHelpers.endVerdict(ALLE, 0, 0, 0)): no
        // function and no eval; the rule fires on its patterns alone. Its value, a constant, has no function either:
        // the consequence carries it, with its sentence above it
        DroolsBackend.Build b = build("jutterswet-bergingstoeslag");
        DrlPiece p = piece(b, "defaults A:Jutter.bergingstoeslag: default 1");
        assertEquals(List.of("regel | roept []"), table(b.drl(), p));
        assertFalse(rule(b, p).contains("eval("), rule(b, p));
        assertTrue(rule(b, p).contains("then\n    // 0.00\n    Object v = ComputeHelpers.collapse(new java.math.BigDecimal(\"0\"));\n"),
                rule(b, p));
        // the same in the default whose binder its rule does not reach: its functions are those of its value
        assertEquals(List.of(
                "afloop_defaultsAJutterBergingsforfaitDefault0 | // de bergingsgrond is zijn bergingsgrondslag | roept [let_defaultsAJutterBergingsforfaitDefault0]",
                "let_defaultsAJutterBergingsforfaitDefault0 | // de bergingsgrond is zijn bergingsgrondslag | roept []",
                "binder_defaultsAJutterBergingsforfaitDefault0 | // zijn bergingsgrondslag | roept []",
                "regel | roept [afloop_defaultsAJutterBergingsforfaitDefault0]"), table(b.drl(), piece(b, "defaults A:Jutter.bergingsforfait: default 0")));
    }

    @Test void a_composite_of_one_member_is_that_member_in_the_goldens() throws Exception {
        // ALLE, a test of the form: the participation of a key of one component
        DroolsBackend.Build b = build("anchor/jutterswet-kustregister");
        DrlPiece p = piece(b, "eenmalig vondstnummer: sleutel van Strandvondst 1");
        assertEquals(List.of(
                "toets_eenmaligVondstnummerSleutelVanStrandvondst1 | - | roept []",
                "componenten_eenmaligVondstnummerSleutelVanStrandvondst1 | - | roept []",
                "regel | roept [toets_eenmaligVondstnummerSleutelVanStrandvondst1, componenten_eenmaligVondstnummerSleutelVanStrandvondst1]"), table(b.drl(), p));
        assertEquals("function boolean toets_eenmaligVondstnummerSleutelVanStrandvondst1(Object $vondstnummer) {\n"
                + "    return ComputeHelpers.emptyTest($vondstnummer, Boolean.FALSE);\n"
                + "}\n\n", function(b, p, "toets_eenmaligVondstnummerSleutelVanStrandvondst1"));

        // DE, the filter of a fold: in the test without its fault, in the test of the fault and its function, and in the
        // test of the deposit and its function
        b = build("fold-filter-divides-by-zero");
        String filter = "// aan de volgende voorwaarde voldoen: zijn gewicht gedeeld door zijn deelnemers is groter dan 2";
        String member = "RegelSpraakRuntime.rsAggVergelijk(ComputeHelpers.divided($gewicht, $deelnemers, null),"
                + " new java.math.BigDecimal(\"2\"), \">\")";
        p = piece(b, "zware vondsten: bijdrage aan vouw 1");
        assertEquals(List.of(
                "toets_zwareVondstenBijdrageAanVouw1 | " + filter + " | roept []",
                "regel | roept [toets_zwareVondstenBijdrageAanVouw1]"), table(b.drl(), p));
        assertEquals(filter + "\nfunction boolean toets_zwareVondstenBijdrageAanVouw1(Object $gewicht, Object $deelnemers) {\n"
                + "    try {\n"
                + "        return " + member + ";\n"
                + "    } catch (RuntimeException e) {\n"
                + "        ComputeHelpers.caught(e);\n"
                + "        return false;\n"
                + "    }\n"
                + "}\n\n", function(b, p, "toets_zwareVondstenBijdrageAanVouw1"));
        p = piece(b, "zware vondsten: filterfout in vouw 1");
        assertEquals(List.of(
                "beta_zwareVondstenFilterfoutInVouw1 | - | roept []",
                "toets_zwareVondstenFilterfoutInVouw1 | " + filter + " | roept []",
                "fout_zwareVondstenFilterfoutInVouw1 | " + filter + " | roept []",
                "plaats_zwareVondstenFilterfoutInVouw1 | - | roept []",
                "regel | roept [beta_zwareVondstenFilterfoutInVouw1, toets_zwareVondstenFilterfoutInVouw1, fout_zwareVondstenFilterfoutInVouw1, plaats_zwareVondstenFilterfoutInVouw1]"), table(b.drl(), p));
        assertTrue(function(b, p, "toets_zwareVondstenFilterfoutInVouw1").contains("        boolean uitkomst = " + member + ";\n"),
                function(b, p, "toets_zwareVondstenFilterfoutInVouw1"));
        p = piece(b, "zware vondsten: filterdeponering in vouw 1");
        List<String> names = new ArrayList<>();
        for (DrlPiece.DrlFunction f : p.functions()) names.add(f.name());
        assertTrue(names.stream().noneMatch(n -> n.contains("samengesteld")), names::toString);

        // GEEN over one member is its negation, no member's verdict: the test carries the composite
        b = build("jutterswet-uniekwantor");
        p = piece(b, "ongemerkt aantal: bijdrage aan vouw 0, selectie 0");
        assertEquals(List.of(
                "toets_ongemerktAantalBijdrageAanVouw0Selectie0 | // aan geen van de volgende voorwaarden voldoen: gemerkt zijn | roept []",
                "regel | roept [toets_ongemerktAantalBijdrageAanVouw0Selectie0]"), table(b.drl(), p));
        assertTrue(function(b, p, "toets_ongemerktAantalBijdrageAanVouw0Selectie0").contains(
                "return ComputeHelpers.endVerdict(dev.uchar.regelspraak.ir.Condition.Quantifier.GEEN, 0, raak, 1);"),
                function(b, p, "toets_ongemerktAantalBijdrageAanVouw0Selectie0"));
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
            String name = "toets_éénLid" + i;
            assertEquals(List.of(name + " | " + sentence + " | roept []", "regel | roept [" + name + "]"),
                    table(b.drl(), p), q[0] + " " + q[1]);
            String test = function(b, p, name);
            if ((Boolean) q[2])
                assertEquals(sentence + "\nfunction boolean " + name + "(Object $lid) {\n"
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

    @Test void a_composite_over_no_member_is_its_verdict() throws Exception {
        // the default without a condition under another quantifier: GEEN over no member holds, TEN_MINSTE 1 does not
        // (ComputeHelpers.endVerdict over no member); the first is no test, the second a test that returns the verdict
        // of the composite, without a function of the composite and without its locals
        Path document = TmsDocuments.document(TmsDocuments.model(List.of("geenDoel", "tenMinsteDoel")), List.of(
                characteristicRule("geen lid onder geen", "geenDoel", compound("none", 0, List.of(),
                        "aan geen van de voorwaarden voldoet")),
                characteristicRule("geen lid onder ten minste een", "tenMinsteDoel", compound("atLeast", 1, List.of(),
                        "aan ten minste een van de voorwaarden voldoet"))));
        DroolsBackend.Build b = DroolsBackend.build(document, DroolsBackend.Branch.DRL_TEXT);
        DrlPiece none = piece(b, "geen lid onder geen");
        assertEquals(List.of("regel | roept []"), table(b.drl(), none));
        assertFalse(rule(b, none).contains("eval("), rule(b, none));
        DrlPiece atLeast = piece(b, "geen lid onder ten minste een");
        assertEquals(List.of("toets_geenLidOnderTenMinsteEen | // aan ten minste een van de voorwaarden voldoet | roept []",
                "regel | roept [toets_geenLidOnderTenMinsteEen]"), table(b.drl(), atLeast));
        assertEquals("// aan ten minste een van de voorwaarden voldoet\n"
                + "function boolean toets_geenLidOnderTenMinsteEen() {\n"
                + "    return ComputeHelpers.endVerdict(dev.uchar.regelspraak.ir.Condition.Quantifier.TEN_MINSTE, 1, 0, 0);\n"
                + "}\n\n", function(b, atLeast, "toets_geenLidOnderTenMinsteEen"));
        play(document, List.of(TmsDocuments.jutter("j", List.of())), List.of(
                TmsDocuments.expected("j", "geenDoel", true), TmsDocuments.expected("j", "tenMinsteDoel", false)));
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
                + "function boolean toets_genest(Object $a, Object $b) {\n"
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
                + "}\n\n", function(b, piece(b, "genest"), "toets_genest"));
        assertEquals("// aan de volgende voorwaarde voldoet\n"
                + "function boolean toets_omhuld(Object $a, Object $b) {\n"
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
                + "}\n\n", function(b, piece(b, "omhuld"), "toets_omhuld"));
        assertEquals("// aan a en altijd voldoet\n"
                + "function boolean toets_leeg(Object $a) {\n"
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
                + "}\n\n", function(b, piece(b, "leeg"), "toets_leeg"));
        String gelijk = function(b, piece(b, "gelijk"), "toets_gelijk");
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
