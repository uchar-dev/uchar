package dev.uchar.regelspraak;

import dev.uchar.regelspraak.emit.IrEmitter;
import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.ir.ResolvedRule;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Designatable;
import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.model.RegelSpraakModel.Severity;
import dev.uchar.regelspraak.parser.RegelSpraak;
import dev.uchar.regelspraak.parser.RegelSpraakLexer;
import dev.uchar.regelspraak.resolver.Resolver;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The vocabulary extension (grammar/DIVERGENCES.md, "Vocabulaires en benamingen"; IR-SPEC ch. 2.12): a head
 * {@code Vocabulaire <naam>} with an optional {@code (standaard)} and {@code (taal: <code>)}, and an annotation
 * {@code (<vocabulaire>: <benaming>)} at the end of the name group of the nine designatable kinds. The tokens and the
 * fixed position, the side table the resolver fills, the emission — byte for byte the document without the extension
 * where none is written — the rule that a designation never resolves in rule text, and every refusal on its line in
 * the sentence the resolver writes. Measured before this card: every document below that writes a vocabulary or an
 * annotation was a syntax error at its first parenthesis.
 */
class VocabularyDialectTest {

    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden", "jutterswet-vocabulaires");

    private static String source() throws Exception { return Files.readString(GOLDEN.resolve("bron.rgs")); }

    // ---------------- the tokens and the grammar ----------------

    private static List<String> tokens(String source) {
        RegelSpraakLexer lexer = new RegelSpraakLexer(CharStreams.fromString(source));
        List<String> result = new ArrayList<>();
        for (Token t : lexer.getAllTokens())
            if (t.getChannel() == Token.DEFAULT_CHANNEL) result.add(RegelSpraakLexer.VOCABULARY.getSymbolicName(t.getType()));
        return result;
    }

    private static int syntaxErrors(String source) {
        List<String> meldingen = new ArrayList<>();
        BaseErrorListener l = new BaseErrorListener() {
            @Override public void syntaxError(Recognizer<?, ?> r, Object sym, int line, int col, String msg,
                                              RecognitionException e) {
                meldingen.add(line + ":" + col + " " + msg);
            }
        };
        RegelSpraakLexer lexer = new RegelSpraakLexer(CharStreams.fromString(source));
        lexer.removeErrorListeners(); lexer.addErrorListener(l);
        RegelSpraak parser = new RegelSpraak(new CommonTokenStream(lexer));
        parser.removeErrorListeners(); parser.addErrorListener(l);
        ParseTree tree = parser.regelSpraakDocument();
        return meldingen.size() + errorNodes(tree);
    }

    private static int errorNodes(ParseTree t) {
        if (t instanceof ErrorNode) return 1;
        int n = 0;
        for (int i = 0; i < t.getChildCount(); i++) n += errorNodes(t.getChild(i));
        return n;
    }

    @Test void the_tokens_and_their_boundaries() {
        assertEquals(List.of("BENAMING_START", "BENAMING_TEKST", "BENAMING_EINDE"), tokens("(handboek: nummer vondst)"));
        // the text is one token: keywords, digits, quotes and punctuation in it are never lexed
        assertEquals(List.of("BENAMING_START", "BENAMING_TEKST", "BENAMING_EINDE"),
                tokens("(wet: de 3 'waarden' van \"x\", per jaar; geldig altijd)"));
        assertEquals(List.of("MV_START", "UPPER_ID", "RPAREN"), tokens("(mv: Jutters)"));   // MV_START wins '(mv:'
        assertEquals(List.of("STANDAARD"), tokens("(standaard)"));
        assertEquals(List.of("VOCABULAIRE", "LOWER_ID"), tokens("Vocabulaire handboek"));
        assertEquals(List.of("UPPER_ID"), tokens("Vocabulaires"));
        assertEquals(List.of("LPAREN", "LOWER_ID", "LOWER_ID", "RPAREN"), tokens("(handboek nummer)"));   // no colon
        assertEquals(List.of("VOOR_HET_ATTRIBUUT_ZONDER_VOORZETSEL"), tokens("(voor het attribuut zonder voorzetsel):"));
        // an annotation left open ends at its line, as its own token
        assertEquals(List.of("BENAMING_START", "BENAMING_TEKST", "BENAMING_ONAF", "LOWER_ID"),
                tokens("(handboek: nummer\nvondst"));
    }

