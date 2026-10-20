package dev.uchar.regelspraak.cases;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A quantifier fold whose range is a path (IR-SPEC ch. 4.5, WF-108 (c); RUNTIME-SEMANTICS ch. 3.4b, 3.9a), with a
 * matrix that can deposit: "het gewicht van ten minste één van de kisten van … groter is dan de norm gedeeld door de
 * deler". Such a matrix is read in the contributors' order, so each contribution carries its member's place (card
 * frontend-kwantor-over-een-pad, 2026-10-20). The two documents under {@code src/test/resources/quantifier-path} come
 * from the RegelSpraak frontend, each with its {@code source.rgs} beside it, and ref and the ASP runner pass both.
 *
 * <p>{@code one-route}: a Melding reaches its Vondsten over its melder, a to-one step and then a to-many one; the walk
 * from the member up to the Melding's melder meets the walk down from the Melding, so each member is bound once, at
 * its place in its melder's list, and both branches play the case.
 *
 * <p>{@code two-routes}: a Jutter reaches the Kisten of his Vondsten, a to-many step and then a to-one one; two vondsten
 * in one kist reach that kist twice. The contribution rule would bind the kist once per route, each at its own place,
 * and the replay would read it twice where the reference interpreter reads it once, at its first route, so both
 * branches refuse the form by name (DROOLS-291).
 */
class QuantifierPathWitnessTest {

    // cwd under surefire is the module dir (backends/drools)
    private static final Path MAP = Path.of("src", "test", "resources", "quantifier-path");

    @Test void an_ordered_matrix_over_one_route_plays_in_both_branches() {
        Path dir = MAP.resolve("one-route");
        List<Path> cases = List.of(dir.resolve("case.json"));
        CaseRunner.Result exec = CaseRunner.viaExecModel(dir.resolve("ir.json"), cases);
        assertEquals("PLAYED", exec.status(), exec.toString());
        assertEquals(exec.toString(), CaseRunner.viaDrlText(dir.resolve("ir.json"), cases).toString());
    }

    @Test void an_ordered_matrix_over_two_routes_is_refused_by_name_in_both_branches() {
        Path dir = MAP.resolve("two-routes");
        List<Path> cases = List.of(dir.resolve("case.json"));
        CaseRunner.Result exec = CaseRunner.viaExecModel(dir.resolve("ir.json"), cases);
        assertEquals("EXCLUDE", exec.status(), exec.toString());
        assertEquals("DROOLS-291", exec.code().id(), exec.toString());
        assertTrue(exec.reason().contains("kistplicht"), "de weigering noemt de regel: " + exec.reason());
        assertEquals(exec.toString(), CaseRunner.viaDrlText(dir.resolve("ir.json"), cases).toString(),
                "beide takken weigeren met dezelfde code en reden");
    }
}
