package dev.uchar.regelspraak.ir;

import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The structured {@link Source} on a resolved form carries file/line/column back to the source,
 * and a {@code // Bron:} statuutannotatie directly above a rule is retrievable from its
 * {@link ResolvedRule}. Byte-identity of the wire (the source snippet) is proven elsewhere
 * (the emitter goldens); here we prove the ADDED coordinates and annotations.
 */
class SourceTest {

    static final String SRC = """
        Objecttype de Som
            het bedrag Numeriek (geheel getal);
            de uitkomst Numeriek (geheel getal);
        // Bron: art. 3 Jutterswet
        Regel bereken uitkomst
            geldig altijd
                De uitkomst van een Som moet gesteld worden op het bedrag van een Som.
        """;

    private static int lineOf(String src, String needle) {
        int idx = src.indexOf(needle);
        assertTrue(idx >= 0, () -> "needle niet gevonden: " + needle);
        return 1 + (int) src.substring(0, idx).chars().filter(c -> c == '\n').count();
    }

    @Test void resolvedFormCarriesFileLineColumn() {
        RegelSpraakModel m = Resolver.resolve(SRC, "Jutterswet.rgs");
        assertTrue(m.errors().isEmpty(), () -> "onverwachte fouten: " + m.errors());
        assertEquals(1, m.resolvedRegels.size());

        ResolvedRule rr = m.resolvedRegels.get(0);
        Source source = rr.target().source();
        assertNotNull(source, "doel draagt geen Bron");
        assertEquals("Jutterswet.rgs", source.file(), "de bestandsnaam moet doorgegeven zijn");
        assertEquals(lineOf(SRC, "De uitkomst van een Som moet"), source.line(), "regelnummer van het doel");
        assertTrue(source.column() >= 0, "kolom is 0-gebaseerd");
        assertTrue(source.snippet().contains("uitkomst"), () -> "snippet: " + source.snippet());
        // toString() levert de snippet (minimaliseert de diff op plekken die de bron als tekst gebruiken)
        assertEquals(source.snippet(), source.toString());
    }

    @Test void sourceAnnotationRetrievableFromResolvedRule() {
        RegelSpraakModel m = Resolver.resolve(SRC, "Jutterswet.rgs");
        assertTrue(m.errors().isEmpty(), () -> "onverwachte fouten: " + m.errors());

        ResolvedRule rr = m.resolvedRegels.get(0);
        assertTrue(rr.statuteAnnotations().contains("art. 3 Jutterswet"),
                () -> "verwachtte de // Bron:-annotatie boven de regel, kreeg: " + rr.statuteAnnotations());
    }

    @Test void noSourceAnnotationYieldsEmptyList() {
        // A rule with no // Bron: comment above it carries an empty (never null) annotatielijst.
        String src = """
            Objecttype de Som
                het bedrag Numeriek (geheel getal);
                de uitkomst Numeriek (geheel getal);
            Regel bereken uitkomst
                geldig altijd
                    De uitkomst van een Som moet gesteld worden op het bedrag van een Som.
            """;
        RegelSpraakModel m = Resolver.resolve(src, "Jutterswet.rgs");
        assertTrue(m.resolvedRegels.get(0).statuteAnnotations().isEmpty(), "geen annotatie -> lege lijst");
    }
}
