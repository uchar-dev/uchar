package dev.uchar.regelspraak;

import dev.uchar.regelspraak.emit.IrEmitter;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.model.RegelSpraakModel.Severity;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A fold over a relation of the rule subject, read in the filter of another fold (card fold-in-element-condition,
 * bridge K13 of the rule set herleving-WW): the filter is read per element, so the inner fold is written from the
 * element with the way back to the rule subject in front of its range — the IR the element-role spelling R4 already
 * gets (grammar/DIVERGENCES.md, "A read of the rule subject in a filter").
 */
class FoldInElementConditionTest {

    static final String MODEL = SubselectionPerChainTest.MODEL;

    /** The rule of the card with {@code bullet} as its second condition on the ladingen. */
    static String rule(String model, String bullet) {
        return model + """
            Regel eerste schone maand vanaf de eindmaand
                geldig altijd
                    De eerste schone maand vanaf-de-eindmaand van een Schuit moet berekend worden als de eerste van de maand van alle ladingen van de Schuit die aan alle volgende voorwaarden voldoen:
                    - hij heeft geen overlading
                    -\s""" + bullet + "\n";
    }

    static final String R1 = rule(MODEL, "zijn maand plus 1 mnd is later dan de laatste van de einddag van alle klachten van de "
            + "Schuit die aan de volgende voorwaarde voldoen: zijn soort is gelijk aan 'Zwaar'.");
    static final String R2 = rule(MODEL, "zijn maand plus 1 mnd is later dan (de laatste van de einddag van alle klachten van de "
            + "Schuit die aan de volgende voorwaarde voldoen: zijn soort is gelijk aan 'Zwaar').");
    static final String R3 = rule(MODEL, "zijn maand plus 1 mnd is later dan de laatste van de einddag van alle klachten van de "
            + "Schuit.");
    static final String R4 = rule(MODEL, "zijn maand plus 1 mnd is later dan de laatste van de einddag van alle klachten van de "
            + "schuit van de Lading in-een-maand die aan de volgende voorwaarde voldoen: zijn soort is gelijk aan 'Zwaar'.");
    static final String R4_WITHOUT_FILTER = rule(MODEL, "zijn maand plus 1 mnd is later dan de laatste van de einddag van "
            + "alle klachten van de schuit van de Lading in-een-maand.");

    @Test void r1_reads_the_klachten_of_the_rule_subject_over_the_way_back() {
        RegelSpraakModel m = Resolver.resolve(R1);
        assertTrue(m.diagnostics.isEmpty(), () -> "R1 gaat zonder diagnostiek: " + m.diagnostics);
        Object filter = filter(m);
        assertEquals(filter(clean(R4)), filter, "R1 krijgt het IR van R4, de spelling via de rol van het element");
        assertTrue(text(filter).contains("sourcePath=[{node=RoleBinding, factType=lading in-een-maand, role=schuit}, "
                + "{node=RoleBinding, factType=schuitklacht, role=klacht}]"), () -> "het pad terug staat vóór de klachten: " + filter);
    }

    @Test void r2_with_parentheses_is_r1() {
        assertEquals(filter(clean(R1)), filter(clean(R2)));
    }

    @Test void r3_without_a_filter_reads_the_klachten_of_the_rule_subject() {
        RegelSpraakModel m = Resolver.resolve(R3);
        assertTrue(m.diagnostics.isEmpty(), () -> "R3 gaat zonder WARNING: " + m.diagnostics);
        assertEquals(filter(clean(R4_WITHOUT_FILTER)), filter(m));
    }

    @Test void r4_carries_no_stale_warning() {
        RegelSpraakModel m = Resolver.resolve(R4);
        assertTrue(m.diagnostics.isEmpty(), () -> "R4 draagt de vouw gelowerd, dus geen 'blijft onopgelost': " + m.diagnostics);
    }

    @Test void a_fold_without_a_way_back_is_refused_with_the_way_out() {
        String manyToMany = MODEL.replace("Eén schuit heeft meerdere ladingen", "meerdere schuiten hebben meerdere ladingen");
        RegelSpraakModel m = Resolver.resolve(rule(manyToMany, "zijn maand plus 1 mnd is later dan de laatste van de einddag "
                + "van alle klachten van de Schuit die aan de volgende voorwaarde voldoen: zijn soort is gelijk aan 'Zwaar'."));
        Diagnostic d = only(m, "RGS-352");
        assertEquals(Severity.ERROR, d.severity());
        assertEquals(5 + MODEL.lines().count(), d.line());
        for (String part : List.of("de laatste van de einddag van alle klachten van de Schuit", "Schuit", "Lading in-een-maand",
                "'schuit'", "'lading in-een-maand'", "eigen lid"))
            assertTrue(d.message().contains(part), () -> "de weigering noemt '" + part + "': " + d.message());
        assertTrue(m.diagnostics.stream().noneMatch(x -> x.severity() == Severity.WARNING),
                () -> "geen WARNING met een marker: " + m.diagnostics);
    }

