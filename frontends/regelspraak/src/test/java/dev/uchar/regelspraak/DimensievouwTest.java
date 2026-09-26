package dev.uchar.regelspraak;

import dev.uchar.regelspraak.emit.IrEmitter;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A sum, minimum or maximum over the labels of the subject's own dimensioned attribute (§13.4.17.51-.54) is an
 * {@code Aggregation} with a {@code dimensionSelection} (IR-SPEC ch. 4.3, CONSTRUCTS row 28): it folds label cells,
 * not instances, so it carries no element type and no range, and its one argument is the subject's own read. The
 * resolver records the subject as the fold's element type; written on the wire, an element type on a
 * single-argument fold demands a range. Measured on the frontend before this: the emitter refused every such fold
 * ("haar bron is het eigen attribuut van het onderwerp en geen instantiebron").
 */
class DimensievouwTest {

    private static final String KOP = """
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

    private static String emitCompact(String waarde) {
        RegelSpraakModel m = Resolver.resolve(KOP + waarde + "\n");
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        return IrEmitter.emit(m).replaceAll("\\s+", "");
    }

    private static final String EIGEN_LEZING = "\"arguments\":[{\"node\":\"AttributeReference\",\"attribute\":\"vondstgewicht\","
            + "\"subjectType\":\"Jutter\",\"source\":{\"snippet\":\"zijnvondstgewicht\"}}],";

    @Test void de_som_over_alle_labels_draagt_de_selectie_en_geen_elementtype() {
        String c = emitCompact("de som van zijn vondstgewicht over alle seizoenen.");
        assertTrue(c.contains("\"node\":\"Aggregation\",\"kind\":\"sum\"," + EIGEN_LEZING
                + "\"dimensionSelection\":{\"node\":\"DimensionSelection\",\"dimension\":\"seizoen\","
                + "\"labels\":[\"voorjaar\",\"zomer\",\"najaar\"],\"source\":{\"snippet\":\"overalleseizoenen\"}},"
                + "\"type\":{\"node\":\"Number\""), () -> "de som over alle seizoenen:\n" + c);
        assertFalse(c.contains("\"elementType\""), () -> "een elementtype op de dimensievouw:\n" + c);
        assertFalse(c.contains("\"sourceRole\"") || c.contains("\"sourcePath\""), () -> "een bereik op de dimensievouw:\n" + c);
    }

    @Test void de_som_over_een_interval_draagt_de_labels_in_gedeclareerde_volgorde() {
        String c = emitCompact("de som van zijn vondstgewicht vanaf voorjaar t/m zomer.");
        assertTrue(c.contains("\"kind\":\"sum\"," + EIGEN_LEZING
                + "\"dimensionSelection\":{\"node\":\"DimensionSelection\",\"dimension\":\"seizoen\","
                + "\"labels\":[\"voorjaar\",\"zomer\"],\"source\":{\"snippet\":\"vanafvoorjaart/mzomer\"}},"),
                () -> "de som over een interval:\n" + c);
        assertFalse(c.contains("\"elementType\""), () -> "een elementtype op de dimensievouw:\n" + c);
    }

    /** The grammar admits "en" inside a label, so a set of two labels written without a comma is parsed as ONE
     *  member "voorjaar en najaar"; measured before this: no declared label matched, the selection stayed
     *  unresolved with a WARNING and the emitter refused the fold. */
    @Test void een_verzameling_van_twee_labels_zonder_komma_leest_twee_labels() {
        String c = emitCompact("de maximale waarde van zijn vondstgewicht over de seizoenen in {voorjaar en najaar}.");
        assertTrue(c.contains("\"kind\":\"maximum\"," + EIGEN_LEZING
                + "\"dimensionSelection\":{\"node\":\"DimensionSelection\",\"dimension\":\"seizoen\","
                + "\"labels\":[\"voorjaar\",\"najaar\"],"), () -> "de verzameling van twee labels:\n" + c);
    }

    /** A declared label that contains "en" is read whole, and a member that joins such a label to another reads
     *  as the one way of splitting it into declared labels. */
    @Test void een_label_met_en_blijft_heel() {
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

    @Test void het_maximum_over_een_interval_draagt_de_selectie() {
        String c = emitCompact("de maximale waarde van zijn vondstgewicht vanaf zomer t/m najaar.");
        assertTrue(c.contains("\"kind\":\"maximum\"," + EIGEN_LEZING
                + "\"dimensionSelection\":{\"node\":\"DimensionSelection\",\"dimension\":\"seizoen\","
                + "\"labels\":[\"zomer\",\"najaar\"],"), () -> "het maximum over een interval:\n" + c);
        assertFalse(c.contains("\"elementType\""), () -> "een elementtype op de dimensievouw:\n" + c);
    }
}
