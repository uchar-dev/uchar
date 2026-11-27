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
 * The marker {@code (volledig gegeven)} (grammar/DIVERGENCES.md, "Volledig gegeven"): on an attribute or a characteristic it
 * writes {@code complete: true} on the member's declaration (IR-SPEC ch. 2.2), after its timeline; the two attributes
 * an opgave marker generates carry the field always; {@code (volledig gegeven)} beside {@code (mag opgegeven worden)} is RGS-463,
 * and on a member a writer other than an initialisation writes on the declaring type itself RGS-464, a writer on a
 * subtype alone and an initialisation no reason; the token reserves no word; and it is a keyword to the semantic tokens. Measured before this card: every
 * document below that writes the marker was a syntax error on it.
 */
class CompleteMarkerTest {

    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden", "jutterswet-volledig-gegeven");

    private static final String MODEL = """
            Afleiden uit leeg: nee

            Objecttype de Vondst (mv: Vondsten)
                de taxatie Numeriek (geheel getal);
                de toeslag Numeriek (geheel getal) (volledig gegeven);
                de uitbetaling Numeriek (geheel getal);
                is drijfhout kenmerk (bijvoeglijk) (volledig gegeven);
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
        String without = clean(MODEL.replace(" (volledig gegeven)", "")).document();
        assertEquals(without, ir.replaceAll(",\\n\\s*\"complete\": true", ""));
    }

    @Test void on_a_time_dependent_member_the_field_stands_after_the_timeline() {
        String ir = clean("""
                Objecttype de Vondst (mv: Vondsten)
                    de opslagkosten Numeriek (geheel getal) voor elke maand (volledig gegeven);
                    is gemeld kenmerk (bijvoeglijk) voor elke maand (volledig gegeven);
                """).document();
        assertEquals(List.of("Attribute opslagkosten", "Characteristic gemeld"), complete(ir));
        assertEquals(List.of("node", "name", "datatype", "timeline", "complete"),
                List.copyOf(members(ir).get("Attribute opslagkosten").keySet()));
        assertEquals(List.of("node", "name", "kind", "timeline", "complete"),
                List.copyOf(members(ir).get("Characteristic gemeld").keySet()));
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
        for (String markers : List.of("(volledig gegeven) (mag opgegeven worden)", "(mag opgegeven worden) (volledig gegeven)")) {
            Diagnostic d = refused(WITH_OPGAVE.formatted(markers), "RGS-463");
            assertEquals(3, d.line(), markers);
            assertEquals(Map.of("attribute", "bergloon", "objectType", "Vondst", "complete", "(volledig gegeven)",
                    "opgave", "(mag opgegeven worden)"), d.params());
            assertEquals("attribuut 'bergloon' van Vondst: '(volledig gegeven)' staat naast '(mag opgegeven worden)' — het "
                    + "attribuut is dan het doel van een opgave boven zijn berekening en geen invoer, en de opgave die "
                    + "'(mag opgegeven worden)' genereert, is al volledig; schrijf alleen '(mag opgegeven worden)' "
                    + "(dialect, Volledig gegeven)", d.message(Locale.forLanguageTag("nl")));
            assertEquals("attribute 'bergloon' of Vondst: '(volledig gegeven)' stands beside '(mag opgegeven worden)' — the "
                    + "attribute is then the target of an opgave above its computation and no input, and the opgave "
                    + "that '(mag opgegeven worden)' generates is complete already; write '(mag opgegeven worden)' "
                    + "alone (dialect, Volledig gegeven)", d.message(Locale.ENGLISH));
        }
    }

    @Test void the_marker_on_a_member_a_rule_writes_on_its_type_is_rgs_464() {
        Diagnostic d = refused("""
                Objecttype de Vondst (mv: Vondsten)
                    de taxatie Numeriek (geheel getal);
                    het vindloon Numeriek (geheel getal) (volledig gegeven);

