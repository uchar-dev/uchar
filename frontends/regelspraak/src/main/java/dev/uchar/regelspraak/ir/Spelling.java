package dev.uchar.regelspraak.ir;

import dev.uchar.regelspraak.model.Datatype;
import dev.uchar.regelspraak.model.RegelSpraakModel.Designatable;
import dev.uchar.regelspraak.model.RegelSpraakModel.GeneratedFrom;
import dev.uchar.regelspraak.model.RegelSpraakModel.Granulariteit;
import dev.uchar.regelspraak.model.RegelSpraakModel.OriginForm;
import dev.uchar.regelspraak.model.RegelSpraakModel.Objecttype.CharacteristicKind;
import dev.uchar.regelspraak.model.RegelSpraakModel.Severity;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The wire spelling of every enumeration value, family by family (IR-SPEC, Conventions: English lowerCamel since
 * the L5 landing). The Java records keep their v0 form, so their constants are the v0 wire values the emitter
 * used to write by name; memory and wire now spell differently, and this class is the one place where the two
 * meet. The emitter asks it for the spelling of an in-memory value, and the Drools loader, which compiles these
 * records, asks it for the value a wire spelling names at a place. No other code turns an enum into wire text
 * or wire text into an enum: a second copy of the table would drift from this one and nothing would notice.
 *
 * <p>The families of {@link #FAMILIES}, their places and their rows mirror the section {@code waarden} of
 * {@code spec/proposals/v1-naamtabel.json}, which the migration script's own table is held to as well; a JUnit
 * test reads that section and holds this table equal to it. The two families of the origin field (IR-SPEC
 * ch. 2.13), {@link #ORIGIN_FORM} and {@link #ORIGIN_KIND}, came after the L5 landing and have no v0 value, so that
 * section, which records the renaming from v0, does not list them and they stand outside {@link #FAMILIES}.
 */
public final class Spelling {

    private Spelling() {}

    /**
     * One family: a bijection between in-memory values and their wire spellings. {@link #rows} are the values
     * every place of the family admits. {@link #refused} are spelled like the others but refused by name at the
     * places in {@link #refusedOn} and admitted elsewhere: a reader that meets one must name it, not call it
     * unknown. An in-memory value the family cannot spell stands in {@link #unwritten} with its reason, so the
     * table accounts for every constant of the enum it maps. A place in {@link #nullStays} admits {@code null};
     * a {@link #free} family is a free string on the wire that a check closes, not the schema.
     */
    public static final class Family<K> {
        private final String naam;
        private final List<String> places;
        private final Map<K, String> rows = new LinkedHashMap<>();
        private final Map<K, String> refused = new LinkedHashMap<>();
        private final Set<String> refusedOn = new LinkedHashSet<>();
        private final Set<String> nullStays = new LinkedHashSet<>();
        private boolean free;
        private final Map<K, String> unwritten = new LinkedHashMap<>();
        private final Map<String, K> back = new HashMap<>();

        private Family(String naam, String... places) {
            this.naam = naam;
            this.places = List.of(places);
        }

        private Family<K> row(K value, String spelling) {
            add(rows, value, spelling);
            return this;
        }

        private Family<K> refused(K value, String spelling) {
            add(refused, value, spelling);
            return this;
        }

        private void add(Map<K, String> target, K value, String spelling) {
            if (known(value) || back.containsKey(spelling))
                throw new IllegalStateException("Spelling: the family " + naam + " spells '" + value + "' or '"
                        + spelling + "' twice; a family is a bijection");
            target.put(value, spelling);
            back.put(spelling, value);
        }

        private Family<K> unwritten(K value, String reason) {
            if (known(value))
                throw new IllegalStateException("Spelling: the family " + naam + " both spells and leaves '"
                        + value + "' unspelled");
            unwritten.put(value, reason);
            return this;
        }

        private boolean known(K value) {
            return rows.containsKey(value) || refused.containsKey(value) || unwritten.containsKey(value);
        }

        private Family<K> refusedOn(String... op) {
            for (String p : op) refusedOn.add(ownPlace(p));
            return this;
        }

        private Family<K> nullStays(String... op) {
            for (String p : op) nullStays.add(ownPlace(p));
            return this;
        }

        private Family<K> asFree() {
            free = true;
            return this;
        }

        private String ownPlace(String place) {
            if (!places.contains(place))
                throw new IllegalArgumentException("Spelling: the family " + naam + " is not written at '" + place
                        + "'; its places are " + places);
            return place;
        }

        /** The family's name in the section {@code waarden}. */
        public String naam() { return naam; }

        /** The wire positions that carry the family, as {@code <Node>.<field>}. */
        public List<String> places() { return places; }

        /** In-memory value to spelling, for the values every place admits, in table order. */
        public Map<K, String> rows() { return Collections.unmodifiableMap(rows); }

        /** In-memory value to spelling, for the values the places of {@link #refusedOn()} refuse by name. */
        public Map<K, String> refused() { return Collections.unmodifiableMap(refused); }

        public Set<String> refusedOn() { return Collections.unmodifiableSet(refusedOn); }

        public Set<String> nullStays() { return Collections.unmodifiableSet(nullStays); }

        public boolean free() { return free; }

        /** The in-memory values without a spelling in this family, each with the reason. */
        public Map<K, String> unwritten() { return Collections.unmodifiableMap(unwritten); }

        /** The wire spelling of an in-memory value. Where it stands is the caller's affair, so a value some place
         *  refuses by name is spelled too. A value this family does not spell is a defect of the caller, refused
         *  rather than written by its enum name, which would put a v0 spelling on the wire. */
        public String spelling(K value) {
            String s = rows.containsKey(value) ? rows.get(value) : refused.get(value);
            if (s == null)
                throw new IllegalArgumentException("Spelling: the family " + naam + " has no wire spelling for '"
                        + value + "'" + (unwritten.containsKey(value) ? ": " + unwritten.get(value) : ""));
            return s;
        }

        /** The in-memory value a wire spelling names at {@code place}, or null where that place does not admit
         *  it: refused by name there ({@link #refusesByName}), or not a spelling of this family at all, a v0
         *  value included. Never a default in place of an unknown value; the caller refuses the document. */
        public K value(String place, String spelling) {
            ownPlace(place);
            K k = back.get(spelling);
            if (k == null || refused.containsKey(k) && refusedOn.contains(place)) return null;
            return k;
        }

        /** Whether {@code place} refuses this wire spelling by name: a spelling of the family that stands on a
         *  place which does not admit it. */
        public boolean refusesByName(String place, String spelling) {
            ownPlace(place);
            K k = back.get(spelling);
            return k != null && refused.containsKey(k) && refusedOn.contains(place);
        }

        /** Whether {@code place} admits {@code null}. */
        public boolean allowsNull(String place) {
            return nullStays.contains(ownPlace(place));
        }
    }

    public static final Family<Datatype.Teken> NUMBER_SIGN = new Family<Datatype.Teken>("Number.sign",
            "Number.sign")
            .row(Datatype.Teken.ONBEPAALD, "unspecified")
            .row(Datatype.Teken.NEGATIEF, "negative")
            .row(Datatype.Teken.NIET_NEGATIEF, "nonNegative")
            .row(Datatype.Teken.POSITIEF, "positive");

    public static final Family<CharacteristicKind> CHARACTERISTIC_KIND = new Family<CharacteristicKind>("Characteristic.kind",
            "Characteristic.kind")
            .row(CharacteristicKind.BIJVOEGLIJK, "adjectival")
            .row(CharacteristicKind.BEZITTELIJK, "possessive")
            .row(CharacteristicKind.ZELFSTANDIG, "nominal");

    public static final Family<Granulariteit> GRANULARITY = new Family<Granulariteit>("granularity",
            "TimelineFacet.granularity", "Timeline.granularity", "BuiltinCall.granularity")
            .row(Granulariteit.DAG, "day")
            .row(Granulariteit.WEEK, "week")
            .row(Granulariteit.MAAND, "month")
            .row(Granulariteit.KWARTAAL, "quarter")
            .row(Granulariteit.JAAR, "year")
            .nullStays("Timeline.granularity");

    public static final Family<ResolvedRule.Kind> PRESENTATION = new Family<ResolvedRule.Kind>(
            "RuleVersion.presentation", "RuleVersion.presentation")
            .row(ResolvedRule.Kind.GELIJKSTELLING, "equation")
            .row(ResolvedRule.Kind.BEREKENING, "calculation")
            .unwritten(ResolvedRule.Kind.INITIALISATIE,
                    "an initialisation writes initialisation true and no presentation tag (IR-SPEC ch. 3.1)")
            .unwritten(ResolvedRule.Kind.KENMERKTOEKENNING,
                    "a characteristic attribution writes neither the flag nor a presentation tag (IR-SPEC ch. 3.1)");

    public static final Family<ResolvedAllocation.Method> METHOD = new Family<ResolvedAllocation.Method>(
            "AllocationVersion.method", "AllocationVersion.method")
            .row(ResolvedAllocation.Method.GELIJKE_DELEN, "equalShares")
            .row(ResolvedAllocation.Method.NAAR_RATO, "proportional")
            .row(ResolvedAllocation.Method.OP_VOLGORDE, "ordered")
            .unwritten(ResolvedAllocation.Method.COMPLEX,
                    "the frontend decided none of the three methods, so the allocation has no document (IR-SPEC "
                            + "ch. 3.2) and a warning names it");

    public static final Family<Condition.Operator> COMPARISON_OP = new Family<Condition.Operator>("Comparison.op",
            "Comparison.op")
            .row(Condition.Operator.KLEINER, "less")
            .row(Condition.Operator.KLEINER_OF_GELIJK, "lessOrEqual")
            .row(Condition.Operator.GROTER, "greater")
            .row(Condition.Operator.GROTER_OF_GELIJK, "greaterOrEqual")
            .row(Condition.Operator.GELIJK, "equal")
            .row(Condition.Operator.ONGELIJK, "notEqual")
            .row(Condition.Operator.EERDER, "earlier")
            .row(Condition.Operator.EERDER_OF_GELIJK, "earlierOrEqual")
            .row(Condition.Operator.LATER, "later")
            .row(Condition.Operator.LATER_OF_GELIJK, "laterOrEqual");

    public static final Family<Expr.BinOp> BINARY_OP = new Family<Expr.BinOp>("Binary.op", "Binary.op")
            .row(Expr.BinOp.PLUS, "plus")
            .row(Expr.BinOp.MIN, "minus")
            .row(Expr.BinOp.VERMINDERD_MET, "reducedBy")
            .row(Expr.BinOp.MAAL, "times")
            .row(Expr.BinOp.GEDEELD_DOOR, "dividedBy")
            .row(Expr.BinOp.GEDEELD_DOOR_ABS, "dividedByAbs")
            .row(Expr.BinOp.TOT_DE_MACHT, "power")
            .row(Expr.BinOp.PERCENTAGE_VAN, "percentageOf");

    public static final Family<Expr.RoundingMode> ROUNDING_MODE = new Family<Expr.RoundingMode>("Rounding.mode",
            "Rounding.mode")
            .row(Expr.RoundingMode.NAAR_BENEDEN, "floor")
            .row(Expr.RoundingMode.NAAR_BOVEN, "ceiling")
            .row(Expr.RoundingMode.RICHTING_NUL, "towardZero")
            .row(Expr.RoundingMode.WEG_VAN_NUL, "awayFromZero")
            .row(Expr.RoundingMode.REKENKUNDIG, "halfAwayFromZero");

    /** The eight fold kinds of IR-SPEC ch. 4.3; every other function kind is a {@link #BUILTIN_KIND}, and the two
     *  families split the one enum between them. */
    public static final Family<Expr.FunctionKind> AGGREGATION_KIND = new Family<Expr.FunctionKind>(
            "Aggregation.kind", "Aggregation.kind")
            .row(Expr.FunctionKind.SOM, "sum")
            .row(Expr.FunctionKind.TOTAAL, "total")
            .row(Expr.FunctionKind.AANTAL, "count")
            .row(Expr.FunctionKind.MINIMUM, "minimum")
            .row(Expr.FunctionKind.MAXIMUM, "maximum")
            .row(Expr.FunctionKind.EERSTE, "first")
            .row(Expr.FunctionKind.LAATSTE, "last")
            .row(Expr.FunctionKind.AANTAL_GEVULDE, "filledCount");

    public static final Family<Expr.FunctionKind> BUILTIN_KIND = new Family<Expr.FunctionKind>("BuiltinCall.kind",
            "BuiltinCall.kind")
            .row(Expr.FunctionKind.WORTEL, "squareRoot")
            .row(Expr.FunctionKind.ABSOLUTE_WAARDE, "absoluteValue")
            .row(Expr.FunctionKind.CONCATENATIE, "concatenation")
            .row(Expr.FunctionKind.TIJDSDUUR, "duration")
            .row(Expr.FunctionKind.ABSOLUTE_TIJDSDUUR, "absoluteDuration")
            .row(Expr.FunctionKind.EERSTE_PAASDAG, "easterSunday")
            .row(Expr.FunctionKind.TIJDSEVENREDIG_DEEL, "timeProportionalPart")
            .row(Expr.FunctionKind.AANTAL_DAGEN_IN, "dayCount")
            .row(Expr.FunctionKind.JAAR_UIT, "yearOf")
            .row(Expr.FunctionKind.MAAND_UIT, "monthOf")
            .row(Expr.FunctionKind.DAG_UIT, "dayOf")
            .row(Expr.FunctionKind.UUR_UIT, "hourOf")
            .row(Expr.FunctionKind.MINUUT_UIT, "minuteOf")
            .row(Expr.FunctionKind.SECONDE_UIT, "secondOf")
            .row(Expr.FunctionKind.MILLISECONDE_UIT, "millisecondOf")
            .row(Expr.FunctionKind.DATUM_MET, "dateWith")
            .row(Expr.FunctionKind.DATUMTIJD_MET, "dateTimeWith");

    static {
        // the split of the one function-kind enum: each kind is spelled by exactly one of the two nodes
        for (Expr.FunctionKind k : Expr.FunctionKind.values()) {
            if (AGGREGATION_KIND.rows.containsKey(k)) BUILTIN_KIND.unwritten(k, "a fold kind is an Aggregation");
            else AGGREGATION_KIND.unwritten(k, "a kind outside the eight folds is a BuiltinCall");
        }
    }

    public static final Family<Condition.PredicateKind> PREDICATE_KIND = new Family<Condition.PredicateKind>(
            "Predicate.kind", "Predicate.kind")
            .row(Condition.PredicateKind.LEEG, "empty")
            .row(Condition.PredicateKind.GEVULD, "filled")
            .row(Condition.PredicateKind.ELFPROEF, "elevenTest")
            .row(Condition.PredicateKind.NUMERIEK_MET_EXACT, "exactDigits")
            .row(Condition.PredicateKind.DAGSOORT, "dayKind");

    /** One family on two nodes: {@code the} is a quantifier of a compound condition, and a quantifier fold
     *  refuses it by name (WF-108 (a)). */
    public static final Family<Condition.Quantifier> QUANTIFIER = new Family<Condition.Quantifier>("quantifier",
            "Compound.quantifier", "QuantifierFold.quantifier")
            .row(Condition.Quantifier.ALLE, "all")
            .row(Condition.Quantifier.GEEN, "none")
            .row(Condition.Quantifier.TEN_MINSTE, "atLeast")
            .row(Condition.Quantifier.TEN_HOOGSTE, "atMost")
            .row(Condition.Quantifier.PRECIES, "exactly")
            .refused(Condition.Quantifier.DE, "the")
            .refusedOn("QuantifierFold.quantifier");

    /** The two session values by their in-memory names. The wire field is a free string that WF-098 closes byte
     *  for byte, so a name outside the table is no spelling of this family. */
    public static final Family<String> CONTEXT_VALUE_NAME = new Family<String>("ContextValue.name",
            "ContextValue.name")
            .row("Rekendatum", "evaluationDate")
            .row("Rekenjaar", "evaluationYear")
            .asFree();

    /** The whole-period qualifier: the record's {@code jaar} flag (true: the whole year, false: the whole
     *  month). */
    public static final Family<Boolean> WHOLE_PERIOD_GRANULARITY = new Family<Boolean>("WholePeriod.granularity",
            "WholePeriod.granularity")
            .row(true, "year")
            .row(false, "month");

    /** One wire severity; {@code error} and {@code info} are refused by name (WF-113 (a)), and a null severity
     *  stays a null that the same rule refuses. */
    public static final Family<Severity> SEVERITY = new Family<Severity>("Diagnostic.severity",
            "Diagnostic.severity")
            .row(Severity.WARNING, "warning")
            .refused(Severity.ERROR, "error")
            .refused(Severity.INFO, "info")
            .refusedOn("Diagnostic.severity")
            .nullStays("Diagnostic.severity");

    /** The precision of a date: the datatype variant itself is the in-memory value (both records are
     *  component-free, so every instance of one is equal to every other). */
    public static final Family<Datatype> DATE_PRECISION = new Family<Datatype>("Date.precision", "Date.precision")
            .row(new Datatype.DatumInDagen(), "day")
            .row(new Datatype.DateTime(), "millisecond");

    /** The form of an origin (IR-SPEC ch. 2.13): the five forms by which the frontend makes a declaration of its own
     *  ({@code RegelSpraakModel.generated}). The Drools backend's two keys name entries of its coverage manifest,
     *  which are no declarations of the document and so never carry an origin. */
    public static final Family<OriginForm> ORIGIN_FORM = new Family<OriginForm>("Origin.form", "Origin.form")
            .row(OriginForm.OPGAVE_BOVEN_BEREKENING, "inputAboveComputation")
            .row(OriginForm.EXCEPTION_FOR_SUBTYPE, "exceptionForSubtype")
            .row(OriginForm.BESLISTABELRIJ, "decisionTableRow")
            .row(OriginForm.STEPS_AFTER_SET, "stepsAfterSet")
            .row(OriginForm.START_POINT, "startPoint")
            .unwritten(OriginForm.DEFAULTS_STACK, "a name of the Drools backend's coverage manifest, no declaration")
            .unwritten(OriginForm.VERSION, "a name of the Drools backend's coverage manifest, no declaration");

    /** The kind of the declaration an origin comes from (IR-SPEC ch. 2.13). The wire has a tenth kind,
     *  {@link #ORIGIN_DECISION_TABLE}, which is no kind of declaration: see {@link #originKind}. */
    public static final Family<Designatable.Kind> ORIGIN_KIND = new Family<Designatable.Kind>("OriginSource.kind",
            "OriginSource.kind")
            .row(Designatable.Kind.OBJECTTYPE, "objectType")
            .row(Designatable.Kind.ATTRIBUUT, "attribute")
            .row(Designatable.Kind.KENMERK, "characteristic")
            .row(Designatable.Kind.FEITTYPE, "factType")
            .row(Designatable.Kind.ROL, "role")
            .row(Designatable.Kind.ENUMERATIEWAARDE, "enumerationValue")
            .row(Designatable.Kind.PARAMETER, "parameter")
            .row(Designatable.Kind.VORM, "namedForm")
            .row(Designatable.Kind.REGELGROEP, "ruleGroup");

    /** The wire kind of a decision table, the source of each of its rows. */
    public static final String ORIGIN_DECISION_TABLE = "decisionTable";

    /** The wire kind of the source of {@code uit}. The model records a decision table as a form, because it is no
     *  element of the model (its rows are) and the table has no kind of its own there; on the wire it is
     *  {@link #ORIGIN_DECISION_TABLE}, so that a row's {@code from} never names a form, which would be a form that does
     *  not exist or another one under the table's name. Every other source is its kind's spelling. */
    public static String originKind(GeneratedFrom generatedFrom) {
        if (generatedFrom.form() == OriginForm.BESLISTABELRIJ) {
            if (generatedFrom.source().category() != Designatable.Kind.VORM)
                throw new IllegalArgumentException("Spelling: a decision table row comes from a table, recorded as a "
                        + "form, and this one comes from " + generatedFrom.source());
            return ORIGIN_DECISION_TABLE;
        }
        return ORIGIN_KIND.spelling(generatedFrom.source().category());
    }

    /** The in-memory kind a wire kind names at {@code OriginSource.kind}: a decision table is a form, as the model
     *  records it ({@link #originKind}); null for a spelling that is no kind, which the caller refuses. */
    public static Designatable.Kind originKindFromWire(String kind) {
        if (ORIGIN_DECISION_TABLE.equals(kind)) return Designatable.Kind.VORM;
        return ORIGIN_KIND.value("OriginSource.kind", kind);
    }

    /** Every family, in the order of the section {@code waarden}. */
    public static final List<Family<?>> FAMILIES = List.of(NUMBER_SIGN, CHARACTERISTIC_KIND, GRANULARITY,
            PRESENTATION, METHOD, COMPARISON_OP, BINARY_OP, ROUNDING_MODE, AGGREGATION_KIND, BUILTIN_KIND,
            PREDICATE_KIND, QUANTIFIER, CONTEXT_VALUE_NAME, WHOLE_PERIOD_GRANULARITY, SEVERITY, DATE_PRECISION);
}
