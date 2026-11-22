package dev.uchar.regelspraak.cases;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The batch of {@code DroolsBackendMain --batch} ({@link DroolsBackendMain#batch}): per task it leaves the output and the
 * exit code of the call alone, the exit code once the output is closed; and it stops, with a sentence and an exit code no
 * call has, at a task that could decide the outcome of a later one, whose exit code it then leaves out, as it does those of
 * the tasks after it. The gates that replay in batches hold every task to its exit code ({@code scripts/lib/parallel.sh},
 * whose self-test has a program of its own in the place of the JVM). The rows whose task ends the JVM run in a child JVM,
 * with the flags the gates give a batch.
 */
class BatchWitnessTest {

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up
    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");

    @TempDir Path temp;
    private Path directory;

    @BeforeEach void an_empty_directory() throws IOException {
        directory = Files.createDirectory(temp.resolve("batch"));
    }

    @Test void every_task_leaves_the_output_and_the_exit_code_of_its_call_alone() throws IOException {
        Path played = GOLDEN.resolve("jutterswet-penningdrempel"), excluded = GOLDEN.resolve("jutterswet-havenrooster");
        Path missed = Files.writeString(temp.resolve("case-missed.json"), Files.readString(played.resolve("case.json"))
                .replace("\"characteristicPresent\", \"label\": \"J1\"", "\"characteristicPresent\", \"label\": \"J2\""));
        Map<String, List<String>> calls = new LinkedHashMap<>();
        for (List<String> flags : List.of(List.<String>of(), List.of("--drl"), List.of("--readings"),
                List.of("--readings", "--drl")))
            for (Path golden : List.of(played, excluded)) {
                List<String> call = new ArrayList<>(flags);
                call.add(golden.resolve("ir.json").toString());
                try (Stream<Path> s = Files.list(golden)) {
                    s.filter(p -> p.getFileName().toString().matches("case.*\\.json")).sorted().forEach(p -> call.add(p.toString()));
                }
                calls.put(calls.size() + "", call);
            }
        calls.put("missed", List.of(played.resolve("ir.json").toString(), missed.toString()));
        calls.put("no.file", List.of(temp.resolve("none.json").toString(), missed.toString()));
        calls.put("usage", List.of("--drl", played.resolve("ir.json").toString()));
        // a path that is none: an exception before the arms of the CLI
        calls.put("no-path", List.of("\0", missed.toString()));
        List<String> order = new ArrayList<>(calls.keySet());
        for (int turn = 0; turn < 2; turn++) {
            Path files = Files.createDirectory(temp.resolve("turn" + turn));
            Set<Thread> threads = userThreads();
            Run run = batch(files, order.stream().map(key -> line(key, calls.get(key))).toList(), DroolsBackendMain::run);
            assertEquals(0, run.code(), run.err());
            Set<String> tokens = new TreeSet<>();
            for (Map.Entry<String, List<String>> call : calls.entrySet()) {
                ByteArrayOutputStream alone = new ByteArrayOutputStream();
                int code;
                try {
                    code = DroolsBackendMain.run(call.getValue().toArray(String[]::new),
                            new PrintStream(alone, true, System.out.charset()));
                } catch (InvalidPathException e) {
                    code = 1;
                }
                assertEquals(alone.toString(System.out.charset()),
                        Files.readString(files.resolve(call.getKey() + ".out"), System.out.charset()), call.toString());
                assertEquals(code + "\n", Files.readString(files.resolve(call.getKey() + ".rc")), call.toString());
                List<String> lines = alone.toString(System.out.charset()).lines().toList();
                tokens.add(code + (lines.isEmpty() ? "" : " " + lines.get(lines.size() - 1).split(" ")[0]));
            }
            // every end of a call is among them
            assertEquals(Set.of("0 PLAYED", "0 READINGS", "1", "1 FAIL", "2 EXCLUDE", "3", "3 ERROR"), tokens);
            Set<Thread> left = userThreads();
            left.removeAll(threads);
            assertEquals(Set.of(), left, "a thread that keeps the JVM alive after the batch");
            Collections.reverse(order);
        }
    }

    @Test void the_exit_code_of_a_task_appears_once_its_output_is_closed() throws IOException {
        PrintStream[] first = new PrintStream[1];
        Run run = batch(directory, List.of("a\tx", "b\tx"), (args, out) -> {
            if (first[0] == null) {
                first[0] = out;
                out.print("all of it");
                assertEquals(Set.of("a.out"), names());
                return 2;
            }
            assertEquals(Set.of("a.out", "a.rc", "b.out"), names());
            first[0].print(" and more");
            first[0].flush();
            return 0;
        });
        assertEquals(0, run.code(), run.err());
        assertEquals(Set.of("a.out", "a.rc", "b.out", "b.rc"), names());
        assertEquals("all of it", Files.readString(directory.resolve("a.out")));
        assertEquals("2\n", Files.readString(directory.resolve("a.rc")));
    }

    @Test void a_task_that_leaves_the_JVM_other_than_it_found_it_stops_the_batch() throws IOException {
        String property = "uchar.batch.row";
        Locale locale = Locale.getDefault(), format = Locale.getDefault(Locale.Category.FORMAT);
        TimeZone zone = TimeZone.getDefault();
        Map<String, Runnable> rows = new LinkedHashMap<>();
        rows.put("the system property uchar.batch.row (null to left)", () -> System.setProperty(property, "left"));
        rows.put("the default locale (" + locale + " to tlh)", () -> Locale.setDefault(Locale.of("tlh")));
        rows.put("the default locale of FORMAT (" + format + " to tlh)",
                () -> Locale.setDefault(Locale.Category.FORMAT, Locale.of("tlh")));
        rows.put("the default time zone", () -> TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Chatham")));
        for (Map.Entry<String, Runnable> row : rows.entrySet()) {
            Path files = Files.createTempDirectory(temp, "row");
            Run run;
            try {
                run = batch(files, List.of("a\tx", "b\ty", "c\tx"), (args, out) -> {
                    out.println(args[0]);
                    if (args[0].equals("y")) row.getValue().run();
                    return 0;
                });
            } finally {
                System.clearProperty(property);
                Locale.setDefault(locale);
                Locale.setDefault(Locale.Category.FORMAT, format);
                TimeZone.setDefault(zone);
            }
            assertEquals(DroolsBackendMain.BATCH_STOPPED, run.code(), row.getKey());
            assertTrue(run.err().startsWith("batch: the task 'b\ty' left the JVM other than it found it: ")
                    && run.err().contains(row.getKey()), run.err());
            assertEquals(Set.of("a.out", "a.rc", "b.out"), names(files), row.getKey());
            assertEquals("y\n", Files.readString(files.resolve("b.out")), "the output was closed before the exit code was due");
        }
    }

    @Test void an_error_stops_the_batch_and_an_exception_is_the_exit_code_1_of_its_task() throws IOException {
        Run run = batch(directory, List.of("a\tx", "b\ty", "c\tz", "d\tx"), (args, out) -> {
            out.print(args[0]);
            if (args[0].equals("y")) throw new IllegalStateException("no arm of the CLI");
            if (args[0].equals("z")) throw new StackOverflowError();
            return 0;
        });
        assertEquals(DroolsBackendMain.BATCH_STOPPED, run.code());
        assertTrue(run.err().contains("java.lang.IllegalStateException: no arm of the CLI"), run.err());
        assertTrue(run.err().contains("batch: the task 'c\tz' ended with an error: java.lang.StackOverflowError"), run.err());
        assertEquals(Set.of("a.out", "a.rc", "b.out", "b.rc", "c.out"), names());
        assertEquals("1\n", Files.readString(directory.resolve("b.rc")));
    }

    @Test void a_list_that_is_none_stops_the_batch() throws IOException {
        DroolsBackendMain.Call plays = (args, out) -> 0;
        Files.writeString(directory.resolve("old.rc"), "0\n");
        Files.writeString(directory.resolve("cut.out"), "PLAYED 1\n");
        Map<String, List<String>> rows = new LinkedHashMap<>();
        rows.put("batch: stopped at the task '': java.io.IOException: the list of tasks is empty", List.of());
        rows.put("batch: stopped at the task 'b': java.io.IOException: the line is no task", List.of("b"));
        rows.put("batch: stopped at the task '../b\tx': java.io.IOException: the line is no task", List.of("../b\tx"));
        rows.put("batch: stopped at the task 'b\ty': java.nio.file.FileAlreadyExistsException: ", List.of("b\tx", "b\ty"));
        rows.put("batch: stopped at the task 'old\tx': java.nio.file.FileAlreadyExistsException: ", List.of("old\tx"));
        rows.put("batch: stopped at the task 'cut\tx': java.nio.file.FileAlreadyExistsException: ", List.of("cut\tx"));
        for (Map.Entry<String, List<String>> row : rows.entrySet()) {
            Run run = batch(directory, row.getValue(), plays);
            assertEquals(DroolsBackendMain.BATCH_STOPPED, run.code(), row.getKey());
            assertTrue(run.err().startsWith(row.getKey()), run.err());
        }
    }

    @Test void a_task_that_ends_the_JVM_leaves_no_exit_code_there_or_after_it() throws Exception {
        for (String what : List.of("exit", "memory")) {
            Path files = Files.createTempDirectory(temp, what);
            Path tasks = Files.write(temp.resolve("tasks-" + what), List.of("a\tplays", "b\t" + what, "c\tplays"));
            Process p = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                    "-XX:TieredStopAtLevel=1", "-XX:+UseSerialGC", "-Xmx32m", "-XX:+ExitOnOutOfMemoryError", "-cp",
                    System.getProperty("java.class.path"), BatchWitnessTest.class.getName(), tasks.toString(),
                    files.toString()).redirectErrorStream(true).start();
            String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(what.equals("exit") ? 7 : 3, p.waitFor(), out);
            assertEquals(what.equals("memory"), out.contains("java.lang.OutOfMemoryError"), out);
            assertEquals(Set.of("a.out", "a.rc", "b.out"), names(files), out);
        }
    }

    /** The child JVM of the row above: a batch whose task plays, ends the JVM, or fills its memory and catches what the
     *  JVM throws there. */
    public static void main(String[] args) {
        System.exit(DroolsBackendMain.batch(Path.of(args[0]), Path.of(args[1]), (task, out) -> {
            if (task[0].equals("exit")) System.exit(7);
            List<byte[]> held = new ArrayList<>();
            try {
                while (task[0].equals("memory")) held.add(new byte[1 << 20]);
            } catch (OutOfMemoryError e) {
                return held.size() > 0 ? 3 : 1;
            }
            return 0;
        }, System.err));
    }

    private record Run(int code, String err) {
    }

    private Run batch(Path files, List<String> lines, DroolsBackendMain.Call call) throws IOException {
        Path tasks = Files.write(Files.createTempFile(temp, "tasks", ""), lines);
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        int code = DroolsBackendMain.batch(tasks, files, call, new PrintStream(err, true, StandardCharsets.UTF_8));
        return new Run(code, err.toString(StandardCharsets.UTF_8));
    }

    private static String line(String key, List<String> arguments) {
        return key + "\t" + String.join("\t", arguments);
    }

    private Set<String> names() {
        return names(directory);
    }

    private static Set<String> names(Path files) {
        try (Stream<Path> s = Files.list(files)) {
            return s.map(f -> f.getFileName().toString()).collect(Collectors.toCollection(TreeSet::new));
        } catch (IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    /** The threads that keep a JVM alive: those that live and are no daemon. */
    private static Set<Thread> userThreads() {
        return Thread.getAllStackTraces().keySet().stream().filter(t -> !t.isDaemon()).collect(Collectors.toSet());
    }
}
