package dev.uchar.regelspraak.resolver;

import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WF-046 holds on every cycle of the dependency graph, also on one that WF-045 admits because it runs through a creation
 * of a recursive rule group (IR-SPEC ch. 5.2): {@code check} refuses a negative edge on such a cycle at the head of the
 * form that reads, in the verifier's sentence, as the verifier's {@code wf045_046} does over the whole graph.
 *
 * <p><b>Red before the fix</b> (measured 2026-11-06 on the frontend of {@code 575de08e5}): the source of the negative
 * {@code wf046-negated-role-check-recursive-creation} and the empty test in a plain rule below resolved without a
 * diagnostic (exit 0 of {@code --ir}), while the verifier refused the written document under WF-046 (one line and two
 * lines); {@link Dependencies} found no WF-046 violation in any of the corpus's {@code wf046-*} negatives.
 */
class NegativeEdgeOnCycleTest {

    private static final String WACHTER = """
        Objecttype de Wachter (mv: Wachters) (bezield)
            de rang Numeriek (geheel getal);
            de opvolgerrang Numeriek (geheel getal);
            het merk Numeriek (geheel getal);

        Feittype opvolging
            de voorganger Wachter
            de opvolger (mv: opvolgers) Wachter
        Eén voorganger heeft één opvolger
        """;

    private static final String REFUSED = "leest negatief, zoals in een ontkenning of een leeg-toets, wat ";
    private static final String CLAUSE = ", op een kring; een kring draagt geen negatieve rand, ook niet een kring door "
            + "een recursieve creatie (WF-046): negative dependency edge on a cycle: ";

    private static List<Diagnostic> errors(RegelSpraakModel m) {
        return m.diagnostics.stream().filter(Diagnostic::isError).toList();
    }

    @Test void a_negated_role_check_on_the_relation_the_recursive_creation_lays_is_refused() {
        // the source of corpus/negative/wf046-negated-role-check-recursive-creation
        RegelSpraakModel m = Resolver.resolve(WACHTER + """

            Regelgroep estafette is recursief
            Regel werf de opvolger
                geldig altijd
                    Een Wachter heeft een opvolger met rang gelijk aan de rang van de Wachter plus 1
                    indien hij aan alle volgende voorwaarden voldoet:
                    - hij is geen voorganger
                    - zijn rang is kleiner dan 3.
            """);
        List<Diagnostic> errors = errors(m);
        assertEquals(1, errors.size(), () -> "verwachtte één ERROR: " + m.diagnostics);
        Diagnostic d = errors.get(0);
        assertEquals("RGS-379", d.code());
        assertEquals(12, d.line(), "de kop 'Regel werf de opvolger'");
        assertEquals("objectcreatie 'werf de opvolger' " + REFUSED + "objectcreatie 'werf de opvolger' schrijft" + CLAUSE
                + "werf de opvolger reads what werf de opvolger produces while werf de opvolger is reachable from it "
                + "(no strata exist)", d.message());
    }

    @Test void an_empty_test_in_a_plain_rule_on_a_cycle_through_a_recursive_creation_is_refused() {
        // the shape of corpus/negative/wf046-empty-test-recursive-creation: both tests are negative edges
        RegelSpraakModel m = Resolver.resolve(WACHTER + """

            Regel merk van een wachter
                geldig altijd
                    Het merk van een Wachter moet gesteld worden op 1
                    indien de rang van de opvolger van de Wachter gevuld is.

            Regelgroep estafette is recursief
            Regel werf de opvolger
                geldig altijd
                    Een Wachter heeft een opvolger met rang gelijk aan de rang van de Wachter plus 1
                    indien het merk van de Wachter leeg is.
            """);
        List<Diagnostic> errors = errors(m);
        assertEquals(List.of("RGS-379", "RGS-379"), errors.stream().map(Diagnostic::code).toList(), () -> "" + m.diagnostics);
        assertEquals(List.of(17, 11), errors.stream().map(Diagnostic::line).toList(), "de kop van de vorm die leest");
        assertEquals(List.of(
                "objectcreatie 'werf de opvolger' " + REFUSED + "regel 'merk van een wachter' schrijft" + CLAUSE
                        + "werf de opvolger reads what merk van een wachter produces while merk van een wachter is "
                        + "reachable from it (no strata exist)",
                "regel 'merk van een wachter' " + REFUSED + "objectcreatie 'werf de opvolger' schrijft" + CLAUSE
                        + "merk van een wachter reads what werf de opvolger produces while werf de opvolger is reachable "
                        + "from it (no strata exist)"),
                errors.stream().map(Diagnostic::message).toList());
    }

