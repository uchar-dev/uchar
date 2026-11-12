package dev.uchar.regelspraak.resolver;

import dev.uchar.regelspraak.ir.Condition;
import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.model.Datatype;
import dev.uchar.regelspraak.model.Unit;
import java.util.List;
import dev.uchar.regelspraak.core.PrecisionBound;
import dev.uchar.regelspraak.core.TimeCalculation;
import dev.uchar.regelspraak.message.Component;
import dev.uchar.regelspraak.message.Msg;
import static dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic.params;

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

    /** A refusal: the {@code RGS-###} code and its named parameters, and the message they render to. A frame
     *  ({@link #frame}) puts a construct's name round the sentence; the code stays the refusal's own. */
    public record Coded(String code, java.util.Map<String, Object> params, Msg msg) {
        public Coded(String code, java.util.Map<String, Object> params) {
            this(code, params, new Msg(Component.FRONTEND, code, params));
        }
        /** The same refusal inside the {@code FRAME.<frame>} sentence, whose parameters {@code frameParams} name
         *  beside {@code sentence}, the refusal's own message. */
        Coded frame(String frame, Object... frameParams) {
            java.util.Map<String, Object> args = new java.util.LinkedHashMap<>(dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic.params(frameParams));
            args.put("sentence", msg);
            return new Coded(code, params, new Msg(Component.FRONTEND, "FRAME." + frame, args));
        }
        /** The same refusal under the variant template {@code code + suffix}: one template for each form of a
         *  sentence that agrees with a number or a combination of operands. */
        Coded variant(String suffix) {
            return suffix.isEmpty() ? this : new Coded(code, params, new Msg(Component.FRONTEND, code + suffix, params));
        }
        /** The refusal's text in the default language of the frontend. */
        public String text() { return msg.text(); }
        /** The diagnostic this refusal is, at {@code line}. */
        dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic at(
                dev.uchar.regelspraak.model.RegelSpraakModel.Severity severity, int line) {
            return new dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic(severity, code, params, msg, null, line);
        }
    }

    /** The verifier's own sentence for a unit mismatch at one position, with its template. */
    private record VerifierSentence(String code, java.util.Map<String, Object> params) {}

    /** Outcome of a typeringen check: a result datatype, or a refusal. */
    public record Result(Datatype type, Coded failure) {
        public boolean ok() { return failure == null; }
        /** The refusal's text, null when the check passed. */
        public String error() { return failure == null ? null : failure.text(); }
        static Result ok(Datatype t) { return new Result(t, null); }
        static Result error(Coded f) { return new Result(null, f); }
    }

    public enum BinOp { PLUS, MIN, VERMINDERD_MET, MAAL, GEDEELD_DOOR, TOT_DE_MACHT }
    public enum CmpOp {
        KLEINER, KLEINER_OF_GELIJK, GROTER, GROTER_OF_GELIJK,   // §5.1,5.2,5.4,5.5 — getalachtig
        GELIJK, ONGELIJK,                                        // §5.3,5.6 — any, both same
        EERDER, EERDER_OF_GELIJK, LATER, LATER_OF_GELIJK         // §5.7-5.10 — datum
    }
    public enum UnaryPred { LEEG, GEVULD, ELFPROEF, NUMERIEK_MET_EXACT, DAGSOORT }

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
            // core's chronoUnit is the one time-unit classifier, so the resolver rejects exactly what the
            // back-ends cannot execute). The result stays the LEFT type. A sub-day unit on a day-precision date is
            // refused, as the verifier refuses it (WF-035): such a date has no time of day to shift; §4.1's
            // "datum + sub-day unit -> Datum-tijd" reading has no wire form.
            case PLUS, MIN -> {
                String onName = op == BinOp.PLUS ? "plus" : "min";
                if (l.isDate() && r instanceof Datatype.Numeric rn && rn.hasUnit()) {
                    if (l instanceof Datatype.DateInDays && subDayUnit(TimeCalculation.chronoUnit(r)))
                        yield Result.error(new Coded("RGS-280", params("operation", onName)));
                    if (dayKindUnitRight || TimeCalculation.chronoUnit(r) != null) yield Result.ok(l);
                    yield Result.error(unknownDurationUnit(onName, rn.unitOf().rendering()).frame("operation", "operation", onName));
                }
                yield numericBinary(l, r, onName, lLit, rLit);
            }
            // §4.3 verminderd met: numeriek only
            case VERMINDERD_MET -> numericBinary(l, r, "verminderd met", lLit, rLit);
            // §4.4/4.5: Numeriek only (Tabel 11/14 list no Percentage row). Units form a group whose IDENTITY is
            // "geen eenheid" (the dimensionless "1"): 1 × E = E, so a unitless operand ABSORBS under multiplication
            // and the product carries the other operand's unit (besluit 16Y). Two units multiply and divide by their
            // factors (RUNTIME-SEMANTICS ch. 2.6), and every outcome is a unit or no unit.
            case MAAL, GEDEELD_DOOR -> {
                if (!l.isNumber() || !r.isNumber()) {
                    // WF-035 per operand that is no number; a percentage is a number to the verifier, which
                    // admits the product with it, so that refusal is RegelSpraak's own (Tabel 11/14)
                    Coded sentence = perSide("RGS-323", name(op), l, r,
                            Datatype::isNumberLike);
                    yield Result.error(sentence != null ? sentence.frame("operation", "operation", name(op))
                            : new Coded("RGS-281",
                                    params("operator", name(op), "left", l.name(), "right", r.name())));
                }
                // §4.4 Tabel 13: a product gets the SUM of the operands' decimals ("X,X maal X,XX = X,XXX"),
                // not the max — so a 2dec × 2dec product is 4dec and §6.1.2 rightly demands an
                // afronding before it lands in a narrower attribute. §4.5 gedeeld door is an exact
                // fraction (no decimal precision of its own); maxDec under-reports it, but that errs
                // safe (a false negative, never a false positive) and is carded separately.
                int dec = op == BinOp.MAAL ? decimalsOf(l) + decimalsOf(r) : maxDec(l, r);
                // §4.5 GEDEELD: Tabel 14 rij 4 says (none) gedeeld door Eenheid1 -> Eenheid1, but that cell is
                // dimensionally UNSOUND — a unitless numerator over a unit denominator is the reciprocal 1/E, NOT E.
                // Besluit 17b KNOWINGLY diverges from the arbiter here.
                Unit unit = op == BinOp.MAAL ? Unit.times(l.unitOf(), r.unitOf()) : Unit.divide(l.unitOf(), r.unitOf());
                yield Result.ok(new Datatype.Numeric(false, dec, Datatype.Sign.ONBEPAALD, unit));
            }
            // §4.7 tot de macht: Numeriek without a unit on either side; result without a unit.
            case TOT_DE_MACHT -> {
                if (!l.isNumber() || !r.isNumber()) {
                    // WF-035 per operand that is no number; a percentage's '%' is a unit to the verifier
                    Coded sentence = perSide("RGS-323", name(op), l, r,
                            Datatype::isNumberLike);
                    yield Result.error(sentence != null ? sentence.frame("power")
                            : new Coded("RGS-283", params()));
                }
                if (l.unitOf() != null || r.unitOf() != null)
                    yield Result.error(new Coded("RGS-283", params()));   // the verifier's WF-035
                yield Result.ok(new Datatype.Numeric(false, maxDec(l, r), Datatype.Sign.ONBEPAALD, null));
            }
        };
    }

    private static Result numericBinary(Datatype l, Datatype r, String op, boolean lLit, boolean rLit) {
        if (!l.isNumberLike() || !r.isNumberLike()) {
            Coded sentence = op.equals("plus") || op.equals("min") ? dateComputeSentence(op, l, r)
                    : perSide("RGS-323", op, l, r,
                            Datatype::isNumberLike);
            return Result.error(sentence != null ? sentence.frame("operation", "operation", op)
                    : new Coded("RGS-284",
                            params("operator", op, "left", l.name(), "right", r.name())));
        }
        Coded unitMismatch = unitsCompatible(l, r, op, lLit, rLit,
                new VerifierSentence("RGS-324", params("operation", op)));
        if (unitMismatch != null) return Result.error(unitMismatch);
        // §4.1/§4.2/§4.3 (plus/min/verminderd met — the only callers): geheel ± geheel IS geheel, so the
        // result keeps geheel-ness instead of erasing it — a geheel-demanding gate downstream (§5.13
        // elfproef, §4.19 paasdag, min/max allWhole) must not falsely reject whole-number arithmetic.
        // An unbounded "getal" operand (geheel=false, dec=0) correctly yields a non-geheel result.
        // decimalen stays maxDec per Tabellen 4/7/10; §6.1.2 checks read decimalen(), never geheel().
        // Two percentages give a percentage: on the wire a percentage is a number in '%' (IR-SPEC ch. 2.4), which the
        // sum keeps, so it goes into a Percentage target and compares with one as its operands do.
        if (l.isPercentage() && r.isPercentage())
            return Result.ok(new Datatype.Percentage(wholeOf(l) && wholeOf(r), maxDec(l, r), l.unitOf()));
        return Result.ok(new Datatype.Numeric(wholeOf(l) && wholeOf(r), maxDec(l, r), Datatype.Sign.ONBEPAALD, l.unitOf()));
    }

    private static boolean wholeOf(Datatype t) {
        return t instanceof Datatype.Numeric n ? n.wholeNumber()
             : t instanceof Datatype.Percentage p && p.wholeNumber();
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
                    yield Result.error(perSide("RGS-325", "vergelijking", l, r,
                            Datatype::isNumberLike).frame("comparison"));
                Coded u = unitsCompatible(l, r, "vergelijking", lLit, rLit,
                        new VerifierSentence("RGS-326", params()));
                yield u != null ? Result.error(u) : Result.ok(new Datatype.BooleanType());
            }
            // A day and a millisecond share no order: the verifier refuses the pair (WF-036), so the frontend does,
            // as it refuses the equality and the assignment of the two precisions.
            case EERDER, EERDER_OF_GELIJK, LATER, LATER_OF_GELIJK -> {
                if (!l.isDate() || !r.isDate())
                    yield Result.error(perSide("RGS-327", "vergelijking", l, r,
                            Datatype::isDate).frame("comparison"));
                if (otherPrecision(l, r))
                    yield Result.error(new Coded("RGS-430", params()).frame("comparison"));
                yield Result.ok(new Datatype.BooleanType());
            }
            case GELIJK, ONGELIJK -> {
                if (!sameFamily(l, r)) {
                    // two kinds are WF-036's family sentence; a number beside a percentage is one kind to the verifier,
                    // which reads the percentage's '%' as a unit and refuses the pair on units where no literal adopts it
                    if (!(l.isNumberLike() && r.isNumberLike()))
                        yield Result.error(new Coded("RGS-285",
                                params("left", kindWord(l), "right", kindWord(r))));
                    if (verifierRefusesUnits(l, r, lLit, rLit))
                        yield Result.error(new Coded("RGS-286", params()).frame("operation", "operation", "gelijk"));
                    yield Result.error(new Coded("RGS-287", params("left", l.name(), "right", r.name())));
                }
                if (otherPrecision(l, r))
                    yield Result.error(new Coded("RGS-288", params()));
                if (anotherDomain(l, r))
                    yield Result.error(new Coded("RGS-289", params()));
                if (l.isNumberLike()) {
                    Coded u = unitsCompatible(l, r, "gelijk", lLit, rLit,
                            new VerifierSentence("RGS-286", params()));
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
    public static Coded clamp(Datatype base, Datatype limit, String which) {
        if (base.isUnknown() || limit.isUnknown()) return null;
        Coded baseError = clampBase(base);
        if (baseError != null) return baseError;
        if (!limit.isNumberLike())
            return clampNonNumeric(which, limit);
        Unit eb = base.unitOf(), eg = limit.unitOf();
        if (eb != null && eg != null && !eb.convertibleTo(eg))
            return new Coded("RGS-060", params("bound", Msg.frontend("PART.clamp." + which)));
        return null;
    }

    /** §6.1.4 the bounded value must be Numeriek/Percentage: the verifier's WF-035 sentence for the clamp's base, or
     *  null. The base is one position whatever the number of bounds, so a caller reports it once. */
    public static Coded clampBase(Datatype base) {
        if (base.isUnknown() || base.isNumberLike()) return null;
        return clampNonNumeric("base", base);
    }

    /** WF-035's sentence for a clamp position (base, minimum or maximum) that holds no number, one template for the
     *  three positions as in the verifier. */
    private static Coded clampNonNumeric(String position, Datatype d) {
        return new Coded("RGS-328", params("position", Msg.frontend("PART.clamp." + position), "kind", kindWord(d)));
    }

    /** §4.12/4.13 Tabel 22/23 — minimale/maximale waarde van (getalaggregatiefunctie): every
     *  operand must be a numerieke expressie, and "wanneer eenheden worden gebruikt, deze in
     *  elkaar om te rekenen zijn." Strictly Numeriek, NOT getalachtig: Tabel 22/23 carry only a
     *  Numeriek operand row, whereas the §5.1-5.4 ordinal-comparison tables (Tabel 32/34/39/41)
     *  carry an explicit Percentage row — the spec admits Percentage exactly where it lists it, so
     *  min/max excludes it (a Percentage min/max is a missed-diagnostic false-negative otherwise).
     *  The unit rule is the STRICT §4 one (unlike the lenient {@link #clamp}): all operands
     *  unitless, or all unit-bearing and pairwise inter-convertible — no casus quirk forces leniency
     *  here (its lone max is single-operand). Onbekend operands make no claim (skipped); a single
     *  operand is trivially well-typed. Returns the diagnostic, or null when ok. */
    public static Coded numberAggregation(String function, java.util.List<Datatype> operands) {
        Unit ref = null; boolean refSet = false;
        for (Datatype d : operands) {
            if (d.isUnknown()) continue;
            if (!d.isNumber())
                return new Coded("RGS-329", params("function", function, "type", d.name()));
            Unit e = d.unitOf();
            if (!refSet) { ref = e; refSet = true; continue; }
            if ((ref == null) != (e == null))
                return new Coded("RGS-330", params("function", function));
            if (ref != null && !ref.convertibleTo(e))
                return new Coded("RGS-331",
                        params("function", function, "left", ref.rendering(), "right", e.rendering()));
        }
        return null;
    }

    /** §4.14/4.15 Tabel 24/25 — eerste/laatste van (datumaggregatiefunctie): every operand must
     *  be a datumexpressie, and "alle expressies dezelfde tijd-precisie bevatten" — modelled as
     *  the same concrete date kind (all {@link Datatype.DateInDays} or all {@link
     *  Datatype.DateTime}). The equal-precision rule is normative and twice-stated (§4.14/4.15
     *  prose AND §5.8.4: "Deze moeten namelijk allemaal gelijk zijn") with no coercion path — so
     *  this is strict equality. NOTE a spec tension: the §5.8.4 "Bevestigingstijdstip" worked
     *  example mixes a Datum-tijd operand with a Datum-in-dagen one, violating its own rule; we
     *  follow the normative prose over the inconsistent illustration (the example's standalone
     *  corpus form resolves clean only because its operands are Onbekend "Daarbij geldt"
     *  variables). Unknown = no claim. Returns the diagnostic, or null when ok. */
    public static Coded dateAggregation(String function, String fold, java.util.List<Datatype> operands) {
        Datatype ref = null;
        for (Datatype d : operands) {
            if (d.isUnknown()) continue;
            if (!d.isDate())
                return new Coded("RGS-332", params("function", function, "type", d.name()));
            if (ref == null) { ref = d; continue; }
            if (!ref.getClass().equals(d.getClass()))   // WF-038, with the fold's wire name ({@code fold})
                return new Coded("RGS-333", params("function", function, "fold", fold));
        }
        return null;
    }

    /** §5.11-5.16 one-sided predicates. */
    public static Result predicate(UnaryPred p, Datatype operand) {
        if (operand.isUnknown()) return Result.ok(new Datatype.BooleanType());
        return switch (p) {
            case LEEG, GEVULD -> Result.ok(new Datatype.BooleanType()); // §5.11/5.12 any datatype
            case ELFPROEF -> // §5.13 Tabel 55: Numeriek (geheel getal, geen eenheid) OR Tekst
                    operand.isText()
                        || (operand instanceof Datatype.Numeric n && n.wholeNumber() && !n.hasUnit())
                    ? Result.ok(new Datatype.BooleanType())
                    : Result.error(new Coded("RGS-290", params("kind", kindWord(operand))));
            case NUMERIEK_MET_EXACT -> operand.isText()                 // §5.16 Tekst only
                    ? Result.ok(new Datatype.BooleanType())
                    : Result.error(new Coded("RGS-291", params("kind", kindWord(operand))));
            case DAGSOORT -> operand.isDate()                           // §5.15 Datum only
                    ? Result.ok(new Datatype.BooleanType())
                    : Result.error(new Coded("RGS-292", params("kind", kindWord(operand))));
        };
    }

    /** §6.1/§6.2 assignment of a value whose type is its own precision, a scenario's case value: family, unit and
     *  domain as {@link #assignment(Datatype, Datatype, boolean)}, and the decimals of the value against the target's
     *  (RGS-338): a case value is the number it writes, so its type is its precision. */
    public static Coded assignment(Datatype target, Datatype value) {
        Coded kind = assignment(target, value, false);
        if (kind != null || target.isUnknown() || value.isUnknown() || !target.isNumberLike() || target.unbounded())
            return kind;
        int dv = decimalsOf(value), dd = decimalsOf(target);
        return dv > dd ? new Coded("RGS-338", params("valueDecimals", dv, "targetDecimals", dd)) : null;
    }

    /** §6.1/§6.2 assignment / initialisation of a computed value: family, unit and domain. Its decimals are the
     *  precision gate's ({@link #precisionGate}), over the bound of the expression rather than its type. */
    public static Coded assignment(Datatype target, Datatype value, boolean valueLit) {
        if (target.isUnknown() || value.isUnknown()) return null;
        if (!sameFamily(target, value)) {
            // as in the equality arm of vergelijk: two kinds are WF-023's family sentence, a number beside a
            // percentage its units sentence where no literal adopts the '%'
            if (!(target.isNumberLike() && value.isNumberLike()))
                return new Coded("RGS-334", params("value", kindWord(value), "target", kindWord(target)));
            if (verifierRefusesUnits(target, value, false, valueLit))
                return new Coded("RGS-335", params("valueUnit", unitSpelling(value), "targetUnit", unitSpelling(target)))
                        .frame("operation", "operation", "toekenning");
            return new Coded("RGS-336", params("valueType", value.name(), "targetType", target.name()));
        }
        if (otherPrecision(target, value))
            return new Coded("RGS-334", params("value", kindWord(value), "target", kindWord(target)));
        if (anotherDomain(target, value))
            // the verifier's WF-023 domain clause, its format string with its variant word
            return new Coded("RGS-337",
                    params("valueDomain", ((Datatype.Enumeration) value).domain(),
                            "targetDomain", ((Datatype.Enumeration) target).domain(), "domainWord", "enumeration domain"));
        if (!target.isNumberLike()) return null;
        return unitsCompatible(target, value, "toekenning", false, valueLit,
                new VerifierSentence("RGS-335", params("valueUnit", unitSpelling(value), "targetUnit", unitSpelling(target))));
    }

    // -------- §6.1.2 the precision bound (RUNTIME-SEMANTICS ch. 2.3) and the gate of ch. 2.4 --------

    /** The precision bound of {@code e} (RUNTIME-SEMANTICS ch. 2.3), read through the types this resolver computed;
     *  null where it makes no claim. */
    public static PrecisionBound.Bounded precision(Expr e, dev.uchar.regelspraak.model.RegelSpraakModel model) {
        return new PrecisionBound(model, reading(model)).of(e, java.util.Map.of());
    }

    /** The document as the resolver sees it: the types it computed, a fold over members where the operand reads a
     *  set ({@code AttributeReference.set}), and the defaults groups of the model. */
    private static PrecisionBound.Reading reading(dev.uchar.regelspraak.model.RegelSpraakModel model) {
        return new PrecisionBound.Reading() {
            @Override public Datatype typeOf(Expr e) { return e.type(); }
            @Override public boolean ranged(Expr.FunctionCall f) {
                return f.arguments().size() == 1 && f.arguments().get(0) instanceof Expr.AttributeReference a && a.set();
            }
            @Override public List<dev.uchar.regelspraak.ir.PrioritizedDefaults> defaults() {
                return model.prioritizedDefaults;
            }
        };
    }

    /** §6.1.2, the precision gate of RUNTIME-SEMANTICS ch. 2.4: a computed value whose bound, restated into the unit of
     *  the target, exceeds the target's declared decimals is RGS-372, and one without a finite bound RGS-370; an
     *  unbounded target has no ceiling, and a value that makes no claim passes. A rule on a target with a timeline
     *  stands in the gate with its bound per segment, the value of each segment being the value of the expression on
     *  its period (ch. 2.3, ch. 3.4). A write marked {@code outside} stands outside the gate: the pro-rata write of
     *  ch. 3.7 into a timeline, around which no executor carries a rounding, so the gate would leave that write no form
     *  (ch. 2.8), and a timeline member of an object creation, which no executor writes. The positions are the
     *  verifier's ({@code wf023_precision}). */
    public static Coded precisionGate(Datatype target, boolean outside, Expr value,
                                      dev.uchar.regelspraak.model.RegelSpraakModel model) {
        if (outside || target.isUnknown() || !target.isNumberLike() || target.unbounded()) return null;
        PrecisionBound.Bounded b = precision(value, model);
        if (b == null) return null;
        PrecisionBound.Precision p = b.in(target.unitOf());
        int dd = decimalsOf(target);
        if (!p.bounded()) return new Coded("RGS-370", params("targetDecimals", dd));
        return p.decimals() > dd ? new Coded("RGS-372", params("valueDecimals", p.decimals(), "targetDecimals", dd)) : null;
    }


    /** §6.1.2 for a §13.4.10 verdeling share: the family and unit rule of {@link #assignment}, and the precision gate
     *  over the bound of a share, the rounding of the verdeling ({@code roundDec}) or else the declaration of its pot.
     *  An unbounded ({@code getal}) target has no ceiling, and a target with a timeline stands outside the gate: an
     *  allocation writes no timeline. Returns the refusal, or null. */
    public static Coded allocationAssignment(Datatype target, boolean timeline, Datatype source, Integer roundDec) {
        // An Onbekend bron/doel carries no type claim — the assignment check no-ops.
        if (target == null || source == null || target.isUnknown() || source.isUnknown())
            return null;
        // §13.4.10 the share is assigned to the doel like a regel assignment, so it must satisfy the SAME
        // family + unit rule as toekenning. The FAMILY check runs first, BEFORE the getalachtig guard, so a
        // numeric verdeelplafond distributed over a non-numeric recipient (a Numeriek bron into a Tekst/Datum
        // doel) is flagged as the family mismatch it is — exactly the fout the ordinary regel raises — instead
        // of slipping through to an uncompilable numeric setter.
        if (!sameFamily(target, source))
            return new Coded("RGS-339", params("sourceType", source.name(), "targetType", target.name()));
        // Same family but not getalachtig (e.g. both Datum/Tekst) — no unit/precision rule applies.
        if (!target.isNumberLike()) return null;
        // The share carries the BRON's eenheid: equal-shares (total/count) and naar rato (total·w/Σw) both keep
        // it — the count is a plain number and the weight ratio w/Σw is dimensionless, so the weight's unit
        // cancels. An inconvertible unit (€ distributed over a metre target) is the same fout regel raises.
        Coded unitError = unitsCompatible(target, source, "", false, false, null);   // RuleChecker prepends "verdeling: "
        if (unitError != null) return unitError;
        if (timeline || target.unbounded()) return null;   // an unbounded (getal) target has no precision ceiling
        // the bound of a share is the rounding of the allocation, and without one the declaration of its pot (the ALEF
        // form): the division by the recipients is not typed, so a share that does not fit with a pot that does stays
        // the evaluation fault EF-006 (RUNTIME-SEMANTICS ch. 2.4)
        PrecisionBound.Precision share = new PrecisionBound.Bounded(
                roundDec != null ? new PrecisionBound.Precision(roundDec) : PrecisionBound.declared(source), source.unitOf())
                .in(target.unitOf());
        int dd = decimalsOf(target);
        if (!share.bounded()) return new Coded("RGS-371", params("targetDecimals", dd));
        return share.decimals() > dd
                ? new Coded("RGS-340", params("shareDecimals", share.decimals(), "targetDecimals", dd))
                : null;
    }

    // -------- helpers --------

    /** Same datatype family for gelijk/ongelijk/toekenning. Numeriek and Percentage are
     *  distinct here (the §5.3/5.6 and §6 tables list them as separate rows); the two date
     *  kinds count as one (the spec models a single date/time datatype with precision). */
    private static boolean sameFamily(Datatype a, Datatype b) {
        if (a.isNumber() && b.isNumber()) return true;
        if (a.isPercentage() && b.isPercentage()) return true;
        if (a.isDate() && b.isDate()) return true;
        return a.getClass().equals(b.getClass());
    }

    /** Mixed tijd-precisie (Datum-in-dagen vs Datum-en-tijd) in gelijk/ongelijk/toekenning and the orderings — a CHOSEN
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
     *  enumeration — two attributes of DIFFERENT declared domains would pass silently on class equality
     *  (a comparison that could only be label-coincidence, an assignment that puts a foreign label in). The
     *  verifier refuses both (WF-036's equality arm, WF-023's domain clause), and the callers carry its
     *  sentences. */
    private static boolean anotherDomain(Datatype a, Datatype b) {
        return a instanceof Datatype.Enumeration ea && b instanceof Datatype.Enumeration eb
                && !ea.domain().equals(eb.domain());
    }

    /** Unit rule (§4 note): both unitless, or both present and inter-convertible. {@code where} is the
     *  diagnostic context prefix; an EMPTY {@code where} omits it (for a caller that adds its own prefix,
     *  e.g. the verdeling check whose RuleChecker wrapper already prepends "verdeling: ").
     *  {@code verifierSentence} is the verifier's sentence for this position (IR-SPEC ch. 4.6: WF-035, WF-036,
     *  WF-023), null where the verifier crosses no units. A mismatch the verifier refuses too carries that
     *  sentence. The verifier admits one pair this rule refuses: a unit beside a unitless numeric LITERAL
     *  ({@code lLit}, {@code rLit}), which adopts it there; that refusal is RegelSpraak's own and keeps its words. */
    private static Coded unitsCompatible(Datatype l, Datatype r, String where, boolean lLit, boolean rLit,
                                             VerifierSentence verifierSentence) {
        Unit el = l.unitOf(), rightUnit = r.unitOf();
        if (el == null && rightUnit == null) return null;
        if (el == null || rightUnit == null) {
            boolean literalWithoutUnit = el == null ? lLit : rLit;
            return framed(verifierSentence != null && !literalWithoutUnit
                    ? new Coded(verifierSentence.code(), verifierSentence.params())
                    : new Coded("RGS-341", params("operation", where)), where);
        }
        if (!el.convertibleTo(rightUnit))
            return framed(verifierSentence != null
                    ? new Coded(verifierSentence.code(), verifierSentence.params())
                    : new Coded("RGS-342",
                            params("operation", where, "left", el.rendering(), "right", rightUnit.rendering())), where);
        return null;
    }

    /** Whether a quantity in {@code unit} reduces to {@code target}: both without a unit, or two convertible units. */
    static boolean reducible(Unit unit, Unit target) {
        return unit == null ? target == null : target != null && unit.convertibleTo(target);
    }

    /** {@code c} under the sentence of the construct {@code where} names; an empty {@code where} adds none. */
    private static Coded framed(Coded c, String where) {
        return where.isEmpty() ? c : c.frame("operation", "operation", where);
    }

    /** The verifier's sentence for each operand {@code goed} refuses, in operand order, which the template renders
     *  joined; null when both pass. */
    private static Coded perSide(String code, String operation, Datatype l, Datatype r,
                                 java.util.function.Predicate<Datatype> goed) {
        boolean left = !goed.test(l), right = !goed.test(r);
        if (!left && !right) return null;
        java.util.List<Msg> sentences = new java.util.ArrayList<>();
        if (left) sentences.add(Msg.frontend(code + ".side", "kind", kindWord(l)));
        if (right) sentences.add(Msg.frontend(code + ".side", "kind", kindWord(r)));
        // an operand that passes has no sentence, so its kind is empty
        return new Coded(code, params("operation", operation,
                "left", left ? kindWord(l) : "", "right", right ? kindWord(r) : "", "sentences", sentences));
    }

    /** WF-035's sentences for a plus or min over an operand that is no number (RUNTIME-SEMANTICS ch. 3.8): date
     *  arithmetic takes a date on the left and a duration, a number carrying a time unit, on the right. Null where
     *  neither operand is a date: the verifier admits that pair, and RegelSpraak's refusal keeps its own words. */
    private static Coded dateComputeSentence(String op, Datatype l, Datatype r) {
        if (l.isDate() && r.isDate())
            return new Coded("RGS-295", params("operation", op));
        if (r.isDate())
            return op.equals("min")
                    ? new Coded("RGS-296", params())
                    : new Coded("RGS-297", params());
        if (!l.isDate()) return null;
        if (!r.isNumberLike())
            return new Coded("RGS-298", params("operation", op, "kind", kindWord(r)));
        Unit e = r.unitOf();
        if (e == null)
            return new Coded("RGS-299", params("operation", op));
        return unknownDurationUnit(op, e.rendering());
    }

    /** Whether a {@link TimeCalculation#chronoUnit} is shorter than a day (the verifier's {@code time_unit_sub_day}). */
    static boolean subDayUnit(String chronoUnit) {
        return chronoUnit != null && java.util.Set.of("HOURS", "MINUTES", "SECONDS", "MILLIS").contains(chronoUnit);
    }

    /** WF-035's sentence for a duration unit that is neither a recognised time unit nor a declared day kind. */
    private static Coded unknownDurationUnit(String operation, String abbreviation) {
        return new Coded("RGS-343", params("operation", operation, "unit", abbreviation));
    }

    /** WF-038's sentences for the operands of an ordering fold ({@code fold}: its wire name, first, last, minimum or
     *  maximum): each operand that is no number and no date, then two families (numbers and dates) or dates of two
     *  precisions among the rest, in the verifier's order and joined by "; ". Null where the verifier admits the
     *  operands; an unknown type makes no claim. */
    static Coded orderingFoldSentence(String fold, java.util.List<Datatype> types) {
        java.util.List<Msg> sentences = new java.util.ArrayList<>();
        java.util.List<Msg> kinds = new java.util.ArrayList<>();
        java.util.Set<String> families = new java.util.LinkedHashSet<>();
        for (Datatype d : types) {
            if (d.isUnknown()) continue;
            if (d.isNumberLike()) families.add("getal");
            else if (d instanceof Datatype.DateInDays) families.add("dag");
            else if (d instanceof Datatype.DateTime) families.add("milliseconde");
            else {
                kinds.add(kindWord(d));
                sentences.add(new Msg(Component.FRONTEND, "RGS-344.operand-sentence", params("fold", fold, "kind", kindWord(d))));
            }
        }
        String mixed = "";
        if (families.size() > 1) {
            mixed = families.contains("getal") ? "numbers-and-dates" : "dates-of-two-precisions";
            sentences.add(new Msg(Component.FRONTEND,
                    families.contains("getal") ? "RGS-344.numbers-and-dates" : "RGS-344.dates-of-two-precisions",
                    params("fold", fold)));
        }
        // one sentence per operand that is no number and no date, in operand order, and one for the mixture of
        // families when there is one; the template renders the list, joined, and either part may be empty
        return sentences.isEmpty() ? null : new Coded("RGS-344",
                params("fold", fold, "kinds", new Msg.Joined(kinds, ", "), "mixture", mixed, "sentences", sentences));
    }

    /** The type name the verifier writes in WF-091's numeric-operand sentence ({@code wire_type_name}). */
    static String typeName(Datatype t) {
        return switch (t) {
            case Datatype.Numeric n -> "Number";
            case Datatype.Percentage p -> "Number";
            case Datatype.Text x -> "Text";
            case Datatype.BooleanType b -> "Boolean";
            case Datatype.DateInDays d -> "Date";
            case Datatype.DateTime d -> "Date";
            case Datatype.Enumeration e -> "Enumeration";
            case Datatype.SumType s -> "SumType";
            case Datatype.Unknown o -> throw new IllegalStateException("Typing: an unknown type has no type name");
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

    /** A unit in the verifier's WF-023 sentence: an author needs to read which unit the value took and which the
     *  target has, also where one of them has none. */
    static String unitSpelling(Datatype t) {
        return t.unitOf() == null ? "geen" : t.unitOf().rendering();
    }

    /** The kind word of a datatype in a message, a part of the catalogue: its English entry is the word the verifier
     *  writes ({@code datatype_kind} in spec/verifier/lib/checks.ml). An unknown type makes no claim and never reaches
     *  a message. */
    static Msg kindWord(Datatype t) {
        return Msg.frontend("PART.kind." + switch (t) {
            case Datatype.Numeric n -> "number";
            case Datatype.Percentage p -> "percentage";
            case Datatype.Text x -> "text";
            case Datatype.BooleanType b -> "boolean";
            case Datatype.DateInDays d -> "date-day";
            case Datatype.DateTime d -> "date-ms";
            case Datatype.Enumeration e -> "enumeration";
            case Datatype.SumType s -> "sumtype";
            case Datatype.Unknown o -> throw new IllegalStateException("Typing: an unknown type has no kind word");
        });
    }

    /** The declared decimal count of a getalachtig datatype (0 for anything else). */
    private static int decimalsOf(Datatype t) {
        if (t instanceof Datatype.Numeric n) return n.decimals();
        if (t instanceof Datatype.Percentage p) return p.decimals();
        return 0;
    }

    private static int maxDec(Datatype l, Datatype r) {
        return Math.max(decimalsOf(l), decimalsOf(r));
    }

    private static String name(BinOp op) {
        return switch (op) {
            case PLUS -> "plus"; case MIN -> "min"; case VERMINDERD_MET -> "verminderd met";
            case MAAL -> "maal"; case GEDEELD_DOOR -> "gedeeld door"; case TOT_DE_MACHT -> "tot de macht";
        };
    }
}
