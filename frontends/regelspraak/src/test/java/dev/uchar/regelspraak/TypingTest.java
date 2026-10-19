package dev.uchar.regelspraak;

import dev.uchar.regelspraak.model.Datatype;
import dev.uchar.regelspraak.model.Datatype.Sign;
import dev.uchar.regelspraak.model.Unit;
import dev.uchar.regelspraak.resolver.Typing;
import dev.uchar.regelspraak.resolver.Typing.BinOp;
import dev.uchar.regelspraak.resolver.Typing.CmpOp;
import dev.uchar.regelspraak.resolver.Typing.UnaryPred;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The typeringen matrix (typeringen §4 operators, §5 comparisons, §6 toekenning). */
class TypingTest {
    static Datatype num()                    { return new Datatype.Numeric(true, 0, Sign.ONBEPAALD, null); }
    static Datatype num(String abbr, String system) { return new Datatype.Numeric(false, 0, Sign.ONBEPAALD, new Unit(abbr, system)); }
    static Datatype text()                  { return new Datatype.Text(); }
    static Datatype bool()                   { return new Datatype.BooleanType(); }
    static Datatype date()                  { return new Datatype.DateInDays(); }

    // §4.1 plus
    @Test void plus_two_numbers_ok() {
        var r = Typing.compute(BinOp.PLUS, num(), num());
        assertTrue(r.ok()); assertTrue(r.type().isNumber());
    }
    @Test void plus_number_and_text_error() {
        assertFalse(Typing.compute(BinOp.PLUS, num(), text()).ok());
    }
    @Test void plus_date_and_time_unit_gives_date() {    // §4.1 Tabel 2 scenario 2
        var r = Typing.compute(BinOp.PLUS, date(), num("dg", "Tijd"));
        assertTrue(r.ok()); assertTrue(r.type().isDate());
    }
    @Test void plus_date_and_unitless_number_error() {         // scenario 2 requires a (time) unit
        assertFalse(Typing.compute(BinOp.PLUS, date(), num()).ok());
    }
    @Test void plus_date_and_non_time_unit_error() {      // scenario 2: the unit must be a TIJDseenheid
        var r = Typing.compute(BinOp.PLUS, date(), num("km", "afstand"));
        assertFalse(r.ok());
        // the refusal speaks the verifier's WF-035 sentence since the card frontend-emitterrestanten
        assertTrue(r.error().contains("is neither a recognised time unit nor a declared day kind"), r::error);
        assertFalse(Typing.compute(BinOp.MIN, date(), num("km", "afstand")).ok());
    }
    @Test void plus_date_and_day_kind_unit_ok() {         // §6.11: a declared dagsoort is a valid duration unit
        var r = Typing.compute(BinOp.PLUS, date(), num("kerstdagen", null), true);
        assertTrue(r.ok()); assertTrue(r.type().isDate());
    }

    static Datatype dateTime() { return new Datatype.DateTime(); }

    // Datum-vs-DateTime precisie in gelijk/ongelijk/toekenning — a chosen strictness (see
    // grammar-divergences): a mixed toekenning is a type-broken setter, a mixed gelijk never true.
    @Test void equal_mixed_time_precision_error() {
        var r = Typing.compare(CmpOp.GELIJK, date(), dateTime());
        // the refusal speaks the verifier's WF-036 sentence since the card frontend-emitterrestanten
        assertFalse(r.ok()); assertTrue(r.error().contains("equality across date precisions"), r::error);
        assertFalse(Typing.compare(CmpOp.ONGELIJK, dateTime(), date()).ok());
        assertTrue(Typing.compare(CmpOp.GELIJK, date(), date()).ok());
        assertTrue(Typing.compare(CmpOp.GELIJK, dateTime(), dateTime()).ok());
        // the ordering comparisons stay precision-free (Tabellen 45-52 state no restriction)
        assertTrue(Typing.compare(CmpOp.EERDER, date(), dateTime()).ok());
    }
    @Test void assignment_mixed_time_precision_error() {
        assertNotNull(Typing.assignment(date(), dateTime()));
        assertNotNull(Typing.assignment(dateTime(), date()));
        assertNull(Typing.assignment(date(), date()));
        assertNull(Typing.assignment(dateTime(), dateTime()));
    }

