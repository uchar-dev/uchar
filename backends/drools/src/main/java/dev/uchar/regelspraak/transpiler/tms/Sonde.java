package dev.uchar.regelspraak.transpiler.tms;

/**
 * The probe of the §6.5 and §6.6 deposit (RUNTIME-SEMANTICS ch. 1.2): a division of a filled dividend by a leeg
 * divisor, and a square root of a negative number, deposit a §9.5 inconsistency where the evaluation reaches them.
 * Which places run depends on the evaluation itself — the branch an {@code IfThenElse} takes, the binders a body reads
 * (ch. 10.2, 10.6) — so a static test of the operands cannot tell. The consequence of a computing rule or an
 * initialisation puts a fresh probe in its slot of the value array ({@code TmsTranspiler.Lezingen#sonde}) before it
 * computes the value, every place ({@code TmsTranspiler#deponeerplek}, a {@code Term.Deel} or a {@code Term.Wortel}
 * with a probe slot) reports to it where it runs, and the consequence deposits where one did. A condition gets its
 * probe in the test of its deposit rule ({@code TmsTranspiler#deponeerregel}), and a fold's filter in the test of its
 * filter deposit rule ({@code TmsTranspiler#filterdeponeerregel}), whose {@link Filterdeponering} the evaluation of the
 * fold in the reading rule reports to that rule's probe ({@code Term.Vouwlezing}); elsewhere the slot is empty and a
 * place reports to nothing.
 */
final class Sonde {
    private boolean gemeld;

    /** A place that qualifies ran and deposits; {@code plaats} is its probe's slot, -1 where the place does not
     *  qualify. */
    static void meld(Object[] w, int plaats) {
        if (plaats >= 0 && w[plaats] instanceof Sonde s) s.gemeld = true;
    }

    /** A place ran and deposits to {@code sonde}, the probe its slot holds, or nothing where it holds none. */
    static void meld(Object sonde) {
        if (sonde instanceof Sonde s) s.gemeld = true;
    }

    boolean gemeld() {
        return gemeld;
    }
}