    @Test void a_count_over_the_way_back_is_refused() {
        RegelSpraakModel m = Resolver.resolve(rule(MODEL, "zijn gewicht is groter dan het aantal klachten van de Schuit."));
        Diagnostic d = only(m, "RGS-352");
        assertTrue(d.message().contains("WF-102 (c)"), () -> "een telling heeft geen pad als bereik: " + d.message());
    }

    @Test void an_element_of_the_rule_subjects_own_type_is_refused() {
        String towing = MODEL.replace("    de eerste schone maand vanaf-de-eindmaand Datum in dagen;",
                "    de eerste schone maand vanaf-de-eindmaand Datum in dagen;\n    de startdag Datum in dagen;")
                .replace("Objecttype de Lading in-een-maand", """
                Feittype sleep
                    de sleper Schuit
                    de gesleepte (mv: gesleepten) Schuit
                Eén sleper heeft meerdere gesleepten

                Objecttype de Lading in-een-maand""");
        RegelSpraakModel m = Resolver.resolve(towing + """
            Regel eerste schone maand vanaf de eindmaand
                geldig altijd
                    De eerste schone maand vanaf-de-eindmaand van een Schuit moet berekend worden als de eerste van de startdag van alle gesleepten van de Schuit die aan de volgende voorwaarde voldoen: zijn startdag is later dan de laatste van de einddag van alle klachten van de Schuit.
            """);
        Diagnostic d = only(m, "RGS-350");
        assertTrue(d.message().contains("alle klachten van de Schuit"), () -> "de weigering noemt de lezing: " + d.message());
    }

    @Test void a_fold_of_the_rule_subject_in_the_test_of_a_quantifier_reads_over_the_way_back() {
        String model = MODEL.replace("    de eerste schone maand vanaf-de-eindmaand Datum in dagen;",
                "    de eerste schone maand vanaf-de-eindmaand Datum in dagen;\n    is vroeg kenmerk (bijvoeglijk);");
        RegelSpraakModel m = Resolver.resolve(model + """
            Regel vroeg
                geldig altijd
                    Een Schuit is vroeg
                    indien de maand van ten minste één van de ladingen van de Schuit later is dan de laatste van de einddag van alle klachten van de Schuit.
            """);
        assertTrue(m.diagnostics.isEmpty(), () -> "de toets van de kwantor leest de vouw over het pad terug: " + m.diagnostics);
        String ir = text(Report.json(IrEmitter.emit(m)));
        assertTrue(ir.contains("elementType=Klacht, sourcePath=[{node=RoleBinding, factType=lading in-een-maand, role=schuit}, "
                + "{node=RoleBinding, factType=schuitklacht, role=klacht}]"), () -> ir);
    }

    @Test void a_comparison_in_a_filter_is_typed_on_the_element() {
        RegelSpraakModel m = Resolver.resolve(rule(MODEL, "zijn gewicht is later dan 01-01-2026."));
        assertTrue(m.diagnostics.stream().anyMatch(d -> d.severity() == Severity.ERROR && d.line() == 5 + MODEL.lines().count()),
                () -> "een getal is niet later dan een datum; de deelselectie leest haar gewicht op de lading: " + m.diagnostics);
    }

    static RegelSpraakModel clean(String source) {
        RegelSpraakModel m = Resolver.resolve(source);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        return m;
    }

    static Diagnostic only(RegelSpraakModel m, String code) {
        List<Diagnostic> found = m.diagnostics.stream().filter(d -> code.equals(d.code())).toList();
        assertEquals(1, found.size(), () -> "precies één " + code + ": " + m.diagnostics);
        return found.get(0);
    }

    /** The filter of the value of the one rule whose value is a fold with a filter, without source records. */
    static Object filter(RegelSpraakModel m) {
        Map<?, ?> document = (Map<?, ?>) Report.json(IrEmitter.emit(m));
        for (Object r : (List<?>) ((Map<?, ?>) document.get("rules")).get("rules")) {
            Map<?, ?> version = (Map<?, ?>) ((List<?>) ((Map<?, ?>) r).get("versions")).get(0);
            if (version.get("value") instanceof Map<?, ?> value && value.get("filter") != null)
                return withoutSources(value.get("filter"));
        }
        throw new AssertionError("geen regel met een vouw met filter");
    }

    static Object withoutSources(Object x) {
        if (x instanceof Map<?, ?> map) {
            Map<Object, Object> result = new LinkedHashMap<>();
            map.forEach((k, v) -> { if (!"source".equals(k)) result.put(k, withoutSources(v)); });
            return result;
        }
        if (x instanceof List<?> list) return list.stream().map(FoldInElementConditionTest::withoutSources).toList();
        return x;
    }

    static String text(Object json) { return String.valueOf(withoutSources(json)); }
}
