package dev.uchar.regelspraak.transpiler.tms;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Who decided one attribute on a timeline of one object, per period, at the fixpoint of a session, read off the facts
 * the lowering left standing ({@code DroolsBackend.periodDecisionsOf}), never computed again (RUNTIME-SEMANTICS
 * ch. 3.11, 7.11). A timeline target is decided per day: on every day by the writers that decide that day, and on a day
 * none of them decides it holds what it held before them, its input or leeg. Its writers are a defaults stack, which is
 * one writer of the target and decides per day by the rule of ch. 7.11, or the computing rules of the target, each of
 * which decides the days on which its timeline reads a value (ch. 3.11, "A writer's own days and change").
 *
 * <p>Every object of the session whose type has the member, the beans of the input and the objects the creations made
 * ({@link Fixpoint#objects}), has one record per attribute on a timeline that a stack or rules write; a stack on a
 * target that holds one value has its record in {@link DefaultsDecision}. Where the object's concrete type has a stack
 * on the member (ch. 7.10), the stack decides it, and every span names its decider, its conflict or neither; on every
 * other object the rules decide it, and every span names the rules that decided it, or none. {@link #spans} divides all
 * time into the maximal periods with one outcome, in the order of their days, so two adjacent spans differ in who
 * decided, in what was decided, or in the input that stands. A consumer reads the target's timeline off them: on a span
 * the value of its decider, of its rules or of its input, and leeg where it has none.
 *
 * <p>Defaults and rules are named by the JSON pointers of {@link DefaultsDecision}, into the IR document the build was
 * made from: a default by its {@code Default} node, an ordinary rule by the version it is,
 * {@code /rules/rules/0/versions/0}, also where ch. 7.10 takes it into a stack.
 *
 * <p>Where an evaluation fault stands, the session has no answer ({@link Fixpoint#faults}); the records give what the
 * facts say all the same, and a consumer reads the faults first: a stack whose write does not fit the target's
 * decimals (EF-006) wrote nothing, and its spans still name the defaults that decided, each with the value it decided;
 * two rules that write one day differently (EF-016) stand on that day's span side by side.
 *
 * @param object     the object, a bean of the build's bean model, compared by identity
 * @param member     the attribute on a timeline, named as the model declares it, as {@link Fixpoint} names a member
 * @param rootChains where a stack decides the member on the object, the pointers of its defaults at the root, per group,
 *                   highest first, as {@link DefaultsDecision#rootChains} gives them; empty where rules decide it
 * @param spans      the maximal periods with one outcome, in the order of their days, together all time
 */
public record PeriodDecisions(Object object, String member, List<List<String>> rootChains, List<Span> spans) {

    public PeriodDecisions {
        Objects.requireNonNull(object, "object");
        Objects.requireNonNull(member, "member");
        rootChains = rootChains.stream().map(List::copyOf).toList();
        spans = List.copyOf(spans);
        if (spans.isEmpty() || !spans.get(0).from().equals(Period.OPEN_FROM)
                || !spans.get(spans.size() - 1).to().equals(Period.OPEN_TO))
            throw new IllegalArgumentException("the periods of " + member + " do not cover all time: " + spans);
        for (int i = 1; i < spans.size(); i++)
            if (!spans.get(i - 1).to().equals(spans.get(i).from()))
                throw new IllegalArgumentException("the periods of " + member + " are not contiguous: " + spans);
        for (Span s : spans)
            if (!rootChains.isEmpty() && !s.rules().isEmpty() || rootChains.isEmpty() && (s.decider() != null
                    || s.conflict() != null))
                throw new IllegalArgumentException("a period of " + member + " names "
                        + (rootChains.isEmpty() ? "a default, where rules decide the target" : "rules, where a"
                        + " stack decides the target") + ": " + s);
    }

    /**
     * One maximal period {@code [from, to)} with one outcome: on every day of it the same writers decided the target
     * with the same value, or none did and the same input stands.
     *
     * <ul>
     *   <li>A <b>decider</b>: the stack decided those days (ch. 7.11), by the highest default of its chain that decides
     *       them, followed down as {@link DefaultsDecision.Decider} follows it: at a default with exception groups the
     *       member that decided, and the default itself where it fell back on its own content; where unordered members
     *       decided alike, the first in member order. Its decision is the value it wrote on those days.</li>
     *   <li>A <b>conflict</b>: the members that the stack reconciles per day decided those days differently (ch. 7.2,
     *       7.9), as {@link DefaultsDecision.Conflict} names them; the stack wrote nothing on them, the target holds its
     *       input there, and the §9.5 fact of the conflict's key stands on the object, whatever the number of days.</li>
     *   <li><b>Rules</b>: those computing rules of the target whose timelines read a value on those days, each with
     *       that value, in the order of the document. More than one where they agree, and where they differ, which
     *       faults the session (EF-016, ch. 5.7, "Two writers per day").</li>
     *   <li>None of them: no writer decided those days, and the target holds its input, the virtual input default of
     *       ch. 7.8 read per day, or is leeg.</li>
     * </ul>
     *
     * @param from     the first day, {@link Period#OPEN_FROM} where the span begins before every day
     * @param to       the day after the last, {@link Period#OPEN_TO} where the span runs past every day
     * @param decider  the default of the stack that decided those days, with the value it wrote on them; null where
     *                 none did
     * @param conflict the conflict that stood on those days; null where none did
     * @param rules    the computing rules that decided those days, each named by its version with the value its timeline
     *                 reads there, in the order of the document; empty where a stack decides the target, and where no
     *                 rule decided those days
     * @param input    the value the target's input holds on those days, where no writer decided them; null where a
     *                 writer decided them, where the input leaves them empty, and where the case gives the input as the
     *                 asOfDate-bucket scalar (ch. 8.4), which carries no period
     */
    public record Span(LocalDate from, LocalDate to, DefaultsDecision.Decider decider, DefaultsDecision.Conflict conflict,
                       List<DefaultsDecision.Decider> rules, Object input) {

        public Span {
            Objects.requireNonNull(from, "from");
            Objects.requireNonNull(to, "to");
            rules = List.copyOf(rules);
            if (!from.isBefore(to))
                throw new IllegalArgumentException("a period [" + from + ", " + to + ") without a day");
            if (decider != null && conflict != null)
                throw new IllegalArgumentException("a period names at most one of decider and conflict");
            if (input != null && (decider != null || !rules.isEmpty()))
                throw new IllegalArgumentException("the input stands only on days no writer decided");
        }
    }
}