    @Test void every_position_parses_and_the_order_is_fixed() {
        assertEquals(0, syntaxErrors("""
            Vocabulaire handboek
            Vocabulaire cgm (standaard) (taal: nl)
            Vocabulaire wet (taal: nl) (standaard)
            Objecttype de Jutter (mv: Jutters) (bezield) (handboek: Jut01) (cgm: JUTTER)
                het vondstnummer (mv: vondstnummers) (handboek: nummer vondst) (cgm: VONDST_NR) Numeriek (geheel getal);
                is bergingsplichtig (cgm: BERGPLICHT) kenmerk (bijvoeglijk);
                de strandpas (cgm: PAS) kenmerk (bezittelijk);
            """));
        // the annotations close the name group: behind the plural and (bezield), never before them
        assertNotEquals(0, syntaxErrors("Objecttype de Jutter (handboek: Jut01) (mv: Jutters)\n"));
        assertNotEquals(0, syntaxErrors("Objecttype de Jutter (mv: Jutters) (handboek: Jut01) (bezield)\n"));
        assertNotEquals(0, syntaxErrors("Objecttype de Vondst\n    het gewicht Numeriek (geheel getal) (cgm: GEWICHT);\n"));
    }

    // ---------------- the side table ----------------

    @Test void the_golden_fills_the_side_table() throws Exception {
        RegelSpraakModel m = Resolver.resolve(source());
        assertTrue(m.errors().isEmpty(), () -> m.errors().toString());
        assertEquals(List.of(new RegelSpraakModel.Vocabulary("handboek", false, "nl"),
                new RegelSpraakModel.Vocabulary("cgm", true, "nl")), m.vocabularies);
        assertEquals(Optional.of("totale premie"), m.benaming("handboek", Designatable.attribute("Jutter", "vondstbedrag")));
        assertEquals(Optional.of("BARNSTEEN"), m.benaming("cgm", Designatable.enumerationValue("Vondstsoort", "barnsteen")));
        assertEquals(Optional.of("GEMELDE_VONDST"), m.benaming("cgm", Designatable.role("vondstaanmelding", "vondst")));
        assertEquals(Optional.of("premie per strandjutter"),
                m.benaming("handboek", Designatable.form("vondstbedrag van een jutter")));
        assertEquals(Optional.of("PREMIEBEREKENING"), m.benaming("cgm", Designatable.ruleGroup("premieberekening")));
        assertEquals(Optional.of(Designatable.parameter("premietarief")),
                m.namedElement("handboek", Designatable.Kind.PARAMETER, null, "Premie per kilogram"));
        assertEquals(23, m.designatable().size());
    }

    /** The emitter writes a declaration's designations in the declaration order of the vocabularies, however the
     *  annotations are ordered. */
    @Test void the_emitter_follows_the_order_of_the_vocabularies() {
        RegelSpraakModel m = Resolver.resolve("""
            Vocabulaire handboek
            Vocabulaire cgm
            Objecttype de Jutter (mv: Jutters) (cgm: JUTTER) (handboek: strandjutter)
                de naam Tekst;
            """);
        assertTrue(m.errors().isEmpty(), () -> m.errors().toString());
        String ir = IrEmitter.emit(m);
        assertTrue(ir.indexOf("\"strandjutter\"") < ir.indexOf("\"JUTTER\""), ir);
    }

    // ---------------- inertness: without the extension, the document it was ----------------

