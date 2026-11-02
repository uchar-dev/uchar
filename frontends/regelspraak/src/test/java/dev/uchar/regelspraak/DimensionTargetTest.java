package dev.uchar.regelspraak;

import dev.uchar.regelspraak.emit.IrEmitter;
import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.ir.ResolvedObjectCreation;
import dev.uchar.regelspraak.ir.ResolvedRule;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A rule, an initialisation and an object creation that write one CELL of a dimensioned attribute (IR-SPEC ch. 3.1,
 * ch. 3.3; RUNTIME-SEMANTICS ch. 5.8c). A cell target names its labels between the attribute and its subject ("De
 * premie in voorjaar van een Jutter"), the form ALEF writes a cell in on either side of an assignment, and goes to the
 * wire as a {@code DimensionCellRead} over the attribute; a member of a creation that names a cell carries its
 * {@code labels}. The labels after the subject ("De premie van een Jutter in voorjaar") are refused with the cell
 * named (RGS-308): the parse puts them into the subject text, and without the refusal the sentence wrote the whole
 * attribute. Reading a cell stays what it was.
 */
class DimensionTargetTest {

    static final String MODEL = """
        Dimensie het seizoen, bestaande uit de seizoenen (na het attribuut met voorzetsel in):
            1. voorjaar
            2. najaar

        Objecttype de Jutter (mv: Jutters) (bezield)
            de premie Numeriek (geheel getal) gedimensioneerd met seizoen;
            de bonus Numeriek (geheel getal);

        Objecttype de Vangst (mv: Vangsten)
            de vangstpremie Numeriek (geheel getal) gedimensioneerd met seizoen;

        Feittype vangstboeking
            de boeker Jutter
            de geboekte vangst Vangst
        Eén boeker heeft één geboekte vangst

        """;

    private static String rule(String result) {
        return MODEL + """
            Regel doel
                geldig altijd
                    %s
            """.formatted(result);
    }

    /** The IR without whitespace, so a test can name a node by its keys. */
    private static String compactIr(RegelSpraakModel m) {
        return IrEmitter.emit(m).replaceAll("\\s+", "");
    }

    private static void assertCellTarget(ResolvedRule r, String attribute, String label) {
        Expr.DimensionCellRead cell = assertInstanceOf(Expr.DimensionCellRead.class, r.target());
        assertEquals(attribute, cell.attribute().attribute());
        assertEquals("Jutter", cell.attribute().objectType().name);
        assertEquals(List.of(label), cell.labels());
    }

    @Test void a_cell_target_is_written_as_the_cell() {
        RegelSpraakModel m = Resolver.resolve(rule("De premie in voorjaar van een Jutter moet gesteld worden op 10."));
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        assertCellTarget(m.resolvedRules.get(0), "premie", "voorjaar");
        String ir = compactIr(m);
        assertTrue(ir.contains("\"target\":{\"node\":\"DimensionCellRead\",\"attribute\":{\"node\":\"AttributeReference\","
                + "\"attribute\":\"premie\",\"subjectType\":\"Jutter\""), ir);
        assertTrue(ir.contains("\"labels\":[\"voorjaar\"],\"source\":{\"snippet\":\"Depremieinvoorjaarvaneen"), ir);
    }

    @Test void an_initialisation_writes_a_cell() {
        RegelSpraakModel m = Resolver.resolve(rule("De premie in najaar van een Jutter moet geïnitialiseerd worden op 0."));
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        ResolvedRule r = m.resolvedRules.get(0);
        assertEquals(ResolvedRule.Kind.INITIALISATIE, r.kind());
        assertCellTarget(r, "premie", "najaar");
        assertTrue(compactIr(m).contains("\"labels\":[\"najaar\"]"));
    }

    @Test void a_cell_target_reads_another_cell_of_its_attribute() {
        RegelSpraakModel m = Resolver.resolve(rule(
                "De premie in najaar van een Jutter moet berekend worden als zijn premie in voorjaar maal 2."));
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        assertCellTarget(m.resolvedRules.get(0), "premie", "najaar");
    }

    @Test void a_creation_member_that_names_a_cell_carries_its_labels() {
        RegelSpraakModel m = Resolver.resolve(rule(
                "Een Jutter heeft een geboekte vangst met vangstpremie in najaar gelijk aan zijn premie in najaar."));
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        ResolvedObjectCreation.MemberInit t = m.objectCreations.get(0).initialisations().get(0);
        assertEquals("vangstpremie", t.attribute());
        assertEquals(List.of("najaar"), t.labels());
        assertFalse(t.characteristic());
        assertTrue(compactIr(m).contains("{\"node\":\"MemberInit\",\"member\":\"vangstpremie\",\"labels\":[\"najaar\"],"
                + "\"value\":{\"node\":\"DimensionCellRead\""), compactIr(m));
    }

    @Test void labels_after_the_subject_are_refused_with_the_cell() {
        RegelSpraakModel m = Resolver.resolve(rule("De premie van een Jutter in voorjaar moet gesteld worden op 10."));
        List<Diagnostic> faults = m.errors();
        assertEquals(1, faults.size(), () -> "verwachtte precies één fout, kreeg: " + m.diagnostics);
        assertEquals("RGS-308", faults.get(0).code());
        String t = faults.get(0).message();
        assertEquals("regeldoel 'De premie van een Jutter in voorjaar': het doel is een cel van het gedimensioneerde "
                + "attribuut 'premie' (dimensie seizoen, label 'voorjaar') met de labels achter het onderwerp: een "
                + "celdoel noemt zijn labels direct achter het attribuut, vóór het onderwerp, zoals ALEF een cel "
                + "schrijft; zonder dit hek schreef de zin het hele attribuut (WF-022, IR-SPEC ch. 3.1)", t);
    }

    @Test void reading_a_cell_stays_clean() {
        RegelSpraakModel m = Resolver.resolve(rule(
                "De bonus van een Jutter moet gesteld worden op zijn premie in voorjaar."));
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
    }
}
