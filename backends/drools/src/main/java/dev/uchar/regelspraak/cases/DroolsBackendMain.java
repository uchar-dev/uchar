package dev.uchar.regelspraak.cases;

import dev.uchar.regelspraak.transpiler.tms.Evaluatiefouten;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The Drools-backend CLI the differential gate (scripts/check-drools.sh) drives, jar-only (decision
 * V2-B, the gate-10 pattern): given one golden's {@code ir.json} and its {@code case*.json} files, it
 * loads the IR off the wire, builds the Drools KieBase through the exec-model branch, and
 * replays every case. It prints exactly one status line and exits with a code the gate classifies:
 *
 * <pre>
 *   PLAYED &lt;n&gt;              exit 0   loaded, wholly lowered, every case matched the pinned expectations
 *   FAIL &lt;detail&gt;           exit 1   a played case diverged from the pinned expectation (the square broke)
 *   EXCLUDE &lt;reason&gt;        exit 2   a form the v0 backend does not carry (loader refusal / marker / …)
 *   ERROR &lt;detail&gt;          exit 3   an unexpected fault
 * </pre>
 */
public final class DroolsBackendMain {
    private DroolsBackendMain() {}

    public static void main(String[] args) {
        System.exit(voer(args, System.out));
    }

    /** The CLI without its exit: prints the status line on {@code uit} and returns the exit code. A witness runs the
     *  CLI through here inside the test JVM, which {@code System.exit} would end. */
    static int voer(String[] args, PrintStream uit) {
        // --drl replays through the OTHER transpiler branch (DrlTranspiler's DRL text, compiled here) instead of
        // the exec-model branch. Since 2026-09-19 (card gate12-drl-takteller) this flag is ALSO a gate-12
        // path: the gate sweeps every golden through it and pins the played count (EXPECTED_DRL_PLAYED) beside
        // the exec-model square's own. The two sweeps stay separate — a flagless run reaches the exec-model
        // branch, and that run alone is the differential square.
        boolean viaDrl = args.length > 0 && args[0].equals("--drl");
        // --drl-tms replays through DRL_TMS, the analysis of the exec-model branch rendered as DRL text, until the
        // switch-over of card drl-teksttak-op-truth-maintenance; no gate sweeps it
        boolean viaDrlTms = args.length > 0 && args[0].equals("--drl-tms");
        if (viaDrl || viaDrlTms) args = java.util.Arrays.copyOfRange(args, 1, args.length);
        if (args.length < 2) {
            System.err.println("usage: DroolsBackendMain [--drl | --drl-tms] <ir.json> <case*.json>...");
            return 3;
        }
        Path ir = Path.of(args[0]);
        List<Path> cases = new ArrayList<>();
        for (int i = 1; i < args.length; i++) cases.add(Path.of(args[i]));
        try {
            CaseRunner.Result r = viaDrl ? CaseRunner.viaDrlTekst(ir, cases)
                    : viaDrlTms ? CaseRunner.viaDrlTms(ir, cases) : CaseRunner.runGolden(ir, cases);
            switch (r.status()) {
                case "PLAYED" -> { uit.println("PLAYED " + r.casesPlayed()); return 0; }
                case "EXCLUDE" -> { uit.println("EXCLUDE " + r.reason()); return 2; }
                default -> { uit.println("FAIL " + String.join(" | ", r.caseFailures())); return 1; }
            }
        } catch (RuntimeException e) {
            uit.println("ERROR " + e.getClass().getSimpleName() + ": " + melding(e));
            return 3;
        }
    }

    /** The message of {@code e} for the ERROR line. Flattened as the DRL compile path does it (DroolsBackend.drlTak),
     *  and bounded at 200: every caller reads the LAST line of this output as the status line, so a multi-line
     *  exception message would push the ERROR head off it and hand the harness a fragment of a stack trace to diagnose
     *  with. The faults that stand at the fixpoint of the exec-model branch keep the head that names the rule of
     *  each whole, and the bound applies to their sentences only: a bound across the whole message would drop from the
     *  line every rule whose fault comes past it. */
    private static String melding(RuntimeException e) {
        if (e instanceof Evaluatiefouten f) return plat(f.kop()) + ": " + begrensd(plat(f.zinnen()));
        return begrensd(plat(String.valueOf(e.getMessage())));
    }

    private static String plat(String s) {
        return s.replace('\n', ' ');
    }

    private static String begrensd(String s) {
        return s.length() > 200 ? s.substring(0, 200) + "…" : s;
    }
}
