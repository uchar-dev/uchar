package dev.uchar.regelspraak;

import dev.uchar.regelspraak.emit.IrEmitter;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A quantifier over a written path of two relations or more — "ten minste één van de vondsten van de melder van de
 * Melding" — lowers to a {@code QuantifierFold} whose range is the {@code sourcePath} of the walk (IR-SPEC ch. 4.5,
 * WF-108 (c); the flat union of RUNTIME-SEMANTICS ch. 3.4b, shared with ch. 3.9a). Until the card
 * frontend-kwantor-over-een-pad (2026-10-20) the membership spelling declined with a WARNING and left a marker, and the
 * comparison and leeg/gevuld spellings were refused as a set reading outside a fold source.
 */
class QuantifierOverPathTest {

    /** R1 of the card: a Melding reaches its Vondsten through its melder, a to-one step and then a to-many one. */
    static final String R1 = """
        Objecttype de Jutter (mv: Jutters) (bezield)
        Objecttype de Vondst (mv: Vondsten)
            het gewicht Numeriek (geheel getal);
            is zwaar kenmerk (bijvoeglijk);
        Objecttype de Melding (mv: Meldingen)
            is dringend kenmerk (bijvoeglijk);
        Feittype JutterHeeftVondst
            de jutter Jutter
            de vondst (mv: vondsten) Vondst
        Eén jutter heeft meerdere vondsten
        Feittype MeldingVanJutter
            de melder Jutter
            de melding (mv: meldingen) Melding
        Eén melder heeft meerdere meldingen

        Regel zwaar
            geldig altijd
                Een Vondst is zwaar
                indien zijn gewicht groter is dan 10.

        Regel dringend
            geldig altijd
                Een Melding is dringend
                indien ten minste één van de vondsten van de melder van de Melding zwaar is.
        """;

    @Test void r1_lowers_to_a_quantifier_fold_over_the_path() {
        RegelSpraakModel m = Resolver.resolve(R1);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        String ir = IrEmitter.emit(m).replaceAll("\\s+", "");
        String fold = ir.substring(ir.indexOf("\"name\":\"dringend\""));
        assertTrue(fold.contains("\"condition\":{\"node\":\"QuantifierFold\",\"quantifier\":\"atLeast\",\"threshold\":1,"
                        + "\"elementType\":\"Vondst\",\"test\":{\"node\":\"CharacteristicCheck\",\"characteristic\":\"zwaar\""),
                () -> "R1 hoort een kwantorvouw met een kenmerktoets op de Vondst te worden:\n" + fold);
        assertTrue(fold.contains("\"sourcePath\":[{\"node\":\"RoleBinding\",\"factType\":\"MeldingVanJutter\",\"role\":\"melder\"},"
                        + "{\"node\":\"RoleBinding\",\"factType\":\"JutterHeeftVondst\",\"role\":\"vondst\"}]"),
                () -> "het bereik van R1 is het pad melder, vondst:\n" + fold);
        assertFalse(fold.contains("\"node\":\"Unresolved\""), () -> "R1 laat geen marker staan:\n" + fold);
    }

    /** A model with steps in every order: a Ploeg reaches its Jutters (to-many), a Jutter its Vondsten (to-many), a
     *  Vondst its Wegingen (to-many) and its Kist (to-one), a Melding its melder (to-one). */
    static final String MODEL = """
        Objecttype de Ploeg (mv: Ploegen)
            is zwaarbeladen kenmerk (bijvoeglijk);
        Objecttype de Jutter (mv: Jutters) (bezield)
            de leeftijd Numeriek (geheel getal);
            is kistplichtig kenmerk (bijvoeglijk);
        Objecttype de Vondst (mv: Vondsten)
            het gewicht Numeriek (geheel getal);
            is zwaar kenmerk (bijvoeglijk);
            is boven de norm kenmerk (bijvoeglijk);
            het wrakstuk kenmerk;
        Objecttype de Weging (mv: Wegingen)
            de weegdatum Datum in dagen;
        Objecttype de Kist (mv: Kisten)
            is vol kenmerk (bijvoeglijk);
        Objecttype de Melding (mv: Meldingen)
            de drempel Numeriek (geheel getal);
            is dringend kenmerk (bijvoeglijk);
            de voorrang kenmerk (bezittelijk);
        Feittype PloegHeeftJutter
            de ploeg Ploeg
            de jutter (mv: jutters) Jutter
        Eén ploeg heeft meerdere jutters
        Feittype JutterHeeftVondst
            de vinder Jutter
            de vondst (mv: vondsten) Vondst
        Eén vinder heeft meerdere vondsten
        Feittype VondstHeeftWeging
            de gewogen vondst Vondst
            de weging (mv: wegingen) Weging
        Eén gewogen vondst heeft meerdere wegingen
        Feittype KistBevatVondst
            de kist (mv: kisten) Kist
            de ingepakte vondst (mv: ingepakte vondsten) Vondst
        Eén kist bevat meerdere ingepakte vondsten
        Feittype MeldingVanJutter
            de melder Jutter
            de melding (mv: meldingen) Melding
        Eén melder doet meerdere meldingen
        """;

