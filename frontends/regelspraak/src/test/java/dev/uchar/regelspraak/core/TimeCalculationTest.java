package dev.uchar.regelspraak.core;

import dev.uchar.regelspraak.ir.Condition.Operator;
import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.model.Datatype;
import dev.uchar.regelspraak.model.Unit;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Granulariteit;
import dev.uchar.regelspraak.model.RegelSpraakModel.Objecttype;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The shared tijdseenheid → ChronoUnit mapping and §8.2 toets codes (single shared implementation). */
class TimeCalculationTest {

    private static Datatype with(String afkorting) {
        return new Datatype.Numeriek(true, 0, null, new Unit(afkorting, "Tijd"));
    }

    @Test void chronoUnit_normalises_the_time_units() {
        assertEquals("YEARS",  TimeCalculation.chronoUnit(with("jr")));
        assertEquals("YEARS",  TimeCalculation.chronoUnit(with("jaren")));   // plural -> singular reducer
        assertEquals("MONTHS", TimeCalculation.chronoUnit(with("mnd")));
        assertEquals("WEEKS",  TimeCalculation.chronoUnit(with("wk")));
        assertEquals("DAYS",   TimeCalculation.chronoUnit(with("dag")));
        assertEquals("DAYS",   TimeCalculation.chronoUnit(with("dg")));   // §3.7 the declared dag afkorting
        assertEquals("HOURS",  TimeCalculation.chronoUnit(with("u")));
        assertEquals("MILLIS", TimeCalculation.chronoUnit(with("ms")));
        // §3.7 kwartaal = 3 maanden; java.time has no QUARTERS -> the runtime's pseudo-unit
        assertEquals("KWARTAAL", TimeCalculation.chronoUnit(with("kwartaal")));
        assertEquals("KWARTAAL", TimeCalculation.chronoUnit(with("kw"))); // §3.7 the declared kwartaal afkorting
        assertNull(TimeCalculation.chronoUnit(with("werkdagen")), "een dagsoort/onbekende eenheid -> null (marker)");
        assertNull(TimeCalculation.chronoUnit(new Datatype.Numeriek(true, 0, null, null)), "geen eenheid -> null");
    }

    @Test void isCalendarSpelling_knows_exactly_the_thirty_spellings_of_the_table() {
        // The day-kind registration guard (DefinitionResolver) and WF-017 both compare against THIS, so the
        // set has to be the normative table of RUNTIME-SEMANTICS ch. 3.8 exactly: nine units, thirty
        // spellings — the singular, the abbreviation and the plural for each, plus THREE extras that make
        // the count come out at thirty rather than twenty-seven: the second carries a second abbreviation
        // (`s` beside `sec`), and the second and the millisecond each carry a second plural in -s.
        // Pinned as a whole rather than sampled, because both a missing and a surplus spelling is a
        // producer that disagrees with the wire rule.
        List<String> table = List.of(
                "jaar", "jr", "jaren", "maand", "mnd", "maanden", "kwartaal", "kw", "kwartalen",
                "week", "wk", "weken", "dag", "dg", "dagen", "uur", "u", "uren",
                "minuut", "min", "minuten", "seconde", "sec", "s", "seconden", "secondes",
                "milliseconde", "ms", "milliseconden", "millisecondes");
        assertEquals(30, table.size());
        for (String s : table) assertTrue(TimeCalculation.isCalendarSpelling(s), s + " staat in de tabel");
        // and nothing beside it: a day-kind spelling, a near miss, the empty spelling, none
        for (String s : List.of("werkdag", "werkdagen", "schouwdagen", "wkn", "seconds", "kwartaaltje", ""))
            assertFalse(TimeCalculation.isCalendarSpelling(s), s + " staat er niet in");
        assertFalse(TimeCalculation.isCalendarSpelling(null));
    }

    @Test void the_table_is_read_capital_insensitive_and_trimmed() {
        // ch. 3.8 says "recognised by SPELLING, case-insensitive", and the interpreter reads it that way
        // (lower-case after a trim). Until 2026-09-20 only the nine -en plurals survived a capital here:
        // they are the ones the plural reducer REPLACES, and the replacement happened to be lowercase,
        // while every other spelling fell through the reducer's default arm in its original case. Measured
        // then: WEKEN -> WEEKS but WK, Wk, ' wk ' and Sec -> null, and a verifier-clean document with the
        // unit spelled WK split the executors — ref reckoned in weeks, both Drools branches refused the
        // rule as an unknown time unit.
        for (String s : List.of("WK", "Wk", " wk ", "\tSec\n", "S", "JR", "Secondes", "MILLISECONDES"))
            assertTrue(TimeCalculation.isCalendarSpelling(s), "'" + s + "' is dezelfde spelling als zijn kleine letter");
        assertEquals("WEEKS", TimeCalculation.chronoUnit(with(" WK ")));
        assertEquals("SECONDS", TimeCalculation.chronoUnit(with("Sec")));
        assertEquals("KWARTAAL", TimeCalculation.chronoUnit(with("KWARTALEN")));
    }

