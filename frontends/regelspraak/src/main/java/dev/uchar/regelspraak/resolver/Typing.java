package dev.uchar.regelspraak.resolver;

import dev.uchar.regelspraak.model.Datatype;
import dev.uchar.regelspraak.model.Unit;
import dev.uchar.regelspraak.core.TimeCalculation;

/**
 * The RegelSpraak v2.3.0 typeringen matrix (the "typeringen" specification),
 * encoded as pure functions over {@link Datatype}. This is the contract the
 * resolver enforces: which operand datatypes an operator admits, the result
 * datatype, and unit compatibility. Kept free of any parse-tree dependency so it
 * can be unit-tested directly against the matrix tables.
 *
 * References are to the typeringen document: §4 operators, §5 comparisons,
 * §6 resultaatdeel.
 */
public final class Typing {
    private Typing() {}

    /** Outcome of a typeringen check: a result datatype, or an error message. */
    public record Result(Datatype type, String error) {
        public boolean ok() { return error == null; }
        static Result ok(Datatype t) { return new Result(t, null); }
        static Result error(String f) { return new Result(null, f); }
    }

    public enum BinOp { PLUS, MIN, VERMINDERD_MET, MAAL, GEDEELD_DOOR, TOT_DE_MACHT }
    public enum CmpOp {
        KLEINER, KLEINER_OF_GELIJK, GROTER, GROTER_OF_GELIJK,   // §5.1,5.2,5.4,5.5 — getalachtig
        GELIJK, ONGELIJK,                                        // §5.3,5.6 — any, both same
        EERDER, EERDER_OF_GELIJK, LATER, LATER_OF_GELIJK         // §5.7-5.10 — datum
    }
    public enum UnairePred { LEEG, GEVULD, ELFPROEF, NUMERIEK_MET_EXACT, DAGSOORT }

    // -------- §4 arithmetic --------
    public static Result compute(BinOp op, Datatype l, Datatype r) {
        return compute(op, l, r, false);
    }

    /** {@code dayKindUnitRight}: the right operand's eenheid names a DECLARED §3.12 dagsoort (the caller
     *  holds the model, this matrix does not) — §6.11 then admits {@code datum ± N <dagsoort>} as a valid
     *  day-by-day date shift alongside the Tabel 2/5 tijdseenheden. */
    public static Result compute(BinOp op, Datatype l, Datatype r, boolean dayKindUnitRight) {
        return compute(op, l, r, dayKindUnitRight, false, false);
    }

