package dev.uchar.regelspraak;

import dev.uchar.regelspraak.emit.IrEmitter;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A sum, minimum or maximum over the labels of the subject's own dimensioned attribute (§13.4.17.51-.54) is an
 * {@code Aggregation} with {@code dimensionSelections} (IR-SPEC ch. 4.3, CONSTRUCTS row 28): it folds label cells,
 * not instances, so it carries no element type and no range, and its one argument is the subject's own read. The
 * resolver records the subject as the fold's element type; written on the wire, an element type on a
 * single-argument fold demands a range. Measured on the frontend before this: the emitter refused every such fold
 * ("haar bron is het eigen attribuut van het onderwerp en geen instantiebron").
 */
class DimensionFoldTest {

    private static final String HEAD = """
        Eenheidsysteem Gewicht
            het kilogram (mv: kilogrammen) kg

        Dimensie het seizoen, bestaande uit de seizoenen (na het attribuut met voorzetsel in):
            1. voorjaar
            2. zomer
            3. najaar

        Objecttype de Jutter (mv: Jutters) (bezield)
            het vondstgewicht Numeriek (niet-negatief getal met 2 decimalen) met eenheid kg gedimensioneerd met seizoen;
            het jaargewicht Numeriek (niet-negatief getal met 2 decimalen) met eenheid kg;

        Regel jaargewicht van een jutter
            geldig altijd
                Het jaargewicht van een Jutter moet berekend worden als\s""";

    private static String emitCompact(String value) {
        RegelSpraakModel m = Resolver.resolve(HEAD + value + "\n");
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        return IrEmitter.emit(m).replaceAll("\\s+", "");
    }

    private static final String OWN_READ = "\"arguments\":[{\"node\":\"AttributeReference\",\"attribute\":\"vondstgewicht\","
            + "\"subjectType\":\"Jutter\",\"source\":{\"snippet\":\"zijnvondstgewicht\"}}],";

    @Test void the_sum_over_all_labels_carries_the_selection_and_no_element_type() {
        String c = emitCompact("de som van zijn vondstgewicht over alle seizoenen.");
        assertTrue(c.contains("\"node\":\"Aggregation\",\"kind\":\"sum\"," + OWN_READ
                + "\"dimensionSelections\":[{\"node\":\"DimensionSelection\",\"dimension\":\"seizoen\","
                + "\"labels\":[\"voorjaar\",\"zomer\",\"najaar\"],\"source\":{\"snippet\":\"overalleseizoenen\"}}],"
                + "\"type\":{\"node\":\"Number\""), () -> "de som over alle seizoenen:\n" + c);
        assertFalse(c.contains("\"elementType\""), () -> "een elementtype op de dimensievouw:\n" + c);
        assertFalse(c.contains("\"sourceRole\"") || c.contains("\"sourcePath\""), () -> "een bereik op de dimensievouw:\n" + c);
    }

    @Test void the_sum_over_an_interval_carries_the_labels_in_declared_order() {
        String c = emitCompact("de som van zijn vondstgewicht vanaf voorjaar t/m zomer.");
        assertTrue(c.contains("\"kind\":\"sum\"," + OWN_READ
                + "\"dimensionSelections\":[{\"node\":\"DimensionSelection\",\"dimension\":\"seizoen\","
                + "\"labels\":[\"voorjaar\",\"zomer\"],\"source\":{\"snippet\":\"vanafvoorjaart/mzomer\"}}],"),
                () -> "de som over een interval:\n" + c);
        assertFalse(c.contains("\"elementType\""), () -> "een elementtype op de dimensievouw:\n" + c);
    }

    /** The grammar admits "en" inside a label, so a set of two labels written without a comma is parsed as ONE
     *  member "voorjaar en najaar"; measured before this: no declared label matched, the selection stayed
     *  unresolved with a WARNING and the emitter refused the fold. */
    @Test void a_set_of_two_labels_without_comma_reads_two_labels() {
        String c = emitCompact("de maximale waarde van zijn vondstgewicht over de seizoenen in {voorjaar en najaar}.");
        assertTrue(c.contains("\"kind\":\"maximum\"," + OWN_READ
                + "\"dimensionSelections\":[{\"node\":\"DimensionSelection\",\"dimension\":\"seizoen\","
                + "\"labels\":[\"voorjaar\",\"najaar\"],"), () -> "de verzameling van twee labels:\n" + c);
    }

