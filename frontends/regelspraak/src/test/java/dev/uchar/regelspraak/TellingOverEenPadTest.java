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
 * a path exists, so the frontend refuses it by name, with the number of steps and the steps themselves, and writes
 * nothing.
 *
 * <p><b>Red before the refusal</b> (measured 2026-10-02 on the frontend of {@code da03d480}): where the subject and the
 * counted type are joined by one relation, the count went out silently as a count over that relation —
 * {@code sourceRole JutterHeeftVondst.jutter}, {@code diagnostics: []}, in a rule's value, in a condition, under a
 * subselection, behind a possessive anchor, by the type's plural, under "plus", in a default's justification and
 * consequence, and as a selection of a union — and ref and clingo then answer 1 where the path reaches 2. Where no
 * relation joins them the count was refused, as a fold without a range ("zonder bereik"), and over a set step ("van alle
 * havenjutters van de Haven") it stayed a marker with a WARNING: loud, in words that did not name the path.
 */
class TellingOverEenPadTest {

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

    private static final String VONDST_DRIE = "het aantal havenjutters van de haven van de jutter van de Vondst";

    private static String waarde(String type, String doel, String telling) {
        return "Regel " + doel + "\n    geldig altijd\n        Het " + doel + " van een " + type
                + " moet berekend worden als " + telling + ".\n";
    }

    private static List<Diagnostic> padfouten(RegelSpraakModel m) {
        return m.diagnostics.stream().filter(d -> d.message().contains("telling over een pad van")).toList();
    }

    /** One ERROR naming the path, with the number of steps and the steps subject-outward. */
    private static void geweigerd(String bron, int stappen, String namen) {
        RegelSpraakModel m = Resolver.resolve(MODEL + bron);
        List<Diagnostic> ds = padfouten(m);
        assertEquals(1, ds.size(), () -> "verwacht één padweigering, kreeg: " + m.diagnostics);
        assertEquals(Severity.ERROR, ds.get(0).severity());
        assertTrue(ds.get(0).message().contains("een telling over een pad van " + stappen + " stappen (" + namen
                + "): deze frontend telt alleen over één relatie vanaf het onderwerp (IR-SPEC ch. 4.3, WF-102 (c)); "
                + "schrijf de telling op het bereikte object of noem één relatie"), ds.get(0)::message);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "drie stappen in een waarde|het aantal havenjutters van de haven van de jutter van de Vondst",
            "met een subselectie|het aantal havenjutters van de haven van de jutter van de Vondst die gemarkeerd zijn",
            "achter een bezittelijk anker|het aantal havenjutters van de haven van zijn jutter",
            "met het meervoud van het type|het aantal Jutters van de haven van de jutter van de Vondst",
            "onder een optelling|het aantal havenjutters van de haven van de jutter van de Vondst plus 1",
    })
    void een_stille_afkapping_over_drie_stappen_wordt_geweigerd(String vorm, String telling) {
        geweigerd(waarde("Vondst", "aantal havengenoten", telling), 3, "jutter, haven, havenjutters");
    }

    @Test void in_een_voorwaarde() {
        geweigerd("Regel uitkomst\n    geldig altijd\n        De uitkomst van een Vondst moet gesteld worden op zijn "
                + "vinddag\n        indien " + VONDST_DRIE + " groter is dan 1.\n", 3, "jutter, haven, havenjutters");
    }

    @Test void in_een_kenmerktoekenning() {
        geweigerd("Regel gemarkeerd\n    geldig altijd\n        Een Vondst is gemarkeerd\n        indien " + VONDST_DRIE
                + " groter is dan 1.\n", 3, "jutter, haven, havenjutters");
    }

    @Test void in_de_toets_van_een_default() {
        geweigerd("Regel uitkomst\n    geldig altijd\n        De uitkomst van een Vondst moet gesteld worden op zijn "
                + "vinddag.\n\nAfwijking van uitkomst: druk\n    geldig altijd\n        De uitkomst van een Vondst moet "
                + "gesteld worden op 01-01-2026\n        indien " + VONDST_DRIE + " groter is dan 1.\n", 3,
                "jutter, haven, havenjutters");
    }

    @Test void in_de_consequentie_van_een_default() {
        geweigerd("Regel havengenoten\n    geldig altijd\n        Het aantal havengenoten van een Vondst moet gesteld "
                + "worden op 0.\n\nAfwijking van havengenoten: bekend\n    geldig altijd\n        Het aantal havengenoten "
                + "van een Vondst moet berekend worden als " + VONDST_DRIE + "\n        indien zijn vinddag gevuld is.\n",
                3, "jutter, haven, havenjutters");
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "twee stappen vanaf de Jutter|Jutter|aantal havengenoten|het aantal havenjutters van de haven van de Jutter|haven, havenjutters",
            "bezittelijk, twee stappen|Jutter|aantal havengenoten|het aantal havenjutters van zijn haven|haven, havenjutters",
            "twee stappen vanaf de Vondst|Vondst|aantal havengenoten|het aantal vondsten van de jutter van de Vondst|jutter, vondsten",
            "van alle … van de …|Haven|aantal havenvondsten|het aantal vondsten van alle havenjutters van de Haven|havenjutters, vondsten",
            "van alle … met een subselectie|Haven|aantal havenvondsten|het aantal vondsten van alle havenjutters van de Haven die gemarkeerd zijn|havenjutters, vondsten",
            "van de … van de … als meervoud|Haven|aantal havenvondsten|het aantal vondsten van de havenjutters van de Haven|havenjutters, vondsten",
            "twee verzamelingen|Kustwacht|aantal bewaakte jutters|het aantal havenjutters van alle bewaakte havens van de Kustwacht|bewaakte havens, havenjutters",
    })
    void een_luide_vorm_over_twee_stappen_krijgt_dezelfde_weigering(String vorm, String type, String doel, String telling,
                                                                   String namen) {
        geweigerd(waarde(type, doel, telling), 2, namen);
    }

    @Test void drie_verzamelingen() {
        geweigerd(waarde("Kustwacht", "aantal bewaakte vondsten",
                "het aantal vondsten van alle havenjutters van alle bewaakte havens van de Kustwacht"), 3,
                "bewaakte havens, havenjutters, vondsten");
    }

    @Test void een_selectie_van_een_unie() {
        // the union took the second selection's range from the type pair, the first selection's relation again
        RegelSpraakModel m = Resolver.resolve(MODEL + waarde("Jutter", "aantal eigen vondsten",
                "het aantal vondsten van de Jutter en vondsten van de havenjutters van de haven van de Jutter"));
        List<Diagnostic> ds = padfouten(m);
        assertEquals(1, ds.size(), () -> "verwacht één padweigering, kreeg: " + m.diagnostics);
        assertEquals(Severity.ERROR, ds.get(0).severity());
        assertTrue(ds.get(0).message().contains("geen unie van selecties: selectie 2 ('vondsten van de havenjutters van "
                + "de haven van de Jutter') is een telling over een pad van 3 stappen (haven, havenjutters, vondsten): een "
                + "selectie telt over één relatie vanaf het onderwerp (IR-SPEC ch. 4.3, WF-110 (e)); schrijf de telling op "
                + "het bereikte object of noem één relatie"), ds.get(0)::message);
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
    void een_telling_over_een_relatie_blijft_schoon(String vorm, String type, String doel, String telling) {
        RegelSpraakModel m = Resolver.resolve(MODEL + waarde(type, doel, telling));
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek op '" + telling + "': " + m.diagnostics);
    }

    @Test void een_telling_over_een_relatie_in_een_voorwaarde_blijft_schoon() {
        RegelSpraakModel m = Resolver.resolve(MODEL + "Regel druk\n    geldig altijd\n        Een Haven is druk\n"
                + "        indien het aantal havenjutters van de Haven groter is dan 1.\n");
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
    }
}
