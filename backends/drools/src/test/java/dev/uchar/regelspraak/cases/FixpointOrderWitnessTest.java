package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.uchar.regelspraak.runtime.RegelSpraakRuntime;
import dev.uchar.regelspraak.transpiler.tms.Fixpoint;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Acceptance (a) of card fixpoint-inconsistency-order: the lists of {@link Fixpoint} that a consumer writes out as they
 * come — the objects and the links the creations made, and the §9.5 deposits — come in one order, whatever the order in
 * which the input went in and whatever the order of the working memory. Every case of the goldens below is fired on both
 * branches in fresh sessions, with its input objects in the order of the case, reversed, and rotated by one, and the three
 * lists, written in their order with the case labels, must be equal between those sessions. The working memory gives its
 * facts in an order of identities, which differs from one session to the next, so a list taken from it in that order
 * differs between two sessions of one input, as the toolchain measured on 13 of 231 goldens.
 *
 * <p>An input object is named by its case label and a made object by its creation and the name of its subject; two
 * made objects of one name would make the comparison blind to their order, and fail it. Floors, measured 2026-10-21: the
 * most entries one case gives in each list, {@value #EXPECTED_MIN_MOST_INCONSISTENCIES} deposits,
 * {@value #EXPECTED_MIN_MOST_OBJECTS} made objects and {@value #EXPECTED_MIN_MOST_LINKS} made links; a list of one entry
 * has no order to hold.
 */
class FixpointOrderWitnessTest {

    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");

    /** Goldens with several deposits on several objects ({@code jutterswet-keurcriteria}), and with several made objects
     *  and links ({@code jutterswet-afrekening}, {@code jutterswet-havenketen}, {@code jutterswet-premiepot},
     *  {@code jutterswet-dubbelverbinding}). */
    private static final List<String> GOLDENS = List.of("jutterswet-keurcriteria", "jutterswet-afrekening",
            "jutterswet-havenketen", "jutterswet-premiepot", "jutterswet-dubbelverbinding");

    static final int EXPECTED_MIN_MOST_INCONSISTENCIES = 11;
    static final int EXPECTED_MIN_MOST_OBJECTS = 7;
    static final int EXPECTED_MIN_MOST_LINKS = 7;

    @Test void the_lists_of_the_fixpoint_come_in_one_order_whatever_the_order_of_the_input() throws Exception {
        List<String> differences = new ArrayList<>();
        int mostInconsistencies = 0, mostObjects = 0, mostLinks = 0, sessions = 0;
        for (String golden : GOLDENS) {
            Path ir = GOLDEN.resolve(golden).resolve("ir.json");
            List<Path> cases;
            try (Stream<Path> s = Files.list(GOLDEN.resolve(golden))) {
                cases = s.filter(p -> p.getFileName().toString().matches("case.*\\.json")).sorted().toList();
            }
            for (DroolsBackend.Branch branch : TmsDocuments.BRANCHES) {
                CaseRunner runner = CaseRunner.forMeasurement(ir, branch);
                for (Path casePath : cases) {
                    JsonNode c = TmsDocuments.JSON.readTree(casePath.toFile());
                    if (c.has("expectedFault")) continue;
                    String which = branch + " " + golden + "/" + casePath.getFileName();
                    Lists first = null;
                    for (JsonNode ordered : orders(c)) {
                        Lists lists = runner.withSession(ordered, (build, session, labels) -> {
                            DroolsBackend.Firing firing = DroolsBackend.fire(build, session);
                            if (firing.noFixpoint() != null)
                                throw new IllegalStateException(which + ": geen fixpunt — " + firing.noFixpoint());
                            Map<Object, String> label = new IdentityHashMap<>();
                            labels.forEach((l, b) -> label.put(b, l));
                            return Lists.of(DroolsBackend.fixpointOf(build, session, label::get), labels);
                        });
                        sessions++;
                        if (first == null) first = lists;
                        else if (!first.equals(lists))
                            differences.add(which + ": eerst " + first + " | met de invoer in een andere volgorde "
                                    + lists);
                    }
                    mostInconsistencies = Math.max(mostInconsistencies, first.inconsistencies().size());
                    mostObjects = Math.max(mostObjects, first.objects().size());
                    mostLinks = Math.max(mostLinks, first.links().size());
                }
            }
        }
        int i = mostInconsistencies, o = mostObjects, k = mostLinks;
        System.out.println("FixpointOrderWitnessTest: " + sessions + " sessies; het meeste in een case: " + i
                + " deposito's, " + o + " gemaakte objecten, " + k + " koppelingen");
        assertTrue(i >= EXPECTED_MIN_MOST_INCONSISTENCIES, () -> "de meeste deposito's in een case " + i
                + ", onder de vloer " + EXPECTED_MIN_MOST_INCONSISTENCIES);
        assertTrue(o >= EXPECTED_MIN_MOST_OBJECTS, () -> "de meeste gemaakte objecten in een case " + o
                + ", onder de vloer " + EXPECTED_MIN_MOST_OBJECTS);
        assertTrue(k >= EXPECTED_MIN_MOST_LINKS, () -> "de meeste koppelingen in een case " + k + ", onder de vloer "
                + EXPECTED_MIN_MOST_LINKS);
        assertEquals(List.of(), differences, "cases waarvan een lijst van het fixpunt in een andere volgorde kwam");
    }

    /** {@code c} with its input objects in its own order, reversed, and rotated by one. */
    private static List<JsonNode> orders(JsonNode c) {
        List<JsonNode> instances = new ArrayList<>();
        c.get("input").get("instances").forEach(instances::add);
        List<JsonNode> reversed = new ArrayList<>(instances);
        java.util.Collections.reverse(reversed);
        List<JsonNode> rotated = new ArrayList<>(instances);
        java.util.Collections.rotate(rotated, 1);
        List<JsonNode> out = new ArrayList<>();
        for (List<JsonNode> order : List.of(instances, reversed, rotated)) {
            ObjectNode copy = c.deepCopy();
            ArrayNode list = ((ObjectNode) copy.get("input")).putArray("instances");
            order.forEach(list::add);
            out.add(copy);
        }
        return out;
    }

    /** The three lists of a {@link Fixpoint} as a consumer writes them out, in their order, each entry as a line. */
    private record Lists(List<String> objects, List<String> links, List<String> inconsistencies) {

        static Lists of(Fixpoint f, Map<String, Object> labels) {
            Map<Object, String> name = new IdentityHashMap<>();
            labels.forEach((l, b) -> name.put(b, "\"" + l + "\""));
            // a made object is named after its subject, which an earlier phase or the input made
            List<Fixpoint.MadeObject> open = new ArrayList<>(f.objects());
            while (!open.isEmpty()) {
                int before = open.size();
                open.removeIf(m -> {
                    String subject = name.get(m.subject());
                    if (subject == null) return false;
                    name.put(m.object(), "«" + m.creation() + "»(" + subject + ")");
                    return true;
                });
                if (open.size() == before)
                    throw new IllegalStateException("gemaakte objecten zonder benoemd onderwerp: " + open);
            }
            Set<String> names = new HashSet<>();
            for (Fixpoint.MadeObject m : f.objects())
                if (!names.add(name.get(m.object())))
                    throw new IllegalStateException("twee gemaakte objecten heten " + name.get(m.object())
                            + ": de vergelijking zou hun volgorde niet zien");
            List<String> objects = new ArrayList<>(), links = new ArrayList<>(), inconsistencies = new ArrayList<>();
            for (Fixpoint.MadeObject m : f.objects())
                objects.add(name.get(m.object()) + " door " + m.creation() + " voor " + name.get(m.subject()));
            for (Fixpoint.MadeLink k : f.links())
                links.add(k.factType() + " " + k.firstRole() + "=" + name.get(k.first()) + " " + k.secondRole() + "="
                        + name.get(k.second()));
            for (Object i : f.inconsistencies())
                inconsistencies.add(RegelSpraakRuntime.get(i, "rule") + " @ " + name.get(RegelSpraakRuntime.get(i, "subject"))
                        + " " + RegelSpraakRuntime.get(i, "codes"));
            return new Lists(objects, links, inconsistencies);
        }
    }
}
