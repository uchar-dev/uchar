package dev.uchar.regelspraak;

import dev.uchar.regelspraak.emit.IrEmitter;
import dev.uchar.regelspraak.ir.Condition;
import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.ir.ResolvedRule;
import dev.uchar.regelspraak.model.Datatype;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.model.RegelSpraakModel.Severity;
import dev.uchar.regelspraak.resolver.Resolver;
import dev.uchar.regelspraak.resolver.SemanticTokens;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The absence markers of the dialect (grammar/DIVERGENCES.md, "Afwezigheidsmarkeringen"; RUNTIME-SEMANTICS ch. 9.5):
 * "(of onbekend)", "(of niet van toepassing)" and "(of onbekend of niet van toepassing)" at the end of an attribute
 * declaration lower the attribute to a sum type of its own; a marker written is a Construct of it, a marker tested an
 * equality with it, "een waarde heeft" a gevuld test of the value, and every read of the value a PayloadAccess under
 * that test. A read of the value without the test is RGS-376, the frontend's half of WF-127. Measured on the frontend
 * before the markers: the declaration was a syntax error at "(of", the value "onbekend" an unresolved name and each test
 * an unresolved characteristic check, each with a WARNING.
 */
class AbsenceMarkerTest {

    /** The source of the golden corpus/golden/jutterswet-afwezigheidsmarkers; gate 10 holds its ir.json to the
     *  emission of it. */
    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden", "jutterswet-afwezigheidsmarkers", "source.rgs");

    /** The marking is one keyword span of its own; that no word of the golden stays unresolved is
     *  SemanticTokensCorpusTest's, over every golden source. */
    @Test void the_words_of_the_markers_are_keywords() throws Exception {
        var files = List.of(new SemanticTokens.SourceFile("golden.rgs", Files.readString(GOLDEN, StandardCharsets.UTF_8)));
        SemanticTokens.Document d = Resolver.tokens(files);
        assertTrue(d.files().get(0).spans().stream().anyMatch(s -> s.text().equals("(of onbekend of niet van toepassing)")
                && s.kind() == SemanticTokens.Kind.KEYWORD), "de markering is een sleutelwoord");
    }

    private static ResolvedRule rule(RegelSpraakModel m, String name) {
        return m.resolvedRules.stream().filter(r -> r.name().equals(name)).findFirst()
                .orElseThrow(() -> new AssertionError("geen regel '" + name + "'"));
    }

    private static Expr.PayloadAccess firstRead(Expr e) {
        List<Expr.PayloadAccess> found = new java.util.ArrayList<>();
        Expr.contains(e, x -> x instanceof Expr.PayloadAccess p && found.add(p));
        assertFalse(found.isEmpty(), () -> "geen PayloadAccess in " + e);
        return found.get(0);
    }

