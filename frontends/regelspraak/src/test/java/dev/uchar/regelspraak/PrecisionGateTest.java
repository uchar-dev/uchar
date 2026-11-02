package dev.uchar.regelspraak;

import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The precision gate of RUNTIME-SEMANTICS ch. 2.4: a write whose precision bound (ch. 2.3, the ALEF form), restated into
 * the unit of its target, exceeds the target's decimals is RGS-338, one without a finite bound RGS-370; the share of an
 * allocation, bounded by its rounding or else by its pot, RGS-340 and RGS-371. Each refused source is the RegelSpraak of
 * a negative of the verifier, {@code corpus/negative/wf023-precision-<name>}, with the verdict the verifier gives its IR,
 * so the frontend and the verifier refuse the same writes; each passing source writes the same form through a rounding,
 * into the unbounded form, or with a bound that fits.
 */
class PrecisionGateTest {

    private static final String VONDST = """
        Objecttype de Vondst (mv: Vondsten)
            de opbrengst Numeriek (geheel getal);
            het aantal deelnemers Numeriek (geheel getal);
            de taxatie Numeriek (getal met 2 decimalen);
            de factor Numeriek (getal met 2 decimalen);
            de toeslag Numeriek (getal met 1 decimalen);
            de vrije factor Numeriek (getal);
            het aandeel Numeriek (geheel getal);
            het vrije aandeel Numeriek (getal);
            het derde deel Numeriek (getal met 2 decimalen);
            het kwart Numeriek (getal met 1 decimalen);
            het halve kwart Numeriek (getal met 3 decimalen);
            de heffing Numeriek (getal met 1 decimalen);
            de brede heffing Numeriek (getal met 2 decimalen);
            het hoogste bod Numeriek (geheel getal);
            de waarde Numeriek (getal met 2 decimalen);
            de smalle waarde Numeriek (getal met 1 decimalen);
            het bedrag Numeriek (getal met 2 decimalen);

        """;

    private static String rule(String target, String value) {
        return VONDST + "Regel " + target + "\n    geldig altijd\n        De " + target + " van een Vondst moet berekend worden als "
                + value + ".\n";
    }

    /** The codes and parameters of the errors of {@code source}. */
    private static List<String> errors(String source) {
        RegelSpraakModel m = Resolver.resolve(source);
        return m.errors().stream().map(d -> d.code() + " " + d.params()).toList();
    }

    private static void refused(String source, String code, Map<String, String> params) {
        assertEquals(List.of(code + " " + params), errors(source));
    }

    private static void passes(String source) {
        assertEquals(List.of(), errors(source));
    }

    /** The parameters of a refusal, in the order its code names them. */
    private static Map<String, String> params(String... namesAndValues) {
        Map<String, String> m = new java.util.LinkedHashMap<>();
        for (int i = 0; i < namesAndValues.length; i += 2) m.put(namesAndValues[i], namesAndValues[i + 1]);
        return m;
    }

    private static Map<String, String> exceeds(int value, int target) {
        return params("valueDecimals", Integer.toString(value), "targetDecimals", Integer.toString(target));
    }

    private static Map<String, String> unbounded(int target) {
        return params("targetDecimals", Integer.toString(target));
    }

    // wf023-precision-quotient-by-attribute
    @Test void a_quotient_by_an_attribute_has_no_bound() {
        refused(rule("aandeel", "zijn opbrengst gedeeld door zijn aantal deelnemers"), "RGS-370", unbounded(0));
        passes(rule("aandeel", "zijn opbrengst gedeeld door zijn aantal deelnemers naar beneden afgerond op 0 decimalen"));
        passes(rule("vrije aandeel", "zijn opbrengst gedeeld door zijn aantal deelnemers"));
    }

    // wf023-precision-quotient-by-three, wf023-precision-quotient-by-four
    @Test void a_quotient_by_a_literal_is_a_product_with_its_reciprocal() {
        refused(rule("derde deel", "zijn opbrengst gedeeld door 3"), "RGS-370", unbounded(2));
        refused(rule("kwart", "zijn opbrengst gedeeld door 4"), "RGS-338", exceeds(2, 1));
        passes(rule("kwart", "zijn opbrengst gedeeld door 2"));
        // a toeslag of 1 decimal over 8: 1/8 has three decimals, 1 + 3 = 4 does not fit 3
        refused(rule("halve kwart", "zijn toeslag gedeeld door 8"), "RGS-338", exceeds(4, 3));
        passes(rule("halve kwart", "zijn toeslag gedeeld door 4"));
    }

