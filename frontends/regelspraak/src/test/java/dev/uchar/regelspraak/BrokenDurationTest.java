package dev.uchar.regelspraak;

import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A date plus or min a duration whose count can carry a fraction (grammar/DIVERGENCES.md, "A date plus or min a
 * duration that can carry a fraction"). Every executor truncates a fractional count toward zero before the calendar
 * step (RUNTIME-SEMANTICS ch. 3.8), so "zijn startdag plus 20,5 mnd" lands on the first of a month where the
 * sentence says half a month later. Measured on the frontend before this witness: check exit 0 without a
 * diagnostic, and ref, the ASP backend and both Drools branches agreed on the truncated date (01-03-2026 plus 20,5
 * mnd gave 01-11-2027). Now the resolver refuses the form in the author's words: the duration and its unit, the
 * truncation, and the forms that say what is meant — the truncation written as a rounding toward zero, or the rest
 * in a smaller unit where the date has one. A duration whose count is whole by its form keeps playing.
 */
class BrokenDurationTest {

    private static List<String> faults(String source) {
        RegelSpraakModel m = Resolver.resolve(source);
        return m.errors().stream().map(RegelSpraakModel.Diagnostic::message).toList();
    }

    private static final String VERGUNNINGEN = """
        Eenheidsysteem Tijd
            de dag dg
            de week wk = 7 dg
            de maand mnd
            het kwartaal kw = 3 mnd
            het jaar jr = 12 mnd

        Dagsoort de springtijdag (mv: springtijdagen);

        Parameter de standaardduur : Numeriek (getal met 1 decimalen) met eenheid mnd;

        Objecttype de Vergunning (mv: Vergunningen)
            de startdag Datum in dagen;
            de begintijd Datum en tijd in millisecondes;
            de peildag Datum in dagen;
            de duur Numeriek (getal met 1 decimalen) met eenheid mnd;
            de vrije duur Numeriek (getal) met eenheid mnd;
            de ronde duur Numeriek (geheel getal) met eenheid mnd;
            de dagduur Numeriek (getal met 1 decimalen) met eenheid dg;
            de weekduur Numeriek (getal met 1 decimalen) met eenheid wk;
            de kwartaalduur Numeriek (getal met 1 decimalen) met eenheid kw;
            de jaarduur Numeriek (getal met 1 decimalen) met eenheid jr;
            de uurduur Numeriek (getal met 1 decimalen) met eenheid u;
            de minuutduur Numeriek (getal met 1 decimalen) met eenheid minuut;
            de secondeduur Numeriek (getal met 1 decimalen) met eenheid sec;
            de msduur Numeriek (getal met 1 decimalen) met eenheid ms;
            het getijbestek Numeriek (getal met 1 decimalen) met eenheid springtijdagen;
            de einddag Datum in dagen;
            de eindtijd Datum en tijd in millisecondes;
            de status Numeriek (geheel getal);

        Regel Dagsoortdefinitie springtijdag
            geldig altijd
                Een dag is een springtijdag
                indien de dag uit de dag gelijk is aan 15.

        """;

    private static String einddag(String value) {
        return VERGUNNINGEN + "Regel einddag\n    geldig altijd\n        De einddag van een Vergunning moet berekend "
                + "worden als " + value + ".\n";
    }

    private static String eindtijd(String value) {
        return VERGUNNINGEN + "Regel eindtijd\n    geldig altijd\n        De eindtijd van een Vergunning moet berekend "
                + "worden als " + value + ".\n";
    }

    private static String condition(String value) {
        return VERGUNNINGEN + "Regel status\n    geldig altijd\n        De status van een Vergunning moet gesteld "
                + "worden op 1\n        indien zijn peildag later is dan " + value + ".\n";
    }

    private static String justification(String value) {
        return VERGUNNINGEN + """
            Regel status
                geldig altijd
                    De status van een Vergunning moet gesteld worden op 0.

            Uitzondering op status: verlopen
                geldig altijd
                    De status van een Vergunning moet gesteld worden op 1
            """ + "        indien zijn peildag later is dan " + value + ".\n";
    }

    private static String consequence(String value) {
        return VERGUNNINGEN + """
            Regel einddag
                geldig altijd
                    De einddag van een Vergunning moet gesteld worden op zijn startdag.

            Uitzondering op einddag: verlengd
                geldig altijd
            """ + "        De einddag van een Vergunning moet berekend worden als " + value + "\n"
                + "        indien zijn ronde duur groter is dan 0 mnd.\n";
    }

    /** The refusal: {@code example} is ch. 3.8's example in the duration's unit, or the literal truncated;
     *  {@code suffix} the smaller unit to step the rest in, or why the date has none. */
    private static String sentence(String op, String written, String duration, String cause, String eenheid,
                              String example, String truncate, String suffix) {
        return "datum " + op + " duur '" + written + "': de duur '" + duration + "' " + cause
                + ", en de uitvoerders kappen een gebroken telling in " + eenheid + " naar nul af vóór de kalenderstap "
                + "(RUNTIME-SEMANTICS ch. 3.8: " + example + "); schrijf de afkapping uit als '" + truncate + "'" + suffix;
    }

    private static String nineteenth(String eenheid) {
        return "1,9 " + eenheid + " telt als 1 " + eenheid + " en -1,9 " + eenheid + " als -1 " + eenheid;
    }

    private static String takeAlong(String truncate, String op, String smaller) {
        return ", of neem het deel in een kleinere eenheid mee ('" + truncate + " " + op + " … " + smaller + "')";
    }

    /** The refusal of a duration that is no literal, with the next smaller unit or the closing sentence. */
    private static String read(String op, String left, String duration, String cause, String eenheid, String smaller) {
        String truncate = left + " " + op + " (" + duration + " richting nul afgerond op 0 decimalen)";
        String suffix = smaller.startsWith("; ") ? smaller : takeAlong(truncate, op, smaller);
        return sentence(op, left + " " + op + " " + duration, duration, cause, eenheid, nineteenth(eenheid), truncate, suffix);
    }

    private static final String NO_DAGDEEL = "; een datum in dagen kent geen eenheid kleiner dan een dag";

    // ---- the card's reproductions

    @Test void r1_an_attribute_with_a_decimal_in_months() {
        // before: check exit 0, no diagnostic; 01-03-2026 plus 20,5 mnd gave 01-11-2027 in every executor
        assertEquals(List.of("datum plus duur 'zijn startdag plus zijn duur': de duur 'zijn duur' is een getal met 1 "
                        + "decimalen, en de uitvoerders kappen een gebroken telling in mnd naar nul af vóór de "
                        + "kalenderstap (RUNTIME-SEMANTICS ch. 3.8: 1,9 mnd telt als 1 mnd en -1,9 mnd als -1 mnd); "
                        + "schrijf de afkapping uit als 'zijn startdag plus (zijn duur richting nul afgerond op 0 "
                        + "decimalen)', of neem het deel in een kleinere eenheid mee ('zijn startdag plus (zijn duur "
                        + "richting nul afgerond op 0 decimalen) plus … dagen')"),
                faults(einddag("zijn startdag plus zijn duur")));
    }

    @Test void r2_a_literal_with_a_rational_in_months() {
        // before: identical to R1, the literal on the wire as {"number": "20.5"}
        assertEquals(List.of("datum plus duur 'zijn startdag plus 20,5 mnd': de duur '20,5 mnd' heeft een breuk, en de "
                        + "uitvoerders kappen een gebroken telling in mnd naar nul af vóór de kalenderstap "
                        + "(RUNTIME-SEMANTICS ch. 3.8: 20,5 mnd telt als 20 mnd); schrijf de afkapping uit als 'zijn "
                        + "startdag plus 20 mnd', of neem het deel in een kleinere eenheid mee ('zijn startdag plus 20 "
                        + "mnd plus … dagen')"),
                faults(einddag("zijn startdag plus 20,5 mnd")));
    }

    @Test void r3_a_literal_with_a_rational_in_years_and_in_weeks() {
        // before: 1,5 jr stepped one year (01-03-2027), 2,5 wk two weeks (15-03-2026)
        assertEquals(List.of("datum plus duur 'zijn startdag plus 1,5 jr': de duur '1,5 jr' heeft een breuk, en de "
                        + "uitvoerders kappen een gebroken telling in jr naar nul af vóór de kalenderstap "
                        + "(RUNTIME-SEMANTICS ch. 3.8: 1,5 jr telt als 1 jr); schrijf de afkapping uit als 'zijn "
                        + "startdag plus 1 jr', of neem het deel in een kleinere eenheid mee ('zijn startdag plus 1 jr "
                        + "plus … maanden')"),
                faults(einddag("zijn startdag plus 1,5 jr")));
        assertEquals(List.of("datum plus duur 'zijn startdag plus 2,5 wk': de duur '2,5 wk' heeft een breuk, en de "
                        + "uitvoerders kappen een gebroken telling in wk naar nul af vóór de kalenderstap "
                        + "(RUNTIME-SEMANTICS ch. 3.8: 2,5 wk telt als 2 wk); schrijf de afkapping uit als 'zijn "
                        + "startdag plus 2 wk', of neem het deel in een kleinere eenheid mee ('zijn startdag plus 2 wk "
                        + "plus … dagen')"),
                faults(einddag("zijn startdag plus 2,5 wk")));
    }

    // ---- every unit on its date: dg, wk, mnd, kw, jr on a Datum in dagen; u, minuut, sec, ms on a datum-tijd

    @Test void every_unit_on_its_date() {
        String g = "is een getal met 1 decimalen";
        assertEquals(List.of(read("plus", "zijn startdag", "zijn dagduur", g, "dg", NO_DAGDEEL)),
                faults(einddag("zijn startdag plus zijn dagduur")));
        assertEquals(List.of(read("plus", "zijn startdag", "zijn weekduur", g, "wk", "dagen")),
                faults(einddag("zijn startdag plus zijn weekduur")));
        assertEquals(List.of(read("plus", "zijn startdag", "zijn kwartaalduur", g, "kw", "maanden")),
                faults(einddag("zijn startdag plus zijn kwartaalduur")));
        assertEquals(List.of(read("plus", "zijn startdag", "zijn jaarduur", g, "jr", "maanden")),
                faults(einddag("zijn startdag plus zijn jaarduur")));
        assertEquals(List.of(read("plus", "zijn begintijd", "zijn dagduur", g, "dg", "uur")),
                faults(eindtijd("zijn begintijd plus zijn dagduur")));
        assertEquals(List.of(read("plus", "zijn begintijd", "zijn uurduur", g, "u", "minuten")),
                faults(eindtijd("zijn begintijd plus zijn uurduur")));
        assertEquals(List.of(read("plus", "zijn begintijd", "zijn minuutduur", g, "minuut", "seconden")),
                faults(eindtijd("zijn begintijd plus zijn minuutduur")));
        assertEquals(List.of(read("plus", "zijn begintijd", "zijn secondeduur", g, "sec", "milliseconden")),
                faults(eindtijd("zijn begintijd plus zijn secondeduur")));
        assertEquals(List.of(read("plus", "zijn begintijd", "zijn msduur", g, "ms",
                        "; een milliseconde is de kleinste eenheid")),
                faults(eindtijd("zijn begintijd plus zijn msduur")));
        assertEquals(List.of(sentence("plus", "zijn startdag plus 2,5 dg", "2,5 dg", "heeft een breuk", "dg",
                        "2,5 dg telt als 2 dg", "zijn startdag plus 2 dg", NO_DAGDEEL)),
                faults(einddag("zijn startdag plus 2,5 dg")));
        assertEquals(List.of(sentence("plus", "zijn begintijd plus 1,5 u", "1,5 u", "heeft een breuk", "u",
                        "1,5 u telt als 1 u", "zijn begintijd plus 1 u",
                        takeAlong("zijn begintijd plus 1 u", "plus", "minuten"))),
                faults(eindtijd("zijn begintijd plus 1,5 u")));
    }

    @Test void a_day_kind_as_unit() {
        // ch. 3.8: a fractional count truncates toward zero before the walk as well
        assertEquals(List.of(read("plus", "zijn startdag", "zijn getijbestek", "is een getal met 1 decimalen",
                        "springtijdagen", "; een dagsoort kent geen kleinere eenheid")),
                faults(einddag("zijn startdag plus zijn getijbestek")));
    }

    @Test void plus_and_minus() {
        assertEquals(List.of(read("min", "zijn startdag", "zijn duur", "is een getal met 1 decimalen", "mnd",
                        "dagen")),
                faults(einddag("zijn startdag min zijn duur")));
        assertEquals(List.of(sentence("min", "zijn startdag min 20,5 mnd", "20,5 mnd", "heeft een breuk", "mnd",
                        "20,5 mnd telt als 20 mnd", "zijn startdag min 20 mnd",
                        takeAlong("zijn startdag min 20 mnd", "min", "dagen"))),
                faults(einddag("zijn startdag min 20,5 mnd")));
        assertEquals(List.of(sentence("plus", "zijn startdag plus -20,5 mnd", "-20,5 mnd", "heeft een breuk", "mnd",
                        "-20,5 mnd telt als -20 mnd", "zijn startdag plus -20 mnd",
                        takeAlong("zijn startdag plus -20 mnd", "plus", "dagen"))),
                faults(einddag("zijn startdag plus -20,5 mnd")));
    }

    // ---- every source of a fraction: a parameter, an unbounded number, a computation without a whole outcome

    @Test void every_source_of_a_rational() {
        assertEquals(List.of(read("plus", "zijn startdag", "de standaardduur", "is een getal met 1 decimalen",
                        "mnd", "dagen")),
                faults(einddag("zijn startdag plus de standaardduur")));
        assertEquals(List.of(read("plus", "zijn startdag", "zijn vrije duur",
                        "is een getal zonder vast aantal decimalen", "mnd", "dagen")),
                faults(einddag("zijn startdag plus zijn vrije duur")));
        String division = "zijn ronde duur gedeeld door 2";
        assertEquals(List.of(sentence("plus", "zijn startdag plus (" + division + ")", division, "is een deling", "mnd",
                        nineteenth("mnd"), "zijn startdag plus (" + division + " richting nul afgerond op 0 decimalen)",
                        takeAlong("zijn startdag plus (" + division + " richting nul afgerond op 0 decimalen)", "plus",
                                "dagen"))),
                faults(einddag("zijn startdag plus (" + division + ")")));
        String pct = "50% van zijn ronde duur";
        assertEquals(List.of(sentence("plus", "zijn startdag plus (" + pct + ")", pct, "is een percentage", "mnd",
                        nineteenth("mnd"), "zijn startdag plus (" + pct + " richting nul afgerond op 0 decimalen)",
                        takeAlong("zijn startdag plus (" + pct + " richting nul afgerond op 0 decimalen)", "plus",
                                "dagen"))),
                faults(einddag("zijn startdag plus (" + pct + ")")));
        String rounded = "zijn vrije duur rekenkundig afgerond op 1 decimalen";
        assertEquals(List.of(sentence("plus", "zijn startdag plus (" + rounded + ")", rounded,
                        "is afgerond op 1 decimalen", "mnd", nineteenth("mnd"),
                        "zijn startdag plus (" + rounded + " richting nul afgerond op 0 decimalen)",
                        takeAlong("zijn startdag plus (" + rounded + " richting nul afgerond op 0 decimalen)", "plus",
                                "dagen"))),
                faults(einddag("zijn startdag plus (" + rounded + ")")));
        assertEquals(List.of(read("plus", "zijn startdag", "zijn ronde duur maal 1,5",
                        "kan een breuk dragen: '1,5' heeft een breuk", "mnd", "dagen")),
                faults(einddag("zijn startdag plus zijn ronde duur maal 1,5")));
    }

    @Test void a_conversion_to_a_greater_unit() {
        // "1 jr plus 6 mnd" is 1,5 jr: the sum is typed whole, the restatement into jr makes it fractional
        String sum = "1 jr plus 6 mnd";
        assertEquals(List.of(sentence("plus", "zijn startdag plus (" + sum + ")", sum,
                        "kan een breuk dragen: '6 mnd' wordt naar jr omgerekend (een mnd is een breuk van een jr)",
                        "jr", nineteenth("jr"), "zijn startdag plus (" + sum + " richting nul afgerond op 0 decimalen)",
                        takeAlong("zijn startdag plus (" + sum + " richting nul afgerond op 0 decimalen)", "plus",
                                "maanden"))),
                faults(einddag("zijn startdag plus (" + sum + ")")));
        String weeks = "zijn weekduur rekenkundig afgerond op 0 decimalen plus 3 dg";
        assertEquals(List.of(sentence("plus", "zijn startdag plus (" + weeks + ")", weeks,
                        "kan een breuk dragen: '3 dg' wordt naar wk omgerekend (een dg is een breuk van een wk)",
                        "wk", nineteenth("wk"), "zijn startdag plus (" + weeks + " richting nul afgerond op 0 decimalen)",
                        takeAlong("zijn startdag plus (" + weeks + " richting nul afgerond op 0 decimalen)", "plus",
                                "dagen"))),
                faults(einddag("zijn startdag plus (" + weeks + ")")));
    }

    @Test void a_variable_carries_the_rational_of_its_definition() {
        String source = VERGUNNINGEN + """
            Regel einddag
                geldig altijd
                    De einddag van een Vergunning moet berekend worden als zijn startdag plus de halve duur.
                    Daarbij geldt:
                        de halve duur is zijn ronde duur gedeeld door 2.
            """;
        assertEquals(List.of(read("plus", "zijn startdag", "de halve duur",
                        "kan een breuk dragen: 'zijn ronde duur gedeeld door 2' is een deling", "mnd", "dagen")),
                faults(source));
    }

    @Test void a_definition_in_two_steps_is_a_refusal() {
        // the variabelendeel is resolved twice (inlined, then as binders): one defect, one refusal
        String source = VERGUNNINGEN + """
            Regel einddag
                geldig altijd
                    De einddag van een Vergunning moet berekend worden als de verlengde dag.
                    Daarbij geldt:
                        de verlengde dag is zijn startdag plus zijn duur.
            """;
        assertEquals(List.of(read("plus", "zijn startdag", "zijn duur", "is een getal met 1 decimalen", "mnd",
                "dagen")), faults(source));
    }

    // ---- every position: a value, a condition, the justification and the consequence of a default

    @Test void every_position() {
        List<String> sentence = List.of(read("plus", "zijn startdag", "zijn duur", "is een getal met 1 decimalen",
                "mnd", "dagen"));
        assertEquals(sentence, faults(condition("zijn startdag plus zijn duur")));
        assertEquals(sentence, faults(justification("zijn startdag plus zijn duur")));
        assertEquals(sentence, faults(consequence("zijn startdag plus zijn duur")));
    }

    // ---- found in the form space: the rounding written behind the date sum, the parentheses left out

    @Test void a_rounding_over_a_date_is_refused() {
        // before: check exit 0 and a Rounding over a Date on the wire, verifier PASS; ref faulted ("afronding: expected
        // a number"), the ASP backend answered the unrounded date and the Drools exec branch crashed at fire
        String withoutParentheses = "zijn startdag plus zijn ronde duur richting nul afgerond op 0 decimalen";
        assertEquals(List.of("afronding '" + withoutParentheses + "': 'zijn startdag plus zijn ronde duur' is een datum, en "
                        + "een afronding rondt een getal af; rond de duur af: 'zijn startdag plus (zijn ronde duur "
                        + "richting nul afgerond op 0 decimalen)'"),
                faults(einddag(withoutParentheses)));
        // with a fraction in the duration, the sum inside is refused first, in its own words
        String broken = "zijn startdag plus zijn duur richting nul afgerond op 0 decimalen";
        assertEquals(List.of(read("plus", "zijn startdag", "zijn duur", "is een getal met 1 decimalen", "mnd",
                        "dagen"),
                        "afronding '" + broken + "': 'zijn startdag plus zijn duur' is een datum, en een afronding "
                        + "rondt een getal af; rond de duur af: 'zijn startdag plus (zijn duur richting nul afgerond "
                        + "op 0 decimalen)'"),
                faults(einddag(broken)));
        assertEquals(List.of("afronding 'zijn begintijd naar boven afgerond op 0 decimalen': 'zijn begintijd' is een "
                        + "datum-tijd, en een afronding rondt een getal af"),
                faults(eindtijd("zijn begintijd naar boven afgerond op 0 decimalen")));
    }

    // ---- the forms that keep playing

    @Test void a_whole_duration_stays_playing() {
        for (String value : List.of("zijn startdag plus zijn ronde duur", "zijn startdag plus 20 mnd",
                "zijn startdag plus 20,0 mnd", "zijn startdag min zijn ronde duur",
                "zijn startdag plus zijn ronde duur maal 2", "zijn startdag plus (zijn ronde duur plus 1 jr)",
                "zijn startdag plus (1 jr plus 12 mnd)", "zijn startdag plus (2 wk plus 7 dg)",
                "zijn startdag plus 1 jr plus 6 mnd",
                "zijn startdag plus de tijdsduur van zijn startdag tot zijn peildag in hele maanden"))
            assertEquals(List.of(), faults(einddag(value)), value);
        assertEquals(List.of(), faults(eindtijd("zijn begintijd plus 26 uur")));
    }

    @Test void a_duration_rounded_to_zero_decimals_stays_playing() {
        // R6: the author's detour and the truncation written out
        for (String value : List.of(
                "zijn startdag plus (zijn duur naar beneden afgerond op 0 decimalen) plus 15 dg",
                "zijn startdag plus (zijn duur richting nul afgerond op 0 decimalen)",
                "zijn startdag plus (zijn vrije duur rekenkundig afgerond op 0 decimalen)"))
            assertEquals(List.of(), faults(einddag(value)), value);
    }

    @Test void the_proposed_forms_play() {
        for (String value : List.of(
                "zijn startdag plus (zijn duur richting nul afgerond op 0 decimalen) plus 15 dagen",
                "zijn startdag plus 20 mnd plus 15 dagen", "zijn startdag plus 1 jr plus 6 maanden",
                "zijn startdag plus (zijn ronde duur gedeeld door 2 richting nul afgerond op 0 decimalen)",
                "zijn startdag min (zijn duur richting nul afgerond op 0 decimalen) min 15 dagen"))
            assertEquals(List.of(), faults(einddag(value)), value);
        assertEquals(List.of(), faults(eindtijd(
                "zijn begintijd plus (zijn uurduur richting nul afgerond op 0 decimalen) plus 30 minuten")));
    }
}
