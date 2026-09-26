package dev.uchar.regelspraak.ir;

import dev.uchar.regelspraak.model.Datatype;
import dev.uchar.regelspraak.model.RegelSpraakModel.Objecttype;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * A fully resolved scenariotoets — a documented extension of this frontend (grammar/DIVERGENCES.md,
 * "Scenariotoetsen"), not a §13 construct: v2.3.0 has no test chapter. It is the resolver's typed form of one
 * {@code Scenario} block: which rules it tests ({@link #scope}), on which day ({@link #peildatum}), against which
 * world ({@link #situatie}, the merged situation) and what it expects ({@link #verwachtingen}). The lowering
 * ({@code emit.CaseEmitter}) writes it as one case file of the format uchar-case-v0 and never re-reads the parse
 * tree, so every value here is already what that file carries: a number is its exact case lexeme in the declared
 * unit of its target, a date a {@link LocalDate}, a date-time a {@link LocalDateTime}, an enumeration value its
 * bare label.
 *
 * @param naam          the scenario name as written, whitespace collapsed
 * @param scope         the rules the scenario tests, or null when the header names none (the whole document)
 * @param peildatum     the evaluation date, or null when the header sets none (a time-free case)
 * @param situatie      the merged situation: the based-on situations in written order, then the inline one
 * @param verwachtingen the expectations in written order; the segment bullets of one timeline are one entry, at
 *                      the position of its first bullet
 * @param line          the 1-based source line of the {@code Scenario} header
 */
public record ResolvedScenario(String naam, Scope scope, LocalDate peildatum, Situatie situatie,
                               List<Verwachting> verwachtingen, int line) {

    /** The rules a scenario tests. {@code regels} holds each name as written (a rule, a decision table, a
     *  defaults group, or for {@link ScopeSoort#REGELGROEP} the one group); {@code vormen} holds the names of the
     *  model forms the scope carries, as the model spells them — what the emitted IR keeps; {@code weggelaten} holds
     *  the source lines of the blocks (a rule, a decision table, an extension block, a rule group) whose every form
     *  the scope leaves out, so the IR document drops the diagnostics raised there. */
    public record Scope(ScopeSoort soort, List<String> regels, List<String> vormen, List<Regelbereik> weggelaten) {
        public Scope(ScopeSoort soort, List<String> regels, List<String> vormen) {
            this(soort, regels, vormen, List.of());
        }
    }

    /** The source lines {@code van} to {@code tot}, both inclusive. */
    public record Regelbereik(int van, int tot) {
        public boolean bevat(int regel) { return regel >= van && regel <= tot; }
    }
    public enum ScopeSoort { ENKELE_REGEL, MEERDERE_REGELS, REGELGROEP }

    /** The merged situation: the instances by label, in first-seen order, and the parameter values (a later
     *  value replaces an earlier one in place). */
    public record Situatie(List<Instantie> instanties, List<ParameterWaarde> parameters) {
        /** The instance carrying label {@code label}, or null. */
        public Instantie instantie(String label) {
            return instanties.stream().filter(i -> i.label().equals(label)).findFirst().orElse(null);
        }
    }

    /** One instance: its label, its declared type, and its assignments. A later assignment of the same attribute,
     *  role or characteristic has replaced the earlier one in place, so each appears once. */
    public record Instantie(String label, Objecttype objecttype, List<AttribuutWaarde> attributen,
                            List<RolWaarde> rollen, List<KenmerkWaarde> kenmerken) {}

    /** An attribute assignment under its declared name: a scalar {@code waarde}, or for a time-dependent
     *  attribute a {@code tijdlijn} of segments in written order. Exactly one of the two is non-null. */
    public record AttribuutWaarde(String attribuut, Datatype type, Waarde waarde, List<Segment> tijdlijn, int line) {}

    /** A role assignment: the key as the case file writes it (the declared role name, or its declared plural where
     *  the source wrote the plural), the declared type of the role it names, the labels it links, and whether the
     *  role is to-many. */
    public record RolWaarde(String sleutel, Objecttype doeltype, List<String> doelLabels, boolean meervoudig, int line) {}

    /** A characteristic asserted on an instance under its declared name. {@code perioden} is null for a static
     *  characteristic and holds the periods in which a time-dependent one holds, in written order. */
    public record KenmerkWaarde(String kenmerk, boolean bezittelijk, List<Periode> perioden, int line) {}

    /** A parameter value under its declared name: a scalar {@code waarde} or, for a time-dependent parameter, a
     *  {@code tijdlijn}. Exactly one of the two is non-null. */
    public record ParameterWaarde(String parameter, Datatype type, Waarde waarde, List<Segment> tijdlijn, int line) {}

    /** One timeline segment, {@code van} inclusive and {@code tot} exclusive. */
    public record Segment(LocalDate van, LocalDate tot, Waarde waarde) {}

    /** One period in which a time-dependent characteristic holds, {@code van} inclusive and {@code tot} exclusive. */
    public record Periode(LocalDate van, LocalDate tot) {}

    /** One expectation; {@code soort} selects the meaningful fields. {@code tijdlijn} is the expected timeline of an
     *  {@link VerwachtingSoort#ATTRIBUUT_TIJDLIJN} (empty when the whole timeline is expected empty); {@code regel}
     *  and {@code label} of an {@link VerwachtingSoort#INCONSISTENT} are its optional key. */
    public record Verwachting(VerwachtingSoort soort, String label, String attribuut, String kenmerk,
                              Waarde verwachteWaarde, List<Segment> tijdlijn, String regel, int line, String bron) {}
    public enum VerwachtingSoort {
        ATTRIBUUT_WAARDE,    // de <attr> van <LABEL> is <waarde>
        ATTRIBUUT_LEEG,      // de <attr> van <LABEL> is leeg
        ATTRIBUUT_TIJDLIJN,  // de <attr> van <LABEL> is <waarde> van <d> tot <d> (one per segment) | is altijd leeg
        KENMERK_AANWEZIG,    // <LABEL> is/heeft [een] <kenmerk>
        KENMERK_AFWEZIG,     // <LABEL> is/heeft geen <kenmerk>
        CONSISTENT,          // de gegevens zijn consistent
        INCONSISTENT         // de gegevens zijn inconsistent [volgens "<regel>" [voor <LABEL>]]
    }

    /** A normalised value — exactly one field is non-null. {@code getal} is the exact case lexeme (a decimal with a
     *  dot, or a reduced fraction {@code p/q}) in the declared unit of its target. */
    public record Waarde(String getal, LocalDate datum, LocalDateTime datumTijd, Boolean bool, String tekst,
                         String enumWaarde) {
        public static Waarde vanGetal(String lexeem) { return new Waarde(lexeem, null, null, null, null, null); }
        public static Waarde vanDatum(LocalDate d) { return new Waarde(null, d, null, null, null, null); }
        public static Waarde vanDatumTijd(LocalDateTime d) { return new Waarde(null, null, d, null, null, null); }
        public static Waarde vanBoolean(boolean b) { return new Waarde(null, null, null, b, null, null); }
        public static Waarde vanTekst(String t) { return new Waarde(null, null, null, null, t, null); }
        public static Waarde vanEnum(String e) { return new Waarde(null, null, null, null, null, e); }
    }
}
