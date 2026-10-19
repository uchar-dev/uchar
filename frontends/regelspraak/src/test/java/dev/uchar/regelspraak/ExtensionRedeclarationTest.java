package dev.uchar.regelspraak;

import dev.uchar.regelspraak.emit.IrEmitter;
import dev.uchar.regelspraak.model.Datatype;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.model.RegelSpraakModel.ObjectType;
import dev.uchar.regelspraak.model.RegelSpraakModel.Severity;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A member an {@code Extensie van objecttype} declares again (grammar/DIVERGENCES.md, §5 on the extension and §7).
 * An extension adds members to a type and replaces none: a member the type or its inheritance line already carries
 * may be repeated literally, and the repeat may add the opgave marker once; every other repeat is an ERROR on the
 * repeat's line that names both lines. Measured on the frontend before this work: the second declaration replaced the
 * first without a diagnostic, also with another datatype; a repeated characteristic went to the wire twice; a repeat
 * that carried a designation of the attribute was refused as two designations; and a role written in an extension was
 * refused as an undeclared domain on line 0. The literal repeat of an attribute, the marker-only repeat and the role
 * name on an attribute with a datatype of its own were admitted there and stay admitted.
 */
class ExtensionRedeclarationTest {

    /** The type's own declarations: the attribute on line 3, the characteristic on line 4. */
    private static final String VONDST = """
        Objecttype de Vondst (mv: Vondsten)
            de vinddag Datum in dagen;
            de waarde Numeriek (geheel getal);
            is waardevol kenmerk (bijvoeglijk);
        """;

    /** An extension of the type whose one member stands on line 7. */
    private static String extension(String member) {
        return VONDST + """

            Extensie van objecttype de Vondst
                %s
            """.formatted(member);
    }

    private static final String TAIL = " — een extensie voegt leden toe en vervangt er geen: een lid dat het "
            + "objecttype al draagt, herhaalt zij alleen letterlijk, en '(mag opgegeven worden)' komt er hooguit één "
            + "keer bij (WF-002, dialect §7)";

    /** Exactly one diagnostic, an ERROR with {@code sentence} whole, on {@code rule}. */
    private static void onlyError(RegelSpraakModel m, int rule, String sentence) {
        assertEquals(1, m.diagnostics.size(), () -> "verwachtte precies één diagnose, kreeg: " + m.diagnostics);
        Diagnostic d = m.diagnostics.get(0);
        assertEquals(Severity.ERROR, d.severity(), () -> "verwachtte een ERROR: " + d);
        assertEquals(sentence, d.message());
        assertEquals(rule, d.line(), () -> "de melding staat op de verkeerde regel: " + d);
    }

    private static RegelSpraakModel clean(String source) {
        RegelSpraakModel m = Resolver.resolve(source);
        assertTrue(m.diagnostics.isEmpty(), () -> "verwachtte geen diagnose, kreeg: " + m.diagnostics);
        return m;
    }

    // -------------------------------------------------------------------- a repeat that replaces: refused

