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
 * WF-037's COUNT-SOURCE arm ({@code spec/IR-SPEC.md} ch. 4.3, maintainer decision 2026-09-23): a
 * count MUST carry an element type, unless its one argument is an instance-set union.
 *
 * <p><b>The route was live.</b> {@code aantalFunc} takes any {@code primaireExpressie}; when the
 * operand names no role or object type the element derivation yields null, and unless the whole
 * phrase is a declared parameter the count was emitted anyway. Measured 2026-09-23 before the arm:
 * every operand below emitted an {@code AANTAL} with {@code elementObjectType: null}, and none was
 * blocked — all but the last with {@code diagnostics: []}, the last with a WARNING that does not
 * block. The verifier now rejects those documents; the reference interpreter used to answer them
 * with the OPERAND counted.
 *
 * <p>The grammar is untouched, as with {@link CountEnumeratedTest}: the fence stands where the
 * element is derived.
 */
class CountWithoutSourceTest {

    static final String MODEL = """
        Parameter de drempel : Numeriek (getal met 2 decimalen)
        Parameter het aantal werkdagen : Numeriek (niet-negatief geheel getal)

        Objecttype de Jutter (mv: Jutters) (bezield)
            het aantal gewogen posten Numeriek (niet-negatief geheel getal);
            de gewogen vangst Numeriek (getal met 2 decimalen);

        Objecttype de Vondst (mv: Vondsten)
            het gewicht Numeriek (getal met 2 decimalen);

        Feittype vondstaanmelding
            de aanmelder Jutter
            de vondst (mv: vondsten) Vondst
        Eén aanmelder heeft meerdere vondsten
        """;

    private static String regel(String expressie) {
        return """

            Regel aantal gewogen posten
                geldig altijd
                    Het aantal gewogen posten van een Jutter moet berekend worden als %s.
            """.formatted(expressie);
    }

    private static List<Diagnostic> sourceless(RegelSpraakModel m) {
        return m.diagnostics.stream().filter(d -> d.message().startsWith("telling zonder bron")).toList();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "het aantal gewogen vangst van de Jutter",          // an own attribute
            "het aantal de gewogen vangst van de Jutter",
            "het aantal (de gewogen vangst van de Jutter)",
            "het aantal 3",                                     // a literal
            "het aantal (3 plus 4)",                            // arithmetic
            "het aantal drempel",                               // a parameter not named 'aantal …'
            "het aantal de drempel",
            "het aantal Vondsten",                              // an object type's plural, bare
            "het aantal de Jutter"                              // unresolved, already WARNING before
    })
    void a_count_without_element_type_is_refused(String expressie) {
        RegelSpraakModel m = Resolver.resolve(MODEL + regel(expressie));
        List<Diagnostic> ds = sourceless(m);
        assertEquals(1, ds.size(), () -> "verwacht één bronloze-tellingmelding voor '" + expressie + "', kreeg: "
                + m.diagnostics);
        assertEquals(Severity.ERROR, ds.get(0).severity());
        assertTrue(ds.get(0).message().contains("the count is an INSTANCE fold and MUST carry an element type"),
                ds.get(0).message());
        // ERROR is what keeps the document off disk: irMode writes nothing when the model has one
        assertFalse(m.errors().isEmpty());
    }

    @Test void the_ordinary_count_stays_clean() {
        RegelSpraakModel m = Resolver.resolve(MODEL + regel("het aantal vondsten van de Jutter"));
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        Expr.FunctionCall f = (Expr.FunctionCall) m.resolvedRegels.get(0).value();
        assertEquals(Expr.FunctionKind.AANTAL, f.kind());
        assertNotNull(f.elementObjectType());
    }

    @Test void a_parameter_that_is_named_count_stays_a_parameter() {
        // the one route by which "het aantal …" legitimately binds no element: the whole phrase is a
        // declared parameter, read back with its article. It returns before the fence.
        RegelSpraakModel m = Resolver.resolve(MODEL + regel("het aantal werkdagen"));
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        assertInstanceOf(Expr.ParameterRef.class, m.resolvedRegels.get(0).value());
    }
}