    // §4.1/§4.2/§4.3: geheel ± geheel keeps geheel-ness (the sum of integers is an integer), so a
    // geheel-demanding gate (§5.13 elfproef) admits whole-number arithmetic; a decimal or unbounded
    // "getal" operand still drops it.
    @Test void plus_whole_and_whole_stays_whole() {
        var r = Typing.compute(BinOp.PLUS, num(), num());
        assertTrue(r.ok());
        assertTrue(r.type() instanceof Datatype.Numeric n && n.wholeNumber(),
                () -> "geheel + geheel moet geheel blijven, kreeg " + r.type());
        assertTrue(Typing.predicate(UnaryPred.ELFPROEF, r.type()).ok(),
                "de elfproef moet een gehele som toelaten (Tabel 55)");
        Datatype twoDec = new Datatype.Numeric(false, 2, Sign.ONBEPAALD, null);
        Datatype unbounded = new Datatype.Numeric(false, 0, Sign.ONBEPAALD, null);  // form (c) "getal"
        assertFalse(Typing.compute(BinOp.MIN, num(), twoDec).type() instanceof Datatype.Numeric n2 && n2.wholeNumber());
        assertFalse(Typing.compute(BinOp.PLUS, num(), unbounded).type() instanceof Datatype.Numeric n3 && n3.wholeNumber());
    }
    @Test void power_with_unit_error() {                    // §4.7: no units allowed
        assertFalse(Typing.compute(BinOp.TOT_DE_MACHT, num("km", "afstand"), num()).ok());
    }
    // jr and mnd are convertible because the resolver reduces both to the SAME canonical (mnd) via the
    // declared "jr = 12 mnd"; sharing only a stelsel is not enough (p44), so the fixtures carry that link.
    static Datatype num(String abbr, String system, String canonical, long factor) {
        return new Datatype.Numeric(false, 0, Sign.ONBEPAALD,
                new Unit(abbr, system, canonical, java.math.BigDecimal.valueOf(factor)));
    }
    @Test void plus_convertible_units_ok() {
        assertTrue(Typing.compute(BinOp.PLUS, num("jr", "Tijd", "mnd", 12), num("mnd", "Tijd", "mnd", 1)).ok());
    }
    @Test void plus_non_convertible_units_error() {
        assertFalse(Typing.compute(BinOp.PLUS, num("jr", "Tijd", "mnd", 12), num("km", "afstand", "km", 1)).ok());
    }
    @Test void plus_same_system_other_canonical_error() {
        // p44: two independent base units in ONE system (appel/peer, no declared conversion) reduce to
        // disjoint canonicals, so they are NOT inter-convertible — sharing a stelsel must not make them so.
        assertFalse(Typing.compute(BinOp.PLUS, num("app", "Mix", "app", 1), num("pr", "Mix", "pr", 1)).ok());
        assertFalse(Typing.compare(CmpOp.GELIJK, num("app", "Mix", "app", 1), num("pr", "Mix", "pr", 1)).ok());
    }
    @Test void plus_unit_on_one_side_error() {
        assertFalse(Typing.compute(BinOp.PLUS, num("jr", "Tijd"), num()).ok());
    }

    // §4.4/4.5 maal/gedeeld vereisen numeriek
    @Test void times_numbers_ok()       { assertTrue(Typing.compute(BinOp.MAAL, num(), num()).ok()); }
    @Test void divided_by_text_error(){ assertFalse(Typing.compute(BinOp.GEDEELD_DOOR, num(), text()).ok()); }

