package dev.uchar.regelspraak.transpiler.tms;

import org.kie.api.event.rule.AgendaEventListener;
import org.kie.api.event.rule.BeforeMatchFiredEvent;
import org.kie.api.event.rule.DefaultAgendaEventListener;
import org.kie.api.event.rule.ObjectDeletedEvent;
import org.kie.api.event.rule.ObjectInsertedEvent;
import org.kie.api.event.rule.ObjectUpdatedEvent;
import org.kie.api.event.rule.RuleRuntimeEventListener;
import org.kie.api.runtime.KieSession;

import java.util.function.Supplier;

/**
 * The watch the entry ({@code DroolsBackend}) keeps on a session of the truth-maintenance branch between its own calls:
 * from the last {@code DroolsBackend.vuur} on, it keeps the first fact that was inserted, updated or deleted outside the
 * entry and the first rule that fired outside it. {@link TmsTranspiler#lees} and
 * {@link TmsTranspiler#zetAfgeleideWaarden} read a session only where it kept nothing: after an update the facts of the
 * last fixpoint belong to an input that no longer stands, and a plain {@code fireAllRules} fires the rule layer without
 * the cap and, on a model with a creation, without the creations, which fire only in the generations of {@code vuur}.
 * A mark in the session ({@link Gevuurd}) cannot see either, since it is itself a fact that no event touches.
 *
 * <p>It lives on the session as a listener, so that it goes where the session goes and needs no table beside it, and
 * the entry finds it there ({@link #van}). The entry takes it off the session for its own work ({@link #zonder}): while
 * {@code vuur} fires, the session holds no listener of the watch, and Drools makes no event for it, so the watch costs
 * a fire nothing per firing. No rule reads it.
 */
public final class Bewaking extends DefaultAgendaEventListener implements RuleRuntimeEventListener {

    /** The first insert, update or delete outside the entry since the last {@code vuur}, as a clause
     *  ("Vondst@1f werd gewijzigd met update"), or null. */
    private String feit;
    /** The name of the first rule that fired outside the entry since the last {@code vuur}, or null. */
    private String regel;

    private Bewaking() {}

    /** The watch on {@code sessie}, or null where the entry put none there. */
    public static Bewaking van(KieSession sessie) {
        for (RuleRuntimeEventListener l : sessie.getRuleRuntimeEventListeners())
            if (l instanceof Bewaking b) return b;
        return null;
    }

    /** The watch on {@code sessie}, put there where there is none, watching from now on. */
    public static Bewaking op(KieSession sessie) {
        Bewaking b = van(sessie);
        if (b == null) {
            b = new Bewaking();
            sessie.addEventListener((RuleRuntimeEventListener) b);
            sessie.addEventListener((AgendaEventListener) b);
        }
        b.vergeet();
        return b;
    }

    /** Forgets what happened, at a {@code vuur}: from then on the watch keeps what happens after it. */
    public void vergeet() {
        feit = null;
        regel = null;
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

    /** What happened outside the entry since the last {@code vuur}, as one or two clauses: the first fact inserted,
     *  updated or deleted and the first rule that fired, each where there was one; null where nothing happened. */
    public String buitenom() {
        String vuurde = regel == null ? null : "de regel \"" + regel + "\" vuurde";
        if (feit == null) return vuurde;
        return vuurde == null ? feit : feit + " en " + vuurde;
    }

    @Override public void objectInserted(ObjectInsertedEvent e) {
        if (feit == null) feit = Feitnamen.object(e.getObject()) + " werd ingevoegd";
    }

    @Override public void objectUpdated(ObjectUpdatedEvent e) {
        if (feit == null) feit = Feitnamen.object(e.getObject()) + " werd gewijzigd met update";
    }

    @Override public void objectDeleted(ObjectDeletedEvent e) {
        if (feit == null) feit = Feitnamen.object(e.getOldObject()) + " werd verwijderd";
    }

    @Override public void beforeMatchFired(BeforeMatchFiredEvent e) {
        if (regel == null) regel = e.getMatch().getRule().getName();
    }
}