    // wf023-precision-percentage-literal, wf023-precision-percentage-attribute
    @Test void a_percentage_adds_the_two_decimals_of_its_hundred() {
        refused(rule("heffing", "21% van zijn opbrengst"), "RGS-338", exceeds(2, 1));
        passes(rule("heffing", "20% van zijn opbrengst"));
        String parameter = "Parameter het heffingspercentage : Percentage (getal met 1 decimalen)\n\n";
        refused(parameter + rule("brede heffing", "het heffingspercentage van zijn taxatie"), "RGS-338", exceeds(5, 2));
        passes(parameter + rule("brede heffing", "(het heffingspercentage van zijn taxatie) naar beneden afgerond op 2 decimalen"));
    }

    // wf023-precision-maximum
    @Test void a_maximum_takes_the_highest_precision_of_its_operands() {
        refused(rule("hoogste bod", "de maximale waarde van zijn taxatie en zijn opbrengst"), "RGS-338", exceeds(2, 0));
        refused(rule("hoogste bod", "de maximale waarde van zijn opbrengst en zijn taxatie"), "RGS-338", exceeds(2, 0));
        passes(rule("hoogste bod", "de maximale waarde van zijn opbrengst en zijn aantal deelnemers"));
    }

    // wf023-precision-unbounded-read
    @Test void a_read_of_the_unbounded_form_has_no_bound() {
        refused(rule("bedrag", "zijn vrije factor"), "RGS-370", unbounded(2));
        passes(rule("bedrag", "zijn vrije factor rekenkundig afgerond op 2 decimalen"));
    }

    // wf023-precision-product, wf023-precision-sum (refused before the gate too, by the type of the composite)
    @Test void a_product_sums_and_a_sum_takes_the_highest() {
        refused(rule("waarde", "zijn taxatie maal zijn factor"), "RGS-338", exceeds(4, 2));
        refused(rule("smalle waarde", "zijn taxatie plus zijn toeslag"), "RGS-338", exceeds(2, 1));
        passes(rule("waarde", "zijn taxatie plus zijn toeslag"));
    }

    @Test void a_literal_factor_counts_as_alef_counts_it() {
        // 2 decimals times 100 lose two, so the product of a taxatie and 100 is whole
        passes(rule("aandeel", "zijn taxatie maal 100"));
        // times 0,5: one decimal more
        refused(rule("aandeel", "zijn opbrengst maal 0,5"), "RGS-338", exceeds(1, 0));
    }

    // wf023-precision-unit-factor
    @Test void a_unit_conversion_counts_in_the_bound() {
        String model = """
            Eenheidsysteem afstand
                de meter m
                de kilometer km = 1000 m

            Objecttype de Vondst (mv: Vondsten)
                de sleeplengte Numeriek (geheel getal) met eenheid m;
                de sleepafstand Numeriek (geheel getal) met eenheid km;
                de gemeten afstand Numeriek (getal met 1 decimalen) met eenheid km;
                de lengte Numeriek (geheel getal) met eenheid m;

            """;
        refused(model + "Regel sleepafstand\n    geldig altijd\n        De sleepafstand van een Vondst moet gesteld worden op zijn sleeplengte.\n",
                "RGS-338", exceeds(3, 0));
        // a kilometre of one decimal is a whole number of metres
        passes(model + "Regel lengte\n    geldig altijd\n        De lengte van een Vondst moet gesteld worden op zijn gemeten afstand.\n");
        // the right operand of a sum is restated into the unit of the left: metres into kilometres add three decimals
        refused(model + "Regel sleepafstand\n    geldig altijd\n        De sleepafstand van een Vondst moet berekend worden als "
                + "zijn gemeten afstand plus zijn sleeplengte.\n", "RGS-338", exceeds(3, 0));
    }