    /** The model holds the nodes the emitter writes: a value written is a Construct of the value constructor, a marker
     *  written or tested a Construct of the marker, and a read of the value a PayloadAccess typed with the declared
     *  datatype over a reference typed with the sum type of the attribute, in a computation, a value test and a fold. */
    @Test void the_model_carries_the_nodes_of_the_sum_type() throws Exception {
        RegelSpraakModel m = Resolver.resolve(Files.readString(GOLDEN, StandardCharsets.UTF_8));
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        RegelSpraakModel.ObjectType vondst = m.objectTypes.get("Vondst");
        Datatype payload = vondst.attribute("taxatie");

        Expr.Construct written = assertInstanceOf(Expr.Construct.class, rule(m, "taxatie uit schatting").value());
        assertEquals("waarde", written.constructor());
        assertEquals("taxatie van een Vondst", written.type().domain());
        assertEquals(List.of("waarde", "onbekend", "niet van toepassing"),
                written.type().constructors().stream().map(Datatype.Constructor::name).toList());
        assertEquals(payload, written.type().constructors().get(0).payload());
        assertEquals("schatting", assertInstanceOf(Expr.AttributeReference.class, written.argument()).attribute());

        Expr.Construct marker = assertInstanceOf(Expr.Construct.class, rule(m, "taxatie onbekend").value());
        assertEquals("onbekend", marker.constructor());
        assertNull(marker.argument());
        assertEquals(written.type(), marker.type());

        ResolvedRule fee = rule(m, "vindloon naar taxatie");
        Expr.PayloadAccess read = firstRead(fee.value());
        assertEquals("waarde", read.constructor());
        assertEquals(payload, read.type());
        Expr.AttributeReference base = assertInstanceOf(Expr.AttributeReference.class, read.base());
        assertEquals(written.type(), base.type());
        Condition.Predicate test = assertInstanceOf(Condition.Predicate.class, fee.condition());
        assertEquals(Condition.PredicateKind.GEVULD, test.kind());
        assertEquals(written.type(), assertInstanceOf(Expr.PayloadAccess.class, test.operand()).base().type());

        Condition.Comparison tested = assertInstanceOf(Condition.Comparison.class, rule(m, "forfaitair vindloon").condition());
        assertEquals(written.type(), assertInstanceOf(Expr.AttributeReference.class, tested.left()).type());
        assertEquals("onbekend", assertInstanceOf(Expr.Construct.class, tested.right()).constructor());

        Expr.FunctionCall sum = assertInstanceOf(Expr.FunctionCall.class, rule(m, "getaxeerd totaal").value());
        assertInstanceOf(Expr.PayloadAccess.class, sum.arguments().get(0));
        assertInstanceOf(Expr.PayloadAccess.class, assertInstanceOf(Condition.Predicate.class, sum.filter()).operand());

        Expr.AttributeReference target = assertInstanceOf(Expr.AttributeReference.class, rule(m, "opslag onbekend in de winter").target());
        assertEquals("opslagkosten van een Vondst", assertInstanceOf(Datatype.SumType.class, target.type()).domain());
    }

    /** A fold over the values of the members reads each value through the fold's range: the Aggregation carries the
     *  relation, and the reference under the PayloadAccess carries no path of its own (IR-SPEC ch. 4.3). */
    @Test void a_fold_over_the_values_reads_through_its_range() throws Exception {
        String ir = IrEmitter.emit(Resolver.resolve(Files.readString(GOLDEN, StandardCharsets.UTF_8))).replaceAll("\\s+", "");
        int at = ir.indexOf("\"kind\":\"sum\"");
        assertTrue(at >= 0, ir);
        String fold = ir.substring(at, ir.indexOf("\"presentation\"", at));
        assertTrue(fold.contains("\"arguments\":[{\"node\":\"PayloadAccess\""), fold);
        assertTrue(fold.contains("\"sourceRole\":{\"node\":\"RoleBinding\",\"factType\":\"vangst\",\"role\":\"vondst\"}"), fold);
        assertFalse(fold.contains("\"path\""), fold);
    }

    private static final String VONDST = """
        Objecttype de Vondst (mv: Vondsten)
            de taxatie Numeriek (getal met 2 decimalen) (of niet van toepassing);
            het vindloon Numeriek (getal met 2 decimalen);
            is drijfhout kenmerk (bijvoeglijk);
        """;

    /** One marker: the sum type carries the value and that marker, and its domain is the attribute's own. */
    @Test void one_marker_is_one_constructor_beside_the_value() {
        String ir = IrEmitter.emit(Resolver.resolve(VONDST + """

            Regel geen taxatie
                geldig altijd
                    De taxatie van een Vondst moet gesteld worden op niet van toepassing
                    indien hij drijfhout is.
            """));
        String compact = ir.replaceAll("\\s+", "");
        assertTrue(compact.contains("{\"node\":\"Domain\",\"name\":\"taxatievaneenVondst\",\"datatype\":{\"node\":\"SumType\","
                + "\"constructors\":[{\"name\":\"waarde\",\"payload\":{\"node\":\"Number\",\"whole\":false,\"decimals\":2,"
                + "\"sign\":\"unspecified\"}},{\"name\":\"nietvantoepassing\",\"payload\":null}]}}"), ir);
        assertTrue(compact.contains("\"value\":{\"node\":\"Construct\""), ir);
        assertFalse(compact.contains("onbekend"), ir);
    }

