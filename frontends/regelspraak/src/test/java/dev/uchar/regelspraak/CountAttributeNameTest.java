package dev.uchar.regelspraak;

import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.model.RegelSpraakModel.Severity;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * "het aantal &lt;naam&gt; van &lt;onderwerp&gt;" where "aantal &lt;naam&gt;" is a DECLARED
 * ATTRIBUTE of the subject's type. The source language derives the sentence two ways (§13.4.17.3
 * admits both a getalaggregatie and an attribuutvanonderwerp) and gives no syntactic tie-break; the
 * grammar takes the count, and the count has no instance source (WF-037). The frontend does NOT
 * reinterpret it as the attribute: the attribute reading is written without the article
 * (§13.3.2.6), and that is where it is read. What this pins is that the refusal names the collision
 * and the spelling that reads the attribute, instead of the generic "telling zonder bron" that
 * leaves the author guessing why a declared name is refused.
 */
class CountAttributeNameTest {

    static final String MODEL = """
        Objecttype de Jutter (mv: Jutters) (bezield)
            het aantal gewogen posten Numeriek (niet-negatief geheel getal);
            de gewogen vangst Numeriek (getal met 2 decimalen);
            de score Numeriek (niet-negatief geheel getal);
        """;

    private static String regel(String expressie) {
        return """

            Regel score
                geldig altijd
                    De score van een Jutter moet berekend worden als %s.
            """.formatted(expressie);
    }

    private static List<Diagnostic> fouten(RegelSpraakModel m) {
        return m.diagnostics.stream().filter(d -> d.severity() == Severity.ERROR).toList();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "het aantal gewogen posten van de Jutter",
            "het aantal gewogen posten van een Jutter",
            "(het aantal gewogen posten van de Jutter)"
    })
    void the_collision_with_a_declared_attribute_is_named(String expressie) {
        RegelSpraakModel m = Resolver.resolve(MODEL + regel(expressie));
        List<Diagnostic> ds = fouten(m);
        assertEquals(1, ds.size(), () -> "verwacht één fout voor '" + expressie + "', kreeg: " + m.diagnostics);
        String sentence = ds.get(0).message();
        // the collision in the Dutch context, the count without an element type in the verifier's WF-037 sentence
        String written = expressie.replace("(", "").replace(")", "");
        assertTrue(sentence.startsWith("telling zonder bron '" + written + "', waar 'aantal gewogen posten' een "
                + "gedeclareerd attribuut van Jutter is: "), sentence);
        assertTrue(sentence.contains("the count is an INSTANCE fold and MUST carry an element type"), sentence);
    }

    @Test void the_recommended_articleless_spelling_reads_the_attribute() {
        // the advice must be true: the article-less form resolves clean to the declared attribute
        RegelSpraakModel m = Resolver.resolve(MODEL + regel("aantal gewogen posten van de Jutter"));
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        Expr.AttribuutRef a = assertInstanceOf(Expr.AttribuutRef.class, m.resolvedRegels.get(0).waarde());
        assertEquals("aantal gewogen posten", a.attribuut());
    }

    @Test void without_a_count_attribute_the_general_sentence_stays() {
        // "gewogen vangst" is declared, "aantal gewogen vangst" is not: no collision to name
        RegelSpraakModel m = Resolver.resolve(MODEL + regel("het aantal gewogen vangst van de Jutter"));
        List<Diagnostic> ds = fouten(m);
        assertEquals(1, ds.size(), () -> "kreeg: " + m.diagnostics);
        assertTrue(ds.get(0).message().startsWith("telling zonder bron 'het aantal gewogen vangst van de Jutter': "),
                ds.get(0).message());
        assertFalse(ds.get(0).message().contains("gedeclareerd attribuut"), ds.get(0).message());
    }
}