    /** As {@link #compute(BinOp, Datatype, Datatype, boolean)}, knowing which operand is a unitless numeric literal
     *  ({@code lLit}, {@code rLit}): the one pair the IR admits and RegelSpraak's arithmetic refuses
     *  ({@link #unitsCompatible}). */
    public static Result compute(BinOp op, Datatype l, Datatype r, boolean dayKindUnitRight,
                                  boolean lLit, boolean rLit) {
        if (l.isUnknown() || r.isUnknown()) return Result.ok(new Datatype.Unknown("operand"));
        return switch (op) {
            // §4.1 / §4.2: numeriek±numeriek (units convertible), OR datum ± a numerieke
            // tijdsWAARDE -> datum (Tabel 2/5 scenario 2): the right operand must carry a TIJDseenheid
            // (or a §6.11 dagsoort) — "datum plus 5 km" is a typing ERROR, not a valid date shift (the
            // kern's chronoUnit is the one time-unit classifier, so the resolver rejects exactly what the
            // back-ends cannot execute). The result stays the LEFT type. A sub-day unit on a day-precision date is
            // refused, as the verifier refuses it (WF-035): such a date has no time of day to shift; §4.1's
            // "datum + sub-day unit -> Datum-tijd" reading has no wire form.
            case PLUS, MIN -> {
                String onName = op == BinOp.PLUS ? "plus" : "min";
                if (l.isDate() && r instanceof Datatype.Numeriek rn && rn.hasUnit()) {
                    if (l instanceof Datatype.DatumInDagen && subDayUnit(TimeCalculation.chronoUnit(r)))
                        yield Result.error(onName + ": sub-day time unit on a day-precision date (millisecond "
                                + "precision required)");
                    if (dayKindUnitRight || TimeCalculation.chronoUnit(r) != null) yield Result.ok(l);
                    yield Result.error(onName + ": " + unknownDurationUnit(rn.unitOf().afkorting()));
                }
                yield numericBinary(l, r, onName, lLit, rLit);
            }
            // §4.3 verminderd met: numeriek only
            case VERMINDERD_MET -> numericBinary(l, r, "verminderd met", lLit, rLit);
            // §4.4/4.5: Numeriek only (Tabel 11/14 list no Percentage row). Units form a group whose IDENTITY is
            // "geen eenheid" (the dimensionless "1"): 1 × E = E, so a unitless operand ABSORBS under multiplication
            // and the product carries the other operand's unit (besluit 16Y). Two units multiply/divide into a
            // composite the Eenheid model can spell (a rate "€/jr", an opaque product "€·m") or, when it cannot (a
            // rate factor, a reciprocal), a LOUD error — a unit it cannot name is never a wire type (besluit 17c).
            case MAAL, GEDEELD_DOOR -> {
                if (!l.isNumber() || !r.isNumber()) {
                    // WF-035 per operand that is no number; a percentage is a number to the verifier, which
                    // admits the product with it, so that refusal is RegelSpraak's own (Tabel 11/14)
                    String sentence = perSide("operand of a numeric operator is ", l, r, Datatype::isNumberLike);
                    yield Result.error(sentence != null ? naam(op) + ": " + sentence
                            : naam(op) + " vereist numerieke operanden, kreeg " + l.naam() + " en " + r.naam());
                }
                // §4.4 Tabel 13: a product gets the SUM of the operands' decimals ("X,X maal X,XX = X,XXX"),
                // not the max — so a 2dec × 2dec product is 4dec and §6.1.2 rightly demands an
                // afronding before it lands in a narrower attribute. §4.5 gedeeld door is an exact
                // fraction (no decimal precision of its own); maxDec under-reports it, but that errs
                // safe (a false negative, never a false positive) and is carded separately.
                int dec = op == BinOp.MAAL ? decimalsOf(l) + decimalsOf(r) : maxDec(l, r);
                Unit el = l.unitOf(), rightUnit = r.unitOf();
                if (el == null && rightUnit == null)                                       // Tabel 11/14 rij 5: (none) ⋆ (none) -> (none)
                    yield Result.ok(new Datatype.Numeriek(false, dec, Datatype.Teken.ONBEPAALD, null));
                if (rightUnit == null)                                                     // Tabel 11 rij 3 / Tabel 14 rij 3: E ⋆ (none) -> E
                    yield Result.ok(new Datatype.Numeriek(false, dec, Datatype.Teken.ONBEPAALD, el));
                if (el != null)   // both operands carry a unit -> the representable composite, or a loud error
                    yield compoundUnit(op, dec, el, rightUnit);
                // el == null, er != null: a unitless left, a unit-bearing right.
                if (op == BinOp.MAAL)
                    // §4.4 Tabel 11 rij 4 + Tabel 13: (none) maal Eenheid1 -> Eenheid1, decimalen = som. Unconditional:
                    // the dimensionless "1" absorbs (1 × E = E) whether the left is a LITERAL or an ATTRIBUTE — the old
                    // literal/attribute split retires (besluit 16Y).
                    yield Result.ok(new Datatype.Numeriek(false, dec, Datatype.Teken.ONBEPAALD, rightUnit));
                // §4.5 GEDEELD: Tabel 14 rij 4 says (none) gedeeld door Eenheid1 -> Eenheid1, but that cell is
                // dimensionally UNSOUND — a unitless numerator over a unit denominator is a reciprocal 1/E (a rate the
                // Eenheid model cannot spell), NOT E. Besluit 17b KNOWINGLY diverges from the arbiter here: a LOUD
                // error, never a wire type.
                yield Result.error("gedeeld door: een eenheidloze teller gedeeld door de eenheid '" + rightUnit.rendering()
                        + "' geeft een omgekeerde eenheid (1/" + rightUnit.rendering() + ") die het Eenheid-model niet kan uitdrukken");
            }
            // §4.7 tot de macht: Numeriek without a unit on either side; result without a unit.
            case TOT_DE_MACHT -> {
                if (!l.isNumber() || !r.isNumber()) {
                    // WF-035 per operand that is no number; a percentage's '%' is a unit to the verifier
                    String sentence = perSide("operand of a numeric operator is ", l, r, Datatype::isNumberLike);
                    yield Result.error("tot de macht: " + (sentence != null ? sentence : "power operands must be unitless"));
                }
                if (l.unitOf() != null || r.unitOf() != null)
                    yield Result.error("tot de macht: power operands must be unitless");   // the verifier's WF-035
                yield Result.ok(new Datatype.Numeriek(false, maxDec(l, r), Datatype.Teken.ONBEPAALD, null));
            }
        };
    }

    private static Result numericBinary(Datatype l, Datatype r, String op, boolean lLit, boolean rLit) {
        if (!l.isNumberLike() || !r.isNumberLike()) {
            String sentence = op.equals("plus") || op.equals("min") ? dateComputeSentence(op, l, r)
                    : perSide("operand of a numeric operator is ", l, r, Datatype::isNumberLike);
            return Result.error(sentence != null ? op + ": " + sentence
                    : "operator '" + op + "' vereist numerieke operanden, kreeg " + l.naam() + " en " + r.naam());
        }
        String unitMismatch = unitsCompatible(l, r, op, lLit, rLit, "additive operands have incompatible units");
        if (unitMismatch != null) return Result.error(unitMismatch);
        // §4.1/§4.2/§4.3 (plus/min/verminderd met — the only callers): geheel ± geheel IS geheel, so the
        // result keeps geheel-ness instead of erasing it — a geheel-demanding gate downstream (§5.13
        // elfproef, §4.19 paasdag, min/max allWhole) must not falsely reject whole-number arithmetic.
        // An unbounded "getal" operand (geheel=false, dec=0) correctly yields a non-geheel result.
        // decimalen stays maxDec per Tabellen 4/7/10; §6.1.2 checks read decimalen(), never geheel().
        return Result.ok(new Datatype.Numeriek(wholeOf(l) && wholeOf(r), maxDec(l, r), Datatype.Teken.ONBEPAALD, l.unitOf()));
    }

