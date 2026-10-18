package dev.uchar.regelspraak;

import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.model.RegelSpraakModel.Severity;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A count whose written source walks more than the one relation it counts: "het aantal havenjutters van de haven van
 * de jutter van de Vondst" (to-one, to-one, to the set). The count's range is one relation from the rule subject
 * (IR-SPEC ch. 4.3, WF-102 (c): a path is the range of a VALUE fold's set source only), and no lowering of a count over
 * a path exists, so where the emitter would still write a range — the head's role by name, or the one relation between
 * the subject and the counted type — the frontend refuses the count by name, with the number of steps and the steps
 * themselves, and writes nothing. A union selection takes its range from its head alone, so there every written step
 * beyond it is refused.
 *
 * <p><b>Red before the refusal</b> (measured 2026-10-02 on the frontend of {@code da03d480}): the count went out
 * silently as a count over that one relation — {@code sourceRole JutterHeeftVondst.jutter}, {@code diagnostics: []} —
 * in a rule's value, in a condition, in a characteristic's condition, under a subselection, behind a possessive anchor,
 * by the type's plural, under "plus", in a default's justification and consequence, with an anchor that is not the
 * subject, and as a selection of a union; ref and clingo then answer 1 where the path reaches 2. Where no such range
 * exists the count was refused already, as a fold without a range or with a written step, and over a set step ("van
 * alle havenjutters van de Haven") it stayed a marker with a WARNING: those keep their refusal.
 */
class CountOverPathTest {

    static final String MODEL = """
        Objecttype de Kustwacht (mv: Kustwachten)
            het aantal bewaakte jutters Numeriek (geheel getal);
            het aantal bewaakte vondsten Numeriek (geheel getal);

        Objecttype de Haven (mv: Havens)
            het aantal havenvondsten Numeriek (geheel getal);
            het aantal eigen jutters Numeriek (geheel getal);
            is druk kenmerk (bijvoeglijk);

        Objecttype de Jutter (mv: Jutters) (bezield)
            het aantal havengenoten Numeriek (geheel getal);
            het aantal eigen vondsten Numeriek (geheel getal);
            is gemarkeerd kenmerk (bijvoeglijk);

        Objecttype de Vondst (mv: Vondsten)
            de vinddag Datum in dagen;
            het aantal havengenoten Numeriek (geheel getal);
            de uitkomst Datum in dagen;
            is gemarkeerd kenmerk (bijvoeglijk);

        Feittype JutterHeeftVondst
            de jutter Jutter
            de vondst (mv: vondsten) Vondst
        Eén jutter heeft meerdere vondsten

        Feittype HavenHeeftJutter
            de haven Haven
            de havenjutter (mv: havenjutters) Jutter
        Eén haven heeft meerdere havenjutters

        Feittype KustwachtBewaaktHaven
            de kustwacht Kustwacht
            de bewaakte haven (mv: bewaakte havens) Haven
        Eén kustwacht bewaakt meerdere bewaakte havens

        """;

    private static final String VONDST_THREE = "het aantal havenjutters van de haven van de jutter van de Vondst";

    private static String waarde(String type, String doel, String count) {
        return "Regel " + doel + "\n    geldig altijd\n        Het " + doel + " van een " + type
                + " moet berekend worden als " + count + ".\n";
    }

    private static List<Diagnostic> pathErrors(RegelSpraakModel m) {
        return m.diagnostics.stream().filter(d -> d.message().contains("telling over een pad van")).toList();
    }

    /** One ERROR naming the path, with the number of steps and the steps subject-outward. */
    private static void geweigerd(String bron, int steps, String names) {
        RegelSpraakModel m = Resolver.resolve(MODEL + bron);
        List<Diagnostic> ds = pathErrors(m);
        assertEquals(1, ds.size(), () -> "verwacht één padweigering, kreeg: " + m.diagnostics);
        assertEquals(Severity.ERROR, ds.get(0).severity());
        assertTrue(ds.get(0).message().contains("een telling over een pad van " + steps + " stappen (" + names
                + "): deze frontend telt alleen over één relatie vanaf het onderwerp (IR-SPEC ch. 4.3, WF-102 (c)); "
                + "schrijf de telling op het bereikte object of noem één relatie"), ds.get(0)::message);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "drie stappen in een waarde|het aantal havenjutters van de haven van de jutter van de Vondst|havenjutters",
            "met een subselectie|het aantal havenjutters van de haven van de jutter van de Vondst die gemarkeerd zijn|havenjutters",
            "achter een bezittelijk anker|het aantal havenjutters van de haven van zijn jutter|havenjutters",
            "met het meervoud van het type|het aantal Jutters van de haven van de jutter van de Vondst|Jutters",
            "onder een optelling|het aantal havenjutters van de haven van de jutter van de Vondst plus 1|havenjutters",
    })
    void a_silent_truncation_over_three_steps_is_refused(String vorm, String count, String last) {
        geweigerd(waarde("Vondst", "aantal havengenoten", count), 3, "jutter, haven, " + last);
    }

    @Test void in_a_condition() {
        geweigerd("Regel uitkomst\n    geldig altijd\n        De uitkomst van een Vondst moet gesteld worden op zijn "
                + "vinddag\n        indien " + VONDST_THREE + " groter is dan 1.\n", 3, "jutter, haven, havenjutters");
    }

    @Test void in_a_characteristic_attribution() {
        geweigerd("Regel gemarkeerd\n    geldig altijd\n        Een Vondst is gemarkeerd\n        indien " + VONDST_THREE
                + " groter is dan 1.\n", 3, "jutter, haven, havenjutters");
    }

    @Test void in_the_test_of_a_default() {
        geweigerd("Regel uitkomst\n    geldig altijd\n        De uitkomst van een Vondst moet gesteld worden op zijn "
                + "vinddag.\n\nAfwijking van uitkomst: druk\n    geldig altijd\n        De uitkomst van een Vondst moet "
                + "gesteld worden op 01-01-2026\n        indien " + VONDST_THREE + " groter is dan 1.\n", 3,
                "jutter, haven, havenjutters");
    }

    @Test void in_the_consequence_of_a_default() {
        geweigerd("Regel havengenoten\n    geldig altijd\n        Het aantal havengenoten van een Vondst moet gesteld "
                + "worden op 0.\n\nAfwijking van havengenoten: bekend\n    geldig altijd\n        Het aantal havengenoten "
                + "van een Vondst moet berekend worden als " + VONDST_THREE + "\n        indien zijn vinddag gevuld is.\n",
                3, "jutter, haven, havenjutters");
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "een anker van een ander type|het aantal havenjutters van de Haven|Haven, havenjutters",
            "het type van de rol als anker|het aantal havenjutters van de Jutter|Jutter, havenjutters",
    })
    void an_anchor_that_is_not_the_subject_is_refused(String vorm, String count, String names) {
        geweigerd(waarde("Vondst", "aantal havengenoten", count), 2, names);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "een stap in de naam|het aantal vondsten van de Jutter en vondsten van de havenjutters van de haven van de Jutter|2|vondsten van de havenjutters van de haven van de Jutter|3|haven, havenjutters, vondsten",
            "een anker van een ander type|het aantal vondsten van de Jutter en vondsten van de Haven|2|vondsten van de Haven|2|Haven, vondsten",
            "een bezittelijk anker|het aantal vondsten van zijn haven en vondsten van de Jutter|1|vondsten van zijn haven|2|haven, vondsten",
    })
    void a_selection_of_a_union(String vorm, String count, int selection, String bron, int steps, String names) {
        // the union took the selection's range from its head, the relation of the other selection again
        RegelSpraakModel m = Resolver.resolve(MODEL + waarde("Jutter", "aantal eigen vondsten", count));
        List<Diagnostic> ds = pathErrors(m);
        assertEquals(1, ds.size(), () -> "verwacht één padweigering, kreeg: " + m.diagnostics);
        assertEquals(Severity.ERROR, ds.get(0).severity());
        assertTrue(ds.get(0).message().contains("geen unie van selecties: selectie " + selection + " ('" + bron + "') is een "
                + "telling over een pad van " + steps + " stappen (" + names + "): een selectie telt over één relatie "
                + "vanaf het onderwerp (IR-SPEC ch. 4.3, WF-110 (e)); schrijf de telling op het bereikte object of noem "
                + "één relatie"), ds.get(0)::message);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "twee stappen vanaf de Jutter|Jutter|aantal havengenoten|het aantal havenjutters van de haven van de Jutter|zonder bereik",
            "bezittelijk, twee stappen|Jutter|aantal havengenoten|het aantal havenjutters van zijn haven|zonder bereik",
            "twee stappen vanaf de Vondst|Vondst|aantal havengenoten|het aantal vondsten van de jutter van de Vondst|zonder bereik",
            "van de … van de … als meervoud|Haven|aantal havenvondsten|het aantal vondsten van de havenjutters van de Haven|zonder bereik",
            "van alle … van de …|Haven|aantal havenvondsten|het aantal vondsten van alle havenjutters van de Haven|is geen (meervoud van een) gedeclareerd attribuut",
            "van alle … met een subselectie|Haven|aantal havenvondsten|het aantal vondsten van alle havenjutters van de Haven die gemarkeerd zijn|is geen (meervoud van een) gedeclareerd attribuut",
            "twee verzamelingen|Kustwacht|aantal bewaakte jutters|het aantal havenjutters van alle bewaakte havens van de Kustwacht|is geen (meervoud van een) gedeclareerd attribuut",
            "drie verzamelingen|Kustwacht|aantal bewaakte vondsten|het aantal vondsten van alle havenjutters van alle bewaakte havens van de Kustwacht|is geen (meervoud van een) gedeclareerd attribuut",
    })
    void a_form_without_range_keeps_its_own_refusal(String vorm, String type, String doel, String count,
                                                          String message) {
        // no range to write, so the fold fences speak, as they did before: loud, and not this refusal
        RegelSpraakModel m = Resolver.resolve(MODEL + waarde(type, doel, count));
        assertTrue(pathErrors(m).isEmpty(), () -> "onverwachte padweigering: " + m.diagnostics);
        assertTrue(m.diagnostics.stream().anyMatch(d -> d.message().contains(message)), () -> m.diagnostics.toString());
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "één relatie|Jutter|aantal eigen vondsten|het aantal vondsten van de Jutter",
            "één relatie vanaf de Haven|Haven|aantal eigen jutters|het aantal havenjutters van de Haven",
            "met een subselectie|Haven|aantal eigen jutters|het aantal havenjutters van de Haven die gemarkeerd zijn",
            "het anker noemt het onderwerp met zijn rol|Jutter|aantal eigen vondsten|het aantal vondsten van de jutter",
            "het meervoud van het type|Jutter|aantal eigen vondsten|het aantal Vondsten van de Jutter",
            "een unie van twee selecties over één relatie|Haven|aantal eigen jutters|het aantal havenjutters van de Haven die gemarkeerd zijn en havenjutters van de Haven",
    })
    void a_count_over_a_relation_stays_clean(String vorm, String type, String doel, String count) {
        RegelSpraakModel m = Resolver.resolve(MODEL + waarde(type, doel, count));
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek op '" + count + "': " + m.diagnostics);
    }

    @Test void a_count_over_a_relation_in_a_condition_stays_clean() {
        RegelSpraakModel m = Resolver.resolve(MODEL + "Regel druk\n    geldig altijd\n        Een Haven is druk\n"
                + "        indien het aantal havenjutters van de Haven groter is dan 1.\n");
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
    }
}
