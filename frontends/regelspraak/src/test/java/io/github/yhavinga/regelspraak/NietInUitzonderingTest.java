package io.github.yhavinga.regelspraak;

import io.github.yhavinga.regelspraak.emit.IrEmitter;
import io.github.yhavinga.regelspraak.ir.Conditie;
import io.github.yhavinga.regelspraak.ir.Default;
import io.github.yhavinga.regelspraak.ir.Expr;
import io.github.yhavinga.regelspraak.ir.PrioritizedDefaults;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Diagnostic;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Severity;
import io.github.yhavinga.regelspraak.parser.RegelSpraak;
import io.github.yhavinga.regelspraak.parser.RegelSpraakLexer;
import io.github.yhavinga.regelspraak.resolver.Resolver;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Dialect §4, form 1 (maintainer decisions 1–3, 2026-09-23; grammar/DIVERGENCES.md §4): a negated characteristic
 * decision — {@code is niet X}, {@code heeft geen X}, {@code is geen X}, {@code is niet een X} — in a rule that
 * becomes a default. Measured on the frontend before this card: in an {@code Uitzondering op} the negation read as
 * part of the name and drew "schrijft een ander doel (… niet uitgesloten …)", in a plain {@code Regel} it went to
 * disk as an undeclared characteristic {@code niet uitgesloten} with no diagnostic, and {@code is niet een X} did
 * not parse as a result. The witnesses pin the admitted positions, the lowering to {@code besluit: false} on the
 * carrier's own target, the refusals, and the warning for a group that can never derive anything.
 */
class NietInUitzonderingTest {

    /** One subject with one characteristic of each kind, plus two grounds and a count to hang conditions on. */
    private static final String MODEL = """
        Objecttype de Jutter (mv: Jutters) (bezield)
            is uitgesloten kenmerk (bijvoeglijk);
            het recht op de penning kenmerk (bezittelijk);
            de vaste ingezetene kenmerk;
            is geschorst kenmerk (bijvoeglijk);
            is vergunning geweigerd kenmerk (bijvoeglijk);
            het aantal aangemelde vondsten Numeriek (niet-negatief geheel getal);
        """;

    private static Diagnostic enigeFout(RegelSpraakModel m) {
        List<Diagnostic> fouten = m.errors();
        assertEquals(1, fouten.size(), () -> "verwachtte precies één fout, kreeg: " + m.diagnostics);
        return fouten.get(0);
    }

    private static List<Boolean> besluiten(PrioritizedDefaults g) {
        return g.defaults().stream().map(Default::besluit).toList();
    }

    private static String familie(String basis, String uitzondering) {
        return MODEL + """

            Regel basis
                geldig altijd
                    %s
                    indien zijn aantal aangemelde vondsten groter of gelijk is aan 12.

            Uitzondering op basis: ontkenning
                geldig altijd
                    %s
                    indien hij geschorst is.
            """.formatted(basis, uitzondering);
    }

