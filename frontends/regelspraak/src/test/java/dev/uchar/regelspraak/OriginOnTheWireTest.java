package dev.uchar.regelspraak;

import dev.uchar.regelspraak.emit.IrEmitter;
import dev.uchar.regelspraak.ir.Spelling;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Benoembaar;
import dev.uchar.regelspraak.model.RegelSpraakModel.GegenereerdUit;
import dev.uchar.regelspraak.model.RegelSpraakModel.Herkomstvorm;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The origin on the wire (IR-SPEC ch. 2.13): on every declaration the frontend made itself
 * ({@link RegelSpraakModel#gegenereerd}) the emitter writes the field {@code origin} just before {@code designations} —
 * the form in its wire spelling, and the declaration it comes from by kind, owner and name — and on no other. A
 * decision table, which the model records as a form because it is no element of the model, is the kind
 * {@code decisionTable} on the wire. The field adds itself and nothing else: the model with its table cleared emits the
 * same document without it, byte for byte. Measured before this card: the emitter wrote no origin anywhere.
 */
class OriginOnTheWireTest {

    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");

    private static RegelSpraakModel clean(String bron) {
        RegelSpraakModel m = Resolver.resolve(bron);
        assertEquals(List.of(), m.errors().stream().map(RegelSpraakModel.Diagnostic::toString).toList());
        return m;
    }

    /** Every origin in the emitted document, as "<node> <owner.>name: <form> <- <kind> <owner.>name", in document
     *  order; the owner of a member is the object type or fact type it stands in. */
    private static List<String> origins(String ir) {
        List<String> result = new ArrayList<>();
        loop(Report.json(ir), null, result);
        return result;
    }

    private static void loop(Object x, String eigenaar, List<String> result) {
        if (x instanceof Map<?, ?> m) {
            if (m.get("origin") instanceof Map<?, ?> o) {
                Map<?, ?> van = (Map<?, ?>) o.get("from");
                boolean member = "Attribute".equals(m.get("node")) || "Characteristic".equals(m.get("node"))
                        || "Role".equals(m.get("node"));
                result.add(m.get("node") + " " + (member ? eigenaar + "." : "") + m.get("name") + ": " + o.get("form")
                        + " <- " + van.get("kind") + " " + (van.containsKey("owner") ? van.get("owner") + "." : "")
                        + van.get("name"));
            }
            String within = "ObjectType".equals(m.get("node")) || "FactType".equals(m.get("node"))
                    ? (String) m.get("name") : eigenaar;
            for (Object v : m.values()) loop(v, within, result);
        } else if (x instanceof List<?> l) {
            for (Object v : l) loop(v, eigenaar, result);
        }
    }

    /** The emission of {@code m} holds one origin per entry of its table, no more. */
    private static List<String> emission(RegelSpraakModel m) {
        List<String> h = origins(IrEmitter.emit(m));
        assertEquals(m.gegenereerd.size(), h.size(), () -> "een origin per gegenereerd element: " + h);
        return h;
    }

    // ---------------- every form on the wire ----------------

    @Test void the_input_family_comes_out_of_the_marked_attribute() {
        RegelSpraakModel m = clean("""
            Objecttype de Vondst (mv: Vondsten)
                de taxatie Numeriek (getal met 2 decimalen);
                de waarde Numeriek (getal met 2 decimalen) (mag opgegeven worden);

            Regel waardering
                geldig altijd
                    De waarde van een Vondst moet berekend worden als de taxatie van de Vondst verminderd met 5,00.
            """);
        assertEquals(List.of(
                "Attribute Vondst.opgegeven waarde: inputAboveComputation <- attribute Vondst.waarde",
                "Attribute Vondst.berekende waarde: inputAboveComputation <- attribute Vondst.waarde",
                "Characteristic Vondst.afwijkende opgave waarde: inputAboveComputation <- attribute Vondst.waarde",
                "Rule afwijking opgave waarde: inputAboveComputation <- attribute Vondst.waarde",
                "Consistency controle opgave waarde: inputAboveComputation <- attribute Vondst.waarde",
                "PrioritizedDefaults opgave boven berekening waarde: inputAboveComputation <- attribute Vondst.waarde"),
                emission(m));
    }

    @Test void the_group_of_a_subtype_comes_out_of_the_base_rule_and_the_base_group_carries_no_origin() {
        RegelSpraakModel m = clean("""
            Eenheidsysteem Tijd
                de dag dg
                de maand mnd

            Objecttype de Jutter (mv: Jutters) (bezield)
                de vinddag Datum in dagen;
                de meldtermijn Datum in dagen;

            Objecttype de Strandjutter is een Jutter (mv: Strandjutters)
                het vergunningseinde Datum in dagen;

            Regel meldtermijn
                geldig altijd
                    De meldtermijn van een Jutter moet berekend worden als zijn vinddag plus 6 mnd
                    indien zijn vinddag gevuld is.

            Afwijking van meldtermijn: meldtermijn tot het vergunningseinde
                geldig altijd
                    De meldtermijn van een Strandjutter moet gesteld worden op zijn vergunningseinde
                    indien zijn vergunningseinde gevuld is.
            """);
        // the group 'meldtermijn' carries the name of the base rule the author wrote, and no origin
        assertEquals(List.of(
                "PrioritizedDefaults meldtermijn voor Strandjutter: exceptionForSubtype <- namedForm meldtermijn"),
                emission(m));
    }

    @Test void a_row_of_a_decision_table_comes_out_of_a_table_and_not_out_of_a_form() {
        RegelSpraakModel m = clean("""
            Objecttype de Jutter (mv: Jutters) (bezield)
                de bonus Numeriek (geheel getal);
                het aantal geborgen wrakken Numeriek (niet-negatief geheel getal);

            Beslistabel Bonustabel
                geldig altijd
            | | de bonus van een Jutter moet gesteld worden op | indien zijn aantal geborgen wrakken groter is dan |
            |---|---|---|
            | 1 | 10 | 5 |
            | 2 | 20 | 10 |
            """);
        assertEquals(List.of(
                "Rule Bonustabel 1: decisionTableRow <- decisionTable Bonustabel",
                "Rule Bonustabel 2: decisionTableRow <- decisionTable Bonustabel"),
                emission(m));
    }

    /** A rule may carry the name of a decision table, which is no form: with the kind namedForm the rows would name
     *  that rule as their source. */
    @Test void a_rule_with_the_name_of_the_table_is_not_the_source_of_its_rows() {
        RegelSpraakModel m = clean("""
            Objecttype de Jutter (mv: Jutters) (bezield)
                de bonus Numeriek (geheel getal);
                de premie Numeriek (geheel getal);
                het aantal geborgen wrakken Numeriek (niet-negatief geheel getal);

            Regel Bonustabel
                geldig altijd
                    De premie van een Jutter moet berekend worden als zijn aantal geborgen wrakken maal 2.

            Beslistabel Bonustabel
                geldig altijd
            | | de bonus van een Jutter moet gesteld worden op | indien zijn aantal geborgen wrakken groter is dan |
            |---|---|---|
            | 1 | 10 | 5 |
            | 2 | 20 | 10 |
            """);
        assertTrue(m.benoembaar().contains(Benoembaar.vorm("Bonustabel")));
        assertEquals(List.of(
                "Rule Bonustabel 1: decisionTableRow <- decisionTable Bonustabel",
                "Rule Bonustabel 2: decisionTableRow <- decisionTable Bonustabel"),
                emission(m));
    }

    @Test void the_member_of_a_fold_with_steps_comes_out_of_the_attribute_on_the_type_that_declares_it() {
        RegelSpraakModel m = clean("""
            Objecttype de Ploeg (mv: Ploegen)
                het zwaarste gewicht Numeriek (geheel getal);

            Objecttype de Jutter (mv: Jutters) (bezield)
                de startdag Datum in dagen;

            Objecttype de Vondst (mv: Vondsten)
                de vinddag Datum in dagen;

            Objecttype de Meting (mv: Metingen)
                het gewicht Numeriek (geheel getal);

            Objecttype de Weging is een Meting (mv: Wegingen)
                de weegdag Datum in dagen;

            Feittype PloegHeeftJutter
                de ploeg Ploeg
                de jutter (mv: jutters) Jutter
            Eén ploeg heeft meerdere jutters

            Feittype JutterHeeftVondst
                de jutter Jutter
                de vondst Vondst
            Eén jutter heeft één vondst

            Feittype VondstHeeftWeging
                de vondst Vondst
                de weging Weging
            Eén vondst heeft één weging

            Regel zwaarste gewicht
                geldig altijd
                    Het zwaarste gewicht van een Ploeg moet berekend worden als de maximale waarde van het gewicht van de weging van de vondst van alle jutters van de Ploeg.
            """);
        assertEquals(List.of(
                "Attribute Jutter.gewicht via vondst en weging: stepsAfterSet <- attribute Meting.gewicht",
                "Rule gewicht via vondst en weging: stepsAfterSet <- attribute Meting.gewicht"),
                emission(m));
    }

    private static final String START_POINT = """
        Tijdlijn het penningjaar heeft tijdvakken van een jaar met variabel startpunt;

        Objecttype de Jutter (mv: Jutters) (bezield)
            de aanvangsdatum Datum in dagen;
        %s
        Regel penningjaar van een jutter
            geldig altijd
                Het startpunt van het penningjaar voor een Jutter wordt bepaald door zijn aanvangsdatum.
        """;

    @Test void the_anchor_of_a_start_point_determination_comes_out_of_the_rule_and_a_written_anchor_carries_no_origin() {
        assertEquals(List.of(
                "Attribute Jutter.startpunt penningjaar: startPoint <- namedForm penningjaar van een jutter"),
                emission(clean(START_POINT.formatted(""))));
        RegelSpraakModel own = clean(START_POINT.formatted("    het startpunt penningjaar Datum in dagen;\n"));
        assertEquals(List.of(), emission(own));
    }

    // ---------------- the place of the field ----------------

    /** The origin stands just before the designations, which stay the last field of their node. */
    @Test void origin_stands_just_before_designations() {
        RegelSpraakModel m = clean("""
            Vocabulaire handboek

            Objecttype de Vondst (mv: Vondsten)
                de taxatie Numeriek (getal met 2 decimalen);
                de waarde (handboek: getaxeerde waarde) Numeriek (getal met 2 decimalen) (mag opgegeven worden);

            Regel waardering
                geldig altijd
                    De waarde van een Vondst moet berekend worden als de taxatie van de Vondst verminderd met 5,00.
            """);
        List<List<String>> keys = new ArrayList<>();
        keysWithOrigin(Report.json(IrEmitter.emit(m)), keys);
        assertEquals(6, keys.size());
        for (List<String> k : keys)
            assertEquals(List.of("origin", "designations"), k.subList(k.size() - 2, k.size()), k::toString);
    }

    private static void keysWithOrigin(Object x, List<List<String>> result) {
        if (x instanceof Map<?, ?> m) {
            if (m.containsKey("origin")) result.add(m.keySet().stream().map(String::valueOf).toList());
            for (Object v : m.values()) keysWithOrigin(v, result);
        } else if (x instanceof List<?> l) {
            for (Object v : l) keysWithOrigin(v, result);
        }
    }

    // ---------------- inertness: the field adds itself and nothing else ----------------

    /** An origin record as the emitter writes it, with the comma that joins it to the field before it. */
    private static final Pattern ORIGIN = Pattern.compile(",\\n *\"origin\": \\{\\n.*?\\n *\\}\\n *\\}", Pattern.DOTALL);

    /** Over every rgs golden whose model has a made declaration: the model with its table cleared emits the golden's
     *  document with every origin taken out, byte for byte, and no origin. A golden without one emits none, which gate
     *  10 holds byte for byte against the committed document. */
    @Test void without_the_table_the_document_is_the_same_without_origin() throws Exception {
        int with = 0, without = 0;
        try (var directories = Files.list(GOLDEN)) {
            for (Path map : directories.sorted().toList()) {
                Path bron = map.resolve("bron.rgs");
                if (!Files.exists(bron)) continue;
                RegelSpraakModel m = Resolver.resolve(Files.readString(bron));
                assertTrue(m.isOk(), () -> map + " resolvet niet: " + m.diagnostics);
                String ir = IrEmitter.emit(m);
                if (m.gegenereerd.isEmpty()) {
                    assertFalse(ir.contains("\"origin\""), map::toString);
                    without++;
                    continue;
                }
                with++;
                assertEquals(m.gegenereerd.size(), origins(ir).size(), map::toString);
                m.gegenereerd.clear();
                String plain = IrEmitter.emit(m);
                assertFalse(plain.contains("\"origin\""), map::toString);
                assertEquals(ORIGIN.matcher(ir).replaceAll(""), plain, map::toString);
            }
        }
        assertTrue(with >= 9, "rgs-goldens met een gemaakte declaratie: " + with);
        assertTrue(without >= 100, "rgs-goldens zonder: " + without);
    }

    // ---------------- the spelling, and the schema's enums ----------------

    @Test void the_spelling_of_form_and_kind_is_that_of_the_schema() throws Exception {
        Map<?, ?> defs = (Map<?, ?>) ((Map<?, ?>) Report.json(Files.readString(
                Path.of("..", "..", "spec", "schema", "uchar-ir-v1.schema.json")))).get("$defs");
        List<?> vormen = (List<?>) ((Map<?, ?>) ((Map<?, ?>) ((Map<?, ?>) defs.get("Origin")).get("properties"))
                .get("form")).get("enum");
        List<?> kinds = (List<?>) ((Map<?, ?>) ((Map<?, ?>) ((Map<?, ?>) defs.get("OriginSource")).get("properties"))
                .get("kind")).get("enum");
        assertEquals(vormen, List.copyOf(Spelling.ORIGIN_FORM.rows().values()));
        List<String> java = new ArrayList<>(Spelling.ORIGIN_KIND.rows().values());
        java.add(Spelling.ORIGIN_DECISION_TABLE);
        assertEquals(kinds, java);
        // the five forms of the frontend have a spelling; the backend's two manifest keys none
        for (Herkomstvorm v : Herkomstvorm.values()) {
            boolean frontend = v != Herkomstvorm.DEFAULTS_STACK && v != Herkomstvorm.VERSION;
            assertEquals(frontend, Spelling.ORIGIN_FORM.rows().containsKey(v), v::toString);
            if (!frontend) assertThrows(IllegalArgumentException.class, () -> Spelling.ORIGIN_FORM.spelling(v));
        }
        for (Benoembaar.Soort s : Benoembaar.Soort.values())
            assertEquals(s, Spelling.originKindFromWire(Spelling.ORIGIN_KIND.spelling(s)));
        // a decision table is a form in the model and its own kind on the wire
        assertEquals(Benoembaar.Soort.VORM, Spelling.originKindFromWire("decisionTable"));
        assertEquals("decisionTable", Spelling.originKind(
                new GegenereerdUit(Benoembaar.vorm("Bonustabel"), Herkomstvorm.BESLISTABELRIJ)));
        assertEquals("namedForm", Spelling.originKind(
                new GegenereerdUit(Benoembaar.vorm("meldtermijn"), Herkomstvorm.EXCEPTION_FOR_SUBTYPE)));
        assertNull(Spelling.originKindFromWire("form"));
        assertNull(Spelling.originKindFromWire("VORM"));
    }
}