    private static final String MELDER = "{\"node\":\"RoleBinding\",\"factType\":\"MeldingVanJutter\",\"role\":\"melder\"}";
    private static final String VONDST = "{\"node\":\"RoleBinding\",\"factType\":\"JutterHeeftVondst\",\"role\":\"vondst\"}";

    /** The one rule of {@code rule} over {@link #MODEL}, emitted and compacted from its name on; no diagnostic. */
    private static String clean(String rule) {
        RegelSpraakModel m = Resolver.resolve(MODEL + rule);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        String ir = IrEmitter.emit(m).replaceAll("\\s+", "");
        return ir.substring(ir.indexOf("\"name\":\"t\""));
    }

    private static String rule(String subject, String condition) {
        return "Regel t\n    geldig altijd\n        Een " + subject + "\n        indien " + condition + ".\n";
    }

    /** Every quantifier, and every test the matrix takes: a characteristic, a negated one, a comparison, a leeg test. */
    @Test void every_quantifier_and_every_test_lowers_over_the_path() {
        String[][] forms = {
            {"alle vondsten van de melder van de Melding zwaar zijn", "\"quantifier\":\"all\"", "\"characteristic\":\"zwaar\",\"negated\":false"},
            {"geen van de vondsten van de melder van de Melding niet zwaar is", "\"quantifier\":\"none\"", "\"characteristic\":\"zwaar\",\"negated\":true"},
            {"ten minste twee van de vondsten van de melder van de Melding zwaar zijn", "\"quantifier\":\"atLeast\",\"threshold\":2", "\"characteristic\":\"zwaar\""},
            {"ten hoogste 3 van de vondsten van de melder van de Melding zwaar zijn", "\"quantifier\":\"atMost\",\"threshold\":3", "\"characteristic\":\"zwaar\""},
            {"precies één van de vondsten van de melder van de Melding zwaar is", "\"quantifier\":\"exactly\",\"threshold\":1", "\"characteristic\":\"zwaar\""},
            {"het gewicht van ten minste één van de vondsten van de melder van de Melding groter is dan 10", "\"quantifier\":\"atLeast\",\"threshold\":1", "\"test\":{\"node\":\"Comparison\",\"op\":\"greater\""},
            {"het gewicht van alle vondsten van de melder van de Melding leeg is", "\"quantifier\":\"all\"", "\"test\":{\"node\":\"Predicate\",\"kind\":\"empty\""},
            {"ten minste één van de vondsten van de melder van de Melding boven de norm is", "\"quantifier\":\"atLeast\"", "\"characteristic\":\"bovendenorm\""},
            {"ten minste één van de vondsten van de melder van de Melding een wrakstuk is", "\"quantifier\":\"atLeast\"", "\"characteristic\":\"wrakstuk\""},
        };
        for (String[] f : forms) {
            String ir = clean(rule("Melding is dringend", f[0]));
            assertTrue(ir.contains("\"condition\":{\"node\":\"QuantifierFold\"," + f[1]), () -> f[0] + ":\n" + ir);
            assertTrue(ir.contains(f[2]), () -> f[0] + ":\n" + ir);
            assertTrue(ir.contains("\"sourcePath\":[" + MELDER + "," + VONDST + "]"), () -> f[0] + ":\n" + ir);
        }
    }

