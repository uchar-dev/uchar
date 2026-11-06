package dev.uchar.regelspraak;

import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The precision gate of RUNTIME-SEMANTICS ch. 2.4: a write whose precision bound (ch. 2.3, the ALEF form), restated into
 * the unit of its target, exceeds the target's decimals is RGS-372, one without a finite bound RGS-370; the share of an
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
        refused(rule("kwart", "zijn opbrengst gedeeld door 4"), "RGS-372", exceeds(2, 1));
        passes(rule("kwart", "zijn opbrengst gedeeld door 2"));
        // a toeslag of 1 decimal over 8: 1/8 has three decimals, 1 + 3 = 4 does not fit 3
        refused(rule("halve kwart", "zijn toeslag gedeeld door 8"), "RGS-372", exceeds(4, 3));
        passes(rule("halve kwart", "zijn toeslag gedeeld door 4"));
    }

    // wf023-precision-percentage-literal, wf023-precision-percentage-attribute
    @Test void a_percentage_adds_the_two_decimals_of_its_hundred() {
        refused(rule("heffing", "21% van zijn opbrengst"), "RGS-372", exceeds(2, 1));
        passes(rule("heffing", "20% van zijn opbrengst"));
        String parameter = "Parameter het heffingspercentage : Percentage (getal met 1 decimalen)\n\n";
        refused(parameter + rule("brede heffing", "het heffingspercentage van zijn taxatie"), "RGS-372", exceeds(5, 2));
        passes(parameter + rule("brede heffing", "(het heffingspercentage van zijn taxatie) naar beneden afgerond op 2 decimalen"));
    }

    // wf023-precision-maximum
    @Test void a_maximum_takes_the_highest_precision_of_its_operands() {
        refused(rule("hoogste bod", "de maximale waarde van zijn taxatie en zijn opbrengst"), "RGS-372", exceeds(2, 0));
        refused(rule("hoogste bod", "de maximale waarde van zijn opbrengst en zijn taxatie"), "RGS-372", exceeds(2, 0));
        passes(rule("hoogste bod", "de maximale waarde van zijn opbrengst en zijn aantal deelnemers"));
    }

    // wf023-precision-unbounded-read
    @Test void a_read_of_the_unbounded_form_has_no_bound() {
        refused(rule("bedrag", "zijn vrije factor"), "RGS-370", unbounded(2));
        passes(rule("bedrag", "zijn vrije factor rekenkundig afgerond op 2 decimalen"));
    }

    // wf023-precision-product, wf023-precision-sum (refused before the gate too, by the type of the composite)
    @Test void a_product_sums_and_a_sum_takes_the_highest() {
        refused(rule("waarde", "zijn taxatie maal zijn factor"), "RGS-372", exceeds(4, 2));
        refused(rule("smalle waarde", "zijn taxatie plus zijn toeslag"), "RGS-372", exceeds(2, 1));
        passes(rule("waarde", "zijn taxatie plus zijn toeslag"));
    }

    @Test void a_literal_factor_counts_as_alef_counts_it() {
        // 2 decimals times 100 lose two, so the product of a taxatie and 100 is whole
        passes(rule("aandeel", "zijn taxatie maal 100"));
        // times 0,5: one decimal more
        refused(rule("aandeel", "zijn opbrengst maal 0,5"), "RGS-372", exceeds(1, 0));
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
                "RGS-372", exceeds(3, 0));
        // a kilometre of one decimal is a whole number of metres
        passes(model + "Regel lengte\n    geldig altijd\n        De lengte van een Vondst moet gesteld worden op zijn gemeten afstand.\n");
        // the right operand of a sum is restated into the unit of the left: metres into kilometres add three decimals
        refused(model + "Regel sleepafstand\n    geldig altijd\n        De sleepafstand van een Vondst moet berekend worden als "
                + "zijn gemeten afstand plus zijn sleeplengte.\n", "RGS-372", exceeds(3, 0));
    }

    // wf023-precision-time-proportional-part
    @Test void a_time_proportional_part_has_no_bound_and_its_write_into_a_timeline_stands_outside_the_gate() {
        String model = """
            Objecttype de Bakenmeester (mv: Bakenmeesters) (bezield)
                het onderhoudstarief Numeriek (getal met 2 decimalen) voor elk jaar;
                de onderhoudsbijdrage Numeriek (getal met 2 decimalen)%s;
                is dienstdoend kenmerk (bijvoeglijk) voor elke maand;

            Regel onderhoudsbijdrage van een bakenmeester
                geldig altijd
                    De onderhoudsbijdrage van een Bakenmeester moet gesteld worden op het tijdsevenredig deel per maand \
            van zijn onderhoudstarief gedurende de tijd dat hij dienstdoend is.
            """;
        refused(model.formatted(""), "RGS-370", unbounded(2));
        // into a monthly timeline the pro-rata write is the one computed write, and no executor rounds around it
        passes(model.formatted(" voor elke maand"));
    }

    /** The negative wf023-precision-timeline-target: the five other rules are those of the golden
     *  jutterswet-rekenen-per-dag, each in the gate per segment, and each fits; {@code %s} is the datatype of the
     *  loonverhouding and {@code %s} what its rule writes after the quotient. */
    private static final String JUTTER = """
        Objecttype de Jutter (mv: Jutters) (bezield)
            het strandloon Numeriek (geheel getal) voor elke dag;
            de stormtoeslag Numeriek (geheel getal) voor elke dag;
            het loonproduct Numeriek (geheel getal) voor elke dag;
            het nettoloon Numeriek (geheel getal) voor elke dag;
            de loonverhouding Numeriek (%s) voor elke dag;
            de afgekapte loonverhouding Numeriek (getal met 5 decimalen) voor elke dag;
            de strandheffing Numeriek (getal met 2 decimalen) voor elke dag;
            het jaarproduct Numeriek (geheel getal);

        Regel loonproduct van een jutter
            geldig altijd
                Het loonproduct van een Jutter moet berekend worden als zijn strandloon maal zijn stormtoeslag.

        Regel nettoloon van een jutter
            geldig altijd
                Het nettoloon van een Jutter moet berekend worden als zijn strandloon verminderd met zijn stormtoeslag.

        Regel loonverhouding van een jutter
            geldig altijd
                De loonverhouding van een Jutter moet berekend worden als zijn strandloon gedeeld door zijn stormtoeslag%s.

        Regel afgekapte loonverhouding van een jutter
            geldig altijd
                De afgekapte loonverhouding van een Jutter moet berekend worden als zijn strandloon gedeeld door (ABS) \
        zijn stormtoeslag.

        Regel strandheffing van een jutter
            geldig altijd
                De strandheffing van een Jutter moet berekend worden als 10%% van zijn strandloon.

        Regel jaarproduct van een jutter
            geldig altijd
                Het jaarproduct van een Jutter moet gesteld worden op het totaal van zijn loonproduct.
        """;

    // wf023-precision-timeline-target
    @Test void a_rule_on_a_timeline_target_stands_in_the_gate_per_segment() {
        refused(JUTTER.formatted("geheel getal", ""), "RGS-370", unbounded(0));
        // the golden: the quotient into the unbounded form
        passes(JUTTER.formatted("getal", ""));
        passes(JUTTER.formatted("geheel getal", " naar beneden afgerond op 0 decimalen"));
        // 15% of a whole number has two decimals in every segment, 10% one
        String heffing = """
            Objecttype de Jutter (mv: Jutters) (bezield)
                het strandloon Numeriek (geheel getal) voor elke dag;
                de smalle heffing Numeriek (getal met 1 decimalen) voor elke dag;

            Regel smalle heffing van een jutter
                geldig altijd
                    De smalle heffing van een Jutter moet berekend worden als %s van zijn strandloon.
            """;
        refused(heffing.formatted("15%"), "RGS-372", exceeds(2, 1));
        passes(heffing.formatted("10%"));
    }

    @Test void a_time_proportional_part_under_a_variable_part_stands_in_the_gate() {
        // the pro-rata write is read off the value's own node, as the verifier reads it: under the chain of a
        // variable part the value is the chain, and the gate holds it
        refused("""
            Objecttype de Bakenmeester (mv: Bakenmeesters) (bezield)
                het onderhoudstarief Numeriek (getal met 2 decimalen) voor elk jaar;
                de onderhoudsbijdrage Numeriek (getal met 2 decimalen) voor elke maand;
                is dienstdoend kenmerk (bijvoeglijk) voor elke maand;

            Regel onderhoudsbijdrage van een bakenmeester
                geldig altijd
                    De onderhoudsbijdrage van een Bakenmeester moet gesteld worden op de bijdrage.
                    Daarbij geldt:
                        de bijdrage is het tijdsevenredig deel per maand van zijn onderhoudstarief gedurende de tijd dat \
            hij dienstdoend is.
            """, "RGS-370", unbounded(2));
    }

    /** The negative wf023-precision-calendar-total, the golden jutterswet-verzuimrente-dagen-afgerond without its rounding;
     *  {@code %s} is the value of the verzuimrente. */
    private static final String AFDRACHT = """
        Eenheidsysteem Munt
            de euro (mv: euros) EUR €

        Eenheidsysteem Tijd
            de dag (mv: dagen) dg
            het jaar (mv: jaren) jr

        Parameter de verzuimrentevoet : Numeriek (getal met 2 decimalen) met eenheid 1/jr voor elke maand;

        Objecttype de Afdracht (mv: Afdrachten)
            het afdrachtbedrag Numeriek (geheel getal) met eenheid €;
            de vervaldatum Datum in dagen;
            de betaaldatum Datum in dagen;
            is rentedragend kenmerk (bijvoeglijk) voor elke dag;
            de dagrente Numeriek (getal met 2 decimalen) met eenheid €/jr voor elke dag;
            de verzuimrente Numeriek (getal met 2 decimalen) met eenheid €;

        Regel dagrente
            geldig altijd
                De dagrente van een Afdracht moet berekend worden als de verzuimrentevoet maal zijn afdrachtbedrag.

        Regel verzuimrente
            geldig altijd
                De verzuimrente van een Afdracht moet berekend worden als %s.
        """;

    // wf023-precision-calendar-total
    @Test void a_total_the_calendar_integrates_has_no_bound_unless_it_counts_whole_days() {
        String total = "het totaal van zijn dagrente gedurende de tijd dat hij rentedragend is";
        // a day weighs 1/365 or 1/366 of its year
        refused(AFDRACHT.formatted(total), "RGS-370", unbounded(2));
        // the anchor rounds it to the cent
        passes(AFDRACHT.formatted("(" + total + ") rekenkundig afgerond op 2 decimalen"));
        // over a grid of the month family a rate per day counts whole days, and keeps the bound of its operand; a rate
        // per week weighs 1/7 of the days of the month
        String huur = """
            Eenheidsysteem Munt
                de euro (mv: euros) EUR €

            Eenheidsysteem Tijd
                de dag (mv: dagen) dg
                de week (mv: weken) wk = 7 dg
                de maand (mv: maanden) mnd

            Objecttype de Strandtent (mv: Strandtenten)
                de huur Numeriek (geheel getal) met eenheid €/%s voor elke maand;
                de huursom Numeriek (geheel getal) met eenheid €;

            Regel huursom van een strandtent
                geldig altijd
                    De huursom van een Strandtent moet berekend worden als het totaal van zijn huur.
            """;
        passes(huur.formatted("dg"));
        refused(huur.formatted("wk"), "RGS-370", unbounded(0));
        // a rate whose period is declared in the class of the grid is restated by that factor, not by the calendar: with
        // "jr = 400 dg", which WF-009 refuses, a day is 1/400 year, four decimals more than the two of the dagrente
        String declared = AFDRACHT.formatted(total).replace("het jaar (mv: jaren) jr\n", "het jaar (mv: jaren) jr = 400 dg\n");
        assertEquals(List.of("RGS-373 " + params("unit", "jr", "unitFamily", "de maandfamilie (maand, kwartaal, jaar)",
                        "canonical", "dg", "canonicalFamily", "de dagfamilie (dag, week)", "unitSystem", "Tijd"),
                "RGS-372 " + exceeds(6, 2)), errors(declared));
    }

    // wf023-precision-partial-unit-clip
    @Test void a_clip_by_a_characteristic_can_split_a_period_of_the_grid() {
        String model = """
            Tijdlijn het stormjaar heeft tijdvakken van een jaar startend op dag 1 van maand 10;

            Objecttype de Jutter (mv: Jutters) (bezield)
                is wachtplichtig kenmerk (bijvoeglijk) voor elke week startend op 30-9-2024;
                de stormtoelage Numeriek (geheel getal) voor elke stormjaar;
                de toelagesom Numeriek (geheel getal);

            Regel toelagesom van een jutter
                geldig altijd
                    De toelagesom van een Jutter moet gesteld worden op %s.
            """;
        String total = "het totaal van zijn stormtoelage gedurende de tijd dat hij wachtplichtig is";
        // a characteristic holds on periods of days: 100 over five days around 1 October is 6090/4453
        refused(model.formatted(total), "RGS-370", unbounded(0));
        passes(model.formatted("(" + total + ") rekenkundig afgerond op 0 decimalen"));
        // the same total without a clip: the case lays every segment on the walls of the storm year
        passes(model.formatted("het totaal van zijn stormtoelage"));
        // on the day grid every bound is a day, and no segment holds a part of a period
        passes(model.replace("voor elke stormjaar", "voor elke dag").formatted(total));
    }

    // ref/test/fixtures/partial-unit/calendar without its rounding
    @Test void a_rate_per_day_counts_whole_days_whatever_the_clip_cuts() {
        // the calendar measures the clipped segments in days, not in months
        passes("""
            Eenheidsysteem Munt
                de euro (mv: euros) EUR €

            Eenheidsysteem Tijd
                de dag (mv: dagen) dg
                de maand (mv: maanden) mnd

            Objecttype de Jutter (mv: Jutters) (bezield)
                is wachtplichtig kenmerk (bijvoeglijk) voor elke maand;
                het dagtarief Numeriek (geheel getal) met eenheid €/dg voor elke maand;
                de dagsom Numeriek (geheel getal) met eenheid €;

            Regel dagsom van een jutter
                geldig altijd
                    De dagsom van een Jutter moet gesteld worden op het totaal van zijn dagtarief gedurende de tijd dat hij wachtplichtig is.
            """);
    }

    /** The negative wf023-precision-partial-unit-written; {@code %s} stands where the total stands among the rules. */
    private static final String WACHT = """
        Objecttype de Jutter (mv: Jutters) (bezield)
            is wachtplichtig kenmerk (bijvoeglijk) voor elke maand;
            de wachttoelage Numeriek (geheel getal) voor elke maand;
            de wachtsom Numeriek (geheel getal);
            de vrije wachtsom Numeriek (getal);

        %sRegel wachttoelage van een jutter
            geldig altijd
                De wachttoelage van een Jutter moet gesteld worden op 30 indien hij wachtplichtig is.
        %s""";

    // wf023-precision-partial-unit-written
    @Test void the_total_of_a_timeline_a_rule_writes_can_split_a_period_of_the_grid() {
        String total = """
            Regel wachtsom van een jutter
                geldig altijd
                    De wachtsom van een Jutter moet gesteld worden op %s.

            """;
        String plain = total.formatted("het totaal van zijn wachttoelage");
        // the rule writes 30 on the days a Jutter is wachtplichtig, so a part of a month counts by its days
        refused(WACHT.formatted("", "\n" + plain), "RGS-370", unbounded(0));
        // the writer may stand after the total: the gate is judged again once every writer has registered
        refused(WACHT.formatted(plain, ""), "RGS-370", unbounded(0));
        passes(WACHT.formatted(total.formatted("(het totaal van zijn wachttoelage) rekenkundig afgerond op 0 decimalen"), ""));
        passes(WACHT.formatted(total.replace("De wachtsom", "De vrije wachtsom").formatted("het totaal van zijn wachttoelage"), ""));
    }

    /** {@code %s} is the grid of the peilstand, {@code %s} its rule, {@code %s} the clip of the total. */
    private static final String PEIL = """
        Tijdlijn het stormjaar heeft tijdvakken van een jaar startend op dag 1 van maand 10;

        Objecttype de Jutter (mv: Jutters) (bezield)
            is wachtplichtig kenmerk (bijvoeglijk) voor elke maand;
            de peilstand Numeriek (geheel getal) voor %s;
            de wachtvergoeding Numeriek (geheel getal) voor elke maand;
            de jaarpremie Numeriek (geheel getal) voor elk jaar;
            de peilsom Numeriek (geheel getal);

        %sRegel peilsom van een jutter
            geldig altijd
                De peilsom van een Jutter moet gesteld worden op het totaal van zijn %s gedurende de tijd dat zijn peilstand groter is dan 10.
        """;

    // wf023-precision-partial-unit-compared-finer, -compared-written, -compared-other-start
    @Test void a_clip_by_a_comparison_cuts_on_walls_where_the_compared_timeline_fits_the_grid() {
        String writer = """
            Regel peilstand van een jutter
                geldig altijd
                    De peilstand van een Jutter moet gesteld worden op 20 indien hij wachtplichtig is.

            """;
        // corpus/golden/jutterswet-peilwacht: both timelines monthly, the case lays both on the walls of the month
        passes(PEIL.formatted("elke maand", "", "wachtvergoeding"));
        // a year of the calendar is twelve months, so a clip by a yearly timeline cuts on walls of the month
        passes(PEIL.formatted("elk jaar", "", "wachtvergoeding"));
        refused(PEIL.formatted("elke dag", "", "wachtvergoeding"), "RGS-370", unbounded(0));
        refused(PEIL.formatted("elke maand", writer, "wachtvergoeding"), "RGS-370", unbounded(0));
        refused(PEIL.formatted("elke stormjaar", "", "jaarpremie"), "RGS-370", unbounded(0));
        // a clip by a characteristic of the month: its periods are days
        refused(PEIL.formatted("elke maand", "", "wachtvergoeding").replace("zijn peilstand groter is dan 10", "hij wachtplichtig is"),
                "RGS-370", unbounded(0));
    }

    // corpus/golden/jutterswet-keurlast
    @Test void the_filter_of_a_fold_over_a_set_selects_members_and_cuts_nothing() {
        passes("""
            Eenheidsysteem Munt
                de euro (mv: euros) EUR €

            Eenheidsysteem Keurtijd
                de maand mnd

            Objecttype de Jutter (mv: Jutters) (bezield)
                de keurlast Numeriek (geheel getal) met eenheid €;

            Objecttype de Vondst (mv: Vondsten)
                het keurtarief (mv: keurtarieven) Numeriek (geheel getal) met eenheid €/mnd voor elke maand;
                is gekeurd kenmerk (bijvoeglijk);

            Feittype keuring
                de keurende jutter Jutter
                de vondst (mv: vondsten) Vondst
            Eén keurende jutter keurt meerdere vondsten

            Regel keurlast van een jutter
                geldig altijd
                    De keurlast van een Jutter moet berekend worden als het totaal van het keurtarief van alle vondsten van de Jutter die gekeurd zijn.
            """);
    }

    @Test void a_sum_across_units_is_bound_in_the_unit_that_bounds_it_best() {
        String model = """
            Eenheidsysteem Termijn
                de maand mnd
                het jaar jr = 12 mnd

            Objecttype de Bergingsvergunning (mv: Bergingsvergunningen)
                de basisduur Numeriek (geheel getal) met eenheid mnd;
                de looptijd Numeriek (geheel getal) met eenheid mnd;
                de lange looptijd Numeriek (geheel getal) met eenheid jr;

            """;
        // 2 jr plus b mnd is b/12 jr in the left unit, and whole in months
        passes(model + "Regel looptijd\n    geldig altijd\n        De looptijd van een Bergingsvergunning moet berekend worden "
                + "als 2 jr plus zijn basisduur.\n");
        // into years it does not fit whatever the unit it is bound in
        refused(model + "Regel lange looptijd\n    geldig altijd\n        De lange looptijd van een Bergingsvergunning moet "
                + "berekend worden als 2 jr plus zijn basisduur.\n", "RGS-370", unbounded(0));
    }

    // wf023-precision-creation-member
    @Test void a_member_assignment_of_an_object_creation_passes_the_gate() {
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
