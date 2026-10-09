package dev.uchar.regelspraak.transpiler.tms;

/**
 * The creation phase the driver opened, as a stated control fact: {@code nummer} counts the phases of one session from
 * 1, and exactly one stands while the session fires a phase; {@code stratum} is the evaluation stratum the phase
 * belongs to. A generation is one phase, in which the fact creations fire and then the object creations, both on the
 * fixpoint the rule layer reached before the phase; the driver runs the generations of a stratum until one creates
 * nothing, and then those of the next ({@code DroolsBackend.vuur}). Every creation rule binds this fact, only in a
 * phase of its own stratum or one above, as a pass of the reference interpreter over stratum k runs the forms of
 * stratum k and below (RUNTIME-SEMANTICS ch. 6.3, {@code TmsTranspiler.creatiestrata}). What a creation made carries
 * the number of its phase ({@link Gemaakt}, {@link Koppeling}): a creation rule does not take an object or a link of
 * the phase it fires in, so what one creation made reaches the creations of the next phase only after the rule layer
 * has reached a fixpoint over it (RUNTIME-SEMANTICS ch. 5.5).
 */
public record Creatiefase(int nummer, int stratum) {

    public Creatiefase {
        if (nummer < 1) throw new IllegalArgumentException("creatiefase " + nummer + " < 1");
        if (stratum < 0) throw new IllegalArgumentException("stratum " + stratum + " < 0");
    }
}