    @Test void the_chain_of_the_golden_without_a_negation_stays_without_a_message() throws IOException {
        Path source = corpus().resolve("golden").resolve("jutterswet-wachtersketen-rgs").resolve("source.rgs");
        RegelSpraakModel m = Resolver.resolve(Files.readString(source, StandardCharsets.UTF_8));
        assertEquals(List.of(), m.diagnostics);
    }

    /** Every {@code wf046-*} negative of the corpus carries a WF-046 violation in {@link Dependencies}, the one its
     *  {@code expect.txt} pins (reader and producer) where it has one; no golden and no red case carries one, as gate 3
     *  holds them verifier-clean. */
    @Test void every_wf046_negative_carries_its_violation_and_no_clean_document_one() throws IOException {
        List<Path> negatives;
        try (Stream<Path> s = Files.list(corpus().resolve("negative"))) {
            negatives = s.filter(p -> p.getFileName().toString().startsWith("wf046-")).sorted().toList();
        }
        // a floor: the seventeen negatives of WF-046 on 2026-11-06
        assertTrue(negatives.size() >= 17, () -> "verwachtte ten minste zeventien negatieven, vond " + negatives);
        List<String> problems = new ArrayList<>();
        for (Path negative : negatives) {
            List<Dependencies.Cycle> found = wf046(negative.resolve("ir.json"));
            if (found.isEmpty()) problems.add(negative.getFileName() + ": geen WF-046");
            Path expect = negative.resolve("expect.txt");
            if (!Files.exists(expect)) continue;
            List<String> pin = Files.readAllLines(expect, StandardCharsets.UTF_8);
            String reader = pin.get(1).substring("reader=".length()), producer = pin.get(2).substring("producer=".length());
            if (found.stream().noneMatch(k -> k.forms().get(0).name().equals(reader) && k.forms().get(1).name().equals(producer)))
                problems.add(negative.getFileName() + ": " + reader + " leest " + producer + " niet onder " + found);
        }
        List<Path> clean;
        try (Stream<Path> s = Stream.concat(Files.walk(corpus().resolve("golden")), Files.walk(corpus().resolve("red")))) {
            clean = s.filter(p -> p.getFileName().toString().equals("ir.json")).sorted().toList();
        }
        assertTrue(clean.size() >= 300, () -> "verwachtte ten minste 300 goldens en rode gevallen, vond " + clean.size());
        for (Path document : clean)
            if (!wf046(document).isEmpty()) problems.add(document + ": " + wf046(document));
        assertEquals(List.of(), problems);
    }

    private static List<Dependencies.Cycle> wf046(Path document) throws IOException {
        return Dependencies.find(Files.readString(document, StandardCharsets.UTF_8)).cycles().stream()
                .filter(k -> k.wf().equals("WF-046")).toList();
    }

    private static Path corpus() {
        for (Path p = Path.of("").toAbsolutePath(); p != null; p = p.getParent()) {
            Path candidate = p.resolve("corpus");
            if (Files.isDirectory(candidate.resolve("golden")) && Files.isDirectory(candidate.resolve("negative"))) return candidate;
        }
        return fail("kon corpus/ niet vinden vanaf " + Path.of("").toAbsolutePath()
                + " — deze getuige leest het corpus en mag niet overslaan als hij het niet vindt");
    }
}