    // §5.1/5.5 ordening: alleen getalachtig
    @Test void less_numbers_ok()  { assertTrue(Typing.compare(CmpOp.KLEINER, num(), num()).ok()); }
    @Test void greater_text_error()    { assertFalse(Typing.compare(CmpOp.GROTER, text(), text()).ok()); }
    @Test void less_result_is_boolean() {
        assertTrue(Typing.compare(CmpOp.KLEINER, num(), num()).type().isBoolean());
    }

    // §5.3/5.6 gelijk/ongelijk: zelfde datatype
    @Test void equal_same_type_ok()  { assertTrue(Typing.compare(CmpOp.GELIJK, text(), text()).ok()); }
    @Test void equal_different_type_error() { assertFalse(Typing.compare(CmpOp.GELIJK, text(), num()).ok()); }
    @Test void equal_number_vs_percentage_error() {        // §5.3 Tabel 36: distinct rows
        assertFalse(Typing.compare(CmpOp.GELIJK, num(), new Datatype.Percentage(true, 0)).ok());
    }

    // §5.7-5.10 eerder/later: datum
    @Test void later_dates_ok()    { assertTrue(Typing.compare(CmpOp.LATER, date(), date()).ok()); }
    @Test void later_numbers_error(){ assertFalse(Typing.compare(CmpOp.LATER, num(), num()).ok()); }

    // §5.11-5.16 predicaten
    @Test void empty_on_each_type_ok()      { assertTrue(Typing.predicate(UnaryPred.LEEG, num()).ok()); }
    @Test void eleven_test_text_ok()        { assertTrue(Typing.predicate(UnaryPred.ELFPROEF, text()).ok()); }
    @Test void eleven_test_numeric_whole_ok() { assertTrue(Typing.predicate(UnaryPred.ELFPROEF, num()).ok()); } // §5.13 Tabel 55 sc.1
    @Test void eleven_test_boolean_error()    { assertFalse(Typing.predicate(UnaryPred.ELFPROEF, bool()).ok()); }
    @Test void exact_digits_text_ok()  { assertTrue(Typing.predicate(UnaryPred.NUMERIEK_MET_EXACT, text()).ok()); }
    @Test void exact_digits_number_error(){ assertFalse(Typing.predicate(UnaryPred.NUMERIEK_MET_EXACT, num()).ok()); }
    @Test void day_kind_date_ok()        { assertTrue(Typing.predicate(UnaryPred.DAGSOORT, date()).ok()); }
    @Test void day_kind_text_error()      { assertFalse(Typing.predicate(UnaryPred.DAGSOORT, text()).ok()); }

    // §6.1 toekenning
    @Test void assignment_match_ok()      { assertNull(Typing.assignment(num(), num())); }
    @Test void assignment_mismatch_error() { assertNotNull(Typing.assignment(num(), text())); }
    @Test void assignment_unit_mismatch_error() {
        assertNotNull(Typing.assignment(num("jr", "Tijd"), num("km", "afstand")));
    }

    // §3.3.5 a Percentage always carries the "%" unit, optionally a rate "%/jr"
    static Datatype pct()        { return new Datatype.Percentage(true, 0); }
    static Datatype pctPerYear() { return new Datatype.Percentage(true, 0,
            Unit.compound(Datatype.Percentage.PERCENT, new Unit("jr", "Tijd"))); }

    @Test void percentage_always_carries_the_percent_unit() {
        assertEquals("%", pct().unitOf().abbreviation());
        assertEquals("%/jr", pctPerYear().unitOf().rendering());
    }
    @Test void percentage_less_than_percentage_stays_ok() {
        assertTrue(Typing.compare(CmpOp.KLEINER, pct(), pct()).ok());
    }
    @Test void percentage_equal_to_number_stays_family_error() {
        assertFalse(Typing.compare(CmpOp.GELIJK, pct(), num()).ok());   // distinct families
    }
    @Test void percentage_assignment_with_deviating_rate_is_a_unit_error() {
        assertNotNull(Typing.assignment(pct(), pctPerYear()));   // "%" vs "%/jr": a simple unit vs a rate
    }
    @Test void percentage_plus_unitless_number_is_a_unit_error() {
        // matrix-aligned (Tabel 32/34 list Percentage with Percentage, never with a unitless plain Numeriek):
        // now that a Percentage carries "%", mixing it with a unitless number flags the §4 unit rule.
        assertFalse(Typing.compute(BinOp.PLUS, pct(), num()).ok());
    }

