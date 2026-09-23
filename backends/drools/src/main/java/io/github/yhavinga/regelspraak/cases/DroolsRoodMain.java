package io.github.yhavinga.regelspraak.cases;

import java.nio.file.Path;

/**
 * The Drools-backend RED-PATH CLI the red fence (scripts/check-rood.sh) drives, jar-only (the gate-12
 * pattern): given one rood golden's {@code ir.json} and ONE {@code expectedFault} {@code case.json}, it
 * loads the IR off the wire, builds the Drools KieBase through the ported exec-model back-end, and replays
 * the case to see what the v0 backend OBSERVES. It prints exactly one status line and exits with a code the
 * gate classifies:
 *
 * <pre>
 *   FAULT &lt;text&gt;      exit 0   any runtime exception after the build: at fire, or while the case is read,
 *                              a named refusal or a crash alike; the gate matches the pinned fragment, which
 *                              cites this backend's own sentence and never platform text
 *   NOFAULT &lt;detail&gt;  exit 1   the case ran to a model — the backend does not enforce this red-path constraint
 *   EXCLUDE &lt;reason&gt;  exit 2   a form the v0 backend does not carry (loader refusal / marker / unsupported input)
 *   ERROR &lt;detail&gt;    exit 3   bad invocation, I/O, or an exception while the IR is loaded and the KieBase
 *                              built — a crash AFTER the build is a FAULT, not an ERROR
 * </pre>
 *
 * The gate pins the Drools fault fragment only where the outcome is FAULT; a NOFAULT / EXCLUDE case is
 * listed in the script's reason-carrying exclusion list — the backend does not carry that red-path form.
 */
public final class DroolsRoodMain {
    private DroolsRoodMain() {}

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("usage: DroolsRoodMain <ir.json> <case.json>");
            System.exit(3);
        }
        Path ir = Path.of(args[0]);
        Path c = Path.of(args[1]);
        try {
            CaseRunner.RoodResult r = CaseRunner.runRood(ir, c);
            switch (r.status()) {
                case "FAULT" -> { System.out.println("FAULT " + r.detail()); System.exit(0); }
                case "EXCLUDE" -> { System.out.println("EXCLUDE " + r.detail()); System.exit(2); }
                default -> { System.out.println("NOFAULT " + r.detail()); System.exit(1); }
            }
        } catch (RuntimeException e) {
            System.out.println("ERROR " + e.getClass().getSimpleName() + ": " + e.getMessage());
            System.exit(3);
        }
    }
}
