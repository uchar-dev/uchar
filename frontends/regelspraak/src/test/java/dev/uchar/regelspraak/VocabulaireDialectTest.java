package dev.uchar.regelspraak;

import dev.uchar.regelspraak.emit.IrEmitter;
import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.ir.ResolvedRegel;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Benoembaar;
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
class VocabulaireDialectTest {

    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden", "jutterswet-vocabulaires");

    private static String bron() throws Exception { return Files.readString(GOLDEN.resolve("bron.rgs")); }

    // ---------------- the tokens and the grammar ----------------

    private static List<String> tokens(String bron) {
        RegelSpraakLexer lexer = new RegelSpraakLexer(CharStreams.fromString(bron));
        List<String> uit = new ArrayList<>();
        for (Token t : lexer.getAllTokens())
            if (t.getChannel() == Token.DEFAULT_CHANNEL) uit.add(RegelSpraakLexer.VOCABULARY.getSymbolicName(t.getType()));
        return uit;
    }

    private static int syntaxfouten(String bron) {
        List<String> meldingen = new ArrayList<>();
        BaseErrorListener l = new BaseErrorListener() {
            @Override public void syntaxError(Recognizer<?, ?> r, Object sym, int line, int col, String msg,
                                              RecognitionException e) {
                meldingen.add(line + ":" + col + " " + msg);
            }
        };
        RegelSpraakLexer lexer = new RegelSpraakLexer(CharStreams.fromString(bron));
        lexer.removeErrorListeners(); lexer.addErrorListener(l);
        RegelSpraak parser = new RegelSpraak(new CommonTokenStream(lexer));
        parser.removeErrorListeners(); parser.addErrorListener(l);
        ParseTree boom = parser.regelSpraakDocument();
        return meldingen.size() + foutknopen(boom);
    }

    private static int foutknopen(ParseTree t) {
        if (t instanceof ErrorNode) return 1;
        int n = 0;
        for (int i = 0; i < t.getChildCount(); i++) n += foutknopen(t.getChild(i));
        return n;
    }

