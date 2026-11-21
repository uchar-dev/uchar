package dev.uchar.regelspraak;

import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.parser.RegelSpraakLexer;
import dev.uchar.regelspraak.resolver.Resolver;
import dev.uchar.regelspraak.resolver.SemanticTokens;
import dev.uchar.regelspraak.resolver.SemanticTokens.Kind;
import dev.uchar.regelspraak.resolver.SemanticTokens.Span;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The marker {@code (volledig)} (grammar/DIVERGENCES.md, "Volledig gegeven"): on an attribute or a characteristic it
 * writes {@code complete: true} on the member's declaration (IR-SPEC ch. 2.2), after its timeline; the two attributes
 * an opgave marker generates carry the field always; {@code (volledig)} beside {@code (mag opgegeven worden)} is RGS-463,
 * and on a member a rule or a defaults group writes on the declaring type itself RGS-464, a writer on a subtype alone
 * no reason; the token reserves no word; and it is a keyword to the semantic tokens. Measured before this card: every
 * document below that writes the marker was a syntax error on it.
 */
class CompleteMarkerTest {

    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden", "jutterswet-volledig-gegeven");

    private static final String MODEL = """
            Afleiden uit leeg: nee

            Objecttype de Vondst (mv: Vondsten)
                de taxatie Numeriek (geheel getal);
                de toeslag Numeriek (geheel getal) (volledig);
                de uitbetaling Numeriek (geheel getal);
                is drijfhout kenmerk (bijvoeglijk) (volledig);
                de drijftoeslag Numeriek (geheel getal);

            Regel uitbetaling
                geldig altijd
                    De uitbetaling van een Vondst moet berekend worden als zijn taxatie plus zijn toeslag.

            Regel drijftoeslag
                geldig altijd
                    De drijftoeslag van een Vondst moet gesteld worden op 2 indien hij niet drijfhout is.
            """;

    private static RegelSpraakModel clean(String source) {
        RegelSpraakModel m = Resolver.resolve(source);
        assertEquals(List.of(), m.errors().stream().map(Diagnostic::toString).toList());
        return m;
    }

    /** The member nodes of the first object type of the emitted document, by node kind and name, keys in order. */
    @SuppressWarnings("unchecked")
    private static Map<String, Map<String, Object>> members(String ir) {
        Map<String, Object> root = (Map<String, Object>) Report.json(ir);
        Map<String, Object> model = (Map<String, Object>) root.get("model");
        Map<String, Object> ot = (Map<String, Object>) ((List<Object>) model.get("objectTypes")).get(0);
        Map<String, Map<String, Object>> result = new java.util.LinkedHashMap<>();
        for (String list : List.of("attributes", "characteristics"))
            for (Object m : (List<Object>) ot.get(list)) {
                Map<String, Object> node = (Map<String, Object>) m;
                result.put(node.get("node") + " " + node.get("name"), node);
            }
        return result;
    }

    /** The members of the emitted document that carry complete, as "<node> <name>". */
    private static List<String> complete(String ir) {
        List<String> result = new ArrayList<>();
        members(ir).forEach((k, node) -> {
            if (node.containsKey("complete")) {
                assertEquals(Boolean.TRUE, node.get("complete"), k);
                result.add(k);
            }
        });
        return result;
    }

    // ---------------- the field on the wire ----------------

    @Test void the_marker_writes_complete_on_an_attribute_and_on_a_characteristic() {
        String ir = clean(MODEL).document();
        assertEquals(List.of("Attribute toeslag", "Characteristic drijfhout"), complete(ir));
        assertEquals(List.of("node", "name", "datatype", "complete"), List.copyOf(members(ir).get("Attribute toeslag").keySet()));
        assertEquals(List.of("node", "name", "kind", "complete"),
                List.copyOf(members(ir).get("Characteristic drijfhout").keySet()));
        // the field is the whole difference: the document without the two markers is the one without the field
        String without = clean(MODEL.replace(" (volledig)", "")).document();
        assertEquals(without, ir.replaceAll(",\\n\\s*\"complete\": true", ""));
    }