    private static boolean wholeOf(Datatype t) {
        return t instanceof Datatype.Numeriek n ? n.geheel()
             : t instanceof Datatype.Percentage p && p.geheel();
    }

    // -------- §5 comparison (result is always Boolean) --------
    public static Result compare(CmpOp op, Datatype l, Datatype r) {
        return compare(op, l, r, false, false);
    }

    /** As {@link #compare(CmpOp, Datatype, Datatype)}, knowing which operand is a unitless numeric literal. */
    public static Result compare(CmpOp op, Datatype l, Datatype r, boolean lLit, boolean rLit) {
        if (l.isUnknown() || r.isUnknown()) return Result.ok(new Datatype.BooleanType());
        return switch (op) {
            case KLEINER, KLEINER_OF_GELIJK, GROTER, GROTER_OF_GELIJK -> {
                if (!l.isNumberLike() || !r.isNumberLike())
                    yield Result.error("vergelijking: " + perSide("ordering operand is ", l, r, Datatype::isNumberLike));
                String u = unitsCompatible(l, r, "vergelijking", lLit, rLit, "ordering operands have incompatible units");
                yield u != null ? Result.error(u) : Result.ok(new Datatype.BooleanType());
            }
            // The ordering comparisons deliberately do NOT require equal tijd-precisie: Tabellen 45-52 list
            // only "Datum-tijd" operands with no precision restriction (unlike tijdsduur/eerste/laatste,
            // whose prose demands "dezelfde tijd-precisie") — the asymmetry is the spec's, kept as-is.
            case EERDER, EERDER_OF_GELIJK, LATER, LATER_OF_GELIJK -> {
                if (!l.isDate() || !r.isDate())
                    yield Result.error("vergelijking: " + perSide("date-ordering operand is ", l, r, Datatype::isDate));
                yield Result.ok(new Datatype.BooleanType());
            }
            case GELIJK, ONGELIJK -> {
                if (!sameFamily(l, r)) {
                    // two kinds are WF-036's family sentence; a number beside a percentage is one kind to the verifier,
                    // which reads the percentage's '%' as a unit and refuses the pair on units where no literal adopts it
                    if (!(l.isNumberLike() && r.isNumberLike()))
                        yield Result.error("gelijk: equality across families: " + kindWord(l) + " vs " + kindWord(r));
                    if (verifierRefusesUnits(l, r, lLit, rLit))
                        yield Result.error("gelijk: equality operands have incompatible units");
                    yield Result.error("gelijk/ongelijk vereist operanden van hetzelfde datatype, kreeg " + l.naam() + " en " + r.naam());
                }
                if (otherPrecision(l, r)) yield Result.error("gelijk: equality across date precisions");
                if (anotherDomain(l, r)) yield Result.error("gelijk: equality across different enumeration domains");
                if (l.isNumberLike()) {
                    String u = unitsCompatible(l, r, "gelijk", lLit, rLit, "equality operands have incompatible units");
                    if (u != null) yield Result.error(u);
                }
                yield Result.ok(new Datatype.BooleanType());
            }
        };
    }

    /** §6.1.4 begrenzing: the bounded value must be Numeriek/Percentage and the bound "een
     *  numerieke expressie" — both getalachtig, but NOT necessarily the same family (§6.1.4 lets
     *  a Percentage value take a plain numeric-literal bound). The unit rule is intentionally
     *  looser than {@link #unitsCompatible}: only two PRESENT-but-inconvertible units (km vs €)
     *  are flagged, while a unit-bearing literal against a unitless numeric is tolerated — not
     *  because the spec sanctions it, but to accommodate a known casus inconsistency:
     *  a bedrag declared unitless yet bounded by "0 €" (a convertible pair like mnd/jr is
     *  restated at transpile time). The verifier refuses that one-sided pair (WF-099), so the
     *  resolver warns about it beside this check. Two inconvertible units are the same WF-099
     *  defect and carry the verifier's sentence, naming the bound as the wire does ({@code which}:
     *  {@code minimum} or {@code maximum}). Returns the diagnostic, or null when ok / either
     *  side is Onbekend (no claim). */
    public static String begrenzing(Datatype base, Datatype limit, String which) {
        if (base.isUnknown() || limit.isUnknown()) return null;
        String baseError = clampBase(base);
        if (baseError != null) return baseError;
        if (!limit.isNumberLike())
            return clampNonNumeric(which, limit);
        Unit eb = base.unitOf(), eg = limit.unitOf();
        if (eb != null && eg != null && !eb.convertibleTo(eg))
            return "clamp " + which + " and base have incompatible units";
        return null;
    }

