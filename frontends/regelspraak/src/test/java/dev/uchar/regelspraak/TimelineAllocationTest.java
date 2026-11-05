package dev.uchar.regelspraak;

import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** An allocation distributes one amount at one moment (RUNTIME-SEMANTICS ch. 4): one that names a time-dependent
 *  attribute, as its pot, a share, its rest, its weight, its maximum or its ordering key, is refused with RGS-378
 *  (IR-SPEC WF-128), a share for every month over a scalar pot as well. */
class TimelineAllocationTest {

    private static final String RULE = "verdeel de opbrengst van een berging";

    /** The document with " voor elke maand" after the declaration of {@code timed}, or none where it is null. */
    private static String document(String timed, String allocation) {
        String model = """
            Objecttype de Berging (mv: Bergingen)
                de opbrengst Numeriek (geheel getal)@opbrengst;
                de onverdeelde rest Numeriek (geheel getal)@onverdeelde rest;

            Objecttype de Berger (mv: Bergers) (bezield)
                het bergingsaandeel (mv: bergingsaandelen) Numeriek (geheel getal)@bergingsaandeel;
                het geborgen gewicht Numeriek (geheel getal)@geborgen gewicht;
                het bergingsmaximum Numeriek (geheel getal)@bergingsmaximum;
                de strandrang Numeriek (geheel getal)@strandrang;

            Feittype bergingsdeelname
                de berging Berging
                de berger (mv: bergers) Berger
            Eén berging heeft meerdere bergers

            Regel verdeel de opbrengst van een berging
                geldig altijd
                    De opbrengst van een berging wordt verdeeld over de bergingsaandelen van alle bergers van de berging, waarbij wordt verdeeld:
            """;
        for (String a : List.of("opbrengst", "onverdeelde rest", "bergingsaandeel", "geborgen gewicht", "bergingsmaximum",
                "strandrang"))
            model = model.replace("@" + a + ";", a.equals(timed) ? " voor elke maand;" : ";");
        return model + allocation + "        Als onverdeelde rest blijft de onverdeelde rest van de berging over.\n";
    }

    private static final String PROPORTIONAL = """
                    - naar rato van het geborgen gewicht,
                    - met een maximum van het bergingsmaximum,
                    - afgerond op 0 decimalen naar beneden.
            """;

    private static final String ORDERED = """
                    - op volgorde van afnemende strandrang,
                    - bij een even groot criterium in gelijke delen,
                    - afgerond op 0 decimalen naar beneden.
            """;

    private static List<String> errors(String source) {
        RegelSpraakModel m = Resolver.resolve(source);
        return m.errors().stream().map(d -> d.code() + " " + d.params()).toList();
    }

    private static String refusal(String attribute, String objectType) {
        Map<String, String> p = new java.util.LinkedHashMap<>();
        p.put("name", RULE);
        p.put("attribute", attribute);
        p.put("objectType", objectType);
        return "RGS-378 " + p;
    }

    @Test void a_scalar_allocation_passes() {
        assertEquals(List.of(), errors(document(null, PROPORTIONAL)));
        assertEquals(List.of(), errors(document(null, ORDERED)));
    }

    @Test void each_time_dependent_attribute_of_an_allocation_is_refused() {
        assertEquals(List.of(refusal("opbrengst", "Berging")), errors(document("opbrengst", PROPORTIONAL)));
        assertEquals(List.of(refusal("bergingsaandeel", "Berger")), errors(document("bergingsaandeel", PROPORTIONAL)));
        assertEquals(List.of(refusal("onverdeelde rest", "Berging")), errors(document("onverdeelde rest", PROPORTIONAL)));
        assertEquals(List.of(refusal("geborgen gewicht", "Berger")), errors(document("geborgen gewicht", PROPORTIONAL)));
        assertEquals(List.of(refusal("bergingsmaximum", "Berger")), errors(document("bergingsmaximum", PROPORTIONAL)));
        assertEquals(List.of(refusal("strandrang", "Berger")), errors(document("strandrang", ORDERED)));
    }
}
