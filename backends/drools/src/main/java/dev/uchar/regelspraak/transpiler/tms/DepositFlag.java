package dev.uchar.regelspraak.transpiler.tms;

import java.util.List;
import java.util.TreeSet;

/**
 * The probe of the §6.5 and §6.6 deposit (RUNTIME-SEMANTICS ch. 1.2): a division of a filled dividend by a leeg
 * divisor, and a square root of a negative number, deposit a §9.5 inconsistency where the evaluation reaches them.
 * Which places run depends on the evaluation itself — the branch an {@code IfThenElse} takes, the binders a body reads
 * (ch. 10.2, 10.6) — so a static test of the operands cannot tell. The consequence of a computing rule or an
 * initialisation puts a fresh probe in its slot of the value array ({@code TmsTranspiler.Reads#depositFlag}) before it
 * computes the value, every place ({@link TmsTranspiler#depositSite}, a {@code Term.Division} or a {@code Term.SquareRoot}
 * with a probe slot) reports to it where it runs, and the consequence deposits where one did. A condition gets its
 * probe in the test of its deposit rule ({@link TmsTranspiler#depositRule}), and a fold's filter in the test of its
 * filter deposit rule ({@link TmsTranspiler#filterDepositRule}), whose {@link FilterDeposit} the evaluation of the
 * fold in the reading rule reports to that rule's probe ({@code Term.FoldRead}); elsewhere the slot is empty and a
 * place reports to nothing. A rendering makes, reports to and reads a probe through the public routes of
 * {@link ComputeHelpers} ({@code plan.Route}), which a DRL text reaches as well.
 */
final class DepositFlag {
    private boolean reported;
    private final TreeSet<String> codes = new TreeSet<>();

    /** A place ran and deposits to {@code depositFlag}, the probe its slot holds, or nothing where it holds none, with
     *  {@code codes}, the EF codes (RUNTIME-SEMANTICS ch. 5.7b) of the kinds it deposits: none for a kind without a code,
     *  the square root of a negative number, which the probe hears all the same. */
    static void report(Object depositFlag, List<?> codes) {
        if (depositFlag instanceof DepositFlag s) {
            s.reported = true;
            for (Object c : codes) s.codes.add((String) c);
        }
    }

    boolean reported() {
        return reported;
    }

    /** The codes the probe heard, ascending, each once. */
    List<String> codes() {
        return List.copyOf(codes);
    }
}