    private static Diagnostic only(RegelSpraakModel m, String code) {
        List<Diagnostic> found = m.diagnostics.stream().filter(d -> code.equals(d.code())).toList();
        assertEquals(1, found.size(), () -> "verwachtte één " + code + ", kreeg: " + m.diagnostics);
        return found.get(0);
    }

    /** A read of the value in a computation without the test is refused, with the attribute, its type and the read. */
    @Test void a_read_without_the_test_is_refused() {
        RegelSpraakModel m = Resolver.resolve(VONDST + """

            Regel vindloon
                geldig altijd
                    Het vindloon van een Vondst moet berekend worden als zijn taxatie gedeeld door 10 rekenkundig afgerond op 2 decimalen.
            """);
        Diagnostic d = only(m, "RGS-376");
        assertEquals(Severity.ERROR, d.severity());
        assertEquals("taxatie", d.args().get("attribute"));
        assertEquals("Vondst", d.args().get("objectType"));
        assertEquals("zijn taxatie", String.valueOf(d.args().get("expression")));
        assertEquals(8, d.line());
    }

    /** A read of a cell of a dimensioned attribute with markers is a read of its value, a PayloadAccess over the cell;
     *  without a test of that cell the read is refused. */
    @Test void a_read_of_a_cell_is_refused() {
        RegelSpraakModel m = Resolver.resolve("""
            Dimensie het seizoen, bestaande uit de seizoenen (na het attribuut met voorzetsel in):
                1. voorjaar
                2. najaar

            Objecttype de Vondst (mv: Vondsten)
                het gewicht Numeriek (getal met 2 decimalen) gedimensioneerd met seizoen (of onbekend);
                het vindloon Numeriek (getal met 2 decimalen);

            Regel vindloon
                geldig altijd
                    Het vindloon van een Vondst moet berekend worden als zijn gewicht in najaar plus 1.
            """);
        Diagnostic d = only(m, "RGS-376");
        assertEquals("gewicht", d.args().get("attribute"));
        assertEquals("zijn gewicht in najaar", String.valueOf(d.args().get("expression")));
        Expr.PayloadAccess read = firstRead(rule(m, "vindloon").value());
        assertInstanceOf(Expr.DimensionCellRead.class, read.base());
    }

    private static final String CELLS = """
        Dimensie het seizoen, bestaande uit de seizoenen (na het attribuut met voorzetsel in):
            1. voorjaar
            2. najaar

        Objecttype de Vondst (mv: Vondsten)
            het gewicht Numeriek (getal met 2 decimalen) gedimensioneerd met seizoen (of onbekend);
            het vindloon Numeriek (getal met 2 decimalen);
            is onleesbaar kenmerk (bijvoeglijk);
        """;

    /** A marker written into a cell is the Construct of the marker on the cell target, and the tests of a cell are those
     *  of the attribute over the cell: "een waarde heeft" a gevuld test of the PayloadAccess of the cell, "onbekend is"
     *  an equality of the cell with the Construct of the marker; a read of the cell after the test of that cell stands
     *  under it. */
    @Test void a_marker_is_written_and_tested_on_a_cell() {
        RegelSpraakModel m = Resolver.resolve(CELLS + """

            Regel gewicht onbekend
                geldig altijd
                    Het gewicht in voorjaar van een Vondst moet gesteld worden op onbekend
                    indien hij onleesbaar is.

            Regel vindloon
                geldig altijd
                    Het vindloon van een Vondst moet berekend worden als zijn gewicht in najaar plus 1
                    indien zijn gewicht in najaar een waarde heeft.

            Regel vast vindloon
                geldig altijd
                    Het vindloon van een Vondst moet gesteld worden op 5,00
                    indien zijn gewicht in voorjaar onbekend is.
            """);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        ResolvedRule marked = rule(m, "gewicht onbekend");
        assertEquals(List.of("voorjaar"), assertInstanceOf(Expr.DimensionCellRead.class, marked.target()).labels());
        Expr.Construct marker = assertInstanceOf(Expr.Construct.class, marked.value());
        assertEquals("onbekend", marker.constructor());
        assertEquals("gewicht van een Vondst", marker.type().domain());

        Condition.Predicate test = assertInstanceOf(Condition.Predicate.class, rule(m, "vindloon").condition());
        assertEquals(Condition.PredicateKind.GEVULD, test.kind());
        Expr.PayloadAccess tested = assertInstanceOf(Expr.PayloadAccess.class, test.operand());
        assertEquals(List.of("najaar"), assertInstanceOf(Expr.DimensionCellRead.class, tested.base()).labels());
        assertInstanceOf(Expr.DimensionCellRead.class, firstRead(rule(m, "vindloon").value()).base());

        Condition.Comparison equal = assertInstanceOf(Condition.Comparison.class, rule(m, "vast vindloon").condition());
        assertEquals(List.of("voorjaar"), assertInstanceOf(Expr.DimensionCellRead.class, equal.left()).labels());
        assertEquals("onbekend", assertInstanceOf(Expr.Construct.class, equal.right()).constructor());
    }

