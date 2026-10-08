package dev.uchar.regelspraak.transpiler.tms;

/**
 * The creation phase the driver opened, as a stated control fact: {@code nummer} counts the phases of one session from
 * 1, and exactly one stands while the session fires a phase. A generation is a phase of fact creations followed by a
 * phase of object creations, and the driver lets the rule layer reach its fixpoint before each phase
 * ({@code DroolsBackend.vuur}). Every creation rule binds this fact, and what a creation made carries the number of
 * its phase ({@link Gemaakt}, {@link Koppeling}): a creation rule does not take an object or a link of the phase it
 * fires in, so what one creation made reaches the creations of the next phase only after the rule layer has reached a
 * fixpoint over it (RUNTIME-SEMANTICS ch. 5.5).
 */
public record Creatiefase(int nummer) {

    public Creatiefase {
        if (nummer < 1) throw new IllegalArgumentException("creatiefase " + nummer + " < 1");
    }
}
