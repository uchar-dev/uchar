package dev.uchar.regelspraak.cases;

import java.nio.file.Path;

/**
 * The Drools-backend RED-PATH CLI the red fence (scripts/check-red.sh) drives, jar-only (the gate-12
 * pattern): given one red golden's {@code ir.json} and ONE {@code expectedFault} {@code case.json}, it
 * loads the IR off the wire, builds the Drools KieBase through the exec-model branch, and replays
 * the case to see what the v0 backend OBSERVES. It prints exactly one status line and exits with a code the
 * gate classifies:
 *
 * <pre>
 *   FAULT [&lt;code&gt; &lt;params&gt;[, ...]: ]&lt;text&gt;  exit 0   (the codes: the EF codes of the fault's kinds,
 *                              RUNTIME-SEMANTICS ch. 5.7b, where it names any, each followed by its parameters as one JSON
 *                              object of strings, {} where it has none) any runtime exception after the build other than an Excluded — at fire or while
 *                              the case is read, a named refusal or a crash alike — and a run that hits the fire
 *                              cap without a fixpoint; the gate matches the pinned code and parameters
 *   NOFAULT &lt;detail&gt;  exit 1   the case ran to a model — the backend does not enforce this red-path constraint
 *   EXCLUDE DROOLS-###: &lt;reason&gt;  exit 2   (the register code of the refusal, beside its sentence) a form the v0 backend does not carry: a loader refusal, or an Excluded from the
 *                              build (a transpiler marker) or from the case reader (an input it declines)
 *   ERROR &lt;detail&gt;    exit 3   bad invocation, I/O, or an exception while the IR is loaded and the KieBase
 *                              built — a crash AFTER the build is a FAULT, not an ERROR
 * </pre>
 *
 * The gate pins the Drools code and parameters only where the outcome is FAULT; a NOFAULT / EXCLUDE case is
 * listed in the script's reason-carrying exclusion list — the backend does not carry that red-path form.
 */
public final class DroolsRedMain {
    private DroolsRedMain() {}

    /** {@code EF-005 {"rule":"…"}, EF-016 {…}}: each code of the report followed by its parameters. */
    private static String codesWithParams(CaseRunner.RedResult r) {
        String[] codes = r.code().split(", ");
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < codes.length; i++) {
            if (i > 0) out.append(", ");
            try {
                out.append(codes[i]).append(' ')
                        .append(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(r.params().get(i)));
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                throw new IllegalStateException("parameters of " + codes[i] + " are not JSON: " + r.params().get(i), e);
            }
        }
        return out.toString();
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("usage: DroolsRoodMain <ir.json> <case.json>");
            System.exit(3);
        }
        Path ir = Path.of(args[0]);
        Path c = Path.of(args[1]);
        try {
            CaseRunner.RedResult r = CaseRunner.runRed(ir, c);
            switch (r.status()) {
                // the fault kind's code at the head of the report, beside the text (RUNTIME-SEMANTICS ch. 5.7b)
                case "FAULT" -> {
                    System.out.println("FAULT " + (r.code() == null ? "" : codesWithParams(r) + ": ") + r.detail());
                    System.exit(0);
                }
                case "EXCLUDE" -> { System.out.println("EXCLUDE " + r.droolsCode().id() + ": " + r.detail()); System.exit(2); }
                default -> { System.out.println("NOFAULT " + r.detail()); System.exit(1); }
            }
        } catch (RuntimeException e) {
            System.out.println("ERROR " + e.getClass().getSimpleName() + ": " + e.getMessage());
            System.exit(3);
        }
    }
}