    @Test void de_tokens_en_hun_grenzen() {
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

    @Test void elke_positie_parseert_en_de_volgorde_ligt_vast() {
        assertEquals(0, syntaxfouten("""
            Vocabulaire handboek
            Vocabulaire cgm (standaard) (taal: nl)
            Vocabulaire wet (taal: nl) (standaard)
            Objecttype de Jutter (mv: Jutters) (bezield) (handboek: Jut01) (cgm: JUTTER)
                het vondstnummer (mv: vondstnummers) (handboek: nummer vondst) (cgm: VONDST_NR) Numeriek (geheel getal);
                is bergingsplichtig (cgm: BERGPLICHT) kenmerk (bijvoeglijk);
                de strandpas (cgm: PAS) kenmerk (bezittelijk);
            """));
        // the annotations close the name group: behind the plural and (bezield), never before them
        assertNotEquals(0, syntaxfouten("Objecttype de Jutter (handboek: Jut01) (mv: Jutters)\n"));
        assertNotEquals(0, syntaxfouten("Objecttype de Jutter (mv: Jutters) (handboek: Jut01) (bezield)\n"));
        assertNotEquals(0, syntaxfouten("Objecttype de Vondst\n    het gewicht Numeriek (geheel getal) (cgm: GEWICHT);\n"));
    }

    // ---------------- the side table ----------------

    @Test void de_golden_vult_de_zijtafel() throws Exception {
        RegelSpraakModel m = Resolver.resolve(bron());
        assertTrue(m.errors().isEmpty(), () -> m.errors().toString());
        assertEquals(List.of(new RegelSpraakModel.Vocabulaire("handboek", false, "nl"),
                new RegelSpraakModel.Vocabulaire("cgm", true, "nl")), m.vocabulaires);
        assertEquals(Optional.of("totale premie"), m.benaming("handboek", Benoembaar.attribuut("Jutter", "vondstbedrag")));
        assertEquals(Optional.of("BARNSTEEN"), m.benaming("cgm", Benoembaar.enumeratiewaarde("Vondstsoort", "barnsteen")));
        assertEquals(Optional.of("GEMELDE_VONDST"), m.benaming("cgm", Benoembaar.rol("vondstaanmelding", "vondst")));
        assertEquals(Optional.of("premie per strandjutter"),
                m.benaming("handboek", Benoembaar.vorm("vondstbedrag van een jutter")));
        assertEquals(Optional.of("PREMIEBEREKENING"), m.benaming("cgm", Benoembaar.regelgroep("premieberekening")));
        assertEquals(Optional.of(Benoembaar.parameter("premietarief")),
                m.benoemd("handboek", Benoembaar.Soort.PARAMETER, null, "Premie per kilogram"));
        assertEquals(23, m.benoembaar().size());
    }

    /** The emitter writes a declaration's designations in the declaration order of the vocabularies, however the
     *  annotations are ordered. */
    @Test void de_emitter_volgt_de_volgorde_van_de_vocabulaires() {
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
    @Test void zonder_de_extensie_is_het_document_hetzelfde() throws Exception {
        // a vocabulary line becomes an empty one, so every other line keeps the number its source records carry
        String kaal = bron().lines().map(l -> l.startsWith("Vocabulaire ") ? ""
                        : l.replaceAll(" \\((handboek|cgm): [^)]*\\)", ""))
                .reduce("", (a, b) -> a + b + "\n");
        assertFalse(kaal.contains("handboek") || kaal.contains("cgm"), kaal);
        RegelSpraakModel zonder = Resolver.resolve(kaal);
        assertTrue(zonder.errors().isEmpty(), () -> zonder.errors().toString());
        String ir = IrEmitter.emit(zonder);
        assertFalse(ir.contains("vocabularies") || ir.contains("designations"), ir);

        RegelSpraakModel met = Resolver.resolve(bron());
        met.vocabulaires.clear();
        met.benamingen.clear();
        assertTrue(met.diagnostics.removeIf(d -> d.message().endsWith("(WF-120)")));
        assertEquals(ir, IrEmitter.emit(met));
    }

    // ---------------- a designation never resolves ----------------

    private static final String KOP = """
        Vocabulaire handboek

        Objecttype de Jutter (mv: Jutters) (bezield) (handboek: strandjutter)
            het vondstbedrag (handboek: totale premie) Numeriek (geheel getal);
            het tegoed (handboek: saldo) Numeriek (geheel getal);
            de toeslag (handboek: tegoed) Numeriek (geheel getal);
        """;

    @Test void een_benaming_als_regeldoel_is_een_ongedeclareerde_naam() {
        String bron = KOP + """

            Regel totaal
                geldig altijd
                    De totale premie van een Jutter moet berekend worden als 5.
            """;
        fout(bron, "De totale premie", "regeldoel 'De totale premie van een Jutter': 'totale premie' is geen "
                + "gedeclareerd attribuut van Jutter (WF-022, IR-SPEC ch. 3.1)");
    }

    @Test void een_benaming_in_een_lezing_is_een_ongedeclareerde_naam() {
        RegelSpraakModel m = Resolver.resolve(KOP + """

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
    @Test void een_benaming_die_de_naam_van_een_ander_type_spelt_lost_niet_op() {
        String bron = """
            Vocabulaire handboek

            Objecttype de Jutter (mv: Jutters) (bezield)
                de toeslag (handboek: gewicht) Numeriek (geheel getal);

            Objecttype de Vondst (mv: Vondsten)
                het gewicht Numeriek (geheel getal);

            Regel gewicht
                geldig altijd
                    Het gewicht van een Jutter moet berekend worden als 5.
            """;
        fout(bron, "Het gewicht van een Jutter", "regeldoel 'Het gewicht van een Jutter': 'gewicht' is geen "
                + "gedeclareerd attribuut van Jutter (WF-022, IR-SPEC ch. 3.1)");
    }

    /** The designation 'tegoed' of the attribute 'toeslag' spells the canonical name of the attribute 'tegoed': a read of
     *  'tegoed' reads that attribute, never the one the designation belongs to. */
    @Test void een_benaming_die_een_andere_naam_spelt_leest_die_andere() {
        RegelSpraakModel m = Resolver.resolve(KOP + """

            Regel vondstbedrag
                geldig altijd
                    Het vondstbedrag van een Jutter moet berekend worden als zijn tegoed plus 1.
            """);
        assertTrue(m.errors().isEmpty(), () -> m.errors().toString());
        ResolvedRegel r = m.resolvedRegels.get(0);
        Expr.Binair plus = (Expr.Binair) r.waarde();
        assertEquals("tegoed", ((Expr.AttribuutRef) plus.links()).attribuut());
    }

    // ---------------- the refusals, each on its line ----------------

    /** The ERROR {@code fragment} stands on the first line of {@code bron} that carries {@code merker}. */
    private static void fout(String bron, String merker, String fragment) {
        RegelSpraakModel m = Resolver.resolve(bron);
        String[] regels = bron.split("\n", -1);
        int regel = 0;
        for (int i = 0; i < regels.length; i++) if (regels[i].contains(merker)) { regel = i + 1; break; }
        assertTrue(regel > 0, "merker '" + merker + "' niet in de bron");
        final int r = regel;
        List<Diagnostic> fouten = m.errors();
        assertTrue(fouten.stream().anyMatch(d -> d.message().contains(fragment) && d.line() == r),
                () -> "verwacht op regel " + r + ": " + fragment + "\nkreeg: " + fouten);
    }

    private static final String JUTTER = """
        Objecttype de Jutter (mv: Jutters) (bezield)
            de naam Tekst;
        """;

    @Test void de_kop_van_een_vocabulaire() {
        fout("Vocabulaire mv\n" + JUTTER, "Vocabulaire mv",
                "vocabulaire 'mv': die naam is gereserveerd, want '(mv:' opent de meervoudsvorm van een naam");
        fout("Vocabulaire handboek\nVocabulaire Handboek\n" + JUTTER, "Vocabulaire Handboek",
                "vocabulaire 'Handboek' is al gedeclareerd als 'handboek'; de namen van de vocabulaires zijn uniek, "
                        + "hoofdletterongevoelig (WF-116)");
        fout("Vocabulaire cgm (standaard) (standaard)\n" + JUTTER, "Vocabulaire cgm",
                "vocabulaire 'cgm' draagt '(standaard)' twee keer");
        fout("Vocabulaire cgm (soort: code)\n" + JUTTER, "Vocabulaire cgm",
                "vocabulaire 'cgm': '(soort: …)' is geen eigenschap van een vocabulaire; een vocabulairekop kent "
                        + "'(standaard)' en '(taal: <code>)'");
        fout("Vocabulaire cgm (taal: nl) (taal: en)\n" + JUTTER, "Vocabulaire cgm",
                "vocabulaire 'cgm' draagt '(taal: …)' twee keer");
        fout("Vocabulaire cgm (taal: nl NL)\n" + JUTTER, "Vocabulaire cgm",
                "vocabulaire 'cgm': de taalcode 'nl NL' is leeg of bevat witruimte; een taalcode is één woord (WF-116)");
        fout("Vocabulaire cgm (standaard)\nVocabulaire wet (standaard)\n" + JUTTER, "Vocabulaire wet",
                "de vocabulaires 'cgm' en 'wet' zijn allebei het standaardvocabulaire; een document heeft er hooguit "
                        + "één (WF-116)");
    }

    @Test void de_vorm_van_een_benaming() {
        fout("Vocabulaire handboek\nObjecttype de Jutter (mv: Jutters) (handboek: strand\n    de naam Tekst;\n",
                "(handboek: strand", "de annotatie '(handboek: strand' bij het objecttype 'Jutter' is niet op haar "
                        + "regel gesloten; een benaming staat tussen haakjes op één regel");
        fout("Vocabulaire handboek\nObjecttype de Jutter (mv: Jutters) (handboek: )\n    de naam Tekst;\n",
                "(handboek: )", "de benaming van het objecttype 'Jutter' in het vocabulaire 'handboek' is leeg; "
                        + "een benaming is tekst tussen de dubbele punt en het sluithaakje (WF-117)");
        fout("Vocabulaire handboek\nObjecttype de Jutter (mv: Jutters) (handboek: strand (jutter)\n    de naam Tekst;\n",
                "(handboek: strand", "de benaming 'strand (jutter' van het objecttype 'Jutter' in het vocabulaire "
                        + "'handboek' bevat een haakje; een benaming is tekst zonder haakjes, want het sluithaakje sluit "
                        + "de annotatie (WF-117)");
        fout("Vocabulaire handboek\nObjecttype de Jutter (mv: Jutters) (wet: jutter)\n    de naam Tekst;\n",
                "(wet: jutter)", "de benaming 'jutter' van het objecttype 'Jutter' noemt het vocabulaire 'wet', dat "
                        + "het document niet declareert ('Vocabulaire wet', WF-117)");
        fout("Vocabulaire handboek\nObjecttype de Jutter (mv: Jutters) (handboek: jutter) (Handboek: strandjutter)\n"
                        + "    de naam Tekst;\n", "(handboek: jutter)",
                "Het objecttype 'Jutter' draagt twee benamingen in het vocabulaire 'handboek', 'jutter' en "
                        + "'strandjutter'; een declaratie heeft hooguit één benaming per vocabulaire (WF-117)");
    }

    @Test void geen_benaming_op_een_tabel_of_een_uitzonderingsblok() {
        fout("Vocabulaire handboek\n" + JUTTER + """
            Beslistabel naamtabel (handboek: tabel)
            | | de naam van een Jutter |
            |---|---|
            | 1 | "a" |
            """, "Beslistabel naamtabel", "beslistabel 'naamtabel' draagt een benaming, en een beslistabel heeft er "
                        + "geen: zij staat voor de regels van haar rijen");
        fout("""
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

    @Test void een_regel_die_geen_benoemde_vorm_wordt() {
        fout("""
            Vocabulaire handboek
            Dagsoort de havendag (mv: havendagen);
            Regel havendagdefinitie (handboek: definitie havendag)
                geldig altijd
                    Een dag is een havendag
                    indien de dag uit de dag gelijk is aan 1.
            """, "Regel havendagdefinitie", "de regel 'havendagdefinitie' draagt een benaming, maar wordt op de wire "
                        + "geen benoemde vorm");
    }

    @Test void het_standaardvocabulaire_is_volledig() {
        fout("""
            Vocabulaire cgm (standaard)
            Objecttype de Jutter (mv: Jutters) (bezield) (cgm: JUTTER)
                de naam (cgm: NAAM) Tekst;
                de leeftijd Numeriek (geheel getal);
            """, "de leeftijd", "Het attribuut 'leeftijd' van 'Jutter' draagt geen benaming in het "
                        + "standaardvocabulaire 'cgm', dat elke declaratie noemt (WF-119)");
    }

    @Test void de_effectieve_namen_zijn_uniek_per_reikwijdte() {
        // two object types, the second's designation the first's name in another case
        fout("""
            Vocabulaire handboek
            Objecttype de Jutter (mv: Jutters) (bezield)
                de naam Tekst;
            Objecttype de Vondst (mv: Vondsten) (handboek: jutter)
                het gewicht Numeriek (geheel getal);
            """, "Objecttype de Vondst", "Het objecttype 'Jutter' en het objecttype 'Vondst' hebben in het "
                        + "vocabulaire 'handboek' dezelfde effectieve naam 'jutter'");
        // an inherited attribute's canonical name, taken by a subtype's own attribute
        fout("""
            Vocabulaire handboek
            Objecttype de Vondst (mv: Vondsten)
                het gewicht Numeriek (geheel getal);
            Objecttype de Wrakvondst is een Vondst (mv: Wrakvondsten)
                de diepte (handboek: Gewicht) Numeriek (geheel getal);
            """, "de diepte", "Het attribuut 'diepte' van 'Wrakvondst' en het attribuut 'gewicht' van 'Vondst' hebben "
                        + "in het vocabulaire 'handboek' dezelfde effectieve naam 'gewicht'");
        // two roles of one fact type
        fout("""
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
        fout("""
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

    @Test void een_overbodige_benaming_is_een_waarschuwing() {
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