    /** The test of one cell guards the read of that cell and of no other: a read of the cell in najaar after the test of
     *  the cell in voorjaar is refused. */
    @Test void a_read_of_a_cell_after_the_test_of_another_cell_is_refused() {
        RegelSpraakModel m = Resolver.resolve(CELLS + """

            Regel vindloon
                geldig altijd
                    Het vindloon van een Vondst moet berekend worden als zijn gewicht in najaar plus 1
                    indien zijn gewicht in voorjaar een waarde heeft.
            """);
        assertEquals("zijn gewicht in najaar", String.valueOf(only(m, "RGS-376").args().get("expression")));
    }

    /** An object creation writes a marker into a member with markers as a rule writes it, the Construct of the marker,
     *  into an attribute and into a cell; the typing of a member's value has nothing to judge in a marker. */
    @Test void a_creation_writes_a_marker_as_a_construct() {
        RegelSpraakModel m = Resolver.resolve(CELLS + """
                de taxatie Numeriek (getal met 2 decimalen) (of onbekend);

            Objecttype de Jutter (mv: Jutters)
                is actief kenmerk (bijvoeglijk);

            Feittype vangst
                de jutter Jutter
                de vondst Vondst
                één jutter heeft meerdere vondsten

            Regel nieuwe vondst
                geldig altijd
                    Een Jutter heeft een vondst met taxatie gelijk aan onbekend, gewicht in najaar gelijk aan onbekend
                    indien hij actief is.
            """);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        var inits = m.objectCreations.get(0).initialisations();
        Expr.Construct attribute = assertInstanceOf(Expr.Construct.class, inits.get(0).value());
        assertEquals("onbekend", attribute.constructor());
        assertEquals("taxatie van een Vondst", attribute.type().domain());
        Expr.Construct cell = assertInstanceOf(Expr.Construct.class, inits.get(1).value());
        assertEquals("onbekend", cell.constructor());
        assertEquals("gewicht van een Vondst", cell.type().domain());
        assertEquals(List.of("najaar"), inits.get(1).labels());
    }

    /** An object creation writes a value into an attribute with markers as a rule does: the payload of a Construct of
     *  the value constructor (the verifier refuses a number in a sum-typed member, WF-023). */
    @Test void a_creation_writes_a_value_as_a_construct() {
        RegelSpraakModel m = Resolver.resolve("""
            Objecttype de Jutter (mv: Jutters)
                is actief kenmerk (bijvoeglijk);

            Objecttype de Vondst (mv: Vondsten)
                de taxatie Numeriek (getal met 2 decimalen) (of onbekend);

            Feittype vangst
                de jutter Jutter
                de vondst Vondst
                één jutter heeft meerdere vondsten

            Regel nieuwe vondst
                geldig altijd
                    Een Jutter heeft een vondst met taxatie gelijk aan 5,00
                    indien hij actief is.
            """);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        Expr.Construct written = assertInstanceOf(Expr.Construct.class,
                m.objectCreations.get(0).initialisations().get(0).value());
        assertEquals("waarde", written.constructor());
        assertEquals("5,00", assertInstanceOf(Expr.Literal.class, written.argument()).source().snippet());
    }