                Regel vindloon
                    geldig altijd
                        Het vindloon van een Vondst moet gesteld worden op 10 indien zijn taxatie groter is dan 1000.
                """, "RGS-464");
        assertEquals(3, d.line());
        assertEquals("attribuut 'vindloon' van Vondst: '(volledig gegeven)' staat op een lid dat regel 'vindloon' op Vondst zelf "
                + "schrijft — het lid volgt dan op elk object van Vondst zijn schrijvers en is nergens invoer, dus de "
                + "markering doet niets (RUNTIME-SEMANTICS ch. 1.5); laat '(volledig gegeven)' weg, of zet het op de invoerleden "
                + "die regel 'vindloon' leest (dialect, Volledig gegeven)", d.message(Locale.forLanguageTag("nl")));
        assertEquals("attribute 'vindloon' of Vondst: '(volledig gegeven)' stands on a member that rule 'vindloon' writes on "
                + "Vondst itself — the member then follows its writers on every object of Vondst and is input nowhere, "
                + "so the marking does nothing (RUNTIME-SEMANTICS ch. 1.5); leave '(volledig gegeven)' out, or put it on the "
                + "input members that rule 'vindloon' reads (dialect, Volledig gegeven)", d.message(Locale.ENGLISH));
    }

    @Test void the_marker_on_a_characteristic_a_rule_attributes_is_rgs_464() {
        Diagnostic d = refused("""
                Objecttype de Vondst (mv: Vondsten)
                    de taxatie Numeriek (geheel getal);
                    is kostbaar kenmerk (bijvoeglijk) (volledig gegeven);

