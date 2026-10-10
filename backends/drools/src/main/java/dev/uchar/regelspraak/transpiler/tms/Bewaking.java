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
 * The watch the entry ({@code DroolsBackend}) keeps on a session of the truth-maintenance branch between its own calls.
 * Outside the entry a consumer changes the attributes and the characteristics of the beans {@code DroolsBackend.voegIn}
 * put in, with {@code update}, and does nothing else: the watch keeps, from {@code voegIn} or the last
 * {@code DroolsBackend.vuur} on, the first fact that was inserted, updated or deleted outside the entry, the first rule
 * that fired outside it, the first change of the agenda, the first insert or delete of a fact of a kind {@code voegIn}
 * puts in, and the first change beyond such an update: any insert or delete, an update of a fact that is no bean of the
 * input, and an update that changed a relation, a reference field, of a bean of the input. From the first {@code vuur}
 * on it keeps the globals as they stood then. Two readings rest on it.
 *
 * <ul>
 *   <li>{@link TmsTranspiler#lees} and {@link TmsTranspiler#zetAfgeleideWaarden} read a session only where it kept
 *       nothing after the last {@code vuur}, the agenda holds no change ({@link #agenda}) and no global changed: after
 *       an update the facts of the last fixpoint belong to an input that no longer stands, and a plain
 *       {@code fireAllRules} fires the rule layer without the cap and, on a model with a creation, without the
 *       creations, which fire only in the generations of {@code vuur}. A mark in the session ({@link Gevuurd}) cannot
 *       see either, since it is itself a fact that no event touches.</li>
 *   <li>{@code vuur} fires a session only where nothing but such updates happened outside it. {@code voegIn} states
 *       the places of a list ({@link Lijstplaats}) over the relations of one input, so a relation changed between two
 *       fires leaves a place behind or none, and the first fault of a fold is then that of the wrong member, or the
 *       fold stops the fire; the branch states its own facts, so a fact of the branch inserted, deleted or updated by
 *       a consumer stands beside what the rules derive; a rule that fires outside {@code vuur} fires without its cap and
 *       its generations; a cleared agenda drops what the network had to fire, and a focus change fires another group
 *       first; and a rule reads a global once, in the rule that states it as a fact at the first fire, so a fire after
 *       a change keeps the old value.</li>
 * </ul>
 *
 * <p>It lives on the session as a listener, so that it goes where the session goes and needs no table beside it, and
 * the entry finds it there ({@link #van}). The entry takes it off the session for its own work ({@link #zonder}): while
 * {@code vuur} fires, the session holds no listener of the watch, and Drools makes no event for it, so the watch costs
 * a fire nothing per firing. No rule reads it. Drools makes no event for {@code Agenda.clear}, and puts a call to
 * {@code AgendaGroup.clear} or {@code AgendaGroup.setFocus} on its propagation list for the next fire, so
 * {@link #agenda} reads those from the agenda itself.
 */
public final class Bewaking extends DefaultAgendaEventListener implements RuleRuntimeEventListener {

    /** The reference fields of a bean class, the relations of its object type: their names, and a getter per name. */
    public record Relatievelden(List<String> namen, List<Function<Object, Object>> lezers) {
        public Relatievelden {
            namen = List.copyOf(namen);
            lezers = List.copyOf(lezers);
            if (namen.size() != lezers.size())
                throw new IllegalArgumentException(namen.size() + " namen bij " + lezers.size() + " lezers");
        }

        Object[] lees(Object bean) {
            Object[] uit = new Object[lezers.size()];
            for (int i = 0; i < uit.length; i++) uit[i] = lezers.get(i).apply(bean);
            return uit;
        }
    }

    /** The bean classes of the model, each with its relations. */
    private final Map<Class<?>, Relatievelden> beans;
    /** The kinds of fact {@code voegIn} puts in: the bean classes, and what it states beside them. */
    private final Set<Class<?>> invoer;
    /** The beans of the input, each with the objects its reference fields held when it came in. */
    private final Map<Object, Object[]> relaties = new IdentityHashMap<>();
    /** The globals at the first {@code vuur}, by name; null before it. */
    private Map<String, Object> globalen;
    /** The call of the entry the watch keeps from, as a sentence names it. */
    private String sinds;
    /** The first insert, update or delete outside the entry, as a clause ("Vondst@1f werd gewijzigd met update"), or
     *  null. */
    private String feit;
    /** The name of the first rule that fired outside the entry, or null. */
    private String regel;
    /** The first change of the agenda outside the entry that Drools reports as an event, as a clause, or null. */
    private String agendawijziging;
    /** The first insert or delete outside the entry of a fact of a kind {@link #invoer} holds, as a clause, or null. */
    private String erbijOfEraf;
    /** The first insert, update or delete outside the entry other than an update of an attribute or a characteristic of
     *  a bean of the input, as a clause, or null. */
    private String anders;

    private Bewaking(Map<Class<?>, Relatievelden> beans) {
        this.beans = Map.copyOf(beans);
        Set<Class<?>> soorten = new HashSet<>(beans.keySet());
        soorten.add(Emmerinvoer.class);
        soorten.add(Lijstplaats.class);
        this.invoer = Set.copyOf(soorten);
    }

    /** The watch on {@code sessie}, or null where the entry put none there. */
    public static Bewaking van(KieSession sessie) {
        for (RuleRuntimeEventListener l : sessie.getRuleRuntimeEventListeners())
            if (l instanceof Bewaking b) return b;
        return null;
    }

    /** The watch on {@code sessie}, put there where there is none, with {@code beans} the bean classes of the model and
     *  their relations; it takes {@code invoerbeans} as beans of the input, with the relations they hold now, forgets
     *  what happened and keeps from now on, {@code sinds} naming the call of the entry it keeps from. */
    public static Bewaking op(KieSession sessie, Map<Class<?>, Relatievelden> beans, Iterable<?> invoerbeans,
                              String sinds) {
        Bewaking b = van(sessie);
        if (b == null) {
            b = new Bewaking(beans);
            sessie.addEventListener((RuleRuntimeEventListener) b);
            sessie.addEventListener((AgendaEventListener) b);
        }
        for (Object bean : invoerbeans) {
            Relatievelden r = b.beans.get(bean.getClass());
            if (r == null) throw new IllegalArgumentException(Feitnamen.object(bean) + " is geen bean van het model");
            b.relaties.putIfAbsent(bean, r.lees(bean));
        }
        b.vergeet(sinds);
        return b;
    }

    /** Forgets what happened and keeps from now on, {@code sinds} naming the call of the entry it keeps from. */
    public void vergeet(String sinds) {
        this.sinds = Objects.requireNonNull(sinds, "sinds");
        feit = null;
        regel = null;
        agendawijziging = null;
        erbijOfEraf = null;
        anders = null;
    }

    /** Keeps the globals of {@code sessie} as they stand now, where it keeps none yet: the entry calls this at the
     *  first {@code vuur}. */
    public void legGlobalenVast(KieSession sessie) {
        if (globalen == null) globalen = globalen(sessie);
    }

    /** The first global of {@code sessie} whose value differs from the one it had at the first {@code vuur}, as a
     *  clause, by name; null where none does, or before the first {@code vuur}. */
    public String veranderdeGlobal(KieSession sessie) {
        if (globalen == null) return null;
        Map<String, Object> nu = globalen(sessie);
        for (String naam : new TreeSet<>(union(globalen.keySet(), nu.keySet()))) {
            Object oud = globalen.get(naam);
            Object nieuw = nu.get(naam);
            if (!Objects.equals(oud, nieuw))
                return "de global '" + naam + "' veranderde van " + waarde(oud) + " in " + waarde(nieuw)
                        + " na de eerste DroolsBackend.vuur van deze sessie";
        }
        return null;
    }

    private static Map<String, Object> globalen(KieSession sessie) {
        Map<String, Object> uit = new HashMap<>();
        for (String naam : sessie.getGlobals().getGlobalKeys()) uit.put(naam, sessie.getGlobals().get(naam));
        return uit;
    }

    private static Set<String> union(Set<String> a, Set<String> b) {
        Set<String> uit = new HashSet<>(a);
        uit.addAll(b);
        return uit;
    }

    private static String waarde(Object w) {
        return w == null ? "geen waarde" : w.toString();
    }

    /** Runs {@code werk}, the entry's own work, with this watch off {@code sessie}, and puts it back after, also where
     *  {@code werk} throws. */
    public <T> T zonder(KieSession sessie, Supplier<T> werk) {
        sessie.removeEventListener((RuleRuntimeEventListener) this);
        sessie.removeEventListener((AgendaEventListener) this);
        try {
            return werk.get();
        } finally {
            sessie.addEventListener((RuleRuntimeEventListener) this);
            sessie.addEventListener((AgendaEventListener) this);
        }
    }

    /** The call of the entry the watch keeps from, as a sentence names it. */
    public String sinds() {
        return sinds;
    }

    /** What happened outside the entry since {@link #sinds}, as one to three clauses: the first fact inserted, updated
     *  or deleted, the first change of the agenda Drools reports as an event, and the first rule that fired, each where
     *  there was one; null where nothing happened. */
    public String buitenom() {
        return samen(feit, agendawijziging, regel == null ? null : "de regel \"" + regel + "\" vuurde");
    }

    /** What happened outside the entry since {@link #sinds} beyond an update of an attribute or a characteristic of a
     *  bean of the input, as a clause: a rule that fired, else a change of the agenda Drools reports as an event, else
     *  the first other insert, update or delete; null where nothing did. A fire names its rule rather than the facts
     *  truth maintenance inserted and deleted on the way, which are its consequence. */
    public String meerDanEenUpdate() {
        if (regel != null) return "de regel \"" + regel + "\" vuurde";
        return agendawijziging != null ? agendawijziging : anders;
    }

    /** The first insert or delete outside the entry since {@link #sinds} of a fact of a kind {@code voegIn} puts in, as
     *  a clause, or null. */
    public String erbijOfEraf() {
        return erbijOfEraf;
    }

    /**
     * What a consumer did to the agenda of {@code sessie} that Drools does not report as an event, as a clause, or
     * null: an agenda group cleared, by {@code Agenda.clear}, which clears every group, or by {@code AgendaGroup.clear}
     * at a fire that ran it; and a call to {@code AgendaGroup.clear} or {@code AgendaGroup.setFocus} that waits on the
     * propagation list for the next fire. The entry never clears a group, so a group that holds a recency of a clear
     * was cleared by a consumer, at any moment of the session. Read from Drools' own agenda, since no listener sees
     * these: {@code Agenda.clear} resets the groups and the propagation list without an event, and the two calls on a
     * group only queue an action.
     */
    public static String agenda(KieSession sessie) {
        if (!(sessie.getAgenda() instanceof InternalAgenda agenda))
            throw new IllegalStateException("de agenda van deze sessie is geen " + InternalAgenda.class.getName() + " maar "
                    + sessie.getAgenda().getClass().getName() + ", dus de bewaking kan haar niet lezen");
        Set<String> wat = new LinkedHashSet<>();
        for (Iterator<PropagationEntry> it = agenda.getActionsIterator(); it.hasNext(); ) {
            PropagationEntry e = it.next();
            if (e instanceof AgendaGroupQueueImpl.ClearAction)
                wat.add("een agendagroep werd geleegd met AgendaGroup.clear");
            else if (e instanceof AgendaGroupQueueImpl.SetFocusAction)
                wat.add("een agendagroep kreeg de focus met AgendaGroup.setFocus");
        }
        Set<String> geleegd = new TreeSet<>();
        for (InternalAgendaGroup g : agenda.getAgendaGroupsManager().getAgendaGroups())
            if (g.getClearedForRecency() != -1) geleegd.add("de agendagroep \"" + g.getName() + "\" werd geleegd");
        wat.addAll(geleegd);
        return wat.isEmpty() ? null : String.join(" en ", wat);
    }

    @Override public void objectInserted(ObjectInsertedEvent e) {
        gebeurd(e.getObject(), Feitnamen.object(e.getObject()) + " werd ingevoegd", true);
    }

    @Override public void objectUpdated(ObjectUpdatedEvent e) {
        Object o = e.getObject();
        String zin = Feitnamen.object(o) + " werd gewijzigd met update";
        if (feit == null) feit = zin;
        if (anders != null) return;
        Object[] voor = relaties.get(o);
        if (voor == null) {
            anders = zin;
            return;
        }
        Relatievelden r = beans.get(o.getClass());
        Object[] na = r.lees(o);
        for (int i = 0; i < na.length; i++)
            if (na[i] != voor[i]) {
                anders = "het veld '" + r.namen().get(i) + "' van " + Feitnamen.object(o) + ", een relatie, werd"
                        + " gewijzigd met update";
                return;
            }
    }

    @Override public void objectDeleted(ObjectDeletedEvent e) {
        gebeurd(e.getOldObject(), Feitnamen.object(e.getOldObject()) + " werd verwijderd", true);
    }

    @Override public void beforeMatchFired(BeforeMatchFiredEvent e) {
        if (regel == null) regel = e.getMatch().getRule().getName();
    }

    /** A match that a change of a fact cancels is the network at work during a fire; any other cancellation is a
     *  consumer's: a cleared group run at a fire, or a filter of a fire. */
    @Override public void matchCancelled(MatchCancelledEvent e) {
        if (e.getCause() != MatchCancelledCause.WME_MODIFY)
            agendawijziging("een activatie van de regel \"" + e.getMatch().getRule().getName() + "\" werd geannuleerd");
    }

    @Override public void agendaGroupPushed(AgendaGroupPushedEvent e) {
        agendawijziging("de agendagroep \"" + e.getAgendaGroup().getName() + "\" kreeg de focus");
    }

    @Override public void agendaGroupPopped(AgendaGroupPoppedEvent e) {
        agendawijziging("de agendagroep \"" + e.getAgendaGroup().getName() + "\" verloor de focus");
    }

    private void agendawijziging(String zin) {
        if (agendawijziging == null) agendawijziging = zin;
    }

    private void gebeurd(Object o, String zin, boolean erbijOfEraf) {
        if (feit == null) feit = zin;
        if (anders == null) anders = zin;
        if (erbijOfEraf && this.erbijOfEraf == null && invoer.contains(o.getClass())) this.erbijOfEraf = zin;
    }

    /** The clauses that are not null, joined: "a", "a en b", "a, b en c". */
    private static String samen(String... delen) {
        List<String> er = java.util.Arrays.stream(delen).filter(Objects::nonNull).toList();
        if (er.isEmpty()) return null;
        if (er.size() == 1) return er.get(0);
        return String.join(", ", er.subList(0, er.size() - 1)) + " en " + er.get(er.size() - 1);
    }
}
