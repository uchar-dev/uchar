package dev.uchar.regelspraak;

import dev.uchar.regelspraak.emit.IrEmitter;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A read of one cell of a dimensioned attribute (§3.6): the attribute with one label of each of its dimensions, a
 * label of a dimension declared "na het attribuut met voorzetsel" written behind the attribute after that preposition,
 * a label of a dimension declared "voor het attribuut zonder voorzetsel" written before it. The labels may stand in
 * any order; the {@code DimensionCellRead} carries them per position, in the declared order of the attribute's
 * dimensions (IR-SPEC ch. 2.7, WF-040). A name that writes labels of some dimensions only, or that fits two cells, is
 * refused with its code.
 */
class DimensionCellTest {

    private static final String MODEL = """
        Dimensie het seizoen, bestaande uit de seizoenen (na het attribuut met voorzetsel in):
            1. voorjaar
            2. late zomer
            3. najaar

        Dimensie het strand, bestaande uit de stranden (na het attribuut met voorzetsel op):
            1. noord
            2. zuid

        Dimensie de soort, bestaande uit de soorten (voor het attribuut zonder voorzetsel):
            1. natte
            2. droge

        Dimensie het getij, bestaande uit de getijden (na het attribuut met voorzetsel bij):
            1. eb
            2. vloed

        Dimensie het boekjaar, bestaande uit de boekjaren (na het attribuut met voorzetsel van):
            1. vorig boekjaar
            2. dit boekjaar

        Objecttype de Jutter (mv: Jutters) (bezield)
            het vondstgewicht Numeriek (niet-negatief getal met 2 decimalen) gedimensioneerd met seizoen en strand;
            de vangst Numeriek (niet-negatief getal met 2 decimalen) gedimensioneerd met soort;
            de buit Numeriek (niet-negatief getal met 2 decimalen) gedimensioneerd met seizoen en soort;
            de stand Numeriek (niet-negatief getal met 2 decimalen) gedimensioneerd met seizoen en strand en getij;
            de winst Numeriek (niet-negatief getal met 2 decimalen) gedimensioneerd met boekjaar en strand;
            de uitkomst Numeriek (niet-negatief getal met 2 decimalen);

        Regel uitkomst van een jutter
            geldig altijd
                De uitkomst van een Jutter moet berekend worden als\s""";

    private static RegelSpraakModel resolve(String value) {
        return Resolver.resolve(MODEL + value + "\n");
    }

    private static String emitCompact(String value) {
        RegelSpraakModel m = resolve(value);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        return IrEmitter.emit(m).replaceAll("\\s+", "");
    }

    private static String cell(String attribute, String snippet, String... labels) {
        return "\"value\":{\"node\":\"DimensionCellRead\",\"attribute\":{\"node\":\"AttributeReference\",\"attribute\":\""
                + attribute + "\",\"subjectType\":\"Jutter\",\"source\":{\"snippet\":\"" + snippet + "\"}},\"labels\":[\""
                + String.join("\",\"", labels) + "\"],";
    }

    @Test void a_cell_over_two_dimensions_names_a_label_of_each() {
        String c = emitCompact("zijn vondstgewicht in voorjaar op zuid.");
        assertTrue(c.contains(cell("vondstgewicht", "zijnvondstgewichtinvoorjaaropzuid", "voorjaar", "zuid")),
                () -> "de cel (voorjaar, zuid):\n" + c);
    }

    /** The second label is written first and carries two words: the cell keeps the declared order of the dimensions. */
    @Test void the_labels_may_be_written_in_any_order() {
        String c = emitCompact("zijn vondstgewicht op zuid in late zomer.");
        assertTrue(c.contains(cell("vondstgewicht", "zijnvondstgewichtopzuidinlatezomer", "latezomer", "zuid")),
                () -> "de cel (late zomer, zuid):\n" + c);
    }

    /** ALEF joins the labels behind the attribute with "en". */
    @Test void the_labels_behind_the_attribute_may_be_joined_with_en() {
        String c = emitCompact("zijn vondstgewicht in voorjaar en op zuid.");
        assertTrue(c.contains(cell("vondstgewicht", "zijnvondstgewichtinvoorjaarenopzuid", "voorjaar", "zuid")),
                () -> "de cel (voorjaar, zuid):\n" + c);
    }

    /** Three labels behind the attribute as ALEF enumerates them: a comma, then "en" before the last. */
    @Test void three_labels_behind_the_attribute_are_an_enumeration() {
        String c = emitCompact("zijn stand op noord, in late zomer en bij vloed.");
        assertTrue(c.contains(cell("stand", "zijnstandopnoord,inlatezomerenbijvloed", "latezomer", "noord", "vloed")),
                () -> "de cel (late zomer, noord, vloed):\n" + c);
    }

