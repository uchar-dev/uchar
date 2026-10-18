package dev.uchar.regelspraak.ir;

import dev.uchar.regelspraak.model.Datatype;
import dev.uchar.regelspraak.model.RegelSpraakModel.Benoembaar;
import dev.uchar.regelspraak.model.RegelSpraakModel.GegenereerdUit;
import dev.uchar.regelspraak.model.RegelSpraakModel.Granulariteit;
import dev.uchar.regelspraak.model.RegelSpraakModel.Herkomstvorm;
import dev.uchar.regelspraak.model.RegelSpraakModel.Objecttype.KenmerkSoort;
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
     * every place of the family admits. {@link #geweigerd} are spelled like the others but refused by name at the
     * places in {@link #refusedOn} and admitted elsewhere: a reader that meets one must name it, not call it
     * unknown. An in-memory value the family cannot spell stands in {@link #unwritten} with its reason, so the
     * table accounts for every constant of the enum it maps. A place in {@link #nullStays} admits {@code null};
     * a {@link #free} family is a free string on the wire that a check closes, not the schema.
     */
    public static final class Family<K> {
        private final String naam;
        private final List<String> places;
        private final Map<K, String> rows = new LinkedHashMap<>();
        private final Map<K, String> geweigerd = new LinkedHashMap<>();
        private final Set<String> refusedOn = new LinkedHashSet<>();
        private final Set<String> nullStays = new LinkedHashSet<>();
        private boolean free;
        private final Map<K, String> unwritten = new LinkedHashMap<>();
        private final Map<String, K> back = new HashMap<>();

        private Family(String naam, String... places) {
            this.naam = naam;
            this.places = List.of(places);
        }

        private Family<K> row(K waarde, String spelling) {
            add(rows, waarde, spelling);
            return this;
        }

        private Family<K> geweigerd(K waarde, String spelling) {
            add(geweigerd, waarde, spelling);
            return this;
        }

        private void add(Map<K, String> doel, K waarde, String spelling) {
            if (known(waarde) || back.containsKey(spelling))
                throw new IllegalStateException("Spelling: the family " + naam + " spells '" + waarde + "' or '"
                        + spelling + "' twice; a family is a bijection");
            doel.put(waarde, spelling);
            back.put(spelling, waarde);
        }

        private Family<K> unwritten(K waarde, String reason) {
            if (known(waarde))
                throw new IllegalStateException("Spelling: the family " + naam + " both spells and leaves '"
                        + waarde + "' unspelled");
            unwritten.put(waarde, reason);
            return this;
        }

        private boolean known(K waarde) {
            return rows.containsKey(waarde) || geweigerd.containsKey(waarde) || unwritten.containsKey(waarde);
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
        public Map<K, String> geweigerd() { return Collections.unmodifiableMap(geweigerd); }

        public Set<String> refusedOn() { return Collections.unmodifiableSet(refusedOn); }

        public Set<String> nullStays() { return Collections.unmodifiableSet(nullStays); }

        public boolean free() { return free; }

        /** The in-memory values without a spelling in this family, each with the reason. */
        public Map<K, String> unwritten() { return Collections.unmodifiableMap(unwritten); }

        /** The wire spelling of an in-memory value. Where it stands is the caller's affair, so a value some place
         *  refuses by name is spelled too. A value this family does not spell is a defect of the caller, refused
         *  rather than written by its enum name, which would put a v0 spelling on the wire. */
        public String spelling(K waarde) {
            String s = rows.containsKey(waarde) ? rows.get(waarde) : geweigerd.get(waarde);
            if (s == null)
                throw new IllegalArgumentException("Spelling: the family " + naam + " has no wire spelling for '"
                        + waarde + "'" + (unwritten.containsKey(waarde) ? ": " + unwritten.get(waarde) : ""));
            return s;
        }

        /** The in-memory value a wire spelling names at {@code place}, or null where that place does not admit
         *  it: refused by name there ({@link #refusesByName}), or not a spelling of this family at all, a v0
         *  value included. Never a default in place of an unknown value; the caller refuses the document. */
        public K waarde(String place, String spelling) {
            ownPlace(place);
            K k = back.get(spelling);
            if (k == null || geweigerd.containsKey(k) && refusedOn.contains(place)) return null;
            return k;
        }

        /** Whether {@code place} refuses this wire spelling by name: a spelling of the family that stands on a
         *  place which does not admit it. */
        public boolean refusesByName(String place, String spelling) {
            ownPlace(place);
            K k = back.get(spelling);
            return k != null && geweigerd.containsKey(k) && refusedOn.contains(place);
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

    public static final Family<KenmerkSoort> CHARACTERISTIC_KIND = new Family<KenmerkSoort>("Characteristic.kind",
            "Characteristic.kind")
            .row(KenmerkSoort.BIJVOEGLIJK, "adjectival")
            .row(KenmerkSoort.BEZITTELIJK, "possessive")
            .row(KenmerkSoort.ZELFSTANDIG, "nominal");

    public static final Family<Granulariteit> GRANULARITY = new Family<Granulariteit>("granularity",
            "TimelineFacet.granularity", "Timeline.granularity", "BuiltinCall.granularity")
            .row(Granulariteit.DAG, "day")
            .row(Granulariteit.WEEK, "week")
            .row(Granulariteit.MAAND, "month")
            .row(Granulariteit.KWARTAAL, "quarter")
            .row(Granulariteit.JAAR, "year")
            .nullStays("Timeline.granularity");

    public static final Family<ResolvedRegel.Soort> PRESENTATION = new Family<ResolvedRegel.Soort>(
            "RuleVersion.presentation", "RuleVersion.presentation")
            .row(ResolvedRegel.Soort.GELIJKSTELLING, "equation")
            .row(ResolvedRegel.Soort.BEREKENING, "calculation")
            .unwritten(ResolvedRegel.Soort.INITIALISATIE,
                    "an initialisation writes initialisation true and no presentation tag (IR-SPEC ch. 3.1)")
            .unwritten(ResolvedRegel.Soort.KENMERKTOEKENNING,
                    "a characteristic attribution writes neither the flag nor a presentation tag (IR-SPEC ch. 3.1)");

    public static final Family<ResolvedVerdeling.Methode> METHOD = new Family<ResolvedVerdeling.Methode>(
            "AllocationVersion.method", "AllocationVersion.method")
            .row(ResolvedVerdeling.Methode.GELIJKE_DELEN, "equalShares")
            .row(ResolvedVerdeling.Methode.NAAR_RATO, "proportional")
            .row(ResolvedVerdeling.Methode.OP_VOLGORDE, "ordered")
            .unwritten(ResolvedVerdeling.Methode.COMPLEX,
                    "the frontend decided none of the three methods, so the allocation has no document (IR-SPEC "
                            + "ch. 3.2) and a warning names it");

    public static final Family<Conditie.Operator> COMPARISON_OP = new Family<Conditie.Operator>("Comparison.op",
            "Comparison.op")
            .row(Conditie.Operator.KLEINER, "less")
            .row(Conditie.Operator.KLEINER_OF_GELIJK, "lessOrEqual")
            .row(Conditie.Operator.GROTER, "greater")
            .row(Conditie.Operator.GROTER_OF_GELIJK, "greaterOrEqual")
            .row(Conditie.Operator.GELIJK, "equal")
            .row(Conditie.Operator.ONGELIJK, "notEqual")
            .row(Conditie.Operator.EERDER, "earlier")
            .row(Conditie.Operator.EERDER_OF_GELIJK, "earlierOrEqual")
            .row(Conditie.Operator.LATER, "later")
            .row(Conditie.Operator.LATER_OF_GELIJK, "laterOrEqual");

    public static final Family<Expr.BinOp> BINARY_OP = new Family<Expr.BinOp>("Binary.op", "Binary.op")
            .row(Expr.BinOp.PLUS, "plus")
            .row(Expr.BinOp.MIN, "minus")
            .row(Expr.BinOp.VERMINDERD_MET, "reducedBy")
            .row(Expr.BinOp.MAAL, "times")
            .row(Expr.BinOp.GEDEELD_DOOR, "dividedBy")
            .row(Expr.BinOp.GEDEELD_DOOR_ABS, "dividedByAbs")
            .row(Expr.BinOp.TOT_DE_MACHT, "power")
            .row(Expr.BinOp.PERCENTAGE_VAN, "percentageOf");

    public static final Family<Expr.AfrondModus> ROUNDING_MODE = new Family<Expr.AfrondModus>("Rounding.mode",
            "Rounding.mode")
            .row(Expr.AfrondModus.NAAR_BENEDEN, "floor")
            .row(Expr.AfrondModus.NAAR_BOVEN, "ceiling")
            .row(Expr.AfrondModus.RICHTING_NUL, "towardZero")
            .row(Expr.AfrondModus.WEG_VAN_NUL, "awayFromZero")
            .row(Expr.AfrondModus.REKENKUNDIG, "halfAwayFromZero");

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

    public static final Family<Conditie.PredicateKind> PREDICATE_KIND = new Family<Conditie.PredicateKind>(
            "Predicate.kind", "Predicate.kind")
            .row(Conditie.PredicateKind.LEEG, "empty")
            .row(Conditie.PredicateKind.GEVULD, "filled")
            .row(Conditie.PredicateKind.ELFPROEF, "elevenTest")
            .row(Conditie.PredicateKind.NUMERIEK_MET_EXACT, "exactDigits")
            .row(Conditie.PredicateKind.DAGSOORT, "dayKind");

    /** One family on two nodes: {@code the} is a quantifier of a compound condition, and a quantifier fold
     *  refuses it by name (WF-108 (a)). */
    public static final Family<Conditie.Kwantor> QUANTIFIER = new Family<Conditie.Kwantor>("quantifier",
            "Compound.quantifier", "QuantifierFold.quantifier")
            .row(Conditie.Kwantor.ALLE, "all")
            .row(Conditie.Kwantor.GEEN, "none")
            .row(Conditie.Kwantor.TEN_MINSTE, "atLeast")
            .row(Conditie.Kwantor.TEN_HOOGSTE, "atMost")
            .row(Conditie.Kwantor.PRECIES, "exactly")
            .geweigerd(Conditie.Kwantor.DE, "the")
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
            .geweigerd(Severity.ERROR, "error")
            .geweigerd(Severity.INFO, "info")
            .refusedOn("Diagnostic.severity")
            .nullStays("Diagnostic.severity");

    /** The precision of a date: the datatype variant itself is the in-memory value (both records are
     *  component-free, so every instance of one is equal to every other). */
    public static final Family<Datatype> DATE_PRECISION = new Family<Datatype>("Date.precision", "Date.precision")
            .row(new Datatype.DatumInDagen(), "day")
            .row(new Datatype.DatumTijd(), "millisecond");

    /** The form of an origin (IR-SPEC ch. 2.13): the five forms by which the frontend makes a declaration of its own
     *  ({@code RegelSpraakModel.gegenereerd}). The Drools backend's two keys name entries of its coverage manifest,
     *  which are no declarations of the document and so never carry an origin. */
    public static final Family<Herkomstvorm> ORIGIN_FORM = new Family<Herkomstvorm>("Origin.form", "Origin.form")
            .row(Herkomstvorm.OPGAVE_BOVEN_BEREKENING, "inputAboveComputation")
            .row(Herkomstvorm.EXCEPTION_FOR_SUBTYPE, "exceptionForSubtype")
            .row(Herkomstvorm.BESLISTABELRIJ, "decisionTableRow")
            .row(Herkomstvorm.STEPS_AFTER_SET, "stepsAfterSet")
            .row(Herkomstvorm.START_POINT, "startPoint")
            .unwritten(Herkomstvorm.DEFAULTS_STACK, "a name of the Drools backend's coverage manifest, no declaration")
            .unwritten(Herkomstvorm.VERSION, "a name of the Drools backend's coverage manifest, no declaration");

    /** The kind of the declaration an origin comes from (IR-SPEC ch. 2.13). The wire has a tenth kind,
     *  {@link #ORIGIN_DECISION_TABLE}, which is no kind of declaration: see {@link #originKind}. */
    public static final Family<Benoembaar.Soort> ORIGIN_KIND = new Family<Benoembaar.Soort>("OriginSource.kind",
            "OriginSource.kind")
            .row(Benoembaar.Soort.OBJECTTYPE, "objectType")
            .row(Benoembaar.Soort.ATTRIBUUT, "attribute")
            .row(Benoembaar.Soort.KENMERK, "characteristic")
            .row(Benoembaar.Soort.FEITTYPE, "factType")
            .row(Benoembaar.Soort.ROL, "role")
            .row(Benoembaar.Soort.ENUMERATIEWAARDE, "enumerationValue")
            .row(Benoembaar.Soort.PARAMETER, "parameter")
            .row(Benoembaar.Soort.VORM, "namedForm")
            .row(Benoembaar.Soort.REGELGROEP, "ruleGroup");

    /** The wire kind of a decision table, the source of each of its rows. */
    public static final String ORIGIN_DECISION_TABLE = "decisionTable";

    /** The wire kind of the source of {@code uit}. The model records a decision table as a form, because it is no
     *  element of the model (its rows are) and the table has no kind of its own there; on the wire it is
     *  {@link #ORIGIN_DECISION_TABLE}, so that a row's {@code from} never names a form, which would be a form that does
     *  not exist or another one under the table's name. Every other source is its kind's spelling. */
    public static String originKind(GegenereerdUit generatedFrom) {
        if (generatedFrom.vorm() == Herkomstvorm.BESLISTABELRIJ) {
            if (generatedFrom.bron().soort() != Benoembaar.Soort.VORM)
                throw new IllegalArgumentException("Spelling: a decision table row comes from a table, recorded as a "
                        + "form, and this one comes from " + generatedFrom.bron());
            return ORIGIN_DECISION_TABLE;
        }
        return ORIGIN_KIND.spelling(generatedFrom.bron().soort());
    }

    /** The in-memory kind a wire kind names at {@code OriginSource.kind}: a decision table is a form, as the model
     *  records it ({@link #originKind}); null for a spelling that is no kind, which the caller refuses. */
    public static Benoembaar.Soort originKindFromWire(String kind) {
        if (ORIGIN_DECISION_TABLE.equals(kind)) return Benoembaar.Soort.VORM;
        return ORIGIN_KIND.waarde("OriginSource.kind", kind);
    }

    /** Every family, in the order of the section {@code waarden}. */
    public static final List<Family<?>> FAMILIES = List.of(NUMBER_SIGN, CHARACTERISTIC_KIND, GRANULARITY,
            PRESENTATION, METHOD, COMPARISON_OP, BINARY_OP, ROUNDING_MODE, AGGREGATION_KIND, BUILTIN_KIND,
            PREDICATE_KIND, QUANTIFIER, CONTEXT_VALUE_NAME, WHOLE_PERIOD_GRANULARITY, SEVERITY, DATE_PRECISION);
}
