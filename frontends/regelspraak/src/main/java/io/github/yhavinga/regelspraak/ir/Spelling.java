package io.github.yhavinga.regelspraak.ir;

import io.github.yhavinga.regelspraak.model.Datatype;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Granulariteit;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Objecttype.KenmerkSoort;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Severity;

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
 * <p>The families, their places and their rows mirror the section {@code waarden} of
 * {@code spec/proposals/v1-naamtabel.json}, which the migration script's own table is held to as well; a JUnit
 * test reads that section and holds this table equal to it.
 */
public final class Spelling {

    private Spelling() {}

    /**
     * One family: a bijection between in-memory values and their wire spellings. {@link #rijen} are the values
     * every place of the family admits. {@link #geweigerd} are spelled like the others but refused by name at the
     * places in {@link #geweigerdOp} and admitted elsewhere: a reader that meets one must name it, not call it
     * unknown. An in-memory value the family cannot spell stands in {@link #ongeschreven} with its reason, so the
     * table accounts for every constant of the enum it maps. A place in {@link #nullBlijft} admits {@code null};
     * a {@link #vrij} family is a free string on the wire that a check closes, not the schema.
     */
    public static final class Familie<K> {
        private final String naam;
        private final List<String> plaatsen;
        private final Map<K, String> rijen = new LinkedHashMap<>();
        private final Map<K, String> geweigerd = new LinkedHashMap<>();
        private final Set<String> geweigerdOp = new LinkedHashSet<>();
        private final Set<String> nullBlijft = new LinkedHashSet<>();
        private boolean vrij;
        private final Map<K, String> ongeschreven = new LinkedHashMap<>();
        private final Map<String, K> terug = new HashMap<>();

        private Familie(String naam, String... plaatsen) {
            this.naam = naam;
            this.plaatsen = List.of(plaatsen);
        }

        private Familie<K> rij(K waarde, String spelling) {
            voegToe(rijen, waarde, spelling);
            return this;
        }

        private Familie<K> geweigerd(K waarde, String spelling) {
            voegToe(geweigerd, waarde, spelling);
            return this;
        }

        private void voegToe(Map<K, String> doel, K waarde, String spelling) {
            if (bekend(waarde) || terug.containsKey(spelling))
                throw new IllegalStateException("Spelling: the family " + naam + " spells '" + waarde + "' or '"
                        + spelling + "' twice; a family is a bijection");
            doel.put(waarde, spelling);
            terug.put(spelling, waarde);
        }

        private Familie<K> ongeschreven(K waarde, String reden) {
            if (bekend(waarde))
                throw new IllegalStateException("Spelling: the family " + naam + " both spells and leaves '"
                        + waarde + "' unspelled");
            ongeschreven.put(waarde, reden);
            return this;
        }

        private boolean bekend(K waarde) {
            return rijen.containsKey(waarde) || geweigerd.containsKey(waarde) || ongeschreven.containsKey(waarde);
        }

        private Familie<K> geweigerdOp(String... op) {
            for (String p : op) geweigerdOp.add(eigenPlaats(p));
            return this;
        }

        private Familie<K> nullBlijft(String... op) {
            for (String p : op) nullBlijft.add(eigenPlaats(p));
            return this;
        }

        private Familie<K> alsVrij() {
            vrij = true;
            return this;
        }

        private String eigenPlaats(String plaats) {
            if (!plaatsen.contains(plaats))
                throw new IllegalArgumentException("Spelling: the family " + naam + " is not written at '" + plaats
                        + "'; its places are " + plaatsen);
            return plaats;
        }

        /** The family's name in the section {@code waarden}. */
        public String naam() { return naam; }

        /** The wire positions that carry the family, as {@code <Node>.<field>}. */
        public List<String> plaatsen() { return plaatsen; }

        /** In-memory value to spelling, for the values every place admits, in table order. */
        public Map<K, String> rijen() { return Collections.unmodifiableMap(rijen); }

        /** In-memory value to spelling, for the values the places of {@link #geweigerdOp()} refuse by name. */
        public Map<K, String> geweigerd() { return Collections.unmodifiableMap(geweigerd); }

        public Set<String> geweigerdOp() { return Collections.unmodifiableSet(geweigerdOp); }

        public Set<String> nullBlijft() { return Collections.unmodifiableSet(nullBlijft); }

        public boolean vrij() { return vrij; }

        /** The in-memory values without a spelling in this family, each with the reason. */
        public Map<K, String> ongeschreven() { return Collections.unmodifiableMap(ongeschreven); }