    /** The golden's source with every vocabulary line and annotation taken out emits what the golden's model emits with
     *  its side table cleared and the one warning an annotation drew taken out: the annotations add the side table and
     *  nothing else. Over the 99 older rgs goldens and the parity zone the same holds by gates 10 and 11. */
    @Test void without_the_extension_the_document_is_the_same() throws Exception {
        // a vocabulary line becomes an empty one, so every other line keeps the number its source records carry
        String plain = source().lines().map(l -> l.startsWith("Vocabulaire ") ? ""
                        : l.replaceAll(" \\((handboek|cgm): [^)]*\\)", ""))
                .reduce("", (a, b) -> a + b + "\n");
        assertFalse(plain.contains("handboek") || plain.contains("cgm"), plain);
        RegelSpraakModel without = Resolver.resolve(plain);
        assertTrue(without.errors().isEmpty(), () -> without.errors().toString());
        String ir = IrEmitter.emit(without);
        assertFalse(ir.contains("vocabularies") || ir.contains("designations"), ir);

        RegelSpraakModel with = Resolver.resolve(source());
        with.vocabularies.clear();
        with.designations.clear();
        assertTrue(with.diagnostics.removeIf(d -> d.message().endsWith("(WF-120)")));
        assertEquals(ir, IrEmitter.emit(with));
    }

    // ---------------- a designation never resolves ----------------

    private static final String HEAD = """
        Vocabulaire handboek

        Objecttype de Jutter (mv: Jutters) (bezield) (handboek: strandjutter)
            het vondstbedrag (handboek: totale premie) Numeriek (geheel getal);
            het tegoed (handboek: saldo) Numeriek (geheel getal);
            de toeslag (handboek: tegoed) Numeriek (geheel getal);
        """;

    @Test void a_designation_as_rule_target_is_an_undeclared_name() {
        String source = HEAD + """

            Regel totaal
                geldig altijd
                    De totale premie van een Jutter moet berekend worden als 5.
            """;
        error(source, "De totale premie", "regeldoel 'De totale premie van een Jutter': 'totale premie' is geen "
                + "gedeclareerd attribuut van Jutter (WF-022, IR-SPEC ch. 3.1)");
    }

    @Test void a_designation_in_a_reading_is_an_undeclared_name() {
        RegelSpraakModel m = Resolver.resolve(HEAD + """

            Regel saldo
                geldig altijd
                    Het tegoed van een Jutter moet berekend worden als zijn totale premie plus 1.
            """);
        assertTrue(m.diagnostics.stream().anyMatch(d -> d.severity() == Severity.WARNING
                && d.message().startsWith("onopgelost: de naam 'zijn totale premie' is geen gedeclareerde parameter")),
                () -> m.diagnostics.toString());
    }

    /** The designation 'gewicht' of the attribute 'toeslag' of Jutter spells the canonical name of an attribute of
     *  ANOTHER type, Vondst: a Jutter rule that writes 'gewicht' names nothing on Jutter and gets the existing refusal of
     *  an undeclared name, the designation resolving neither to 'toeslag' nor, across the scope, to Vondst's attribute. */
    @Test void a_designation_that_spells_the_name_of_another_type_does_not_resolve() {
        String source = """
            Vocabulaire handboek

            Objecttype de Jutter (mv: Jutters) (bezield)
                de toeslag (handboek: gewicht) Numeriek (geheel getal);

            Objecttype de Vondst (mv: Vondsten)
                het gewicht Numeriek (geheel getal);

            Regel gewicht
                geldig altijd
                    Het gewicht van een Jutter moet berekend worden als 5.
            """;
        error(source, "Het gewicht van een Jutter", "regeldoel 'Het gewicht van een Jutter': 'gewicht' is geen "
                + "gedeclareerd attribuut van Jutter (WF-022, IR-SPEC ch. 3.1)");
    }

