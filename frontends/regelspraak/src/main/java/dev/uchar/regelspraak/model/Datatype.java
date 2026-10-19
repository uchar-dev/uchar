package dev.uchar.regelspraak.model;

import java.util.List;

/**
 * The RegelSpraak datatypes (§13.3.3), the typed core of the resolved AST. Every
 * attribute, parameter, domain, and (inferred) expression carries one of these.
 * The typeringen matrix (see {@link dev.uchar.regelspraak.resolver.Typing}) is
 * defined over them.
 *
 * Units live on {@link Numeric}: a unit is optional, but once one operand in an
 * arithmetic pattern carries a unit the others must too (typeringen §4).
 */
public sealed interface Datatype
        permits Datatype.Numeric, Datatype.Percentage, Datatype.Text, Datatype.BooleanType,
                Datatype.DateInDays, Datatype.DateTime, Datatype.Enumeration, Datatype.Unknown {

    /** §13.3.3.2 Numeriek (geheel/decimaal) with an optional unit. */
    record Numeric(boolean wholeNumber, int decimals, Sign sign, Unit unit) implements Datatype {
        public boolean hasUnit() { return unit != null; }
    }
    /** §13.3.3.3 Percentage. A Percentage attribuut "heeft verder altijd als eenheid %" (§3.3.5),
     *  optionally specialized as a rate ("%/jaar"); so its {@code unit} is never null. */
    record Percentage(boolean wholeNumber, int decimals, Unit unit) implements Datatype {
        /** The implicit "%" unit every Percentage carries (§3.3.5). */
        public static final Unit PERCENT = new Unit("%", null);
        /** A bare percentage, carrying the implicit "%" unit. */
        public Percentage(boolean wholeNumber, int decimals) { this(wholeNumber, decimals, PERCENT); }
    }
    /** §13.3.3.4 Tekst. */
    record Text() implements Datatype {}
    /** §13.3.3.5 Boolean. */
    record BooleanType() implements Datatype {}
    /** §13.3.3.6 Datum in dagen. */
    record DateInDays() implements Datatype {}
    /** §13.3.3.6 Datum en tijd in millisecondes. */
    record DateTime() implements Datatype {}
    /** §13.3.4.2 Enumeratie domain: the declaring domain's NAME (its identity — Tabel 36/43/61 rij 7
     *  require the SAME enumeration on both sides, not merely any enumeration) plus the declared value set. */
    record Enumeration(String domain, List<String> values) implements Datatype {}
    /** A reference that could not be resolved to a concrete datatype. */
    record Unknown(String reason) implements Datatype {}

    enum Sign { ONBEPAALD, NEGATIEF, NIET_NEGATIEF, POSITIEF }

    // --- predicates used by the typeringen matrix ---
    default boolean isNumber()   { return this instanceof Numeric; }
    default boolean isPercentage() { return this instanceof Percentage; }
    /** numeric in the typeringen sense: a number or a percentage. */
    default boolean isNumberLike() { return isNumber() || isPercentage(); }
    default boolean isText()      { return this instanceof Text; }
    default boolean isBoolean()    { return this instanceof BooleanType; }
    default boolean isDate()      { return this instanceof DateInDays || this instanceof DateTime; }
    default boolean isUnknown()   { return this instanceof Unknown; }
    /** Orderable by a scalar &lt;/&gt; (so it can drive a §9.7.2 "op volgorde van toenemende/afnemende"
     *  ordering): a number/percentage or a date. Tekst, Booleaan and Enumeratie carry only gelijk/ongelijk
     *  in the typeringen matrix (§5.x), no ordinal {@code <}, so they are NOT orderable — a non-orderable
     *  ordering key keeps the verdeling a COMPLEX marker rather than guessing a lexicographic order. */
    default boolean isOrderable()  { return isNumberLike() || isDate(); }

    /** §13.3.3 form (c) "getal": an unbounded numeriek/percentage — any number of decimals, no
     *  precision ceiling. The three getalspecificatie forms are encoded as: (a) "geheel getal"
     *  = geheel∧0; (b) "getal met x decimalen" = ¬geheel∧x≥1; (c) "getal" = ¬geheel∧0. So a
     *  decimalen of 0 is unbounded precisely when the type is NOT a whole number — the geheel
     *  flag is what tells form (c) apart from form (a), both of which carry decimalen 0. */
    default boolean unbounded() {
        if (this instanceof Numeric n)   return !n.wholeNumber() && n.decimals() == 0;
        if (this instanceof Percentage p) return !p.wholeNumber() && p.decimals() == 0;
        return false;
    }

    /** The unit of a Numeriek or a Percentage (always "%", §3.3.5), or null. */
    default Unit unitOf() {
        if (this instanceof Numeric n) return n.unit();
        if (this instanceof Percentage p) return p.unit();
        return null;
    }

    /** A short, human-readable name for diagnostics. */
    default String name() {
        return switch (this) {
            case Numeric n   -> "Numeriek" + (n.hasUnit() ? " (" + n.unit().abbreviation() + ")" : "");
            case Percentage p -> "Percentage" + (p.unit() != null && p.unit().denominator() != null
                                                  ? " (" + p.unit().rendering() + ")" : "");
            case Text t      -> "Tekst";
            case BooleanType b   -> "Boolean";
            case DateInDays d -> "Datum in dagen";
            case DateTime d  -> "Datum-tijd";
            case Enumeration e -> "Enumeratie " + e.domain();
            case Unknown o   -> "onbekend(" + o.reason() + ")";
        };
    }
}