        /** The wire spelling of an in-memory value. Where it stands is the caller's affair, so a value some place
         *  refuses by name is spelled too. A value this family does not spell is a defect of the caller, refused
         *  rather than written by its enum name, which would put a v0 spelling on the wire. */
        public String spelling(K waarde) {
            String s = rijen.containsKey(waarde) ? rijen.get(waarde) : geweigerd.get(waarde);
            if (s == null)
                throw new IllegalArgumentException("Spelling: the family " + naam + " has no wire spelling for '"
                        + waarde + "'" + (ongeschreven.containsKey(waarde) ? ": " + ongeschreven.get(waarde) : ""));
            return s;
        }

        /** The in-memory value a wire spelling names at {@code plaats}, or null where that place does not admit
         *  it: refused by name there ({@link #weigertBijNaam}), or not a spelling of this family at all, a v0
         *  value included. Never a default in place of an unknown value; the caller refuses the document. */
        public K waarde(String plaats, String spelling) {
            eigenPlaats(plaats);
            K k = terug.get(spelling);
            if (k == null || geweigerd.containsKey(k) && geweigerdOp.contains(plaats)) return null;
            return k;
        }

        /** Whether {@code plaats} refuses this wire spelling by name: a spelling of the family that stands on a
         *  place which does not admit it. */
        public boolean weigertBijNaam(String plaats, String spelling) {
            eigenPlaats(plaats);
            K k = terug.get(spelling);
            return k != null && geweigerd.containsKey(k) && geweigerdOp.contains(plaats);
        }

        /** Whether {@code plaats} admits {@code null}. */
        public boolean laatNullToe(String plaats) {
            return nullBlijft.contains(eigenPlaats(plaats));
        }
    }

    public static final Familie<Datatype.Teken> NUMBER_SIGN = new Familie<Datatype.Teken>("Number.sign",
            "Number.sign")
            .rij(Datatype.Teken.ONBEPAALD, "unspecified")
            .rij(Datatype.Teken.NEGATIEF, "negative")
            .rij(Datatype.Teken.NIET_NEGATIEF, "nonNegative")
            .rij(Datatype.Teken.POSITIEF, "positive");

    public static final Familie<KenmerkSoort> CHARACTERISTIC_KIND = new Familie<KenmerkSoort>("Characteristic.kind",
            "Characteristic.kind")
            .rij(KenmerkSoort.BIJVOEGLIJK, "adjectival")
            .rij(KenmerkSoort.BEZITTELIJK, "possessive")
            .rij(KenmerkSoort.ZELFSTANDIG, "nominal");

    public static final Familie<Granulariteit> GRANULARITY = new Familie<Granulariteit>("granularity",
            "TimelineFacet.granularity", "Timeline.granularity", "BuiltinCall.granularity")
            .rij(Granulariteit.DAG, "day")
            .rij(Granulariteit.WEEK, "week")
            .rij(Granulariteit.MAAND, "month")
            .rij(Granulariteit.KWARTAAL, "quarter")
            .rij(Granulariteit.JAAR, "year")
            .nullBlijft("Timeline.granularity");

    public static final Familie<ResolvedRegel.Soort> PRESENTATION = new Familie<ResolvedRegel.Soort>(
            "RuleVersion.presentation", "RuleVersion.presentation")
            .rij(ResolvedRegel.Soort.GELIJKSTELLING, "equation")
            .rij(ResolvedRegel.Soort.BEREKENING, "calculation")
            .ongeschreven(ResolvedRegel.Soort.INITIALISATIE,
                    "an initialisation writes initialisation true and no presentation tag (IR-SPEC ch. 3.1)")
            .ongeschreven(ResolvedRegel.Soort.KENMERKTOEKENNING,
                    "a characteristic attribution writes neither the flag nor a presentation tag (IR-SPEC ch. 3.1)");

    public static final Familie<ResolvedVerdeling.Methode> METHOD = new Familie<ResolvedVerdeling.Methode>(
            "AllocationVersion.method", "AllocationVersion.method")
            .rij(ResolvedVerdeling.Methode.GELIJKE_DELEN, "equalShares")
            .rij(ResolvedVerdeling.Methode.NAAR_RATO, "proportional")
            .rij(ResolvedVerdeling.Methode.OP_VOLGORDE, "ordered")
            .ongeschreven(ResolvedVerdeling.Methode.COMPLEX,
                    "the frontend decided none of the three methods, so the allocation has no document (IR-SPEC "
                            + "ch. 3.2) and a warning names it");