    /** "en" before a label of a dimension whose preposition is "van", and after one: the name's own "van" splits it
     *  in the parse, the cell reads the same. */
    @Test void a_label_after_van_joins_with_en_either_way() {
        String first = emitCompact("zijn winst op zuid en van dit boekjaar.");
        assertTrue(first.contains(cell("winst", "zijnwinstopzuidenvanditboekjaar", "ditboekjaar", "zuid")),
                () -> "de cel (dit boekjaar, zuid):\n" + first);
        String second = emitCompact("zijn winst van vorig boekjaar en op noord.");
        assertTrue(second.contains(cell("winst", "zijnwinstvanvorigboekjaarenopnoord", "vorigboekjaar", "noord")),
                () -> "de cel (vorig boekjaar, noord):\n" + second);
    }

    /** Where "en" can end the name, it does: in an operand list a name after "en" that opens with a preposition
     *  stays the next operand, as it was before a cell could carry "en". */
    @Test void an_operand_list_keeps_its_en() {
        RegelSpraakModel m = Resolver.resolve(
                "Parameter het in te houden gewicht : Numeriek (niet-negatief getal met 2 decimalen);\n\n" + MODEL
                + "de maximale waarde van zijn vondstgewicht in voorjaar op zuid en in te houden gewicht.\n");
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        String c = IrEmitter.emit(m).replaceAll("\\s+", "");
        assertTrue(c.contains("{\"node\":\"ParameterReference\",\"parameter\":\"intehoudengewicht\""), c);
        assertTrue(c.contains(cell("vondstgewicht", "zijnvondstgewichtinvoorjaaropzuid", "voorjaar", "zuid")
                .substring("\"value\":".length())), c);
    }

    @Test void a_label_before_the_attribute_is_the_preceding_form() {
        String c = emitCompact("zijn natte vangst.");
        assertTrue(c.contains(cell("vangst", "zijnnattevangst", "natte")), () -> "de cel (natte):\n" + c);
    }

    /** One label before the attribute, one behind it; the declared order puts the season first. */
    @Test void a_cell_mixes_the_two_forms() {
        String c = emitCompact("zijn droge buit in najaar.");
        assertTrue(c.contains(cell("buit", "zijndrogebuitinnajaar", "najaar", "droge")), () -> "de cel (najaar, droge):\n" + c);
    }

    @Test void a_cell_without_a_label_of_every_dimension_is_refused() {
        assertOneError(resolve("zijn vondstgewicht in voorjaar."), "RGS-364",
                Map.of("written", "zijn vondstgewicht in voorjaar", "attribute", "vondstgewicht", "dimension", "strand"));
        assertOneError(resolve("zijn natte buit."), "RGS-364",
                Map.of("written", "zijn natte buit", "attribute", "buit", "dimension", "seizoen"));
    }

    /** Two dimensions with one preposition and the same labels: "in noord in zuid" names two cells. */
    @Test void a_cell_that_fits_two_ways_is_refused() {
        RegelSpraakModel m = Resolver.resolve("""
            Dimensie de kust, bestaande uit de kusten (na het attribuut met voorzetsel in):
                1. noord
                2. zuid

            Dimensie de wind, bestaande uit de winden (na het attribuut met voorzetsel in):
                1. noord
                2. zuid

            Objecttype de Jutter (mv: Jutters) (bezield)
                de stand Numeriek (geheel getal) gedimensioneerd met kust en wind;
                de uitkomst Numeriek (geheel getal);

            Regel uitkomst van een jutter
                geldig altijd
                    De uitkomst van een Jutter moet berekend worden als zijn stand in noord in zuid.
            """);
        assertOneError(m, "RGS-365",
                Map.of("written", "zijn stand in noord in zuid", "cells", "stand[noord, zuid]; stand[zuid, noord]"));
    }

    static void assertOneError(RegelSpraakModel m, String code, Map<String, Object> args) {
        List<Diagnostic> all = m.diagnostics;
        assertEquals(1, all.size(), () -> "verwachtte precies één diagnose " + code + ", kreeg: " + all);
        Diagnostic d = all.get(0);
        assertEquals(RegelSpraakModel.Severity.ERROR, d.severity(), d::toString);
        assertEquals(code, d.code(), d::toString);
        assertEquals(args, d.args(), d::toString);
    }
}