    /** §6.1.4 the bounded value must be Numeriek/Percentage: the verifier's WF-035 sentence for the clamp's base, or
     *  null. The base is one position whatever the number of bounds, so a caller reports it once. */
    public static String clampBase(Datatype base) {
        if (base.isUnknown() || base.isNumberLike()) return null;
        return clampNonNumeric("base", base);
    }

    /** WF-035's sentence for a clamp position (base, minimum or maximum) that holds no number, one template for the
     *  three positions as in the verifier. */
    private static String clampNonNumeric(String position, Datatype d) {
        return "clamp " + position + " is " + kindWord(d) + ", must be numeric";
    }

    /** §4.12/4.13 Tabel 22/23 — minimale/maximale waarde van (getalaggregatiefunctie): every
     *  operand must be a numerieke expressie, and "wanneer eenheden worden gebruikt, deze in
     *  elkaar om te rekenen zijn." Strictly Numeriek, NOT getalachtig: Tabel 22/23 carry only a
     *  Numeriek operand row, whereas the §5.1-5.4 ordinal-comparison tables (Tabel 32/34/39/41)
     *  carry an explicit Percentage row — the spec admits Percentage exactly where it lists it, so
     *  min/max excludes it (a Percentage min/max is a missed-diagnostic false-negative otherwise).
     *  The unit rule is the STRICT §4 one (unlike the lenient {@link #begrenzing}): all operands
     *  unitless, or all unit-bearing and pairwise inter-convertible — no casus quirk forces leniency
     *  here (its lone max is single-operand). Onbekend operands make no claim (skipped); a single
     *  operand is trivially well-typed. Returns the diagnostic, or null when ok. */
    public static String numberAggregation(String function, java.util.List<Datatype> operands) {
        Unit ref = null; boolean refSet = false;
        for (Datatype d : operands) {
            if (d.isUnknown()) continue;
            if (!d.isNumber())
                return function + " vereist numerieke operanden, kreeg " + d.naam();
            Unit e = d.unitOf();
            if (!refSet) { ref = e; refSet = true; continue; }
            if ((ref == null) != (e == null))
                return function + ": als één operand een eenheid heeft, moeten alle operanden er een hebben";
            if (ref != null && !ref.convertibleTo(e))
                return function + ": eenheden '" + ref.rendering() + "' en '" + e.rendering()
                        + "' zijn niet in elkaar om te rekenen";
        }
        return null;
    }

    /** §4.14/4.15 Tabel 24/25 — eerste/laatste van (datumaggregatiefunctie): every operand must
     *  be a datumexpressie, and "alle expressies dezelfde tijd-precisie bevatten" — modelled as
     *  the same concrete date kind (all {@link Datatype.DatumInDagen} or all {@link
     *  Datatype.DateTime}). The equal-precision rule is normative and twice-stated (§4.14/4.15
     *  prose AND §5.8.4: "Deze moeten namelijk allemaal gelijk zijn") with no coercion path — so
     *  this is strict equality. NOTE a spec tension: the §5.8.4 "Bevestigingstijdstip" worked
     *  example mixes a Datum-tijd operand with a Datum-in-dagen one, violating its own rule; we
     *  follow the normative prose over the inconsistent illustration (the example's standalone
     *  corpus form resolves clean only because its operands are Onbekend "Daarbij geldt"
     *  variables). Onbekend = no claim. Returns the diagnostic, or null when ok. */
    public static String dateAggregation(String function, String fold, java.util.List<Datatype> operands) {
        Datatype ref = null;
        for (Datatype d : operands) {
            if (d.isUnknown()) continue;
            if (!d.isDate())
                return function + " vereist datumexpressies, kreeg " + d.naam();
            if (ref == null) { ref = d; continue; }
            if (!ref.getClass().equals(d.getClass()))   // WF-038, with the fold's wire name ({@code fold})
                return function + ": " + fold + " orders dates of two precisions (day and millisecond), which share no "
                        + "order";
        }
        return null;
    }