    /** The designation 'tegoed' of the attribute 'toeslag' spells the canonical name of the attribute 'tegoed': a read of
     *  'tegoed' reads that attribute, never the one the designation belongs to. */
    @Test void a_designation_that_spells_another_name_reads_that_other() {
        RegelSpraakModel m = Resolver.resolve(HEAD + """

            Regel vondstbedrag
                geldig altijd
                    Het vondstbedrag van een Jutter moet berekend worden als zijn tegoed plus 1.
            """);
        assertTrue(m.errors().isEmpty(), () -> m.errors().toString());
        ResolvedRule r = m.resolvedRegels.get(0);
        Expr.Binary plus = (Expr.Binary) r.value();
        assertEquals("tegoed", ((Expr.AttributeReference) plus.left()).attribute());
    }

    // ---------------- the refusals, each on its line ----------------

    /** The ERROR {@code fragment} stands on the first line of {@code bron} that carries {@code merker}. */
    private static void error(String source, String merker, String fragment) {
        RegelSpraakModel m = Resolver.resolve(source);
        String[] regels = source.split("\n", -1);
        int regel = 0;
        for (int i = 0; i < regels.length; i++) if (regels[i].contains(merker)) { regel = i + 1; break; }
        assertTrue(regel > 0, "merker '" + merker + "' niet in de bron");
        final int r = regel;
        List<Diagnostic> faults = m.errors();
        assertTrue(faults.stream().anyMatch(d -> d.message().contains(fragment) && d.line() == r),
                () -> "verwacht op regel " + r + ": " + fragment + "\nkreeg: " + faults);
    }

    private static final String JUTTER = """
        Objecttype de Jutter (mv: Jutters) (bezield)
            de naam Tekst;
        """;

    @Test void the_head_of_a_vocabulary() {
        error("Vocabulaire mv\n" + JUTTER, "Vocabulaire mv",
                "vocabulaire 'mv': die naam is gereserveerd, want '(mv:' opent de meervoudsvorm van een naam");
        error("Vocabulaire handboek\nVocabulaire Handboek\n" + JUTTER, "Vocabulaire Handboek",
                "vocabulaire 'Handboek' is al gedeclareerd als 'handboek'; de namen van de vocabulaires zijn uniek, "
                        + "hoofdletterongevoelig (WF-116)");
        error("Vocabulaire cgm (standaard) (standaard)\n" + JUTTER, "Vocabulaire cgm",
                "vocabulaire 'cgm' draagt '(standaard)' twee keer");
        error("Vocabulaire cgm (soort: code)\n" + JUTTER, "Vocabulaire cgm",
                "vocabulaire 'cgm': '(soort: …)' is geen eigenschap van een vocabulaire; een vocabulairekop kent "
                        + "'(standaard)' en '(taal: <code>)'");
        error("Vocabulaire cgm (taal: nl) (taal: en)\n" + JUTTER, "Vocabulaire cgm",
                "vocabulaire 'cgm' draagt '(taal: …)' twee keer");
        error("Vocabulaire cgm (taal: nl NL)\n" + JUTTER, "Vocabulaire cgm",
                "vocabulaire 'cgm': de taalcode 'nl NL' is leeg of bevat witruimte; een taalcode is één woord (WF-116)");
        error("Vocabulaire cgm (standaard)\nVocabulaire wet (standaard)\n" + JUTTER, "Vocabulaire wet",
                "de vocabulaires 'cgm' en 'wet' zijn allebei het standaardvocabulaire; een document heeft er hooguit "
                        + "één (WF-116)");
    }

