package dev.uchar.regelspraak.transpiler.tms;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The exceptions a {@link Fault} can carry and the section "Faults" of the backend's README that a consumer reads to
 * map them (card tweede-vulling-als-eigen-fouttype). A {@link Fault} admits only the kinds of {@link Fault#KINDS}, so
 * every fault the branch can set has one of them, and the README names exactly those, one bullet each. A fault of a
 * second filling has its own kind, whose rule is a fact type, and the head of {@link EvaluationFaults} names it so.
 */
class FaultKindsWitnessTest {

    private static final Path README = Path.of("README.md");

    @Test void the_readme_names_every_kind_a_fault_carries_and_no_other() throws Exception {
        String readme = Files.readString(README);
        int start = readme.indexOf("**Faults.**");
        int end = readme.indexOf("Any other exception is no evaluation fault", start);
        assertTrue(start >= 0 && end > start, "the README has a section \"Faults\" that ends in its closing sentence");
        Set<String> named = new TreeSet<>();
        Matcher m = Pattern.compile("(?m)^- `([A-Za-z.]+)`").matcher(readme.substring(start, end));
        while (m.find()) named.add(m.group(1));
        Set<String> kinds = new TreeSet<>();
        for (Class<?> k : Fault.KINDS)
            kinds.add(k.getEnclosingClass() == null ? k.getSimpleName()
                    : k.getEnclosingClass().getSimpleName() + "." + k.getSimpleName());
        assertEquals(kinds, named);
    }

    @Test void a_fault_of_another_kind_is_a_broken_contract() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new Fault(new Object(), "havenlid", new IllegalStateException("een tweede vulling")));
        assertTrue(e.getMessage().contains("java.lang.IllegalStateException"), e.getMessage());
    }

    @Test void a_second_filling_has_its_own_kind_and_the_head_names_its_fact_type() {
        Object jutter = new Object(), haven = new Object();
        SecondFillingFault second = new SecondFillingFault("haven", jutter);
        assertEquals("EF-017", second.code());
        assertEquals(List.of("role", "label"), List.copyOf(second.params().keySet()));
        assertEquals("haven", second.params().get("role"));
        assertEquals("J1", second.named(o -> o == jutter ? "J1" : null).params().get("label"));
        EvaluationFaults faults = new EvaluationFaults(List.of(
                new Fault(haven, "aandeel", new ComputeFault("een fout in een berekening")),
                new Fault(jutter, "havenlid", second)));
        assertEquals("aan het fixpunt staan 2 evaluatiefouten, in de regel \"aandeel\" en het feittype \"havenlid\"",
                faults.head());
        EvaluationFaults two = new EvaluationFaults(List.of(
                new Fault(jutter, "havenlid", second),
                new Fault(haven, "zeelid", new SecondFillingFault("zee", haven))));
        assertEquals("aan het fixpunt staan 2 evaluatiefouten, in de feittypen \"havenlid\", \"zeelid\"", two.head());
    }
}