    @Test void on_a_time_dependent_member_the_field_stands_after_the_timeline() {
        String ir = clean("""
                Objecttype de Vondst (mv: Vondsten)
                    de opslagkosten Numeriek (geheel getal) voor elke maand (volledig);
                    is gemeld kenmerk (bijvoeglijk) voor elke maand (volledig);
                """).document();
        assertEquals(List.of("Attribute opslagkosten", "Characteristic gemeld"), complete(ir));
        assertEquals(List.of("node", "name", "datatype", "timeline", "complete"),
                List.copyOf(members(ir).get("Attribute opslagkosten").keySet()));
        assertEquals(List.of("node", "name", "kind", "timeline", "complete"),
                List.copyOf(members(ir).get("Characteristic gemeld").keySet()));
    }

    @Test void the_marker_after_an_absence_marking_stands_on_the_attribute_with_markers() {
        String ir = clean("""
                Objecttype de Vondst (mv: Vondsten)
                    de taxatie Numeriek (geheel getal) (of onbekend) (volledig);
                """).document();
        assertEquals(List.of("Attribute taxatie"), complete(ir));
    }

    @Test void the_two_attributes_an_opgave_generates_carry_the_field_and_the_marked_attribute_does_not() {
        String ir = clean("""
                Objecttype de Vondst (mv: Vondsten)
                    het gewicht Numeriek (geheel getal);
                    het bergloon Numeriek (geheel getal) (mag opgegeven worden);

                Regel bergloon
                    geldig altijd
                        Het bergloon van een Vondst moet berekend worden als zijn gewicht maal 2.
                """).document();
        assertEquals(List.of("Attribute opgegeven bergloon", "Attribute berekende bergloon"), complete(ir));
        // after the datatype and before the origin of the generated member
        assertEquals(List.of("node", "name", "datatype", "complete", "origin"),
                List.copyOf(members(ir).get("Attribute opgegeven bergloon").keySet()));
    }

    @Test void an_opgave_without_a_computation_generates_nothing_and_writes_no_field() {
        RegelSpraakModel m = Resolver.resolve("""
                Objecttype de Vondst (mv: Vondsten)
                    het bergloon Numeriek (geheel getal) (mag opgegeven worden);
                """);
        assertTrue(m.isOk(), () -> m.errors().toString());
        assertEquals(List.of(), complete(m.document()));
    }

    @Test void the_golden_re_emits_byte_for_byte() throws Exception {
        assertEquals(Files.readString(GOLDEN.resolve("ir.json")),
                clean(Files.readString(GOLDEN.resolve("source.rgs"))).document());
    }

    // ---------------- the refusals ----------------

    private static Diagnostic refused(String source, String code) {
        RegelSpraakModel m = Resolver.resolve(source);
        assertEquals(1, m.errors().size(), () -> "" + m.diagnostics);
        Diagnostic d = m.errors().get(0);
        assertEquals(code, d.code(), () -> "" + m.diagnostics);
        return d;
    }

    private static final String WITH_OPGAVE = """
            Objecttype de Vondst (mv: Vondsten)
                het gewicht Numeriek (geheel getal);
                het bergloon Numeriek (geheel getal) %s;

            Regel bergloon
                geldig altijd
                    Het bergloon van een Vondst moet berekend worden als zijn gewicht maal 2.
            """;

    @Test void the_marker_beside_the_opgave_marker_is_rgs_463_in_either_order() {
        for (String markers : List.of("(volledig) (mag opgegeven worden)", "(mag opgegeven worden) (volledig)")) {
            Diagnostic d = refused(WITH_OPGAVE.formatted(markers), "RGS-463");
            assertEquals(3, d.line(), markers);
            assertEquals(Map.of("attribute", "bergloon", "objectType", "Vondst", "complete", "(volledig)",
                    "opgave", "(mag opgegeven worden)"), d.params());
            assertEquals("attribuut 'bergloon' van Vondst: '(volledig)' staat naast '(mag opgegeven worden)' — het "
                    + "attribuut is dan het doel van een opgave boven zijn berekening en geen invoer, en de opgave die "
                    + "'(mag opgegeven worden)' genereert, is al volledig; schrijf alleen '(mag opgegeven worden)' "
                    + "(dialect, Volledig gegeven)", d.message(Locale.forLanguageTag("nl")));
            assertEquals("attribute 'bergloon' of Vondst: '(volledig)' stands beside '(mag opgegeven worden)' — the "
                    + "attribute is then the target of an opgave above its computation and no input, and the opgave "
                    + "that '(mag opgegeven worden)' generates is complete already; write '(mag opgegeven worden)' "
                    + "alone (dialect, Volledig gegeven)", d.message(Locale.ENGLISH));
        }
    }