    // -------------------------------------------------------------------- admitted: the body of an Uitzondering op

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "bijvoeglijk is-niet        | Een Jutter is uitgesloten            | Een Jutter is niet uitgesloten            | uitgesloten",
            "bezittelijk heeft-geen     | Een Jutter heeft recht op de penning | Een Jutter heeft geen recht op de penning | recht op de penning",
            "zelfstandig is-geen        | Een Jutter is een vaste ingezetene   | Een Jutter is geen vaste ingezetene       | vaste ingezetene",
            "zelfstandig is-niet-een    | Een Jutter is een vaste ingezetene   | Een Jutter is niet een vaste ingezetene   | vaste ingezetene"
    })
    void een_ontkende_uitzondering_beslist_onwaar_op_het_doel_van_de_basis(String vorm, String basis,
                                                                          String uitzondering, String kenmerk) {
        RegelSpraakModel m = Resolver.resolve(familie(basis, uitzondering));
        assertTrue(m.diagnostics.isEmpty(), () -> vorm + ": onverwachte diagnostiek: " + m.diagnostics);
        assertEquals(1, m.prioritizedDefaults.size(), "één defaults-groep over basis en uitzondering");
        assertEquals(0, m.resolvedRegels.size(), "basis en uitzondering horen uit resolvedRegels gelift te zijn");
        PrioritizedDefaults g = m.prioritizedDefaults.get(0);
        assertEquals(kenmerk, g.kenmerk(), "de ontkenning schrijft het doel van de basis, niet 'niet …'/'geen …'");
        assertEquals(List.of(Boolean.FALSE, Boolean.TRUE), besluiten(g),
                "de uitzondering beslist onwaar boven de basis die waar beslist");
        String ir = IrEmitter.emit(m).replaceAll("\\s+", "");
        assertTrue(ir.contains("\"besluit\":false"), () -> "de wire draagt geen besluit false:\n" + ir);
    }

    // -------------------------------------------------------------------- admitted: the base of a chain (decision 2)

    @Test void een_ontkende_basisregel_is_toegestaan() {
        RegelSpraakModel m = Resolver.resolve(MODEL + """

            Regel penning zonder aanmelding
                geldig altijd
                    Een Jutter heeft geen recht op de penning.

            Uitzondering op penning zonder aanmelding: penning bij drempel
                geldig altijd
                    Een Jutter heeft recht op de penning
                    indien zijn aantal aangemelde vondsten groter of gelijk is aan 12.
            """);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        PrioritizedDefaults g = m.prioritizedDefaults.get(0);
        assertEquals("recht op de penning", g.kenmerk());
        assertEquals(List.of(Boolean.TRUE, Boolean.FALSE), besluiten(g), "de basis is de laagste default en beslist onwaar");
        assertInstanceOf(Conditie.Samengesteld.class, g.defaults().get(1).voorwaarde(),
                "een onvoorwaardelijke basis draagt de lege ALLE-rechtvaardiging, zoals elke onvoorwaardelijke basis");
    }

    // -------------------------------------------------------------------- admitted: a member of an unordered block

    @Test void een_ontkend_lid_van_een_ongeordend_blok_maakt_het_kenmerkconflict_schrijfbaar() {
        RegelSpraakModel m = Resolver.resolve(MODEL + """

            Uitzonderingen ongeordend:
            Regel uitgesloten bij weigering
                geldig altijd
                    Een Jutter is uitgesloten
                    indien hij vergunning geweigerd is.

            Regel niet uitgesloten bij schorsing
                geldig altijd
                    Een Jutter is niet uitgesloten
                    indien hij geschorst is.
            """);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        assertEquals(2, m.prioritizedDefaults.size(), "twee zustergroepen op één doel");
        for (PrioritizedDefaults g : m.prioritizedDefaults) {
            assertEquals("uitgesloten", g.kenmerk());
            assertTrue(g.ongeordend());
        }
        assertEquals(List.of(Boolean.TRUE), besluiten(m.prioritizedDefaults.get(0)));
        assertEquals(List.of(Boolean.FALSE), besluiten(m.prioritizedDefaults.get(1)));
    }

    // -------------------------------------------------------------------- refused: a plain Regel

    @ParameterizedTest
    @ValueSource(strings = {
            "Een Jutter is niet uitgesloten",
            "Een Jutter heeft geen recht op de penning",
            "Een Jutter is geen vaste ingezetene",
            "Een Jutter is niet een vaste ingezetene"
    })
    void een_ontkenning_in_een_vlakke_regel_weigert_gericht(String resultaat) {
        RegelSpraakModel m = Resolver.resolve(MODEL + """

            Regel vlak
                geldig altijd
                    %s
                    indien hij geschorst is.
            """.formatted(resultaat));
        Diagnostic d = enigeFout(m);
        assertTrue(d.message().startsWith("regel 'vlak'"), d.message());
        assertTrue(d.message().contains("§9.2") && d.message().contains("dialect §4"),
                () -> "de weigering noemt §9.2 en het dialect §4 niet: " + d.message());
        assertFalse(d.message().contains("ander doel"), "niet de misleidende doelmelding van meting 1");
        assertFalse(m.isOk(), "een ERROR houdt het document van de schijf");
    }

    @Test void een_vlakke_ontkenning_naast_een_defaults_groep_spreekt_alleen() {
        // The same target has a defaults group: only the negation refusal speaks, not WF-050's second-writer
        // message about a rule the author has to rewrite anyway.
        RegelSpraakModel m = Resolver.resolve(familie("Een Jutter is uitgesloten", "Een Jutter is uitgesloten") + """

            Regel vlak
                geldig altijd
                    Een Jutter is niet uitgesloten
                    indien hij vergunning geweigerd is.
            """);
        Diagnostic d = enigeFout(m);
        assertTrue(d.message().startsWith("regel 'vlak'") && d.message().contains("§9.2"), d.message());
    }

    // -------------------------------------------------------------------- refused: undeclared after the strip

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "Een Jutter is niet vervallen            | vervallen",
            "Een Jutter heeft geen recht op de munt  | recht op de munt",
            "Een Jutter is niet een vaste gast       | vaste gast"
    })
    void een_ontkend_ongedeclareerd_kenmerk_weigert(String resultaat, String kenmerk) {
        RegelSpraakModel m = Resolver.resolve(familie("Een Jutter is uitgesloten", resultaat));
        Diagnostic d = enigeFout(m);
        assertTrue(d.message().contains("ontkent geen kenmerk van Jutter")
                        && d.message().contains("'" + kenmerk + "' is niet gedeclareerd"),
                () -> "de weigering noemt het ongedeclareerde kenmerk niet: " + d.message());
        assertFalse(d.message().contains("ander doel"), "niet de misleidende doelmelding van meting 1");
    }

    @Test void een_gedeclareerd_kenmerk_dat_met_niet_begint_bindt_positief() {
        // Verbatim first: a declared 'niet erkend' binds itself, and is never read as the negation of 'erkend'.
        RegelSpraakModel m = Resolver.resolve("""
            Objecttype de Jutter (mv: Jutters) (bezield)
                is erkend kenmerk (bijvoeglijk);
                is niet erkend kenmerk (bijvoeglijk);
                is geschorst kenmerk (bijvoeglijk);

            Regel vlak
                geldig altijd
                    Een Jutter is niet erkend
                    indien hij geschorst is.
            """);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        assertEquals(1, m.resolvedRegels.size());
        Expr.AttribuutRef doel = (Expr.AttribuutRef) m.resolvedRegels.get(0).doel();
        assertEquals("niet erkend", doel.attribuut());
    }

    // -------------------------------------------------------------------- no negated gelijkstelling

    @Test void een_gelijkstelling_op_onwaar_blijft_een_booleaanse_waarde() {
        // An attribute has no polarity: 'moet gesteld worden op onwaar' is a value, lowered as the default's
        // consequentie, never as a besluit.
        RegelSpraakModel m = Resolver.resolve("""
            Objecttype de Jutter (mv: Jutters) (bezield)
                de penningwaardigheid Boolean;
                is geschorst kenmerk (bijvoeglijk);

            Regel waardigheid
                geldig altijd
                    De penningwaardigheid van een Jutter moet gesteld worden op waar.

            Uitzondering op waardigheid: geen waardigheid bij schorsing
                geldig altijd
                    De penningwaardigheid van een Jutter moet gesteld worden op onwaar
                    indien hij geschorst is.
            """);
        assertTrue(m.errors().isEmpty(), () -> "onverwachte fouten: " + m.errors());
        PrioritizedDefaults g = m.prioritizedDefaults.get(0);
        assertEquals("penningwaardigheid", g.attribuut());
        assertNull(g.kenmerk());
        for (Default d : g.defaults()) assertNull(d.besluit(), "een attribuutdefault draagt geen besluit");
        assertInstanceOf(Expr.Literal.class, g.defaults().get(0).consequentie());
        assertEquals("onwaar", ((Expr.Literal) g.defaults().get(0).consequentie()).waarde());
    }

    @Test void niet_in_een_attribuutnaam_is_geen_ontkenning() {
        RegelSpraakModel m = Resolver.resolve("""
            Objecttype de Jutter (mv: Jutters) (bezield)
                de penningwaardigheid Boolean;

            Regel waardigheid
                geldig altijd
                    De niet penningwaardigheid van een Jutter moet gesteld worden op waar.
            """);
        Diagnostic d = enigeFout(m);
        assertTrue(d.message().contains("'niet penningwaardigheid' is geen gedeclareerd attribuut van Jutter"),
                d.message());
    }

    // -------------------------------------------------------------------- a group that never derives anything

    @Test void een_groep_die_alleen_onwaar_beslist_waarschuwt() {
        RegelSpraakModel m = Resolver.resolve(MODEL + """

            Regel niet uitgesloten
                geldig altijd
                    Een Jutter is niet uitgesloten.

            Uitzondering op niet uitgesloten: niet uitgesloten bij schorsing
                geldig altijd
                    Een Jutter is niet uitgesloten
                    indien hij geschorst is.
            """);
        assertTrue(m.isOk(), () -> "welgevormd, dus geen fout: " + m.errors());
        assertEquals(1, m.diagnostics.size(), () -> "verwachtte één waarschuwing: " + m.diagnostics);
        Diagnostic w = m.diagnostics.get(0);
        assertEquals(Severity.WARNING, w.severity());
        assertTrue(w.message().contains("wordt nooit afgeleid") && w.message().contains("Jutter.uitgesloten"),
                w.message());
        assertTrue(w.line() > 0, "de waarschuwing staat bij het blok");
        // WF-113 (a): the WARNING rides the wire; the emitter refuses anything else.
        assertTrue(IrEmitter.emit(m).contains("wordt nooit afgeleid"));
    }

    @Test void een_ongeordend_blok_dat_alleen_onwaar_beslist_waarschuwt_eenmaal() {
        RegelSpraakModel m = Resolver.resolve(MODEL + """

            Uitzonderingen ongeordend:
            Regel niet uitgesloten bij weigering
                geldig altijd
                    Een Jutter is niet uitgesloten
                    indien hij vergunning geweigerd is.

            Regel niet uitgesloten bij schorsing
                geldig altijd
                    Een Jutter is niet uitgesloten
                    indien hij geschorst is.
            """);
        assertTrue(m.isOk(), () -> "welgevormd, dus geen fout: " + m.errors());
        assertEquals(1, m.diagnostics.size(), () -> "één waarschuwing per doel, niet per zustergroep: " + m.diagnostics);
        assertTrue(m.diagnostics.get(0).message().contains("wordt nooit afgeleid"), m.diagnostics.get(0).message());
    }

    // -------------------------------------------------------------------- the register lint (Tabel 16)

    @Test void is_geen_op_een_bezittelijk_kenmerk_krijgt_de_registerwaarschuwing() {
        RegelSpraakModel m = Resolver.resolve(familie("Een Jutter heeft recht op de penning",
                "Een Jutter is geen recht op de penning"));
        assertTrue(m.isOk(), () -> "de lowering is gelijk, dus geen fout: " + m.errors());
        assertEquals(1, m.diagnostics.size(), () -> "verwachtte één waarschuwing: " + m.diagnostics);
        assertTrue(m.diagnostics.get(0).message().contains("Tabel 16 vereist 'heeft'"), m.diagnostics.get(0).message());
        assertEquals(List.of(Boolean.FALSE, Boolean.TRUE), besluiten(m.prioritizedDefaults.get(0)));
    }

    // -------------------------------------------------------------------- the scenario layer parses the blocks

    @Test void een_testdocument_aanvaardt_de_uitzonderingsvormen() {
        String src = familie("Een Jutter is uitgesloten", "Een Jutter is niet uitgesloten") + """

            Uitzonderingen ongeordend:
            Regel penning bij drempel
                geldig altijd
                    Een Jutter heeft recht op de penning
                    indien zijn aantal aangemelde vondsten groter of gelijk is aan 12.
            """;
        RegelSpraak parser = new RegelSpraak(new CommonTokenStream(new RegelSpraakLexer(CharStreams.fromString(src))));
        List<String> fouten = new ArrayList<>();
        parser.removeErrorListeners();
        parser.addErrorListener(new BaseErrorListener() {
            @Override public void syntaxError(Recognizer<?, ?> r, Object sym, int line, int col, String msg,
                                              RecognitionException e) { fouten.add(line + ": " + msg); }
        });
        RegelSpraak.TestDocumentContext doc = parser.testDocument();
        assertTrue(fouten.isEmpty(), () -> "syntaxfouten: " + fouten);
        assertEquals(1, doc.children.stream().filter(c -> c instanceof RegelSpraak.UitzonderingRegelContext).count());
        assertEquals(1, doc.children.stream().filter(c -> c instanceof RegelSpraak.UitzonderingGroepContext).count());
    }
}