    /** to-many then to-many, three steps (to-one, to-many, to-many), and to-many then to-one. */
    @Test void steps_in_every_order_lower_to_their_path() {
        String ir = clean(rule("Ploeg is zwaarbeladen", "het gewicht van alle vondsten van alle jutters van de Ploeg groter is dan 10"));
        assertTrue(ir.contains("\"sourcePath\":[{\"node\":\"RoleBinding\",\"factType\":\"PloegHeeftJutter\",\"role\":\"jutter\"},"
                + VONDST + "]"), ir);
        ir = clean(rule("Melding is dringend",
                "de weegdatum van ten minste één van de wegingen van alle vondsten van de melder van de Melding gevuld is"));
        assertTrue(ir.contains("\"elementType\":\"Weging\",\"test\":{\"node\":\"Predicate\",\"kind\":\"filled\""), ir);
        assertTrue(ir.contains("\"sourcePath\":[" + MELDER + "," + VONDST
                + ",{\"node\":\"RoleBinding\",\"factType\":\"VondstHeeftWeging\",\"role\":\"weging\"}]"), ir);
        ir = clean(rule("Jutter is kistplichtig", "ten minste twee van de kisten van de vondsten van de Jutter vol zijn"));
        assertTrue(ir.contains("\"elementType\":\"Kist\""), ir);
        assertTrue(ir.contains("\"sourcePath\":[" + VONDST
                + ",{\"node\":\"RoleBinding\",\"factType\":\"KistBevatVondst\",\"role\":\"kist\"}]"), ir);
    }

    /** The shape of bridge K6: the justification of an exception in a defaults group. */
    @Test void the_justification_of_an_exception_lowers_over_the_path() {
        RegelSpraakModel m = Resolver.resolve(MODEL + """
            Regel voorrang
                geldig altijd
                    Een Melding heeft voorrang.

            Uitzondering op voorrang: wrakstuk
                geldig altijd
                    Een Melding heeft geen voorrang
                    indien ten minste één van de vondsten van de melder van de Melding een wrakstuk is.
            """);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        String ir = IrEmitter.emit(m).replaceAll("\\s+", "");
        assertTrue(ir.contains("\"decision\":false,\"justification\":{\"node\":\"QuantifierFold\",\"quantifier\":\"atLeast\","
                + "\"threshold\":1,\"elementType\":\"Vondst\",\"test\":{\"node\":\"CharacteristicCheck\",\"characteristic\":\"wrakstuk\""), ir);
        assertTrue(ir.contains("\"sourcePath\":[" + MELDER + "," + VONDST + "]"), ir);
    }

    /** The matrix reads with the element as subject: a read the frontend built from the rule subject has, over a path,
     *  no walk back from the element, and is refused rather than bound from the element. Over one step it binds back
     *  over the relation's other role, as before. */
    @Test void a_test_that_reads_the_rule_subject_over_a_path_is_refused() {
        RegelSpraakModel m = Resolver.resolve(MODEL + rule("Melding is dringend",
                "het gewicht van ten minste één van de vondsten van de melder van de Melding groter is dan de drempel van de Melding"));
        assertTrue(m.diagnostics.stream().anyMatch(d -> d.isError() && "RGS-348".equals(d.code())
                        && d.message().contains("'de drempel van de Melding'")),
                () -> "de lezing van het onderwerp hoort luid geweigerd te worden: " + m.diagnostics);
        RegelSpraakModel one = Resolver.resolve(MODEL + """
            Regel t
                geldig altijd
                    Een Jutter is kistplichtig
                    indien het gewicht van ten minste één van de vondsten van de Jutter groter is dan de leeftijd van de Jutter.
            """);
        assertTrue(one.diagnostics.isEmpty(), () -> "over één stap bindt de lezing van het onderwerp terug: " + one.diagnostics);
    }

    /** A read the matrix builds again from the element ("de ... van de vinder van de Vondst") is the element's. */
    @Test void a_read_written_from_the_element_binds_over_the_path() {
        String ir = clean(rule("Melding is dringend",
                "het gewicht van ten minste één van de vondsten van de melder van de Melding groter is dan de leeftijd van "
                        + "de vinder van de Vondst"));
        assertTrue(ir.contains("\"right\":{\"node\":\"AttributeReference\",\"attribute\":\"leeftijd\",\"subjectType\":\"Jutter\","), ir);
        assertTrue(ir.contains("\"path\":[{\"node\":\"RoleBinding\",\"factType\":\"JutterHeeftVondst\",\"role\":\"vinder\"}]"), ir);
    }

