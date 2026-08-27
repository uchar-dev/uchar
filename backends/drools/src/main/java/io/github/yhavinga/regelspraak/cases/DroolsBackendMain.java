package io.github.yhavinga.regelspraak.cases;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The Drools-backend CLI the differential gate (scripts/check-drools.sh) drives, jar-only (decision
 * V2-B, the gate-10 pattern): given one golden's {@code ir.json} and its {@code case*.json} files, it
 * loads the IR off the wire, builds the Drools KieBase through the ported exec-model back-end, and
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
        if (args.length < 2) {
            System.err.println("usage: DroolsBackendMain <ir.json> <case*.json>...");
            System.exit(3);
        }
        Path ir = Path.of(args[0]);
        List<Path> cases = new ArrayList<>();
        for (int i = 1; i < args.length; i++) cases.add(Path.of(args[i]));
        try {
            CaseRunner.Result r = CaseRunner.runGolden(ir, cases);
            switch (r.status()) {
                case "PLAYED" -> { System.out.println("PLAYED " + r.casesPlayed()); System.exit(0); }
                case "EXCLUDE" -> { System.out.println("EXCLUDE " + r.reason()); System.exit(2); }
                default -> { System.out.println("FAIL " + String.join(" | ", r.caseFailures())); System.exit(1); }
            }
        } catch (RuntimeException e) {
            System.out.println("ERROR " + e.getClass().getSimpleName() + ": " + e.getMessage());
            System.exit(3);
        }
    }
}