    @Test void the_form_of_a_designation() {
        error("Vocabulaire handboek\nObjecttype de Jutter (mv: Jutters) (handboek: strand\n    de naam Tekst;\n",
                "(handboek: strand", "de annotatie '(handboek: strand' bij het objecttype 'Jutter' is niet op haar "
                        + "regel gesloten; een benaming staat tussen haakjes op één regel");
        error("Vocabulaire handboek\nObjecttype de Jutter (mv: Jutters) (handboek: )\n    de naam Tekst;\n",
                "(handboek: )", "de benaming van het objecttype 'Jutter' in het vocabulaire 'handboek' is leeg; "
                        + "een benaming is tekst tussen de dubbele punt en het sluithaakje (WF-117)");
        error("Vocabulaire handboek\nObjecttype de Jutter (mv: Jutters) (handboek: strand (jutter)\n    de naam Tekst;\n",
                "(handboek: strand", "de benaming 'strand (jutter' van het objecttype 'Jutter' in het vocabulaire "
                        + "'handboek' bevat een haakje; een benaming is tekst zonder haakjes, want het sluithaakje sluit "
                        + "de annotatie (WF-117)");
        error("Vocabulaire handboek\nObjecttype de Jutter (mv: Jutters) (wet: jutter)\n    de naam Tekst;\n",
                "(wet: jutter)", "de benaming 'jutter' van het objecttype 'Jutter' noemt het vocabulaire 'wet', dat "
                        + "het document niet declareert ('Vocabulaire wet', WF-117)");
        error("Vocabulaire handboek\nObjecttype de Jutter (mv: Jutters) (handboek: jutter) (Handboek: strandjutter)\n"
                        + "    de naam Tekst;\n", "(handboek: jutter)",
                "Het objecttype 'Jutter' draagt twee benamingen in het vocabulaire 'handboek', 'jutter' en "
                        + "'strandjutter'; een declaratie heeft hooguit één benaming per vocabulaire (WF-117)");
    }

    @Test void no_designation_on_a_table_or_an_exception_block() {
        error("Vocabulaire handboek\n" + JUTTER + """
            Beslistabel naamtabel (handboek: tabel)
            | | de naam van een Jutter |
            |---|---|
            | 1 | "a" |
            """, "Beslistabel naamtabel", "beslistabel 'naamtabel' draagt een benaming, en een beslistabel heeft er "
                        + "geen: zij staat voor de regels van haar rijen");
        error("""
            Vocabulaire handboek
            Objecttype de Jutter (mv: Jutters) (bezield)
                is gerechtigd kenmerk (bijvoeglijk);
                de leeftijd Numeriek (geheel getal);
            Regel basis
                geldig altijd
                    Een Jutter is gerechtigd.
            Uitzondering op basis: jong (handboek: jonge jutter)
                geldig altijd
                    Een Jutter is niet gerechtigd
                    indien zijn leeftijd kleiner is dan 16.
            """, "Uitzondering op basis", "het blok 'jong' draagt een benaming, en een uitzonderings- of "
                        + "afwijkingsblok heeft er geen");
    }

    @Test void a_rule_that_becomes_no_named_form() {
        error("""
            Vocabulaire handboek
            Dagsoort de havendag (mv: havendagen);
            Regel havendagdefinitie (handboek: definitie havendag)
                geldig altijd
                    Een dag is een havendag
                    indien de dag uit de dag gelijk is aan 1.
            """, "Regel havendagdefinitie", "de regel 'havendagdefinitie' draagt een benaming, maar wordt op de wire "
                        + "geen benoemde vorm");
    }

    @Test void the_default_vocabulary_is_complete() {
        error("""
            Vocabulaire cgm (standaard)
            Objecttype de Jutter (mv: Jutters) (bezield) (cgm: JUTTER)
                de naam (cgm: NAAM) Tekst;
                de leeftijd Numeriek (geheel getal);
            """, "de leeftijd", "Het attribuut 'leeftijd' van 'Jutter' draagt geen benaming in het "
                        + "standaardvocabulaire 'cgm', dat elke declaratie noemt (WF-119)");
    }

