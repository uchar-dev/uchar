package dev.uchar.regelspraak.resolver;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link Dependencies} admits the instance-ordered cycles the verifier admits (IR-SPEC ch. 5.3, {@code
 * instance_ordering} in {@code spec/verifier/lib/checks.ml}) and no more. The witnesses are the corpus's: the two
 * goldens of the form carry no cycle, and every negative {@code wf045-instance-cycle-*} keeps its WF-045 cycle; where
 * its {@code expect.txt} pins a read under {@code WF-045.instance-read-outside}, the frontend names that read with the
 * same form, node, path and clause.
 *
 * <p><b>Red before the fix</b> (measured 2026-10-22 on the frontend of {@code 6efa0aa5}): both goldens gave WF-045
 * cycles (three in {@code jutterswet-vergunningketen}, five in {@code jutterswet-vergunningtermijnen}), and no read
 * outside was named.
 */
class InstanceOrderingWitnessTest {

    private static Path corpus() {
        for (Path p = Path.of("").toAbsolutePath(); p != null; p = p.getParent()) {
            Path candidate = p.resolve("corpus");
            if (Files.isDirectory(candidate.resolve("golden")) && Files.isDirectory(candidate.resolve("negative"))) return candidate;
        }
        return fail("kon corpus/ niet vinden vanaf " + Path.of("").toAbsolutePath()
                + " — deze getuige leest het corpus en mag niet overslaan als hij het niet vindt");
    }

    private static String read(Path p) throws IOException {
        return Files.readString(p, StandardCharsets.UTF_8);
    }

    @Test void the_goldens_of_the_instance_ordered_cycle_carry_no_cycle() throws IOException {
        for (String golden : List.of("jutterswet-vergunningketen", "jutterswet-vergunningtermijnen")) {
            Dependencies.Findings found = Dependencies.find(read(corpus().resolve("golden").resolve(golden).resolve("ir.json")));
            assertEquals(List.of(), found.cycles(), golden);
            assertEquals(List.of(), found.readsOutside(), golden);
        }
    }

    @Test void every_negative_keeps_its_cycle_and_names_the_read_its_expectation_pins() throws IOException {
        List<Path> negatives;
        try (Stream<Path> s = Files.list(corpus().resolve("negative"))) {
            negatives = s.filter(p -> p.getFileName().toString().startsWith("wf045-instance-cycle-")).sorted().toList();
        }
        // a floor: the nine negatives of the cards cycle-across-instances and instance-cycle-household-reads
        assertTrue(negatives.size() >= 9, () -> "verwachtte ten minste negen negatieven, vond " + negatives);
        List<String> problems = new ArrayList<>();
        int pinned = 0;
        for (Path negative : negatives) {
            String name = negative.getFileName().toString();
            Dependencies.Findings found = Dependencies.find(read(negative.resolve("ir.json")));
            if (found.cycles().stream().noneMatch(c -> c.wf().equals("WF-045"))) problems.add(name + ": geen WF-045-kring");
            List<String> expect = Files.readAllLines(negative.resolve("expect.txt"), StandardCharsets.UTF_8);
            if (!expect.get(0).equals("WF-045.instance-read-outside")) continue;
            pinned++;
            Map<String, String> params = new LinkedHashMap<>();
            for (String line : expect.subList(1, expect.size())) params.put(line.substring(0, line.indexOf('=')), line.substring(line.indexOf('=') + 1));
            boolean named = found.readsOutside().stream().anyMatch(r -> r.form().name().equals(params.get("form"))
                    && r.node().equals(params.get("node")) && r.path().equals(params.get("path"))
                    && r.clause().equals(params.get("clause")));
            if (!named) problems.add(name + ": " + params + " niet bij de lezingen buiten " + found.readsOutside());
        }
        assertEquals(List.of(), problems);
        assertTrue(pinned >= 6, "zes negatieven pinnen een lezing buiten; gezien: " + pinned);
    }
}