    /** §5.11-5.16 one-sided predicates. */
    public static Result predicaat(UnairePred p, Datatype operand) {
        if (operand.isUnknown()) return Result.ok(new Datatype.BooleanType());
        return switch (p) {
            case LEEG, GEVULD -> Result.ok(new Datatype.BooleanType()); // §5.11/5.12 any datatype
            case ELFPROEF -> // §5.13 Tabel 55: Numeriek (geheel getal, geen eenheid) OR Tekst
                    operand.isText()
                        || (operand instanceof Datatype.Numeriek n && n.geheel() && !n.hasUnit())
                    ? Result.ok(new Datatype.BooleanType())
                    : Result.error("elfproef: elevenTest operand is " + kindWord(operand)
                            + ", must be text or a whole unitless number");   // the verifier's WF-036
            case NUMERIEK_MET_EXACT -> operand.isText()                 // §5.16 Tekst only
                    ? Result.ok(new Datatype.BooleanType())
                    : Result.error("numeriek met exact: exactDigits operand is " + kindWord(operand) + ", must be text");
            case DAGSOORT -> operand.isDate()                           // §5.15 Datum only
                    ? Result.ok(new Datatype.BooleanType())
                    : Result.error("dagsoort: day-kind predicate operand is " + kindWord(operand) + ", must be a date");
        };
    }

    /** §6.1/§6.2 assignment / initialisation: value must fit the target attribute. */
    public static String assignment(Datatype target, Datatype value) {
        return assignment(target, value, false);
    }

    /** As {@link #assignment(Datatype, Datatype)}, knowing whether the value is a unitless numeric literal. */
    public static String assignment(Datatype target, Datatype value, boolean valueLit) {
        if (target.isUnknown() || value.isUnknown()) return null;
        if (!sameFamily(target, value)) {
            // as in the equality arm of vergelijk: two kinds are WF-023's family sentence, a number beside a
            // percentage its units sentence where no literal adopts the '%'
            if (!(target.isNumberLike() && value.isNumberLike()))
                return "toekenning: a " + kindWord(value) + " value is assigned to a " + kindWord(target) + " target";
            if (verifierRefusesUnits(target, value, false, valueLit))
                return "toekenning: assignment across incompatible units";
            return "kan een waarde van type " + value.naam() + " niet toekennen aan een " + target.naam();
        }
        if (otherPrecision(target, value))
            return "toekenning: a " + kindWord(value) + " value is assigned to a " + kindWord(target) + " target";
        if (anotherDomain(target, value))
            // the verifier's WF-023 domain clause, its format string with its variant word
            return "toekenning: " + String.format("a value of the %s '%s' is assigned to a target of the %s '%s'",
                    "enumeration domain", ((Datatype.Enumeratie) value).domain(), "enumeration domain",
                    ((Datatype.Enumeratie) target).domain());
        if (!target.isNumberLike()) return null;
        String unitError = unitsCompatible(target, value, "toekenning", false, valueLit,
                "assignment across incompatible units");
        if (unitError != null) return unitError;
        // §6.1.2: a result may be assigned only when its decimal count does not exceed the
        // attribute's; a higher-precision result must first be explicitly rounded (§6.1.3). The
        // value's precision already reflects any afronding wrapper (Expr.Rounding.type()).
        // An UNBOUNDED target (§13.3.3 form (c) "getal") has no precision ceiling — it accepts any
        // number of decimals — so the check does not apply. Without this, an unbounded target reads
        // decimalsOf == 0 (the sentinel) and is wrongly treated like a "geheel getal" (cap 0),
        // rejecting any decimal value as over-precise (the false positive p33 reproduced).
        if (target.unbounded()) return null;
        int dv = decimalsOf(value), dd = decimalsOf(target);
        if (dv > dd)
            return "het toe te kennen resultaat heeft meer decimalen (" + dv + ") dan het attribuut toelaat ("
                    + dd + "); rond het resultaat eerst expliciet af (§6.1.2)";
        return null;
    }

    /** §6.1.2 for a §13.4.10 verdeling share, consistent with {@link #assignment} (a verdeling assigns a share
     *  to an attribute, so the same precision rule governs it). The share's decimal count follows the same
     *  rekentabel model as the equivalent expression: equal-shares is {@code total / count} — a division, and
     *  {@code count} is a whole count, so {@code maxDec} = the total's decimals; naar rato is
     *  {@code total · weight / Σweight} — the multiply contributes {@code total+weight} decimals and the
     *  division by {@code Σweight} does not raise that. An afronding to {@code roundDec} decimals caps the share
     *  at {@code roundDec} (exactly as an {@code Expr.Rounding} wrapper caps the value for {@link #assignment}),
     *  so a rounded verdeling fits a bounded target. An unbounded ({@code getal}) target has no ceiling.
     *  Returns the §6.1.2 message when the share is over-precise, else null. For all-integer operands
     *  the share is 0 decimals and this is a no-op — it only flags a decimal-typed bron/weight
     *  distributed into a narrower target, the case the ordinary {@code regel} assignment already flags. */
    public static String allocationAssignment(Datatype target, Datatype source, Datatype weight, Integer roundDec) {
        // An Onbekend bron/doel carries no type claim — the assignment check no-ops.
        if (target == null || source == null || target.isUnknown() || source.isUnknown())
            return null;
        // §13.4.10 the share is assigned to the doel like a regel assignment, so it must satisfy the SAME
        // family + unit rule as toekenning. The FAMILY check runs first, BEFORE the getalachtig guard, so a
        // numeric verdeelplafond distributed over a non-numeric recipient (a Numeriek bron into a Tekst/Datum
        // doel) is flagged as the family mismatch it is — exactly the fout the ordinary regel raises — instead
        // of slipping through to an uncompilable numeric setter.
        if (!sameFamily(target, source))
            return "kan een waarde van type " + source.naam() + " niet verdelen over een " + target.naam();
        // Same family but not getalachtig (e.g. both Datum/Tekst) — no unit/precision rule applies.
        if (!target.isNumberLike()) return null;
        // The share carries the BRON's eenheid: equal-shares (total/count) and naar rato (total·w/Σw) both keep
        // it — the count is a plain number and the weight ratio w/Σw is dimensionless, so the weight's unit
        // cancels. An inconvertible unit (€ distributed over a metre target) is the same fout regel raises.
        String unitError = unitsCompatible(target, source, "", false, false, null);   // RuleChecker prepends "verdeling: "
        if (unitError != null) return unitError;
        if (target.unbounded()) return null;   // an unbounded (getal) target has no precision ceiling
        int share = roundDec != null ? roundDec
                : decimalsOf(source) + (weight != null ? decimalsOf(weight) : 0);
        int dd = decimalsOf(target);
        return share > dd
                ? "het toe te kennen aandeel heeft meer decimalen (" + share + ") dan het attribuut toelaat ("
                  + dd + "); rond de verdeling eerst expliciet af (§6.1.2/§9.7.4)"
                : null;
    }