    @Test void the_effective_names_are_unique_per_scope() {
        // two object types, the second's designation the first's name in another case
        error("""
            Vocabulaire handboek
            Objecttype de Jutter (mv: Jutters) (bezield)
                de naam Tekst;
            Objecttype de Vondst (mv: Vondsten) (handboek: jutter)
                het gewicht Numeriek (geheel getal);
            """, "Objecttype de Vondst", "Het objecttype 'Jutter' en het objecttype 'Vondst' hebben in het "
                        + "vocabulaire 'handboek' dezelfde effectieve naam 'jutter'");
        // an inherited attribute's canonical name, taken by a subtype's own attribute
        error("""
            Vocabulaire handboek
            Objecttype de Vondst (mv: Vondsten)
                het gewicht Numeriek (geheel getal);
            Objecttype de Wrakvondst is een Vondst (mv: Wrakvondsten)
                de diepte (handboek: Gewicht) Numeriek (geheel getal);
            """, "de diepte", "Het attribuut 'diepte' van 'Wrakvondst' en het attribuut 'gewicht' van 'Vondst' hebben "
                        + "in het vocabulaire 'handboek' dezelfde effectieve naam 'gewicht'");
        // two roles of one fact type
        error("""
            Vocabulaire handboek
            Objecttype de Jutter (mv: Jutters) (bezield)
                de naam Tekst;
            Objecttype de Vondst (mv: Vondsten)
                het gewicht Numeriek (geheel getal);
            Feittype vondstaanmelding
                de aanmelder (handboek: melder) Jutter
                de vondst (mv: vondsten) (handboek: Melder) Vondst
            Eén aanmelder heeft meerdere vondsten
            """, "de vondst (mv: vondsten)", "De rol 'aanmelder' van 'vondstaanmelding' en de rol 'vondst' van "
                        + "'vondstaanmelding' hebben in het vocabulaire 'handboek' dezelfde effectieve naam 'Melder'");
        // two values of one domain
        error("""
            Vocabulaire handboek
            Domein Vondstsoort is van het type Enumeratie
                'wrakhout' (handboek: hout)
                'drijfhout' (handboek: Hout)
            """, "'drijfhout'", "De enumeratiewaarde 'wrakhout' van 'Vondstsoort' en de enumeratiewaarde 'drijfhout' "
                        + "van 'Vondstsoort' hebben in het vocabulaire 'handboek' dezelfde effectieve naam 'Hout'");
        // the same designation in two scopes is no collision: an attribute of each of two object types
        RegelSpraakModel m = Resolver.resolve("""
            Vocabulaire handboek
            Objecttype de Jutter (mv: Jutters) (bezield)
                de naam (handboek: aanduiding) Tekst;
            Objecttype de Vondst (mv: Vondsten)
                de naam (handboek: aanduiding) Tekst;
            """);
        assertTrue(m.errors().isEmpty(), () -> m.errors().toString());
    }

    @Test void a_redundant_designation_is_a_warning() {
        RegelSpraakModel m = Resolver.resolve("""
            Vocabulaire handboek
            Vocabulaire cgm (standaard)
            Objecttype de Jutter (mv: Jutters) (bezield) (handboek: Jutter) (cgm: Jutter)
                de naam (handboek: Naam) (cgm: naam) Tekst;
            """);
        assertTrue(m.errors().isEmpty(), () -> m.errors().toString());
        List<Diagnostic> w = m.diagnostics.stream().filter(d -> d.severity() == Severity.WARNING).toList();
        // one warning: 'Jutter' in handboek; 'Naam' differs in case, and cgm is the default, where one is required
        assertEquals(1, w.size(), w::toString);
        assertEquals("de benaming 'Jutter' van het objecttype 'Jutter' in het vocabulaire 'handboek' spelt haar "
                + "canonieke naam en is overbodig: de effectieve naam is dezelfde zonder haar (WF-120)", w.get(0).message());
        assertEquals(3, w.get(0).line());
    }
}