    /** A declared label that contains "en" is read whole, and a member that joins such a label to another reads
     *  as the one way of splitting it into declared labels. */
    @Test void a_label_with_and_stays_whole() {
        RegelSpraakModel m = Resolver.resolve("""
            Dimensie het getij, bestaande uit de getijden (na het attribuut met voorzetsel in):
                1. eb en vloed
                2. doodtij
                3. springtij

            Objecttype de Jutter (mv: Jutters) (bezield)
                de getijvangst Numeriek (niet-negatief getal met 2 decimalen) gedimensioneerd met getij;
                de getijsom Numeriek (niet-negatief getal met 2 decimalen);
                de kentering Numeriek (niet-negatief getal met 2 decimalen);

            Regel getijsom van een jutter
                geldig altijd
                    De getijsom van een Jutter moet berekend worden als de som van zijn getijvangst over de getijden in {eb en vloed en doodtij}.

            Regel kentering van een jutter
                geldig altijd
                    De kentering van een Jutter moet berekend worden als de som van zijn getijvangst over de getijden in {eb en vloed, doodtij en springtij}.
            """);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        String c = IrEmitter.emit(m).replaceAll("\\s+", "");
        assertTrue(c.contains("\"labels\":[\"ebenvloed\",\"doodtij\"],"), () -> "het label met en, heel gelezen:\n" + c);
        assertTrue(c.contains("\"labels\":[\"ebenvloed\",\"doodtij\",\"springtij\"],"), () -> "drie leden:\n" + c);
    }

    @Test void the_maximum_over_an_interval_carries_the_selection() {
        String c = emitCompact("de maximale waarde van zijn vondstgewicht vanaf zomer t/m najaar.");
        assertTrue(c.contains("\"kind\":\"maximum\"," + OWN_READ
                + "\"dimensionSelections\":[{\"node\":\"DimensionSelection\",\"dimension\":\"seizoen\","
                + "\"labels\":[\"zomer\",\"najaar\"],"), () -> "het maximum over een interval:\n" + c);
        assertFalse(c.contains("\"elementType\""), () -> "een elementtype op de dimensievouw:\n" + c);
    }

    // A fold over an attribute with two dimensions writes one selection per dimension, as ALEF writes it (one filter
    // per dimension, a fixed dimension as a set of one label); the wire carries them in the declared order of the
    // attribute's dimensions, whatever order the sentence names them in.

    private static final String TWO = """
        Dimensie het seizoen, bestaande uit de seizoenen (na het attribuut met voorzetsel in):
            1. voorjaar
            2. zomer
            3. najaar

        Dimensie het strand, bestaande uit de stranden (na het attribuut met voorzetsel op):
            1. noord
            2. zuid

        Dimensie de herkomst, bestaande uit de herkomsten (na het attribuut met voorzetsel uit):
            1. zee
            2. rivier

        Objecttype de Jutter (mv: Jutters) (bezield)
            het vondstgewicht Numeriek (niet-negatief getal met 2 decimalen) gedimensioneerd met seizoen en strand;
            de aanvoer Numeriek (niet-negatief getal met 2 decimalen) gedimensioneerd met strand en herkomst;
            het jaargewicht Numeriek (niet-negatief getal met 2 decimalen);

        Regel jaargewicht van een jutter
            geldig altijd
                Het jaargewicht van een Jutter moet berekend worden als\s""";

    private static RegelSpraakModel resolveTwo(String value) {
        return Resolver.resolve(TWO + value + "\n");
    }

    private static String emitTwo(String value) {
        RegelSpraakModel m = resolveTwo(value);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        return IrEmitter.emit(m).replaceAll("\\s+", "");
    }

    private static String selection(String dimension, String snippet, String... labels) {
        return "{\"node\":\"DimensionSelection\",\"dimension\":\"" + dimension + "\",\"labels\":[\""
                + String.join("\",\"", labels) + "\"],\"source\":{\"snippet\":\"" + snippet + "\"}}";
    }