    // -------- helpers --------

    /** §4.4/4.5 result datatype for a maal/gedeeld of two unit-bearing operands, in the cases the Eenheid
     *  model can represent (a numerator over one simple denominator, or an opaque product); the rest are a LOUD
     *  error (besluit 17c: a unit the model cannot spell is never a wire type — it used to degrade to Onbekend).
     *  MAAL: a rate times its (convertible) denominator-unit cancels to the numerator ("€/jr maal jr = €"); two
     *  plain numerators are the opaque product "€ maal m = €·m"; a rate factor ("€·m/jr") is unspellable.
     *  GEDEELD: two plain units divide to a rate ("€ gedeeld door jr = €/jr"), or to a dimensionless number when
     *  the units are convertible ("€ gedeeld door € = een getal"); a rate divisor (a second denominator / "1/jr")
     *  is unspellable. */
    private static Result compoundUnit(BinOp op, int dec, Unit el, Unit rightUnit) {
        if (op == BinOp.MAAL) {
            if (el.denominator() != null && rightUnit.denominator() == null && rightUnit.convertibleTo(el.denominator()))
                return Result.ok(new Datatype.Numeriek(false, dec, Datatype.Teken.ONBEPAALD, el.numerator()));
            if (rightUnit.denominator() != null && el.denominator() == null && el.convertibleTo(rightUnit.denominator()))
                return Result.ok(new Datatype.Numeriek(false, dec, Datatype.Teken.ONBEPAALD, rightUnit.numerator()));
            // §4.4: two plain units multiply to a PRODUCT unit ("€ maal m = €·m") the model CAN spell — an opaque
            // composite (no canonical, IR-SPEC ch. 2.5), convertible only to an identical product, so a €·m result
            // assigned to a € target now flags a unit mismatch (it used to degrade to Onbekend and pass silently).
            if (el.denominator() == null && rightUnit.denominator() == null)
                return Result.ok(new Datatype.Numeriek(false, dec, Datatype.Teken.ONBEPAALD, Unit.product(el, rightUnit)));
            // a rate factor (a second denominator / "€·m/jr") is UNSPELLABLE — besluit 17c: a loud error, not Onbekend.
            return Result.error("maal: de samengestelde eenheid van '" + el.rendering() + "' en '" + rightUnit.rendering()
                    + "' (een producteenheid met een noemer) kan het Eenheid-model niet uitdrukken");
        }
        if (el.denominator() == null && rightUnit.denominator() == null)   // GEDEELD: only two plain units are representable
            return el.convertibleTo(rightUnit)
                    ? Result.ok(new Datatype.Numeriek(false, dec, Datatype.Teken.ONBEPAALD, null))                        // €/€ -> getal
                    : Result.ok(new Datatype.Numeriek(false, dec, Datatype.Teken.ONBEPAALD, Unit.compound(el, rightUnit))); // €/jr
        // a rate divisor (its result needs a second denominator / "1/jr") is UNSPELLABLE — besluit 17c: a loud error.
        return Result.error("gedeeld door: het quotiënt van '" + el.rendering() + "' en '" + rightUnit.rendering()
                + "' (een eenheid met een omgekeerde noemer) kan het Eenheid-model niet uitdrukken");
    }