    @Test void a_unit_without_abbreviation_is_no_time_unit_and_no_day_kind() {
        // The wire admits an Eenheid whose afkorting is null (the interpreter reads it as "no unit"), and
        // the plural reducer hands a null straight back through its default arm — so both readers below
        // would throw on a document the IR admits. They answer null instead, which is the same verdict
        // ref gives.
        Unit without = new Unit(null, "Tijd");
        assertNull(TimeCalculation.chronoUnit(without));
        assertNull(TimeCalculation.chronoUnit(new Datatype.Numeriek(true, 0, null, without)));
        RegelSpraakModel model = new RegelSpraakModel();
        model.dayKindPerUnit.put("werkdagen", "werkdag");
        assertNull(TimeCalculation.dayKindForUnit(model, without));
        assertNull(TimeCalculation.dayKindForUnit(model, null));
        // and the reader still finds a real day kind beside it
        assertEquals("werkdag", TimeCalculation.dayKindForUnit(model, new Unit("werkdagen", null)));
    }

    @Test void test_encodes_the_six_comparisons_and_refuses_date_ordering() {
        assertEquals(0, TimeCalculation.test(Operator.KLEINER));
        assertEquals(1, TimeCalculation.test(Operator.KLEINER_OF_GELIJK));
        assertEquals(2, TimeCalculation.test(Operator.GROTER));
        assertEquals(3, TimeCalculation.test(Operator.GROTER_OF_GELIJK));
        assertEquals(4, TimeCalculation.test(Operator.GELIJK));
        assertEquals(5, TimeCalculation.test(Operator.ONGELIJK));
        assertNull(TimeCalculation.test(Operator.EERDER), "een datum-ordening valt buiten rsGeheelPeriode");
        assertNull(TimeCalculation.test(Operator.LATER_OF_GELIJK));
    }

    @Test void periodBoundaryLowerable_reads_the_facets_of_an_inherited_attribute_through_the_chain() {
        // Both Drools branches share this helper, so a wrong answer here is a wrong answer in two
        // executors at once. It must judge the FACETS of the bound attribute — a timeline or a
        // dimensioned attribute is not a lowerable §8.4.3 bound — and those facets sit at the
        // DECLARANT. Reading them on the subject's own maps calls an inherited timeline scalar and
        // admits a bound the runtime cannot compute.
        Objecttype parent = new Objecttype("Jutter");
        parent.attributen.put("peildatum", new Datatype.DatumInDagen());
        parent.attributen.put("knipdatum", new Datatype.DatumInDagen());
        parent.attribuutGranulariteit.put("knipdatum", Granulariteit.MAAND);
        parent.attributen.put("seizoensdatum", new Datatype.DatumInDagen());
        parent.attributeDimensions.put("seizoensdatum", List.of("seizoen"));
        Objecttype sub = new Objecttype("Klipjutter");
        sub.supertype = parent;

        assertTrue(TimeCalculation.periodBoundLowerable(limit("peildatum", sub), sub),
                "een geërfde scalaire datum is een gewone lowerbare grens");
        assertFalse(TimeCalculation.periodBoundLowerable(limit("knipdatum", sub), sub),
                "een geërfd TIJDLIJN-attribuut is geen scalaire grens — het antwoord mag niet van de "
                        + "declaratieplaats afhangen");
        assertFalse(TimeCalculation.periodBoundLowerable(limit("seizoensdatum", sub), sub),
                "een geërfd GEDIMENSIONEERD attribuut evenmin");
        // …en het vlakke equivalent geeft precies dezelfde drie antwoorden
        assertTrue(TimeCalculation.periodBoundLowerable(limit("peildatum", parent), parent));
        assertFalse(TimeCalculation.periodBoundLowerable(limit("knipdatum", parent), parent));
        assertFalse(TimeCalculation.periodBoundLowerable(limit("seizoensdatum", parent), parent));
    }

    private static Expr limit(String attribute, Objecttype subject) {
        return new Expr.AttributeReference(attribute, subject, "zijn", null, new Datatype.DatumInDagen(), null);
    }
}
