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

        Dimensie het strand, bestaande uit de stranden (na het attribuut met voorzetsel op):
            1. noord
            2. zuid

        Dimensie de herkomst, bestaande uit de herkomsten (na het attribuut met voorzetsel uit):
            1. zee
            2. rivier

        Objecttype de Jutter (mv: Jutters) (bezield)
            de premie Numeriek (geheel getal) gedimensioneerd met seizoen;
            de bonus Numeriek (geheel getal);
            de strandpremie Numeriek (geheel getal) gedimensioneerd met seizoen en strand;
            de herkomstpremie Numeriek (geheel getal) gedimensioneerd met herkomst;
            de zeepremie Numeriek (geheel getal) gedimensioneerd met strand en herkomst;

        Objecttype de Vangst (mv: Vangsten)
            de vangstpremie Numeriek (geheel getal) gedimensioneerd met seizoen;
            de strandvangst Numeriek (geheel getal) gedimensioneerd met seizoen en strand;
            de zeevangst Numeriek (geheel getal) gedimensioneerd met strand en herkomst;

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

    private static void assertCellTarget(ResolvedRule r, String attribute, String... labels) {
        Expr.DimensionCellRead cell = assertInstanceOf(Expr.DimensionCellRead.class, r.target());
        assertEquals(attribute, cell.attribute().attribute());
        assertEquals("Jutter", cell.attribute().objectType().name);
        assertEquals(List.of(labels), cell.labels());
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
        ResolvedObjectCreation.MemberAssignment t = m.objectCreations.get(0).assignments().get(0);
        assertEquals("vangstpremie", t.attribute());
        assertEquals(List.of("najaar"), t.labels());
        assertFalse(t.characteristic());
        assertTrue(compactIr(m).contains("{\"node\":\"MemberAssignment\",\"member\":\"vangstpremie\",\"labels\":[\"najaar\"],"
                + "\"value\":{\"node\":\"DimensionCellRead\""), compactIr(m));
    }

    /** A cell target over two dimensions with its labels joined by "en", as ALEF writes it; the labels go out in the
     *  declared order of the dimensions whatever order the target writes them in. */
    @Test void a_cell_target_joins_its_labels_with_en() {
        RegelSpraakModel m = Resolver.resolve(rule(
                "De strandpremie op zuid en in najaar van een Jutter moet berekend worden als zijn premie in najaar."));
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        assertCellTarget(m.resolvedRules.get(0), "strandpremie", "najaar", "zuid");
        assertTrue(compactIr(m).contains("\"labels\":[\"najaar\",\"zuid\"],\"source\":{\"snippet\":"
                + "\"Destrandpremieopzuideninnajaarvaneen"), compactIr(m));
    }

    /** The same target with its labels side by side, the form a cell read takes too. */
    @Test void a_cell_target_over_two_dimensions_names_a_label_of_each() {
        RegelSpraakModel m = Resolver.resolve(rule("De strandpremie in voorjaar op noord van een Jutter moet gesteld worden op 4."));
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        assertCellTarget(m.resolvedRules.get(0), "strandpremie", "voorjaar", "noord");
    }

    /** A target that names a label of one of two dimensions is refused as a cell read is, naming the dimension. */
    @Test void a_cell_target_without_a_label_of_every_dimension_is_refused() {
        RegelSpraakModel m = Resolver.resolve(rule("De strandpremie in voorjaar van een Jutter moet gesteld worden op 4."));
        DimensionCellTest.assertOneError(m, "RGS-364", java.util.Map.of("written", "De strandpremie in voorjaar van een Jutter",
                "attribute", "strandpremie", "dimension", "strand"));
    }

    @Test void a_creation_member_joins_its_labels_with_en() {
        RegelSpraakModel m = Resolver.resolve(rule(
                "Een Jutter heeft een geboekte vangst met strandvangst in najaar en op zuid gelijk aan zijn premie in najaar."));
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        ResolvedObjectCreation.MemberAssignment t = m.objectCreations.get(0).assignments().get(0);
        assertEquals("strandvangst", t.attribute());
        assertEquals(List.of("najaar", "zuid"), t.labels());
        assertTrue(compactIr(m).contains("{\"node\":\"MemberAssignment\",\"member\":\"strandvangst\",\"labels\":[\"najaar\",\"zuid\"],"),
                compactIr(m));
    }

    /** A dimension declared with "uit" (§3.6): its label stands after "uit", in a target as in a cell read. */
    @Test void a_cell_target_names_a_label_after_uit() {
        RegelSpraakModel m = Resolver.resolve(rule("De zeepremie uit zee op noord van een Jutter moet gesteld worden op 4."));
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        assertCellTarget(m.resolvedRules.get(0), "zeepremie", "noord", "zee");
    }

    @Test void a_cell_target_joins_a_label_after_uit_with_en() {
        RegelSpraakModel m = Resolver.resolve(rule(
                "De zeepremie op zuid en uit rivier van een Jutter moet berekend worden als zijn zeepremie uit zee en op zuid."));
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        assertCellTarget(m.resolvedRules.get(0), "zeepremie", "zuid", "rivier");
        assertTrue(compactIr(m).contains("\"labels\":[\"zuid\",\"rivier\"],\"source\":{\"snippet\":"
                + "\"Dezeepremieopzuidenuitriviervaneen"), compactIr(m));
        assertTrue(compactIr(m).contains("\"snippet\":\"zijnzeepremieuitzeeenopzuid\"}},\"labels\":[\"zuid\",\"zee\"]"),
                compactIr(m));
    }

    @Test void a_creation_member_names_a_label_after_uit() {
        RegelSpraakModel m = Resolver.resolve(rule(
                "Een Jutter heeft een geboekte vangst met zeevangst uit rivier en op noord gelijk aan zijn premie in najaar."));
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        ResolvedObjectCreation.MemberAssignment t = m.objectCreations.get(0).assignments().get(0);
        assertEquals("zeevangst", t.attribute());
        assertEquals(List.of("noord", "rivier"), t.labels());
    }

    @Test void a_label_after_uit_behind_the_subject_is_refused_with_the_cell() {
        RegelSpraakModel m = Resolver.resolve(rule("De herkomstpremie van een Jutter uit zee moet gesteld worden op 10."));
        List<Diagnostic> faults = m.errors();
        assertEquals(1, faults.size(), () -> "verwachtte precies één fout, kreeg: " + m.diagnostics);
        assertEquals("RGS-308", faults.get(0).code());
        assertEquals("regeldoel 'De herkomstpremie van een Jutter uit zee': het doel is een cel van het gedimensioneerde "
                + "attribuut 'herkomstpremie' (dimensie herkomst, label 'zee') met de labels achter het onderwerp: een "
                + "celdoel noemt zijn labels direct achter het attribuut, vóór het onderwerp, zoals ALEF een cel "
                + "schrijft; zonder dit hek schreef de zin het hele attribuut (WF-022, IR-SPEC ch. 3.1)",
                faults.get(0).message());
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

    /** A model with a to-one and a to-many role from the Jutter, for the reads a cell target's value makes over them. */
    static final String ROLES = """
        Dimensie het seizoen, bestaande uit de seizoenen (na het attribuut met voorzetsel in):
            1. voorjaar
            2. najaar

        Objecttype de Jutter (mv: Jutters) (bezield)
            de premie Numeriek (geheel getal) gedimensioneerd met seizoen;

        Objecttype de Vangst (mv: Vangsten)
            het gewicht Numeriek (geheel getal);

        Objecttype de Haven (mv: Havens)
            het havengeld Numeriek (geheel getal);

        Feittype vangstboeking
            de boeker Jutter
            de geboekte vangst (mv: geboekte vangsten) Vangst
        Eén boeker heeft meerdere geboekte vangsten

        Feittype thuishaven van jutter
            de thuishaven Haven
            de bewoner Jutter
        Eén thuishaven heeft meerdere bewoners

        Regel doel
            geldig altijd
                %s
        """;

    /** The subject of a rule with a cell target is the type of the attribute under the cell, as the emitter writes
     *  it: a navigated read in its value binds from there. */
    @Test void a_cell_target_reads_over_a_role() {
        RegelSpraakModel m = Resolver.resolve(ROLES.formatted(
                "De premie in voorjaar van een Jutter moet berekend worden als het havengeld van zijn thuishaven."));
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        assertTrue(compactIr(m).contains("\"path\":[{\"node\":\"RoleBinding\",\"factType\":\"thuishavenvanjutter\","
                + "\"role\":\"thuishaven\"}]"), compactIr(m));
    }

    @Test void a_cell_target_folds_over_a_role() {
        RegelSpraakModel m = Resolver.resolve(ROLES.formatted("De premie in voorjaar van een Jutter moet berekend "
                + "worden als de som van het gewicht van alle geboekte vangsten van de Jutter."));
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        assertTrue(compactIr(m).contains("\"sourceRole\":{\"node\":\"RoleBinding\",\"factType\":\"vangstboeking\","
                + "\"role\":\"geboektevangst\"}"), compactIr(m));
    }

    /** A set reading outside a fold under a cell target is refused in the sentence of the read the emitter writes
     *  from the cell's subject: the to-many hop (RGS-165). */
    @Test void a_set_reading_under_a_cell_target_names_its_to_many_hop() {
        RegelSpraakModel m = Resolver.resolve(ROLES.formatted("De premie in voorjaar van een Jutter moet berekend "
                + "worden als het gewicht van alle geboekte vangsten van de Jutter plus 1."));
        List<Diagnostic> faults = m.errors();
        assertEquals(1, faults.size(), () -> "verwachtte precies één fout, kreeg: " + m.diagnostics);
        assertEquals("RGS-165", faults.get(0).code(), faults.get(0).message());
    }

    /** A target that names a dimensioned attribute without any label writes the whole attribute, which has a value per
     *  cell and none of its own (IR-SPEC WF-125 (b)): refused as a cell that misses its labels is, once per dimension.
     *  Before, the rule went to disk without a diagnostic and the verifier refused it (WF-125.whole-write). */
    @Test void a_target_that_writes_the_whole_dimensioned_attribute_is_refused() {
        String source = rule("De premie van een Jutter moet gesteld worden op 10.");
        RegelSpraakModel m = Resolver.resolve(source);
        DimensionCellTest.assertOneError(m, "RGS-364", java.util.Map.of("written", "De premie van een Jutter",
                "attribute", "premie", "dimension", "seizoen"));
        assertEquals(List.of(source.split("\n", -1)).indexOf("        De premie van een Jutter moet gesteld worden op 10.") + 1,
                m.diagnostics.get(0).line());
    }

    @Test void a_whole_target_over_two_dimensions_is_refused_per_dimension() {
        RegelSpraakModel m = Resolver.resolve(rule("De strandpremie van een Jutter moet berekend worden als zijn bonus."));
        assertEquals(List.of("RGS-364 seizoen", "RGS-364 strand"),
                m.diagnostics.stream().map(d -> d.code() + " " + d.args().get("dimension")).toList());
        assertTrue(m.diagnostics.stream().allMatch(d -> d.severity() == RegelSpraakModel.Severity.ERROR), m.diagnostics::toString);
    }
}
