package dev.uchar.regelspraak;

import dev.uchar.regelspraak.emit.CaseEmitter;
import dev.uchar.regelspraak.emit.IrEmitter;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.resolver.Resolver;
import dev.uchar.regelspraak.resolver.ScenarioResolver;
import dev.uchar.regelspraak.resolver.SemanticTokens;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A cell of a dimensioned attribute of another object than the subject (§3.6, the forms ALEF writes): read over a path
 * of to-one roles ("het vondstgewicht in voorjaar van de leider van de Ploeg"), folded over its dimensions on the
 * object the path reaches ("de som van het vondstgewicht van de leider van de Ploeg over alle seizoenen"), and read
 * on each member of a set as the source of a value fold ("de som van het vondstgewicht in voorjaar van alle leden van
 * de Ploeg", IR-SPEC ch. 4.3). Beside it a label that contains the word "en" ("bij eb en vloed"), read behind the
 * attribute as one label in every position a cell is written (grammar/DIVERGENCES.md, "More dimensions").
 */
class DimensionCellOverRoleTest {

    private static final String MODEL = """
        Dimensie het seizoen, bestaande uit de seizoenen (na het attribuut met voorzetsel in):
            1. voorjaar
            2. zomer
            3. najaar

        Dimensie het strand, bestaande uit de stranden (na het attribuut met voorzetsel op):
            1. noord
            2. zuid

        Objecttype de Jutter (mv: Jutters) (bezield)
            het vondstgewicht Numeriek (niet-negatief getal met 2 decimalen) gedimensioneerd met seizoen;
            de aanvoer Numeriek (niet-negatief getal met 2 decimalen) gedimensioneerd met seizoen en strand;

        Objecttype de Ploeg (mv: Ploegen)
            de uitkomst Numeriek (niet-negatief getal met 2 decimalen);
            het aantal Numeriek (niet-negatief geheel getal);

        Objecttype de Haven (mv: Havens)
            de havenuitkomst Numeriek (niet-negatief getal met 2 decimalen);

        Feittype ploegleiding
            de leider Jutter
            de geleide ploeg Ploeg
        Eén leider heeft één geleide ploeg

        Feittype ploeglidmaatschap
            de ploeg Ploeg
            het lid (mv: leden) Jutter
        Eén ploeg heeft meerdere leden

        Feittype havenbemanning
            de haven Haven
            de havenploeg Ploeg
        Eén haven heeft één havenploeg

        """;

    private static final String LEIDER = "{\"node\":\"RoleBinding\",\"factType\":\"ploegleiding\",\"role\":\"leider\"}";

    /** The model resolved with one rule on a Ploeg whose target is {@code target} and whose value is {@code value}. */
    private static RegelSpraakModel resolve(String target, String value) {
        return Resolver.resolve(MODEL + "Regel r\n    geldig altijd\n        " + target
                + " van een Ploeg moet berekend worden als " + value + ".\n");
    }

    private static String emitCompact(String target, String value) {
        RegelSpraakModel m = resolve(target, value);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        return IrEmitter.emit(m).replaceAll("\\s+", "");
    }

    @Test void a_cell_is_read_over_a_to_one_role() {
        String ir = emitCompact("De uitkomst", "het vondstgewicht in voorjaar van de leider van de Ploeg");
        assertTrue(ir.contains("\"value\":{\"node\":\"DimensionCellRead\",\"attribute\":{\"node\":\"AttributeReference\","
                + "\"attribute\":\"vondstgewicht\",\"subjectType\":\"Jutter\",\"source\":{\"snippet\":\"hetvondstgewichtin"
                + "voorjaarvandeleidervandePloeg\"},\"path\":[" + LEIDER + "]},\"labels\":[\"voorjaar\"],"), ir);
    }

    @Test void a_cell_over_two_dimensions_is_read_over_a_role() {
        String ir = emitCompact("De uitkomst", "de aanvoer op zuid en in najaar van de leider van de Ploeg");
        assertTrue(ir.contains("\"attribute\":\"aanvoer\",\"subjectType\":\"Jutter\""), ir);
        assertTrue(ir.contains("\"path\":[" + LEIDER + "]},\"labels\":[\"najaar\",\"zuid\"]"), ir);
    }

    /** "de X van de leider van de havenploeg van de Haven": the chain of two to-one roles ends in the cell. */
    @Test void a_cell_is_read_over_two_roles() {
        RegelSpraakModel m = Resolver.resolve(MODEL + """
            Regel r
                geldig altijd
                    De havenuitkomst van een Haven moet berekend worden als het vondstgewicht in najaar van de leider van de havenploeg van de Haven.
            """);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        String ir = IrEmitter.emit(m).replaceAll("\\s+", "");
        assertTrue(ir.contains("\"attribute\":\"vondstgewicht\",\"subjectType\":\"Jutter\""), ir);
        assertTrue(ir.contains("\"path\":[{\"node\":\"RoleBinding\",\"factType\":\"havenbemanning\",\"role\":\"havenploeg\"},"
                + LEIDER + "]},\"labels\":[\"najaar\"]"), ir);
    }

    @Test void a_cell_over_a_role_names_a_label_of_every_dimension() {
        DimensionCellTest.assertOneError(resolve("De uitkomst", "de aanvoer in najaar van de leider van de Ploeg"),
                "RGS-364", Map.of("written", "de aanvoer in najaar van de leider van de Ploeg", "attribute", "aanvoer",
                        "dimension", "strand"));
    }

    @Test void a_dimension_fold_reads_the_cells_of_the_object_a_role_reaches() {
        String ir = emitCompact("De uitkomst", "de som van het vondstgewicht van de leider van de Ploeg over alle seizoenen");
        assertTrue(ir.contains("\"value\":{\"node\":\"Aggregation\",\"kind\":\"sum\",\"arguments\":[{\"node\":"
                + "\"AttributeReference\",\"attribute\":\"vondstgewicht\",\"subjectType\":\"Jutter\",\"source\":{\"snippet\":"
                + "\"hetvondstgewichtvandeleidervandePloeg\"},\"path\":[" + LEIDER + "]}],\"dimensionSelections\":[{\"node\":"
                + "\"DimensionSelection\",\"dimension\":\"seizoen\",\"labels\":[\"voorjaar\",\"zomer\",\"najaar\"]"), ir);
        assertFalse(ir.contains("\"elementType\""), ir);
        String last = emitCompact("De uitkomst", "de laatste van het vondstgewicht van de leider van de Ploeg over alle seizoenen");
        assertTrue(last.contains("\"kind\":\"last\",\"arguments\":[{\"node\":\"AttributeReference\""), last);
    }

    /** ALEF's fold of one cell over a role: the value fold's source is the cell of each member, its reference read on
     *  the element type without a path, the range the fold's own. */
    @Test void a_value_fold_reads_one_cell_of_each_member() {
        String ir = emitCompact("De uitkomst", "de som van het vondstgewicht in voorjaar van alle leden van de Ploeg");
        assertTrue(ir.contains("\"value\":{\"node\":\"Aggregation\",\"kind\":\"sum\",\"arguments\":[{\"node\":"
                + "\"DimensionCellRead\",\"attribute\":{\"node\":\"AttributeReference\",\"attribute\":\"vondstgewicht\","
                + "\"subjectType\":\"Jutter\",\"source\":{\"snippet\":\"hetvondstgewichtinvoorjaarvanalleledenvandePloeg\"}},"
                + "\"labels\":[\"voorjaar\"],"), ir);
        assertTrue(ir.contains("\"elementType\":\"Jutter\""), ir);
        assertTrue(ir.contains("\"sourceRole\":{\"node\":\"RoleBinding\",\"factType\":\"ploeglidmaatschap\",\"role\":\"lid\"}"), ir);
        String max = emitCompact("De uitkomst", "de maximale waarde van de aanvoer in zomer op noord van alle leden van de Ploeg");
        assertTrue(max.contains("\"kind\":\"maximum\",\"arguments\":[{\"node\":\"DimensionCellRead\""), max);
        assertTrue(max.contains("\"labels\":[\"zomer\",\"noord\"]"), max);
        String count = emitCompact("Het aantal",
                "het aantal gevulde waarden van het vondstgewicht in najaar van alle leden van de Ploeg");
        assertTrue(count.contains("\"kind\":\"filledCount\",\"arguments\":[{\"node\":\"DimensionCellRead\""), count);
    }

    // ---------------- a label that contains "en" ----------------

    private static final String TIDES = """
        Dimensie het getij, bestaande uit de getijden (na het attribuut met voorzetsel bij):
            1. eb
            2. vloed
            3. eb en vloed

        Dimensie het strand, bestaande uit de stranden (na het attribuut met voorzetsel op):
            1. noord
            2. zuid

        Objecttype de Jutter (mv: Jutters) (bezield)
            het getijgewicht Numeriek (getal met 2 decimalen) gedimensioneerd met getij en strand;
            de uitkomst Numeriek (getal met 2 decimalen);

        """;

    private static RegelSpraakModel tides(String rule) {
        return Resolver.resolve(TIDES + "Regel r\n    geldig altijd\n        " + rule + "\n");
    }

    private static String tidesCompact(String rule) {
        RegelSpraakModel m = tides(rule);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        return IrEmitter.emit(m).replaceAll("\\s+", "");
    }

    /** The declared label "eb en vloed" behind the attribute, beside a label of the other dimension, before or after
     *  it, side by side or joined; the labels "eb" and "zuid" joined by "en" stay two labels. */
    @Test void a_label_with_en_reads_as_one_label_behind_the_attribute() {
        String[][] forms = {
                {"zijn getijgewicht bij eb en vloed op noord", "\"eb en vloed\",\"noord\""},
                {"zijn getijgewicht op noord en bij eb en vloed", "\"eb en vloed\",\"noord\""},
                {"zijn getijgewicht bij eb en vloed en op zuid", "\"eb en vloed\",\"zuid\""},
                {"zijn getijgewicht bij eb en op zuid", "\"eb\",\"zuid\""}};
        for (String[] f : forms) {
            String ir = tidesCompact("De uitkomst van een Jutter moet berekend worden als " + f[0] + ".");
            assertTrue(ir.contains("\"labels\":[" + f[1].replace(" ", "") + "]"), () -> f[0] + ": " + ir);
        }
    }

    @Test void a_label_with_en_names_a_cell_target() {
        String ir = tidesCompact("Het getijgewicht bij eb en vloed op zuid van een Jutter moet berekend worden als "
                + "zijn getijgewicht bij vloed op zuid.");
        assertTrue(ir.contains("\"target\":{\"node\":\"DimensionCellRead\""), ir);
        assertTrue(ir.contains("\"labels\":[\"ebenvloed\",\"zuid\"]"), ir);
    }

    /** An "en" that no declared label holds and no preposition follows stays the syntax error it was. */
    @Test void an_en_outside_a_declared_label_stays_a_syntax_error() {
        RegelSpraakModel m = tides("De uitkomst van een Jutter moet berekend worden als zijn getijgewicht bij eb en noord.");
        assertTrue(m.diagnostics.stream().anyMatch(d -> d.code().equals("RGS-091")), () -> m.diagnostics.toString());
        RegelSpraakModel withoutLabel = Resolver.resolve(TIDES.replace("    3. eb en vloed\n", "")
                + "Regel r\n    geldig altijd\n        De uitkomst van een Jutter moet berekend worden als zijn "
                + "getijgewicht bij eb en vloed op noord.\n");
        assertTrue(withoutLabel.diagnostics.stream().anyMatch(d -> d.code().equals("RGS-091")),
                () -> withoutLabel.diagnostics.toString());
    }

    /** A bullet of a test document names the cell with the label as a rule does, input and expectation. */
    @Test void a_scenario_names_a_cell_with_a_label_with_en() {
        ScenarioResolver.ScenarioBundle b = ScenarioResolver.resolve(TIDES + """
            Regel r
                geldig altijd
                    De uitkomst van een Jutter moet berekend worden als zijn getijgewicht bij eb en vloed op noord.

            Scenario s
            Gegeven is de volgende situatie:
            een Jutter J
            - het getijgewicht bij eb en vloed op noord is 2,00
            Met als verwacht resultaat:
            - de uitkomst van J is 2,00
            - het getijgewicht bij eb en vloed op noord van J is 2,00
            """);
        assertTrue(b.isOk(), () -> b.model().errors().toString());
        String c = CaseEmitter.emit(b.scenarios().get(0)).replaceAll("\\s+", "");
        assertTrue(c.contains("\"cells\":[{\"labels\":[\"ebenvloed\",\"noord\"]"), c);
        assertTrue(c.contains("\"kind\":\"cellValue\",\"label\":\"J\",\"attribute\":\"getijgewicht\",\"labels\":"
                + "[\"ebenvloed\",\"noord\"]"), c);
    }

    /** The semantic tokens read the label as one name of a label, as a member of a set reads it: none is
     *  unresolved, and "en" inside it is no keyword. */
    @Test void the_semantic_tokens_read_a_label_with_en() {
        String source = TIDES + """
            Regel r
                geldig altijd
                    De uitkomst van een Jutter moet berekend worden als zijn getijgewicht bij eb en vloed op noord.
            """;
        List<SemanticTokens.SourceFile> files = List.of(new SemanticTokens.SourceFile("getij.rgs", source));
        SemanticTokens.Document d = Resolver.tokens(files);
        SemanticTokensCheck.check(files, d);
        assertTrue(d.diagnostics().isEmpty(), () -> d.diagnostics().toString());
        for (SemanticTokens.Span s : d.files().get(0).spans())
            assertNotEquals(SemanticTokens.Kind.UNRESOLVED, s.kind(), () -> "unresolved: '" + s.text() + "' at line " + s.line());
        int line = (int) TIDES.lines().count() + 3;
        List<String> spans = d.files().get(0).spans().stream().filter(s -> s.line() == line)
                .map(s -> s.kind() + " " + s.text()).toList();
        assertTrue(spans.contains("DIMENSION_LABEL eb en vloed"), spans::toString);
        assertTrue(spans.contains("DIMENSION_LABEL noord"), spans::toString);
        assertFalse(spans.stream().anyMatch(s -> s.endsWith(" en")), spans::toString);
    }
}