    /** Same datatype family for gelijk/ongelijk/toekenning. Numeriek and Percentage are
     *  distinct here (the §5.3/5.6 and §6 tables list them as separate rows); the two date
     *  kinds count as one (the spec models a single date/time datatype with precision). */
    private static boolean sameFamily(Datatype a, Datatype b) {
        if (a.isNumber() && b.isNumber()) return true;
        if (a.isPercentage() && b.isPercentage()) return true;
        if (a.isDate() && b.isDate()) return true;
        return a.getClass().equals(b.getClass());
    }

    /** Mixed tijd-precisie (Datum-in-dagen vs Datum-en-tijd) in gelijk/ongelijk/toekenning — a CHOSEN
     *  strictness, not a table fact: Tabel 36/61 rij 4 list only "Datum-tijd" with no precision split,
     *  but a mixed toekenning generates a type-broken setter (a LocalDateTime into a LocalDate property)
     *  and a mixed gelijk compares values that can never be equal — both silently-dead outcomes. The
     *  language's own conversion forms are the escape hatch (documented in grammar/DIVERGENCES.md). The
     *  precision test is the same getClass() equality tijdsduur/eerste/laatste already use. The verifier refuses
     *  both (WF-036's equality arm, WF-023's family arm, where the two precisions are two kinds), and the callers
     *  carry its sentences. */
    private static boolean otherPrecision(Datatype a, Datatype b) {
        return a.isDate() && b.isDate() && !a.getClass().equals(b.getClass());
    }

    /** Tabel 36/43 rij 7 (vergelijking) and Tabel 61 rij 7 (toekenning): both sides must be the SAME
     *  enumeration — two attributes of DIFFERENT declared domains used to pass silently on class equality
     *  (a comparison that could only be label-coincidence, an assignment that puts a foreign label in). The
     *  verifier refuses both (WF-036's equality arm, WF-023's domain clause), and the callers carry its
     *  sentences. */
    private static boolean anotherDomain(Datatype a, Datatype b) {
        return a instanceof Datatype.Enumeratie ea && b instanceof Datatype.Enumeratie eb
                && !ea.domain().equals(eb.domain());
    }

    /** Unit rule (§4 note): both unitless, or both present and inter-convertible. {@code where} is the
     *  diagnostic context prefix; an EMPTY {@code where} omits it (for a caller that adds its own prefix,
     *  e.g. the verdeling check whose RuleChecker wrapper already prepends "verdeling: ").
     *  {@code verifierSentence} is the verifier's sentence for this position (IR-SPEC ch. 4.6: WF-035, WF-036,
     *  WF-023), null where the verifier crosses no units. A mismatch the verifier refuses too carries that
     *  sentence. The verifier admits one pair this rule refuses: a unit beside a unitless numeric LITERAL
     *  ({@code lLit}, {@code rLit}), which adopts it there; that refusal is RegelSpraak's own and keeps its words. */
    private static String unitsCompatible(Datatype l, Datatype r, String where, boolean lLit, boolean rLit,
                                             String verifierSentence) {
        Unit el = l.unitOf(), rightUnit = r.unitOf();
        if (el == null && rightUnit == null) return null;
        String pre = where.isEmpty() ? "" : where + ": ";
        if (el == null || rightUnit == null) {
            boolean literalWithoutUnit = el == null ? lLit : rLit;
            return pre + (verifierSentence != null && !literalWithoutUnit ? verifierSentence
                    : "als één operand een eenheid heeft, moeten beide er een hebben");
        }
        if (!el.convertibleTo(rightUnit))
            return pre + (verifierSentence != null ? verifierSentence
                    : "eenheden '" + el.rendering() + "' en '" + rightUnit.rendering() + "' zijn niet in elkaar om te rekenen");
        return null;
    }

    /** The verifier's sentence for each operand {@code goed} refuses, in operand order and joined by "; " as its two
     *  reports would be read; null when both pass. */
    private static String perSide(String sentenceStart, Datatype l, Datatype r,
                                  java.util.function.Predicate<Datatype> goed) {
        java.util.List<String> sentences = new java.util.ArrayList<>();
        if (!goed.test(l)) sentences.add(sentenceStart + kindWord(l));
        if (!goed.test(r)) sentences.add(sentenceStart + kindWord(r));
        return sentences.isEmpty() ? null : String.join("; ", sentences);
    }

    /** WF-035's sentences for a plus or min over an operand that is no number (RUNTIME-SEMANTICS ch. 3.8): date
     *  arithmetic takes a date on the left and a duration, a number carrying a time unit, on the right. Null where
     *  neither operand is a date: the verifier admits that pair, and RegelSpraak's refusal keeps its own words. */
    private static String dateComputeSentence(String op, Datatype l, Datatype r) {
        if (l.isDate() && r.isDate()) return "date \u00b1 date is ill-formed (date arithmetic takes a date and a duration)";
        if (r.isDate())
            return op.equals("min") ? "duration \u2212 date is ill-formed (subtraction admits the date on the left only)"
                    : "duration + date is ill-formed (the date stands on the left; date arithmetic does not commute)";
        if (!l.isDate()) return null;
        if (!r.isNumberLike())
            return "date \u00b1 duration: the duration operand is " + kindWord(r) + ", must be a number carrying a time unit";
        Unit e = r.unitOf();
        if (e == null) return "date \u00b1 duration: the duration operand carries no time unit";
        return unknownDurationUnit(e.afkorting());
    }