    public static final Familie<Conditie.Operator> COMPARISON_OP = new Familie<Conditie.Operator>("Comparison.op",
            "Comparison.op")
            .rij(Conditie.Operator.KLEINER, "less")
            .rij(Conditie.Operator.KLEINER_OF_GELIJK, "lessOrEqual")
            .rij(Conditie.Operator.GROTER, "greater")
            .rij(Conditie.Operator.GROTER_OF_GELIJK, "greaterOrEqual")
            .rij(Conditie.Operator.GELIJK, "equal")
            .rij(Conditie.Operator.ONGELIJK, "notEqual")
            .rij(Conditie.Operator.EERDER, "earlier")
            .rij(Conditie.Operator.EERDER_OF_GELIJK, "earlierOrEqual")
            .rij(Conditie.Operator.LATER, "later")
            .rij(Conditie.Operator.LATER_OF_GELIJK, "laterOrEqual");

    public static final Familie<Expr.BinOp> BINARY_OP = new Familie<Expr.BinOp>("Binary.op", "Binary.op")
            .rij(Expr.BinOp.PLUS, "plus")
            .rij(Expr.BinOp.MIN, "minus")
            .rij(Expr.BinOp.VERMINDERD_MET, "reducedBy")
            .rij(Expr.BinOp.MAAL, "times")
            .rij(Expr.BinOp.GEDEELD_DOOR, "dividedBy")
            .rij(Expr.BinOp.GEDEELD_DOOR_ABS, "dividedByAbs")
            .rij(Expr.BinOp.TOT_DE_MACHT, "power")
            .rij(Expr.BinOp.PERCENTAGE_VAN, "percentageOf");

    public static final Familie<Expr.AfrondModus> ROUNDING_MODE = new Familie<Expr.AfrondModus>("Rounding.mode",
            "Rounding.mode")
            .rij(Expr.AfrondModus.NAAR_BENEDEN, "floor")
            .rij(Expr.AfrondModus.NAAR_BOVEN, "ceiling")
            .rij(Expr.AfrondModus.RICHTING_NUL, "towardZero")
            .rij(Expr.AfrondModus.WEG_VAN_NUL, "awayFromZero")
            .rij(Expr.AfrondModus.REKENKUNDIG, "halfAwayFromZero");

    /** The eight fold kinds of IR-SPEC ch. 4.3; every other function kind is a {@link #BUILTIN_KIND}, and the two
     *  families split the one enum between them. */
    public static final Familie<Expr.FunctieKind> AGGREGATION_KIND = new Familie<Expr.FunctieKind>(
            "Aggregation.kind", "Aggregation.kind")
            .rij(Expr.FunctieKind.SOM, "sum")
            .rij(Expr.FunctieKind.TOTAAL, "total")
            .rij(Expr.FunctieKind.AANTAL, "count")
            .rij(Expr.FunctieKind.MINIMUM, "minimum")
            .rij(Expr.FunctieKind.MAXIMUM, "maximum")
            .rij(Expr.FunctieKind.EERSTE, "first")
            .rij(Expr.FunctieKind.LAATSTE, "last")
            .rij(Expr.FunctieKind.AANTAL_GEVULDE, "filledCount");

    public static final Familie<Expr.FunctieKind> BUILTIN_KIND = new Familie<Expr.FunctieKind>("BuiltinCall.kind",
            "BuiltinCall.kind")
            .rij(Expr.FunctieKind.WORTEL, "squareRoot")
            .rij(Expr.FunctieKind.ABSOLUTE_WAARDE, "absoluteValue")
            .rij(Expr.FunctieKind.CONCATENATIE, "concatenation")
            .rij(Expr.FunctieKind.TIJDSDUUR, "duration")
            .rij(Expr.FunctieKind.ABSOLUTE_TIJDSDUUR, "absoluteDuration")
            .rij(Expr.FunctieKind.EERSTE_PAASDAG, "easterSunday")
            .rij(Expr.FunctieKind.TIJDSEVENREDIG_DEEL, "timeProportionalPart")
            .rij(Expr.FunctieKind.AANTAL_DAGEN_IN, "dayCount")
            .rij(Expr.FunctieKind.JAAR_UIT, "yearOf")
            .rij(Expr.FunctieKind.MAAND_UIT, "monthOf")
            .rij(Expr.FunctieKind.DAG_UIT, "dayOf")
            .rij(Expr.FunctieKind.UUR_UIT, "hourOf")
            .rij(Expr.FunctieKind.MINUUT_UIT, "minuteOf")
            .rij(Expr.FunctieKind.SECONDE_UIT, "secondOf")
            .rij(Expr.FunctieKind.MILLISECONDE_UIT, "millisecondOf")
            .rij(Expr.FunctieKind.DATUM_MET, "dateWith")
            .rij(Expr.FunctieKind.DATUMTIJD_MET, "dateTimeWith");