                Regel kostbaarheid
                    geldig altijd
                        Een Vondst is kostbaar indien zijn taxatie groter is dan 1000.
                """, "RGS-464");
        assertEquals("kenmerk 'kostbaar' van Vondst: '(volledig gegeven)' staat op een lid dat regel 'kostbaarheid' op Vondst "
                + "zelf schrijft — het lid volgt dan op elk object van Vondst zijn schrijvers en is nergens invoer, dus "
                + "de markering doet niets (RUNTIME-SEMANTICS ch. 1.5); laat '(volledig gegeven)' weg, of zet het op de "
                + "invoerleden die regel 'kostbaarheid' leest (dialect, Volledig gegeven)",
                d.message(Locale.forLanguageTag("nl")));
    }

    @Test void the_marker_on_a_member_a_defaults_group_writes_on_its_type_is_rgs_464() {
        Diagnostic d = refused("""
                Objecttype de Vondst (mv: Vondsten)
                    het gewicht Numeriek (geheel getal);
                    het vindloon Numeriek (geheel getal) (volledig gegeven);
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
        assertTrue(nl.startsWith("attribuut 'vindloon' van Vondst: '(volledig gegeven)' staat op een lid dat de defaults-groep '"),
                nl);
        assertTrue(nl.contains("' op Vondst zelf schrijft — "), nl);
    }

    /** An extension that writes the marked declaration again with the opgave marker added: the two markers stand in two
     *  declarations, and the group the opgave generates on the type is the writer RGS-464 names. A repeat that adds or
     *  drops '(volledig gegeven)' is not literal (RGS-009). */
    @Test void an_opgave_added_by_an_extension_to_a_complete_attribute_is_rgs_464() {
        String model = """
                Objecttype de Vondst (mv: Vondsten)
                    het gewicht Numeriek (geheel getal);
                    het bergloon Numeriek (geheel getal) (volledig gegeven);

                Extensie van objecttype de Vondst
                    het bergloon Numeriek (geheel getal) %s;

                Regel bergloon
                    geldig altijd
                        Het bergloon van een Vondst moet berekend worden als zijn gewicht maal 2.
                """;
        Diagnostic d = refused(model.formatted("(volledig gegeven) (mag opgegeven worden)"), "RGS-464");
        assertEquals(3, d.line());
        String nl = d.message(Locale.forLanguageTag("nl"));
        assertTrue(nl.startsWith("attribuut 'bergloon' van Vondst: '(volledig gegeven)' staat op een lid dat de defaults-groep "
                + "'opgave boven berekening bergloon' op Vondst zelf schrijft — "), nl);
        assertTrue(Resolver.resolve(model.formatted("(mag opgegeven worden)")).errors().stream()
                .anyMatch(e -> e.code().equals("RGS-009") && e.line() == 6), "the repeat that drops the marker");
    }

    /** A writer on a subtype alone leaves the member input on every object outside that subtype: the marker stands. */
    @Test void a_writer_on_a_subtype_alone_is_no_reason() {
        String ir = clean("""
                Objecttype de Vondst (mv: Vondsten)
                    de taxatie Numeriek (geheel getal);
                    het vindloon Numeriek (geheel getal) (volledig gegeven);

                Objecttype de Strandvondst is een Vondst (mv: Strandvondsten)

                Regel vindloon van een strandvondst
                    geldig altijd
                        Het vindloon van een Strandvondst moet gesteld worden op 10 indien zijn taxatie groter is dan 1000.
                """).document();
        assertEquals(List.of("Attribute vindloon"), complete(ir));
    }

    /** A member that only an initialisation writes is input (RUNTIME-SEMANTICS ch. 1.5, "The three states"): the marker
     *  stands there, and the field is written; beside a computing rule on the same target the rule is the writer
     *  RGS-464 names. */
    @Test void an_initialisation_is_no_reason() {
        String model = """
                Afleiden uit leeg: nee

                Objecttype de Vondst (mv: Vondsten)
                    de vangst Numeriek (geheel getal);
                    het strandloon Numeriek (geheel getal) (volledig gegeven);

                Regel strandloon aanvankelijk
                    geldig altijd
                        Het strandloon van een Vondst moet geïnitialiseerd worden op 5.
                %s""";
        assertEquals(List.of("Attribute strandloon"), complete(clean(model.formatted("")).document()));
        Diagnostic d = refused(model.formatted("""

                Regel strandloon
                    geldig altijd
                        Het strandloon van een Vondst moet berekend worden als zijn vangst maal 2.
                """), "RGS-464");
        assertTrue(d.message(Locale.forLanguageTag("nl")).contains("regel 'strandloon' op Vondst zelf schrijft"),
                d.message(Locale.forLanguageTag("nl")));
    }

    // ---------------- the token ----------------

    /** The marker is one token with its parentheses, the words apart by any white space: 'volledig' and 'gegeven' alone
     *  stay name words, in parentheses as well, in a declaration, a rule name, a text and a comment. */
    @Test void the_marker_is_one_token_and_reserves_no_word() {
        assertEquals(List.of("VOLLEDIG_GEGEVEN"), types("(volledig gegeven)"));
        assertEquals(List.of("VOLLEDIG_GEGEVEN"), types("(volledig\n    gegeven)"));
        assertEquals(List.of("LPAREN", "LOWER_ID", "RPAREN"), types("(volledig)"));
        assertEquals(List.of("LPAREN", "LOWER_ID", "RPAREN"), types("(gegeven)"));
        assertEquals(List.of("LOWER_ID", "LOWER_ID"), types("volledig gegeven"));
        assertEquals(List.of(), types("// (volledig gegeven)"));
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
                        De omschrijving van een Vondst moet gesteld worden op "(volledig gegeven)".
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
        List<Span> markers = d.files().get(0).spans().stream().filter(s -> s.text().equals("(volledig gegeven)")).toList();
        assertEquals(2, markers.size(), () -> d.files().get(0).spans().toString());
        assertTrue(markers.stream().allMatch(s -> s.kind() == Kind.KEYWORD && !s.declaration() && s.declaredAt() == null));
        assertEquals(List.of(5, 7), markers.stream().map(Span::line).toList());
        Span opgave = Resolver.tokens(List.of(new SemanticTokens.SourceFile("bergloon.rgs", WITH_OPGAVE.formatted(
                "(mag opgegeven worden)")))).files().get(0).spans().stream()
                .filter(s -> s.text().equals("(mag opgegeven worden)")).findFirst().orElseThrow();
        assertEquals(Kind.KEYWORD, opgave.kind(), "the opgave marker is the same kind of span");
    }

    // ---------------- roles ----------------

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> factTypeRoles(String ir, int factTypeIndex) {
        Map<String, Object> root = (Map<String, Object>) Report.json(ir);
        Map<String, Object> model = (Map<String, Object>) root.get("model");
        Map<String, Object> ft = (Map<String, Object>) ((List<Object>) model.get("factTypes")).get(factTypeIndex);
        return (List<Map<String, Object>>) ft.get("roles");
    }

    @Test void the_marker_writes_complete_on_non_reciprocal_to_one_and_to_many_roles_in_schema_key_order() {
        String source = """
                Afleiden uit leeg: nee

                Objecttype de Jutter (mv: Jutters) (bezield)
                Objecttype de Vondst (mv: Vondsten)
                Objecttype de Keuring (mv: Keuringen)

                Feittype vinder van een vondst
                    de vinder Jutter (volledig gegeven)
                    de vondst (mv: vondsten) Vondst (volledig gegeven)
                Eén vinder heeft meerdere vondsten

                Feittype keuring van een vondst
                    de vondst Vondst
                    de keuring (mv: keuringen) Keuring (volledig gegeven)
                Eén vondst heeft één keuring
                """;
        String ir = clean(source).document();
        Map<String, Object> vinder = factTypeRoles(ir, 0).get(0);
        assertEquals(List.of("node", "name", "objectType", "many", "complete"), List.copyOf(vinder.keySet()));
        assertEquals(Boolean.FALSE, vinder.get("many"));
        assertEquals(Boolean.TRUE, vinder.get("complete"));

        Map<String, Object> vondst = factTypeRoles(ir, 0).get(1);
        assertEquals(List.of("node", "name", "objectType", "plural", "many", "complete"), List.copyOf(vondst.keySet()));
        assertEquals(Boolean.TRUE, vondst.get("many"));
        assertEquals(Boolean.TRUE, vondst.get("complete"));

        assertFalse(factTypeRoles(ir, 1).get(0).containsKey("complete"));
        Map<String, Object> keuring = factTypeRoles(ir, 1).get(1);
        assertEquals(List.of("node", "name", "objectType", "plural", "many", "complete"),
                List.copyOf(keuring.keySet()));
        assertEquals(Boolean.TRUE, keuring.get("complete"));

        List<SemanticTokens.SourceFile> files = List.of(new SemanticTokens.SourceFile("rollen.rgs", source));
        SemanticTokens.Document tokens = Resolver.tokens(files);
        SemanticTokensCheck.check(files, tokens);
        List<Span> markers = tokens.files().get(0).spans().stream()
                .filter(s -> s.text().equals("(volledig gegeven)")).toList();
        assertEquals(3, markers.size());
        assertTrue(markers.stream().allMatch(s -> s.kind() == Kind.KEYWORD && !s.declaration() && s.declaredAt() == null));
    }

    @Test void rgs_474_refuses_complete_on_reciprocal_roles_only() {
        Diagnostic reciprocal = refused("""
                Afleiden uit leeg: nee

                Objecttype de Jutter (mv: Jutters) (bezield)

                Wederkerig feittype strandmaat
                    de maat (mv: maten) Jutter (volledig gegeven)
                Eén maat is strandmaat van één maat
                """, "RGS-474");
        assertEquals(6, reciprocal.line());
        assertTrue(reciprocal.message(Locale.forLanguageTag("nl")).contains("wederkerig feittype"),
                reciprocal.message(Locale.forLanguageTag("nl")));
        assertTrue(reciprocal.message(Locale.ENGLISH).contains("reciprocal fact type"),
                reciprocal.message(Locale.ENGLISH));
    }

    @Test void rgs_475_refuses_complete_on_a_role_written_by_object_or_fact_creation_slot_and_subtype_exact() {
        // 1:1 ObjectCreation writes the created-side role ('keuring'), NOT the back-link ('vondst') on case Keuring instances
        String objectCreationBase = """
                Afleiden uit leeg: nee

                Objecttype de Vondst (mv: Vondsten)
                    het gewicht Numeriek (geheel getal);
                Objecttype de Strandvondst is een Vondst (mv: Strandvondsten)
                Objecttype de Keuring (mv: Keuringen)
                    het rapportnummer Numeriek (geheel getal);

                Feittype keuring van een vondst
                    de vondst Vondst %s
                    de keuring Keuring %s
                Eén vondst heeft één keuring

                Regel maak keuring
                    geldig altijd
                        Een %s heeft een keuring met rapportnummer gelijk aan 1.
                """;
        Diagnostic ocRefused = refused(objectCreationBase.formatted("", "(volledig gegeven)", "Vondst"), "RGS-475");
        assertEquals(11, ocRefused.line());
        assertTrue(ocRefused.message(Locale.forLanguageTag("nl")).contains("objectcreatie 'maak keuring' op Vondst zelf legt"),
                ocRefused.message(Locale.forLanguageTag("nl")));
        assertTrue(ocRefused.message(Locale.ENGLISH).contains("object creation 'maak keuring' lays on Vondst itself"),
                ocRefused.message(Locale.ENGLISH));

        // Back-link role ('vondst') on case instances of Keuring stays input, so '(volledig gegeven)' stands there
        String backLinkAllowed = clean(objectCreationBase.formatted("(volledig gegeven)", "", "Vondst")).document();
        assertEquals(Boolean.TRUE, factTypeRoles(backLinkAllowed, 0).get(0).get("complete"));

        // Writer on a strict subtype (Strandvondst) alone leaves 'keuring' input on other Vondst instances
        String subtypeWriterAllowed = clean(objectCreationBase.formatted("", "(volledig gegeven)", "Strandvondst")).document();
        assertEquals(Boolean.TRUE, factTypeRoles(subtypeWriterAllowed, 0).get(1).get("complete"));

        // 1:n ObjectCreation writes the to-many role ('keuring' / mv 'keuringen') on Vondst -> refused on 'keuring', allowed on 'vondst'
        String toManyObjectCreation = """
                Afleiden uit leeg: nee

                Objecttype de Vondst (mv: Vondsten)
                Objecttype de Keuring (mv: Keuringen)
                    het rapportnummer Numeriek (geheel getal);

                Feittype keuringen van een vondst
                    de vondst Vondst %s
                    de keuring (mv: keuringen) Keuring %s
                Eén vondst heeft meerdere keuringen

                Regel maak keuring
                    geldig altijd
                        Een Vondst heeft een keuring met rapportnummer gelijk aan 1.
                """;
        Diagnostic ocManyRefused = refused(toManyObjectCreation.formatted("", "(volledig gegeven)"), "RGS-475");
        assertEquals(9, ocManyRefused.line());
        assertTrue(ocManyRefused.message(Locale.forLanguageTag("nl")).contains("objectcreatie 'maak keuring' op Vondst zelf legt"),
                ocManyRefused.message(Locale.forLanguageTag("nl")));
        String ocManyBackLinkAllowed = clean(toManyObjectCreation.formatted("(volledig gegeven)", "")).document();
        assertEquals(Boolean.TRUE, factTypeRoles(ocManyBackLinkAllowed, 0).get(0).get("complete"));

        // Self-relation: slot 1 ('volgende vergunning') written by ObjectCreation -> refused on slot 1, allowed on slot 0
        String selfRel = """
                Afleiden uit leeg: nee

                Objecttype de Vergunning (mv: Vergunningen)
                    het volgnummer Numeriek (geheel getal);

                Feittype opvolging van vergunningen
                    de vorige vergunning Vergunning %s
                    de volgende vergunning Vergunning %s
                Eén vorige vergunning wordt gevolgd door één volgende vergunning

                Regelgroep vervolggroep
                is recursief
                Regel vervolgvergunning
                    geldig altijd
                        Een Vergunning heeft een volgende vergunning met volgnummer gelijk aan 2
                            indien zijn volgnummer gelijk is aan 1.
                """;
        Diagnostic selfRefused = refused(selfRel.formatted("", "(volledig gegeven)"), "RGS-475");
        assertEquals(8, selfRefused.line());
        String selfAllowed = clean(selfRel.formatted("(volledig gegeven)", "")).document();
        assertEquals(Boolean.TRUE, factTypeRoles(selfAllowed, 0).get(0).get("complete"));

        // FactCreation: writes both 'hulpjutter' (to-one on Vondst) AND 'hulpvondst' (to-many on Jutter) -> refused on either!
        String factCreationModel = """
                Afleiden uit leeg: nee

                Objecttype de Jutter (mv: Jutters) (bezield)
                Objecttype de Vondst (mv: Vondsten)

                Feittype vinder van een vondst
                    de vinder Jutter
                    de vondst (mv: vondsten) Vondst
                Eén vinder heeft meerdere vondsten

                Feittype hulp bij een vondst
                    de hulpjutter Jutter %s
                    de hulpvondst (mv: hulpvondsten) Vondst %s
                Eén hulpjutter helpt bij meerdere hulpvondsten

                Regel koppel hulpvondst
                    geldig altijd
                        Een hulpvondst van een Jutter is een vondst van de Jutter.
                """;
        Diagnostic fcToOneRefused = refused(factCreationModel.formatted("(volledig gegeven)", ""), "RGS-475");
        assertEquals(12, fcToOneRefused.line());
        assertTrue(fcToOneRefused.message(Locale.forLanguageTag("nl")).contains("feitcreatie 'koppel hulpvondst' op Vondst zelf legt"),
                fcToOneRefused.message(Locale.forLanguageTag("nl")));

        Diagnostic fcToManyRefused = refused(factCreationModel.formatted("", "(volledig gegeven)"), "RGS-475");
        assertEquals(13, fcToManyRefused.line());
        assertTrue(fcToManyRefused.message(Locale.forLanguageTag("nl")).contains("feitcreatie 'koppel hulpvondst' op Jutter zelf legt"),
                fcToManyRefused.message(Locale.forLanguageTag("nl")));
    }
}