    @Test void a_fold_names_one_selection_per_dimension() {
        String c = emitTwo("de som van zijn vondstgewicht over alle seizoenen over de stranden in {noord}.");
        assertTrue(c.contains("\"kind\":\"sum\"," + OWN_READ + "\"dimensionSelections\":["
                + selection("seizoen", "overalleseizoenen", "voorjaar", "zomer", "najaar") + ","
                + selection("strand", "overdestrandenin{noord}", "noord") + "],"), () -> "twee selecties:\n" + c);
    }

    @Test void the_selections_go_out_in_the_declared_order() {
        String c = emitTwo("de maximale waarde van zijn vondstgewicht over de stranden in {zuid} over de seizoenen "
                + "vanaf voorjaar t/m zomer.");
        assertTrue(c.contains("\"kind\":\"maximum\"," + OWN_READ + "\"dimensionSelections\":["
                + selection("seizoen", "overdeseizoenenvanafvoorjaart/mzomer", "voorjaar", "zomer") + ","
                + selection("strand", "overdestrandenin{zuid}", "zuid") + "],"), () -> "de gedeclareerde volgorde:\n" + c);
    }

    /** The operand takes "over de seizoenen" as name words and leaves the bare interval; the resolver splits it off
     *  as in the one-dimension fold, and the selection after it is read as usual. */
    @Test void an_explicit_interval_absorbed_by_the_operand_is_split_off() {
        String c = emitTwo("de minimale waarde van zijn vondstgewicht over de seizoenen vanaf zomer t/m najaar "
                + "over alle stranden.");
        assertTrue(c.contains("\"kind\":\"minimum\",\"arguments\":[{\"node\":\"AttributeReference\",\"attribute\":"
                + "\"vondstgewicht\",\"subjectType\":\"Jutter\",\"source\":{\"snippet\":\"zijnvondstgewichtoverdeseizoenen\"}}],"
                + "\"dimensionSelections\":[" + selection("seizoen", "vanafzomert/mnajaar", "zomer", "najaar") + ","
                + selection("strand", "overallestranden", "noord", "zuid") + "],"), () -> "het afgesplitste interval:\n" + c);
    }

    /** An interval without "over de <meervoud>" names no dimension; it selects the one dimension no other selection
     *  of the fold names and whose labels it fits. */
    @Test void a_bare_interval_takes_the_dimension_left_over() {
        String c = emitTwo("de som van zijn vondstgewicht vanaf voorjaar t/m zomer over de stranden in {noord}.");
        assertTrue(c.contains("\"kind\":\"sum\"," + OWN_READ + "\"dimensionSelections\":["
                + selection("seizoen", "vanafvoorjaart/mzomer", "voorjaar", "zomer") + ","
                + selection("strand", "overdestrandenin{noord}", "noord") + "],"), () -> "het kale interval:\n" + c);
        String s = emitTwo("de som van zijn vondstgewicht vanaf noord t/m zuid over alle seizoenen.");
        assertTrue(s.contains("\"kind\":\"sum\"," + OWN_READ + "\"dimensionSelections\":["
                + selection("seizoen", "overalleseizoenen", "voorjaar", "zomer", "najaar") + ","
                + selection("strand", "vanafnoordt/mzuid", "noord", "zuid") + "],"), () -> "het kale interval, tweede dimensie:\n" + s);
    }

    /** Both dimensions carry the interval's bounds; the over-form names one, so the interval takes the other. */
    @Test void a_bare_interval_skips_a_dimension_another_selection_names() {
        RegelSpraakModel m = Resolver.resolve(COAST + "de som van zijn stand over de kusten in {noord} vanaf noord t/m zuid.\n");
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        String c = IrEmitter.emit(m).replaceAll("\\s+", "");
        assertTrue(c.contains("\"dimensionSelections\":[" + selection("kust", "overdekustenin{noord}", "noord") + ","
                + selection("wind", "vanafnoordt/mzuid", "noord", "zuid") + "],"), () -> "het interval op de wind:\n" + c);
    }