    /** A walk that does not bind stays loud: the step that binds no role is named. */
    @Test void a_step_that_binds_no_role_keeps_the_marker_and_names_the_step() {
        RegelSpraakModel m = Resolver.resolve(MODEL + rule("Melding is dringend",
                "ten minste één van de vondsten van de zender van de Melding zwaar is"));
        assertTrue(m.diagnostics.stream().anyMatch(d -> "RGS-213".equals(d.code())
                        && d.message().contains("bindt de stap 'zender' vanaf Melding geen rol")),
                () -> "de stap die niet bindt hoort genoemd te worden: " + m.diagnostics);
        assertTrue(IrEmitter.emit(m).replaceAll("\\s+", "").contains("\"node\":\"Unresolved\""));
    }

    /** A Jutter reaches Jutters of his own type twice: through his Ploeg (a path) and as begeleider (one step). */
    static final String SAME_TYPE = """
        Objecttype de Ploeg (mv: Ploegen)
        Objecttype de Jutter (mv: Jutters) (bezield)
            de leeftijd Numeriek (geheel getal);
            de ouderen Numeriek (geheel getal);
            is kistplichtig kenmerk (bijvoeglijk);
            het lid kenmerk;
        Feittype PloegHeeftJutter
            de ploeg Ploeg
            de jutter (mv: jutters) Jutter
        Eén ploeg heeft meerdere jutters
        Feittype Begeleiding
            de begeleider Jutter
            de leerling (mv: leerlingen) Jutter
        Eén begeleider heeft meerdere leerlingen
        """;

    /** Review finding 1 (2026-10-20): where element and rule subject have the same type, a read of the subject in the
     *  test of a quantifier names the element as well, and the test is read per element with the element as its
     *  subject — every executor compared each element with itself. Refused by name (RGS-349), over a path and over one
     *  step, beside the read an operand of another type takes (RGS-348). */
    @Test void a_test_that_reads_a_subject_of_the_element_type_is_refused() {
        String[][] forms = {
            {"de leeftijd van ten minste één van de jutters van de ploeg van de Jutter groter is dan de leeftijd van de Jutter",
                    "'de leeftijd van de Jutter'"},
            {"de leeftijd van ten minste één van de jutters van de ploeg van de Jutter groter is dan zijn leeftijd",
                    "'zijn leeftijd'"},
            {"de leeftijd van alle leerlingen van de Jutter groter is dan de leeftijd van de Jutter",
                    "'de leeftijd van de Jutter'"},
        };
        for (String[] f : forms) {
            RegelSpraakModel m = Resolver.resolve(SAME_TYPE + rule("Jutter is kistplichtig", f[0]));
            assertTrue(m.diagnostics.stream().anyMatch(d -> d.isError() && "RGS-349".equals(d.code())
                            && d.message().contains(f[1])),
                    () -> f[0] + ": de lezing van het onderwerp van het elementtype hoort luid geweigerd te worden: "
                            + m.diagnostics);
        }
    }

    /** The same finding in a subselection ("die aan de volgende voorwaarde voldoen: …"), which is read with the element
     *  as its subject: "de leeftijd van de Jutter" names the element as well as a rule subject of the same type. Refused
     *  (RGS-350) in the filter of a value fold over a path and over one step, and in the filter of a quantifier; "zijn leeftijd"
     *  there is the element's and binds. */
    @Test void a_filter_that_names_a_subject_of_the_element_type_is_refused() {
        String filter = " die aan de volgende voorwaarde voldoen: zijn leeftijd is groter dan de leeftijd van de Jutter";
        String[] sentences = {
            "De ouderen van een Jutter moet berekend worden als de som van de leeftijd van alle jutters van de ploeg van de "
                    + "Jutter" + filter + ".",
            "De ouderen van een Jutter moet berekend worden als de som van de leeftijd van alle leerlingen van de Jutter"
                    + filter + ".",
            "De ouderen van een Jutter moet berekend worden als het aantal leerlingen van de Jutter" + filter + ".",
            "Een Jutter is kistplichtig\n        indien ten minste één van de jutters van de ploeg van de Jutter" + filter
                    + " een lid is.",
        };
        for (String s : sentences) {
            RegelSpraakModel m = Resolver.resolve(SAME_TYPE + "Regel t\n    geldig altijd\n        " + s + "\n");
            assertTrue(m.diagnostics.stream().anyMatch(d -> d.isError() && "RGS-350".equals(d.code())
                            && d.message().contains("'de leeftijd van de Jutter'")),
                    () -> s + ": de lezing in het filter hoort luid geweigerd te worden: " + m.diagnostics);
        }
        RegelSpraakModel own = Resolver.resolve(SAME_TYPE + "Regel t\n    geldig altijd\n        De ouderen van een Jutter "
                + "moet berekend worden als de som van de leeftijd van alle jutters van de ploeg van de Jutter die aan de "
                + "volgende voorwaarde voldoen: zijn leeftijd is groter dan 45.\n");
        assertTrue(own.diagnostics.isEmpty(), () -> "'zijn leeftijd' in het filter is die van het element: " + own.diagnostics);
    }