    @Test void the_marker_on_a_member_a_rule_writes_on_its_type_is_rgs_464() {
        Diagnostic d = refused("""
                Objecttype de Vondst (mv: Vondsten)
                    de taxatie Numeriek (geheel getal);
                    het vindloon Numeriek (geheel getal) (volledig);

                Regel vindloon
                    geldig altijd
                        Het vindloon van een Vondst moet gesteld worden op 10 indien zijn taxatie groter is dan 1000.
                """, "RGS-464");
        assertEquals(3, d.line());
        assertEquals("attribuut 'vindloon' van Vondst: '(volledig)' staat op een lid dat regel 'vindloon' op Vondst zelf "
                + "schrijft — het lid volgt dan op elk object van Vondst zijn schrijvers en is nergens invoer, dus de "
                + "markering doet niets (RUNTIME-SEMANTICS ch. 1.5); laat '(volledig)' weg, of zet het op de invoerleden "
                + "die regel 'vindloon' leest (dialect, Volledig gegeven)", d.message(Locale.forLanguageTag("nl")));
        assertEquals("attribute 'vindloon' of Vondst: '(volledig)' stands on a member that rule 'vindloon' writes on "
                + "Vondst itself — the member then follows its writers on every object of Vondst and is input nowhere, "
                + "so the marking does nothing (RUNTIME-SEMANTICS ch. 1.5); leave '(volledig)' out, or put it on the "
                + "input members that rule 'vindloon' reads (dialect, Volledig gegeven)", d.message(Locale.ENGLISH));
    }

    @Test void the_marker_on_a_characteristic_a_rule_attributes_is_rgs_464() {
        Diagnostic d = refused("""
                Objecttype de Vondst (mv: Vondsten)
                    de taxatie Numeriek (geheel getal);
                    is kostbaar kenmerk (bijvoeglijk) (volledig);

                Regel kostbaarheid
                    geldig altijd
                        Een Vondst is kostbaar indien zijn taxatie groter is dan 1000.
                """, "RGS-464");
        assertEquals("kenmerk 'kostbaar' van Vondst: '(volledig)' staat op een lid dat regel 'kostbaarheid' op Vondst "
                + "zelf schrijft — het lid volgt dan op elk object van Vondst zijn schrijvers en is nergens invoer, dus "
                + "de markering doet niets (RUNTIME-SEMANTICS ch. 1.5); laat '(volledig)' weg, of zet het op de "
                + "invoerleden die regel 'kostbaarheid' leest (dialect, Volledig gegeven)",
                d.message(Locale.forLanguageTag("nl")));
    }

    @Test void the_marker_on_a_member_a_defaults_group_writes_on_its_type_is_rgs_464() {
        Diagnostic d = refused("""
                Objecttype de Vondst (mv: Vondsten)
                    het gewicht Numeriek (geheel getal);
                    het vindloon Numeriek (geheel getal) (volledig);
                    is betwist kenmerk (bijvoeglijk);

                Regel vindloon
                    geldig altijd
                        Het vindloon van een Vondst moet berekend worden als zijn gewicht maal 2.

                Afwijking van vindloon: betwist vindloon
                    geldig altijd
                        Het vindloon van een Vondst moet gesteld worden op 0 indien hij betwist is.
                """, "RGS-464");
        assertEquals(3, d.line());
        String nl = d.message(Locale.forLanguageTag("nl"));
        assertTrue(nl.startsWith("attribuut 'vindloon' van Vondst: '(volledig)' staat op een lid dat de defaults-groep '"),
                nl);
        assertTrue(nl.contains("' op Vondst zelf schrijft — "), nl);
    }

    /** An extension that writes the marked declaration again with the opgave marker added: the two markers stand in two
     *  declarations, and the group the opgave generates on the type is the writer RGS-464 names. A repeat that adds or
     *  drops '(volledig)' is not literal (RGS-009). */
    @Test void an_opgave_added_by_an_extension_to_a_complete_attribute_is_rgs_464() {
        String model = """
                Objecttype de Vondst (mv: Vondsten)
                    het gewicht Numeriek (geheel getal);
                    het bergloon Numeriek (geheel getal) (volledig);

                Extensie van objecttype de Vondst
                    het bergloon Numeriek (geheel getal) %s;

                Regel bergloon
                    geldig altijd
                        Het bergloon van een Vondst moet berekend worden als zijn gewicht maal 2.
                """;
        Diagnostic d = refused(model.formatted("(volledig) (mag opgegeven worden)"), "RGS-464");
        assertEquals(3, d.line());
        String nl = d.message(Locale.forLanguageTag("nl"));
        assertTrue(nl.startsWith("attribuut 'bergloon' van Vondst: '(volledig)' staat op een lid dat de defaults-groep "
                + "'opgave boven berekening bergloon' op Vondst zelf schrijft — "), nl);
        assertTrue(Resolver.resolve(model.formatted("(mag opgegeven worden)")).errors().stream()
                .anyMatch(e -> e.code().equals("RGS-009") && e.line() == 6), "the repeat that drops the marker");
    }