    /** A comparison before the test in an `alle` is read before the test (RUNTIME-SEMANTICS ch. 5.10), so it is refused;
     *  the test before the comparison is the golden's. */
    @Test void a_comparison_before_the_test_is_refused() {
        RegelSpraakModel m = Resolver.resolve(VONDST + """
                is kostbaar kenmerk (bijvoeglijk);

            Regel kostbaar
                geldig altijd
                    Een Vondst is kostbaar
                    indien hij aan alle volgende voorwaarden voldoet:
                    - zijn taxatie is groter dan 1000,00
                    - zijn taxatie heeft een waarde.
            """);
        assertEquals("taxatie", only(m, "RGS-376").args().get("attribute"));
    }

    /** A sum over members reads each member's value, so it needs the test per member: "die een waarde hebben". */
    @Test void a_sum_without_the_subselection_is_refused() {
        RegelSpraakModel m = Resolver.resolve(VONDST + """

            Objecttype de Jutter (mv: Jutters)
                het getaxeerd totaal Numeriek (getal met 2 decimalen);

            Feittype vangst
                de jutter Jutter
                de vondst Vondst
                één jutter heeft meerdere vondsten

            Regel totaal
                geldig altijd
                    Het getaxeerd totaal van een Jutter moet berekend worden als de som van de taxatie van alle vondsten van de Jutter.
            """);
        assertEquals("taxatie", only(m, "RGS-376").args().get("attribute"));
    }

    /** A test of the attribute itself reads no value: "gevuld is" holds on a marker, which is a filled value. */
    @Test void a_filled_test_of_the_attribute_needs_no_test() {
        RegelSpraakModel m = Resolver.resolve(VONDST + """

            Regel vindloon
                geldig altijd
                    Het vindloon van een Vondst moet gesteld worden op 5,00
                    indien zijn taxatie gevuld is.
            """);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        assertFalse(IrEmitter.emit(m).contains("PayloadAccess"));
    }

    /** A marker the attribute does not declare stays the unresolved name it is without markers, written as a value. */
    @Test void a_marker_the_attribute_does_not_declare_stays_unresolved() {
        RegelSpraakModel m = Resolver.resolve(VONDST + """

            Regel onbekende taxatie
                geldig altijd
                    De taxatie van een Vondst moet gesteld worden op onbekend
                    indien hij drijfhout is.
            """);
        assertEquals("RGS-092", m.diagnostics.get(0).code(), () -> "" + m.diagnostics);
        Expr.Construct written = assertInstanceOf(Expr.Construct.class, rule(m, "onbekende taxatie").value());
        assertEquals("waarde", written.constructor());
        assertInstanceOf(Expr.Unresolved.class, written.argument());
    }

    /** The markers stand on an attribute, in the place of the opgave marker and not beside it. */
    @Test void a_marker_on_a_characteristic_or_beside_the_opgave_marker_is_a_syntax_error() {
        RegelSpraakModel k = Resolver.resolve("""
            Objecttype de Vondst (mv: Vondsten)
                is drijfhout kenmerk (bijvoeglijk) (of onbekend);
            """);
        assertEquals("RGS-091", k.errors().get(0).code(), () -> "" + k.diagnostics);
        RegelSpraakModel o = Resolver.resolve("""
            Objecttype de Vondst (mv: Vondsten)
                de taxatie Numeriek (getal met 2 decimalen) (of onbekend) (mag opgegeven worden);
            """);
        assertEquals("RGS-091", o.errors().get(0).code(), () -> "" + o.diagnostics);
    }

    /** Without the markers nothing changes: a characteristic named onbekend is checked as before, and the words lex as
     *  names. */
    @Test void without_markers_the_words_are_names() {
        RegelSpraakModel m = Resolver.resolve("""
            Objecttype de Vondst (mv: Vondsten)
                de waarde Numeriek (getal met 2 decimalen);
                is onbekend kenmerk (bijvoeglijk);

            Regel waarde
                geldig altijd
                    De waarde van een Vondst moet gesteld worden op 5,00
                    indien hij onbekend is.
            """);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        String ir = IrEmitter.emit(m);
        assertTrue(ir.contains("\"node\": \"CharacteristicCheck\""), ir);
        assertFalse(ir.contains("SumType"), ir);
    }
}
