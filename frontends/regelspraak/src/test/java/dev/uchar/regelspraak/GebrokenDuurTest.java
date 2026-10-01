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
class GebrokenDuurTest {

    private static List<String> fouten(String bron) {
        RegelSpraakModel m = Resolver.resolve(bron);
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

    private static String einddag(String waarde) {
        return VERGUNNINGEN + "Regel einddag\n    geldig altijd\n        De einddag van een Vergunning moet berekend "
                + "worden als " + waarde + ".\n";
    }

    private static String eindtijd(String waarde) {
        return VERGUNNINGEN + "Regel eindtijd\n    geldig altijd\n        De eindtijd van een Vergunning moet berekend "
                + "worden als " + waarde + ".\n";
    }

    private static String voorwaarde(String waarde) {
        return VERGUNNINGEN + "Regel status\n    geldig altijd\n        De status van een Vergunning moet gesteld "
                + "worden op 1\n        indien zijn peildag later is dan " + waarde + ".\n";
    }

    private static String rechtvaardiging(String waarde) {
        return VERGUNNINGEN + """
            Regel status
                geldig altijd
                    De status van een Vergunning moet gesteld worden op 0.

            Uitzondering op status: verlopen
                geldig altijd
                    De status van een Vergunning moet gesteld worden op 1
            """ + "        indien zijn peildag later is dan " + waarde + ".\n";
    }

    private static String consequentie(String waarde) {
        return VERGUNNINGEN + """
            Regel einddag
                geldig altijd
                    De einddag van een Vergunning moet gesteld worden op zijn startdag.

            Uitzondering op einddag: verlengd
                geldig altijd
            """ + "        De einddag van een Vergunning moet berekend worden als " + waarde + "\n"
                + "        indien zijn ronde duur groter is dan 0 mnd.\n";
    }

    /** The refusal: {@code voorbeeld} is ch. 3.8's example in the duration's unit, or the literal truncated;
     *  {@code staart} the smaller unit to step the rest in, or why the date has none. */
    private static String zin(String op, String geschreven, String duur, String oorzaak, String eenheid,
                              String voorbeeld, String afkap, String staart) {
        return "datum " + op + " duur '" + geschreven + "': de duur '" + duur + "' " + oorzaak
                + ", en de uitvoerders kappen een gebroken telling in " + eenheid + " naar nul af vóór de kalenderstap "
                + "(RUNTIME-SEMANTICS ch. 3.8: " + voorbeeld + "); schrijf de afkapping uit als '" + afkap + "'" + staart;
    }

    private static String negentiende(String eenheid) {
        return "1,9 " + eenheid + " telt als 1 " + eenheid + " en -1,9 " + eenheid + " als -1 " + eenheid;
    }

    private static String meeNemen(String afkap, String op, String kleiner) {
        return ", of neem het deel in een kleinere eenheid mee ('" + afkap + " " + op + " … " + kleiner + "')";
    }

    /** The refusal of a duration that is no literal, with the next smaller unit or the closing sentence. */
    private static String gelezen(String op, String links, String duur, String oorzaak, String eenheid, String kleiner) {
        String afkap = links + " " + op + " (" + duur + " richting nul afgerond op 0 decimalen)";
        String staart = kleiner.startsWith("; ") ? kleiner : meeNemen(afkap, op, kleiner);
        return zin(op, links + " " + op + " " + duur, duur, oorzaak, eenheid, negentiende(eenheid), afkap, staart);
    }

    private static final String GEEN_DAGDEEL = "; een datum in dagen kent geen eenheid kleiner dan een dag";

    // ---- the card's reproductions

    @Test void r1_een_attribuut_met_een_decimaal_in_maanden() {
        // before: check exit 0, no diagnostic; 01-03-2026 plus 20,5 mnd gave 01-11-2027 in every executor
        assertEquals(List.of("datum plus duur 'zijn startdag plus zijn duur': de duur 'zijn duur' is een getal met 1 "
                        + "decimalen, en de uitvoerders kappen een gebroken telling in mnd naar nul af vóór de "
                        + "kalenderstap (RUNTIME-SEMANTICS ch. 3.8: 1,9 mnd telt als 1 mnd en -1,9 mnd als -1 mnd); "
                        + "schrijf de afkapping uit als 'zijn startdag plus (zijn duur richting nul afgerond op 0 "
                        + "decimalen)', of neem het deel in een kleinere eenheid mee ('zijn startdag plus (zijn duur "
                        + "richting nul afgerond op 0 decimalen) plus … dagen')"),
                fouten(einddag("zijn startdag plus zijn duur")));
    }

    @Test void r2_een_literal_met_een_breuk_in_maanden() {
        // before: identical to R1, the literal on the wire as {"number": "20.5"}
        assertEquals(List.of("datum plus duur 'zijn startdag plus 20,5 mnd': de duur '20,5 mnd' heeft een breuk, en de "
                        + "uitvoerders kappen een gebroken telling in mnd naar nul af vóór de kalenderstap "
                        + "(RUNTIME-SEMANTICS ch. 3.8: 20,5 mnd telt als 20 mnd); schrijf de afkapping uit als 'zijn "
                        + "startdag plus 20 mnd', of neem het deel in een kleinere eenheid mee ('zijn startdag plus 20 "
                        + "mnd plus … dagen')"),
                fouten(einddag("zijn startdag plus 20,5 mnd")));
    }

    @Test void r3_een_literal_met_een_breuk_in_jaren_en_in_weken() {
        // before: 1,5 jr stepped one year (01-03-2027), 2,5 wk two weeks (15-03-2026)
        assertEquals(List.of("datum plus duur 'zijn startdag plus 1,5 jr': de duur '1,5 jr' heeft een breuk, en de "
                        + "uitvoerders kappen een gebroken telling in jr naar nul af vóór de kalenderstap "
                        + "(RUNTIME-SEMANTICS ch. 3.8: 1,5 jr telt als 1 jr); schrijf de afkapping uit als 'zijn "
                        + "startdag plus 1 jr', of neem het deel in een kleinere eenheid mee ('zijn startdag plus 1 jr "
                        + "plus … maanden')"),
                fouten(einddag("zijn startdag plus 1,5 jr")));
        assertEquals(List.of("datum plus duur 'zijn startdag plus 2,5 wk': de duur '2,5 wk' heeft een breuk, en de "
                        + "uitvoerders kappen een gebroken telling in wk naar nul af vóór de kalenderstap "
                        + "(RUNTIME-SEMANTICS ch. 3.8: 2,5 wk telt als 2 wk); schrijf de afkapping uit als 'zijn "
                        + "startdag plus 2 wk', of neem het deel in een kleinere eenheid mee ('zijn startdag plus 2 wk "
                        + "plus … dagen')"),
                fouten(einddag("zijn startdag plus 2,5 wk")));
    }

    // ---- every unit on its date: dg, wk, mnd, kw, jr on a Datum in dagen; u, minuut, sec, ms on a datum-tijd

    @Test void elke_eenheid_op_haar_datum() {
        String g = "is een getal met 1 decimalen";
        assertEquals(List.of(gelezen("plus", "zijn startdag", "zijn dagduur", g, "dg", GEEN_DAGDEEL)),
                fouten(einddag("zijn startdag plus zijn dagduur")));
        assertEquals(List.of(gelezen("plus", "zijn startdag", "zijn weekduur", g, "wk", "dagen")),
                fouten(einddag("zijn startdag plus zijn weekduur")));
        assertEquals(List.of(gelezen("plus", "zijn startdag", "zijn kwartaalduur", g, "kw", "maanden")),
                fouten(einddag("zijn startdag plus zijn kwartaalduur")));
        assertEquals(List.of(gelezen("plus", "zijn startdag", "zijn jaarduur", g, "jr", "maanden")),
                fouten(einddag("zijn startdag plus zijn jaarduur")));
        assertEquals(List.of(gelezen("plus", "zijn begintijd", "zijn dagduur", g, "dg", "uur")),
                fouten(eindtijd("zijn begintijd plus zijn dagduur")));
        assertEquals(List.of(gelezen("plus", "zijn begintijd", "zijn uurduur", g, "u", "minuten")),
                fouten(eindtijd("zijn begintijd plus zijn uurduur")));
        assertEquals(List.of(gelezen("plus", "zijn begintijd", "zijn minuutduur", g, "minuut", "seconden")),
                fouten(eindtijd("zijn begintijd plus zijn minuutduur")));
        assertEquals(List.of(gelezen("plus", "zijn begintijd", "zijn secondeduur", g, "sec", "milliseconden")),
                fouten(eindtijd("zijn begintijd plus zijn secondeduur")));
        assertEquals(List.of(gelezen("plus", "zijn begintijd", "zijn msduur", g, "ms",
                        "; een milliseconde is de kleinste eenheid")),
                fouten(eindtijd("zijn begintijd plus zijn msduur")));
        assertEquals(List.of(zin("plus", "zijn startdag plus 2,5 dg", "2,5 dg", "heeft een breuk", "dg",
                        "2,5 dg telt als 2 dg", "zijn startdag plus 2 dg", GEEN_DAGDEEL)),
                fouten(einddag("zijn startdag plus 2,5 dg")));
        assertEquals(List.of(zin("plus", "zijn begintijd plus 1,5 u", "1,5 u", "heeft een breuk", "u",
                        "1,5 u telt als 1 u", "zijn begintijd plus 1 u",
                        meeNemen("zijn begintijd plus 1 u", "plus", "minuten"))),
                fouten(eindtijd("zijn begintijd plus 1,5 u")));
    }

    @Test void een_dagsoort_als_eenheid() {
        // ch. 3.8: a fractional count truncates toward zero before the walk as well
        assertEquals(List.of(gelezen("plus", "zijn startdag", "zijn getijbestek", "is een getal met 1 decimalen",
                        "springtijdagen", "; een dagsoort kent geen kleinere eenheid")),
                fouten(einddag("zijn startdag plus zijn getijbestek")));
    }

    @Test void plus_en_min() {
        assertEquals(List.of(gelezen("min", "zijn startdag", "zijn duur", "is een getal met 1 decimalen", "mnd",
                        "dagen")),
                fouten(einddag("zijn startdag min zijn duur")));
        assertEquals(List.of(zin("min", "zijn startdag min 20,5 mnd", "20,5 mnd", "heeft een breuk", "mnd",
                        "20,5 mnd telt als 20 mnd", "zijn startdag min 20 mnd",
                        meeNemen("zijn startdag min 20 mnd", "min", "dagen"))),
                fouten(einddag("zijn startdag min 20,5 mnd")));
        assertEquals(List.of(zin("plus", "zijn startdag plus -20,5 mnd", "-20,5 mnd", "heeft een breuk", "mnd",
                        "-20,5 mnd telt als -20 mnd", "zijn startdag plus -20 mnd",
                        meeNemen("zijn startdag plus -20 mnd", "plus", "dagen"))),
                fouten(einddag("zijn startdag plus -20,5 mnd")));
    }

    // ---- every source of a fraction: a parameter, an unbounded number, a computation without a whole outcome

    @Test void elke_bron_van_een_breuk() {
        assertEquals(List.of(gelezen("plus", "zijn startdag", "de standaardduur", "is een getal met 1 decimalen",
                        "mnd", "dagen")),
                fouten(einddag("zijn startdag plus de standaardduur")));
        assertEquals(List.of(gelezen("plus", "zijn startdag", "zijn vrije duur",
                        "is een getal zonder vast aantal decimalen", "mnd", "dagen")),
                fouten(einddag("zijn startdag plus zijn vrije duur")));
        String deling = "zijn ronde duur gedeeld door 2";
        assertEquals(List.of(zin("plus", "zijn startdag plus (" + deling + ")", deling, "is een deling", "mnd",
                        negentiende("mnd"), "zijn startdag plus (" + deling + " richting nul afgerond op 0 decimalen)",
                        meeNemen("zijn startdag plus (" + deling + " richting nul afgerond op 0 decimalen)", "plus",
                                "dagen"))),
                fouten(einddag("zijn startdag plus (" + deling + ")")));
        String pct = "50% van zijn ronde duur";
        assertEquals(List.of(zin("plus", "zijn startdag plus (" + pct + ")", pct, "is een percentage", "mnd",
                        negentiende("mnd"), "zijn startdag plus (" + pct + " richting nul afgerond op 0 decimalen)",
                        meeNemen("zijn startdag plus (" + pct + " richting nul afgerond op 0 decimalen)", "plus",
                                "dagen"))),
                fouten(einddag("zijn startdag plus (" + pct + ")")));
        String afgerond = "zijn vrije duur rekenkundig afgerond op 1 decimalen";
        assertEquals(List.of(zin("plus", "zijn startdag plus (" + afgerond + ")", afgerond,
                        "is afgerond op 1 decimalen", "mnd", negentiende("mnd"),
                        "zijn startdag plus (" + afgerond + " richting nul afgerond op 0 decimalen)",
                        meeNemen("zijn startdag plus (" + afgerond + " richting nul afgerond op 0 decimalen)", "plus",
                                "dagen"))),
                fouten(einddag("zijn startdag plus (" + afgerond + ")")));
        assertEquals(List.of(gelezen("plus", "zijn startdag", "zijn ronde duur maal 1,5",
                        "kan een breuk dragen: '1,5' heeft een breuk", "mnd", "dagen")),
                fouten(einddag("zijn startdag plus zijn ronde duur maal 1,5")));
    }

    @Test void een_omrekening_naar_een_grotere_eenheid() {
        // "1 jr plus 6 mnd" is 1,5 jr: the sum is typed whole, the restatement into jr makes it fractional
        String som = "1 jr plus 6 mnd";
        assertEquals(List.of(zin("plus", "zijn startdag plus (" + som + ")", som,
                        "kan een breuk dragen: '6 mnd' wordt naar jr omgerekend (een mnd is een breuk van een jr)",
                        "jr", negentiende("jr"), "zijn startdag plus (" + som + " richting nul afgerond op 0 decimalen)",
                        meeNemen("zijn startdag plus (" + som + " richting nul afgerond op 0 decimalen)", "plus",
                                "maanden"))),
                fouten(einddag("zijn startdag plus (" + som + ")")));
        String weken = "zijn weekduur rekenkundig afgerond op 0 decimalen plus 3 dg";
        assertEquals(List.of(zin("plus", "zijn startdag plus (" + weken + ")", weken,
                        "kan een breuk dragen: '3 dg' wordt naar wk omgerekend (een dg is een breuk van een wk)",
                        "wk", negentiende("wk"), "zijn startdag plus (" + weken + " richting nul afgerond op 0 decimalen)",
                        meeNemen("zijn startdag plus (" + weken + " richting nul afgerond op 0 decimalen)", "plus",
                                "dagen"))),
                fouten(einddag("zijn startdag plus (" + weken + ")")));
    }

    @Test void een_variabele_draagt_de_breuk_van_haar_definitie() {
        String bron = VERGUNNINGEN + """
            Regel einddag
                geldig altijd
                    De einddag van een Vergunning moet berekend worden als zijn startdag plus de halve duur.
                    Daarbij geldt:
                        de halve duur is zijn ronde duur gedeeld door 2.
            """;
        assertEquals(List.of(gelezen("plus", "zijn startdag", "de halve duur",
                        "kan een breuk dragen: 'zijn ronde duur gedeeld door 2' is een deling", "mnd", "dagen")),
                fouten(bron));
    }

    @Test void een_definitie_in_twee_passen_is_een_weigering() {
        // the variabelendeel is resolved twice (inlined, then as binders): one defect, one refusal
        String bron = VERGUNNINGEN + """
            Regel einddag
                geldig altijd
                    De einddag van een Vergunning moet berekend worden als de verlengde dag.
                    Daarbij geldt:
                        de verlengde dag is zijn startdag plus zijn duur.
            """;
        assertEquals(List.of(gelezen("plus", "zijn startdag", "zijn duur", "is een getal met 1 decimalen", "mnd",
                "dagen")), fouten(bron));
    }

    // ---- every position: a value, a condition, the justification and the consequence of a default

    @Test void elke_positie() {
        List<String> zin = List.of(gelezen("plus", "zijn startdag", "zijn duur", "is een getal met 1 decimalen",
                "mnd", "dagen"));
        assertEquals(zin, fouten(voorwaarde("zijn startdag plus zijn duur")));
        assertEquals(zin, fouten(rechtvaardiging("zijn startdag plus zijn duur")));
        assertEquals(zin, fouten(consequentie("zijn startdag plus zijn duur")));
    }

    // ---- found in the form space: the rounding written behind the date sum, the parentheses left out

    @Test void een_afronding_over_een_datum_weigert() {
        // before: check exit 0 and a Rounding over a Date on the wire, verifier PASS; ref faulted ("afronding: expected
        // a number"), the ASP backend answered the unrounded date and the Drools exec branch crashed at fire
        String zonderHaakjes = "zijn startdag plus zijn ronde duur richting nul afgerond op 0 decimalen";
        assertEquals(List.of("afronding '" + zonderHaakjes + "': 'zijn startdag plus zijn ronde duur' is een datum, en "
                        + "een afronding rondt een getal af; rond de duur af: 'zijn startdag plus (zijn ronde duur "
                        + "richting nul afgerond op 0 decimalen)'"),
                fouten(einddag(zonderHaakjes)));
        // with a fraction in the duration, the sum inside is refused first, in its own words
        String gebroken = "zijn startdag plus zijn duur richting nul afgerond op 0 decimalen";
        assertEquals(List.of(gelezen("plus", "zijn startdag", "zijn duur", "is een getal met 1 decimalen", "mnd",
                        "dagen"),
                        "afronding '" + gebroken + "': 'zijn startdag plus zijn duur' is een datum, en een afronding "
                        + "rondt een getal af; rond de duur af: 'zijn startdag plus (zijn duur richting nul afgerond "
                        + "op 0 decimalen)'"),
                fouten(einddag(gebroken)));
        assertEquals(List.of("afronding 'zijn begintijd naar boven afgerond op 0 decimalen': 'zijn begintijd' is een "
                        + "datum-tijd, en een afronding rondt een getal af"),
                fouten(eindtijd("zijn begintijd naar boven afgerond op 0 decimalen")));
    }

    // ---- the forms that keep playing

    @Test void een_hele_duur_blijft_spelen() {
        for (String waarde : List.of("zijn startdag plus zijn ronde duur", "zijn startdag plus 20 mnd",
                "zijn startdag plus 20,0 mnd", "zijn startdag min zijn ronde duur",
                "zijn startdag plus zijn ronde duur maal 2", "zijn startdag plus (zijn ronde duur plus 1 jr)",
                "zijn startdag plus (1 jr plus 12 mnd)", "zijn startdag plus (2 wk plus 7 dg)",
                "zijn startdag plus 1 jr plus 6 mnd",
                "zijn startdag plus de tijdsduur van zijn startdag tot zijn peildag in hele maanden"))
            assertEquals(List.of(), fouten(einddag(waarde)), waarde);
        assertEquals(List.of(), fouten(eindtijd("zijn begintijd plus 26 uur")));
    }

    @Test void een_op_nul_decimalen_afgeronde_duur_blijft_spelen() {
        // R6: the author's detour and the truncation written out
        for (String waarde : List.of(
                "zijn startdag plus (zijn duur naar beneden afgerond op 0 decimalen) plus 15 dg",
                "zijn startdag plus (zijn duur richting nul afgerond op 0 decimalen)",
                "zijn startdag plus (zijn vrije duur rekenkundig afgerond op 0 decimalen)"))
            assertEquals(List.of(), fouten(einddag(waarde)), waarde);
    }

    @Test void de_voorgestelde_vormen_spelen() {
        for (String waarde : List.of(
                "zijn startdag plus (zijn duur richting nul afgerond op 0 decimalen) plus 15 dagen",
                "zijn startdag plus 20 mnd plus 15 dagen", "zijn startdag plus 1 jr plus 6 maanden",
                "zijn startdag plus (zijn ronde duur gedeeld door 2 richting nul afgerond op 0 decimalen)",
                "zijn startdag min (zijn duur richting nul afgerond op 0 decimalen) min 15 dagen"))
            assertEquals(List.of(), fouten(einddag(waarde)), waarde);
        assertEquals(List.of(), fouten(eindtijd(
                "zijn begintijd plus (zijn uurduur richting nul afgerond op 0 decimalen) plus 30 minuten")));
    }
}