    /** A writer on a subtype alone leaves the member input on every object outside that subtype: the marker stands. */
    @Test void a_writer_on_a_subtype_alone_is_no_reason() {
        String ir = clean("""
                Objecttype de Vondst (mv: Vondsten)
                    de taxatie Numeriek (geheel getal);
                    het vindloon Numeriek (geheel getal) (volledig);

                Objecttype de Strandvondst is een Vondst (mv: Strandvondsten)

                Regel vindloon van een strandvondst
                    geldig altijd
                        Het vindloon van een Strandvondst moet gesteld worden op 10 indien zijn taxatie groter is dan 1000.
                """).document();
        assertEquals(List.of("Attribute vindloon"), complete(ir));
    }

    // ---------------- the token ----------------

    /** The marker is one token with its parentheses: 'volledig' alone stays a name word, in a declaration, a rule name,
     *  a text and a comment. */
    @Test void the_marker_is_one_token_and_reserves_no_word() {
        assertEquals(List.of("VOLLEDIG"), types("(volledig)"));
        assertEquals(List.of("LPAREN", "LOWER_ID", "RPAREN"), types("( volledig )"));
        assertEquals(List.of("LOWER_ID", "LOWER_ID"), types("volledig gewicht"));
        assertEquals(List.of(), types("// (volledig)"));
        String ir = clean("""
                Objecttype de Vondst (mv: Vondsten)
                    het volledig gewicht Numeriek (geheel getal);
                    de omschrijving Tekst;
                    is volledig kenmerk (bijvoeglijk);

                Regel volledig gewicht
                    geldig altijd
                        Het volledig gewicht van een Vondst moet gesteld worden op 3.

                Regel omschrijving
                    geldig altijd
                        De omschrijving van een Vondst moet gesteld worden op "(volledig)".
                """).document();
        assertEquals(List.of(), complete(ir));
        assertTrue(members(ir).containsKey("Attribute volledig gewicht"));
        assertTrue(members(ir).containsKey("Characteristic volledig"));
    }

    private static List<String> types(String source) {
        RegelSpraakLexer lexer = new RegelSpraakLexer(CharStreams.fromString(source));
        List<String> result = new ArrayList<>();
        for (Token t : lexer.getAllTokens())
            if (t.getChannel() == Token.DEFAULT_CHANNEL) result.add(RegelSpraakLexer.VOCABULARY.getSymbolicName(t.getType()));
        return result;
    }

    // ---------------- the semantic tokens ----------------

    /** As the opgave marker: one span with its parentheses, a keyword, on an attribute and on a characteristic. */
    @Test void the_marker_is_a_keyword_span() {
        List<SemanticTokens.SourceFile> files = List.of(new SemanticTokens.SourceFile("vondst.rgs", MODEL));
        SemanticTokens.Document d = Resolver.tokens(files);
        SemanticTokensCheck.check(files, d);
        assertTrue(d.diagnostics().isEmpty(), () -> d.diagnostics().toString());
        List<Span> markers = d.files().get(0).spans().stream().filter(s -> s.text().equals("(volledig)")).toList();
        assertEquals(2, markers.size(), () -> d.files().get(0).spans().toString());
        assertTrue(markers.stream().allMatch(s -> s.kind() == Kind.KEYWORD && !s.declaration() && s.declaredAt() == null));
        assertEquals(List.of(5, 7), markers.stream().map(Span::line).toList());
        Span opgave = Resolver.tokens(List.of(new SemanticTokens.SourceFile("bergloon.rgs", WITH_OPGAVE.formatted(
                "(mag opgegeven worden)")))).files().get(0).spans().stream()
                .filter(s -> s.text().equals("(mag opgegeven worden)")).findFirst().orElseThrow();
        assertEquals(Kind.KEYWORD, opgave.kind(), "the opgave marker is the same kind of span");
    }
}