    // §4.4/4.5 composite-unit synthesis (both operands carry a unit)
    static final Unit EUR = new Unit("EUR", "Geld");
    static final Unit JR  = new Unit("jr", "Tijd");
    static Datatype numUnit(Unit e) { return new Datatype.Numeric(false, 0, Sign.ONBEPAALD, e); }

    @Test void times_rate_time_its_denominator_unit_cancels_to_the_numerator() {
        // €/jr maal jr = € (the denominator cancels) — was Onbekend
        var res = Typing.compute(BinOp.MAAL, numUnit(Unit.compound(EUR, JR)), numUnit(JR));
        assertTrue(res.ok());
        assertEquals("EUR", res.type().unitOf().abbreviation());
        assertNull(res.type().unitOf().denominator(), "de noemer moet weggevallen zijn");
    }
    @Test void unit_divided_by_unit_gives_a_rate() {
        // € gedeeld door jr = €/jr — was Onbekend
        var res = Typing.compute(BinOp.GEDEELD_DOOR, numUnit(EUR), numUnit(JR));
        assertTrue(res.ok());
        assertEquals("EUR/jr", res.type().unitOf().rendering());
    }
    @Test void divided_by_equal_unit_gives_a_dimensionless_number() {
        // € gedeeld door € = een getal (dimensionless) — was Onbekend
        var res = Typing.compute(BinOp.GEDEELD_DOOR, numUnit(EUR), numUnit(EUR));
        assertTrue(res.ok());
        assertNull(res.type().unitOf(), "een quotiënt van gelijke eenheden is eenheidloos");
    }
    @Test void times_two_separate_units_gives_an_opaque_product_unit() {
        // §4.4: € maal m = €·m — an opaque PRODUCT unit (no canonical), commutative, not Unknown.
        Unit m = new Unit("m", "Lengte");
        var res = Typing.compute(BinOp.MAAL, numUnit(EUR), numUnit(m));
        assertTrue(res.ok());
        assertEquals("EUR·m", res.type().unitOf().abbreviation(), "het productunit (factoren gesorteerd)");
        var reversed = Typing.compute(BinOp.MAAL, numUnit(m), numUnit(EUR));   // commutatief: m maal € == € maal m
        assertEquals("EUR·m", reversed.type().unitOf().abbreviation());
    }
    @Test void product_unit_is_commutative_and_associative_for_three_factors() {
        // §4.4 Tabel 11: unit multiplication is commutative AND associative, so (€·m)·km and €·(m·km) must denote
        // the ONE same product unit (else a correct 3-way product is spuriously rejected on assignment/comparison).
        Unit m = new Unit("m", "Lengte"), km = new Unit("km", "Lengte");
        var ab  = Typing.compute(BinOp.MAAL, numUnit(EUR), numUnit(m)).type();   // € maal m  = €·m
        var abc = Typing.compute(BinOp.MAAL, ab, numUnit(km)).type();            // (€·m) maal km
        var bc  = Typing.compute(BinOp.MAAL, numUnit(m), numUnit(km)).type();    // m maal km = m·km
        var abc2 = Typing.compute(BinOp.MAAL, numUnit(EUR), bc).type();          // € maal (m·km)
        assertEquals(abc.unitOf().abbreviation(), abc2.unitOf().abbreviation(), "associatief: één canoniek productunit");
        assertEquals("EUR·km·m", abc.unitOf().abbreviation(), "gesorteerde factor-multiset");
        assertNull(Typing.assignment(abc, abc2), "dezelfde 3-weg producteenheid toekennen is geen fout");
    }

