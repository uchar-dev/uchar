package dev.uchar.regelspraak.cases;

import dev.uchar.regelspraak.transpiler.tms.EvaluationFaults;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * The Drools-backend CLI the differential gate (scripts/check-drools.sh) drives, jar-only (decision
 * V2-B, the gate-10 pattern): given one golden's {@code ir.json} and its {@code case*.json} files, it
 * loads the IR off the wire, builds the Drools KieBase through the exec-model branch, or with {@code --drl}
 * through the DRL-text branch, and replays every case. It prints exactly one status line and exits with a code the gate classifies:
 *
 * <pre>
 *   PLAYED &lt;n&gt;              exit 0   loaded, wholly lowered, every case matched the pinned expectations
 *   FAIL &lt;detail&gt;           exit 1   a played case diverged from the pinned expectation (the square broke)
 *   EXCLUDE DROOLS-###: &lt;reason&gt;  exit 2   (the register code of the refusal, beside its sentence) a form the v0 backend does not carry (loader refusal / marker / …)
 *   ERROR &lt;detail&gt;          exit 3   an unexpected fault
 * </pre>
 *
 * <p>With {@code --readings} (card fixpoint-readings) it builds the document with readings, plays every case and prints
 * per case {@code case <file>} and the readings of the conditions at its fixpoint, one line per record and per node in
 * the canonical text of {@code SessionDump.readingLines}, each after {@code reading }, then the end state of the session
 * (RUNTIME-SEMANTICS ch. 5.12a) in the text of {@code SessionDump.endStateLines}, each after {@code state }, and as its
 * last line
 * {@code READINGS <n>}, the cases it read, exit 0; a form it does not carry is the {@code EXCLUDE} line above, exit 2.
 * It compares no expectation: the text is what the trace of the reference interpreter is held to (card
 * ref-trace-and-readings-gate). {@code --readings --drl} reads the DRL-text branch.
 *
 * <p>With {@code --batch <tasks> <directory>} one JVM runs many of these calls, one after the other ({@link #batch}).
 */
public final class DroolsBackendMain {
    private DroolsBackendMain() {}

    /** The exit code of a batch that stopped before its end: a code no call of the CLI ends with. */
    static final int BATCH_STOPPED = 4;

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--batch")) {
            if (args.length == 3) System.exit(batch(Path.of(args[1]), Path.of(args[2]), DroolsBackendMain::run, System.err));
            System.err.println("usage: DroolsBackendMain --batch <tasks> <directory>");
            System.exit(BATCH_STOPPED);
        }
        System.exit(run(args, System.out));
    }

    /** The CLI without its exit: prints the status line on {@code out} and returns the exit code. A witness runs the
     *  CLI through here inside the test JVM, which {@code System.exit} would end. */
    static int run(String[] args, PrintStream out) {
        // --drl replays through the OTHER branch, the analysis rendered as DRL text and compiled here, instead of the
        // executable model. This flag is ALSO a gate-12 path (card gate12-drl-takteller): the gate sweeps every
        // golden through it and demands per golden the status line of the flagless run, which reaches the exec-model
        // branch and alone is the differential square.
        boolean readings = args.length > 0 && args[0].equals("--readings");
        if (readings) args = java.util.Arrays.copyOfRange(args, 1, args.length);
        boolean viaDrl = args.length > 0 && args[0].equals("--drl");
        if (viaDrl) args = java.util.Arrays.copyOfRange(args, 1, args.length);
        if (args.length < 2) {
            System.err.println("usage: DroolsBackendMain [--readings] [--drl] <ir.json> <case*.json>...");
            return 3;
        }
        Path ir = Path.of(args[0]);
        List<Path> cases = new ArrayList<>();
        for (int i = 1; i < args.length; i++) cases.add(Path.of(args[i]));
        if (readings) return readings(ir, cases, viaDrl, out);
        try {
            CaseRunner.Result r = viaDrl ? CaseRunner.viaDrlText(ir, cases) : CaseRunner.runGolden(ir, cases);
            switch (r.status()) {
                case "PLAYED" -> { out.println("PLAYED " + r.casesPlayed()); return 0; }
                case "EXCLUDE" -> { out.println("EXCLUDE " + r.code().id() + ": " + r.reason()); return 2; }
                default -> { out.println("FAIL " + String.join(" | ", r.caseFailures())); return 1; }
            }
        } catch (RuntimeException e) {
            out.println("ERROR " + e.getClass().getSimpleName() + ": " + message(e));
            return 3;
        }
    }

    /** A call of a CLI without its exit. */
    interface Call {
        int run(String[] args, PrintStream out);
    }

    /**
     * Runs the calls {@code tasks} lists, one after the other. A line is a task: its key, made of letters, digits, '.', '_'
     * and '-', and the arguments of the call, separated by tabs. Per task {@code <key>.out} in {@code directory} holds what
     * the call wrote on its standard output, and {@code <key>.rc}, which appears in one move once that file is closed, its
     * exit code: what a JVM of its own leaves. That holds for a call that throws an exception as well, whose stack trace
     * goes to the standard error and whose exit code is 1. The standard error of the calls is the one of this JVM.
     *
     * <p>The batch stops with a sentence on {@code err} and {@link #BATCH_STOPPED} where a task ends with an {@link Error};
     * where a task leaves the JVM other than it found it, in a system property, a default locale or the default time zone,
     * since that could decide the outcome of a later one; and where a line is no task, or a file of a task exists already
     * or cannot be written. The task it stopped at has no {@code .rc}, nor has one after it. A task that ends the JVM
     * itself leaves the same files, so whoever runs a batch holds every task to its {@code .rc}.
     */
    static int batch(Path tasks, Path directory, Call call, PrintStream err) {
        String line = "";
        try {
            List<String> lines = Files.readAllLines(tasks);
            if (lines.isEmpty()) throw new IOException("the list of tasks is empty");
            // the MVEL dialect writes this property on its first use, with the value an absent one gives it there
            // (Boolean.valueOf in MVELDebugHandler.isDebugMode, its one reader)
            System.setProperty("mvel.debugger", System.getProperty("mvel.debugger", "false"));
            Map<String, Object> found = jvmState();
            for (String next : lines) {
                line = next;
                String[] fields = line.split("\t", -1);
                if (fields.length < 2 || !fields[0].matches("[A-Za-z0-9._-]+")) throw new IOException("the line is no task");
                Path rc = directory.resolve(fields[0] + ".rc");
                if (Files.exists(rc)) throw new FileAlreadyExistsException(rc.toString());
                PrintStream out = new PrintStream(new BufferedOutputStream(Files.newOutputStream(
                        directory.resolve(fields[0] + ".out"), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)),
                        false, System.out.charset());
                int code;
                try (out) {
                    code = call.run(java.util.Arrays.copyOfRange(fields, 1, fields.length), out);
                } catch (RuntimeException e) {
                    e.printStackTrace(err);
                    code = 1;
                }
                if (out.checkError()) throw new IOException("the output of the task could not be written");
                String left = difference(found, jvmState());
                if (!left.isEmpty()) {
                    err.println("batch: the task '" + line + "' left the JVM other than it found it: " + left);
                    return BATCH_STOPPED;
                }
                Path unfinished = directory.resolve(fields[0] + ".rc.unfinished");
                Files.writeString(unfinished, code + "\n");
                Files.move(unfinished, rc, StandardCopyOption.ATOMIC_MOVE);
            }
            return 0;
        } catch (IOException e) {
            err.println("batch: stopped at the task '" + line + "': " + e);
            return BATCH_STOPPED;
        } catch (Error e) {
            err.println("batch: the task '" + line + "' ended with an error: " + e);
            e.printStackTrace(err);
            return BATCH_STOPPED;
        }
    }

    /** What a call could leave behind in the JVM for the next one, by name. The time zone first: the JDK determines it on
     *  its first read and keeps it in a system property. */
    private static Map<String, Object> jvmState() {
        Map<String, Object> state = new TreeMap<>();
        state.put("the default time zone", TimeZone.getDefault());
        state.put("the default locale", Locale.getDefault());
        for (Locale.Category c : Locale.Category.values()) state.put("the default locale of " + c, Locale.getDefault(c));
        System.getProperties().forEach((name, value) -> state.put("the system property " + name, value));
        return state;
    }

    private static String difference(Map<String, Object> found, Map<String, Object> left) {
        TreeSet<String> names = new TreeSet<>(found.keySet());
        names.addAll(left.keySet());
        names.removeIf(n -> Objects.equals(found.get(n), left.get(n)));
        return String.join(", ", names.stream().map(n -> n + " (" + found.get(n) + " to " + left.get(n) + ")").toList());
    }

    /** The run of {@code --readings}: the readings of every case, then {@code READINGS <n>}. */
    private static int readings(Path ir, List<Path> cases, boolean viaDrl, PrintStream out) {
        try {
            List<String> lines = CaseRunner.readings(ir, cases, viaDrl ? DroolsBackend.Branch.DRL_TEXT
                    : DroolsBackend.Branch.EXEC_MODEL);
            lines.forEach(out::println);
            out.println("READINGS " + lines.stream().filter(l -> l.startsWith("case ")).count());
            return 0;
        } catch (CaseRunner.Excluded e) {
            out.println("EXCLUDE " + e.code().id() + ": " + e.getMessage());
            return 2;
        } catch (dev.uchar.regelspraak.loader.IrLoader.Refusal e) {
            out.println("EXCLUDE " + e.code().id() + ": " + e.reason().framed("loader").text());
            return 2;
        } catch (java.io.IOException e) {
            out.println("ERROR IOException: " + message(new java.io.UncheckedIOException(e)));
            return 3;
        } catch (RuntimeException e) {
            out.println("ERROR " + e.getClass().getSimpleName() + ": " + message(e));
            return 3;
        }
    }

    /** The message of {@code e} for the ERROR line, flattened to one line and bounded at 200: every caller reads the LAST line of this output as the status line, so a multi-line
     *  exception message would push the ERROR head off it and hand the harness a fragment of a stack trace to diagnose
     *  with. The faults that stand at the fixpoint keep the head that names the rule of
     *  each whole, and the bound applies to their sentences only: a bound across the whole message would drop from the
     *  line every rule whose fault comes past it. */
    private static String message(RuntimeException e) {
        if (e instanceof EvaluationFaults f) return flatten(f.head()) + ": " + clamped(flatten(f.sentences()));
        return clamped(flatten(String.valueOf(e.getMessage())));
    }

    private static String flatten(String s) {
        return s.replace('\n', ' ');
    }

    private static String clamped(String s) {
        return s.length() > 200 ? s.substring(0, 200) + "…" : s;
    }
}