    /** Review finding 2 (2026-10-20): "niet een &lt;kenmerk&gt;" under a quantifier is the negated nominal
     *  characteristic, the same test as "geen &lt;kenmerk&gt;" (§8.1.8.3), over a path and over one step, in the
     *  vragende and the stellende order. It was refused with the reason that the characteristic is none of the
     *  element. A name that is no nominal characteristic of the element stays refused. */
    @Test void niet_een_under_a_quantifier_is_the_negated_characteristic() {
        String[][] forms = {
            {"Melding is dringend", "ten minste één van de vondsten van de melder van de Melding niet een wrakstuk is"},
            {"Jutter is kistplichtig", "ten minste één van de vondsten van de Jutter niet een wrakstuk is"},
            {"Melding is dringend", "alle vondsten van de melder van de Melding niet een wrakstuk zijn"},
            {"Melding is dringend", "hij aan alle volgende voorwaarden voldoet:\n        - ten minste één van de vondsten "
                    + "van de melder van de Melding is niet een wrakstuk\n        - zijn drempel is groter dan 0"},
            {"Jutter is kistplichtig", "hij aan alle volgende voorwaarden voldoet:\n        - ten minste één van de vondsten "
                    + "van de Jutter is niet een wrakstuk\n        - zijn leeftijd is groter dan 0"},
        };
        for (String[] f : forms) {
            String ir = clean(rule(f[0], f[1]));
            assertTrue(ir.contains("\"node\":\"QuantifierFold\""), () -> f[1] + ":\n" + ir);
            assertTrue(ir.contains("\"test\":{\"node\":\"CharacteristicCheck\",\"characteristic\":\"wrakstuk\",\"negated\":true"),
                    () -> f[1] + ":\n" + ir);
            assertFalse(ir.contains("\"node\":\"Unresolved\""), () -> f[1] + ":\n" + ir);
        }
        RegelSpraakModel m = Resolver.resolve(MODEL + rule("Melding is dringend",
                "ten minste één van de vondsten van de melder van de Melding niet een zwaar is"));
        assertTrue(m.diagnostics.stream().anyMatch(d -> "RGS-213".equals(d.code())
                        && d.message().contains("'zwaar' is geen kenmerk van Vondst")),
                () -> "een bijvoeglijk kenmerk na 'niet een' blijft geweigerd: " + m.diagnostics);
    }

    /** A count over a path is no quantifier and stays refused (acceptance (e)): here, with no relation between the
     *  Jutter and the Kist, as a fold without a range (RGS-131), as on main. */
    @Test void a_count_over_a_path_stays_refused() {
        RegelSpraakModel m = Resolver.resolve(MODEL.replace("is kistplichtig kenmerk (bijvoeglijk);",
                "is kistplichtig kenmerk (bijvoeglijk);\n    het kistaantal Numeriek (geheel getal);") + """
            Regel t
                geldig altijd
                    Het kistaantal van een Jutter moet berekend worden als het aantal kisten van de vondsten van de Jutter.
            """);
        assertTrue(m.diagnostics.stream().anyMatch(d -> d.isError() && "RGS-131".equals(d.code())),
                () -> "een telling over een pad blijft een ERROR: " + m.diagnostics);
    }
}