    /** A selection that names no dimension of the attribute stays the honest marker; its warning quotes every
     *  selection of the fold. */
    @Test void an_unlowered_selection_warns_with_every_selection() {
        RegelSpraakModel m = resolveTwo("de som van zijn vondstgewicht over alle seizoenen over alle kusten.");
        assertTrue(m.diagnostics.stream().anyMatch(d -> "RGS-070".equals(d.code())
                        && d.args().get("expression").toString().equals("over alle seizoenen over alle kusten")),
                () -> "RGS-070 over beide selecties: " + m.diagnostics);
    }

    @Test void a_fold_without_a_selection_of_every_dimension_is_refused() {
        DimensionCellTest.assertOneError(resolveTwo("de som van zijn vondstgewicht over alle seizoenen."), "RGS-366",
                Map.of("expression", "de som van zijn vondstgewicht over alle seizoenen", "attribute", "vondstgewicht",
                        "dimension", "strand", "plural", "stranden"));
    }

    @Test void a_dimension_selected_twice_is_refused() {
        DimensionCellTest.assertOneError(resolveTwo("de som van zijn vondstgewicht over alle seizoenen over de seizoenen "
                + "in {voorjaar} over alle stranden."), "RGS-367",
                Map.of("expression", "de som van zijn vondstgewicht over alle seizoenen over de seizoenen in {voorjaar} "
                        + "over alle stranden", "dimension", "seizoen"));
    }

    /** A fold names a dimension by its plural, so one declared with "uit" is selected like any other. */
    @Test void a_fold_selects_in_a_dimension_declared_with_uit() {
        String c = emitTwo("de som van zijn aanvoer over alle stranden over de herkomsten in {zee}.");
        assertTrue(c.contains("\"kind\":\"sum\",\"arguments\":[{\"node\":\"AttributeReference\",\"attribute\":\"aanvoer\","
                + "\"subjectType\":\"Jutter\",\"source\":{\"snippet\":\"zijnaanvoer\"}}],\"dimensionSelections\":["
                + selection("strand", "overallestranden", "noord", "zuid") + ","
                + selection("herkomst", "overdeherkomstenin{zee}", "zee") + "],"), () -> "twee selecties:\n" + c);
    }

    /** The operand of a fold carries no label (ALEF); a label after "uit" on it makes the operand a cell without a label
     *  of the other dimension, refused as one after any other preposition. */
    @Test void a_label_after_uit_on_the_operand_is_a_cell_without_its_other_label() {
        RegelSpraakModel m = resolveTwo("de som van zijn aanvoer uit zee over alle stranden.");
        assertTrue(m.diagnostics.stream().noneMatch(d -> "RGS-091".equals(d.code())), () -> "een syntaxfout: " + m.diagnostics);
        assertTrue(m.diagnostics.stream().anyMatch(d -> "RGS-364".equals(d.code()) && d.args().equals(Map.of(
                        "written", "zijn aanvoer uit zee", "attribute", "aanvoer", "dimension", "strand"))),
                () -> "RGS-364 voor de dimensie strand: " + m.diagnostics);
    }

    /** Two dimensions with the same labels. */
    private static final String COAST = """
        Dimensie de kust, bestaande uit de kusten (na het attribuut met voorzetsel in):
            1. noord
            2. zuid

        Dimensie de wind, bestaande uit de winden (na het attribuut met voorzetsel op):
            1. noord
            2. zuid

        Objecttype de Jutter (mv: Jutters) (bezield)
            de stand Numeriek (geheel getal) gedimensioneerd met kust en wind;
            de uitkomst Numeriek (geheel getal);

        Regel uitkomst van een jutter
            geldig altijd
                De uitkomst van een Jutter moet berekend worden als\s""";

    /** Two dimensions carry both bounds of the interval and neither has a selection of its own. */
    @Test void a_bare_interval_that_fits_two_dimensions_is_refused() {
        RegelSpraakModel m = Resolver.resolve(COAST + "de som van zijn stand vanaf noord t/m zuid.\n");
        DimensionCellTest.assertOneError(m, "RGS-368", Map.of("expression", "de som van zijn stand vanaf noord t/m zuid",
                "interval", "vanaf noord t/m zuid", "dimensions", "kust, wind"));
    }
}