    // wf023-precision-time-proportional-part
    @Test void a_time_proportional_part_has_no_bound() {
        String model = """
            Objecttype de Bakenmeester (mv: Bakenmeesters) (bezield)
                het onderhoudstarief Numeriek (getal met 2 decimalen) voor elk jaar;
                de onderhoudsbijdrage Numeriek (getal met 2 decimalen) voor elke maand;
                is dienstdoend kenmerk (bijvoeglijk) voor elke maand;

            Regel onderhoudsbijdrage van een bakenmeester
                geldig altijd
                    De onderhoudsbijdrage van een Bakenmeester moet gesteld worden op\s""";
        refused(model + "het tijdsevenredig deel per maand van zijn onderhoudstarief gedurende de tijd dat hij dienstdoend is.\n",
                "RGS-370", unbounded(2));
        passes(model + "(het tijdsevenredig deel per maand van zijn onderhoudstarief gedurende de tijd dat hij dienstdoend is)"
                + " rekenkundig afgerond op 2 decimalen.\n");
    }

    // wf023-precision-creation-member
    @Test void a_member_initialisation_of_an_object_creation_passes_the_gate() {
        String model = """
            Objecttype het Strandrecht (mv: Strandrechten)
                de uitkering Numeriek (niet-negatief getal met 2 decimalen);

            Objecttype de Melding (mv: Meldingen)
                de omvang Numeriek (niet-negatief getal met 2 decimalen);
                het aantal vinders Numeriek (geheel getal);

            Feittype strandmelding
                de melding Melding
                het gemeld strandrecht Strandrecht
            Eén melding meldt één gemeld strandrecht

            Regel maak een strandrecht bij elke melding
                geldig altijd
                    Een Melding heeft een gemeld strandrecht met uitkering gelijk aan\s""";
        refused(model + "zijn omvang gedeeld door zijn aantal vinders.\n", "RGS-370", unbounded(2));
        passes(model + "zijn omvang gedeeld door zijn aantal vinders naar beneden afgerond op 2 decimalen.\n");
    }

    // wf023-precision-allocation-share
    @Test void an_allocation_is_bounded_by_its_rounding_or_its_pot() {
        String model = """
            Objecttype de Berging (mv: Bergingen)
                de opbrengst Numeriek (getal met 2 decimalen);
                de vrije opbrengst Numeriek (getal);
                de onverdeelde rest Numeriek (getal met 2 decimalen);

            Objecttype de Berger (mv: Bergers) (bezield)
                het bergingsaandeel (mv: bergingsaandelen) Numeriek (geheel getal);
                de weging Numeriek (getal met 2 decimalen);

            Feittype bergingsdeelname
                de berging Berging
                de berger (mv: bergers) Berger
            Eén berging heeft meerdere bergers

            Regel verdeel de opbrengst van een berging
                geldig altijd
            """;
        String rest = "        Als onverdeelde rest blijft de onverdeelde rest van de berging over.\n";
        refused(model + "        De opbrengst van een berging wordt verdeeld over de bergingsaandelen van alle bergers van de berging, "
                + "waarbij wordt verdeeld:\n        - in gelijke delen.\n" + rest, "RGS-340",
                params("shareDecimals", "2", "targetDecimals", "0"));
        refused(model + "        De vrije opbrengst van een berging wordt verdeeld over de bergingsaandelen van alle bergers van de "
                + "berging, waarbij wordt verdeeld:\n        - in gelijke delen.\n" + rest, "RGS-371", unbounded(0));
        passes(model + "        De opbrengst van een berging wordt verdeeld over de bergingsaandelen van alle bergers van de berging, "
                + "waarbij wordt verdeeld:\n        - in gelijke delen,\n        - afgerond op 0 decimalen naar beneden.\n" + rest);
        // the weight is not typed: the pot is whole after the rounding, whatever the weight's decimals
        passes(model + "        De opbrengst van een berging wordt verdeeld over de bergingsaandelen van alle bergers van de berging, "
                + "waarbij wordt verdeeld:\n        - naar rato van de weging,\n        - afgerond op 0 decimalen naar beneden.\n" + rest);
    }
}