    /** Whether a {@link TimeCalculation#chronoUnit} is shorter than a day (the verifier's {@code time_unit_sub_day}). */
    static boolean subDayUnit(String chronoUnit) {
        return chronoUnit != null && java.util.Set.of("HOURS", "MINUTES", "SECONDS", "MILLIS").contains(chronoUnit);
    }

    /** WF-035's sentence for a duration unit that is neither a recognised time unit nor a declared day kind. */
    private static String unknownDurationUnit(String afkorting) {
        return "date \u00b1 duration: the duration unit '" + afkorting
                + "' is neither a recognised time unit nor a declared day kind";
    }

    /** WF-038's sentences for the operands of an ordering fold ({@code fold}: its wire name, first, last, minimum or
     *  maximum): each operand that is no number and no date, then two families (numbers and dates) or dates of two
     *  precisions among the rest, in the verifier's order and joined by "; ". Null where the verifier admits the
     *  operands; an unknown type makes no claim. */
    static String orderingFoldSentence(String fold, java.util.List<Datatype> types) {
        java.util.List<String> sentences = new java.util.ArrayList<>();
        java.util.Set<String> families = new java.util.LinkedHashSet<>();
        for (Datatype d : types) {
            if (d.isUnknown()) continue;
            if (d.isNumberLike()) families.add("getal");
            else if (d instanceof Datatype.DatumInDagen) families.add("dag");
            else if (d instanceof Datatype.DateTime) families.add("milliseconde");
            else sentences.add("operand of the " + fold + " ordering fold is " + kindWord(d) + ", must be a number or a date");
        }
        if (families.size() > 1)
            sentences.add(families.contains("getal")
                    ? fold + " orders operands of two comparable families, numbers and dates, which share no ordering"
                    : fold + " orders dates of two precisions (day and millisecond), which share no order");
        return sentences.isEmpty() ? null : String.join("; ", sentences);
    }

    /** The type name the verifier writes in WF-091's numeric-operand sentence ({@code wire_type_name}). */
    static String typeName(Datatype t) {
        return switch (t) {
            case Datatype.Numeriek n -> "Number";
            case Datatype.Percentage p -> "Number";
            case Datatype.Tekst x -> "Text";
            case Datatype.BooleanType b -> "Boolean";
            case Datatype.DatumInDagen d -> "Date";
            case Datatype.DateTime d -> "Date";
            case Datatype.Enumeratie e -> "Enumeration";
            case Datatype.Unknown o -> throw new IllegalStateException("Typering: een onbekend type heeft geen typenaam");
        };
    }

    /** Whether the verifier refuses a pair of number-like types on units: its {@code units_compatible_adopting}
     *  (IR-SPEC ch. 4.6) — a unitless numeric literal on either side adopts the other side's unit, and otherwise
     *  both sides are unitless or carry convertible units. */
    private static boolean verifierRefusesUnits(Datatype l, Datatype r, boolean lLit, boolean rLit) {
        if (lLit || rLit) return false;
        Unit el = l.unitOf(), rightUnit = r.unitOf();
        if (el == null && rightUnit == null) return false;
        return el == null || rightUnit == null || !el.convertibleTo(rightUnit);
    }

    /** The kind word the verifier writes for a datatype in a message ({@code datatype_kind} in
     *  spec/verifier/lib/checks.ml). An unknown type makes no claim and never reaches a message. */
    static String kindWord(Datatype t) {
        return switch (t) {
            case Datatype.Numeriek n -> "number";
            case Datatype.Percentage p -> "percentage";
            case Datatype.Tekst x -> "text";
            case Datatype.BooleanType b -> "boolean";
            case Datatype.DatumInDagen d -> "date-day";
            case Datatype.DateTime d -> "date-ms";
            case Datatype.Enumeratie e -> "enumeration";
            case Datatype.Unknown o -> throw new IllegalStateException("Typering: een onbekend type heeft geen soortwoord");
        };
    }

    /** The declared decimal count of a getalachtig datatype (0 for anything else). */
    private static int decimalsOf(Datatype t) {
        if (t instanceof Datatype.Numeriek n) return n.decimalen();
        if (t instanceof Datatype.Percentage p) return p.decimalen();
        return 0;
    }

    private static int maxDec(Datatype l, Datatype r) {
        return Math.max(decimalsOf(l), decimalsOf(r));
    }

    private static String naam(BinOp op) {
        return switch (op) {
            case PLUS -> "plus"; case MIN -> "min"; case VERMINDERD_MET -> "verminderd met";
            case MAAL -> "maal"; case GEDEELD_DOOR -> "gedeeld door"; case TOT_DE_MACHT -> "tot de macht";
        };
    }
}
