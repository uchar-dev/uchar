package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.cases.CaseRunner;
import dev.uchar.regelspraak.cases.DroolsBackend;
import dev.uchar.regelspraak.loader.IrLoader;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The DRL text of every golden held against a file in the test resources (card weergaven-als-getuige): for every golden
 * the exec-model branch builds ({@link DrlRenderingWitnessTest#goldens}), the text {@link DrlRendering} renders of its
 * plan must equal {@code src/test/resources/drl-text/<golden>.drl}, line by line. The witnesses of behaviour see a change
 * of the analysis or of a rendering only where it changes a fixpoint; this one sees every change of the text, also one
 * that changes no outcome, such as the order of the facts within one consequence. The DRL text is the plan written out
 * rule by rule, so a change of the plan shows here too; the rendering into the executable model has no text of its own
 * and is held to the same fixpoints by {@code BranchDumpsCorpusWitnessTest}.
 *
 * <p>A text that differs fails with the golden and the first line that differs, a golden without a file fails by name,
 * and so does a file whose golden no longer renders. A deliberate change of the analysis or the rendering, or a new
 * golden, writes the files again in the same commit: with {@code -Duchar.drlpin=src/test/resources/drl-text} (from
 * {@code backends/drools}, {@code mvn -o test -Dtest=DrlTextPinWitnessTest -Duchar.drlpin=…}) the witness writes the
 * text of every golden into that directory and removes the files there of a golden that no longer renders, before it
 * compares, and the diff of the commit shows the change of the text.
 *
 * <p><b>Red</b> with mutation W1 of Fable advice 4 (not committed): the facts of a decision of a defaults stack inserted
 * in reverse order in the DRL text ({@code DrlRendering}, the consequence of a {@code StackFact.Value}). The behaviour
 * witnesses stay green on it; this one names the golden and the line.
 */
class DrlTextPinWitnessTest {

    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");

    private static final Path PIN_DIRECTORY = Path.of("src", "test", "resources", "drl-text");

    /** Floor, measured 2026-11-06 (card weergaven-als-getuige): the goldens whose text the witness compares. A deliberate
     *  removal lowers it in the same commit. */
    private static final int EXPECTED_MIN_PINS = 355;

    @Test void the_text_of_every_golden_is_its_pin() throws Exception {
        Map<String, String> texts = new TreeMap<>();
        for (Path g : DrlRenderingWitnessTest.goldens()) {
            DroolsBackend.Build b;
            try {
                b = DroolsBackend.build(g.resolve("ir.json"), DroolsBackend.Branch.EXEC_MODEL);
            } catch (IrLoader.Refusal | IrLoader.DocumentError | CaseRunner.Excluded notPlayed) {
                continue;
            }
            texts.put(GOLDEN.relativize(g) + ".drl", DrlRendering.text(b.execModelTranspiler().modelplan()));
        }
        String renew = System.getProperty("uchar.drlpin");
        if (renew != null) {
            Path directory = Path.of(renew);
            for (String stale : pins(directory)) if (!texts.containsKey(stale)) Files.delete(directory.resolve(stale));
            for (Map.Entry<String, String> t : texts.entrySet()) {
                Path file = directory.resolve(t.getKey());
                Files.createDirectories(file.getParent());
                Files.writeString(file, t.getValue(), StandardCharsets.UTF_8);
            }
        }
        List<String> faults = new ArrayList<>();
        for (Map.Entry<String, String> t : texts.entrySet()) {
            Path pin = PIN_DIRECTORY.resolve(t.getKey());
            if (!Files.exists(pin)) {
                faults.add(t.getKey() + ": no pin");
                continue;
            }
            String difference = firstDifference(Files.readString(pin, StandardCharsets.UTF_8), t.getValue());
            if (difference != null) faults.add(t.getKey() + ": " + difference);
        }
        for (String pin : pins(PIN_DIRECTORY))
            if (!texts.containsKey(pin)) faults.add(pin + ": a pin of no golden the branch renders");
        assertEquals(List.of(), faults, "DRL texts that differ from their pin; after a deliberate change, write the pins"
                + " again with -Duchar.drlpin=src/test/resources/drl-text (see the class comment)");
        assertTrue(texts.size() >= EXPECTED_MIN_PINS,
                () -> texts.size() + " goldens compared, under the floor " + EXPECTED_MIN_PINS);
    }

    /** The first line where {@code text} differs from {@code pin}, with both lines, or null where they are equal. */
    static String firstDifference(String pin, String text) {
        if (pin.equals(text)) return null;
        String[] p = pin.split("\n", -1), t = text.split("\n", -1);
        for (int k = 0; k < Math.max(p.length, t.length); k++) {
            String a = k < p.length ? p[k] : "<end of the pin>", c = k < t.length ? t[k] : "<end of the text>";
            if (!a.equals(c)) return "line " + (k + 1) + ": the pin has «" + clip(a) + "», the text «" + clip(c) + "»";
        }
        throw new IllegalStateException("two texts that differ have no line that differs");
    }

    private static String clip(String line) {
        return line.length() > 160 ? line.substring(0, 160) + "…" : line;
    }

    /** The pin files under {@code directory}, by their path relative to it. */
    private static TreeSet<String> pins(Path directory) throws Exception {
        TreeSet<String> out = new TreeSet<>();
        if (!Files.isDirectory(directory)) return out;
        try (Stream<Path> s = Files.walk(directory)) {
            s.filter(f -> f.getFileName().toString().endsWith(".drl"))
                    .forEach(f -> out.add(directory.relativize(f).toString()));
        }
        return out;
    }

    @Test void the_first_difference_names_its_line_and_both_texts() {
        assertEquals(null, firstDifference("a\nb\n", "a\nb\n"));
        assertEquals("line 2: the pin has «b», the text «c»", firstDifference("a\nb\n", "a\nc\n"));
        assertEquals("line 3: the pin has «<end of the pin>», the text «d»", firstDifference("a\nb", "a\nb\nd"));
    }
}
