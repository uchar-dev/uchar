package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.uchar.regelspraak.transpiler.tms.TmsTranspiler.DrlPiece;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static dev.uchar.regelspraak.cases.DrlPiecesWitnessTest.piece;
import static dev.uchar.regelspraak.cases.DrlPiecesWitnessTest.table;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The functions of a value in the DRL text (card drl-no-trivial-value-functions, acceptance (b)): the value of a
 * decision ({@code afloop}) and of a value consequence ({@code waarde}) that computes nothing, a constant or the variable
 * a binding binds, with or without {@code ComputeHelpers.collapse}, has no function. The consequence carries its
 * expression, {@code Object v = …;}, with the sentence of the term as a comment line above it, and asks no evaluation
 * fault of {@code v}, since that expression cannot fault; a probe of the plan stays. A value that computes something,
 * also one call, keeps its function. On goldens where the corpus shows the form, the forms the analysis makes: a
 * decision over a constant without a fault in the plan and over a collapsed one with it, over a bound variable
 * without a fault and over a collapsed one with it, a value consequence, always collapsed and with its fault, over a
 * constant, a bound variable and a text with a quote, and one with a probe in the plan; on documents written here a
 * text that Drools's preparation of a consequence and MVEL would read otherwise. Per rule the table of its piece
 * ({@link DrlPiecesWitnessTest#table}) and its consequence; the cases of each play on both branches.
 * {@code transpiler.tms.DrlCarriedValueWitnessTest} holds every class of constant and the forms the analysis does not
 * make.
 *
 * <p><b>Red</b> on {@code 4707a183}, before the card: every table here named a function {@code afloop_…} or
 * {@code waarde_…} that only returned its value, and the consequence called it.
 */
class DrlValueFunctionsWitnessTest {

    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");

    private static DroolsBackend.Build build(String golden) throws Exception {
        return DroolsBackend.build(GOLDEN.resolve(golden).resolve("ir.json"), DroolsBackend.Branch.DRL_TEXT);
    }

    /** The consequence of the rule of piece {@code p}: the text after its line {@code then} up to its end. */
    private static String consequence(DroolsBackend.Build b, DrlPiece p) {
        String rule = b.drl().substring(p.ruleStart(), p.end());
        return rule.substring(rule.indexOf("\nthen\n") + "\nthen\n".length());
    }

    /** The cases of golden {@code golden} play on both branches. */
    private static void plays(String golden) throws Exception {
        Path map = GOLDEN.resolve(golden);
        List<Path> cases;
        try (Stream<Path> s = Files.list(map)) {
            cases = s.filter(f -> f.getFileName().toString().matches("case.*\\.json")).sorted().toList();
        }
        assertTrue(!cases.isEmpty(), golden);
        CaseRunner.Result exec = CaseRunner.viaExecModel(map.resolve("ir.json"), cases);
        assertEquals("PLAYED", exec.status(), golden + " exec-model: " + exec.reason() + " " + exec.caseFailures());
        CaseRunner.Result drl = CaseRunner.viaDrlText(map.resolve("ir.json"), cases);
        assertEquals("PLAYED", drl.status(), golden + " drl-tekst: " + drl.reason() + " " + drl.caseFailures());
    }

    @Test void a_decision_that_computes_nothing_is_carried_by_its_consequence() throws Exception {
        // a constant, without a fault in the plan: the decision of a characteristic default
        DroolsBackend.Build b = build("jutterswet-uitsluiting-rgs");
        DrlPiece p = piece(b, "defaults K:Jutter.uitgesloten: default 0.0.0");
        assertEquals(List.of("toets_defaultsKJutterUitgeslotenDefault000 | // hij vergunning geweigerd is en hij weigering"
                        + " herroepen is en hij herroeping vervallen is | roept []",
                "regel | roept [toets_defaultsKJutterUitgeslotenDefault000]"), table(b.drl(), p));
        assertEquals("    Object v = Boolean.TRUE;\n"
                + "    if (v != null) {\n"
                + "        insertLogical(new Outcome($onderwerp, \"defaults K:Jutter.uitgesloten\", 1, 0,"
                + " dev.uchar.regelspraak.transpiler.tms.Outcome.Kind.DECIDED, v, null));\n"
                + "    }\n"
                + "end\n\n", consequence(b, p));
        plays("jutterswet-uitsluiting-rgs");

        // a collapsed constant, with its fault in the plan: the decision of an attribute default; and a bound variable,
        // without a fault: the agreement of a reconciliation, which hands the decision of a member on
        b = build("anchor/jutterswet-nestdiepte");
        p = piece(b, "defaults A:Jutter.heffing: default 0.0.0");
        assertEquals(List.of("toets_defaultsAJutterHeffingDefault000 | // hij grondY heeft | roept []",
                "regel | roept [toets_defaultsAJutterHeffingDefault000]"), table(b.drl(), p));
        assertEquals("    // 10.00\n"
                + "    Object v = ComputeHelpers.collapse(new java.math.BigDecimal(\"10\"));\n"
                + "    if (v != null) {\n"
                + "        insertLogical(new Outcome($onderwerp, \"defaults A:Jutter.heffing\", 1, 0,"
                + " dev.uchar.regelspraak.transpiler.tms.Outcome.Kind.DECIDED, v, null));\n"
                + "    }\n"
                + "end\n\n", consequence(b, p));
        p = piece(b, "defaults A:Jutter.heffing: eens 0.1.0");
        assertEquals(List.of("beta1_defaultsAJutterHeffingEens010 | - | roept []",
                "beta2_defaultsAJutterHeffingEens010 | - | roept []",
                "regel | roept [beta1_defaultsAJutterHeffingEens010, beta2_defaultsAJutterHeffingEens010]"),
                table(b.drl(), p));
        assertEquals("    Object v = $waarde;\n"
                + "    if (v != null) {\n"
                + "        insertLogical(new Outcome($onderwerp, \"defaults A:Jutter.heffing\", 2, 0,"
                + " dev.uchar.regelspraak.transpiler.tms.Outcome.Kind.DECIDED, v, null));\n"
                + "    }\n"
                + "end\n\n", consequence(b, p));
        plays("anchor/jutterswet-nestdiepte");

        // a collapsed bound variable, with its fault in the plan
        b = build("anchor/jutterswet-strandrechtketen");
        p = piece(b, "defaults A:Strandrecht.uitkering: default 1");
        assertEquals(List.of("regel | roept []"), table(b.drl(), p));
        assertEquals("    // zijn grondslag\n"
                + "    Object v = ComputeHelpers.collapse($grondslag);\n"
                + "    if (v != null) {\n"
                + "        insertLogical(new Value($onderwerp, ElementKey.of(\"strandrecht\", \"uitkering\", false), v,"
                + " \"defaults A:Strandrecht.uitkering\"));\n"
                + "        insertLogical(new Decides($onderwerp, \"defaults A:Strandrecht.uitkering\", 1));\n"
                + "    }\n"
                + "end\n\n", consequence(b, p));
        plays("anchor/jutterswet-strandrechtketen");
    }

    @Test void a_value_that_computes_nothing_is_carried_by_its_consequence() throws Exception {
        // a collapsed constant
        DroolsBackend.Build b = build("anchor/jutterswet-strandrechtketen");
        DrlPiece p = piece(b, "registratiekosten van een strandrecht");
        assertEquals(List.of("regel | roept []"), table(b.drl(), p));
        assertEquals("    // 5.00\n"
                + "    Object v = ComputeHelpers.collapse(new java.math.BigDecimal(\"5\"));\n"
                + "    if (!ComputeHelpers.isEmpty(v)) {\n"
                + "        insertLogical(new Value($onderwerp, ElementKey.of(\"strandrecht\", \"registratiekosten\", false),"
                + " v, \"registratiekosten van een strandrecht\"));\n"
                + "    }\n"
                + "end\n\n", consequence(b, p));

        // a collapsed bound variable, read along a path
        b = build("anchor/jutterswet-kustwacht");
        p = piece(b, "uitkijkbonus van een wachter");
        assertEquals(List.of("pad_uitkijkbonusVanEenWachter | - | roept []",
                "regel | roept [pad_uitkijkbonusVanEenWachter]"), table(b.drl(), p));
        assertEquals("    // de hoogte van zijn post\n"
                + "    Object v = ComputeHelpers.collapse($postHoogte);\n"
                + "    if (!ComputeHelpers.isEmpty(v)) {\n"
                + "        insertLogical(new Value($onderwerp, ElementKey.of(\"wachter\", \"uitkijkbonus\", false), v,"
                + " \"uitkijkbonus van een wachter\"));\n"
                + "    }\n"
                + "end\n\n", consequence(b, p));
        plays("anchor/jutterswet-kustwacht");

        // a collapsed text with a quote, under a sentence with quotes
        b = build("jutterswet-keurmerk");
        p = piece(b, "afslag van een gekeurde vondst");
        assertEquals(List.of("toets_afslagVanEenGekeurdeVondst | // de Vondst is gekeurd | roept []",
                "regel | roept [toets_afslagVanEenGekeurdeVondst]"), table(b.drl(), p));
        assertEquals("    // 'veiling \"Zee\"'\n"
                + "    Object v = ComputeHelpers.collapse(\"veiling \\\"Zee\\\"\");\n"
                + "    if (!ComputeHelpers.isEmpty(v)) {\n"
                + "        insertLogical(new Value($onderwerp, ElementKey.of(\"vondst\", \"afslag\", false), v,"
                + " \"afslag van een gekeurde vondst\"));\n"
                + "    }\n"
                + "end\n\n", consequence(b, p));
        plays("jutterswet-keurmerk");
    }

    @Test void a_value_with_a_probe_that_computes_nothing_keeps_its_probe() throws Exception {
        // the condition divides, so the rule has a probe, and the plan gives it to the value consequence as well; a
        // constant reports nothing to it, and the consequence keeps it as the plan says
        DroolsBackend.Build b = build("jutterswet-ploegaandeel");
        DrlPiece p = piece(b, "bergingsaandeel van een jutter");
        assertEquals(List.of("toets_bergingsaandeelVanEenJutter | // hij aan alle volgende voorwaarden voldoet: - zijn"
                        + " ploegomvang is ongelijk aan 0 - het aandeel is groter dan 0 | roept []",
                "regel | roept [toets_bergingsaandeelVanEenJutter]"), table(b.drl(), p));
        assertEquals("    Object sonde = ComputeHelpers.depositFlag();\n"
                + "    // 1\n"
                + "    Object v = ComputeHelpers.collapse(new java.math.BigDecimal(\"1\"));\n"
                + "    if (ComputeHelpers.reported(sonde)) {\n"
                + "        insertLogical(ComputeHelpers.inconsistency(\"bergingsaandeel van een jutter\", $onderwerp,"
                + " ComputeHelpers.depositCodes(sonde)));\n"
                + "    }\n"
                + "    if (!ComputeHelpers.isEmpty(v)) {\n"
                + "        insertLogical(new Value($onderwerp, ElementKey.of(\"jutter\", \"bergingsaandeel\", false), v,"
                + " \"bergingsaandeel van een jutter\"));\n"
                + "    }\n"
                + "end\n\n", consequence(b, p));
        plays("jutterswet-ploegaandeel");
    }

    @Test void a_value_that_computes_something_keeps_its_function() throws Exception {
        // a decision of one call
        DroolsBackend.Build b = build("anchor/jutterswet-strandrechtketen");
        DrlPiece p = piece(b, "defaults A:Wrakrecht.uitkering: default 2");
        assertEquals(List.of("afloop_defaultsAWrakrechtUitkeringDefault2 | // zijn grondslag maal 2 | roept []",
                "regel | roept [afloop_defaultsAWrakrechtUitkeringDefault2]"), table(b.drl(), p));
        assertTrue(consequence(b, p).startsWith("    Object v = afloop_defaultsAWrakrechtUitkeringDefault2($grondslag);\n"
                + "    if (ComputeHelpers.isEvaluationFault(v)) {\n"), consequence(b, p));
        // a value of one call
        b = build("anchor/jutterswet-leegrekening");
        p = piece(b, "bruto van een vondst");
        assertEquals(List.of("waarde_brutoVanEenVondst | // het gewicht van de Vondst plus 5 | roept []",
                "regel | roept [waarde_brutoVanEenVondst]"), table(b.drl(), p));
        assertEquals("function Object waarde_brutoVanEenVondst(Object $gewicht) {\n"
                + "    try {\n"
                + "        return ComputeHelpers.collapse(RegelSpraakRuntime.rsPlus($gewicht, new java.math.BigDecimal(\"5\")));\n"
                + "    } catch (RuntimeException e) {\n"
                + "        return ComputeHelpers.caught(e);\n"
                + "    }\n"
                + "}\n\n", b.drl().substring(p.functions().get(0).declaration(), p.functions().get(0).end()));
        assertTrue(consequence(b, p).startsWith("    Object v = waarde_brutoVanEenVondst($gewicht);\n"
                + "    if (ComputeHelpers.isEvaluationFault(v)) {\n"), consequence(b, p));
        plays("anchor/jutterswet-leegrekening");
    }

    /** The texts of {@link #a_carried_text_and_its_sentence_are_read_as_written}: a backslash at the end, which the
     *  preparation of a consequence reads as escaping the closing quote; U+0000 beside an escape, which MVEL does not keep
     *  in its place; a backslash before {@code u}, which MVEL reads as a text and not as an escape; and the marks of a
     *  comment and the word that ends a rule. */
    private static final List<String> TEXTS = List.of("eindigt op een backslash \\", "n\0\"z", "a\0\\", "\\u0041",
            "h//x #y /*z*/ end");

    /** The sentence of the value: the marks that open a string, a block, a comment or an escape, and a line break. */
    private static final String SENTENCE = "zin met 'apostrof', \"aanhalingsteken\", {accolade, (haakje, \\u005c, \\u12,"
            + " end, then, #, // en /* en */\nnog een regel";

    @Test void a_carried_text_and_its_sentence_are_read_as_written() throws Exception {
        // the golden jutterswet-keurmerk with another text as the value of its afslag and another sentence above it;
        // its own case, with the afslag of V1 the text, plays on both branches
        ObjectNode golden = (ObjectNode) TmsDocuments.JSON.readTree(GOLDEN.resolve("jutterswet-keurmerk").resolve("ir.json")
                .toFile());
        JsonNode goldenCase = TmsDocuments.JSON.readTree(GOLDEN.resolve("jutterswet-keurmerk").resolve("case.json").toFile());
        List<String> consequences = new ArrayList<>();
        for (String text : TEXTS) {
            ObjectNode ir = golden.deepCopy();
            ObjectNode value = (ObjectNode) IrPath.version(ir, "rules", 1, 0).get("value");
            value.putObject("value").put("text", text);
            value.set("source", IrPath.sourceReference(SENTENCE));
            Path document = Files.createTempFile("drl-gedragen-tekst", ".json");
            document.toFile().deleteOnExit();
            Files.writeString(document, ir.toString());
            ObjectNode c = goldenCase.deepCopy();
            for (JsonNode e : (ArrayNode) c.get("expected"))
                if (e.get("kind").asText().equals("attributeValue") && e.get("label").asText().equals("V1"))
                    ((ObjectNode) e.get("value")).put("text", text);
            assertNull(CaseRunner.playCase(document, c, DroolsBackend.Branch.EXEC_MODEL), "exec-model: " + text);
            assertNull(CaseRunner.playCase(document, c, DroolsBackend.Branch.DRL_TEXT), "drl-tekst: " + text);
            DroolsBackend.Build b = DroolsBackend.build(document, DroolsBackend.Branch.DRL_TEXT);
            String carried = consequence(b, piece(b, "afslag van een gekeurde vondst"));
            consequences.add(carried.substring(0, carried.indexOf("    if (")));
        }
        String comment = "    // zin met 'apostrof', \"aanhalingsteken\", {accolade, (haakje, \\u005c, \\u12, end, then, #,"
                + " // en /* en * / nog een regel\n";
        assertEquals(List.of(
                comment + "    Object v = ComputeHelpers.collapse(\"eindigt op een backslash \\u005C\");\n",
                comment + "    Object v = ComputeHelpers.collapse(\"n\" + \"\0\" + \"\\\"z\");\n",
                comment + "    Object v = ComputeHelpers.collapse(\"a\" + \"\0\" + \"\\u005C\");\n",
                comment + "    Object v = ComputeHelpers.collapse(\"\\u005Cu0041\");\n",
                comment + "    Object v = ComputeHelpers.collapse(\"h//x #y /*z*/ end\");\n"), consequences);
    }

    /** The case of {@code instances} with {@code expectations} plays on the exec-model branch and on the DRL-text
     *  branch. */
    private static void play(Path document, List<String> instances, List<String> expectations) throws Exception {
        JsonNode c = TmsDocuments.caseDocument(instances, expectations);
        assertNull(CaseRunner.playCase(document, c, DroolsBackend.Branch.EXEC_MODEL), "exec-model");
        assertNull(CaseRunner.playCase(document, c, DroolsBackend.Branch.DRL_TEXT), "drl-tekst");
    }
}
