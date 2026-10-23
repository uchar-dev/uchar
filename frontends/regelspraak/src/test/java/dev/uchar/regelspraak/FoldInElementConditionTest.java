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
 * bridge K13 of the rule set herleving-WW): the filter is read per element, so the inner fold is built at the rule
 * subject and carries the form anchor (IR-SPEC ch. 4.1a, WF-122; card subselection-reads-subject,
 * grammar/DIVERGENCES.md, "The rule subject in a subselection — zelf"). Until 2026-10-23 it was written from the
 * element with the way back in front of its range, the IR of the element-role spelling R4, which keeps it.
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

    /** Changed 2026-10-23 (card subselection-reads-subject): R1 had the IR of R4, the way back over 'schuit' in front of
     *  the klachten; now its fold is read at the rule subject, with the anchor and the one-step range from the Schuit. R4
     *  navigates from the element and keeps the path: the two spellings of one reading (RUNTIME-SEMANTICS ch. 3.4a). */
    @Test void r1_reads_the_klachten_of_the_rule_subject_with_the_form_anchor() {
        RegelSpraakModel m = Resolver.resolve(R1);
        assertTrue(m.diagnostics.isEmpty(), () -> "R1 gaat zonder diagnostiek: " + m.diagnostics);
        String filter = text(filter(m));
        assertTrue(filter.contains("elementType=Klacht, anchor=form, filter="), () -> "de vouw draagt het anker: " + filter);
        assertTrue(filter.contains("sourceRole={node=RoleBinding, factType=schuitklacht, role=klacht}"),
                () -> "het bereik begint bij de Schuit: " + filter);
        assertFalse(filter.contains("sourcePath"), () -> "geen weg terug: " + filter);
        String r4 = text(filter(clean(R4)));
        assertTrue(r4.contains("sourcePath=[{node=RoleBinding, factType=lading in-een-maand, role=schuit}, "
                + "{node=RoleBinding, factType=schuitklacht, role=klacht}]"), () -> "R4 navigeert vanaf het element: " + r4);
        assertFalse(r4.contains("anchor"), () -> r4);
    }

    @Test void r2_with_parentheses_is_r1() {
        assertEquals(filter(clean(R1)), filter(clean(R2)));
    }

    /** Changed 2026-10-23, as R1: the anchor instead of the IR of R4 without its filter, which keeps its path. */
    @Test void r3_without_a_filter_reads_the_klachten_of_the_rule_subject() {
        RegelSpraakModel m = Resolver.resolve(R3);
        assertTrue(m.diagnostics.isEmpty(), () -> "R3 gaat zonder WARNING: " + m.diagnostics);
        String filter = text(filter(m));
        assertTrue(filter.contains("elementType=Klacht, anchor=form, sourceRole={node=RoleBinding, factType=schuitklacht, "
                + "role=klacht}"), () -> filter);
        assertTrue(text(filter(clean(R4_WITHOUT_FILTER))).contains("sourcePath="));
    }

    @Test void r4_carries_no_stale_warning() {
        RegelSpraakModel m = Resolver.resolve(R4);
        assertTrue(m.diagnostics.isEmpty(), () -> "R4 draagt de vouw gelowerd, dus geen 'blijft onopgelost': " + m.diagnostics);
    }

    /** Changed 2026-10-23 (card subselection-reads-subject): a lading of several schuiten has no way back, and the read
     *  was refused (RGS-352, retired); the anchor reads the rule subject without one. */
    @Test void a_fold_of_the_rule_subject_needs_no_way_back() {
        String manyToMany = MODEL.replace("Eén schuit heeft meerdere ladingen", "meerdere schuiten hebben meerdere ladingen");
        RegelSpraakModel m = Resolver.resolve(rule(manyToMany, "zijn maand plus 1 mnd is later dan de laatste van de einddag "
                + "van alle klachten van de Schuit die aan de volgende voorwaarde voldoen: zijn soort is gelijk aan 'Zwaar'."));
        assertTrue(m.diagnostics.isEmpty(), () -> "" + m.diagnostics);
        String filter = text(filter(m));
        assertTrue(filter.contains("elementType=Klacht, anchor=form, filter="), () -> filter);
        assertTrue(filter.contains("sourceRole={node=RoleBinding, factType=schuitklacht, role=klacht}"), () -> filter);
    }

    /** Changed 2026-10-23, as above: a count over the way back was refused (RGS-352: its range would be a path, WF-102
     *  (c)); with the anchor its range is the one relation from the Schuit. */
    @Test void a_count_of_the_rule_subject_carries_the_form_anchor() {
        RegelSpraakModel m = Resolver.resolve(rule(MODEL, "zijn gewicht is groter dan het aantal klachten van de Schuit."));
        assertTrue(m.diagnostics.isEmpty(), () -> "" + m.diagnostics);
        String filter = text(filter(m));
        assertTrue(filter.contains("{node=Aggregation, kind=count, arguments=[{node=AttributeReference, attribute=klachten, "
                + "subjectType=Schuit}], elementType=Klacht, anchor=form, sourceRole={node=RoleBinding, factType=schuitklacht, "
                + "role=klacht}"), () -> filter);
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

    /** Changed 2026-10-23: the fold beside the quantified operand carries the anchor instead of the way back. */
    @Test void a_fold_of_the_rule_subject_in_the_test_of_a_quantifier_carries_the_form_anchor() {
        String model = MODEL.replace("    de eerste schone maand vanaf-de-eindmaand Datum in dagen;",
                "    de eerste schone maand vanaf-de-eindmaand Datum in dagen;\n    is vroeg kenmerk (bijvoeglijk);");
        RegelSpraakModel m = Resolver.resolve(model + """
            Regel vroeg
                geldig altijd
                    Een Schuit is vroeg
                    indien de maand van ten minste één van de ladingen van de Schuit later is dan de laatste van de einddag van alle klachten van de Schuit.
            """);
        assertTrue(m.diagnostics.isEmpty(), () -> "de toets van de kwantor leest de vouw aan het regelonderwerp: " + m.diagnostics);
        String ir = text(Report.json(IrEmitter.emit(m)));
        assertTrue(ir.contains("elementType=Klacht, anchor=form, sourceRole={node=RoleBinding, factType=schuitklacht, "
                + "role=klacht}"), () -> ir);
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
