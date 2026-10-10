package dev.uchar.regelspraak.transpiler.tms;

import org.kie.api.event.rule.AgendaEventListener;
import org.kie.api.event.rule.BeforeMatchFiredEvent;
import org.kie.api.event.rule.DefaultAgendaEventListener;
import org.kie.api.event.rule.ObjectDeletedEvent;
import org.kie.api.event.rule.ObjectInsertedEvent;
import org.kie.api.event.rule.ObjectUpdatedEvent;
import org.kie.api.event.rule.RuleRuntimeEventListener;
import org.kie.api.runtime.KieSession;

import java.util.Set;
import java.util.function.Supplier;

/**
 * The watch the entry ({@code DroolsBackend}) keeps on a session of the truth-maintenance branch between its own calls:
 * from {@code DroolsBackend.voegIn} or the last {@code DroolsBackend.vuur} on, it keeps the first fact that was
 * inserted, updated or deleted outside the entry, the first rule that fired outside it, and the first insert or delete
 * of a fact of a kind {@code voegIn} puts in. Two readings rest on it.
 *
 * <ul>
 *   <li>{@link TmsTranspiler#lees} and {@link TmsTranspiler#zetAfgeleideWaarden} read a session only where it kept
 *       nothing after the last {@code vuur}: after an update the facts of the last fixpoint belong to an input that no
 *       longer stands, and a plain {@code fireAllRules} fires the rule layer without the cap and, on a model with a
 *       creation, without the creations, which fire only in the generations of {@code vuur}. A mark in the session
 *       ({@link Gevuurd}) cannot see either, since it is itself a fact that no event touches.</li>
 *   <li>{@code vuur} fires a session only where no input came or went outside {@code voegIn}: {@code voegIn} states the
 *       places of a list ({@link Lijstplaats}) and the bucket scalars ({@link Emmerinvoer}) over the beans of one input,
 *       so a bean inserted later has neither, and a consumer that adds or removes an object opens a fresh session.
 *       Updates are the way to change input between two fires, and a fire of a rule does not count here: on a model
 *       without a creation no rule inserts or deletes a bean, a bucket input or a place, and a model with a creation
 *       fires once.</li>
 * </ul>
 *
 * <p>It lives on the session as a listener, so that it goes where the session goes and needs no table beside it, and
 * the entry finds it there ({@link #van}). The entry takes it off the session for its own work ({@link #zonder}): while
 * {@code vuur} fires, the session holds no listener of the watch, and Drools makes no event for it, so the watch costs
 * a fire nothing per firing. No rule reads it.
 */
public final class Bewaking extends DefaultAgendaEventListener implements RuleRuntimeEventListener {

    /** The kinds of fact {@code voegIn} puts in: the bean classes of the model, and what it states beside them. */
    private final Set<Class<?>> invoer;
    /** The call of the entry the watch keeps from, as a sentence names it. */
    private String sinds;
    /** The first insert, update or delete outside the entry, as a clause ("Vondst@1f werd gewijzigd met update"), or
     *  null. */
    private String feit;
    /** The name of the first rule that fired outside the entry, or null. */
    private String regel;
    /** The first insert or delete outside the entry of a fact of a kind {@link #invoer} holds, as a clause, or null. */
    private String erbijOfEraf;

    private Bewaking(Set<Class<?>> invoer) {
        this.invoer = Set.copyOf(invoer);
    }

    /** The watch on {@code sessie}, or null where the entry put none there. */
    public static Bewaking van(KieSession sessie) {
        for (RuleRuntimeEventListener l : sessie.getRuleRuntimeEventListeners())
            if (l instanceof Bewaking b) return b;
        return null;
    }

    /** The watch on {@code sessie}, put there where there is none, with {@code invoer} the kinds of fact
     *  {@code voegIn} puts in; it forgets what happened and keeps from now on, {@code sinds} naming the call of the
     *  entry it keeps from. */
    public static Bewaking op(KieSession sessie, Set<Class<?>> invoer, String sinds) {
        Bewaking b = van(sessie);
        if (b == null) {
            b = new Bewaking(invoer);
            sessie.addEventListener((RuleRuntimeEventListener) b);
            sessie.addEventListener((AgendaEventListener) b);
        }
        b.sinds = sinds;
        b.feit = null;
        b.regel = null;
        b.erbijOfEraf = null;
        return b;
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

    /** What happened outside the entry since {@link #sinds}, as one or two clauses: the first fact inserted, updated or
     *  deleted and the first rule that fired, each where there was one; null where nothing happened. */
    public String buitenom() {
        String vuurde = regel == null ? null : "de regel \"" + regel + "\" vuurde";
        if (feit == null) return vuurde;
        return vuurde == null ? feit : feit + " en " + vuurde;
    }

    /** The first insert or delete outside the entry since {@link #sinds} of a fact of a kind {@code voegIn} puts in, as
     *  a clause, or null. */
    public String erbijOfEraf() {
        return erbijOfEraf;
    }

    @Override public void objectInserted(ObjectInsertedEvent e) {
        gebeurd(e.getObject(), "ingevoegd", true);
    }

    @Override public void objectUpdated(ObjectUpdatedEvent e) {
        gebeurd(e.getObject(), "gewijzigd met update", false);
    }

    @Override public void objectDeleted(ObjectDeletedEvent e) {
        gebeurd(e.getOldObject(), "verwijderd", true);
    }

    @Override public void beforeMatchFired(BeforeMatchFiredEvent e) {
        if (regel == null) regel = e.getMatch().getRule().getName();
    }

    private void gebeurd(Object o, String wat, boolean erbijOfEraf) {
        if (feit != null && (!erbijOfEraf || this.erbijOfEraf != null)) return;
        String zin = Feitnamen.object(o) + " werd " + wat;
        if (feit == null) feit = zin;
        if (erbijOfEraf && this.erbijOfEraf == null && invoer.contains(o.getClass())) this.erbijOfEraf = zin;
    }
}