    @Test void another_datatype_is_refused_on_the_line_of_the_redeclaration() {
        RegelSpraakModel m = Resolver.resolve(extension("de waarde Tekst;"));
        onlyError(m, 7, "objecttype 'Vondst': de extensie declareert het attribuut 'waarde' (bronregel 7), dat Vondst "
                + "al draagt (bronregel 3), en 'de waarde Tekst' is niet letterlijk 'de waarde Numeriek (geheel getal)'"
                + TAIL);
        // the declaration the type carried stays, so what the rest of the document reads is the fundament's
        assertInstanceOf(Datatype.Numeric.class, m.objectTypes.get("Vondst").attributes.get("waarde"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "de waarde Numeriek (geheel getal) voor elk jaar",
            "de waarde Numeriek (niet-negatief geheel getal)",
            "het waarde Numeriek (geheel getal)",
            "de waarde (mv: waarden) Numeriek (geheel getal)",
            "de waarde Numeriek (geheel getal) voor elk jaar (mag opgegeven worden)"})
    void the_same_datatype_with_another_difference_refuses(String member) {
        RegelSpraakModel m = Resolver.resolve(extension(member + ";"));
        onlyError(m, 7, "objecttype 'Vondst': de extensie declareert het attribuut 'waarde' (bronregel 7), dat Vondst "
                + "al draagt (bronregel 3), en '" + member + "' is niet letterlijk 'de waarde Numeriek (geheel getal)'"
                + TAIL);
    }

    @Test void a_characteristic_of_another_kind_is_refused() {
        RegelSpraakModel m = Resolver.resolve(extension("het waardevol kenmerk (bezittelijk);"));
        onlyError(m, 7, "objecttype 'Vondst': de extensie declareert het kenmerk 'waardevol' (bronregel 7), dat Vondst "
                + "al draagt (bronregel 4), en 'het waardevol kenmerk (bezittelijk)' is niet letterlijk 'is waardevol "
                + "kenmerk (bijvoeglijk)'" + TAIL);
        assertEquals(ObjectType.CharacteristicKind.BIJVOEGLIJK, m.objectTypes.get("Vondst").characteristicKind("waardevol"));
    }

    @Test void an_extension_for_the_head_is_also_refused() {
        // The files of a chain can come in any order; the head is what the type carries, the extension is the repeat.
        RegelSpraakModel m = Resolver.resolve("""
            Extensie van objecttype de Vondst
                de waarde Tekst;

            """ + VONDST);
        onlyError(m, 2, "objecttype 'Vondst': de extensie declareert het attribuut 'waarde' (bronregel 2), dat Vondst "
                + "al draagt (bronregel 6), en 'de waarde Tekst' is niet letterlijk 'de waarde Numeriek (geheel getal)'"
                + TAIL);
        assertInstanceOf(Datatype.Numeric.class, m.objectTypes.get("Vondst").attributes.get("waarde"));
    }

    @Test void two_extensions_with_a_difference_refuse_the_second() {
        RegelSpraakModel m = Resolver.resolve("""
            Extensie van objecttype de Vondst
                de waarde Numeriek (geheel getal);

            Extensie van objecttype de Vondst
                de waarde Tekst;
            """);
        onlyError(m, 5, "objecttype 'Vondst': de extensie declareert het attribuut 'waarde' (bronregel 5), dat Vondst "
                + "al draagt (bronregel 2), en 'de waarde Tekst' is niet letterlijk 'de waarde Numeriek (geheel getal)'"
                + TAIL);
    }

    // -------------------------------------------------------------------- a role written in an extension

    private static final String VINDER = """
        Objecttype de Vondst (mv: Vondsten)
            de waarde Numeriek (geheel getal);

        Objecttype de Jutter (mv: Jutters) (bezield)
            de leeftijd Numeriek (geheel getal);

        Feittype vondst van jutter
            de vinder Jutter
            de vondst Vondst
        Eén vinder heeft meerdere vondsten

        Extensie van objecttype de Vondst
        """;

    @Test void a_role_in_an_extension_is_refused_on_its_line() {
        // The grammar reads the line as an attribute whose type is a domain; the domain is an object type, and the
        // name is a role of a fact type the Vondst takes part in.
        RegelSpraakModel m = Resolver.resolve(VINDER + "    de vinder Jutter;\n");
        onlyError(m, 13, "objecttype 'Vondst': de extensie declareert 'vinder' met het objecttype 'Jutter' (bronregel "
                + "13), en dat is de rol 'vinder' van feittype 'vondst van jutter' (bronregel 8) — een extensie voegt "
                + "attributen en kenmerken toe en geen rollen: een rol staat in haar feittype");
    }

    @Test void an_attribute_with_the_name_of_a_role_stays_admitted() {
        // Roles and members are separate namespaces (IR-SPEC ch. 2), in a head as in an extension.
        RegelSpraakModel m = clean(VINDER + "    de vinder Tekst;\n");
        assertInstanceOf(Datatype.Text.class, m.objectTypes.get("Vondst").attributes.get("vinder"));
    }

    // -------------------------------------------------------------------- a repeat that changes nothing: admitted

    @ParameterizedTest
    @ValueSource(strings = {"de waarde Numeriek (geheel getal);", "is waardevol kenmerk (bijvoeglijk);",
            "de waarde Numeriek (geheel getal)"})
    void a_literal_repetition_changes_nothing(String member) {
        RegelSpraakModel m = clean(extension(member));
        assertEquals(IrEmitter.emit(clean(VONDST)), IrEmitter.emit(m),
                "een letterlijke herhaling hoort het document niet te veranderen");
    }

    // -------------------------------------------------------------------- the marker added by the repeat

    private static final String MESSAGE = """

        Regel melding
            geldig altijd
                De meldingsdatum van een Vondst moet berekend worden als de vinddag van de Vondst plus 3 dagen.
        """;

    @Test void a_repetition_with_only_the_marking_marks_the_attribute() {
        // The same lowering as the marker written in the head; only the consistency's source names the line the
        // marker stands on, 6 in the extension against 3 in the head.
        String head = """
            Objecttype de Vondst (mv: Vondsten)
                de vinddag Datum in dagen;
                de meldingsdatum Datum in dagen (mag opgegeven worden);
            """;
        String ext = """
            Objecttype de Vondst (mv: Vondsten)
                de vinddag Datum in dagen;
                de meldingsdatum Datum in dagen;

            Extensie van objecttype de Vondst
                de meldingsdatum Datum in dagen (mag opgegeven worden);
            """;
        RegelSpraakModel m = clean(ext + MESSAGE);
        assertEquals(List.of("vinddag", "meldingsdatum", "opgegeven meldingsdatum", "berekende meldingsdatum"),
                List.copyOf(m.objectTypes.get("Vondst").attributes.keySet()));
        String viaExtension = IrEmitter.emit(m);
        assertEquals(1, viaExtension.split("\"line\": 6", -1).length - 1, viaExtension);
        assertEquals(IrEmitter.emit(clean(head + MESSAGE)), viaExtension.replace("\"line\": 6", "\"line\": 3"));
    }

    @Test void a_repetition_with_the_marking_carries_the_designations_of_the_attribute() {
        String source = """
            Vocabulaire handboek

            Objecttype de Vondst (mv: Vondsten)
                de vinddag Datum in dagen;
                de meldingsdatum (handboek: datum van melding) Datum in dagen;

            Extensie van objecttype de Vondst
                de meldingsdatum (handboek: datum van melding) Datum in dagen (mag opgegeven worden);
            """ + MESSAGE;
        RegelSpraakModel m = clean(source);
        assertTrue(m.objectTypes.get("Vondst").attributes.containsKey("opgegeven meldingsdatum"));
        // one designation in the side table; the consistency's source quotes the marked declaration as written
        String ir = IrEmitter.emit(m);
        assertEquals(1, ir.split("\"designation\": \"datum van melding\"", -1).length - 1, () -> "de benaming "
                + "hoort één keer in de zijtabel te staan: " + ir);
    }

    @Test void a_second_marking_is_refused() {
        RegelSpraakModel m = Resolver.resolve("""
            Objecttype de Vondst (mv: Vondsten)
                de vinddag Datum in dagen;
                de meldingsdatum Datum in dagen;

            Extensie van objecttype de Vondst
                de meldingsdatum Datum in dagen (mag opgegeven worden);

            Extensie van objecttype de Vondst
                de meldingsdatum Datum in dagen (mag opgegeven worden);
            """ + MESSAGE);
        onlyError(m, 9, "objecttype 'Vondst': de extensie declareert het attribuut 'meldingsdatum' (bronregel 9), dat "
                + "Vondst al draagt (bronregel 3), en dat draagt '(mag opgegeven worden)' al (bronregel 6)" + TAIL);
    }

    // -------------------------------------------------------------------- the inheritance line

    private static final String WRAK = """
        Objecttype de Vondst (mv: Vondsten)
            de waarde Numeriek (geheel getal);

        Objecttype het Wrak is een Vondst (mv: Wrakken)
            de diepte Numeriek (geheel getal);

        Extensie van objecttype het Wrak
        """;

    private static final String WF105 = " — WF-105: een subtype herdeclareert geen geërfde naam (één exemplaar zou "
            + "twee declaraties van die naam dragen, en welke een lezer vindt bepaalt datatype, dimensies en "
            + "tijdlijnfacet van elke lezing ervan)";

    @ParameterizedTest
    @ValueSource(strings = {"de waarde Tekst;", "de waarde Numeriek (geheel getal);",
            "de waarde Numeriek (geheel getal) (mag opgegeven worden);"})
    void an_extension_of_a_subtype_repeats_no_inherited_member(String member) {
        // Along the chain WF-105 speaks, also for a literal repeat and a marker: the marker would change the
        // supertype's attribute for every instance, and the opgave does not compose along the chain.
        RegelSpraakModel m = Resolver.resolve(WRAK + "    " + member + "\n");
        assertFalse(m.isOk(), () -> "verwachtte een weigering: " + m.diagnostics);
        List<Diagnostic> faults = m.errors();
        assertEquals(1, faults.size(), () -> "verwachtte één fout: " + faults);
        assertEquals("objecttype 'Wrak': attribuut 'waarde' (bronregel 8) is al gedeclareerd op 'Vondst' (bronregel "
                + "2) hogerop in de keten" + WF105, faults.get(0).message());
        assertEquals(8, faults.get(0).line());
    }
}