    @Test void product_unit_assigned_to_a_single_unit_is_a_unit_error() {
        // §4.4 / §6.1: €·m is convertible only to an identical product, so assigning it to a € target flags a
        // unit mismatch — where the prior Onbekend silently passed.
        Datatype productEm = Typing.compute(BinOp.MAAL, numUnit(EUR), numUnit(new Unit("m", "Lengte"))).type();
        assertNotNull(Typing.assignment(numUnit(EUR), productEm), "€·m toegekend aan € moet een eenheidfout zijn");
        assertNull(Typing.assignment(productEm, productEm), "€·m toegekend aan €·m is correct");
    }

    @Test void times_unitless_operand_takes_the_other_unit_over() {   // §4.4 Tabel 11 rij 3/4 + Tabel 13
        // De dimensieloze "1" absorbeert onder vermenigvuldiging (1 × E = E), UNCONDITIONEEL — literal én attribuut
        // (de oude literal/attribuut-split is met besluit 16Y vervallen: was een unitloos ATTRIBUUT × eenheid -> Onbekend).
        var right = Typing.compute(BinOp.MAAL, num(), num("mnd", "Tijd"));
        assertTrue(right.ok() && right.type().unitOf() != null && "mnd".equals(right.type().unitOf().abbreviation()),
                () -> "een unitloze operand × eenheid neemt de eenheid (mnd) over, kreeg: " + right.type());
        var left = Typing.compute(BinOp.MAAL, num("mnd", "Tijd"), num());   // andersom: E × (none) -> E (rij 3)
        assertTrue(left.ok() && left.type().unitOf() != null && "mnd".equals(left.type().unitOf().abbreviation()),
                () -> "een eenheid × unitloze operand neemt de eenheid (mnd) over, kreeg: " + left.type());
        // §4.5 GEDEELD: een unitloze teller / eenheid is een omgekeerde eenheid (1/mnd) die het Eenheid-model niet kan
        // spellen -> besluit 17b: een LUIDE fout (bewuste divergentie van de dimensioneel onsound Tabel-14-rij-4-cel).
        var part = Typing.compute(BinOp.GEDEELD_DOOR, num(), num("mnd", "Tijd"));
        assertFalse(part.ok(), () -> "een unitloze teller / eenheid moet luid falen, kreeg: " + part.type());
        assertTrue(part.error().contains("omgekeerde eenheid"), part::error);
    }

    @Test void times_unspellable_rate_factor_is_a_loud_error() {   // besluit 17c
        // €/jr maal m: een tarief maal een niet-cancelende eenheid geeft een producteenheid MET een noemer (€·m/jr)
        // die het Eenheid-model niet kan spellen -> luide fout (was Onbekend "samengestelde eenheid").
        Unit m = new Unit("m", "Lengte");
        var res = Typing.compute(BinOp.MAAL, numUnit(Unit.compound(EUR, JR)), numUnit(m));
        assertFalse(res.ok(), () -> "een onspelbare ratefactor-product moet luid falen, kreeg: " + res.type());
        assertTrue(res.error().contains("niet uitdrukken"), res::error);
    }

    @Test void divided_by_a_rate_is_a_loud_error() {   // besluit 17c
        // € gedeeld door €/jr heeft een omgekeerde noemer (1/jr) -> onspelbaar -> luide fout (was Onbekend).
        var res = Typing.compute(BinOp.GEDEELD_DOOR, numUnit(EUR), numUnit(Unit.compound(EUR, JR)));
        assertFalse(res.ok(), () -> "een gedeeld-door-tarief moet luid falen, kreeg: " + res.type());
        assertTrue(res.error().contains("niet uitdrukken"), res::error);
    }
}