    static {
        // the split of the one function-kind enum: each kind is spelled by exactly one of the two nodes
        for (Expr.FunctieKind k : Expr.FunctieKind.values()) {
            if (AGGREGATION_KIND.rijen.containsKey(k)) BUILTIN_KIND.ongeschreven(k, "a fold kind is an Aggregation");
            else AGGREGATION_KIND.ongeschreven(k, "a kind outside the eight folds is a BuiltinCall");
        }
    }

    public static final Familie<Conditie.Predicaatsoort> PREDICATE_KIND = new Familie<Conditie.Predicaatsoort>(
            "Predicate.kind", "Predicate.kind")
            .rij(Conditie.Predicaatsoort.LEEG, "empty")
            .rij(Conditie.Predicaatsoort.GEVULD, "filled")
            .rij(Conditie.Predicaatsoort.ELFPROEF, "elevenTest")
            .rij(Conditie.Predicaatsoort.NUMERIEK_MET_EXACT, "exactDigits")
            .rij(Conditie.Predicaatsoort.DAGSOORT, "dayKind");

    /** One family on two nodes: {@code the} is a quantifier of a compound condition, and a quantifier fold
     *  refuses it by name (WF-108 (a)). */
    public static final Familie<Conditie.Kwantor> QUANTIFIER = new Familie<Conditie.Kwantor>("quantifier",
            "Compound.quantifier", "QuantifierFold.quantifier")
            .rij(Conditie.Kwantor.ALLE, "all")
            .rij(Conditie.Kwantor.GEEN, "none")
            .rij(Conditie.Kwantor.TEN_MINSTE, "atLeast")
            .rij(Conditie.Kwantor.TEN_HOOGSTE, "atMost")
            .rij(Conditie.Kwantor.PRECIES, "exactly")
            .geweigerd(Conditie.Kwantor.DE, "the")
            .geweigerdOp("QuantifierFold.quantifier");

    /** The two session values by their in-memory names. The wire field is a free string that WF-098 closes byte
     *  for byte, so a name outside the table is no spelling of this family. */
    public static final Familie<String> CONTEXT_VALUE_NAME = new Familie<String>("ContextValue.name",
            "ContextValue.name")
            .rij("Rekendatum", "evaluationDate")
            .rij("Rekenjaar", "evaluationYear")
            .alsVrij();

    /** The whole-period qualifier: the record's {@code jaar} flag (true: the whole year, false: the whole
     *  month). */
    public static final Familie<Boolean> WHOLE_PERIOD_GRANULARITY = new Familie<Boolean>("WholePeriod.granularity",
            "WholePeriod.granularity")
            .rij(true, "year")
            .rij(false, "month");

    /** One wire severity; {@code error} and {@code info} are refused by name (WF-113 (a)), and a null severity
     *  stays a null that the same rule refuses. */
    public static final Familie<Severity> SEVERITY = new Familie<Severity>("Diagnostic.severity",
            "Diagnostic.severity")
            .rij(Severity.WARNING, "warning")
            .geweigerd(Severity.ERROR, "error")
            .geweigerd(Severity.INFO, "info")
            .geweigerdOp("Diagnostic.severity")
            .nullBlijft("Diagnostic.severity");

    /** The precision of a date: the datatype variant itself is the in-memory value (both records are
     *  component-free, so every instance of one is equal to every other). */
    public static final Familie<Datatype> DATE_PRECISION = new Familie<Datatype>("Date.precision", "Date.precision")
            .rij(new Datatype.DatumInDagen(), "day")
            .rij(new Datatype.DatumTijd(), "millisecond");

    /** Every family, in the order of the section {@code waarden}. */
    public static final List<Familie<?>> FAMILIES = List.of(NUMBER_SIGN, CHARACTERISTIC_KIND, GRANULARITY,
            PRESENTATION, METHOD, COMPARISON_OP, BINARY_OP, ROUNDING_MODE, AGGREGATION_KIND, BUILTIN_KIND,
            PREDICATE_KIND, QUANTIFIER, CONTEXT_VALUE_NAME, WHOLE_PERIOD_GRANULARITY, SEVERITY, DATE_PRECISION);
}
