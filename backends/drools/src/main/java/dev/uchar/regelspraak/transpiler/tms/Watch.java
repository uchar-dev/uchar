package dev.uchar.regelspraak.transpiler.tms;

import org.drools.core.common.AgendaGroupQueueImpl;
import org.drools.core.common.InternalAgenda;
import org.drools.core.common.InternalAgendaGroup;
import org.drools.core.phreak.PropagationEntry;
import org.kie.api.event.rule.AgendaEventListener;
import org.kie.api.event.rule.AgendaGroupPoppedEvent;
import org.kie.api.event.rule.AgendaGroupPushedEvent;
import org.kie.api.event.rule.BeforeMatchFiredEvent;
import org.kie.api.event.rule.DefaultAgendaEventListener;
import org.kie.api.event.rule.MatchCancelledCause;
import org.kie.api.event.rule.MatchCancelledEvent;
import org.kie.api.event.rule.ObjectDeletedEvent;
import org.kie.api.event.rule.ObjectInsertedEvent;
import org.kie.api.event.rule.ObjectUpdatedEvent;
import org.kie.api.event.rule.RuleRuntimeEventListener;
import org.kie.api.runtime.KieSession;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The watch the entry ({@code DroolsBackend}) keeps on a session of the exec-model branch between its own calls.
 * Outside the entry a consumer changes the attributes and the characteristics of the beans {@code DroolsBackend.insert}
 * put in, with {@code update}, and does nothing else: the watch keeps, from {@code insert} or the last
 * {@code DroolsBackend.fire} on, the first fact that was inserted, updated or deleted outside the entry, the first rule
 * that fired outside it, the first change of the agenda, the first insert or delete of a fact of a kind {@code insert}
 * puts in, and the first change beyond such an update: any insert or delete, an update of a fact that is no bean of the
 * input, and an update that changed a relation, a reference field, of a bean of the input. From the first {@code fire}
 * on it keeps the globals as they stood then. It records the relations of the beans of the input when it takes them,
 * and tells a relation changed without an update from the beans themselves ({@link #changedRelation}), since such a
 * change makes no event and Drools keeps reading the old relation. Two readings rest on it.
 *
 * <ul>
 *   <li>{@link TmsTranspiler#read} and {@link TmsTranspiler#setDerivedValues} read a session only where it kept
 *       nothing after the last {@code fire}, the agenda holds no change ({@link #agenda}) and no global changed: after
 *       an update, or after a relation changed without one, the facts of the last fixpoint belong to an input that
 *       no longer stands, and a plain
 *       {@code fireAllRules} fires the rule layer without the cap and, on a model with a creation, without the
 *       creations, which fire only in the generations of {@code fire}. A mark in the session ({@link Fired}) cannot
 *       see either, since it is itself a fact that no event touches.</li>
 *   <li>{@code fire} fires a session only where nothing but such updates happened outside it. {@code insert} states
 *       the places of a list ({@link ListPlace}) over the relations of one input, so a relation changed between two
 *       fires leaves a place behind or none, and the first fault of a fold is then that of the wrong member, or the
 *       fold stops the fire; the branch states its own facts, so a fact of the branch inserted, deleted or updated by
 *       a consumer stands beside what the rules derive; a rule that fires outside {@code fire} fires without its cap and
 *       its generations; a cleared agenda drops what the network had to fire, and a focus change fires another group
 *       first; and a rule reads a global once, in the rule that states it as a fact at the first fire, so a fire after
 *       a change keeps the old value.</li>
 * </ul>
 *
 * <p>It lives on the session as a listener, so that it goes where the session goes and needs no table beside it, and
 * the entry finds it there ({@link #of}). The entry takes it off the session for its own work ({@link #without}): while
 * {@code fire} fires, the session holds no listener of the watch, and Drools makes no event for it, so the watch costs
 * a fire nothing per firing. No rule reads it. Drools makes no event for {@code Agenda.clear}, and puts a call to
 * {@code AgendaGroup.clear} or {@code AgendaGroup.setFocus} on its propagation list for the next fire, so
 * {@link #agenda} reads those from the agenda itself.
 */
public final class Watch extends DefaultAgendaEventListener implements RuleRuntimeEventListener {

    /** The reference fields of a bean class, the relations of its object type: their names, and a getter per name. */
    public record RelationFields(List<String> names, List<Function<Object, Object>> readers) {
        public RelationFields {
            names = List.copyOf(names);
            readers = List.copyOf(readers);
            if (names.size() != readers.size())
                throw new IllegalArgumentException(names.size() + " namen bij " + readers.size() + " lezers");
        }

        Object[] read(Object bean) {
            Object[] out = new Object[readers.size()];
            for (int i = 0; i < out.length; i++) out[i] = readers.get(i).apply(bean);
            return out;
        }
    }

    /** The bean classes of the model, each with its relations. */
    private final Map<Class<?>, RelationFields> beans;
    /** The kinds of fact {@code insert} puts in: the bean classes, and what it states beside them. */
    private final Set<Class<?>> input;
    /** The beans of the input, each with the objects its reference fields held when it came in, or after the
     *  write-back ({@link #recordRelations}). */
    private final Map<Object, Object[]> relations = new IdentityHashMap<>();
    /** The beans of {@link #relations} in the order they came in, so that a sentence names the first changed relation of
     *  the input and not one of an order of identities. */
    private final List<Object> inputBeans = new ArrayList<>();
    /** The call of the entry at which the watch took the beans of the input, as a sentence names it. */
    private String recorded;
    /** The globals at the first {@code fire}, by name; null before it. */
    private Map<String, Object> globals;
    /** The call of the entry the watch keeps from, as a sentence names it. */
    private String since;
    /** The first insert, update or delete outside the entry, as a clause ("Vondst@1f werd gewijzigd met update"), or
     *  null. */
    private String fact;
    /** The name of the first rule that fired outside the entry, or null. */
    private String rule;
    /** The first change of the agenda outside the entry that Drools reports as an event, as a clause, or null. */
    private String agendaChange;
    /** The first insert or delete outside the entry of a fact of a kind {@link #input} holds, as a clause, or null. */
    private String onOrOff;
    /** The first insert, update or delete outside the entry other than an update of an attribute or a characteristic of
     *  a bean of the input, as a clause, or null. */
    private String otherChange;

    private Watch(Map<Class<?>, RelationFields> beans) {
        this.beans = Map.copyOf(beans);
        Set<Class<?>> kinds = new HashSet<>(beans.keySet());
        kinds.add(BucketInput.class);
        kinds.add(ListPlace.class);
        this.input = Set.copyOf(kinds);
    }

    /** The watch on {@code session}, or null where the entry put none there. */
    public static Watch of(KieSession session) {
        for (RuleRuntimeEventListener l : session.getRuleRuntimeEventListeners())
            if (l instanceof Watch b) return b;
        return null;
    }

    /** The watch on {@code session}, put there where there is none, with {@code beans} the bean classes of the model and
     *  their relations; it takes {@code inputBeans} as beans of the input, with the relations they hold now, forgets
     *  what happened and keeps from now on, {@code since} naming the call of the entry it keeps from and, where it puts
     *  the watch there, the call at which it took the beans. */
    public static Watch op(KieSession session, Map<Class<?>, RelationFields> beans, Iterable<?> inputBeans,
                              String since) {
        Watch b = of(session);
        if (b == null) {
            b = new Watch(beans);
            b.recorded = Objects.requireNonNull(since, "since");
            session.addEventListener((RuleRuntimeEventListener) b);
            session.addEventListener((AgendaEventListener) b);
        }
        for (Object bean : inputBeans) {
            RelationFields r = b.beans.get(bean.getClass());
            if (r == null) throw new IllegalArgumentException(FactNames.object(bean) + " is geen bean van het model");
            if (b.relations.putIfAbsent(bean, r.read(bean)) == null) b.inputBeans.add(bean);
        }
        b.forget(since);
        return b;
    }

    /** Forgets what happened and keeps from now on, {@code since} naming the call of the entry it keeps from. */
    public void forget(String since) {
        this.since = Objects.requireNonNull(since, "since");
        fact = null;
        rule = null;
        agendaChange = null;
        onOrOff = null;
        otherChange = null;
    }

    /** Keeps the globals of {@code session} as they stand now, where it keeps none yet: the entry calls this at the
     *  first {@code fire}. */
    public void recordGlobals(KieSession session) {
        if (globals == null) globals = globals(session);
    }

    /** The first global of {@code session} whose value differs from the one it had at the first {@code fire}, as a
     *  clause, by name; null where none does, or before the first {@code fire}. */
    public String changedGlobal(KieSession session) {
        if (globals == null) return null;
        Map<String, Object> now = globals(session);
        for (String name : new TreeSet<>(union(globals.keySet(), now.keySet()))) {
            Object oud = globals.get(name);
            Object fresh = now.get(name);
            if (!Objects.equals(oud, fresh))
                return "de global '" + name + "' veranderde van " + value(oud) + " in " + value(fresh)
                        + " na de eerste DroolsBackend.fire van deze sessie";
        }
        return null;
    }

    private static Map<String, Object> globals(KieSession session) {
        Map<String, Object> out = new HashMap<>();
        for (String name : session.getGlobals().getGlobalKeys()) out.put(name, session.getGlobals().get(name));
        return out;
    }

    private static Set<String> union(Set<String> a, Set<String> b) {
        Set<String> out = new HashSet<>(a);
        out.addAll(b);
        return out;
    }

    private static String value(Object w) {
        return w == null ? "geen waarde" : w.toString();
    }

    /** The first relation of a bean of the input, in the order the beans came in, whose reference field holds another
     *  object than the watch recorded, as a clause, or null. An update that changes a relation the watch keeps as more
     *  than an update ({@link #moreThanOneUpdate}); this reads the beans themselves, since a field set without an update
     *  makes no event, and the network keeps matching the object the field held when the bean came in. */
    public String changedRelation() {
        for (Object bean : inputBeans) {
            RelationFields r = beans.get(bean.getClass());
            Object[] before = relations.get(bean);
            Object[] now = r.read(bean);
            for (int i = 0; i < now.length; i++)
                if (now[i] != before[i])
                    return "het veld '" + r.names().get(i) + "' van " + FactNames.object(bean) + ", een relatie,"
                            + " veranderde zonder update: het hield bij " + recorded + " " + target(before[i])
                            + " en houdt nu " + target(now[i]);
        }
        return null;
    }

    private static String target(Object o) {
        return o == null ? "geen object" : FactNames.object(o);
    }

    /** Records the relations of the beans of the input as they stand now: the write-back puts the links the creations
     *  laid on the reference fields of those beans, the entry's own work, which a reading after it must not count as a
     *  changed relation. */
    public void recordRelations() {
        for (Object bean : inputBeans) relations.put(bean, beans.get(bean.getClass()).read(bean));
    }

    /** Runs {@code work}, the entry's own work, with this watch off {@code session}, and puts it back after, also where
     *  {@code work} throws. */
    public <T> T without(KieSession session, Supplier<T> work) {
        session.removeEventListener((RuleRuntimeEventListener) this);
        session.removeEventListener((AgendaEventListener) this);
        try {
            return work.get();
        } finally {
            session.addEventListener((RuleRuntimeEventListener) this);
            session.addEventListener((AgendaEventListener) this);
        }
    }

    /** The call of the entry the watch keeps from, as a sentence names it. */
    public String since() {
        return since;
    }

    /** What happened outside the entry since {@link #since}, as one to three clauses: the first fact inserted, updated
     *  or deleted, the first change of the agenda Drools reports as an event, and the first rule that fired, each where
     *  there was one; null where nothing happened. */
    public String outsideClause() {
        return combined(fact, agendaChange, rule == null ? null : "de regel \"" + rule + "\" vuurde");
    }

    /** What happened outside the entry since {@link #since} beyond an update of an attribute or a characteristic of a
     *  bean of the input, as a clause: a rule that fired, else a change of the agenda Drools reports as an event, else
     *  the first other insert, update or delete; null where nothing did. A fire names its rule rather than the facts
     *  truth maintenance inserted and deleted on the way, which are its consequence. */
    public String moreThanOneUpdate() {
        if (rule != null) return "de regel \"" + rule + "\" vuurde";
        return agendaChange != null ? agendaChange : otherChange;
    }

    /** The first insert or delete outside the entry since {@link #since} of a fact of a kind {@code insert} puts in, as
     *  a clause, or null. */
    public String onOrOff() {
        return onOrOff;
    }

    /**
     * What a consumer did to the agenda of {@code session} that Drools does not report as an event, as a clause, or
     * null: an agenda group cleared, by {@code Agenda.clear}, which clears every group, or by {@code AgendaGroup.clear}
     * at a fire that ran it; and a call to {@code AgendaGroup.clear} or {@code AgendaGroup.setFocus} that waits on the
     * propagation list for the next fire. The entry never clears a group, so a group that holds a recency of a clear
     * was cleared by a consumer, at any moment of the session. Read from Drools' own agenda, since no listener sees
     * these: {@code Agenda.clear} resets the groups and the propagation list without an event, and the two calls on a
     * group only queue an action. A fact inserted, updated or deleted waits on that list for the next fire as well, the
     * inserts of {@code insert} among them, and the watch sees each such call through its event. An action of another
     * kind, which this version of Drools does not queue between two calls of the entry and another version could, it
     * refuses with an {@code IllegalStateException} that names its class, since it cannot tell what that action does to
     * the agenda.
     */
    public static String agenda(KieSession session) {
        if (!(session.getAgenda() instanceof InternalAgenda agenda))
            throw new IllegalStateException("de agenda van deze sessie is geen " + InternalAgenda.class.getName() + " maar "
                    + session.getAgenda().getClass().getName() + ", dus de bewaking kan haar niet lezen");
        Set<String> description = new LinkedHashSet<>();
        for (Iterator<PropagationEntry> it = agenda.getActionsIterator(); it.hasNext(); ) {
            PropagationEntry e = it.next();
            if (e instanceof AgendaGroupQueueImpl.ClearAction)
                description.add("een agendagroep werd geleegd met AgendaGroup.clear");
            else if (e instanceof AgendaGroupQueueImpl.SetFocusAction)
                description.add("een agendagroep kreeg de focus met AgendaGroup.setFocus");
            else if (!(e instanceof PropagationEntry.Insert || e instanceof PropagationEntry.Update
                    || e instanceof PropagationEntry.Delete))
                throw new IllegalStateException("op de propagatielijst van deze sessie wacht een " + e.getClass().getName()
                        + ", een actie die de bewaking niet kent: zij kent naast een insert, update of delete van een"
                        + " feit alleen die van AgendaGroup.clear en AgendaGroup.setFocus, dus ze kan niet zeggen of een"
                        + " afnemer de agenda veranderde");
        }
        Set<String> emptied = new TreeSet<>();
        for (InternalAgendaGroup g : agenda.getAgendaGroupsManager().getAgendaGroups())
            if (g.getClearedForRecency() != -1) emptied.add("de agendagroep \"" + g.getName() + "\" werd geleegd");
        description.addAll(emptied);
        return description.isEmpty() ? null : String.join(" en ", description);
    }

    @Override public void objectInserted(ObjectInsertedEvent e) {
        happened(e.getObject(), FactNames.object(e.getObject()) + " werd ingevoegd", true);
    }

    @Override public void objectUpdated(ObjectUpdatedEvent e) {
        Object o = e.getObject();
        String sentence = FactNames.object(o) + " werd gewijzigd met update";
        if (fact == null) fact = sentence;
        if (otherChange != null) return;
        Object[] before = relations.get(o);
        if (before == null) {
            otherChange = sentence;
            return;
        }
        RelationFields r = beans.get(o.getClass());
        Object[] after = r.read(o);
        for (int i = 0; i < after.length; i++)
            if (after[i] != before[i]) {
                otherChange = "het veld '" + r.names().get(i) + "' van " + FactNames.object(o) + ", een relatie, werd"
                        + " gewijzigd met update";
                return;
            }
    }

    @Override public void objectDeleted(ObjectDeletedEvent e) {
        happened(e.getOldObject(), FactNames.object(e.getOldObject()) + " werd verwijderd", true);
    }

    @Override public void beforeMatchFired(BeforeMatchFiredEvent e) {
        if (rule == null) rule = e.getMatch().getRule().getName();
    }

    /** A match that a change of a fact cancels is the network at work during a fire; any other cancellation is a
     *  consumer's: a cleared group run at a fire, or a filter of a fire. */
    @Override public void matchCancelled(MatchCancelledEvent e) {
        if (e.getCause() != MatchCancelledCause.WME_MODIFY)
            agendaChange("een activatie van de regel \"" + e.getMatch().getRule().getName() + "\" werd geannuleerd");
    }

    @Override public void agendaGroupPushed(AgendaGroupPushedEvent e) {
        agendaChange("de agendagroep \"" + e.getAgendaGroup().getName() + "\" kreeg de focus");
    }

    @Override public void agendaGroupPopped(AgendaGroupPoppedEvent e) {
        agendaChange("de agendagroep \"" + e.getAgendaGroup().getName() + "\" verloor de focus");
    }

    private void agendaChange(String sentence) {
        if (agendaChange == null) agendaChange = sentence;
    }

    private void happened(Object o, String sentence, boolean onOrOff) {
        if (fact == null) fact = sentence;
        if (otherChange == null) otherChange = sentence;
        if (onOrOff && this.onOrOff == null && input.contains(o.getClass())) this.onOrOff = sentence;
    }

    /** The clauses that are not null, joined: "a", "a en b", "a, b en c". */
    private static String combined(String... parts) {
        List<String> present = java.util.Arrays.stream(parts).filter(Objects::nonNull).toList();
        if (present.isEmpty()) return null;
        if (present.size() == 1) return present.get(0);
        return String.join(", ", present.subList(0, present.size() - 1)) + " en " + present.get(present.size() - 1);
    }
}
