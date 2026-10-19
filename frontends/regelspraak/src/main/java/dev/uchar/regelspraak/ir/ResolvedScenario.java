package dev.uchar.regelspraak.ir;

import dev.uchar.regelspraak.model.RegelSpraakModel.FactType;
import dev.uchar.regelspraak.model.RegelSpraakModel.ObjectType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * A fully resolved scenariotoets — a documented extension of this frontend (grammar/DIVERGENCES.md,
 * "Scenariotoetsen"), not a §13 construct: v2.3.0 has no test chapter. It is the resolver's typed form of one
 * {@code Scenario} block: which rules it tests ({@link #scope}), on which day ({@link #asOfDate}), against which
 * world ({@link #situation}, the merged situation) and what it expects ({@link #expectations}). The lowering
 * ({@code emit.CaseEmitter}) writes it as one case file of the format uchar-case-v0 and never re-reads the parse
 * tree, so every value here is already what that file carries: a number is its exact case lexeme in the declared
 * unit of its target, a date a {@link LocalDate}, a date-time a {@link LocalDateTime}, an enumeration value its
 * bare label.
 *
 * @param name          the scenario name as written, whitespace collapsed
 * @param scope         the rules the scenario tests, or null when the header names none (the whole document)
 * @param asOfDate     the evaluation date, or null when the header sets none (a time-free case)
 * @param situation      the merged situation: the based-on situations in written order, then the inline one; its
 *                      parameter values open with those of the chosen parameter set on the peildatum
 * @param expectations the expectations in written order; the segment bullets of one timeline are one entry, at
 *                      the position of its first bullet
 * @param line          the 1-based source line of the {@code Scenario} header
 */
public record ResolvedScenario(String name, Scope scope, LocalDate asOfDate, Situation situation,
                               List<Expectation> expectations, int line) {

    /** The rules a scenario tests: {@code forms} holds the names of the model forms the scope carries, as the model
     *  spells them — what the emitted IR keeps; {@code omitted} holds the source lines of the blocks (a rule, a
     *  decision table, an extension block, a rule group) whose every form the scope leaves out, so the IR document
     *  drops the diagnostics raised there. */
    public record Scope(List<String> forms, List<LineRange> omitted) {
        public Scope(List<String> forms) { this(forms, List.of()); }
    }

    /** The source lines {@code van} to {@code until}, both inclusive. */
    public record LineRange(int van, int until) {
        public boolean contains(int rule) { return rule >= van && rule <= until; }
    }

    /** The merged situation: the instances by label, in first-seen order, and the parameter values (a later
     *  value replaces an earlier one in place), preceded by the values the chosen parameter set gives on the
     *  peildatum — per parameter its jaargang, or its whole timeline. No parameter stands in both parts: the
     *  resolver refuses one the set and the situation both give. */
    public record Situation(List<Instance> instances, List<ParameterValue> parameters) {
        /** The instance carrying label {@code label}, or null. */
        public Instance instance(String label) {
            return instances.stream().filter(i -> i.label().equals(label)).findFirst().orElse(null);
        }
    }

    /** One instance: its label, its declared type, and its assignments. A later assignment of the same attribute,
     *  role or characteristic has replaced the earlier one in place, so each appears once. */
    public record Instance(String label, ObjectType objectType, List<AttributeValue> attributes,
                            List<RoleValue> roles, List<CharacteristicValue> characteristics) {}

    /** An attribute assignment under its declared name: a scalar {@code value}, or for a time-dependent
     *  attribute a {@code timeline} of segments in written order. Exactly one of the two is non-null. */
    public record AttributeValue(String attribute, ScenarioValue value, List<Segment> timeline, int line) {}

    /** A role assignment: the key as the case file writes it (the declared role name, or its declared plural where
     *  the source wrote the plural), the fact type the role was resolved in — one role name can stand in several
     *  fact types, and the type of the instance decides which one the assignment means —, the declared type of the
     *  role it names, the labels it links, and whether the role is to-many. */
    public record RoleValue(String key, FactType factType, ObjectType targetType, List<String> targetLabels,
                            boolean many, int line) {}

    /** A characteristic asserted on an instance under its declared name. {@code periods} is null for a static
     *  characteristic and holds the periods in which a time-dependent one holds, in written order. */
    public record CharacteristicValue(String characteristic, List<TimeWindow> periods, int line) {}

    /** A parameter value under its declared name: a scalar {@code value} or, for a time-dependent parameter, a
     *  {@code timeline}. Exactly one of the two is non-null. */
    public record ParameterValue(String parameter, ScenarioValue value, List<Segment> timeline, int line) {}

    /** One timeline segment, {@code van} inclusive and {@code until} exclusive. */
    public record Segment(LocalDate van, LocalDate until, ScenarioValue value) {}

    /** One period in which a time-dependent characteristic holds, {@code van} inclusive and {@code until} exclusive. */
    public record TimeWindow(LocalDate van, LocalDate until) {}

    /** One expectation; {@code kind} selects the meaningful fields. {@code timeline} is the expected timeline of an
     *  {@link ExpectationKind#ATTRIBUTE_TIMELINE} (empty when the whole timeline is expected empty); {@code rule}
     *  and {@code label} of an {@link ExpectationKind#INCONSISTENT} are its optional key. */
    public record Expectation(ExpectationKind kind, String label, String attribute, String characteristic,
                              ScenarioValue expectedValue, List<Segment> timeline, String rule, int line) {}
    public enum ExpectationKind {
        ATTRIBUTE_VALUE,    // de <attr> van <LABEL> is <waarde>
        ATTRIBUTE_EMPTY,      // de <attr> van <LABEL> is leeg
        ATTRIBUTE_TIMELINE,  // de <attr> van <LABEL> is <waarde> van <d> tot <d> (one per segment) | is altijd leeg
        CHARACTERISTIC_PRESENT,    // <LABEL> is/heeft [een] <kenmerk>
        CHARACTERISTIC_ABSENT,     // <LABEL> is/heeft geen <kenmerk>
        CONSISTENT,          // de gegevens zijn consistent
        INCONSISTENT         // de gegevens zijn inconsistent [volgens "<regel>" [voor <LABEL>]]
    }

    /** A normalised value — exactly one field is non-null. {@code number} is the exact case lexeme (a decimal with a
     *  dot, or a reduced fraction {@code p/q}) in the declared unit of its target. */
    public record ScenarioValue(String number, LocalDate date, LocalDateTime dateTime, Boolean bool, String text,
                         String enumValue) {
        public static ScenarioValue fromNumber(String lexemeText) { return new ScenarioValue(lexemeText, null, null, null, null, null); }
        public static ScenarioValue fromDate(LocalDate d) { return new ScenarioValue(null, d, null, null, null, null); }
        public static ScenarioValue fromDateTime(LocalDateTime d) { return new ScenarioValue(null, null, d, null, null, null); }
        public static ScenarioValue fromBoolean(boolean b) { return new ScenarioValue(null, null, null, b, null, null); }
        public static ScenarioValue fromText(String t) { return new ScenarioValue(null, null, null, null, t, null); }
        public static ScenarioValue fromEnum(String e) { return new ScenarioValue(null, null, null, null, null, e); }
    }
}
